package com.example.horizonsystems

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.TextView
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
    private val fullHistoryList = mutableListOf<Transaction>()
    private var currentFilter = "ALL"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_membership, container, false)

        val rvMembershipHistory = view.findViewById<RecyclerView>(R.id.rvMembershipHistory)
        rvMembershipHistory.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter(historyList)
        rvMembershipHistory.adapter = adapter

        // Filter Buttons
        val btnAll = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFilterAll)
        val btnPending = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFilterPending)
        val btnApproved = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFilterApproved)

        btnAll.setOnClickListener { updateFilter("ALL", btnAll, btnPending, btnApproved) }
        btnPending.setOnClickListener { updateFilter("Pending", btnAll, btnPending, btnApproved) }
        btnApproved.setOnClickListener { updateFilter("Approved", btnAll, btnPending, btnApproved) }

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
        fetchActiveMembership(view)
        fetchHistory()

        return view
    }

    private fun fetchActiveMembership(root: View) {
        val loader = root.findViewById<View>(R.id.membershipLoading)
        val cardActive = root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardActiveMembership)
        val selectionHeader = root.findViewById<View>(R.id.layoutPlanSelectionHeader)
        val selectionScroll = root.findViewById<View>(R.id.layoutPlanSelectionScroll)

        // Reset state to Neutral/Loading
        loader.visibility = View.VISIBLE
        cardActive.visibility = View.GONE
        selectionHeader.visibility = View.GONE
        selectionScroll.visibility = View.GONE

        val userId = com.example.horizonsystems.utils.GymManager.getUserId(requireContext())
        if (userId == -1) {
            loader.visibility = View.GONE
            selectionHeader.visibility = View.VISIBLE
            selectionScroll.visibility = View.VISIBLE
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(context)
                val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(context)
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.getActiveMembership(userId)
                withContext(Dispatchers.Main) {
                    loader.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.success == true) {
                        val active = response.body()!!

                        // Show active card
                        val tvName = root.findViewById<TextView>(R.id.tvActivePlanName)
                        val tvStatus = root.findViewById<TextView>(R.id.tvActivePlanStatus)
                        val tvDuration = root.findViewById<TextView>(R.id.tvActivePlanDuration)
                        val tvRemaining = root.findViewById<TextView>(R.id.tvDaysRemaining)
                        
                        if (active.subscriptionStatus == "Pending Approval") {
                            tvName.text = active.planName
                            tvStatus.text = "(Pending Approval)"
                            tvStatus.visibility = View.VISIBLE
                            tvDuration.text = "Awaiting Staff Verification"
                            tvRemaining.text = "Payment verified by PayMongo"
                            tvName.setTextColor(android.graphics.Color.WHITE)
                        } else {
                            tvName.text = active.planName
                            tvStatus.visibility = View.GONE
                            tvDuration.text = "Until: ${active.formattedEnd}"
                            tvRemaining.text = "${active.daysRemaining} Days Remaining"
                            tvName.setTextColor(android.graphics.Color.WHITE)
                        }

                        cardActive.visibility = View.VISIBLE
                        selectionHeader.visibility = View.GONE
                        selectionScroll.visibility = View.GONE
                    } else {
                        cardActive.visibility = View.GONE
                        selectionHeader.visibility = View.VISIBLE
                        selectionScroll.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Log.e("MembershipFragment", "Active Fetch Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    loader.visibility = View.GONE
                    cardActive.visibility = View.GONE
                    selectionHeader.visibility = View.VISIBLE
                    selectionScroll.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showConfirmationSheet(id: Int, name: String, price: String, days: Int) {
        val sheet = MembershipSheet.newInstance(id, name, price, days)
        sheet.onSubscriptionCreated = {
            fetchActiveMembership(requireView())
            fetchHistory()
        }
        sheet.show(parentFragmentManager, "membership_sheet")
    }

    private fun fetchHistory() {
        val userId = com.example.horizonsystems.utils.GymManager.getUserId(requireContext())
        if (userId == -1) {
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(context)
                val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(context)
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.getMembershipHistory(userId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        fullHistoryList.clear()
                        fullHistoryList.addAll(response.body()!!)
                        applyFilter()
                    } else {
                        fullHistoryList.clear()
                        applyFilter()
                    }
                }
            } catch (e: Exception) {
                Log.e("MembershipFragment", "Fetch Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    fullHistoryList.clear()
                    applyFilter()
                }
            }
        }
    }

    private fun updateFilter(filter: String, btnAll: View, btnPending: View, btnApproved: View) {
        currentFilter = filter
        
        // UI Visual Update (Purple for Active, Semi-transparent for Inactive)
        val activeColor = android.graphics.Color.WHITE
        val inactiveColor = android.graphics.Color.parseColor("#94A3B8")
        val activeBg = android.graphics.Color.parseColor("#A855F7")
        val inactiveBg = android.graphics.Color.parseColor("#1AFFFFFF")

        (btnAll as? com.google.android.material.button.MaterialButton)?.let {
            it.setTextColor(if (filter == "ALL") activeColor else inactiveColor)
            it.backgroundTintList = android.content.res.ColorStateList.valueOf(if (filter == "ALL") activeBg else inactiveBg)
        }
        (btnPending as? com.google.android.material.button.MaterialButton)?.let {
            it.setTextColor(if (filter == "Pending") activeColor else inactiveColor)
            it.backgroundTintList = android.content.res.ColorStateList.valueOf(if (filter == "Pending") activeBg else inactiveBg)
        }
        (btnApproved as? com.google.android.material.button.MaterialButton)?.let {
            it.setTextColor(if (filter == "Approved") activeColor else inactiveColor)
            it.backgroundTintList = android.content.res.ColorStateList.valueOf(if (filter == "Approved") activeBg else inactiveBg)
        }

        applyFilter()
    }

    private fun applyFilter() {
        historyList.clear()
        if (currentFilter == "ALL") {
            historyList.addAll(fullHistoryList)
        } else {
            historyList.addAll(fullHistoryList.filter { it.status == currentFilter })
        }
        adapter.notifyDataSetChanged()
    }

}
