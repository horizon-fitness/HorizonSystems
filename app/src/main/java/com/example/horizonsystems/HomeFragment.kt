package com.example.horizonsystems

import android.content.Intent
import android.graphics.Color
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
import com.example.horizonsystems.utils.DialogUtils
import java.text.SimpleDateFormat
import java.util.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.MembershipPlan
import com.example.horizonsystems.network.RetrofitClient
import android.util.Log
import androidx.core.os.bundleOf

class HomeFragment : Fragment() {
    private var currentPlans: List<MembershipPlan>? = null
    private var hasActivePlan = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        applyBranding(view)

        val userName = activity?.intent?.getStringExtra("user_name") ?: "User"
        val dashUserName = view.findViewById<TextView>(R.id.dashUserName)
        val dashGreeting = view.findViewById<TextView>(R.id.dashGreeting)
        
        // Use "WELCOME" to support the two-tone horizontal design [WELCOME] [NAME]
        dashGreeting?.text = "WELCOME "
        
        val shortName = if (userName.length > 12) userName.trim().split(" ").first() else userName
        dashUserName?.text = shortName.uppercase()
        
        // Profile Avatar & Theme Setup (Ultra-Modern Glass)
        val initialsText = view.findViewById<TextView>(R.id.memberInitials)
        val profileBanner = view.findViewById<ImageView>(R.id.memberProfilePic)
        val profileCard = profileBanner?.parent?.let { it.parent as? MaterialCardView }
        
        // Initialize Initials
        val initials = userName.trim().split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
        initialsText?.text = if (initials.isNotEmpty()) initials else "U"

        // Load Profile Picture Native to Home Fragment
        val profilePicPath = activity?.intent?.getStringExtra("profile_pic")
        if (!profilePicPath.isNullOrEmpty() && profileBanner != null) {
            profileBanner.visibility = View.VISIBLE
            initialsText?.visibility = View.GONE
            GymManager.loadProfilePicture(requireContext(), profilePicPath, profileBanner)
        } else {
            profileBanner?.visibility = View.GONE
            initialsText?.visibility = View.VISIBLE
        }

        // Membership Plan Preview Setup
        val rvPreview = view.findViewById<RecyclerView>(R.id.rvMembershipPreview)
        rvPreview?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        fetchMembershipPlans(view)

        // Status Card Clicks
        val bookClick = View.OnClickListener {
            if (hasActivePlan) {
                (activity as? LandingActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_booking
            } else {
                DialogUtils.showConfirmationDialog(
                    requireContext(),
                    "Membership Required",
                    "You must have an Active Membership to book a session."
                )
            }
        }
        view.findViewById<View>(R.id.todayStatusCard)?.setOnClickListener(bookClick)
        view.findViewById<View>(R.id.btnBookNow)?.setOnClickListener(bookClick)

        fetchActiveStatus(view)
        fetchUpcomingSession(view)
        
        // Services Offered Setup
        val rvServices = view.findViewById<RecyclerView>(R.id.rvServicesPreview)
        rvServices?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        fetchServicesOffered(view)
        
        ThemeUtils.applyThemeToView(view)
    }

    private fun fetchData() {
        val view = view ?: return
        fetchMembershipPlans(view)
        fetchActiveStatus(view)
        fetchUpcomingSession(view)
        fetchServicesOffered(view)
    }

    private fun fetchServicesOffered(root: View) {
        val ctx = context ?: return
        val rvServices = root.findViewById<RecyclerView>(R.id.rvServicesPreview) ?: return
        val tenantId = GymManager.getTenantId(ctx)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cookie = GymManager.getBypassCookie(ctx)
                val ua = GymManager.getBypassUA(ctx)
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.getGymServices(tenantId)
                
                withContext(Dispatchers.Main) {
                    val layoutServicesHeader = root.findViewById<View>(R.id.layoutServicesHeader)
                    val emptyState = root.findViewById<View>(R.id.emptyStateServicesHome)
                    
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        val services = response.body()!!
                        layoutServicesHeader?.visibility = View.VISIBLE
                        rvServices.visibility = View.VISIBLE
                        emptyState?.visibility = View.GONE

                        val adapter = HomeServiceAdapter(services) { service ->
                            if (hasActivePlan) {
                                val bookingSheet = BookingSheet().apply {
                                    preSelectedServiceId = service.serviceId
                                }
                                bookingSheet.show(parentFragmentManager, "HomeBookingSheet")
                            } else {
                                DialogUtils.showConfirmationDialog(
                                    requireContext(),
                                    "Membership Required",
                                    "You must have an Active Membership to book a session."
                                )
                            }
                        }
                        rvServices.adapter = adapter
                    } else {
                        layoutServicesHeader?.visibility = View.VISIBLE
                        rvServices.visibility = View.GONE
                        emptyState?.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Services Fetch Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    root.findViewById<View>(R.id.layoutServicesHeader)?.visibility = View.GONE
                    root.findViewById<View>(R.id.rvServicesPreview)?.visibility = View.GONE
                }
            }
        }
    }

    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        val iconColorStr = GymManager.getIconColor(ctx)
        val textColorStr = GymManager.getTextColor(ctx)
        val bgColorStr = GymManager.getBgColor(ctx)
        val cardColorStr = GymManager.getCardColor(ctx)
        val isAutoCard = GymManager.getAutoCardTheme(ctx) == "1"

        try {
            val themeColor = if (!themeColorStr.isNullOrEmpty()) android.graphics.Color.parseColor(themeColorStr) else android.graphics.Color.parseColor("#8c2bee")
            val iconColor = if (!iconColorStr.isNullOrEmpty()) android.graphics.Color.parseColor(iconColorStr) else android.graphics.Color.parseColor("#A1A1AA")
            val textColor = if (!textColorStr.isNullOrEmpty()) android.graphics.Color.parseColor(textColorStr) else android.graphics.Color.parseColor("#D1D5DB")
            val bgColor = if (!bgColorStr.isNullOrEmpty()) android.graphics.Color.parseColor(bgColorStr) else android.graphics.Color.parseColor("#0a090d")

            // 1. Fragment Background
            view.findViewById<View>(R.id.homeRoot)?.setBackgroundColor(bgColor)

            // 2. Header & User Greeting
            view.findViewById<TextView>(R.id.dashGreeting)?.setTextColor(textColor)
            view.findViewById<TextView>(R.id.dashUserName)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.dashSubGreeting)?.setTextColor(textColor)
            
            val profileCard = view.findViewById<View>(R.id.cvProfileImage) as? MaterialCardView
            profileCard?.setCardBackgroundColor(themeColor)
            profileCard?.setStrokeColor(android.content.res.ColorStateList.valueOf(themeColor).withAlpha(100))

            // 3. Card Surfaces
            val cardSurface = if (isAutoCard) {
                android.graphics.Color.argb(13, android.graphics.Color.red(themeColor), android.graphics.Color.green(themeColor), android.graphics.Color.blue(themeColor))
            } else {
                try { android.graphics.Color.parseColor(cardColorStr) } catch(e: Exception) { android.graphics.Color.parseColor("#141216") }
            }

            val cards = listOf(R.id.todayStatusCard, R.id.cardMembership)
            cards.forEach { id ->
                val card = view.findViewById<MaterialCardView>(id)
                card?.setCardBackgroundColor(cardSurface)
            }

            // 4. Labels & Buttons
            view.findViewById<TextView>(R.id.tvUpcomingLabel)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.tvPlansHeaderPart2)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.tvServicesHeaderPart2)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.tvServicesSubtitle)?.setTextColor(textColor)
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBookNow)?.let {
                it.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor)
            }
            view.findViewById<TextView>(R.id.btnManageMembership)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.tvMembershipLabel)?.setTextColor(textColor)

            // Empty State Branding (Matching Text Color as requested)
            view.findViewById<ImageView>(R.id.ivEmptyPlansHome)?.imageTintList = android.content.res.ColorStateList.valueOf(textColor)
            view.findViewById<TextView>(R.id.tvNoPlansHome)?.setTextColor(textColor)
            view.findViewById<ImageView>(R.id.ivEmptyServicesHome)?.imageTintList = android.content.res.ColorStateList.valueOf(textColor)
            view.findViewById<TextView>(R.id.tvNoServicesHome)?.setTextColor(textColor)

            // 5. Icons & Tints (Removed grid icons, but keeping logic for sub-card icons)

            
            // Sub-card icons
            val subLayout = view.findViewById<View>(R.id.layoutSessionInfo) as? ViewGroup
            subLayout?.let {
                for (i in 0 until it.childCount) {
                    val child = it.getChildAt(i)
                    if (child is ImageView) {
                        child.imageTintList = android.content.res.ColorStateList.valueOf(iconColor)
                    }
                }
            }

            // 6. Refresh RecyclerView if it exists
            view.findViewById<RecyclerView>(R.id.rvMembershipPreview)?.adapter?.notifyDataSetChanged()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fetchMembershipPlans(root: View) {
        val ctx = context ?: return
        val rvPreview = root.findViewById<RecyclerView>(R.id.rvMembershipPreview) ?: return
        val tenantId = GymManager.getTenantId(ctx)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cookie = GymManager.getBypassCookie(ctx)
                val ua = GymManager.getBypassUA(ctx)
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.getMembershipPlans(tenantId)
                
                withContext(Dispatchers.Main) {
                    val tvPlansLabel = root.findViewById<View>(R.id.tvPlansLabel)
                    val emptyState = root.findViewById<View>(R.id.emptyStatePlansHome)
                    
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        val plans = response.body()!!
                        currentPlans = plans
                        tvPlansLabel?.visibility = View.VISIBLE
                        rvPreview.visibility = View.VISIBLE
                        emptyState?.visibility = View.GONE

                        val adapter = HomePlanAdapter(plans, !hasActivePlan) { plan ->
                            parentFragmentManager.setFragmentResult("plan_selection", bundleOf(
                                "id" to plan.id,
                                "name" to plan.name,
                                "price" to plan.price,
                                "days" to plan.durationDays
                            ))
                            (activity as? LandingActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_membership
                        }
                        rvPreview.adapter = adapter
                    } else {
                        tvPlansLabel?.visibility = View.VISIBLE
                        rvPreview.visibility = View.GONE
                        emptyState?.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Plans Fetch Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    root.findViewById<View>(R.id.tvPlansLabel)?.visibility = View.GONE
                    root.findViewById<View>(R.id.rvMembershipPreview)?.visibility = View.GONE
                }
            }
        }
    }

    private fun fetchUpcomingSession(root: View) {
        val userId = activity?.intent?.getIntExtra("user_id", -1) ?: -1
        if (userId == -1) return

        val sessionStatus = root.findViewById<TextView>(R.id.sessionStatus)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val tenantId = com.example.horizonsystems.utils.GymManager.getTenantId(context)
                val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(context)
                val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(context)
                val api = com.example.horizonsystems.network.RetrofitClient.getApi(cookie, ua)
                val response = api.getUserBookings(userId, tenantId)
                
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
                            sessionStatus?.text = upcoming.service ?: "Session"
                            
                            val tvSessionTime = root.findViewById<TextView>(R.id.tvSessionTime)
                            val tvSessionLocation = root.findViewById<TextView>(R.id.tvSessionLocation)
                            
                            tvSessionTime?.text = "$dateLabel, $timeFormatted"
                            tvSessionLocation?.text = "Studio A" // Placeholder as API doesn't return floor/room yet
                            
                            val tvUpcomingLabel = root.findViewById<TextView>(R.id.tvUpcomingLabel)
                            val tColor = GymManager.getThemeColor(requireContext())
                            tvUpcomingLabel?.setTextColor(android.graphics.Color.parseColor(tColor))
                        } else {
                            sessionStatus?.text = "No Active Bookings"
                            root.findViewById<TextView>(R.id.tvSessionTime)?.text = "Stay productive!"
                            root.findViewById<TextView>(R.id.tvSessionLocation)?.text = "---"
                        }
                    } else {
                        sessionStatus?.text = "No Active Bookings"
                        root.findViewById<TextView>(R.id.tvSessionTime)?.text = "Stay productive!"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val sessionStatus = root.findViewById<TextView>(R.id.sessionStatus)
                    sessionStatus?.text = "No Active Bookings"
                }
            }
        }
    }

    private fun fetchActiveStatus(root: View) {
        val userId = activity?.intent?.getIntExtra("user_id", -1) ?: -1
        if (userId == -1) return
        
        val tvUpcomingLabel = root.findViewById<TextView>(R.id.tvUpcomingLabel) // Reference for color update

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val tenantId = com.example.horizonsystems.utils.GymManager.getTenantId(context)
                val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(context)
                val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(context)
                val api = com.example.horizonsystems.network.RetrofitClient.getApi(cookie, ua)
                val response = api.getActiveMembership(userId, tenantId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val active = response.body()!!
                        
                        // Update Membership Status Text & Button
                        val statusText = root.findViewById<TextView>(R.id.membershipStatusText)
                        val planText = root.findViewById<TextView>(R.id.membershipPlan)
                        val btnManage = root.findViewById<TextView>(R.id.btnManageMembership)
                        
                        hasActivePlan = true

                        if (active.subscriptionStatus == "Pending Approval") {
                            statusText?.text = active.planName ?: "Plan"
                            statusText?.setTextColor(android.graphics.Color.WHITE)
                            btnManage?.text = "PENDING"
                            btnManage?.setTextColor(android.graphics.Color.parseColor("#FFC107"))
                        } else {
                            statusText?.text = active.planName ?: "Active Member"
                            statusText?.setTextColor(android.graphics.Color.WHITE)
                            btnManage?.text = "ACTIVE"
                            btnManage?.setTextColor(android.graphics.Color.parseColor("#34D399")) // Emerald color for active
                        }
                        
                        // Rebuild adapter if plans already fetched
                        if (currentPlans != null) {
                            val rvPreview = root.findViewById<RecyclerView>(R.id.rvMembershipPreview)
                            rvPreview?.adapter = HomePlanAdapter(currentPlans!!, false) { plan ->
                                // (Won't execute since canPurchase is false, but required by lambda)
                            }
                        }

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
                        val btnManage = root.findViewById<TextView>(R.id.btnManageMembership)
                        
                        hasActivePlan = false

                        statusText?.text = "No Active Plan"
                        statusText?.setTextColor(android.graphics.Color.WHITE)
                        btnManage?.text = "Manage"
                        
                        val themeColorStr = GymManager.getThemeColor(requireContext())
                        if (!themeColorStr.isNullOrEmpty()) {
                            btnManage?.setTextColor(android.graphics.Color.parseColor(themeColorStr))
                        }
                        
                        planText?.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hasActivePlan = false
                    val statusText = root.findViewById<TextView>(R.id.membershipStatusText)
                    val planText = root.findViewById<TextView>(R.id.membershipPlan)
                    statusText?.text = "No Active Plan"
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
