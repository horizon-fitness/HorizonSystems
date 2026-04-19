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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.net.Uri
import android.util.Base64
import com.bumptech.glide.Glide
import com.example.horizonsystems.network.RetrofitClient
import android.widget.Toast
import java.io.InputStream

class ProfileFragment : Fragment() {

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            uploadProfilePicture(uri)
        }
    }

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

        // Edit Profile Picture
        view.findViewById<View>(R.id.btnEditProfile)?.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
        val textColorStr = com.example.horizonsystems.utils.GymManager.getTextColor(requireContext())
        
        if (!themeColorStr.isNullOrEmpty()) {
            try {
                val themeColor = android.graphics.Color.parseColor(themeColorStr)
                val textColor = if (!textColorStr.isNullOrEmpty()) android.graphics.Color.parseColor(textColorStr) else android.graphics.Color.parseColor("#D1D5DB")
                val themeList = android.content.res.ColorStateList.valueOf(themeColor)
                
                // 1. Name and Headers Highlight
                val headers = listOf(
                    R.id.profileName, R.id.headerAccount, R.id.headerPersonal, 
                    R.id.headerContact, R.id.headerAddress, R.id.headerHealth, R.id.headerEmergency, R.id.headerParent
                )
                headers.forEach { view.findViewById<TextView>(it)?.setTextColor(themeColor) }
                
                // 2. Avatar Edit Icon (Pen only, matching Text Color as requested)
                val textList = android.content.res.ColorStateList.valueOf(textColor)
                view.findViewById<ImageView>(R.id.btnEditProfile)?.imageTintList = textList
                
                // 3. Hub Icons
                val icons = listOf(
                    R.id.iconUsername, R.id.iconRole, R.id.iconGym,
                    R.id.iconFirstName, R.id.iconMiddleName, R.id.iconLastName, R.id.iconBirthDate, R.id.iconSex,
                    R.id.iconPhone, R.id.iconEmail,
                    R.id.iconAddressLine, R.id.iconBarangay, R.id.iconCity, R.id.iconProvince, R.id.iconRegion,
                    R.id.iconOccupation, R.id.iconMedHistory,
                    R.id.iconEmergencyName, R.id.iconEmergencyPhone,
                    R.id.iconParentName, R.id.iconParentPhone,
                    R.id.iconMenuNotify, R.id.iconMenuInfo, R.id.iconMenuSecure
                )
                icons.forEach { iconId ->
                    view.findViewById<ImageView>(iconId)?.imageTintList = themeList
                }

                // 4. Role Accent 
                view.findViewById<TextView>(R.id.profileRole)?.setTextColor(themeColor)
                
                // 5. Action Cards Themed Backgrounds
                val bgAlphaColor = androidx.core.graphics.ColorUtils.setAlphaComponent(themeColor, 13) // 5% alpha
                val strokeAlphaColor = androidx.core.graphics.ColorUtils.setAlphaComponent(themeColor, 26) // 10% alpha
                
                val actionCards = listOf(R.id.btnProfileNotifications, R.id.btnChangePassword, R.id.btnEditFullProfile)
                actionCards.forEach { cardId ->
                    view.findViewById<com.google.android.material.card.MaterialCardView>(cardId)?.let { card ->
                        card.setCardBackgroundColor(bgAlphaColor)
                        card.strokeColor = strokeAlphaColor
                    }
                }
                
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
        
        // Parent / Guardian
        val parentName = intent?.getStringExtra("parent_name") ?: ""
        val parentPhone = intent?.getStringExtra("parent_contact_number") ?: ""

        // 0. Header Status
        val displayName = if (firstName.isNotEmpty() && lastName.isNotEmpty()) "$firstName $lastName".uppercase() else userName.uppercase()
        val finalEmail = userEmail.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileName)?.text = displayName
        view.findViewById<TextView>(R.id.profileEmail)?.text = finalEmail

        // 0.1 Profile Picture
        val profilePicPath = intent?.getStringExtra("profile_pic")
        val profileImageView = view.findViewById<ImageView>(R.id.profileImage)
        if (!profilePicPath.isNullOrEmpty() && profileImageView != null) {
            val fullUrl = "https://horizonsystems.rf.gd/$profilePicPath"
            
            profileImageView.imageTintList = null // Clear any tint
            profileImageView.alpha = 1.0f
            profileImageView.setPadding(0, 0, 0, 0)

            Glide.with(this)
                .load(fullUrl)
                .placeholder(R.drawable.ic_profile)
                .circleCrop()
                .into(profileImageView)
        } else if (profileImageView != null) {
            profileImageView.setImageResource(R.drawable.ic_profile)
            profileImageView.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            profileImageView.alpha = 0.3f
            profileImageView.setPadding(24, 24, 24, 24)
        }

        // 1. Account Details
        view.findViewById<TextView>(R.id.profileUsernameDisplay)?.text = userName.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileRole)?.text = userRole.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileGym)?.text = gymName.ifEmpty { "---" }

        // 2. Personal Information
        view.findViewById<TextView>(R.id.profileFirstName)?.text = firstName.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileMiddleName)?.text = middleName.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileLastName)?.text = lastName.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileBirthDate)?.text = birthDate.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileSex)?.text = sex.ifEmpty { "---" }

        // 3. Contact Information
        view.findViewById<TextView>(R.id.profilePhone)?.text = phone.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileEmailContact)?.text = finalEmail

        // 3.5. Residential Address
        val addressLine = intent?.getStringExtra("address_line") ?: ""
        val barangay = intent?.getStringExtra("barangay") ?: ""
        val city = intent?.getStringExtra("city") ?: ""
        val province = intent?.getStringExtra("province") ?: ""
        val region = intent?.getStringExtra("region") ?: ""
        view.findViewById<TextView>(R.id.profileAddressLine)?.text = addressLine.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileBarangay)?.text = barangay.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileCity)?.text = city.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileProvince)?.text = province.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileRegion)?.text = region.ifEmpty { "---" }

        // 4. Occupation & Health Status
        view.findViewById<TextView>(R.id.profileOccupation)?.text = occupation.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileMedHistory)?.text = medHistory.ifEmpty { "None listed" }

        // 5. Emergency Contact
        view.findViewById<TextView>(R.id.profileEmergencyName)?.text = emergencyName.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileEmergencyPhone)?.text = emergencyPhone.ifEmpty { "---" }

        // 6. Parent / Guardian
        view.findViewById<TextView>(R.id.profileParentName)?.text = parentName.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileParentPhone)?.text = parentPhone.ifEmpty { "---" }

        // Copy Success Lambda
        val copyToClipboard: (String, String) -> Unit = { label, text ->
            val clipboard = androidx.core.content.ContextCompat.getSystemService(requireContext(), android.content.ClipboardManager::class.java)
            val clip = android.content.ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
            android.widget.Toast.makeText(requireContext(), "$label copied", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Copy Actions
        view.findViewById<View>(R.id.profileParentPhone)?.parent?.let { parent ->
            (parent as? View)?.setOnClickListener { copyToClipboard("Parent Phone number", parentPhone) }
        }
        view.findViewById<View>(R.id.profilePhone)?.parent?.let { parent ->
            (parent as? View)?.setOnClickListener { copyToClipboard("Phone number", phone) }
        }
    }
    private fun uploadProfilePicture(uri: Uri) {
        val userId = activity?.intent?.getIntExtra("user_id", 0) ?: 0
        if (userId <= 0) return

        lifecycleScope.launch {
            try {
                val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes == null) return@launch
                
                val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)
                
                val request = mapOf(
                    "user_id" to userId,
                    "image" to base64Image
                )
                
                val cookie = GymManager.getBypassCookie(requireContext())
                val ua = GymManager.getBypassUA(requireContext())
                val response = RetrofitClient.getApi(cookie, ua).uploadProfilePic(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show()
                    
                    // Update the local URL and refresh
                    val newPath = response.body()?.path ?: ""
                    activity?.intent?.putExtra("profile_pic", newPath)
                    refreshUI()
                } else {
                    Toast.makeText(requireContext(), "Upload failed: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
