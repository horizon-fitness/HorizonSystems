package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class Gym(
    @SerializedName("gym_id") val gymId: Int,
    @SerializedName("owner_user_id") val ownerUserId: Int,
    @SerializedName("application_id") val applicationId: Int?,
    @SerializedName("gym_name") val gymName: String,
    @SerializedName("business_name") val businessName: String,
    @SerializedName("description") val description: String?,
    @SerializedName("address_id") val addressId: Int,
    @SerializedName("contact_number") val contactNumber: String,
    @SerializedName("email") val email: String,
    @SerializedName("profile_picture") val profilePicture: String?,
    @SerializedName("tenant_code") val tenantCode: String,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)
