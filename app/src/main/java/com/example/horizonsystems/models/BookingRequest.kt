package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class BookingRequest(
    @SerializedName("member_id") val memberId: Int,
    @SerializedName("gym_id") val gymId: Int,
    @SerializedName("gym_service_id") val gymServiceId: Int,
    @SerializedName("booking_date") val bookingDate: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("booking_source") val bookingSource: String = "Mobile App",
    @SerializedName("booking_status") val bookingStatus: String = "Pending",
    @SerializedName("action") val action: String = "create_booking"
)

data class BookingResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("booking_id") val bookingId: Int? = null,
    @SerializedName("bookings") val bookings: List<TrainingLog>? = null
)

data class GymService(
    @SerializedName("id") val serviceId: Int,
    @SerializedName("name") val serviceName: String,
    @SerializedName("price") val price: Double,
    @SerializedName("duration_minutes") val duration: Int? = 60
)
