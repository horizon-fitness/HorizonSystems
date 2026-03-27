package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.TrainingLog
import com.google.android.material.floatingactionbutton.FloatingActionButton

class BookingFragment : Fragment() {
    private lateinit var adapter: TrainingLogAdapter
    private val allLogs = listOf(
        TrainingLog("Mar 21, 2024", "10:00 AM", "1hr", "Unlimited Gym Use", "John Doe", "APPROVED"),
        TrainingLog("Mar 22, 2024", "09:00 AM", "2hrs", "Personal Training", "Coach Mike", "PENDING"),
        TrainingLog("Mar 23, 2024", "11:00 AM", "1hr", "Yoga Session", "Sarah Wilson", "APPROVED"),
        TrainingLog("Mar 24, 2024", "10:00 AM", "1hr", "Boxing Class", "Coach Mike", "APPROVED"),
        TrainingLog("Mar 25, 2024", "08:30 AM", "1hr", "Unlimited Gym Use", "John Doe", "PENDING"),
        TrainingLog("Mar 26, 2024", "04:00 PM", "2hrs", "Zumba session", "Sarah Wilson", "APPROVED"),
        TrainingLog("Mar 27, 2024", "09:00 AM", "1hr", "Personal Training", "Coach Mike", "PENDING"),
        TrainingLog("Mar 28, 2024", "10:00 AM", "1hr", "Boxing Class", "Coach Mike", "APPROVED"),
        TrainingLog("Mar 29, 2024", "11:00 AM", "1hr", "Yoga Session", "Sarah Wilson", "PENDING"),
        TrainingLog("Mar 30, 2024", "03:00 PM", "1hr", "Unlimited Gym Use", "John Doe", "APPROVED"),
        TrainingLog("Mar 31, 2024", "10:00 AM", "1hr", "Zumba session", "Sarah Wilson", "APPROVED")
    )

    private var currentPage = 1
    private val itemsPerPage = 5
    private var currentFilter = "ALL"
    private var filteredList = allLogs

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
            val date = "${month + 1}/$dayOfMonth/$year"
            dateDetailsCard.visibility = View.VISIBLE
            dateInfoText.text = "Logs for $date: Found ${allLogs.filter { it.date.contains(dayOfMonth.toString()) }.size} sessions"
        }

        // Action Buttons
        view.findViewById<View>(R.id.btn_quick_book).setOnClickListener {
            Toast.makeText(requireContext(), "Opening Quick Booking...", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.btn_talk_to_admin).setOnClickListener {
            Toast.makeText(requireContext(), "Connecting to Admin...", Toast.LENGTH_SHORT).show()
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
        applyPaginationAndRefresh(view)

        return view
    }

    private fun updateFilterButtons(active: com.google.android.material.button.MaterialButton, inactives: List<com.google.android.material.button.MaterialButton>) {
        active.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#A855F7")))
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
        filteredList = if (currentFilter == "ALL") allLogs else allLogs.filter { it.status.uppercase() == currentFilter }
        
        val totalPages = Math.ceil(filteredList.size.toDouble() / itemsPerPage).toInt()
        if (currentPage > totalPages && totalPages > 0) currentPage = totalPages
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
