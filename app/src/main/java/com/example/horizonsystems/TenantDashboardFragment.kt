package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class TenantDashboardFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_role_dashboard, container, false)
        val role = activity?.intent?.getStringExtra("user_role") ?: "Tenant"
        view.findViewById<TextView>(R.id.roleBadge).text = (if (role == "Admin") "TENANT ADMIN" else role.uppercase())
        view.findViewById<TextView>(R.id.roleTitle).text = "Gym Management"
        view.findViewById<TextView>(R.id.roleSubtitle).text = "Member Analytics and Revenue Tracking"
        return view
    }
}
