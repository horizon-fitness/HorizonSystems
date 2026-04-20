package com.example.horizonsystems.models

data class TrainingLog(
    val booking_id: Int = 0,
    val booking_reference: String? = null,
    val date: String?,
    val time: String?,
    val duration: String?,
    val service: String?,
    val trainer: String?,
    val status: String? // ACTIVE, COMPLETED, CANCELLED, FORFEITED
)
