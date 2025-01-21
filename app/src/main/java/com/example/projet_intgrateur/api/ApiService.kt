package com.example.projet_intgrateur.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Headers

interface ApiService {
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST("predict")
    suspend fun analyzeSms(@Body request: SmsRequest): Response<String>
}