package com.example.horizonsystems.network

import com.example.horizonsystems.models.CheckoutSessionRequest
import com.example.horizonsystems.models.CheckoutSessionResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import android.util.Base64

interface PayMongoApi {
    @POST("v1/checkout_sessions")
    suspend fun createCheckoutSession(
        @Header("Authorization") auth: String,
        @Body request: CheckoutSessionRequest
    ): Response<CheckoutSessionResponse>

    companion object {
        private const val BASE_URL = "https://api.paymongo.com/"
        
        // TEST SECRET KEY (Removed for security)
        private const val TEST_SECRET_KEY = "" // Insert Key Here

        fun create(): PayMongoApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val client = OkHttpClient.Builder()
                .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
                .create(PayMongoApi::class.java)
        }

        fun getAuthHeader(): String {
            val credentials = "$TEST_SECRET_KEY:"
            val base64 = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            return "Basic $base64"
        }
    }
}
