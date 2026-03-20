package com.example.horizonsystems.network

import com.example.horizonsystems.models.LoginResponse
import com.example.horizonsystems.models.TenantPage
import com.example.horizonsystems.models.User
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Query


interface HorizonApi {
    @POST("api/login.php")
    suspend fun login(
        @Body request: com.example.horizonsystems.models.LoginRequest,
        @Query("i") bypass: Int = 1
    ): Response<LoginResponse>

    @POST("api/register.php")
    suspend fun register(
        @Body request: com.example.horizonsystems.models.RegisterRequest,
        @Query("i") bypass: Int = 1
    ): Response<LoginResponse>

    @POST("api/register.php")
    suspend fun verifyUser(@Body body: Map<String, @JvmSuppressWildcards Any>): LoginResponse

    @GET("api/tenant.php")
    suspend fun getTenantInfo(
        @Query("gym") slug: String,
        @Query("i") bypass: Int = 1
    ): Response<TenantPage>

}
