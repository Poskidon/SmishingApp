package com.example.projet_intgrateur.api

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val PREDICT_URL = "http://10.0.2.2:5001/"
    private const val FEEDBACK_URL = "http://10.5.0.2:5004/"
    //private const val PREDICT_URL = "http://192.168.37.103:50001/"
    //private const val FEEDBACK_URL = "http://192.168.37.103:50002/"
    private const val TAG = "RetrofitClient"

    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L
    private const val MAX_RETRIES = 3

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                var retryCount = 0
                var response = try {
                    chain.proceed(chain.request())
                } catch (e: Exception) {
                    Log.e(TAG, "Request failed: ${e.message}")
                    null
                }

                while (response == null && retryCount < MAX_RETRIES) {
                    retryCount++
                    Log.d(TAG, "Retrying request (attempt $retryCount/$MAX_RETRIES)")
                    response = try {
                        chain.proceed(chain.request())
                    } catch (e: Exception) {
                        Log.e(TAG, "Retry $retryCount failed: ${e.message}")
                        null
                    }
                }

                response ?: throw Exception("Failed to complete request after $MAX_RETRIES retries")
            }
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: ApiService = createRetrofit(PREDICT_URL)
        .create(ApiService::class.java)
        .also { Log.d(TAG, "ApiService created with URL: $PREDICT_URL") }

    val feedbackService: FeedbackService = createRetrofit(FEEDBACK_URL)
        .create(FeedbackService::class.java)
        .also { Log.d(TAG, "FeedbackService created with URL: $FEEDBACK_URL") }
}