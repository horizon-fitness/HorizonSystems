package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class CheckSubscriptionResponse(
    @SerializedName("can_buy") val canBuy: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("reason") val reason: String?
)
