package com.example.horizonsystems

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.Transaction
import com.example.horizonsystems.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MembershipFragment : Fragment() {
    private lateinit var adapter: TransactionAdapter
    private val historyList = mutableListOf<Transaction>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_membership, container, false)

        val rvMembershipHistory = view.findViewById<RecyclerView>(R.id.rvMembershipHistory)
        rvMembershipHistory.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter(historyList)
        rvMembershipHistory.adapter = adapter

        // Plan Selection Listeners
        view.findViewById<View>(R.id.btnSelectMonthly).setOnClickListener {
            showConfirmationSheet(1, "MONTHLY PASS", "₱1,500.00", 30)
        }
        
        view.findViewById<View>(R.id.btnSelectQuarterly).setOnClickListener {
            showConfirmationSheet(2, "QUARTERLY ELITE", "₱4,000.00", 90)
        }
        
        view.findViewById<View>(R.id.btnSelectAnnual).setOnClickListener {
            showConfirmationSheet(3, "VIP ANNUAL", "₱14,000.00", 365)
        }

        // Initial fetch
        fetchHistory()

        return view
    }

    private fun showConfirmationSheet(id: Int, name: String, price: String, days: Int) {
        val sheet = MembershipSheet.newInstance(id, name, price, days)
        sheet.onSubscriptionCreated = {
            fetchHistory()
        }
        sheet.show(parentFragmentManager, "membership_sheet")
    }

    private fun fetchHistory() {
        val userId = activity?.intent?.getIntExtra("user_id", -1) ?: -1
        if (userId == -1) {
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi()
                val response = api.getMembershipHistory(userId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        historyList.clear()
                        historyList.addAll(response.body()!!)
                        adapter.notifyDataSetChanged()
                    } else {
                        historyList.clear()
                        adapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                Log.e("MembershipFragment", "Fetch Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    historyList.clear()
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

}
