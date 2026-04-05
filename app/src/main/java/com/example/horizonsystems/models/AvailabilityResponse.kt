package com.example.horizonsystems.models

data class AvailabilityResponse(
    val success: Boolean,
    val available: Boolean,
    val message: String? = null
)
