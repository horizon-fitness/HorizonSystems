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
import java.util.*

class MembershipFragment : Fragment(), MembershipFilterSheet.FilterListener, PaymentSortSheet.SortListener {
    private lateinit var adapter: TransactionAdapter
    private val historyList = mutableListOf<Transaction>()
    private val fullHistoryList = mutableListOf<Transaction>()
    private var currentFilter = "ALL"
    private var currentSort = "NEWEST"
    private var startDate: Long? = null
    private var endDate: Long? = null
    private var searchQuery = ""

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
        androidx.recyclerview.widget.PagerSnapHelper().attachToRecyclerView(rvPlanSelection)

        // --- Labs Search Header Logic ---
        val etSearch = view.findViewById<android.widget.EditText>(R.id.etSearchHistory)
        val btnSort = view.findViewById<View>(R.id.btnSortHistory)
        val btnFilter = view.findViewById<View>(R.id.btnFilterHistory)

        etSearch?.addTextChangedListener(object: android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim()
                applyFilter()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnSort?.setOnClickListener {
            val sheet = PaymentSortSheet()
            sheet.setParams(currentSort, this)
            sheet.show(childFragmentManager, "SORT_SHEET")
        }

        btnFilter?.setOnClickListener {
            val sheet = MembershipFilterSheet()
            sheet.setParams(currentFilter, startDate, endDate, this)
            sheet.show(childFragmentManager, "FILTER_SHEET")
        }

        fetchData(view)
        ThemeUtils.applyThemeToView(view)
        return view
    }

    override fun onFiltersApplied(status: String, start: Long?, end: Long?) {
        this.currentFilter = status
        this.startDate = start
        this.endDate = end
        applyFilter()
    }

    override fun onSortSelected(sort: String) {
        this.currentSort = sort
        applyFilter()
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
                
                val tenantDeferred = async { api.getTenantInfo(slug) }
                val activeDeferred = async { api.getActiveMembership(userId) }
                val plansDeferred = async { api.getMembershipPlans(gymId) }
                
                val tenantResponse = tenantDeferred.await()
                val activeResponse = activeDeferred.await()
                val plansResponse = plansDeferred.await()
                
                withContext(Dispatchers.Main) {
                    if (tenantResponse.isSuccessful) {
                        val tenant = tenantResponse.body()
                        com.example.horizonsystems.utils.GymManager.updateBranding(
                            ctx, tenant?.themeColor, tenant?.iconColor, tenant?.textColor,
                            tenant?.bgColor, tenant?.cardColor, tenant?.autoCardTheme
                        )
                    }

                    val active = activeResponse.body()
                    hasActivePlan = activeResponse.isSuccessful && active?.success == true && (active.subscriptionStatus == "Active" || active.subscriptionStatus == "Pending Approval")
                    
                    applyBranding(root)
                    updateActiveCardUI(root, active)
                    
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
            } catch (e: Exception) { Log.e("MembershipFragment", "Fetch Error: ${e.message}") }
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
            root.findViewById<TextView>(R.id.tvActivePlanStart)?.text = active.formattedStart ?: "N/A"
            root.findViewById<TextView>(R.id.tvActivePlanDuration)?.text = active.formattedEnd ?: "N/A"
            root.findViewById<TextView>(R.id.tvActivePlanStatusBadge)?.text = if (active.subscriptionStatus == "Pending Approval") "PENDING" else "ACTIVE"
            cardActive?.visibility = View.VISIBLE
            
            val ctx = root.context
            val themeColorStr = GymManager.getThemeColor(ctx)
            if (!themeColorStr.isNullOrEmpty()) {
                val themeColor = Color.parseColor(themeColorStr)
                val isAutoCard = GymManager.getAutoCardTheme(ctx) == "1"
                val cardColor = if (isAutoCard) Color.argb(13, Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor))
                               else try { Color.parseColor(GymManager.getCardColor(ctx)) } catch(e: Exception) { Color.parseColor("#0D0D10") }
                cardActive?.setCardBackgroundColor(ColorStateList.valueOf(cardColor))
            }
        } else { cardActive?.visibility = View.GONE }

        selectionHeader?.visibility = View.VISIBLE
        rvPlanSelection?.visibility = View.VISIBLE
    }

    private fun showConfirmationSheet(id: Int, name: String, price: Double, days: Int) {
        val sheet = MembershipSheet.newInstance(id, name, price, days)
        sheet.onSubscriptionCreated = { view?.let { fetchData(it) } }
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
                    } else { fullHistoryList.clear(); applyFilter() }
                }
            } catch (e: Exception) {
                Log.e("MembershipFragment", "Fetch Error: ${e.message}")
                withContext(Dispatchers.Main) { fullHistoryList.clear(); applyFilter() }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBranding(view)

        parentFragmentManager.setFragmentResultListener("plan_selection", viewLifecycleOwner) { _, bundle ->
            val id = bundle.getInt("id")
            val name = bundle.getString("name") ?: ""
            val price = bundle.getDouble("price")
            val days = bundle.getInt("days")
            if (id != 0) showConfirmationSheet(id, name, price, days)
        }
    }

    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        val textColorStr = GymManager.getTextColor(ctx)
        val bgColorStr = GymManager.getBgColor(ctx)

        try {
            val themeColor = if (!themeColorStr.isNullOrEmpty()) Color.parseColor(themeColorStr) else Color.parseColor("#8c2bee")
            val textColor = if (!textColorStr.isNullOrEmpty()) Color.parseColor(textColorStr) else Color.parseColor("#D1D5DB")
            val bgColor = if (!bgColorStr.isNullOrEmpty()) Color.parseColor(bgColorStr) else Color.parseColor("#0a090d")

            view.setBackgroundColor(bgColor)
            view.findViewById<TextView>(R.id.tv_membership_theme_subtitle)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.tvHistoryHeaderPart2)?.setTextColor(themeColor)
            view.findViewById<ProgressBar>(R.id.membershipLoading)?.indeterminateTintList = ColorStateList.valueOf(themeColor)
            
            val isAutoCard = GymManager.getAutoCardTheme(ctx) == "1"
            val cardColor = if (isAutoCard) Color.argb(13, Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor))
                           else try { Color.parseColor(GymManager.getCardColor(ctx)) } catch(e: Exception) { Color.parseColor("#141216") }

            val cardActive = view.findViewById<MaterialCardView>(R.id.cardActiveMembership)
            cardActive?.setCardBackgroundColor(ColorStateList.valueOf(cardColor))
            cardActive?.setStrokeColor(Color.parseColor("#1AFFFFFF"))

            view.findViewById<TextView>(R.id.tvStatusLabel)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.tvActivePlanStatusBadge)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.tvActivePlanDuration)?.setTextColor(themeColor)
            
            view.findViewById<MaterialCardView>(R.id.cvActiveBadge)?.let { badge ->
                badge.setStrokeColor(themeColor)
                badge.setCardBackgroundColor(Color.argb(20, Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor)))
            }

            view.findViewById<TextView>(R.id.tvActivePlanName)?.setTextColor(textColor)
            view.findViewById<TextView>(R.id.tvActivePlanStart)?.setTextColor(textColor)

            // Header Icon Branding
            view.findViewById<View>(R.id.btnSortHistory)?.let { it.backgroundTintList = ColorStateList.valueOf(themeColor).withAlpha(15) }
            view.findViewById<View>(R.id.btnFilterHistory)?.let { it.backgroundTintList = ColorStateList.valueOf(themeColor).withAlpha(15) }
            
            view.findViewById<View>(R.id.etSearchHistory)?.parent?.let { container ->
                if (container is View) {
                    val shape = android.graphics.drawable.GradientDrawable()
                    shape.setColor(Color.parseColor("#0DFFFFFF"))
                    shape.setStroke(1, themeColor.withAlpha(50))
                    shape.cornerRadius = (14 * ctx.resources.displayMetrics.density)
                    container.background = shape
                }
            }
        } catch (e: Exception) { Log.e("MembershipFragment", "Branding Error: ${e.message}") }
    }

    private fun Int.withAlpha(alpha: Int): Int {
        return (this and 0x00FFFFFF) or (alpha shl 24)
    }

    private fun applyFilter() {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        val baseList = fullHistoryList.filter { txn ->
            val statusMatch = when (currentFilter) {
                "ALL" -> true
                else -> txn.status.contains(currentFilter, ignoreCase = true)
            }

            val dateMatch = if (startDate != null && endDate != null) {
                try {
                    val txnDate = sdf.parse(txn.date)?.time ?: 0L
                    txnDate in startDate!!..endDate!!
                } catch (e: Exception) { true }
            } else true

            val searchMatch = if (searchQuery.isNotEmpty()) {
                txn.service.contains(searchQuery, ignoreCase = true) || txn.reference.contains(searchQuery, ignoreCase = true)
            } else true

            statusMatch && dateMatch && searchMatch
        }

        val sortedList = when(currentSort) {
            "OLDEST" -> baseList.sortedBy { try { sdf.parse(it.date)?.time ?: 0L } catch(e: Exception) { 0L } }
            "HIGH_PRICE" -> baseList.sortedByDescending { it.amount.replace(",", "").replace("₱", "").toDoubleOrNull() ?: 0.0 }
            "LOW_PRICE" -> baseList.sortedBy { it.amount.replace(",", "").replace("₱", "").toDoubleOrNull() ?: 0.0 }
            else -> baseList.sortedByDescending { try { sdf.parse(it.date)?.time ?: 0L } catch(e: Exception) { 0L } } // NEWEST
        }

        historyList.clear()
        historyList.addAll(sortedList)
        if (::adapter.isInitialized) adapter.notifyDataSetChanged()
    }
}
