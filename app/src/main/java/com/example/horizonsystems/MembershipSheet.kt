package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.horizonsystems.models.MembershipRequest
import com.example.horizonsystems.network.RetrofitClient
import com.example.horizonsystems.network.PayMongoApi
import com.example.horizonsystems.models.CheckoutSessionRequest
import com.example.horizonsystems.models.CheckoutData
import com.example.horizonsystems.models.CheckoutAttributes
import com.example.horizonsystems.models.LineItem
import com.example.horizonsystems.models.Billing
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.security.MessageDigest
import com.example.horizonsystems.utils.ThemeUtils

class MembershipSheet : BottomSheetDialogFragment() {

    private var planId: Int = 1
    private var planName: String = ""
    private var price: String = ""
    private var durationDays: Int = 30
    
    var onSubscriptionCreated: (() -> Unit)? = null

    companion object {
        fun newInstance(id: Int, name: String, priceStr: String, days: Int): MembershipSheet {
            val fragment = MembershipSheet()
            fragment.planId = id
            fragment.planName = name
            fragment.price = priceStr
            fragment.durationDays = days
            return fragment
        }
    }

    private val paymentResultLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // NOTE: finalizeSubscription() is now handled server-side via payment_success_redirect.php
            Toast.makeText(requireContext(), "Payment Successful! Your membership is now active.", Toast.LENGTH_LONG).show()
            onSubscriptionCreated?.invoke()
            dismiss()
        } else {
            Toast.makeText(requireContext(), "Payment Cancelled or Failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.sheet_membership, container, false)

        view.findViewById<TextView>(R.id.sheetPlanName).text = planName
        view.findViewById<TextView>(R.id.sheetPrice).text = price
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startDate = Calendar.getInstance()
        val endDate = Calendar.getInstance().apply { add(Calendar.DATE, durationDays) }
        
        view.findViewById<TextView>(R.id.sheetDuration).text = 
            "Starts: ${sdf.format(startDate.time)} | Ends: ${sdf.format(endDate.time)}"

        view.findViewById<View>(R.id.btnCancelSheet).setOnClickListener { dismiss() }
        
        view.findViewById<View>(R.id.btnConfirmSheet).setOnClickListener {
            Toast.makeText(requireContext(), "Processing...", Toast.LENGTH_SHORT).show()
            submitSubscription(sdf.format(startDate.time), sdf.format(endDate.time))
        }

        ThemeUtils.applyThemeToView(view)

        return view
    }


    private fun submitSubscription(start: String, end: String) {
        val intent = activity?.intent
        val userEmail = intent?.getStringExtra("email") ?: "customer@horizonsystems.com"
        val userName = intent?.getStringExtra("first_name") + " " + intent?.getStringExtra("last_name")
        val userPhone = intent?.getStringExtra("contact_number") ?: "09170000000"

        // Amount calculation in centavos for PayMongo
        val amountCentavos = when (planId) {
            1 -> 150000  // PHP 1,500.00
            2 -> 400000  // PHP 4,000.00
            3 -> 1400000 // PHP 14,000.00
            else -> 100000 // Default PHP 1,000.00 if unknown
        }

        val gymId = intent?.getIntExtra("gym_id", 1) ?: 1
        val userId = intent?.getIntExtra("user_id", -1) ?: -1
        val amountDecimal = "%.2f".format(amountCentavos / 100.0)

        // Security: Generate Signature for Backend Verification
        val salt = "FitPlatform_Secure_2026!"
        val sigInput = "$gymId$userId$planId$amountDecimal$salt"
        val sig = MessageDigest.getInstance("SHA-256")
            .digest(sigInput.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val baseUrl = "https://horizonfitnesscorp.gt.tc/api"
        val successUrl = "$baseUrl/payment_success_redirect.php?plan_id=$planId&amount=$amountDecimal&gym_id=$gymId&user_id=$userId&sig=$sig"

        val checkoutRequest = CheckoutSessionRequest(
            data = CheckoutData(
                attributes = CheckoutAttributes(
                    successUrl = successUrl,
                    cancelUrl = "https://horizonfitnesscorp.gt.tc/api/payment_cancel.php",
                    billing = Billing(
                        name = if (userName.trim().isNotEmpty()) userName else "Horizon Member",
                        email = userEmail,
                        phone = userPhone
                    ),
                    lineItems = listOf(LineItem(amount = amountCentavos, name = planName)),
                    description = "Membership Subscription: $planName"
                )
            )
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(context)
                val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(context)
                val api = RetrofitClient.getApi(cookie, ua)
                
                // 1. Eligibility Check
                val checkResponse = api.checkSubscriptionStatus(userId)
                if (checkResponse.isSuccessful && checkResponse.body()?.canBuy == false) {
                    val msg = checkResponse.body()?.message ?: "You are not eligible to purchase a new membership at this time."
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // 2. Original Payment Logic
                val payApi = PayMongoApi.create()
                val response = payApi.createCheckoutSession(
                    PayMongoApi.getAuthHeader(),
                    checkoutRequest
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        val checkoutUrl = response.body()!!.data!!.attributes.checkoutUrl
                        val intentPayMongo = Intent(requireContext(), PayMongoActivity::class.java).apply {
                            putExtra("checkout_url", checkoutUrl)
                        }
                        paymentResultLauncher.launch(intentPayMongo)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("PayMongo", "API Error: $errorBody")
                        Toast.makeText(requireContext(), "Payment Gateway Error. Please try again later.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("PayMongo", "Network Exception: ${e.message}")
                    Toast.makeText(requireContext(), "Connection Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
