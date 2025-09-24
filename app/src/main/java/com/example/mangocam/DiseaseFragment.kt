package com.example.mangocam

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.mangoo.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.*

class DiseaseFragment : Fragment() {

    private lateinit var imageView: ImageView
    private lateinit var textViewResult: TextView
    private lateinit var captureButton: Button
    private lateinit var galleryButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var photoUri: Uri

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.disease_fragment, container, false)
        initializeViews(view)
        return view
    }

    private fun initializeViews(view: View) {
        imageView = view.findViewById(R.id.imageView)
        textViewResult = view.findViewById(R.id.accuracyText)
        captureButton = view.findViewById(R.id.captureBtn)
        galleryButton = view.findViewById(R.id.galleryBtn)
        progressBar = view.findViewById(R.id.progressBar)

        captureButton.setOnClickListener { openCamera() }
        galleryButton.setOnClickListener { openGallery() }
    }

    // --- Camera & Gallery ---
    private val captureImageLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) handleCapturedImage()
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleSelectedImage(it) }
        }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                photoFile
            )
            captureImageLauncher.launch(photoUri)
        } catch (e: Exception) {
            showError("Failed to open camera: ${e.message}")
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    // --- Image Handling ---
    private fun handleCapturedImage() {
        try {
            val bitmap = getBitmapFromUri(photoUri)
            imageView.setImageBitmap(bitmap)
            processImage(bitmap)
        } catch (e: Exception) {
            showError("Failed to process image: ${e.message}")
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            val bitmap = getBitmapFromUri(uri)
            imageView.setImageBitmap(bitmap)
            processImage(bitmap)
        } catch (e: Exception) {
            showError("Failed to process image: ${e.message}")
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(requireContext().contentResolver, uri)
            )
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", requireContext().externalCacheDir)
    }

    // --- Processing ---
    private fun processImage(bitmap: Bitmap) {
        val base64Image = compressImage(bitmap)
        identifyAndCheckHealth(base64Image)
    }

    private fun compressImage(bitmap: Bitmap): String {
        val resized = resizeBitmap(bitmap)
        val outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int = 800): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap

        val scale = maxSize.toFloat() / maxOf(width, height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // --- API Call ---
    private fun identifyAndCheckHealth(imageBase64: String) {
        showLoading("Detecting Disease...")

        val request = PlantRequest(
            images = listOf(imageBase64),
            health = "auto",
            classification_level = "species",
            similar_images = true,
            symptoms = true
        )

        RetrofitClient.plantApi.identifyPlant(request).enqueue(object : Callback<PlantResponse> {
            override fun onResponse(call: Call<PlantResponse>, response: Response<PlantResponse>) {
                progressBar.visibility = View.GONE

                if (!response.isSuccessful) {
                    showError("Identification failed: ${response.code()}")
                    return
                }

                val result = response.body()?.result
                if (result?.is_plant?.binary == false) {
                    showError("❌ This image does not appear to be a plant.")
                    return
                }

                val suggestions = result?.suggestions.orEmpty()
                val topSuggestion = suggestions.firstOrNull()
                val rawPlantName = topSuggestion?.plant_name ?: "Unknown Plant"
                val formattedPlantName =
                    if (rawPlantName.lowercase().contains("mangifera")) "Mangifera indica" else rawPlantName

                val diseaseSuggestions = result?.disease?.suggestions
                    ?: response.body()?.health_assessment?.diseases

                if (diseaseSuggestions.isNullOrEmpty()) {
                    showHealthyMessage(formattedPlantName)
                } else {
                    showDiseaseDetails(diseaseSuggestions, formattedPlantName)
                }
            }

            override fun onFailure(call: Call<PlantResponse>, t: Throwable) {
                handleNetworkError(t)
            }
        })
    }

    // --- Results ---
    private fun showDiseaseDetails(diseases: List<DiseaseSuggestion>, plantName: String) {
        val topDisease = diseases.maxByOrNull { it.probability ?: 0.0 }
        val genericName = topDisease?.name ?: "Unknown"
        val specificName =
            getSpecificDiseaseName(genericName, topDisease?.description, topDisease?.name)
        val name = specificName ?: genericName
        val accuracy = topDisease?.probability.toPercentageString()
        val treatmentText = treatmentMap[genericName]
            ?: "• No specific treatment found. Consider general plant care."

        val resultText = """
        🌱 Plant: $plantName
        🦠 Disease: $name
        📊 Accuracy: $accuracy
        $treatmentText
    """.trimIndent()

        activity?.runOnUiThread {
            textViewResult.setTextColor(Color.RED)
            textViewResult.text = resultText
        }

        val history = DiseaseHistory(
            plantName = plantName,
            diseaseName = name,
            accuracy = accuracy,
            treatment = treatmentText,
            date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        )
        saveToHistory(history)
    }

    private fun getSpecificDiseaseName(category: String?, description: String?, fallbackName: String?): String? {
        val keywordMap = specificDiseaseMap[category ?: return null] ?: return null
        val combinedText = listOfNotNull(description, fallbackName).joinToString(" ")

        for ((keyword, specificName) in keywordMap) {
            if (combinedText.contains(keyword, ignoreCase = true)) {
                return specificName
            }
        }
        return null
    }

    private fun showHealthyMessage(plantName: String) {
        val resultText = "✅ $plantName looks healthy!"
        activity?.runOnUiThread {
            textViewResult.setTextColor(Color.parseColor("#388E3C"))
            textViewResult.text = resultText
        }

        val history = DiseaseHistory(
            plantName = plantName,
            diseaseName = "Healthy",
            accuracy = "100%",
            treatment = "No treatment needed",
            date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        )
        saveToHistory(history)
    }

    private fun showLoading(message: String) {
        progressBar.visibility = View.VISIBLE
        activity?.runOnUiThread {
            textViewResult.text = "⏳ $message"
            textViewResult.setTextColor(Color.DKGRAY)
        }
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        activity?.runOnUiThread {
            textViewResult.text = "❌ $message"
            textViewResult.setTextColor(Color.GRAY)
        }
    }

    private fun handleNetworkError(t: Throwable) {
        val message = when (t) {
            is SocketTimeoutException -> "Connection timeout. Please try again."
            is IOException -> "Network error. Check your internet connection."
            else -> "Error: ${t.message}"
        }
        showError(message)
    }

    private fun saveToHistory(newEntry: DiseaseHistory) {
        val prefs = requireContext().getSharedPreferences("disease_history", Context.MODE_PRIVATE)
        val gson = Gson()

        val type = object : TypeToken<MutableList<DiseaseHistory>>() {}.type
        val historyList: MutableList<DiseaseHistory> =
            gson.fromJson(prefs.getString("history", null), type) ?: mutableListOf()

        historyList.add(0, newEntry)

        prefs.edit().putString("history", gson.toJson(historyList)).apply()

        Log.d("DiseaseFragment", "History saved: ${gson.toJson(historyList)}")
    }

    // --- Maps ---
    private val treatmentMap = mapOf(
        "Fungi" to "🍄 Treatment:\n• Apply fungicide.\n• Improve air circulation.\n• Avoid overhead watering.",
        "Bacteria" to "🦠 Treatment:\n• Apply copper-based bactericide.\n• Remove and destroy infected leaves.\n• Avoid overhead watering.",
        "Animalia" to "🐛 Treatment:\n• Use insecticidal soap.\n• Remove affected parts.",
        "Insecta" to "🦗 Treatment:\n• Neem oil spray.\n• Remove infested parts.",
        "mechanical damage" to "🪓 Treatment:\n• Prune damaged areas.\n• Avoid rough handling.",
        "senescence" to "🍂 Treatment:\n• No treatment needed – natural aging.",
        "nutrient deficiency" to "🌿 Treatment:\n• Apply fertilizer.\n• Check pH and water regularly.",
        "light excess" to "🔆 Treatment:\n• Provide partial shade.",
        "water excess" to "💧 Treatment:\n• Improve drainage.\n• Avoid overwatering.",
        "uneven watering" to "🚿 Treatment:\n• Water evenly.\n• Mulch to retain moisture."
    )

    private val specificDiseaseMap = mapOf(
        "Fungi" to mapOf(
            "powdery" to "Powdery Mildew",
            "downy" to "Downy Mildew",
            "blight" to "Leaf Blight",
            "rust" to "Rust Fungus",
            "spot" to "Leaf Spot",
            "mildew" to "Mildew Infection"
        ),
        "Insecta" to mapOf(
            "aphid" to "Aphid Infestation",
            "mite" to "Spider Mites",
            "thrip" to "Thrips Damage",
            "scale" to "Scale Insects",
            "mealybug" to "Mealybug Infestation"
        )
    )

    private fun Double?.toPercentageString(): String {
        return String.format(Locale.getDefault(), "%.2f%%", (this ?: 0.0) * 100)
    }
}
