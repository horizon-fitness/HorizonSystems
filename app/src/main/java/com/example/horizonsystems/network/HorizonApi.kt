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

    @GET("get_tenant.php")
    suspend fun getTenantInfo(
        @Query("gym") slug: String,
        @Query("i") bypass: Int = 1
    ): Response<TenantPage>


    @POST("api/validate_tenant.php")
    suspend fun validateTenant(
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.LoginResponse>

    @POST("api/forgot_password.php")
    suspend fun forgotPasswordAction(
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.LoginResponse>

    @GET("api/get_gym_services.php")
    suspend fun getGymServices(
        @Query("gym_id") gymId: Int,
        @Query("i") bypass: Int = 1
    ): Response<List<com.example.horizonsystems.models.GymService>>

    @GET("api/get_user_bookings.php")
    suspend fun getUserBookings(
        @Query("user_id") userId: Int,
        @Query("gym_id") gymId: Int,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.BookingResponse>

    @POST("api/create_booking.php")
    suspend fun createBooking(
        @Body request: com.example.horizonsystems.models.BookingRequest,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.BookingResponse>

    @GET("api/get_membership_history.php")
    suspend fun getMembershipHistory(
        @Query("user_id") userId: Int,
        @Query("gym_id") gymId: Int,
        @Query("show_all") showAll: Int = 0,
        @Query("i") bypass: Int = 1
    ): Response<List<com.example.horizonsystems.models.Transaction>>

    @GET("api/get_active_membership.php")
    suspend fun getActiveMembership(
        @Query("user_id") userId: Int,
        @Query("gym_id") gymId: Int,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.ActiveMembershipResponse>

    @POST("api/create_subscription.php")
    suspend fun createSubscription(
        @Body request: com.example.horizonsystems.models.MembershipRequest,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.MembershipResponse>

    @GET("api/check_subscription_status.php")
    suspend fun checkSubscriptionStatus(
        @Query("user_id") userId: Int,
        @Query("gym_id") gymId: Int,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.CheckSubscriptionResponse>

    @GET("api/get_gym_coaches.php")
    suspend fun getGymCoaches(
        @Query("gym_id") gymId: Int,
        @Query("date") date: String? = null,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.CoachListResponse>

    @GET("api/check_booking_availability.php")
    suspend fun checkBookingAvailability(
        @Query("user_id") userId: Int,
        @Query("gym_id") gymId: Int,
        @Query("date") date: String,
        @Query("time") time: String,
        @Query("coach_id") coachId: Int? = null,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.AvailabilityResponse>

    @GET("api/get_membership_plans.php")
    suspend fun getMembershipPlans(
        @Query("gym_id") gymId: Int,
        @Query("i") bypass: Int = 1
    ): Response<List<com.example.horizonsystems.models.MembershipPlan>>

    @POST("api/attendance.php")
    suspend fun recordAttendance(
        @Body request: com.example.horizonsystems.models.AttendanceRequest,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.AttendanceResponse>

    @GET("api/get_attendance_logs.php")
    suspend fun getAttendanceLogs(
        @Query("user_id") userId: Int,
        @Query("gym_id") gymId: Int,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.AttendanceLogsResponse>

    @GET("api/get_profile.php")
    suspend fun getProfile(
        @Query("user_id") userId: Int,
        @Query("tenant_code") tenantCode: String,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.LoginResponse>

    @POST("api/update_profile.php")
    suspend fun updateProfile(
        @Body request: Map<String, @JvmSuppressWildcards Any>,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.LoginResponse>

    @POST("api/change_password.php")
    suspend fun changePassword(
        @Body request: Map<String, @JvmSuppressWildcards Any>,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.LoginResponse>

    @POST("api/connect_gym.php")
    suspend fun connectGym(
        @Body request: Map<String, @JvmSuppressWildcards Any>,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.TenantPage>

    @POST("api/upload_profile_pic.php")
    suspend fun uploadProfilePic(
        @Body request: Map<String, @JvmSuppressWildcards Any>,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.LoginResponse>

    @GET("api/get_notifications.php")
    suspend fun getNotifications(
        @Query("user_id") userId: Int,
        @Query("i") bypass: Int = 1
    ): Response<com.example.horizonsystems.models.NotificationResponse>
}
