package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.Transaction

class PaymentFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_payment, container, false)

        val rvTransactions = view.findViewById<RecyclerView>(R.id.rvTransactions)
        val emptyState = view.findViewById<TextView>(R.id.emptyState)

        // Sample data matching web reference
        val sampleTransactions = listOf(
            Transaction("Mar 21, 2024", "10:00 AM", "Unlimited Gym Use", "GCASH-12345", "500.00", "Approved"),
            Transaction("Feb 15, 2024", "02:30 PM", "Personal Training", "CASH-67890", "2000.00", "Approved")
        )

        if (sampleTransactions.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvTransactions.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvTransactions.visibility = View.VISIBLE
            rvTransactions.layoutManager = LinearLayoutManager(requireContext())
            rvTransactions.adapter = TransactionAdapter(sampleTransactions)
            
            // Calculate total (logic kept if needed for later, but UI removed per request)
            val total = sampleTransactions.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        }

        return view
    }
}
