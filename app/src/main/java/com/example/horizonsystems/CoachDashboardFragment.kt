package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class CoachDashboardFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_role_dashboard, container, false)
        val role = activity?.intent?.getStringExtra("user_role") ?: "Coach"
        view.findViewById<TextView>(R.id.roleBadge).text = role.uppercase()
        view.findViewById<TextView>(R.id.roleTitle).text = "Coach Station"
        view.findViewById<TextView>(R.id.roleSubtitle).text = "Client Schedules and Session Monitoring"

        // Configure Cards
        view.findViewById<TextView>(R.id.text1).text = "My Clients"
        view.findViewById<ImageView>(R.id.icon1).setImageResource(R.drawable.ic_trainers)
        
        view.findViewById<TextView>(R.id.text2).text = "Training Schedule"
        view.findViewById<ImageView>(R.id.icon2).setImageResource(R.drawable.ic_appointment)
        
        view.findViewById<TextView>(R.id.text3).text = "My Profile"
        view.findViewById<ImageView>(R.id.icon3).setImageResource(R.drawable.ic_profile)
        
        // Hide unused cards
        view.findViewById<View>(R.id.card4).visibility = View.GONE

        view.findViewById<View>(R.id.btnNavLogout).setOnClickListener {
            val intent = Intent(requireContext(), LandingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
        return view
    }
}
