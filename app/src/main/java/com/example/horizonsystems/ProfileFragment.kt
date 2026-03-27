package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Hide top profile icon when on Profile screen
        (activity as? LandingActivity)?.setTopProfileVisibility(false)

        val intent = activity?.intent
        val userName = intent?.getStringExtra("user_name") ?: "User"
        val userEmail = intent?.getStringExtra("user_email") ?: ""
        val userRole = intent?.getStringExtra("user_role") ?: "Member"
        val gymName = intent?.getStringExtra("gym_name") ?: "Horizon Gym"
        val tenantId = intent?.getStringExtra("tenant_id") ?: "000"

        // Registration Fields
        val firstName = intent?.getStringExtra("first_name") ?: ""
        val lastName = intent?.getStringExtra("last_name") ?: ""
        val middleName = intent?.getStringExtra("middle_name") ?: ""
        val phone = intent?.getStringExtra("contact_number") ?: ""
        val address = intent?.getStringExtra("address") ?: ""
        val birthDate = intent?.getStringExtra("birth_date") ?: ""
        val sex = intent?.getStringExtra("sex") ?: ""
        val occupation = intent?.getStringExtra("occupation") ?: ""
        val medHistory = intent?.getStringExtra("medical_history") ?: ""
        val emergencyName = intent?.getStringExtra("emergency_name") ?: ""
        val emergencyPhone = intent?.getStringExtra("emergency_phone") ?: ""

        // Identification
        val displayName = if (firstName.isNotEmpty() && lastName.isNotEmpty()) "$firstName $lastName".uppercase() else userName.uppercase()
        view.findViewById<TextView>(R.id.profileName).text = displayName
        view.findViewById<TextView>(R.id.profileFirstName).text = firstName.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileLastName).text = lastName.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileMiddleName).text = middleName.ifEmpty { "---" }
        
        // Contact
        view.findViewById<TextView>(R.id.profileEmail).text = userEmail.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profilePhone).text = phone.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileAddress).text = address.ifEmpty { "---" }
        
        // Health & Status
        view.findViewById<TextView>(R.id.profileBirthDate).text = birthDate.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileSex).text = sex.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileOccupation).text = occupation.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileMedHistory).text = medHistory.ifEmpty { "None listed" }
        
        // Emergency
        view.findViewById<TextView>(R.id.profileEmergencyName).text = emergencyName.ifEmpty { "---" }
        view.findViewById<TextView>(R.id.profileEmergencyPhone).text = emergencyPhone.ifEmpty { "---" }

        // Context Footer
        view.findViewById<TextView>(R.id.profileRole).text = userRole
        view.findViewById<TextView>(R.id.profileGym).text = gymName
        view.findViewById<TextView>(R.id.profileTenantId).text = tenantId

        // Sign Out Logic
        view.findViewById<View>(R.id.btnSignOut).setOnClickListener {
            (activity as? LandingActivity)?.performLogout() ?: activity?.finish()
        }

        // Edit Profile Placeholder
        view.findViewById<View>(R.id.btnEditProfile).setOnClickListener {
            android.widget.Toast.makeText(requireContext(), "Edit Profile coming soon", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Copy Success Lambda
        val copyToClipboard: (String, String) -> Unit = { label, text ->
            val clipboard = androidx.core.content.ContextCompat.getSystemService(requireContext(), android.content.ClipboardManager::class.java)
            val clip = android.content.ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
            android.widget.Toast.makeText(requireContext(), "$label copied", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Copy Actions
        view.findViewById<View>(R.id.profileTenantId).parent?.let { parent ->
            (parent as? View)?.setOnClickListener { copyToClipboard("Tenant ID", tenantId) }
        }
        view.findViewById<View>(R.id.profilePhone).parent?.let { parent ->
            (parent as? View)?.setOnClickListener { copyToClipboard("Phone number", phone) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show top profile icon again when leaving
        (activity as? LandingActivity)?.setTopProfileVisibility(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
}
