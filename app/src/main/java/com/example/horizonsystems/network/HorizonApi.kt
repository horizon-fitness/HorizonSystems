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
    /**
     * Fetches the list of users from get_data.php.
     * The 'i=1' query parameter is often required by InfinityFree 
     * after the security cookie is set.
     */
    @GET("get_data.php")
    suspend fun getUsers(@Query("i") bypass: Int = 1): Response<List<User>>

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

    @GET("get_tenant.php")
    suspend fun getTenantInfo(
        @Query("gym") slug: String,
        @Query("i") bypass: Int = 1
    ): Response<TenantPage>

    @POST("api/attendance.php")
    suspend fun handleAttendance(
        @Body request: com.example.horizonsystems.models.AttendanceRequest,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.AttendanceResponse>

}
