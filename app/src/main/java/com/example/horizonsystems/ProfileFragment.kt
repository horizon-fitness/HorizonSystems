package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val userName = activity?.intent?.getStringExtra("user_name") ?: "User"
        val userEmail = activity?.intent?.getStringExtra("user_email") ?: ""
        val userRole = activity?.intent?.getStringExtra("user_role") ?: "Member"
        val gymName = activity?.intent?.getStringExtra("gym_name") ?: "Horizon Gym"
        val tenantId = activity?.intent?.getStringExtra("tenant_id") ?: "000"

        view.findViewById<TextView>(R.id.profileName).text = userName
        view.findViewById<TextView>(R.id.profileEmail).text = userEmail
        view.findViewById<TextView>(R.id.profileRole).text = userRole
        view.findViewById<TextView>(R.id.profileGym).text = gymName
        view.findViewById<TextView>(R.id.profileTenantId).text = tenantId

        return view
    }
}
