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

        val userName = activity?.intent?.getStringExtra("user_name") ?: "Unknown User"
        val userEmail = activity?.intent?.getStringExtra("user_email") ?: "No Email"
        val gymName = activity?.intent?.getStringExtra("gym_name") ?: "No Tenant"
        val logoUrl = activity?.intent?.getStringExtra("logo_url") ?: ""
        val themeColorStr = activity?.intent?.getStringExtra("theme_color") ?: ""

        view.findViewById<TextView>(R.id.dashUserName).text = userName
        view.findViewById<TextView>(R.id.dashUserEmail).text = userEmail
        view.findViewById<TextView>(R.id.dashGymName).text = gymName
        
        val profileInitial = view.findViewById<TextView>(R.id.profileInitial)
        profileInitial.text = userName.firstOrNull()?.toString()?.uppercase() ?: "U"

        val gymLogo = view.findViewById<ImageView>(R.id.gymLogo)
        if (logoUrl.isNotEmpty()) {
            val fullLogoUrl = if (logoUrl.startsWith("http")) logoUrl else "https://horizonfitnesscorp.gt.tc/$logoUrl"
            Glide.with(this).load(fullLogoUrl).into(gymLogo)
            gymLogo.visibility = View.VISIBLE
            profileInitial.visibility = View.GONE
        }

        if (themeColorStr.isNotEmpty()) {
            try {
                val color = android.graphics.Color.parseColor(themeColorStr)
                view.findViewById<MaterialCardView>(R.id.profileCard).setCardBackgroundColor(android.content.res.ColorStateList.valueOf(color))
                view.findViewById<TextView>(R.id.tenantLabel).setTextColor(color)
            } catch (e: Exception) {}
        }

        view.findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            val intent = Intent(requireContext(), LandingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        return view
    }
}
