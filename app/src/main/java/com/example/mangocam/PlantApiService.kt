package com.example.mangoo

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface PlantApiService {
    @POST("identification")
    fun identifyPlant(
        @Body request: PlantRequest
    ): Call<PlantResponse>
}