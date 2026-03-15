package com.example.horizonsystems.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://horizonfitnesscorp.gt.tc/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun getOkHttpClient(cookie: String?, userAgent: String?): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(logging)
        
        // If we have security cookies or specific user agents, inject them into every request
        if (cookie != null || userAgent != null) {
            builder.addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                cookie?.let { requestBuilder.addHeader("Cookie", it) }
                userAgent?.let { requestBuilder.addHeader("User-Agent", it) }
                chain.proceed(requestBuilder.build())
            }
        }
        
        return builder.build()
    }

    /**
     * Creates an instance of the HorizonApi.
     * @param cookie The __test cookie obtained from the WebView bypass.
     * @param userAgent The User-Agent string from the WebView to ensure consistency.
     */
    fun getApi(cookie: String? = null, userAgent: String? = null): HorizonApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getOkHttpClient(cookie, userAgent))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HorizonApi::class.java)
    }
}
