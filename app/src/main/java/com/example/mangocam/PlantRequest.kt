package com.example.mangoo

data class PlantRequest(
    val images: List<String>,
    val health: String?,
    val classification_level: String = "species",
    val similar_images: Boolean = true,
    val symptoms: Boolean = true,

)
