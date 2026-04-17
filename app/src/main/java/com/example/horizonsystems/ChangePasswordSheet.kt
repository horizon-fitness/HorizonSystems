package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.example.horizonsystems.utils.ThemeUtils

class ChangePasswordSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.sheet_change_password, container, false)
        
        val editCurrent = view.findViewById<TextInputEditText>(R.id.editCurrentPassword)
        val editNew = view.findViewById<TextInputEditText>(R.id.editNewPassword)
        val editConfirm = view.findViewById<TextInputEditText>(R.id.editConfirmPassword)
        val btnUpdate = view.findViewById<View>(R.id.btnUpdatePassword)
        val btnCancel = view.findViewById<View>(R.id.btnCancelPassword)

        btnCancel.setOnClickListener { dismiss() }

        btnUpdate.setOnClickListener {
            val current = editCurrent.text.toString()
            val newPass = editNew.text.toString()
            val confirm = editConfirm.text.toString()

            if (newPass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a new password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirm) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // For demo: Verify current password if applicable
            val savedPass = GymManager.getSavedPassword(requireContext())
            if (savedPass.isNotEmpty() && current != savedPass) {
                Toast.makeText(requireContext(), "Current password incorrect", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save locally for demo persistence
            GymManager.updateSavedPassword(requireContext(), newPass)
            
            Toast.makeText(requireContext(), "Password updated. Try logging out and back in!", Toast.LENGTH_LONG).show()
            dismiss()
        }

        ThemeUtils.applyThemeToView(view)

        return view
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
