package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class MembershipPlan(
    @SerializedName("membership_plan_id") val id: Int,
    @SerializedName("plan_name") val name: String,
    @SerializedName("price") val price: Double,
    @SerializedName("duration_value") val durationDays: Int,
    @SerializedName("billing_cycle_text") val billingCycle: String?,
    @SerializedName("featured_badge_text") val badgeText: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("features") val features: String? // Comma-separated or newline string
)
