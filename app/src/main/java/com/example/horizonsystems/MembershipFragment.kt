package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.Transaction

class MembershipFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_membership, container, false)

        val rvMembershipHistory = view.findViewById<RecyclerView>(R.id.rvMembershipHistory)

        // Sample data matching web reference
        val sampleHistory = listOf(
            Transaction("Mar 01, 2024", "09:00 AM", "Unlimited Gym", "REF-001", "500.00", "Approved"),
            Transaction("Feb 01, 2024", "09:00 AM", "Unlimited Gym", "REF-002", "500.00", "Approved")
        )

        rvMembershipHistory.layoutManager = LinearLayoutManager(requireContext())
        rvMembershipHistory.adapter = TransactionAdapter(sampleHistory)

        return view
    }
}
