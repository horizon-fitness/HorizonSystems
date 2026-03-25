package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
        view.findViewById<TextView>(R.id.roleSubtitle).text = "Operational Analytics and Staff Control"

        // Configure Cards
        view.findViewById<TextView>(R.id.text1).text = "Staff Management"
        view.findViewById<ImageView>(R.id.icon1).setImageResource(R.drawable.ic_trainers)
        
        view.findViewById<TextView>(R.id.text2).text = "Sales Reports"
        view.findViewById<ImageView>(R.id.icon2).setImageResource(R.drawable.ic_payment)
        
        view.findViewById<TextView>(R.id.text3).text = "Member Directory"
        view.findViewById<ImageView>(R.id.icon3).setImageResource(R.drawable.ic_membership)
        
        view.findViewById<TextView>(R.id.text4).text = "Gym Settings"
        view.findViewById<ImageView>(R.id.icon4).setImageResource(R.drawable.ic_settings)
        
        // Show extra cards for Tenant
        val card5 = view.findViewById<View>(R.id.card5)
        card5.visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.text5).text = "Subscriptions"
        view.findViewById<ImageView>(R.id.icon5).setImageResource(R.drawable.ic_booking)

        view.findViewById<View>(R.id.btnNavLogout).setOnClickListener {
            val intent = Intent(requireContext(), LandingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
        return view
    }
}
