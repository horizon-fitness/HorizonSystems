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
        val dashUserName = view.findViewById<TextView>(R.id.dashUserName)
        val dashGreeting = view.findViewById<TextView>(R.id.dashGreeting)
        
        // Dynamic Time-based Greeting
        dashGreeting?.text = getDynamicGreeting()
        
        val shortName = if (userName.length > 12) userName.trim().split(" ").first() else userName
        dashUserName?.text = shortName.uppercase()
        
        // Profile Avatar & Theme Setup (Ultra-Modern Glass)
        val initialsText = view.findViewById<TextView>(R.id.memberInitials)
        val profileBanner = view.findViewById<ImageView>(R.id.memberProfilePic)
        val profileCard = profileBanner?.parent?.let { it.parent as? MaterialCardView }
        
        val themeColorStr = GymManager.getThemeColor(requireContext())
        if (!themeColorStr.isNullOrEmpty()) {
            try {
                val themeColor = android.graphics.Color.parseColor(themeColorStr)
                
                // 1. Avatar background (with subtle alpha)
                profileCard?.setCardBackgroundColor(themeColor)
                profileCard?.setStrokeColor(android.content.res.ColorStateList.valueOf(themeColor).withAlpha(100))
                
                // 2. Greeting Name color (Glow Effect)
                dashUserName?.setTextColor(themeColor)
                
                // 3. Quick Action Link color
                view.findViewById<TextView>(R.id.btnBookNow)?.setTextColor(themeColor)
                
                // 4. Service Grid Icon Tints (Silent mapping)
                val iconButtons = listOf(R.id.btnNavBookSession, R.id.btnNavPayment, R.id.btnNavMembership, R.id.btnNavProfile)
                iconButtons.forEach { id ->
                    val card = view.findViewById<MaterialCardView>(id)
                    val layout = card?.getChildAt(0) as? ViewGroup
                    layout?.let {
                        for (i in 0 until it.childCount) {
                            val child = it.getChildAt(i)
                            if (child is ImageView) {
                                child.imageTintList = android.content.res.ColorStateList.valueOf(themeColor)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                profileCard?.setCardBackgroundColor(android.graphics.Color.parseColor("#1AFFFFFF"))
            }
        }

        // Initialize Initials
        val initials = userName.trim().split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
        initialsText?.text = if (initials.isNotEmpty()) initials else "U"

        // Navigation Handlers (Safe transition)
        val setupNav = { id: Int, navId: Int ->
            view.findViewById<View>(id)?.setOnClickListener {
                (activity as? LandingActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = navId
            }
        }
        
        setupNav(R.id.btnNavBookSession, R.id.nav_booking)
        setupNav(R.id.btnNavPayment, R.id.nav_payment)
        setupNav(R.id.btnNavMembership, R.id.nav_membership)
        setupNav(R.id.btnNavProfile, R.id.nav_profile)

        // Status Card Clicks
        val bookClick = View.OnClickListener {
            (activity as? LandingActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_booking
        }
        view.findViewById<View>(R.id.todayStatusCard)?.setOnClickListener(bookClick)
        view.findViewById<View>(R.id.btnBookNow)?.setOnClickListener(bookClick)

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
                        val logoUrl = activity?.intent?.getStringExtra("gym_logo")
                        val gymLogoContainer = root.findViewById<View>(R.id.gymLogoContainer)
                        val gymLogoHeader = root.findViewById<View>(R.id.gymLogoHeader)
                        if (!logoUrl.isNullOrEmpty()) {
                            gymLogoContainer?.visibility = android.view.View.VISIBLE
                            gymLogoHeader?.visibility = android.view.View.VISIBLE
                        }
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

    private fun getDynamicGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning,"
            in 12..17 -> "Good afternoon,"
            else -> "Good evening,"
        }
    }

    private fun logout() {
        (activity as? LandingActivity)?.performLogout()
    }
}
