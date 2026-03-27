package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val userName = activity?.intent?.getStringExtra("user_name") ?: "User"
        val userRole = activity?.intent?.getStringExtra("user_role") ?: "Member"
        val gymName = activity?.intent?.getStringExtra("gym_name") ?: "Horizon Gym"
        
        view.findViewById<TextView>(R.id.dashUserName).text = if (userName.length > 5) userName.take(3).uppercase() else userName.uppercase()

        // Handle Quick Action Navigation
        view.findViewById<View>(R.id.btnNavBookSession).setOnClickListener {
            (activity as? LandingActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_booking
        }

        view.findViewById<View>(R.id.btnNavPayment).setOnClickListener {
            (activity as? LandingActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_payment
        }

        view.findViewById<View>(R.id.btnNavMembership).setOnClickListener {
            (activity as? LandingActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_membership
        }

        // Today's Status "Book Now" link
        view.findViewById<View>(R.id.btnBookNow).setOnClickListener {
            (activity as? LandingActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_booking
        }

        view.findViewById<View>(R.id.btnNavAppointments).setOnClickListener {
            (activity as? LandingActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_appointment
        }

        return view
    }

    private fun logout() {
        (activity as? LandingActivity)?.performLogout()
    }
}
