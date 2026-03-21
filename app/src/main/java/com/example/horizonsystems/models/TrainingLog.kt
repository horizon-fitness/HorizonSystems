package com.example.horizonsystems.models

data class TrainingLog(
    val date: String,
    val time: String,
    val duration: String,
    val service: String,
    val trainer: String,
    val status: String // ACTIVE, COMPLETED, CANCELLED
)
