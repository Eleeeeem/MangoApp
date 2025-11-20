package com.example.mangoo

import java.io.Serializable

data class PlantResponse(
    val result: IdentificationResult? = null,
    val health_assessment: HealthAssessment? = null,
    var date: String = "",
    var imageUri: String? = null
) : Serializable

data class IdentificationResult(
    val classification: Classification? = null,
    val disease: DiseaseResult? = null,
    val is_plant: IsPlant? = null
) : Serializable

data class Classification(
    val suggestions: List<PlantSuggestion> = emptyList()
) : Serializable

data class IsPlant(
    val probability: Double? = null,
    val binary: Boolean? = null
) : Serializable


data class PlantSuggestion(
    val id: String = "",
    val name: String = "",
    val probability: Double = 0.0,
    val similar_images: List<SimilarImage> = emptyList(),
    val details: PlantDetails? = null
) : Serializable


data class SimilarImage(
    val id: String = "",
    val url: String = "",
    val license_name: String = "",
    val license_url: String = "",
    val citation: String = "",
    val similarity: Double = 0.0,
    val url_small: String = ""
) : Serializable


data class DiseaseResult(
    var suggestions: List<DiseaseSuggestion>? = emptyList()
) : Serializable


data class HealthAssessment(
    val diseases: List<DiseaseSuggestion>? = emptyList()
) : Serializable


data class DiseaseSuggestion(
    val name: String? = null,
    val probability: Double? = null,
    val treatment: Treatment? = null,
    val description: String? = null,
    val cause: String? = null,
    val url: String? = null,
    val common_names: List<String>? = emptyList(),
    val similar_images: List<SimilarImage>? = emptyList(),
    var imageUri: String? = null,
    val details: Details? = null
) : Serializable


data class Details(
    val local_name: String = "",
    val description: String = "",
    val url: String? = null,
    val treatment: Treatment? = null,
    val classification: List<String> = emptyList(),
    val common_names: List<String>? = emptyList(),
    val cause: Any? = null,
    val language: String = "",
    val entity_id: String = ""
) : Serializable


data class Treatment(
    val chemical: List<String>? = emptyList(),
    val biological: List<String>? = emptyList(),
    val prevention: List<String>? = emptyList()
) : Serializable


data class PlantDetails(
    val common_names: List<String>? = emptyList(),
    val taxonomy: Taxonomy? = null,
    val url: String = "",
    val gbif_id: Int = 0,
    val inaturalist_id: Int = 0,
    val rank: String = "",
    val description: Description? = null,
    val synonyms: List<String> = emptyList(),
    val image: PlantImage? = null,
    val edible_parts: List<String>? = emptyList(),
    val watering: Watering? = null,
    val best_light_condition: String? = null,
    val best_soil_type: String? = null,
    val common_uses: String? = null,
    val cultural_significance: String? = null,
    val toxicity: String? = null,
    val best_watering: String? = null,
    val language: String = "",
    val entity_id: String = ""
) : Serializable


data class Taxonomy(
    val `class`: String = "",
    val genus: String = "",
    val order: String = "",
    val family: String = "",
    val phylum: String = "",
    val kingdom: String = ""
) : Serializable

data class Description(
    val value: String = "",
    val citation: String = "",
    val license_name: String = "",
    val license_url: String = ""
) : Serializable


data class PlantImage(
    val value: String = "",
    val citation: String = "",
    val license_name: String = "",
    val license_url: String = ""
) : Serializable


data class Watering(
    val max: Int = 0,
    val min: Int = 0
) : Serializable
