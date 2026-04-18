package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.fragment.app.Fragment
import com.example.horizonsystems.utils.ThemeUtils
import com.example.horizonsystems.utils.GymManager

class ProfileFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Hide top notifications icon when on Profile screen
        (activity as? LandingActivity)?.setTopNotificationsVisibility(false)

        applyBranding(view)
        refreshUI()

        // Sign Out Logic
        view.findViewById<View>(R.id.btnSignOut)?.setOnClickListener {
            (activity as? LandingActivity)?.performLogout() ?: activity?.finish()
        }

        // Notifications Page
        view.findViewById<View>(R.id.btnProfileNotifications)?.setOnClickListener {
            try {
                NotificationSheet().show(parentFragmentManager, "notifications")
            } catch (e: Exception) {
            }
        }

        // Edit Personal Information (Full Profile)
        view.findViewById<View>(R.id.btnEditFullProfile)?.setOnClickListener {
            try {
                val sheet = EditProfileSheet()
                sheet.onSavedListener = {
                    refreshUI()
                }
                sheet.show(parentFragmentManager, "edit_profile_full")
            } catch (e: Exception) {
            }
        }
        // Change Password
        view.findViewById<View>(R.id.btnChangePassword)?.setOnClickListener {
            try {
                ChangePasswordSheet().show(parentFragmentManager, "change_password")
            } catch (e: Exception) {
            }
        }
    }

    private fun applyBranding(view: View) {
        val themeColorStr = com.example.horizonsystems.utils.GymManager.getThemeColor(requireContext())
        if (!themeColorStr.isNullOrEmpty()) {
            try {
                val themeColor = android.graphics.Color.parseColor(themeColorStr)
                val themeList = android.content.res.ColorStateList.valueOf(themeColor)
                
                // 1. Name Highlight
                view.findViewById<TextView>(R.id.profileName)?.setTextColor(themeColor)
                
                // 2. Avatar Edit Icon
                view.findViewById<ImageView>(R.id.iv_profile_edit_icon)?.imageTintList = themeList
                
                // 3. Hub Icons
                val icons = listOf(
                    R.id.iconUsername, R.id.iconMemberCode, R.id.iconPhone, 
                    R.id.iconAddress, R.id.iconMenuNotify, R.id.iconMenuInfo, R.id.iconMenuSecure
                )
                icons.forEach { iconId ->
                    view.findViewById<ImageView>(iconId)?.imageTintList = themeList
                }

                // 4. Role Accent 
                view.findViewById<TextView>(R.id.profileRole)?.setTextColor(themeColor)
                
            } catch (e: Exception) {}
        }
    }

    fun refreshUI() {
        val view = view ?: return
        val intent = activity?.intent
        val userName = intent?.getStringExtra("user_name") ?: "User"
        val userEmail = intent?.getStringExtra("email") ?: ""
        val userRole = intent?.getStringExtra("user_role") ?: "Member"
        val gymName = intent?.getStringExtra("gym_name") ?: "Horizon Gym"
        val tenantId = intent?.getStringExtra("tenant_id") ?: "000"
        val memberCode = intent?.getStringExtra("member_code") ?: ""

        // Registration Fields (Schema Aligned)
        val firstName = intent?.getStringExtra("first_name") ?: ""
        val lastName = intent?.getStringExtra("last_name") ?: ""
        val middleName = intent?.getStringExtra("middle_name") ?: ""
        val phone = intent?.getStringExtra("contact_number") ?: ""
        val address = intent?.getStringExtra("address") ?: ""
        val birthDate = intent?.getStringExtra("birth_date") ?: ""
        val sex = intent?.getStringExtra("sex") ?: ""
        val occupation = intent?.getStringExtra("occupation") ?: ""
        val medHistory = intent?.getStringExtra("medical_history") ?: ""
        val emergencyName = intent?.getStringExtra("emergency_contact_name") ?: ""
        val emergencyPhone = intent?.getStringExtra("emergency_contact_number") ?: ""

        // Identification
        val displayName = if (firstName.isNotEmpty() && lastName.isNotEmpty()) "$firstName $lastName".uppercase() else userName.uppercase()
        view.findViewById<TextView>(R.id.profileName)?.text = displayName
        view.findViewById<TextView>(R.id.profileFirstName)?.text = firstName.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileLastName)?.text = lastName.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileMiddleName)?.text = middleName.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileUsernameDisplay)?.text = userName.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileMemberCode)?.text = memberCode.ifEmpty { "---" }
        
        // Contact
        val finalEmail = if (userEmail.isNotEmpty()) {
            if (gymName.isNotEmpty() && gymName != "Horizon Gym") "$userEmail ($gymName)" else userEmail
        } else "---"
        view.findViewById<TextView>(R.id.profileEmail)?.text = finalEmail
        view.findViewById<TextView>(R.id.profilePhone)?.text = phone.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileAddress)?.text = address.ifEmpty { "---" }
        
        // Health & Status
        view.findViewById<TextView>(R.id.profileBirthDate)?.text = birthDate.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileSex)?.text = sex.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileOccupation)?.text = occupation.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileMedHistory)?.text = medHistory.ifEmpty { "None listed" }
        
        // Emergency
        view.findViewById<TextView>(R.id.profileEmergencyName)?.text = emergencyName.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileEmergencyPhone)?.text = emergencyPhone.ifEmpty { "---" }

        // Context Footer
        view.findViewById<TextView>(R.id.profileRole)?.text = userRole
        view.findViewById<TextView>(R.id.profileGym)?.text = gymName
        view.findViewById<TextView>(R.id.profileTenantId)?.text = tenantId

        // Copy Success Lambda
        val copyToClipboard: (String, String) -> Unit = { label, text ->
            val clipboard = androidx.core.content.ContextCompat.getSystemService(requireContext(), android.content.ClipboardManager::class.java)
            val clip = android.content.ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
            android.widget.Toast.makeText(requireContext(), "$label copied", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Copy Actions
        view.findViewById<View>(R.id.profileTenantId)?.parent?.let { parent ->
            (parent as? View)?.setOnClickListener { copyToClipboard("Tenant ID", tenantId) }
        }
        view.findViewById<View>(R.id.profilePhone)?.parent?.let { parent ->
            (parent as? View)?.setOnClickListener { copyToClipboard("Phone number", phone) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show top notifications icon again when leaving
        (activity as? LandingActivity)?.setTopNotificationsVisibility(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
}
