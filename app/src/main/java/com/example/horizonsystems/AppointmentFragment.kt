package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.Appointment
import com.example.horizonsystems.utils.ThemeUtils

class AppointmentFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_appointment, container, false)

        val rvMeetingRequests = view.findViewById<RecyclerView>(R.id.rvMeetingRequests)

        // Sample data matching web reference
        val sampleAppointments = listOf(
            Appointment("1", "Membership Inquiry", "Mar 21, 2024", "10:00 AM", "Ask about family plan", "Pending"),
            Appointment("2", "Personal Trainer Meetup", "Mar 25, 2024", "02:00 PM", "Initial consultation", "Approved")
        )

        rvMeetingRequests.layoutManager = LinearLayoutManager(requireContext())
        rvMeetingRequests.adapter = AppointmentAdapter(sampleAppointments)

        ThemeUtils.applyThemeToView(view)

        return view
    }
}
