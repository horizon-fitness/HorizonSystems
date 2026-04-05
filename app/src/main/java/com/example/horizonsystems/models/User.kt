package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("member_id") val memberId: Int? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("password_hash") val passwordHash: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("middle_name") val middleName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("contact_number") val contactNumber: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null,
    @SerializedName("is_verified") val isVerified: Int? = null,
    @SerializedName("is_active") val isActive: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("tenant_id") val tenantId: String? = null,
    @SerializedName("gym_name") val gymName: String? = null,
    @SerializedName("gym_id") val gymId: Int? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("birth_date") val birthDate: String? = null,
    @SerializedName("sex") val sex: String? = null,
    @SerializedName("occupation") val occupation: String? = null,
    @SerializedName("medical_history") val medicalHistory: String? = null,
    @SerializedName("emergency_contact_name") val emergencyContactName: String? = null,
    @SerializedName("emergency_contact_number") val emergencyContactNumber: String? = null,
    @SerializedName("member_code") val memberCode: String? = null
)
