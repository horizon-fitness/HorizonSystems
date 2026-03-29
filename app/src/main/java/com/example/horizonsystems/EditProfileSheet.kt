package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText

class EditProfileSheet : BottomSheetDialogFragment() {

    var onSavedListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.sheet_edit_profile, container, false)
        
        val editFirst = view.findViewById<TextInputEditText>(R.id.editFirstName)
        val editLast = view.findViewById<TextInputEditText>(R.id.editLastName)
        val editPhone = view.findViewById<TextInputEditText>(R.id.editPhone)
        val editAddress = view.findViewById<TextInputEditText>(R.id.editAddress)
        val editOccupation = view.findViewById<TextInputEditText>(R.id.editOccupation)
        val btnSave = view.findViewById<View>(R.id.btnSaveChanges)

        // Pre-fill from activity intent
        activity?.intent?.let { intent ->
            editFirst.setText(intent.getStringExtra("first_name"))
            editLast.setText(intent.getStringExtra("last_name"))
            editPhone.setText(intent.getStringExtra("contact_number"))
            editAddress.setText(intent.getStringExtra("address"))
            editOccupation.setText(intent.getStringExtra("occupation"))
        }

        btnSave.setOnClickListener {
            val first = editFirst.text.toString()
            val last = editLast.text.toString()
            val phone = editPhone.text.toString()
            val addr = editAddress.text.toString()
            val occ = editOccupation.text.toString()

            if (first.isEmpty() || last.isEmpty()) {
                Toast.makeText(requireContext(), "Name fields cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update Activity's intent extras for the demo
            (activity as? LandingActivity)?.updateUserData(mapOf(
                "first_name" to first,
                "last_name" to last,
                "contact_number" to phone,
                "address" to addr,
                "occupation" to occ
            ))

            Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            onSavedListener?.invoke()
            dismiss()
        }

        return view
    }
    
    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
