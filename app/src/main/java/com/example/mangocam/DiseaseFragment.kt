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
import com.example.mangocam.utils.PlantDescriptionCreator
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

    private var listener: OnDiseaseDataListener? = null

    private lateinit var imageView: ImageView
    private lateinit var textViewResult: TextView
    private lateinit var captureButton: Button
    private lateinit var galleryButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var photoUri: Uri

    val mangoScientificNames = listOf( "Mangifera indica L.", "Mangifera altissima", "Mangifera odorata", "Mangifera caesia", "Mangifera indica 'Carabao'", "Mangifera indica 'Ataulfo'", "Mangifera indica 'Katchamitha'", "Mangifera indica 'Pico'" )

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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnDiseaseDataListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
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
            classification_level = "all",
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

                val suggestions = result?.classification?.suggestions
                val topSuggestion = suggestions?.firstOrNull()
                val inputName = topSuggestion?.name

                if(inputName == null)
                {
                    showError("Unable to process the image.")
                }else if(!matchesScientificNameContain(inputName, mangoScientificNames))
                {
                    showError("The image does not indicate a distinguishable part of a mango tree.")
                }else
                {
                    getMangoAssessment(imageBase64)
                }
            }

            override fun onFailure(call: Call<PlantResponse>, t: Throwable) {
                handleNetworkError(t)
            }
        })
    }

    fun matchesScientificNameContain(input: String, names: List<String>): Boolean {
        val inputLower = input.trim().lowercase()
        return names.any { fullName ->
            fullName.lowercase().contains(inputLower) || inputLower.contains(fullName.lowercase())
        }
    }
    private fun getMangoAssessment(imageBase64: String)
    {
        val request = PlantRequest(
            images = listOf(imageBase64),
            health = "auto",
            classification_level = "all",
            similar_images = true,
            symptoms = true
        )

        RetrofitClient.plantApi.healthAssesment(request).enqueue(object : Callback<PlantResponse> {
            override fun onResponse(call: Call<PlantResponse>, response: Response<PlantResponse>) {
                progressBar.visibility = View.GONE

                if (!response.isSuccessful) {
                    showError("Identification failed: ${response.code()}")
                    return
                }

                val result = response.body()?.result
                if (result?.is_plant?.binary == false) {
                    showError("This image does not appear to be a plant.")
                    return
                }

                listener?.onDataReceived(response.body())

                val suggestions = result?.classification?.suggestions
                val topSuggestion = suggestions?.firstOrNull()
                val rawPlantName = topSuggestion?.name ?: "Unknown Plant"
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
        val history = PlantDescriptionCreator.showDiseaseDetails(diseases,plantName)
        val diseaseName = history.diseaseName
        val resultText = """
                        Not Healthy
                        🦠 Disease: $diseaseName
                        """.trimIndent()

        activity?.runOnUiThread {
            textViewResult.setTextColor(Color.RED)
            textViewResult.text = resultText
        }

        saveToHistory(history)
    }

    private fun showHealthyMessage(plantName: String) {
        val resultText = "✅ $plantName looks healthy!"

        activity?.runOnUiThread {
            textViewResult.setTextColor(Color.parseColor("#388E3C"))
            textViewResult.text = resultText
        }

        val diseaseHistory = PlantDescriptionCreator.showHealthyMessage(plantName);
        saveToHistory(diseaseHistory)
    }

    private fun showLoading(message: String) {
        progressBar.visibility = View.VISIBLE
        activity?.runOnUiThread {
            textViewResult.text = "⏳ $message"
        }
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        activity?.runOnUiThread {
            textViewResult.text = "❌ $message"
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
}
