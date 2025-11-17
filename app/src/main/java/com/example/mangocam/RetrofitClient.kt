package com.example.mangoo

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Base URL for the Plant ID API
    private const val PLANT_API_BASE_URL = "https://plant.id/api/v3/"

    private const val TIMEOUT = 30L // seconds

    // HTTP Client with Headers
    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Content-Type", "application/json")
                .header("Api-Key", "rARBuZXShw23PhZ9AqhJolLseHj9e5ZkERSyf8HS0wzmZbKGyw") // API Key for Plant ID
                .build()
            chain.proceed(request)
        }
        .build()

    // Retrofit instance for Plant API
    val plantApi: PlantApiService by lazy {
        Retrofit.Builder()
            .baseUrl(PLANT_API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlantApiService::class.java)
    }

}