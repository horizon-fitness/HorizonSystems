package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("tenant_id") val tenantId: String,
    @SerializedName("action") val action: String = "register"
)
