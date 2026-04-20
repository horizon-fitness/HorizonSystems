package com.example.horizonsystems

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
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
import com.example.horizonsystems.utils.DialogUtils
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class BookingFragment : Fragment(), BookingFilterSheet.FilterListener, BookingSortSheet.SortListener {
    private lateinit var adapter: TrainingLogAdapter
    private val allLogs = mutableListOf<TrainingLog>()

    private var currentPage = 1
    private val itemsPerPage = 5
    private var currentFilter = "ALL"
    private var currentSort = "NEWEST"
    private var searchQuery = ""
    private var startDate: Long? = null
    private var endDate: Long? = null

    private var filteredList: List<TrainingLog> = allLogs

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return try {
            inflater.inflate(R.layout.fragment_booking, container, false)
        } catch (e: Exception) {
            Log.e("BookingFragment", "Inflation Error: ${e.message}")
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

        adapter = TrainingLogAdapter(emptyList()) { log ->
            showCancelDialog(log)
        }
        rvTrainingLogs?.let {
            it.layoutManager = LinearLayoutManager(context ?: return@let)
            it.adapter = adapter
        }

        // --- Labs Search Header Logic ---
        val etSearch = view.findViewById<android.widget.EditText>(R.id.etSearchBooking)
        val btnSort = view.findViewById<View>(R.id.btnSortBooking)
        val btnFilter = view.findViewById<View>(R.id.btnFilterBooking)

        etSearch?.addTextChangedListener(object: android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim()
                applyPaginationAndRefresh(view)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnSort?.setOnClickListener {
            val sheet = BookingSortSheet()
            sheet.setParams(currentSort, this)
            sheet.show(childFragmentManager, "SORT_SHEET")
        }

        btnFilter?.setOnClickListener {
            val sheet = BookingFilterSheet()
            sheet.setParams(currentFilter, startDate, endDate, this)
            sheet.show(childFragmentManager, "FILTER_SHEET")
        }

        // Calendar Selection (Legacy feature kept for scheduling value)
        calendarView?.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            dateDetailsCard?.visibility = View.VISIBLE
            val displayDate = "${month + 1}/$dayOfMonth/$year"
            
            val approvedSessions = allLogs.filter { 
                val status = it.status?.uppercase()
                it.date == selectedDate && (status == "APPROVED" || status == "CONFIRMED") 
            }
            
            if (approvedSessions.isEmpty()) {
                dateInfoText?.text = "No approved sessions for $displayDate"
            } else {
                val sb = StringBuilder("Approved for $displayDate:\n")
                approvedSessions.forEachIndexed { index, session ->
                    val timeFormatted = formatTime(session.time)
                    sb.append("• ${session.service ?: "Service"} at $timeFormatted")
                    if (index < approvedSessions.size - 1) sb.append("\n")
                }
                dateInfoText?.text = sb.toString()
            }
        }

        // Action Buttons
        btnQuickBook?.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            val userId = GymManager.getUserId(ctx)
            if (userId == -1) return@setOnClickListener
            it.isEnabled = false
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val cookie = GymManager.getBypassCookie(ctx)
                    val ua = GymManager.getBypassUA(ctx)
                    val api = RetrofitClient.getApi(cookie, ua)
                    val gymId = GymManager.getTenantId(ctx)
                    val response = api.getActiveMembership(userId, gymId)
                    withContext(Dispatchers.Main) {
                        it.isEnabled = true
                        if (response.isSuccessful && response.body()?.success == true && response.body()?.subscriptionStatus == "Active") {
                            val sheet = BookingSheet()
                            sheet.onBookingCreated = { fetchBookings(view) }
                            sheet.show(parentFragmentManager, "booking_sheet")
                        } else if (response.body()?.subscriptionStatus == "Pending Approval") {
                            DialogUtils.showConfirmationDialog(
                                ctx,
                                "Approval Required",
                                "Your membership is currently pending approval. Please wait for the admin to approve it."
                            )
                        } else {
                            DialogUtils.showConfirmationDialog(
                                ctx,
                                "Membership Required",
                                "You must have an Active Membership to book a session."
                            )
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

        // Pagination Buttons
        view.findViewById<View>(R.id.btn_prev_booking)?.setOnClickListener {
            if (currentPage > 1) { currentPage--; applyPaginationAndRefresh(view) }
        }
        view.findViewById<View>(R.id.btn_next_booking)?.setOnClickListener {
            val totalPages = Math.max(1, Math.ceil(filteredList.size.toDouble() / itemsPerPage).toInt())
            if (currentPage < totalPages) { currentPage++; applyPaginationAndRefresh(view) }
        }

        fetchBookings(view)
    }

    private fun showCancelDialog(log: TrainingLog) {
        val ctx = context ?: return
        val reasons = arrayOf("Schedule Conflict", "Health Issues", "Emergency", "Change of Mind", "Others")
        var selectedReasonIndex = -1

        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle("Cancel Booking")
            .setSingleChoiceItems(reasons, -1) { _, which ->
                selectedReasonIndex = which
            }
            .setPositiveButton("Confirm") { dialog, _ ->
                if (selectedReasonIndex == -1) {
                    Toast.makeText(ctx, "Please select a reason for cancellation.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val selectedReason = reasons[selectedReasonIndex]
                processCancellation(log, selectedReason)
                dialog.dismiss()
            }
            .setNegativeButton("Back") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun processCancellation(log: TrainingLog, reason: String) {
        val ctx = context ?: return
        val userId = GymManager.getUserId(ctx)
        val gymId = GymManager.getTenantId(ctx)
        val bookingId = log.booking_id
        
        if (userId == -1 || bookingId == 0) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cookie = GymManager.getBypassCookie(ctx)
                val ua = GymManager.getBypassUA(ctx)
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.cancelBooking(userId, gymId, bookingId, reason)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(ctx, response.body()?.message ?: "Booking cancelled.", Toast.LENGTH_LONG).show()
                        fetchBookings(view ?: return@withContext)
                    } else {
                        Toast.makeText(ctx, response.body()?.message ?: "Failed to cancel booking.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(ctx, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formatTime(time: String?): String {
        return try {
            val sdf24 = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
            val sdf12 = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
            sdf12.format(sdf24.parse(time ?: "00:00:00")!!)
        } catch (e: Exception) { (time ?: "00:00").substringBeforeLast(":") }
    }

    override fun onFiltersApplied(status: String, start: Long?, end: Long?) {
        this.currentFilter = status
        this.startDate = start
        this.endDate = end
        applyPaginationAndRefresh(view ?: return)
    }

    override fun onSortSelected(sort: String) {
        this.currentSort = sort
        applyPaginationAndRefresh(view ?: return)
    }

    private fun fetchBookings(root: View) {
        val ctx = context ?: return
        val userId = GymManager.getUserId(ctx)
        if (userId == -1) { applyPaginationAndRefresh(root); return }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cookie = GymManager.getBypassCookie(ctx)
                val ua = GymManager.getBypassUA(ctx)
                val gymId = GymManager.getTenantId(ctx)
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.getUserBookings(userId, gymId)
                withContext(Dispatchers.Main) {
                    allLogs.clear()
                    if (response.isSuccessful && response.body()?.bookings != null) {
                        allLogs.addAll(response.body()!!.bookings!!)
                    }
                    applyPaginationAndRefresh(root)
                }
            } catch (e: Exception) {
                Log.e("BookingFragment", "Fetch Error: ${e.message}")
                withContext(Dispatchers.Main) { allLogs.clear(); applyPaginationAndRefresh(root) }
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

            view.setBackgroundColor(bgColor)
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_quick_book)?.let { btn ->
                btn.strokeColor = android.content.res.ColorStateList.valueOf(themeColor)
                btn.setTextColor(themeColor) 
                btn.setIconTint(android.content.res.ColorStateList.valueOf(themeColor))
            }

            view.findViewById<TextView>(R.id.tv_booking_theme_title)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.tv_selected_date_label)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.tv_upcoming_sessions_accent)?.setTextColor(themeColor)

            // Pagination Buttons
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_prev_booking)?.setIconTint(android.content.res.ColorStateList.valueOf(themeColor))
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_next_booking)?.setIconTint(android.content.res.ColorStateList.valueOf(themeColor))

            // Labs Header Branding
            view.findViewById<View>(R.id.btnSortBooking)?.let { it.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor.withAlpha(15)) }
            view.findViewById<View>(R.id.btnFilterBooking)?.let { it.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor.withAlpha(15)) }
            
            view.findViewById<View>(R.id.etSearchBooking)?.parent?.let { container ->
                if (container is View) {
                    val shape = android.graphics.drawable.GradientDrawable()
                    shape.setColor(android.graphics.Color.parseColor("#0DFFFFFF"))
                    shape.setStroke(1, themeColor.withAlpha(50))
                    shape.cornerRadius = (14 * ctx.resources.displayMetrics.density)
                    container.background = shape
                }
            }
            
            // Empty State Branding (Matching Text Color as requested)
            view.findViewById<ImageView>(R.id.ivEmptyBooking)?.imageTintList = android.content.res.ColorStateList.valueOf(textColor)
            view.findViewById<TextView>(R.id.bookingEmptyState)?.setTextColor(textColor)

        } catch (e: Exception) { Log.e("BookingFragment", "Branding Error: ${e.message}") }
    }

    private fun Int.withAlpha(alpha: Int): Int {
        return (this and 0x00FFFFFF) or (alpha shl 24)
    }

    private fun applyPaginationAndRefresh(root: View) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        
        // 1. Filter logic
        var baseList = allLogs.filter { log ->
            val statusMatch = if (currentFilter == "ALL") {
                true
            } else {
                val filters = currentFilter.split(",")
                val logStatus = log.status?.uppercase() ?: ""
                val mappedLogStatus = if (logStatus == "CONFIRMED") "APPROVED" else logStatus
                filters.contains(mappedLogStatus)
            }

            val dateMatch = if (startDate != null && endDate != null) {
                try {
                    val logDate = sdf.parse(log.date ?: "")?.time ?: 0L
                    logDate in startDate!!..endDate!!
                } catch (e: Exception) { true }
            } else true

            val searchMatch = if (searchQuery.isNotEmpty()) {
                (log.service ?: "").contains(searchQuery, ignoreCase = true) || 
                (log.trainer ?: "").contains(searchQuery, ignoreCase = true)
            } else true

            statusMatch && dateMatch && searchMatch
        }

        // 2. Sort Logic
        baseList = when(currentSort) {
            "OLDEST" -> baseList.sortedBy { try { sdf.parse(it.date ?: "")?.time ?: 0L } catch(e: Exception) { 0L } }
            "COACH" -> baseList.sortedBy { it.trainer ?: "" }
            else -> baseList.sortedByDescending { try { sdf.parse(it.date ?: "")?.time ?: 0L } catch(e: Exception) { 0L } } // NEWEST
        }

        filteredList = baseList
        
        val totalPages = Math.max(1, Math.ceil(filteredList.size.toDouble() / itemsPerPage).toInt())
        if (currentPage > totalPages) currentPage = totalPages
        if (currentPage < 1) currentPage = 1

        val startIndex = (currentPage - 1) * itemsPerPage
        val pageItems = if (filteredList.isEmpty()) emptyList() else filteredList.subList(startIndex, Math.min(startIndex + itemsPerPage, filteredList.size))
        
        if (::adapter.isInitialized) adapter.updateLogs(pageItems)

        // Visibility & UI
        val hasActiveFilter = currentFilter != "ALL" || startDate != null || searchQuery.isNotEmpty()
        val emptyMsg = if (hasActiveFilter) "No records in this filter" else "No training logs yet"
        root.findViewById<android.widget.TextView>(R.id.bookingEmptyState)?.text = emptyMsg

        root.findViewById<View>(R.id.emptyStateBooking)?.visibility = if (pageItems.isEmpty()) View.VISIBLE else View.GONE
        root.findViewById<View>(R.id.rvTrainingLogs)?.visibility = if (pageItems.isEmpty()) View.GONE else View.VISIBLE
        root.findViewById<View>(R.id.pagination_container_booking)?.visibility = if (totalPages > 1) View.VISIBLE else View.GONE
        root.findViewById<TextView>(R.id.tv_page_number_booking)?.text = "$currentPage/$totalPages"
        root.findViewById<View>(R.id.btn_prev_booking)?.isEnabled = currentPage > 1
        root.findViewById<View>(R.id.btn_next_booking)?.isEnabled = currentPage < totalPages
    }
}

