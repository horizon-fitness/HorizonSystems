package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.graphics.Color
import android.content.res.ColorStateList

class SuperAdminDashboardFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_super_admin_dashboard, container, false)
        
        setupStats(view)

        view.findViewById<View>(R.id.btnNavLogout).setOnClickListener {
            val intent = Intent(requireContext(), LandingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
        return view
    }

    private fun setupStats(view: View) {
        // In a real app, these would come from an API call
        view.findViewById<TextView>(R.id.statRevenue).text = "₱0.00"
        view.findViewById<TextView>(R.id.statTenants).text = "3 GYMS"
        view.findViewById<TextView>(R.id.statUsers).text = "7"
        view.findViewById<TextView>(R.id.statPending).text = "1"
    }
}
