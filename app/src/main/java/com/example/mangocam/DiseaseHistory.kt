package com.example.mangoo
import java.io.Serializable

data class DiseaseHistory(
    var diseaseName: String = "",
    var accuracy: String = "",
    var date: String = "",
    var biologicalTreatment: List<String> = emptyList(),
    var chemicalTreatment: List<String> = emptyList(),
    var prevention: List<String> = emptyList(),
    var imageUri: String? = null,
    var treatment: String? = null,
    var id: String = ""
) : Serializable
