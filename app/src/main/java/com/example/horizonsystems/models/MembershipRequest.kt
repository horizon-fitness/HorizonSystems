package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class MembershipRequest(
    @SerializedName("member_id") val memberId: Int,
    @SerializedName("membership_plan_id") val planId: Int,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("sessions_total") val sessionsTotal: Int = -1, // -1 for Unlimited
    @SerializedName("subscription_status") val status: String = "Active",
    @SerializedName("payment_status") val paymentStatus: String = "Pending",
    @SerializedName("action") val action: String = "create_subscription"
)

data class MembershipResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("subscription_id") val subscriptionId: Int? = null,
    @SerializedName("history") val history: List<Transaction>? = null
)
