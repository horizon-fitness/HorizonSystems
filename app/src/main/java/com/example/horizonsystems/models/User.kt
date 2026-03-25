package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("username") val username: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("password_hash") val passwordHash: String?,
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("middle_name") val middleName: String?,
    @SerializedName("last_name") val lastName: String?,
    @SerializedName("contact_number") val contactNumber: String?,
    @SerializedName("profile_picture") val profilePicture: String?,
    @SerializedName("is_verified") val isVerified: Int?,
    @SerializedName("is_active") val isActive: Int?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("tenant_id") val tenantId: String?,
    @SerializedName("gym_name") val gymName: String?,
    @SerializedName("gym_id") val gymId: Int?,
    @SerializedName("role") val role: String?
)
