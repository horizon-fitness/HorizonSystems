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
import kotlinx.coroutines.async
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
        
        // Premium Snapping Experience
        androidx.recyclerview.widget.PagerSnapHelper().attachToRecyclerView(rvPlanSelection)

        // Filter Buttons
        val btnAll = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFilterAll)
        val btnPending = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFilterPending)
        val btnApproved = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFilterApproved)

        btnAll?.setOnClickListener { updateFilter("ALL", btnAll, btnPending, btnApproved) }
        btnPending?.setOnClickListener { updateFilter("Pending", btnAll, btnPending, btnApproved) }
        btnApproved?.setOnClickListener { updateFilter("Approved", btnAll, btnPending, btnApproved) }

        // Fetch Initial State
        fetchData(view)

        ThemeUtils.applyThemeToView(view)
        applyBranding(view)

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
                val gymId = com.example.horizonsystems.utils.GymManager.getTenantId(ctx)
                val slug = com.example.horizonsystems.utils.GymManager.getGymSlug(ctx)
                
                // 1. Fetch data in parallel for performance
                val tenantDeferred = async { api.getTenantInfo(slug) }
                val activeDeferred = async { api.getActiveMembership(userId) }
                val plansDeferred = async { api.getMembershipPlans(gymId) }
                
                val tenantResponse = tenantDeferred.await()
                val activeResponse = activeDeferred.await()
                val plansResponse = plansDeferred.await()
                
                withContext(Dispatchers.Main) {
                    // Update branding
                    if (tenantResponse.isSuccessful) {
                        val tenant = tenantResponse.body()
                        com.example.horizonsystems.utils.GymManager.updateBranding(
                            ctx,
                            tenant?.themeColor,
                            tenant?.iconColor,
                            tenant?.textColor,
                            tenant?.bgColor,
                            tenant?.cardColor,
                            tenant?.autoCardTheme
                        )
                    }

                    // Update Active Status
                    val active = activeResponse.body()
                    hasActivePlan = activeResponse.isSuccessful && active?.success == true && (active.subscriptionStatus == "Active" || active.subscriptionStatus == "Pending Approval")
                    
                    applyBranding(root)
                    updateActiveCardUI(root, active)
                    
                    // Update Plans after hasActivePlan is finalized
                    if (plansResponse.isSuccessful && plansResponse.body() != null) {
                        val plans = plansResponse.body()!!
                        val rvPlanSelection = root.findViewById<RecyclerView>(R.id.rvPlanSelection)
                        val planAdapter = PlanAdapter(plans, !hasActivePlan) { plan ->
                            showConfirmationSheet(plan.id, plan.name, plan.price, plan.durationDays)
                        }
                        rvPlanSelection?.adapter = planAdapter
                        rvPlanSelection?.visibility = View.VISIBLE
                    }
                    
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
            val tvStart = root.findViewById<TextView>(R.id.tvActivePlanStart)
            val tvEnd = root.findViewById<TextView>(R.id.tvActivePlanDuration)
            val tvBadgeStatus = root.findViewById<TextView>(R.id.tvActivePlanStatusBadge)
            
            tvStart?.text = active.formattedStart ?: "N/A"
            tvEnd?.text = active.formattedEnd ?: "N/A"
            
            if (active.subscriptionStatus == "Pending Approval") {
                tvBadgeStatus?.text = "PENDING"
            } else {
                tvBadgeStatus?.text = "ACTIVE"
            }

            cardActive?.visibility = View.VISIBLE
            
            // Apply Dynamic Card Background
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

        // Listen for plan selection from HomeFragment
        parentFragmentManager.setFragmentResultListener("plan_selection", viewLifecycleOwner) { _, bundle ->
            val id = bundle.getInt("id")
            val name = bundle.getString("name") ?: ""
            val price = bundle.getDouble("price")
            val days = bundle.getInt("days")
            
            if (id != 0) {
                showConfirmationSheet(id, name, price, days)
            }
        }
    }

    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        val textColorStr = GymManager.getTextColor(ctx)
        val bgColorStr = GymManager.getBgColor(ctx)
        val cardColorStr = GymManager.getCardColor(ctx)
        val isAutoCard = GymManager.getAutoCardTheme(ctx) == "1"

        try {
            val themeColor = if (!themeColorStr.isNullOrEmpty()) Color.parseColor(themeColorStr) else Color.parseColor("#8c2bee")
            val textColor = if (!textColorStr.isNullOrEmpty()) Color.parseColor(textColorStr) else Color.parseColor("#D1D5DB")
            val bgColor = if (!bgColorStr.isNullOrEmpty()) Color.parseColor(bgColorStr) else Color.parseColor("#0a090d")

            // 1. Fragment Background
            view.setBackgroundColor(bgColor)

            // 2. Horizontal Headers
            view.findViewById<TextView>(R.id.tv_membership_theme_subtitle)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.tvHistoryHeaderPart2)?.setTextColor(themeColor)
            
            // 3. Loading Indicator
            view.findViewById<ProgressBar>(R.id.membershipLoading)?.indeterminateTintList = 
                ColorStateList.valueOf(themeColor)
            
            // 4. Component Appearance (Dynamic Background Sync)
            val cardColor = if (isAutoCard) {
                val r = Color.red(themeColor)
                val g = Color.green(themeColor)
                val b = Color.blue(themeColor)
                Color.argb(13, r, g, b)
            } else {
                try { Color.parseColor(cardColorStr) } catch(e: Exception) { Color.parseColor("#141216") }
            }

            // 4a. Active Membership Card (Professional Reference Design)
            val cardActive = view.findViewById<MaterialCardView>(R.id.cardActiveMembership)
            cardActive?.setCardBackgroundColor(ColorStateList.valueOf(cardColor))
            cardActive?.setStrokeColor(Color.parseColor("#1AFFFFFF")) // Keep subtle border

            // Theme Accents (Matching Reference Image)
            view.findViewById<TextView>(R.id.tvStatusLabel)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.tvActivePlanStatusBadge)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.tvActivePlanDuration)?.setTextColor(themeColor)
            
            view.findViewById<MaterialCardView>(R.id.cvActiveBadge)?.let { badge ->
                badge.setStrokeColor(themeColor)
                // Subtle tinted background for the badge
                badge.setCardBackgroundColor(Color.argb(20, Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor)))
            }

            // Text Accents
            view.findViewById<TextView>(R.id.tvActivePlanName)?.setTextColor(textColor)
            view.findViewById<TextView>(R.id.tvActivePlanStart)?.setTextColor(textColor)
            
            // 5. Initial filter state
            val btnAll = view.findViewById<View>(R.id.btnFilterAll)
            val btnPending = view.findViewById<View>(R.id.btnFilterPending)
            val btnApproved = view.findViewById<View>(R.id.btnFilterApproved)
            updateFilter(currentFilter, btnAll, btnPending, btnApproved)
        } catch (e: Exception) {
            Log.e("MembershipFragment", "Branding Error: ${e.message}")
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
                    it.backgroundTintList = ColorStateList.valueOf(themeColor).withAlpha(30)
                    it.setTextColor(themeColor) // Match brand color for active text
                    it.alpha = 1.0f
                } else {
                    it.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                    it.setTextColor(Color.parseColor("#94A3B8")) // Muted secondary text
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
