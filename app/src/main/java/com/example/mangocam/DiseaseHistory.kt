package com.example.mangoo

data class DiseaseHistory(
    val plantName: String = "",
    val diseaseName: String = "",
    val accuracy: String = "",
    val treatment: String? = null,
    val date: String = ""    // e.g. "03/04/2025 20:15"
)
