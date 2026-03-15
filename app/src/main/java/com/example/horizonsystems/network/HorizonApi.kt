package com.example.horizonsystems.network

import com.example.horizonsystems.models.User
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface HorizonApi {
    /**
     * Fetches the list of users from get_data.php.
     * The 'i=1' query parameter is often required by InfinityFree 
     * after the security cookie is set.
     */
    @GET("get_data.php")
    suspend fun getUsers(@Query("i") bypass: Int = 1): Response<List<User>>

    @FormUrlEncoded
    @POST("login.php")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Query("i") bypass: Int = 1
    ): Response<LoginResponse>

    @FormUrlEncoded
    @POST("register.php")
    suspend fun register(
        @Field("username") username: String,
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("first_name") firstName: String,
        @Field("last_name") lastName: String,
        @Field("gym_id") gymId: Int,
        @Query("i") bypass: Int = 1
    ): Response<LoginResponse>
}
