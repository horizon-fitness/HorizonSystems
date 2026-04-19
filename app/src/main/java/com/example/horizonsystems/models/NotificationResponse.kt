package com.example.horizonsystems.models

data class NotificationResponse(
    val success: Boolean,
    val message: String? = null,
    val notifications: List<Notification>? = null
)
