package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("tenant_code") val tenantId: String? = null
)
