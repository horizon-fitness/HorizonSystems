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
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_booking, container, false)

        val rvTrainingLogs = view.findViewById<RecyclerView>(R.id.rvTrainingLogs)
        val emptyState = view.findViewById<TextView>(R.id.bookingEmptyState)
        val fabBook = view.findViewById<FloatingActionButton>(R.id.fabBook)

        // Sample data matching web reference
        val sampleLogs = listOf(
            TrainingLog("Mar 21, 2024", "10:00 AM", "1hr", "Unlimited Gym Use", "John Doe", "ACTIVE"),
            TrainingLog("Mar 22, 2024", "09:00 AM", "2hrs", "Personal Training", "Coach Mike", "ACTIVE")
        )

        if (sampleLogs.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvTrainingLogs.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvTrainingLogs.visibility = View.VISIBLE
            rvTrainingLogs.layoutManager = LinearLayoutManager(requireContext())
            rvTrainingLogs.adapter = TrainingLogAdapter(sampleLogs)
        }

        fabBook.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Booking Dialog...", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
