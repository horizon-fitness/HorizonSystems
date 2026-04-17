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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.horizonsystems.utils.ThemeUtils
import com.example.horizonsystems.utils.GymManager
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val userName = activity?.intent?.getStringExtra("user_name") ?: "User"
        val userRole = activity?.intent?.getStringExtra("user_role") ?: "Member"
        val gymName = activity?.intent?.getStringExtra("gym_name") ?: "Horizon Gym"
        
        val dashUserName = view.findViewById<TextView>(R.id.dashUserName)
        val shortName = if (userName.length > 6) userName.take(3).uppercase() else userName.uppercase()
        dashUserName?.text = shortName
        
        // Profile Picture Placeholder with Initials
        val profilePic = view.findViewById<ImageView>(R.id.memberProfilePic)
        val initialsText = view.findViewById<TextView>(R.id.memberInitials)
        val profileCard = profilePic?.parent?.let { it.parent as? com.google.android.material.card.MaterialCardView }
        
        if (profileCard != null) {
            val themeColor = GymManager.getThemeColor(requireContext())
            profileCard.setCardBackgroundColor(android.graphics.Color.parseColor(themeColor))
            
            // Calculate Initials
            val initials = userName.trim().split(" ")
                .filter { it.isNotEmpty() }
                .take(2)
                .map { it.first().uppercase() }
                .joinToString("")
            
            initialsText?.text = if (initials.isNotEmpty()) initials else "U"
            initialsText?.visibility = View.VISIBLE
            profilePic?.visibility = View.GONE
        }

        // Handle Quick Action Navigation
        view.findViewById<View>(R.id.btnNavBookSession)?.setOnClickListener {
            (activity as? LandingActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_booking
        }

        view.findViewById<View>(R.id.btnNavPayment)?.setOnClickListener {
            (activity as? LandingActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_payment
        }

        view.findViewById<View>(R.id.btnNavMembership)?.setOnClickListener {
            (activity as? LandingActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_membership
        }

        // Today's Status Card & "Book Now" link
        val bookClick = View.OnClickListener {
            (activity as? LandingActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_booking
        }
        view.findViewById<View>(R.id.todayStatusCard)?.setOnClickListener(bookClick)
        view.findViewById<View>(R.id.btnBookNow)?.setOnClickListener(bookClick)

        // Membership Status Card
        view.findViewById<View>(R.id.cardMembership)?.setOnClickListener {
            (activity as? LandingActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_membership
        }

        view.findViewById<View>(R.id.btnNavProfile)?.setOnClickListener {
            (activity as? LandingActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_profile
        }

        fetchActiveStatus(view)
        fetchUpcomingSession(view)
        
        ThemeUtils.applyThemeToView(view)

        return view
    }

    private fun fetchUpcomingSession(root: View) {
        val userId = activity?.intent?.getIntExtra("user_id", -1) ?: -1
        if (userId == -1) return

        val sessionStatus = root.findViewById<TextView>(R.id.sessionStatus)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(context)
                val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(context)
                val api = com.example.horizonsystems.network.RetrofitClient.getApi(cookie, ua)
                val response = api.getUserBookings(userId)
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val allBookings = response.body()?.bookings ?: emptyList()
                        
                        // Find the closest upcoming approved/confirmed booking
                        val upcoming = allBookings
                            .filter { (it.status.uppercase() == "APPROVED" || it.status.uppercase() == "CONFIRMED") && it.date >= today }
                            .sortedWith(compareBy({ it.date }, { it.time }))
                            .firstOrNull()

                        if (upcoming != null) {
                            // Format: "Yoga at 2:00 PM (Today)" or "Yoga at 2:00 PM (Apr 06)"
                            val dateLabel = if (upcoming.date == today) {
                                "Today"
                            } else {
                                try {
                                    val inSdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                    val outSdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.US)
                                    outSdf.format(inSdf.parse(upcoming.date)!!)
                                } catch (e: Exception) {
                                    upcoming.date
                                }
                            }
                            // Format Time to 12h: hh:mm AM/PM
                            val timeFormatted = try {
                                val sdf24 = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                                val sdf12 = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
                                val dateObj = sdf24.parse(upcoming.time)
                                sdf12.format(dateObj!!)
                            } catch (e: Exception) {
                                upcoming.time.substringBeforeLast(":") // Fallback
                            }
                            sessionStatus?.text = "${upcoming.service} at $timeFormatted ($dateLabel)"
                            val tColor = GymManager.getThemeColor(requireContext())
                            sessionStatus?.setTextColor(android.graphics.Color.parseColor(tColor))
                        } else {
                            sessionStatus?.text = "No Active Bookings"
                            sessionStatus?.setTextColor(android.graphics.Color.WHITE)
                        }
                    } else {
                        sessionStatus?.text = "No Active Bookings"
                        sessionStatus?.setTextColor(android.graphics.Color.WHITE)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val sessionStatus = root.findViewById<TextView>(R.id.sessionStatus)
                    sessionStatus?.text = "No Active Bookings"
                    sessionStatus?.setTextColor(android.graphics.Color.WHITE)
                }
            }
        }
    }

    private fun fetchActiveStatus(root: View) {
        val userId = activity?.intent?.getIntExtra("user_id", -1) ?: -1
        if (userId == -1) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(context)
                val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(context)
                val api = com.example.horizonsystems.network.RetrofitClient.getApi(cookie, ua)
                val response = api.getActiveMembership(userId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val active = response.body()!!
                        
                        // Update Membership Status Text
                        val statusText = root.findViewById<TextView>(R.id.membershipStatusText)
                        val planText = root.findViewById<TextView>(R.id.membershipPlan)
                        
                        if (active.subscriptionStatus == "Pending Approval") {
                            statusText?.text = "Pending Approval"
                            statusText?.setTextColor(android.graphics.Color.parseColor("#FFC107")) // Warning/Amber color
                        } else {
                            statusText?.text = "Active Member"
                            statusText?.setTextColor(android.graphics.Color.parseColor("#FFD700")) // Gold color
                        }
                        
                        planText?.text = "Plan: ${active.planName}"
                        planText?.visibility = View.VISIBLE
                    } else {
                        val statusText = root.findViewById<TextView>(R.id.membershipStatusText)
                        val planText = root.findViewById<TextView>(R.id.membershipPlan)
                        statusText?.text = "No Active Membership"
                        statusText?.setTextColor(android.graphics.Color.WHITE)
                        planText?.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val statusText = root.findViewById<TextView>(R.id.membershipStatusText)
                    val planText = root.findViewById<TextView>(R.id.membershipPlan)
                    statusText?.text = "No Active Membership"
                    statusText?.setTextColor(android.graphics.Color.WHITE)
                    planText?.visibility = View.GONE
                }
            }
        }
    }

    private fun logout() {
        (activity as? LandingActivity)?.performLogout()
    }
}
