package com.example.horizonsystems.models

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    val type: String, // 'membership', 'booking', 'system'
    val isRead: Boolean = false
)
