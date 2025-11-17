package com.example.mangoo
import java.io.Serializable

data class DiseaseHistory(
    val diseaseName: String,
    val accuracy: String,
    val date: String,
    val biologicalTreatment: List<String>,
    val chemicalTreatment: List<String>,
    val prevention: List<String>,
    val imageUri: String?,
    val treatment: String? = null
) : Serializable


