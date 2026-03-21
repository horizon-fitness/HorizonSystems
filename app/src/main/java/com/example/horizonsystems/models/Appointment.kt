package com.example.horizonsystems.models

data class Appointment(
    val id: String,
    val subject: String,
    val date: String,
    val time: String,
    val notes: String,
    val status: String // Pending, Approved, Cancelled
)
