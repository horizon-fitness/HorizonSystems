package com.example.horizonsystems

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.horizonsystems.utils.ThemeUtils
import com.example.horizonsystems.utils.GymManager
import java.text.SimpleDateFormat
import java.util.Locale

class PaymentFragment : Fragment(), PaymentFilterSheet.FilterListener, PaymentSortSheet.SortListener {
    private lateinit var adapter: TransactionAdapter
    private var currentPage = 1
    private val itemsPerPage = 10
    private var currentSort = "NEWEST"   // NEWEST, OLDEST, HIGH_PRICE, LOW_PRICE
    private var currentStatus = "ALL"    // ALL, PENDING, COMPLETED
    private var currentType = "ALL"      // ALL, MEMBERSHIP, BOOKING
    private var startDate: Long? = null
    private var endDate: Long? = null
    private var searchQuery = ""
    
    private var allTransactions = mutableListOf<Transaction>()
    private var filteredList = listOf<Transaction>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = try {
            inflater.inflate(R.layout.fragment_payment, container, false)
        } catch (e: Exception) {
            Log.e("PaymentFragment", "Inflation error: ${e.message}")
            null
        } ?: return null

        val rvTransactions = view.findViewById<RecyclerView>(R.id.rvTransactions)
        val btnPrev = view.findViewById<View>(R.id.btn_prev)
        val btnNext = view.findViewById<View>(R.id.btn_next)
        val btnFilter = view.findViewById<View>(R.id.btn_filter_modal)
        val btnSort = view.findViewById<View>(R.id.btn_sort_modal)
        val etSearch = view.findViewById<android.widget.EditText>(R.id.etSearchTransactions)

        adapter = TransactionAdapter(emptyList())
        rvTransactions?.let {
            it.layoutManager = LinearLayoutManager(context ?: return@let)
            it.adapter = adapter
        }

        // --- Search Logic ---
        etSearch?.addTextChangedListener(object: android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim()
                currentPage = 1
                applyFilterAndPage(view)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // --- Dual Sheet Triggers ---
        btnFilter?.setOnClickListener {
            val sheet = PaymentFilterSheet()
            sheet.setParams(currentStatus, currentType, startDate, endDate, this)
            sheet.show(childFragmentManager, "FILTER_SHEET")
        }

        btnSort?.setOnClickListener {
            val sheet = PaymentSortSheet()
            sheet.setParams(currentSort, this)
            sheet.show(childFragmentManager, "SORT_SHEET")
        }

        // --- Pagination ---
        btnPrev?.setOnClickListener { if (currentPage > 1) { currentPage--; applyFilterAndPage(view) } }
        btnNext?.setOnClickListener {
            val totalPages = Math.max(1, Math.ceil(filteredList.size.toDouble() / itemsPerPage).toInt())
            if (currentPage < totalPages) { currentPage++; applyFilterAndPage(view) }
        }

        fetchTransactions(view)
        ThemeUtils.applyThemeToView(view)
        return view
    }

    override fun onFiltersApplied(status: String, type: String, start: Long?, end: Long?) {
        this.currentStatus = status
        this.currentType = type
        this.startDate = start
        this.endDate = end
        this.currentPage = 1
        view?.let { applyFilterAndPage(it) }
    }

    override fun onSortSelected(sort: String) {
        this.currentSort = sort
        this.currentPage = 1
        view?.let { applyFilterAndPage(it) }
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
                val themeColor = android.graphics.Color.parseColor(themeColorStr)
                view.findViewById<TextView>(R.id.tv_payment_theme_subtitle)?.setTextColor(themeColor)
                
                // Pagination Tints
                view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_prev)?.iconTint = android.content.res.ColorStateList.valueOf(themeColor)
                view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_next)?.iconTint = android.content.res.ColorStateList.valueOf(themeColor)
                
                // Header Icon Tints
                view.findViewById<View>(R.id.btn_sort_modal)?.let { it.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor).withAlpha(15) }
                view.findViewById<View>(R.id.btn_filter_modal)?.let { it.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor).withAlpha(15) }
                
                // Search Bar Border Tint
                view.findViewById<View>(R.id.etSearchTransactions)?.parent?.let { container ->
                    if (container is View) {
                        val shape = android.graphics.drawable.GradientDrawable()
                        shape.setColor(android.graphics.Color.parseColor("#0DFFFFFF"))
                        shape.setStroke(1, themeColor.withAlpha(50))
                        shape.cornerRadius = (14 * ctx.resources.displayMetrics.density)
                        container.background = shape
                    }
                }
            } catch (e: Exception) { Log.e("PaymentFragment", "Branding Error: ${e.message}") }
        }
    }

    private fun Int.withAlpha(alpha: Int): Int {
        return (this and 0x00FFFFFF) or (alpha shl 24)
    }

    private fun fetchTransactions(root: View) {
        val ctx = context ?: return
        val userId = GymManager.getUserId(ctx)
        if (userId == -1) { applyFilterAndPage(root); return }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cookie = GymManager.getBypassCookie(ctx)
                val ua = GymManager.getBypassUA(ctx)
                val api = com.example.horizonsystems.network.RetrofitClient.getApi(cookie, ua)
                val response = api.getMembershipHistory(userId, showAll = 1)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        allTransactions.clear()
                        allTransactions.addAll(response.body()!!)
                        applyFilterAndPage(root)
                    } else { applyFilterAndPage(root) }
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { applyFilterAndPage(root) } }
        }
    }

    private fun applyFilterAndPage(root: View) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        // 1. Combine Filtering & Searching
        var baseList = allTransactions.filter { txn ->
            val statusMatch = when (currentStatus) {
                "PENDING" -> txn.status.contains("Pending", ignoreCase = true)
                "COMPLETED" -> txn.status.contains("Approved", ignoreCase = true) || txn.status.contains("Paid", ignoreCase = true) || txn.status.contains("Completed", ignoreCase = true)
                else -> true
            }

            val typeMatch = when (currentType) {
                "MEMBERSHIP" -> txn.service.contains("Plan", ignoreCase = true) || txn.service.contains("Month", ignoreCase = true) || txn.service.contains("Subscription", ignoreCase = true)
                "BOOKING" -> !txn.service.contains("Plan", ignoreCase = true) && !txn.service.contains("Month", ignoreCase = true) && !txn.service.contains("Subscription", ignoreCase = true)
                else -> true
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

            statusMatch && typeMatch && dateMatch && searchMatch
        }

        // 2. Sort Logic (Enhanced)
        baseList = when(currentSort) {
            "OLDEST" -> baseList.sortedBy { try { sdf.parse(it.date)?.time ?: 0L } catch(e: Exception) { 0L } }
            "HIGH_PRICE" -> baseList.sortedByDescending { it.amount.replace(",", "").replace("₱", "").toDoubleOrNull() ?: 0.0 }
            "LOW_PRICE" -> baseList.sortedBy { it.amount.replace(",", "").replace("₱", "").toDoubleOrNull() ?: 0.0 }
            else -> baseList.sortedByDescending { try { sdf.parse(it.date)?.time ?: 0L } catch(e: Exception) { 0L } } // NEWEST
        }

        filteredList = baseList

        val totalPages = Math.max(1, Math.ceil(filteredList.size.toDouble() / itemsPerPage).toInt())
        if (currentPage > totalPages) currentPage = totalPages
        if (currentPage < 1) currentPage = 1

        val startIndex = (currentPage - 1) * itemsPerPage
        val pageItems = if (filteredList.isEmpty()) emptyList() else filteredList.subList(startIndex, Math.min(startIndex + itemsPerPage, filteredList.size))
        
        if (::adapter.isInitialized) adapter.updateTransactions(pageItems)

        val hasActiveFilter = currentStatus != "ALL" || currentType != "ALL" || startDate != null || searchQuery.isNotEmpty()
        val emptyMsg = if (hasActiveFilter) "No results for your filters." else "No transactions found."
        root.findViewById<TextView>(R.id.emptyState)?.text = emptyMsg
        root.findViewById<View>(R.id.emptyState)?.visibility = if (pageItems.isEmpty()) View.VISIBLE else View.GONE
        root.findViewById<View>(R.id.rvTransactions)?.visibility = if (pageItems.isEmpty()) View.GONE else View.VISIBLE
        root.findViewById<View>(R.id.pagination_container)?.visibility = if (totalPages > 1) View.VISIBLE else View.GONE
        root.findViewById<TextView>(R.id.tv_page_number)?.text = "$currentPage/$totalPages"
        root.findViewById<View>(R.id.btn_prev)?.isEnabled = currentPage > 1
        root.findViewById<View>(R.id.btn_next)?.isEnabled = currentPage < totalPages
    }
}
