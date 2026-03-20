package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("user") val user: User?,
    @SerializedName("branding") val branding: TenantPage?,
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("unverified") val unverified: Boolean?
)
