package com.example.mangoo

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.mangocam.R

class DiseaseDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disease_detail)

        val ivDiseaseImage = findViewById<ImageView>(R.id.ivDiseaseImage)
        val tvDiseaseName = findViewById<TextView>(R.id.tvDiseaseName)
        val tvAccuracy = findViewById<TextView>(R.id.tvAccuracy)
        val tvTreatment = findViewById<TextView>(R.id.tvTreatment)
        val tvDate = findViewById<TextView>(R.id.tvDate)

        // Retrieve extras
        val diseaseName = intent.getStringExtra("diseaseName")
        val accuracy = intent.getStringExtra("accuracy")
        val treatment = intent.getStringExtra("treatment")
        val date = intent.getStringExtra("date")
        val imageUriString = intent.getStringExtra("imageUri")

        // Set text
        tvDiseaseName.text = "🦠 $diseaseName"
        tvAccuracy.text = "📊 Accuracy: $accuracy"
        tvTreatment.text = "💊 $treatment"
        tvDate.text = "🕒 $date"

        // ✅ Display image
        imageUriString?.let {
            try {
                Glide.with(this)
                    .load(Uri.parse(imageUriString))
                    .into(ivDiseaseImage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
