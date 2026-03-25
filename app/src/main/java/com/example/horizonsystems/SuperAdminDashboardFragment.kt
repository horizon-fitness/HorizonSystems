package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class SuperAdminDashboardFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_role_dashboard, container, false)
        val role = activity?.intent?.getStringExtra("user_role") ?: "Super Admin"
        view.findViewById<TextView>(R.id.roleBadge).text = role.uppercase()
        view.findViewById<TextView>(R.id.roleTitle).text = "Super Admin Control Center"
        view.findViewById<TextView>(R.id.roleSubtitle).text = "Global Overview and Tenant Management"
        return view
    }
}
