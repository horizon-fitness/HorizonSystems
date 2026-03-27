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
    private lateinit var adapter: TransactionAdapter
    private val allTransactions = listOf(
        Transaction("Mar 21, 2024", "10:00 AM", "Unlimited Gym Use", "GCASH-123", "500.00", "Approved"),
        Transaction("Mar 20, 2024", "09:30 AM", "Personal Training", "CASH-456", "1500.00", "Approved"),
        Transaction("Mar 19, 2024", "02:00 PM", "Yoga Session", "PAYMAYA-789", "300.00", "Approved"),
        Transaction("Mar 18, 2024", "11:00 AM", "Boxing Class", "GCASH-001", "800.00", "Approved"),
        Transaction("Mar 17, 2024", "04:30 PM", "Unlimited Gym Use", "CASH-002", "500.00", "Approved"),
        Transaction("Mar 16, 2024", "10:00 AM", "Zumba Class", "GCASH-003", "400.00", "Approved"),
        Transaction("Mar 15, 2024", "01:00 PM", "Personal Training", "CASH-004", "1500.00", "Approved"),
        Transaction("Mar 14, 2024", "09:00 AM", "Unlimited Gym Use", "PAYMAYA-005", "500.00", "Approved"),
        Transaction("Mar 13, 2024", "10:00 AM", "Crossfit", "GCASH-006", "1200.00", "Approved"),
        Transaction("Mar 12, 2024", "03:00 PM", "Unlimited Gym Use", "CASH-007", "500.00", "Approved"),
        Transaction("Mar 11, 2024", "05:00 PM", "Personal Training", "GCASH-008", "1500.00", "Approved"),
        Transaction("Mar 10, 2024", "08:00 AM", "Unlimited Gym Use", "CASH-009", "500.00", "Approved"),
        Transaction("Mar 09, 2024", "11:00 AM", "Yoga Session", "PAYMAYA-010", "300.00", "Approved"),
        Transaction("Mar 08, 2024", "10:00 AM", "Boxing Class", "GCASH-011", "800.00", "Approved"),
        Transaction("Mar 07, 2024", "04:30 PM", "Unlimited Gym Use", "CASH-012", "500.00", "Approved")
    )

    private var currentPage = 1
    private val itemsPerPage = 10
    private var currentFilter = "RECENT"
    private var filteredList = allTransactions

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

        // Initial setup
        applyFilterAndPage(view)

        return view
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

    private fun applyFilterAndPage(root: View) {
        filteredList = if (currentFilter == "RECENT") {
            allTransactions.take(5)
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
