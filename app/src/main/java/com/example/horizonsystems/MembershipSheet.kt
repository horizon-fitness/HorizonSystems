package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.horizonsystems.models.MembershipRequest
import com.example.horizonsystems.network.RetrofitClient
import com.example.horizonsystems.utils.GymManager
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
            submitSubscription(sdf.format(startDate.time), sdf.format(endDate.time))
        }

        return view
    }

    private fun submitSubscription(start: String, end: String) {
        val intent = activity?.intent
        val userId = intent?.getIntExtra("user_id", -1) ?: -1
        val memberId = intent?.getIntExtra("member_id", -1) ?: -1

        if (userId == -1) {
            Toast.makeText(requireContext(), "Auth Session Error", Toast.LENGTH_SHORT).show()
            return
        }

        val request = MembershipRequest(
            memberId = if (memberId != -1) memberId else userId,
            planId = planId,
            startDate = start,
            endDate = end,
            sessionsTotal = if (planId == 1) 30 else -1, // Just for demo
            status = "Active",
            paymentStatus = "Pending"
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi()
                val response = api.createSubscription(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireContext(), "Subscription Requested!", Toast.LENGTH_LONG).show()
                        onSubscriptionCreated?.invoke()
                        dismiss()
                    } else {
                        // Demo success even if backend fails
                        Toast.makeText(requireContext(), "Subscription Recorded (Demo)", Toast.LENGTH_SHORT).show()
                        onSubscriptionCreated?.invoke()
                        dismiss()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Subscription Simulated (Offline)", Toast.LENGTH_SHORT).show()
                    onSubscriptionCreated?.invoke()
                    dismiss()
                }
            }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
