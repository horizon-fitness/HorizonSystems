package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RegisterRequest(
    @SerializedName("first_name") val firstName: String,
    @SerializedName("middle_name") val middleName: String? = null,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("phone_number") val contactNumber: String,
    @SerializedName("birth_date") val birthDate: String,
    @SerializedName("sex") val sex: String,
    @SerializedName("occupation") val occupation: String? = null,
    @SerializedName("address_line") val addressLine: String,
    @SerializedName("barangay") val barangay: String,
    @SerializedName("city") val city: String,
    @SerializedName("province") val province: String,
    @SerializedName("region") val region: String,
    @SerializedName("medical_history") val medicalHistory: String? = null,
    @SerializedName("emergency_contact_name") val emergencyContactName: String,
    @SerializedName("emergency_contact_number") val emergencyContactNumber: String,
    @SerializedName("parent_name") val parentName: String? = null,
    @SerializedName("parent_contact_number") val parentContactNumber: String? = null,
    @SerializedName("gym_id") val tenantCode: String,
    @SerializedName("registration_source") val registrationSource: String = "Mobile",
    @SerializedName("registration_status") val registrationStatus: String = "Pending",
    @SerializedName("action") val action: String = "register",
    @SerializedName("pin") val pin: String? = null
) : Serializable
