package com.example.horizonsystems

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.TrainingLog
import com.example.horizonsystems.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.horizonsystems.utils.ThemeUtils
import com.example.horizonsystems.utils.GymManager

class BookingFragment : Fragment() {
    private lateinit var adapter: TrainingLogAdapter
    private val allLogs = mutableListOf<TrainingLog>()

    private var currentPage = 1
    private val itemsPerPage = 5
    private var currentFilter = "ALL"
    private var filteredList: List<TrainingLog> = allLogs

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return try {
            inflater.inflate(R.layout.fragment_booking, container, false)
        } catch (e: Exception) {
            Log.e("BookingFragment", "Inflation Error: ${e.message}")
            try {
                Toast.makeText(context, "Layout Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } catch (te: Exception) {}
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvTrainingLogs = view.findViewById<RecyclerView>(R.id.rvTrainingLogs)
        val calendarView = view.findViewById<android.widget.CalendarView>(R.id.calendar_view)
        val dateDetailsCard = view.findViewById<View>(R.id.cv_date_details)
        val dateInfoText = view.findViewById<TextView>(R.id.tv_selected_date_info)
        val btnQuickBook = view.findViewById<View>(R.id.btn_quick_book)

        applyBranding(view)

        adapter = TrainingLogAdapter(emptyList())

        rvTrainingLogs?.let {
            it.layoutManager = LinearLayoutManager(context ?: return@let)
            it.adapter = adapter
        }

        // Calendar Selection
        calendarView?.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            val displayDate = "${month + 1}/$dayOfMonth/$year"
            dateDetailsCard?.visibility = View.VISIBLE
            
            val approvedSessions = allLogs.filter { 
                val status = it.status?.uppercase()
                it.date == selectedDate && (status == "APPROVED" || status == "CONFIRMED") 
            }
            
            if (approvedSessions.isEmpty()) {
                dateInfoText?.text = "No approved sessions for $displayDate"
            } else {
                val sb = StringBuilder("Approved for $displayDate:\n")
                approvedSessions.forEachIndexed { index, session ->
                    val timeFormatted = try {
                        val sessionTime = session.time ?: "00:00:00"
                        val sdf24 = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                        val sdf12 = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
                        val dateObj = sdf24.parse(sessionTime)
                        sdf12.format(dateObj!!)
                    } catch (e: Exception) {
                        (session.time ?: "00:00").substringBeforeLast(":") // Fallback
                    }
                    
                    sb.append("• ${session.service ?: "Service"} at $timeFormatted")
                    if (index < approvedSessions.size - 1) sb.append("\n")
                }
                dateInfoText?.text = sb.toString()
            }
        }

        // Action Buttons
        btnQuickBook?.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            val userId = com.example.horizonsystems.utils.GymManager.getUserId(ctx)
            if (userId == -1) return@setOnClickListener

            // Disable button briefly to prevent double clicks
            it.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(ctx)
                    val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(ctx)
                    val api = RetrofitClient.getApi(cookie, ua)
                    val response = api.getActiveMembership(userId)
                    
                    withContext(Dispatchers.Main) {
                        it.isEnabled = true
                        if (response.isSuccessful && response.body()?.success == true && response.body()?.subscriptionStatus == "Active") {
                            // User is an active member
                            val sheet = BookingSheet()
                            sheet.onBookingCreated = {
                                fetchBookings(view)
                            }
                            sheet.show(parentFragmentManager, "booking_sheet")
                        } else {
                            // Membership pending, expired, or non-existent
                            android.app.AlertDialog.Builder(ctx, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                                .setTitle("Membership Required")
                                .setMessage("You must have an Active Membership to book a session. Please ensure your membership is approved and not expired.")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        it.isEnabled = true
                        Toast.makeText(ctx, "Failed to verify membership status.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Filter Buttons
        val btnAll = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filter_all)
        val btnPending = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filter_pending)
        val btnApproved = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filter_approved)

        btnAll?.setOnClickListener { 
            currentFilter = "ALL"
            updateFilterButtons(btnAll, listOf(btnPending, btnApproved))
            applyPaginationAndRefresh(view)
        }
        btnPending?.setOnClickListener { 
            currentFilter = "PENDING"
            updateFilterButtons(btnPending, listOf(btnAll, btnApproved))
            applyPaginationAndRefresh(view)
        }
        btnApproved?.setOnClickListener { 
            currentFilter = "APPROVED"
            updateFilterButtons(btnApproved, listOf(btnAll, btnPending))
            applyPaginationAndRefresh(view)
        }

        // Pagination Buttons
        view.findViewById<View>(R.id.btn_prev_booking)?.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                applyPaginationAndRefresh(view)
            }
        }
        view.findViewById<View>(R.id.btn_next_booking)?.setOnClickListener {
            val totalPages = Math.ceil(filteredList.size.toDouble() / itemsPerPage).toInt()
            if (currentPage < totalPages) {
                currentPage++
                applyPaginationAndRefresh(view)
            }
        }

        // Initial setup
        if (btnAll != null) {
            updateFilterButtons(btnAll, listOf(btnPending, btnApproved).filterNotNull())
        }
        fetchBookings(view)
        
        ThemeUtils.applyThemeToView(view)
    }

    private fun fetchBookings(root: View) {
        val ctx = context ?: return
        val userId = com.example.horizonsystems.utils.GymManager.getUserId(ctx)
        val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(ctx)
        val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(ctx)

        if (userId == -1) {
            applyPaginationAndRefresh(root)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.getUserBookings(userId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val bookings = response.body()?.bookings
                        allLogs.clear()
                        if (bookings != null) {
                            allLogs.addAll(bookings)
                        }
                        applyPaginationAndRefresh(root)
                    } else {
                        allLogs.clear()
                        applyPaginationAndRefresh(root)
                    }
                }
            } catch (e: Exception) {
                Log.e("BookingFragment", "Fetch Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    allLogs.clear()
                    applyPaginationAndRefresh(root)
                }
            }
        }
    }


    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        if (!themeColorStr.isNullOrEmpty()) {
            try {
                val themeColor = android.graphics.Color.parseColor(themeColorStr)
                // Main Booking Button
                view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_quick_book)?.let {
                    it.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor)
                }
                // Accent Title
                view.findViewById<TextView>(R.id.tv_booking_theme_title)?.setTextColor(themeColor)
                
                // Date Details Card
                view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cv_date_details)?.setCardBackgroundColor(themeColor)
            } catch (e: Exception) {}
        }
    }

    private fun updateFilterButtons(active: com.google.android.material.button.MaterialButton?, inactives: List<com.google.android.material.button.MaterialButton>) {
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        val themeColor = try {
            if (!themeColorStr.isNullOrEmpty()) android.graphics.Color.parseColor(themeColorStr) else android.graphics.Color.parseColor("#A855F7")
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#A855F7")
        }
        
        active?.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor).withAlpha(40)
            setTextColor(themeColor)
            strokeColor = android.content.res.ColorStateList.valueOf(themeColor)
            strokeWidth = 2
            alpha = 1.0f
        }

        inactives.forEach { inactive ->
            inactive.apply {
                backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#0DFFFFFF"))
                setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                strokeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1AFFFFFF"))
                strokeWidth = 2
                alpha = 0.6f
            }
        }
    }

    private fun applyPaginationAndRefresh(root: View) {
        filteredList = if (currentFilter == "ALL") {
            allLogs
        } else if (currentFilter == "APPROVED") {
            allLogs.filter { 
                val status = it.status?.uppercase()
                status == "APPROVED" || status == "CONFIRMED" 
            }
        } else {
            allLogs.filter { it.status?.uppercase() == currentFilter }
        }
        
        val totalPages = Math.max(1, Math.ceil(filteredList.size.toDouble() / itemsPerPage).toInt())
        if (currentPage > totalPages) currentPage = totalPages
        if (currentPage < 1) currentPage = 1

        val startIndex = (currentPage - 1) * itemsPerPage
        val endIndex = Math.min(startIndex + itemsPerPage, filteredList.size)
        
        val pageItems = if (filteredList.isEmpty()) emptyList() else filteredList.subList(startIndex, endIndex)
        if (::adapter.isInitialized) {
            adapter.updateLogs(pageItems)
        }

        // Visibility & Text
        root.findViewById<View>(R.id.emptyStateContainer)?.visibility = if (pageItems.isEmpty()) View.VISIBLE else View.GONE
        root.findViewById<View>(R.id.rvTrainingLogs)?.visibility = if (pageItems.isEmpty()) View.GONE else View.VISIBLE
        
        root.findViewById<View>(R.id.pagination_container_booking)?.let { container ->
            container.visibility = if (totalPages > 1) View.VISIBLE else View.GONE
        }
        
        root.findViewById<TextView>(R.id.tv_page_number_booking)?.text = "$currentPage/$totalPages"
        
        root.findViewById<View>(R.id.btn_prev_booking)?.isEnabled = currentPage > 1
        root.findViewById<View>(R.id.btn_next_booking)?.isEnabled = currentPage < totalPages
    }
}

