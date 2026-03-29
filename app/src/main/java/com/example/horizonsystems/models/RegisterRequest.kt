package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("first_name") val firstName: String,
    @SerializedName("middle_name") val middleName: String? = null,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("contact_number") val contactNumber: String,
    @SerializedName("birth_date") val birthDate: String,
    @SerializedName("sex") val sex: String,
    @SerializedName("occupation") val occupation: String? = null,
    @SerializedName("address") val address: String,
    @SerializedName("medical_history") val medicalHistory: String? = null,
    @SerializedName("emergency_contact_name") val emergencyContactName: String,
    @SerializedName("emergency_contact_number") val emergencyContactNumber: String,
    @SerializedName("tenant_code") val tenantCode: String,
    @SerializedName("registration_source") val registrationSource: String = "Mobile App",
    @SerializedName("registration_status") val registrationStatus: String = "Pending",
    @SerializedName("action") val action: String = "register"
)
