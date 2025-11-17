package com.example.mangocam

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
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

    // ✅ Store image URI for both camera and gallery
    private var currentImageUri: Uri? = null

    private val mangoScientificNames = listOf(
        "Mangifera indica L.",
        "Mangifera altissima",
        "Mangifera odorata",
        "Mangifera caesia",
        "Mangifera indica 'Carabao'",
        "Mangifera indica 'Ataulfo'",
        "Mangifera indica 'Katchamitha'",
        "Mangifera indica 'Pico'"
    )

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
        if (context is OnDiseaseDataListener) listener = context
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    // ------------------------------- CAMERA + GALLERY --------------------------------------
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
            val photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                photoFile
            )
            currentImageUri = photoUri
            captureImageLauncher.launch(photoUri)
        } catch (e: Exception) {
            showError("Failed to open camera: ${e.message}")
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun handleCapturedImage() {
        try {
            currentImageUri?.let { uri ->
                var bitmap = getBitmapFromUri(uri)
                bitmap = toMutableBitmap(bitmap)
                imageView.setImageBitmap(bitmap)
                processImage(bitmap)
            }
        } catch (e: Exception) {
            showError("Failed to process image: ${e.message}")
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            currentImageUri = uri
            var bitmap = getBitmapFromUri(uri)
            bitmap = toMutableBitmap(bitmap)
            imageView.setImageBitmap(bitmap)
            processImage(bitmap)
        } catch (e: Exception) {
            showError("Failed to process image: ${e.message}")
        }
    }

    // ------------------------- BITMAP PROCESSING -------------------------------------
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

    private fun toMutableBitmap(bitmap: Bitmap): Bitmap {
        return if (bitmap.config == Bitmap.Config.HARDWARE || !bitmap.isMutable) {
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        } else bitmap
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File.createTempFile(
            "JPEG_${timeStamp}_", ".jpg", requireContext().externalCacheDir
        )
    }

    // ------------------------- API CALL FLOW -----------------------------------------
    private fun processImage(bitmap: Bitmap) {
        val base64Image = compressImage(bitmap)
        identifyAndCheckHealth(base64Image)
    }

    private fun compressImage(bitmap: Bitmap): String {
        val resized = resizeBitmap(bitmap)
        val output = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 85, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int = 800): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap
        val scale = maxSize.toFloat() / maxOf(width, height)
        return Bitmap.createScaledBitmap(
            bitmap,
            (width * scale).toInt(),
            (height * scale).toInt(),
            true
        )
    }

    // -------------------- IDENTIFICATION + HEALTH CHECK -------------------------------
    private fun identifyAndCheckHealth(imageBase64: String) {
        showLoading("Detecting Disease…")
        val request = PlantRequest(
            images = listOf(imageBase64),
            health = "auto",
            classification_level = "all",
            similar_images = true,
            symptoms = true
        )
        RetrofitClient.plantApi.identifyPlant(request).enqueue(object : Callback<PlantResponse> {
            override fun onResponse(
                call: Call<PlantResponse>,
                response: Response<PlantResponse>
            ) {
                progressBar.visibility = View.GONE
                if (!response.isSuccessful) {
                    showError("Identification failed: ${response.code()}")
                    return
                }
                val result = response.body()?.result ?: return
                val suggestions = result.classification?.suggestions ?: emptyList()
                val detectedNames = suggestions.map { it.name.lowercase() }
                val hasMango = detectedNames.any { name ->
                    mangoScientificNames.any { sci -> name.contains(sci.lowercase()) }
                            || name.contains("mango")
                            || name.contains("mangifera")
                }
                if (!hasMango) {
                    showError("⚠️ This does not appear to be a mango leaf or fruit.")
                    return
                }
                getMangoAssessment(imageBase64)
            }

            override fun onFailure(call: Call<PlantResponse>, t: Throwable) {
                handleNetworkError(t)
            }
        })
    }

    private fun getMangoAssessment(imageBase64: String) {
        val request = PlantRequest(
            images = listOf(imageBase64),
            health = "auto",
            classification_level = "all",
            similar_images = true,
            symptoms = true
        )
        RetrofitClient.plantApi.healthAssesment(request)
            .enqueue(object : Callback<PlantResponse> {
                override fun onResponse(
                    call: Call<PlantResponse>,
                    response: Response<PlantResponse>
                ) {
                    progressBar.visibility = View.GONE
                    if (!response.isSuccessful) {
                        showError("Health check failed: ${response.code()}")
                        return
                    }
                    val result = response.body()?.result ?: return
                    listener?.onDataReceived(response.body(), currentImageUri)
                    val diseases = result.disease?.suggestions
                        ?: response.body()?.health_assessment?.diseases
                    val currentBitmap = (imageView.drawable as? BitmapDrawable)?.bitmap
                    val isGreen = currentBitmap?.let { isGreenAndHealthy(it) } ?: false
                    if (diseases == null || diseases.isEmpty() || isGreen) {
                        showHealthyMessage("Mangifera indica")
                    } else {
                        showDiseaseDetails(diseases, "Mangifera indica")
                    }
                }

                override fun onFailure(call: Call<PlantResponse>, t: Throwable) {
                    handleNetworkError(t)
                }
            })
    }

    // ----------------------------- HEALTH RULES ---------------------------------------
    private fun isGreenAndHealthy(bitmap: Bitmap): Boolean {
        var greenPixels = 0
        var totalPixels = 0
        for (x in 0 until bitmap.width step 10) {
            for (y in 0 until bitmap.height step 10) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                if (g > r && g > b && g > 80) greenPixels++
                totalPixels++
            }
        }
        return greenPixels.toFloat() / totalPixels > 0.5f
    }

    private fun showDiseaseDetails(diseases: List<DiseaseSuggestion>, plantName: String) {
        val history = PlantDescriptionCreator.showDiseaseDetails(diseases, plantName)
        activity?.runOnUiThread {
            textViewResult.setTextColor(Color.RED)
            textViewResult.text = "🦠 Disease detected: ${history.diseaseName}"
        }
        saveToHistory(history)
    }

    private fun showHealthyMessage(plantName: String) {
        val history = PlantDescriptionCreator.showHealthyMessage(plantName)
        activity?.runOnUiThread {
            textViewResult.setTextColor(Color.parseColor("#2E7D32"))
            textViewResult.text = "✅ Mango looks healthy!"
        }
        saveToHistory(history)
    }

    // ------------------------------ SAVE HISTORY --------------------------------------
    private fun saveToHistory(newEntry: DiseaseHistory) {
        val finalEntry = newEntry.copy(
            imageUri = currentImageUri?.toString()
        )
        val prefs = requireContext().getSharedPreferences("disease_history", Context.MODE_PRIVATE)
        val gson = Gson()
        val type = object : TypeToken<MutableList<DiseaseHistory>>() {}.type
        val list: MutableList<DiseaseHistory> =
            gson.fromJson(prefs.getString("history", null), type) ?: mutableListOf()
        list.add(0, finalEntry)
        prefs.edit().putString("history", gson.toJson(list)).apply()
        Log.d("DiseaseFragment", "✅ Saved history: ${gson.toJson(list)}")
    }

    // ------------------------------ UTIL ----------------------------------------------
    private fun showLoading(message: String) {
        progressBar.visibility = View.VISIBLE
        activity?.runOnUiThread { textViewResult.text = "⏳ $message" }
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        activity?.runOnUiThread { textViewResult.text = "❌ $message" }
    }

    private fun handleNetworkError(t: Throwable) {
        val msg = when (t) {
            is SocketTimeoutException -> "Connection timeout. Try again."
            is IOException -> "Network error. Check your internet."
            else -> "Error: ${t.message}"
        }
        showError(msg)
    }
}
