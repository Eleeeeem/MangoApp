package com.example.mangocam.utils

import com.example.mangoo.DiseaseHistory
import com.example.mangoo.DiseaseSuggestion
import java.text.SimpleDateFormat
import java.util.*

object PlantDescriptionCreator {

    fun showDiseaseDetails(
        diseases: List<DiseaseSuggestion>,
        plantName: String
    ): DiseaseHistory {
        val topDisease = diseases.firstOrNull()
        val treatment = topDisease?.details?.treatment

        val bio = treatment?.biological ?: emptyList()
        val chem = treatment?.chemical ?: emptyList()
        val prev = treatment?.prevention ?: emptyList()

        val accuracy = topDisease?.probability?.times(100)?.let {
            String.format("%.2f%%", it)
        } ?: "N/A"

        return DiseaseHistory(
            diseaseName = topDisease?.name ?: "Unknown Disease",
            accuracy = accuracy,
            imageUri = null,
            date = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date()),
            biologicalTreatment = bio,
            chemicalTreatment = chem,
            prevention = prev
        )
    }

    fun showHealthyMessage(plantName: String): DiseaseHistory {
        return DiseaseHistory(
            diseaseName = "$plantName is healthy",
            accuracy = "100%",
            imageUri = null,
            date = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date()),
            biologicalTreatment = emptyList(),
            chemicalTreatment = emptyList(),
            prevention = emptyList()
        )
    }
}
