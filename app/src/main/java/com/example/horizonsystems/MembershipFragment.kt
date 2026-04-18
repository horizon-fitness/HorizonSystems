package com.example.horizonsystems

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.TextView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.Transaction
import com.example.horizonsystems.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.horizonsystems.utils.ThemeUtils
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.card.MaterialCardView
import android.graphics.Color
import android.content.res.ColorStateList

class MembershipFragment : Fragment() {
    private lateinit var adapter: TransactionAdapter
    private val historyList = mutableListOf<Transaction>()
    private val fullHistoryList = mutableListOf<Transaction>()
    private var currentFilter = "ALL"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = try {
            inflater.inflate(R.layout.fragment_membership, container, false)
        } catch (e: Exception) {
            Log.e("MembershipFragment", "Inflation error: ${e.message}")
            null
        } ?: return null

        val rvMembershipHistory = view.findViewById<RecyclerView>(R.id.rvMembershipHistory)
        adapter = TransactionAdapter(historyList)
        rvMembershipHistory?.let {
            it.layoutManager = LinearLayoutManager(context ?: return@let)
            it.adapter = adapter
        }

        val rvPlanSelection = view.findViewById<RecyclerView>(R.id.rvPlanSelection)
        rvPlanSelection?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // Filter Buttons
        val btnAll = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFilterAll)
        val btnPending = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFilterPending)
        val btnApproved = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFilterApproved)

        btnAll?.setOnClickListener { updateFilter("ALL", btnAll, btnPending, btnApproved) }
        btnPending?.setOnClickListener { updateFilter("Pending", btnAll, btnPending, btnApproved) }
        btnApproved?.setOnClickListener { updateFilter("Approved", btnAll, btnPending, btnApproved) }

        // Fetch dynamic plans
        fetchMembershipPlans(view)

        // Fetch Initial State
        fetchData(view)

        ThemeUtils.applyThemeToView(view)

        return view
    }

    private var hasActivePlan = false

    private fun fetchData(root: View) {
        val ctx = context ?: return
        val userId = com.example.horizonsystems.utils.GymManager.getUserId(ctx)
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(ctx)
                val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(ctx)
                val api = RetrofitClient.getApi(cookie, ua)
                
                // 1. Fetch Latest Branding (Hot Refresh)
                val slug = com.example.horizonsystems.utils.GymManager.getGymSlug(ctx)
                val tenantResponse = api.getTenantInfo(slug)
                
                // 2. Fetch Active State
                val activeResponse = api.getActiveMembership(userId)
                
                withContext(Dispatchers.Main) {
                    // Update branding in background so it's ready for applyBranding
                    if (tenantResponse.isSuccessful) {
                        val tenant = tenantResponse.body()
                        com.example.horizonsystems.utils.GymManager.updateBranding(
                            ctx,
                            tenant?.themeColor,
                            tenant?.bgColor,
                            tenant?.cardColor,
                            tenant?.autoCardTheme
                        )
                    }

                    val active = activeResponse.body()
                    hasActivePlan = activeResponse.isSuccessful && active?.success == true && (active.subscriptionStatus == "Active" || active.subscriptionStatus == "Pending Approval")
                    
                    applyBranding(root) // Recalculate and apply colors immediately
                    updateActiveCardUI(root, active)
                    fetchMembershipPlans(root)
                    fetchHistory()
                }
            } catch (e: Exception) {
                Log.e("MembershipFragment", "Fetch Error: ${e.message}")
            }
        }
    }

    private fun updateActiveCardUI(root: View, active: com.example.horizonsystems.models.ActiveMembershipResponse?) {
        val loader = root.findViewById<View>(R.id.membershipLoading)
        val cardActive = root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardActiveMembership)
        val selectionHeader = root.findViewById<View>(R.id.layoutPlanSelectionHeader)
        val rvPlanSelection = root.findViewById<View>(R.id.rvPlanSelection)

        loader?.visibility = View.GONE
        
        if (active != null && active.success == true) {
            root.findViewById<TextView>(R.id.tvActivePlanName)?.text = active.planName ?: "Plan"
            val tvStatus = root.findViewById<TextView>(R.id.tvActivePlanStatus)
            val tvDuration = root.findViewById<TextView>(R.id.tvActivePlanDuration)
            val tvRemaining = root.findViewById<TextView>(R.id.tvDaysRemaining)
            
            if (active.subscriptionStatus == "Pending Approval") {
                tvStatus?.text = "(Pending Approval)"
                tvStatus?.visibility = View.VISIBLE
                tvDuration?.text = "Awaiting Staff Verification"
                tvRemaining?.text = "Payment verified by PayMongo"
            } else {
                tvStatus?.visibility = View.GONE
                tvDuration?.text = "Until: ${active.formattedEnd ?: "N/A"}"
                tvRemaining?.text = "${active.daysRemaining ?: 0} Days Remaining"
            }

            cardActive?.visibility = View.VISIBLE
            
            // Apply Sync Card Color
            val ctx = root.context
            val themeColorStr = GymManager.getThemeColor(ctx)
            val cardColorStr = GymManager.getCardColor(ctx)
            val isAutoCard = GymManager.getAutoCardTheme(ctx) == "1"
            
            if (!themeColorStr.isNullOrEmpty()) {
                val themeColor = Color.parseColor(themeColorStr)
                val cardColor = if (isAutoCard) {
                    val r = Color.red(themeColor)
                    val g = Color.green(themeColor)
                    val b = Color.blue(themeColor)
                    Color.argb(13, r, g, b)
                } else {
                    try { Color.parseColor(cardColorStr) } catch(e: Exception) { Color.parseColor("#0D0D10") }
                }
                cardActive?.setCardBackgroundColor(ColorStateList.valueOf(cardColor))
            }
        } else {
            cardActive?.visibility = View.GONE
        }

        // Always show these now
        selectionHeader?.visibility = View.VISIBLE
        rvPlanSelection?.visibility = View.VISIBLE
    }

    private fun fetchMembershipPlans(root: View) {
        val ctx = context ?: return
        val rvPlanSelection = root.findViewById<RecyclerView>(R.id.rvPlanSelection) ?: return
        val tenantId = GymManager.getTenantId(ctx)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cookie = GymManager.getBypassCookie(ctx)
                val ua = GymManager.getBypassUA(ctx)
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.getMembershipPlans(tenantId)
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val plans = response.body()!!
                        val planAdapter = PlanAdapter(plans, !hasActivePlan) { plan ->
                            showConfirmationSheet(plan.id, plan.name, plan.price, plan.durationDays)
                        }
                        rvPlanSelection.adapter = planAdapter
                    }
                }
            } catch (e: Exception) {
                Log.e("MembershipFragment", "Plans Fetch Error: ${e.message}")
            }
        }
    }

    private fun showConfirmationSheet(id: Int, name: String, price: Double, days: Int) {
        val sheet = MembershipSheet.newInstance(id, name, price, days)
        sheet.onSubscriptionCreated = {
            view?.let { fetchData(it) }
        }
        sheet.show(parentFragmentManager, "membership_sheet")
    }

    private fun fetchHistory() {
        val ctx = context ?: return
        val userId = com.example.horizonsystems.utils.GymManager.getUserId(ctx)
        if (userId == -1) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(ctx)
                val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(ctx)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBranding(view)
    }

    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        if (!themeColorStr.isNullOrEmpty()) {
            try {
                val themeColor = Color.parseColor(themeColorStr)
                
                // 1. Title Accent
                view.findViewById<TextView>(R.id.tv_membership_theme_subtitle)?.setTextColor(themeColor)
                
                // 2. Loading Indicator
                view.findViewById<ProgressBar>(R.id.membershipLoading)?.indeterminateTintList = 
                    ColorStateList.valueOf(themeColor)
                
                // 3. Days Remaining Accent & Active Card Stroke/Surface
                view.findViewById<TextView>(R.id.tvDaysRemaining)?.setTextColor(themeColor)
                val cardActive = view.findViewById<MaterialCardView>(R.id.cardActiveMembership)
                cardActive?.setStrokeColor(themeColor)
                
                // Active Card Surface Sync
                val cardColorStr = GymManager.getCardColor(ctx)
                val isAutoCard = GymManager.getAutoCardTheme(ctx) == "1"
                val cardColor = if (isAutoCard) {
                    val r = Color.red(themeColor)
                    val g = Color.green(themeColor)
                    val b = Color.blue(themeColor)
                    Color.argb(13, r, g, b)
                } else {
                    try { Color.parseColor(cardColorStr) } catch(e: Exception) { Color.parseColor("#0D0D10") }
                }
                cardActive?.setCardBackgroundColor(ColorStateList.valueOf(cardColor))

                // 4. Initial filter state
                val btnAll = view.findViewById<View>(R.id.btnFilterAll)
                val btnPending = view.findViewById<View>(R.id.btnFilterPending)
                val btnApproved = view.findViewById<View>(R.id.btnFilterApproved)
                updateFilter("ALL", btnAll, btnPending, btnApproved)
            } catch (e: Exception) {
                Log.e("MembershipFragment", "Branding Error: ${e.message}")
            }
        }
    }

    private fun updateFilter(filter: String, btnAll: View?, btnPending: View?, btnApproved: View?) {
        currentFilter = filter
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        val themeColor = try {
            if (!themeColorStr.isNullOrEmpty()) Color.parseColor(themeColorStr) else Color.parseColor("#A855F7")
        } catch (e: Exception) { Color.parseColor("#A855F7") }
        
        fun styleButton(btn: View?, isActive: Boolean) {
            (btn as? com.google.android.material.button.MaterialButton)?.let {
                if (isActive) {
                    it.backgroundTintList = ColorStateList.valueOf(themeColor)
                    it.setTextColor(Color.WHITE)
                    it.alpha = 1.0f
                } else {
                    it.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1AFFFFFF"))
                    it.setTextColor(Color.parseColor("#94A3B8"))
                    it.alpha = 1.0f
                }
            }
        }

        styleButton(btnAll, filter == "ALL")
        styleButton(btnPending, filter == "Pending")
        styleButton(btnApproved, filter == "Approved")

        applyFilter()
    }

    private fun applyFilter() {
        historyList.clear()
        if (currentFilter == "ALL") {
            historyList.addAll(fullHistoryList)
        } else {
            historyList.addAll(fullHistoryList.filter { it.status == currentFilter })
        }
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }
}
