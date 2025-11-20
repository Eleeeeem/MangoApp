package com.example.mangocam

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mangoo.PlantResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

interface OnDiseaseDataListener {
    fun onDataReceived(data: PlantResponse?, imageUri: Uri?)
}

class PlantScanActivity : AppCompatActivity(), OnDiseaseDataListener {

    private var plantData: PlantResponse? = null
    private var plantImageUri: Uri? = null
    private val gson = Gson()
    private var treeId: String = "unknown" // unique tree id per plant/tree

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_plant_scan)

        // Retrieve Tree ID sent from previous activity
        treeId = intent.getStringExtra("treeId") ?: "unknown"

        // Load fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, DiseaseFragment())
            .commit()

        // Apply insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Called when SAVE button is pressed
     */
    fun SaveTree(view: android.view.View) {
        if (plantData == null) {
            Toast.makeText(this, "No Data Available.", Toast.LENGTH_SHORT).show()
            return
        }

        // Attach image URI as string
        plantData?.imageUri = plantImageUri?.toString()

        // Send back to previous activity if needed
        val intent = Intent()
        intent.putExtra("plantDetail", plantData)
        setResult(Activity.RESULT_OK, intent)

        Toast.makeText(this, "🌱 Plant scan saved to history.", Toast.LENGTH_SHORT).show()
        finish()
    }


    override fun onDataReceived(data: PlantResponse?, imageUri: Uri?) {
        if (data != null) {
            plantData = data
            plantImageUri = imageUri

            // Add date
            plantData?.date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Date())

            if (imageUri != null) {
                // ⭐ REQUIRED FIX FOR PHOTO PICKER
                try {
                    contentResolver.takePersistableUriPermission(
                        imageUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val imgString = imageUri.toString()
                plantData?.imageUri = imgString

                // Apply to all suggestions
                plantData?.result?.disease?.suggestions?.forEach {
                    it.imageUri = imgString
                }
            }
        }
    }
}
