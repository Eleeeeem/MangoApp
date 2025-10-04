package com.example.mangoo

import java.io.Serializable

data class PlantResponse(
    val result: IdentificationResult?,
    val health_assessment: HealthAssessment?,
    var date : String
) : Serializable

data class IdentificationResult(
    val classification: Classification,
    val disease: DiseaseResult?,
    val is_plant: IsPlant?
): Serializable

data class Classification(
    val suggestions: List<PlantSuggestion>
): Serializable

data class IsPlant(
    val probability: Double?,
    val binary: Boolean?
): Serializable

data class PlantSuggestion(
    val id: String,
    val name: String,
    val probability: Double,
    val similar_images: List<SimilarImage>,
    val details: PlantDetails
): Serializable

data class SimilarImage(
    val id: String,
    val url: String,
    val license_name: String,
    val license_url: String,
    val citation: String,
    val similarity: Double,
    val url_small: String
): Serializable

data class DiseaseResult(
    val suggestions: List<DiseaseSuggestion>?
): Serializable

data class HealthAssessment(
    val diseases: List<DiseaseSuggestion>?
): Serializable

data class DiseaseSuggestion(
    val name: String?,
    val probability: Double?,
    val treatment: Treatment?,
    val description: String?,
    val cause: String?,
    val url: String?,
    val common_names: List<String>?,
    val similar_images: List<SimilarImage>?,
    val details: Details,
): Serializable

data class Details(
    val local_name: String,
    val description: String,
    val url: String?,
    val treatment: Treatment,
    val classification: List<String>,
    val common_names: List<String>?,
    val cause: Any?,
    val language: String,
    val entity_id: String,
) : Serializable

data class Treatment(
    val chemical: List<String>?,
    val biological: List<String>?,
    val prevention: List<String>?,
): Serializable

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
): Serializable

data class Taxonomy(
    val `class`: String,
    val genus: String,
    val order: String,
    val family: String,
    val phylum: String,
    val kingdom: String
): Serializable

data class Description(
    val value: String,
    val citation: String,
    val license_name: String,
    val license_url: String
): Serializable

data class PlantImage(
    val value: String,
    val citation: String,
    val license_name: String,
    val license_url: String
): Serializable

data class Watering(
    val max: Int,
    val min: Int
): Serializable