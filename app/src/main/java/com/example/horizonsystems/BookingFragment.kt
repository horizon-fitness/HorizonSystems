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
        val btnQuickBook = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_quick_book)

        ThemeUtils.applyThemeToView(view)
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
        val textColorStr = GymManager.getTextColor(ctx)
        val bgColorStr = GymManager.getBgColor(ctx)

        try {
            val themeColor = if (!themeColorStr.isNullOrEmpty()) android.graphics.Color.parseColor(themeColorStr) else android.graphics.Color.parseColor("#8c2bee")
            val textColor = if (!textColorStr.isNullOrEmpty()) android.graphics.Color.parseColor(textColorStr) else android.graphics.Color.parseColor("#D1D5DB")
            val bgColor = if (!bgColorStr.isNullOrEmpty()) android.graphics.Color.parseColor(bgColorStr) else android.graphics.Color.parseColor("#0a090d")

            // 1. Fragment Background
            view.setBackgroundColor(bgColor)

            // 2. Buttons & Titles
            val outlinedButtons = listOfNotNull(
                view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_quick_book)
            )

            outlinedButtons.forEach { btn ->
                btn.strokeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1AFFFFFF"))
                btn.strokeWidth = (1 * ctx.resources.displayMetrics.density).toInt()
                btn.setTextColor(textColor) 
                btn.setIconTint(android.content.res.ColorStateList.valueOf(textColor))
                btn.rippleColor = android.content.res.ColorStateList.valueOf(themeColor and 0x33FFFFFF)
            }

            view.findViewById<TextView>(R.id.tv_booking_theme_title)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.tv_book_and_pay)?.setTextColor(textColor)
            view.findViewById<TextView>(R.id.tv_training_label)?.setTextColor(textColor)
            view.findViewById<TextView>(R.id.tv_upcoming_label)?.setTextColor(textColor)
            view.findViewById<TextView>(R.id.tv_page_number_booking)?.setTextColor(textColor)
            view.findViewById<TextView>(R.id.bookingEmptyState)?.setTextColor(textColor)
            
            // 3. Status Accents & Cards
            view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cv_date_details)?.let { card ->
                card.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1AFFFFFF")))
            }
            view.findViewById<TextView>(R.id.tv_selected_date_label)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.tv_upcoming_sessions_accent)?.setTextColor(themeColor)

            // 4. Pagination Tints
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_prev_booking)?.setIconTint(android.content.res.ColorStateList.valueOf(themeColor))
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_next_booking)?.setIconTint(android.content.res.ColorStateList.valueOf(themeColor))

            // 5. Sync Filter Tabs Branding
            val btnAll = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filter_all)
            val btnPending = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filter_pending)
            val btnApproved = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filter_approved)
            updateFilterButtons(
                when(currentFilter) {
                    "PENDING" -> btnPending
                    "APPROVED" -> btnApproved
                    else -> btnAll
                }, 
                listOfNotNull(btnAll, btnPending, btnApproved).filter { 
                    it.id != when(currentFilter) {
                        "PENDING" -> R.id.btn_filter_pending
                        "APPROVED" -> R.id.btn_filter_approved
                        else -> R.id.btn_filter_all
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("BookingFragment", "Branding Error: ${e.message}")
        }
    }

    private fun updateFilterButtons(active: com.google.android.material.button.MaterialButton?, inactives: List<com.google.android.material.button.MaterialButton>) {
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        val textColorStr = GymManager.getTextColor(ctx)
        
        val themeColor = try {
            if (!themeColorStr.isNullOrEmpty()) android.graphics.Color.parseColor(themeColorStr) else android.graphics.Color.parseColor("#8c2bee")
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#8c2bee")
        }

        val textColor = try {
            if (!textColorStr.isNullOrEmpty()) android.graphics.Color.parseColor(textColorStr) else android.graphics.Color.parseColor("#D1D5DB")
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#D1D5DB")
        }
        
        active?.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor)
            setTextColor(textColor)
            alpha = 1.0f
        }

        inactives.forEach { inactive ->
            inactive.apply {
                backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
                setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                alpha = 1.0f
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

