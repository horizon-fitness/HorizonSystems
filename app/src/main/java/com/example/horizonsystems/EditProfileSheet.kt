package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*
import com.example.horizonsystems.utils.ThemeUtils
import com.example.horizonsystems.utils.DialogUtils
import com.example.horizonsystems.network.RetrofitClient
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.horizonsystems.utils.NetworkBypass

class EditProfileSheet : BottomSheetDialogFragment() {

    var onSavedListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.sheet_edit_profile, container, false)
        
        // References
        val editFirst = view.findViewById<TextInputEditText>(R.id.editFirstName)
        val editLast = view.findViewById<TextInputEditText>(R.id.editLastName)
        val editMiddle = view.findViewById<TextInputEditText>(R.id.editMiddleName)
        val editBirth = view.findViewById<TextInputEditText>(R.id.editBirthDate)
        val editSex = view.findViewById<AutoCompleteTextView>(R.id.editSex)
        
        val editEmail = view.findViewById<TextInputEditText>(R.id.editEmail)
        val editPhone = view.findViewById<TextInputEditText>(R.id.editPhone)
        val editAddress = view.findViewById<TextInputEditText>(R.id.editAddress)
        
        val editOccupation = view.findViewById<TextInputEditText>(R.id.editOccupation)
        val editMedical = view.findViewById<TextInputEditText>(R.id.editMedical)
        
        val editEmergencyName = view.findViewById<TextInputEditText>(R.id.editEmergencyName)
        val editEmergencyPhone = view.findViewById<TextInputEditText>(R.id.editEmergencyPhone)
        
        val editUsername = view.findViewById<TextInputEditText>(R.id.editUsername)
        val editMemberCode = view.findViewById<TextInputEditText>(R.id.editMemberCode)
        
        val btnSave = view.findViewById<View>(R.id.btnSaveChanges)
        val btnCancel = view.findViewById<View>(R.id.btnCancelEdit)

        // Setup Sex Dropdown
        val sexOptions = arrayOf("Male", "Female")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sexOptions)
        editSex.setAdapter(adapter)

        // Setup Date Picker
        editBirth.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Birth Date")
                .setTheme(com.example.horizonsystems.utils.CalendarUtils.getCalendarTheme(requireContext()))
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                editBirth.setText(format.format(calendar.time))
            }
            datePicker.show(childFragmentManager, "DATE_PICKER")
        }

        // Pre-fill from activity intent
        activity?.intent?.let { intent ->
            editFirst.setText(intent.getStringExtra("first_name"))
            editLast.setText(intent.getStringExtra("last_name"))
            editMiddle.setText(intent.getStringExtra("middle_name"))
            editBirth.setText(intent.getStringExtra("birth_date"))
            editSex.setText(intent.getStringExtra("sex"), false)
            
            editEmail.setText(intent.getStringExtra("email"))
            editPhone.setText(intent.getStringExtra("contact_number"))
            editAddress.setText(intent.getStringExtra("address"))
            
            editOccupation.setText(intent.getStringExtra("occupation"))
            editMedical.setText(intent.getStringExtra("medical_history"))
            
            editEmergencyName.setText(intent.getStringExtra("emergency_contact_name"))
            editEmergencyPhone.setText(intent.getStringExtra("emergency_contact_number"))
            
            editUsername?.setText(intent.getStringExtra("user_name"))
            editMemberCode?.setText(intent.getStringExtra("member_code"))
        }

        btnCancel.setOnClickListener { dismiss() }

        btnSave.setOnClickListener {
            // Validation
            if (editFirst.text.isNullOrEmpty() || editLast.text.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Name fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = mapOf(
                "first_name" to editFirst.text.toString(),
                "last_name" to editLast.text.toString(),
                "middle_name" to editMiddle.text.toString(),
                "birth_date" to editBirth.text.toString(),
                "sex" to editSex.text.toString(),
                "email" to editEmail.text.toString(),
                "contact_number" to editPhone.text.toString(),
                "address" to editAddress.text.toString(),
                "occupation" to editOccupation.text.toString(),
                "medical_history" to editMedical.text.toString(),
                "emergency_contact_name" to editEmergencyName.text.toString(),
                "emergency_contact_number" to editEmergencyPhone.text.toString()
            )

            // Integrate Backend Update
            val ctx = requireContext()
            val userId = GymManager.getUserId(ctx)
            val gymId = GymManager.getTenantId(ctx)
            val cookie = GymManager.getBypassCookie(ctx)
            val ua = GymManager.getBypassUA(ctx)

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val api = RetrofitClient.getApi(cookie, ua)
                    val response = api.updateProfile(userId, updates)
                    
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            // Update Activity's intent extras for the demo/persistence
                            (activity as? LandingActivity)?.updateUserData(updates)
                            
                            DialogUtils.showConfirmationDialog(
                                ctx,
                                "Profile Updated",
                                "Your personal information has been saved successfully.",
                                positiveText = "OK"
                            ) {
                                onSavedListener?.invoke()
                                dismiss()
                            }
                        } else {
                            Toast.makeText(ctx, response.body()?.message ?: "Update failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(ctx, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        ThemeUtils.applyThemeToView(view)

        return view
    }
    
    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
