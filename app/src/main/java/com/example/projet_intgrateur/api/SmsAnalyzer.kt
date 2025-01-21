package com.example.projet_intgrateur.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsAnalyzer {
    private val TAG = "SmsAnalyzer"

    suspend fun analyzeMessage(message: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Envoi du message pour analyse : $message")
                val response = RetrofitClient.apiService.analyzeSms(SmsRequest(message))
                Log.d(TAG, "Réponse reçue : ${response.code()}")

                if (response.isSuccessful) {
                    val prediction = response.body() ?: throw Exception("Réponse vide de l'API")
                    Log.d(TAG, "Prédiction : $prediction")
                    Result.success(prediction)
                } else {
                    val error = response.errorBody()?.string() ?: "Erreur inconnue"
                    Log.e(TAG, "Erreur API : $error")
                    Result.failure(Exception("Erreur API : ${response.code()} - $error"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception lors de l'analyse", e)
                Result.failure(e)
            }
        }
    }
}