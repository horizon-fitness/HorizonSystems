package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class AttendanceRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("gym_id") val gymId: Int,
    @SerializedName("action") val action: String // 'check_in', 'check_out', or 'status'
)

data class AttendanceResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("is_checked_in") val isCheckedIn: Boolean?,
    @SerializedName("last_session") val lastSession: Map<String, Any>?
)
