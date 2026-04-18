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

class PaymentFragment : Fragment() {
    private lateinit var adapter: TransactionAdapter
    private var currentPage = 1
    private val itemsPerPage = 8
    private var currentFilter = "RECENT"
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
        val emptyState = view.findViewById<TextView>(R.id.emptyState)
        val paginationContainer = view.findViewById<View>(R.id.pagination_container)
        val btnPrev = view.findViewById<View>(R.id.btn_prev)
        val btnNext = view.findViewById<View>(R.id.btn_next)
        val tvPageNumber = view.findViewById<TextView>(R.id.tv_page_number)

        adapter = TransactionAdapter(emptyList())
        rvTransactions?.let {
            it.layoutManager = LinearLayoutManager(context ?: return@let)
            it.adapter = adapter
        }

        // Filter Buttons
        val btnRecent = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filter_recent)
        val btnHistory = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filter_history)

        btnRecent?.setOnClickListener {
            currentFilter = "RECENT"
            updateFilterButtons(btnRecent, btnHistory)
            applyFilterAndPage(view)
        }

        btnHistory?.setOnClickListener {
            currentFilter = "HISTORY"
            updateFilterButtons(btnHistory, btnRecent)
            applyFilterAndPage(view)
        }

        // Pagination Buttons
        btnPrev?.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                applyFilterAndPage(view)
            }
        }

        btnNext?.setOnClickListener {
            val totalPages = Math.ceil(filteredList.size.toDouble() / itemsPerPage).toInt()
            if (currentPage < totalPages) {
                currentPage++
                applyFilterAndPage(view)
            }
        }

        // Initial fetch
        fetchTransactions(view)

        ThemeUtils.applyThemeToView(view)

        return view
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
                
                // 1. Title Accent
                view.findViewById<TextView>(R.id.tv_payment_theme_subtitle)?.setTextColor(themeColor)
                
                // 2. Pagination Controls
                view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_prev)?.iconTint = 
                    android.content.res.ColorStateList.valueOf(themeColor)
                view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_next)?.iconTint = 
                    android.content.res.ColorStateList.valueOf(themeColor)
                
                // 3. Initial filter state
                val btnRecent = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filter_recent)
                val btnHistory = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filter_history)
                updateFilterButtons(btnRecent, btnHistory)
            } catch (e: Exception) {}
        }
    }

    private fun updateFilterButtons(active: com.google.android.material.button.MaterialButton?, inactive: com.google.android.material.button.MaterialButton?) {
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        val themeColor = try {
            if (!themeColorStr.isNullOrEmpty()) android.graphics.Color.parseColor(themeColorStr) else android.graphics.Color.parseColor("#A855F7")
        } catch (e: Exception) { android.graphics.Color.parseColor("#A855F7") }
        
        // Active Style (Glass Glow)
        active?.let {
            it.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor).withAlpha(40)
            it.setTextColor(themeColor)
            it.setStrokeColor(android.content.res.ColorStateList.valueOf(themeColor))
            it.setStrokeWidth(3)
            it.alpha = 1.0f
        }

        // Inactive Style (Clean Transparency)
        inactive?.let {
            it.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#0DFFFFFF"))
            it.setTextColor(android.graphics.Color.WHITE)
            it.setStrokeWidth(0)
            it.alpha = 0.5f
        }
    }

    private fun fetchTransactions(root: View) {
        val ctx = context ?: return
        val userId = com.example.horizonsystems.utils.GymManager.getUserId(ctx)
        if (userId == -1) {
            applyFilterAndPage(root)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(ctx)
                val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(ctx)
                val api = com.example.horizonsystems.network.RetrofitClient.getApi(cookie, ua)
                val response = api.getMembershipHistory(userId, showAll = 1)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        allTransactions.clear()
                        allTransactions.addAll(response.body()!!)
                        applyFilterAndPage(root)
                    } else {
                        applyFilterAndPage(root)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    applyFilterAndPage(root)
                }
            }
        }
    }

    private fun applyFilterAndPage(root: View) {
        filteredList = if (currentFilter == "RECENT") {
            allTransactions.take(8)
        } else {
            allTransactions
        }

        val totalPages = Math.max(1, Math.ceil(filteredList.size.toDouble() / itemsPerPage).toInt())
        if (currentPage > totalPages && totalPages > 0) currentPage = totalPages
        if (currentPage < 1) currentPage = 1

        val startIndex = (currentPage - 1) * itemsPerPage
        val endIndex = Math.min(startIndex + itemsPerPage, filteredList.size)
        
        val pageItems = if (filteredList.isEmpty()) emptyList() else filteredList.subList(startIndex, endIndex)
        if (::adapter.isInitialized) {
            adapter.updateTransactions(pageItems)
        }

        // Visibility & Text (Safe Access)
        root.findViewById<View>(R.id.emptyState)?.visibility = if (pageItems.isEmpty()) View.VISIBLE else View.GONE
        root.findViewById<View>(R.id.rvTransactions)?.visibility = if (pageItems.isEmpty()) View.GONE else View.VISIBLE
        
        root.findViewById<View>(R.id.pagination_container)?.let { container ->
            container.visibility = if (currentFilter == "HISTORY" && totalPages > 1) View.VISIBLE else View.GONE
        }
        
        root.findViewById<TextView>(R.id.tv_page_number)?.text = "$currentPage/$totalPages"
        
        root.findViewById<View>(R.id.btn_prev)?.isEnabled = currentPage > 1
        root.findViewById<View>(R.id.btn_next)?.isEnabled = currentPage < totalPages
    }
}

