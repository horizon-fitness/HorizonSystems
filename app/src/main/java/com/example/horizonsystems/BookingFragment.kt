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
        val view = inflater.inflate(R.layout.fragment_booking, container, false)

        val rvTrainingLogs = view.findViewById<RecyclerView>(R.id.rvTrainingLogs)
        val calendarView = view.findViewById<android.widget.CalendarView>(R.id.calendar_view)
        val dateDetailsCard = view.findViewById<View>(R.id.cv_date_details)
        val dateInfoText = view.findViewById<TextView>(R.id.tv_selected_date_info)

        // Setup Adapter
        rvTrainingLogs.layoutManager = LinearLayoutManager(requireContext())
        adapter = TrainingLogAdapter(emptyList())
        rvTrainingLogs.adapter = adapter

        // Calendar Selection
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            val displayDate = "${month + 1}/$dayOfMonth/$year"
            dateDetailsCard.visibility = View.VISIBLE
            
            val approvedSessions = allLogs.filter { 
                it.date == selectedDate && (it.status.uppercase() == "APPROVED" || it.status.uppercase() == "CONFIRMED") 
            }
            
            if (approvedSessions.isEmpty()) {
                dateInfoText.text = "No approved sessions for $displayDate"
            } else {
                val sb = StringBuilder("Approved for $displayDate:\n")
                approvedSessions.forEachIndexed { index, session ->
                    val timeFormatted = try {
                        val sdf24 = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                        val sdf12 = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
                        val dateObj = sdf24.parse(session.time)
                        sdf12.format(dateObj!!)
                    } catch (e: Exception) {
                        session.time.substringBeforeLast(":") // Fallback
                    }
                    
                    sb.append("• ${session.service} at $timeFormatted")
                    if (index < approvedSessions.size - 1) sb.append("\n")
                }
                dateInfoText.text = sb.toString()
            }
        }

        // Action Buttons
        view.findViewById<View>(R.id.btn_quick_book).setOnClickListener {
            val userId = com.example.horizonsystems.utils.GymManager.getUserId(requireContext())
            if (userId == -1) return@setOnClickListener

            // Disable button briefly to prevent double clicks
            it.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val context = requireContext()
                    val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(context)
                    val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(context)
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
                            android.app.AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                                .setTitle("Membership Required")
                                .setMessage("You must have an Active Membership to book a session. Please ensure your membership is approved and not expired.")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        it.isEnabled = true
                        Toast.makeText(requireContext(), "Failed to verify membership status.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Filter Buttons
        val btnAll = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filter_all)
        val btnPending = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filter_pending)
        val btnApproved = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filter_approved)

        btnAll.setOnClickListener { 
            currentFilter = "ALL"
            updateFilterButtons(btnAll, listOf(btnPending, btnApproved))
            applyPaginationAndRefresh(view)
        }
        btnPending.setOnClickListener { 
            currentFilter = "PENDING"
            updateFilterButtons(btnPending, listOf(btnAll, btnApproved))
            applyPaginationAndRefresh(view)
        }
        btnApproved.setOnClickListener { 
            currentFilter = "APPROVED"
            updateFilterButtons(btnApproved, listOf(btnAll, btnPending))
            applyPaginationAndRefresh(view)
        }

        // Pagination Buttons
        view.findViewById<View>(R.id.btn_prev_booking).setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                applyPaginationAndRefresh(view)
            }
        }
        view.findViewById<View>(R.id.btn_next_booking).setOnClickListener {
            val totalPages = Math.ceil(filteredList.size.toDouble() / itemsPerPage).toInt()
            if (currentPage < totalPages) {
                currentPage++
                applyPaginationAndRefresh(view)
            }
        }

        // Initial setup
        updateFilterButtons(btnAll, listOf(btnPending, btnApproved))
        fetchBookings(view)
        
        ThemeUtils.applyThemeToView(view)

        return view
    }

    private fun fetchBookings(root: View) {
        val context = requireContext()
        val userId = com.example.horizonsystems.utils.GymManager.getUserId(context)
        val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(context)
        val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(context)

        if (userId == -1) {
            applyPaginationAndRefresh(root)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.getUserBookings(userId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.bookings != null) {
                        allLogs.clear()
                        allLogs.addAll(response.body()!!.bookings!!)
                        applyPaginationAndRefresh(root)
                    } else {
                        allLogs.clear()
                        applyPaginationAndRefresh(root)
                        // Optional: Show empty Toast if truly 0
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


    private fun updateFilterButtons(active: com.google.android.material.button.MaterialButton, inactives: List<com.google.android.material.button.MaterialButton>) {
        val themeColor = GymManager.getThemeColor(requireContext())
        active.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(themeColor)))
        active.setTextColor(android.graphics.Color.WHITE)
        active.setStrokeWidth(0)

        inactives.forEach { inactive ->
            inactive.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1A1A1A")))
            inactive.setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
            inactive.setStrokeWidth(1)
            inactive.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#333333")))
        }
    }

    private fun applyPaginationAndRefresh(root: View) {
        filteredList = if (currentFilter == "ALL") {
            allLogs
        } else if (currentFilter == "APPROVED") {
            allLogs.filter { it.status.uppercase() == "APPROVED" || it.status.uppercase() == "CONFIRMED" }
        } else {
            allLogs.filter { it.status.uppercase() == currentFilter }
        }
        
        val totalPages = Math.max(1, Math.ceil(filteredList.size.toDouble() / itemsPerPage).toInt())
        if (currentPage > totalPages) currentPage = totalPages
        if (currentPage < 1) currentPage = 1

        val startIndex = (currentPage - 1) * itemsPerPage
        val endIndex = Math.min(startIndex + itemsPerPage, filteredList.size)
        
        val pageItems = if (filteredList.isEmpty()) emptyList() else filteredList.subList(startIndex, endIndex)
        adapter.updateLogs(pageItems)

        // Visibility & Text
        root.findViewById<View>(R.id.emptyStateContainer).visibility = if (pageItems.isEmpty()) View.VISIBLE else View.GONE
        root.findViewById<View>(R.id.rvTrainingLogs).visibility = if (pageItems.isEmpty()) View.GONE else View.VISIBLE
        
        val paginationContainer = root.findViewById<View>(R.id.pagination_container_booking)
        paginationContainer.visibility = if (totalPages > 1) View.VISIBLE else View.GONE
        
        root.findViewById<TextView>(R.id.tv_page_number_booking).text = "$currentPage/$totalPages"
        
        root.findViewById<View>(R.id.btn_prev_booking).isEnabled = currentPage > 1
        root.findViewById<View>(R.id.btn_next_booking).isEnabled = currentPage < totalPages
    }
}
