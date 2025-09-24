package com.example.mangoo

data class PlantResponse(
    val result: IdentificationResult?,
    val health_assessment: HealthAssessment?
)

data class IdentificationResult(
    val suggestions: List<PlantSuggestion>?,
    val disease: DiseaseResult?,
    val is_plant: IsPlant?
)

data class IsPlant(
    val probability: Double?,
    val binary: Boolean?
)

data class PlantSuggestion(
    val plant_name: String?,
    val probability: Double?,
    val common_names: List<String>? = null,
    val best_match: Boolean? = false,
    val similar_images: List<SimilarImage>?
)

data class SimilarImage(
    val id: String?,
    val url: String?,
    val license: String?,
    val citation: String?,
    val license_name: String?,
    val license_url: String?
)

data class DiseaseResult(
    val suggestions: List<DiseaseSuggestion>?
)

data class HealthAssessment(
    val diseases: List<DiseaseSuggestion>?
)

data class DiseaseSuggestion(
    val name: String?,
    val probability: Double?,
    val treatment: Treatment?,
    val description: String?,
    val cause: String?,
    val url: String?,
    val common_names: List<String>?,
    val similar_images: List<SimilarImage>?
)

data class Treatment(
    val biological: String?,
    val chemical: String?,
    val prevention: String?
)
