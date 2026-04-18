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
    private var price: Double = 0.0
    private var durationDays: Int = 30
    
    var onSubscriptionCreated: (() -> Unit)? = null

    companion object {
        fun newInstance(id: Int, name: String, price: Double, days: Int): MembershipSheet {
            val fragment = MembershipSheet()
            fragment.planId = id
            fragment.planName = name
            fragment.price = price
            fragment.durationDays = days
            return fragment
        }
    }

    private val paymentResultLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val ctx = context ?: return@registerForActivityResult
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // NOTE: finalizeSubscription() is now handled server-side via payment_success_redirect.php
            Toast.makeText(ctx, "Payment Successful! Your membership is now active.", Toast.LENGTH_LONG).show()
            onSubscriptionCreated?.invoke()
            dismiss()
        } else {
            Toast.makeText(ctx, "Payment Cancelled or Failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = try {
            inflater.inflate(R.layout.sheet_membership, container, false)
        } catch (e: Exception) {
            Log.e("MembershipSheet", "Inflation error: ${e.message}")
            null
        } ?: return null

        view.findViewById<TextView>(R.id.sheetPlanName)?.text = planName
        
        val formatter = java.text.NumberFormat.getInstance(java.util.Locale.US)
        view.findViewById<TextView>(R.id.sheetPrice)?.text = "₱${formatter.format(price)}"
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startDate = Calendar.getInstance()
        val endDate = Calendar.getInstance().apply { add(Calendar.DATE, durationDays) }
        
        val btnConfirm = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmSheet)
        val cbAgreement = view.findViewById<android.widget.CheckBox>(R.id.cbAgreement)

        cbAgreement?.setOnCheckedChangeListener { _, isChecked ->
            btnConfirm?.isEnabled = isChecked
            btnConfirm?.alpha = if (isChecked) 1.0f else 0.5f
        }

        view.findViewById<View>(R.id.btnCancelSheet)?.setOnClickListener { dismiss() }
        
        btnConfirm?.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            Toast.makeText(ctx, "Processing Payment...", Toast.LENGTH_SHORT).show()
            submitSubscription(sdf.format(startDate.time), sdf.format(endDate.time))
        }

        applyBranding(view)

        return view
    }

    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColorStr = com.example.horizonsystems.utils.GymManager.getThemeColor(ctx)
        if (!themeColorStr.isNullOrEmpty()) {
            try {
                val themeColor = android.graphics.Color.parseColor(themeColorStr)
                view.findViewById<TextView>(R.id.sheetPlanName)?.setTextColor(themeColor)
                view.findViewById<android.widget.CheckBox>(R.id.cbAgreement)?.buttonTintList = 
                    android.content.res.ColorStateList.valueOf(themeColor)
                
                val btnConfirm = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmSheet)
                btnConfirm?.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor)
            } catch (e: Exception) {}
        }
    }


    private fun submitSubscription(start: String, end: String) {
        val ctx = context ?: return
        val intent = activity?.intent
        val userEmail = intent?.getStringExtra("email") ?: "customer@horizonsystems.com"
        val userName = (intent?.getStringExtra("first_name") ?: "") + " " + (intent?.getStringExtra("last_name") ?: "")
        val userPhone = intent?.getStringExtra("contact_number") ?: "09170000000"

        // Dynamic Amount calculation in centavos for PayMongo
        val amountCentavos = (price * 100).toInt()

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
                val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(ctx)
                val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(ctx)
                val api = RetrofitClient.getApi(cookie, ua)
                
                // 1. Eligibility Check
                val checkResponse = api.checkSubscriptionStatus(userId)
                if (checkResponse.isSuccessful && checkResponse.body()?.canBuy == false) {
                    val msg = checkResponse.body()?.message ?: "You are not eligible to purchase a new membership at this time."
                    withContext(Dispatchers.Main) {
                        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
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
                        val intentPayMongo = Intent(ctx, PayMongoActivity::class.java).apply {
                            putExtra("checkout_url", checkoutUrl)
                        }
                        paymentResultLauncher.launch(intentPayMongo)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("PayMongo", "API Error: $errorBody")
                        Toast.makeText(ctx, "Payment Gateway Error. Please try again later.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("PayMongo", "Network Exception: ${e.message}")
                    Toast.makeText(ctx, "Connection Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
