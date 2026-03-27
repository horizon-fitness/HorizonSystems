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
        TrainingLog("Mar 23, 2024", "11:00 AM", "1hr", "Yoga Session", "Sarah Wilson", "APPROVED")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_booking, container, false)

        val rvTrainingLogs = view.findViewById<RecyclerView>(R.id.rvTrainingLogs)
        val emptyStateContainer = view.findViewById<View>(R.id.emptyStateContainer)
        val calendarView = view.findViewById<android.widget.CalendarView>(R.id.calendar_view)
        val dateDetailsCard = view.findViewById<View>(R.id.cv_date_details)
        val dateInfoText = view.findViewById<TextView>(R.id.tv_selected_date_info)

        // Setup Adapter
        rvTrainingLogs.layoutManager = LinearLayoutManager(requireContext())
        adapter = TrainingLogAdapter(allLogs)
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

        // Filters
        view.findViewById<View>(R.id.btn_filter_all).setOnClickListener { filterLogs("ALL") }
        view.findViewById<View>(R.id.btn_filter_pending).setOnClickListener { filterLogs("PENDING") }
        view.findViewById<View>(R.id.btn_filter_approved).setOnClickListener { filterLogs("APPROVED") }

        return view
    }

    private fun filterLogs(status: String) {
        val filtered = if (status == "ALL") allLogs else allLogs.filter { it.status.uppercase() == status }
        adapter.updateLogs(filtered)
        
        view?.findViewById<View>(R.id.emptyStateContainer)?.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        view?.findViewById<View>(R.id.rvTrainingLogs)?.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }
}
