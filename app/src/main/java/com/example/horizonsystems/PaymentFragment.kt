package com.example.horizonsystems

import android.os.Bundle
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

class PaymentFragment : Fragment() {
    private lateinit var adapter: TransactionAdapter
    private var currentPage = 1
    private val itemsPerPage = 8
    private var currentFilter = "RECENT"
    private var allTransactions = mutableListOf<Transaction>()
    private var filteredList = listOf<Transaction>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_payment, container, false)

        val rvTransactions = view.findViewById<RecyclerView>(R.id.rvTransactions)
        val emptyState = view.findViewById<TextView>(R.id.emptyState)
        val paginationContainer = view.findViewById<View>(R.id.pagination_container)
        val btnPrev = view.findViewById<View>(R.id.btn_prev)
        val btnNext = view.findViewById<View>(R.id.btn_next)
        val tvPageNumber = view.findViewById<TextView>(R.id.tv_page_number)

        rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter(emptyList())
        rvTransactions.adapter = adapter

        // Filter Buttons
        val btnRecent = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filter_recent)
        val btnHistory = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filter_history)

        btnRecent.setOnClickListener {
            currentFilter = "RECENT"
            updateFilterButtons(btnRecent, btnHistory)
            applyFilterAndPage(view)
        }

        btnHistory.setOnClickListener {
            currentFilter = "HISTORY"
            updateFilterButtons(btnHistory, btnRecent)
            applyFilterAndPage(view)
        }

        // Pagination Buttons
        btnPrev.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                applyFilterAndPage(view)
            }
        }

        btnNext.setOnClickListener {
            val totalPages = Math.ceil(filteredList.size.toDouble() / itemsPerPage).toInt()
            if (currentPage < totalPages) {
                currentPage++
                applyFilterAndPage(view)
            }
        }

        // Initial fetch
        fetchTransactions(view)

        return view
    }

    override fun onResume() {
        super.onResume()
        view?.let { fetchTransactions(it) }
    }

    private fun updateFilterButtons(active: com.google.android.material.button.MaterialButton, inactive: com.google.android.material.button.MaterialButton) {
        active.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#A855F7")))
        active.setTextColor(android.graphics.Color.WHITE)
        active.setStrokeWidth(0)

        inactive.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1A1A1A")))
        inactive.setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
        inactive.setStrokeWidth(1)
        inactive.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#333333")))
    }

    private fun fetchTransactions(root: View) {
        val userId = com.example.horizonsystems.utils.GymManager.getUserId(requireContext())
        if (userId == -1) {
            applyFilterAndPage(root)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(context)
                val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(context)
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

        val totalPages = Math.ceil(filteredList.size.toDouble() / itemsPerPage).toInt()
        if (currentPage > totalPages && totalPages > 0) currentPage = totalPages
        if (currentPage < 1) currentPage = 1

        val startIndex = (currentPage - 1) * itemsPerPage
        val endIndex = Math.min(startIndex + itemsPerPage, filteredList.size)
        
        val pageItems = if (filteredList.isEmpty()) emptyList() else filteredList.subList(startIndex, endIndex)
        adapter.updateTransactions(pageItems)

        // Visibility & Text
        root.findViewById<View>(R.id.emptyState).visibility = if (pageItems.isEmpty()) View.VISIBLE else View.GONE
        root.findViewById<View>(R.id.rvTransactions).visibility = if (pageItems.isEmpty()) View.GONE else View.VISIBLE
        
        val paginationContainer = root.findViewById<View>(R.id.pagination_container)
        paginationContainer.visibility = if (currentFilter == "HISTORY" && totalPages > 1) View.VISIBLE else View.GONE
        
        root.findViewById<TextView>(R.id.tv_page_number).text = "$currentPage/$totalPages"
        
        root.findViewById<View>(R.id.btn_prev).isEnabled = currentPage > 1
        root.findViewById<View>(R.id.btn_next).isEnabled = currentPage < totalPages
    }
}
