package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class Coach(
    @SerializedName("coach_id")
    val coachId: Int,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("specialization")
    val specialization: String
)

data class CoachListResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("coaches")
    val coaches: List<Coach>
)
