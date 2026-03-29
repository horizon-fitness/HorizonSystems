package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class CheckoutSessionRequest(
    @SerializedName("data") val data: CheckoutData
)

data class CheckoutData(
    @SerializedName("attributes") val attributes: CheckoutAttributes
)

data class CheckoutAttributes(
    @SerializedName("cancel_url") val cancelUrl: String = "https://horizonsystems.com/cancel",
    @SerializedName("success_url") val successUrl: String = "https://horizonsystems.com/success",
    @SerializedName("billing") val billing: Billing? = null,
    @SerializedName("line_items") val lineItems: List<LineItem>,
    @SerializedName("payment_method_types") val paymentMethodTypes: List<String> = listOf("gcash", "paymaya", "card"),
    @SerializedName("description") val description: String? = null
)

data class Billing(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String
)

data class LineItem(
    @SerializedName("amount") val amount: Int, // In centavos (e.g., 50000 = 500.00 PHP)
    @SerializedName("currency") val currency: String = "PHP",
    @SerializedName("name") val name: String,
    @SerializedName("quantity") val quantity: Int = 1
)

data class CheckoutSessionResponse(
    @SerializedName("data") val data: CheckoutSessionData? = null,
    @SerializedName("errors") val errors: List<PayMongoError>? = null
)

data class CheckoutSessionData(
    @SerializedName("id") val id: String,
    @SerializedName("attributes") val attributes: CheckoutSessionAttributes
)

data class CheckoutSessionAttributes(
    @SerializedName("checkout_url") val checkoutUrl: String,
    @SerializedName("status") val status: String
)

data class PayMongoError(
    @SerializedName("code") val code: String,
    @SerializedName("detail") val detail: String
)
