package com.example.mangoo

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface PlantApiService {
    @POST("identification?details=common_names,url,description,taxonomy,rank,gbif_id,inaturalist_id,image,synonyms,edible_parts,watering,best_light_condition,best_soil_type,common_uses,cultural_significance,toxicity,best_watering&language=en")
    fun identifyPlant(
        @Body request: PlantRequest
    ): Call<PlantResponse>


    @POST("health_assessment?language=en&details=local_name,description,url,treatment,classification,common_names,cause")
    fun healthAssesment(
        @Body request: PlantRequest
    ): Call<PlantResponse>
}