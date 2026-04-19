package com.example.horizonsystems.models

data class Notification(
    val notification_id: Int,
    val user_id: Int,
    val gym_id: Int?,
    val title: String,
    val message: String,
    val notification_type: String, // 'membership', 'booking', 'system'
    val is_read: Boolean = false,
    val created_at: String,
    val time_ago: String? = null
)
