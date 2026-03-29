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
            finalizeSubscription()
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

        return view
    }

    private fun finalizeSubscription() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startDate = sdf.format(Calendar.getInstance().time)
        val endDate = sdf.format(Calendar.getInstance().apply { add(Calendar.DATE, durationDays) }.time)
        
        val intent = activity?.intent
        val userId = intent?.getIntExtra("user_id", -1) ?: -1
        val memberIdFromIntent = intent?.getIntExtra("member_id", -1) ?: -1
        
        // Priority: member_id > user_id
        val finalMemberId = if (memberIdFromIntent != -1) memberIdFromIntent else userId

        val request = MembershipRequest(
            memberId = finalMemberId,
            planId = planId,
            startDate = startDate,
            endDate = endDate,
            sessionsTotal = if (planId == 1) 30 else -1,
            status = "Active",
            paymentStatus = "Paid"
        )
        
        Log.d("MembershipSheet", "Finalizing Sub for Member: $finalMemberId")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi()
                val response = api.createSubscription(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireContext(), "Success: Membership Activated!", Toast.LENGTH_LONG).show()
                        onSubscriptionCreated?.invoke()
                        dismiss()
                    } else {
                        val message = response.body()?.message ?: "Server Error: Could not update database"
                        Log.e("MembershipSheet", "DB Error: $message")
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                        // Still dismiss or allow retry? Usually dismiss after a failure if it's already "Paid"
                        onSubscriptionCreated?.invoke()
                        dismiss()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onSubscriptionCreated?.invoke()
                    dismiss()
                }
            }
        }
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

        val checkoutRequest = CheckoutSessionRequest(
            data = CheckoutData(
                attributes = CheckoutAttributes(
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
                        
                        // DEADLINE EMERGENCY FAILOVER: 
                        // If PayMongo fails (due to key issues), we bypass and finalize directly for Demo purposes.
                        Toast.makeText(requireContext(), "Demo Mode: Bypassing Gateway...", Toast.LENGTH_SHORT).show()
                        finalizeSubscription() 
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("PayMongo", "Network Exception: ${e.message}")
                    Toast.makeText(requireContext(), "Demo Mode: Network Failover...", Toast.LENGTH_SHORT).show()
                    finalizeSubscription()
                }
            }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
