package com.example.mangoo

data class PlantResponse(
    val result: IdentificationResult?,
    val health_assessment: HealthAssessment?
)

data class IdentificationResult(
    val classification: Classification,
    val disease: DiseaseResult?,
    val is_plant: IsPlant?
)

data class Classification(
    val suggestions: List<PlantSuggestion>
)

data class IsPlant(
    val probability: Double?,
    val binary: Boolean?
)

data class PlantSuggestion(
    val id: String,
    val name: String,
    val probability: Double,
    val similar_images: List<SimilarImage>,
    val details: PlantDetails
)

data class SimilarImage(
    val id: String,
    val url: String,
    val license_name: String,
    val license_url: String,
    val citation: String,
    val similarity: Double,
    val url_small: String
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

data class PlantDetails(
    val common_names: List<String>?,
    val taxonomy: Taxonomy,
    val url: String,
    val gbif_id: Int,
    val inaturalist_id: Int,
    val rank: String,
    val description: Description,
    val synonyms: List<String>,
    val image: PlantImage,
    val edible_parts: List<String>?,
    val watering: Watering?,
    val best_light_condition: String?,
    val best_soil_type: String?,
    val common_uses: String?,
    val cultural_significance: String?,
    val toxicity: String?,
    val best_watering: String?,
    val language: String,
    val entity_id: String
)

data class Taxonomy(
    val `class`: String,
    val genus: String,
    val order: String,
    val family: String,
    val phylum: String,
    val kingdom: String
)

data class Description(
    val value: String,
    val citation: String,
    val license_name: String,
    val license_url: String
)

data class PlantImage(
    val value: String,
    val citation: String,
    val license_name: String,
    val license_url: String
)

data class Watering(
    val max: Int,
    val min: Int
)