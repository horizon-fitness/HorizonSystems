package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
        
        view.findViewById<TextView>(R.id.dashUserName).text = userName

        // Handle Logout from Quick Actions
        view.findViewById<View>(R.id.btnNavLogout).setOnClickListener {
            logout()
        }

        // Placeholder logic for status cards
        // In a real app, these would be populated from an API
        view.findViewById<TextView>(R.id.membershipPlan).text = "Plan: Standard Access"
        
        return view
    }

    private fun logout() {
        val intent = Intent(requireContext(), LandingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }
}
