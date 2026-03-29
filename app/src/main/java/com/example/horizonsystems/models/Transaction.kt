package com.example.horizonsystems.models

data class Transaction(
    val date: String,
    val time: String,
    val service: String,
    val reference: String,
    val amount: String,
    val status: String, // Approved, Pending, etc.
    val method: String? = "PayMongo"
)
