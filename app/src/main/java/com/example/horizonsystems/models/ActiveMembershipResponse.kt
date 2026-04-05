package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class ActiveMembershipResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("plan_name") val planName: String?,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("formatted_start") val formattedStart: String?,
    @SerializedName("formatted_end") val formattedEnd: String?,
    @SerializedName("subscription_status") val subscriptionStatus: String?,
    @SerializedName("days_remaining") val daysRemaining: Int?
)
