package com.example.horizonsystems.models

data class AttendanceRequest(
    val user_id: Int,
    val gym_id: Int,
    val action: String
)

data class AttendanceResponse(
    val success: Boolean,
    val message: String?,
    val is_checked_in: Boolean?,
    val last_session: AttendanceSession? = null
)

data class AttendanceSession(
    val check_in_time: String?,
    val check_out_time: String?,
    val attendance_status: String?
)

data class AttendanceLogsResponse(
    val success: Boolean,
    val message: String?,
    val logs: List<AttendanceLogItem>?
)

data class AttendanceLogItem(
    val attendance_date: String?,
    val check_in_time: String?,
    val check_out_time: String?,
    val attendance_status: String?,
    val gym_name: String?
)
