package com.example.horizonsystems

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.horizonsystems.utils.GymManager
import com.example.horizonsystems.network.RetrofitClient
import com.example.horizonsystems.utils.ThemeUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class EditProfileSheet : BottomSheetDialogFragment() {

    var onSavedListener: (() -> Unit)? = null
    
    // View references for global access
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var btnSave: MaterialButton
    private lateinit var pbSaving: ProgressBar
    
    // Page View Holders (to access inputs across pages)
    private val pageViews = mutableMapOf<Int, View>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.sheet_edit_profile, container, false)
        
        tabLayout = root.findViewById(R.id.tabLayoutProfile)
        viewPager = root.findViewById(R.id.viewPagerProfile)
        btnSave = root.findViewById(R.id.btnSaveChanges)
        pbSaving = root.findViewById(R.id.pbSaving)
        
        setupViewPager()
        
        root.findViewById<View>(R.id.btnCancelEdit).setOnClickListener { dismiss() }

        btnSave.setOnClickListener {
            performSave()
        }

        ThemeUtils.applyThemeToView(root)
        
        // Initial branding apply
        applyBranding(root)
        
        // Background sync for real-time reflection
        lifecycleScope.launch {
            GymManager.syncBranding(requireContext())
            // Re-apply branding if it changed
            applyBranding(root)
        }

        return root
    }

    private fun setupViewPager() {
        val adapter = ProfilePagerAdapter()
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 4 // Keep all pages in memory for easy data access
        
        val tabTitles = arrayOf("ACCOUNT", "CONTACT", "RESIDENCE", "DETAILS", "IDENTITY")
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    private fun performSave() {
        val updates = mutableMapOf<String, Any>()
        
        // Collect data from all pages
        for (i in 0 until 5) {
            val page = pageViews[i] ?: continue
            collectDataFromPage(i, page, updates)
        }

        // Add IDs
        val userId = activity?.intent?.getIntExtra("user_id", -1) ?: -1
        val gymId = activity?.intent?.getIntExtra("gym_id", -1) ?: -1
        if (userId != -1) updates["user_id"] = userId
        if (gymId != -1) updates["gym_id"] = gymId

        // Validation (Basic check)
        if (updates["first_name"]?.toString().isNullOrEmpty() || updates["last_name"]?.toString().isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Name fields in BASIC tab are required", Toast.LENGTH_SHORT).show()
            viewPager.currentItem = 0
            return
        }

        submitProfileUpdates(updates)
    }

    private fun collectDataFromPage(position: Int, view: View, updates: MutableMap<String, Any>) {
        when (position) {
            0 -> { // Account
                updates["first_name"] = view.findViewById<TextInputEditText>(R.id.editFirstName).text.toString()
                updates["last_name"] = view.findViewById<TextInputEditText>(R.id.editLastName).text.toString()
                updates["middle_name"] = view.findViewById<TextInputEditText>(R.id.editMiddleName).text.toString()
                updates["birth_date"] = view.findViewById<TextInputEditText>(R.id.editBirthDate).text.toString()
                updates["sex"] = view.findViewById<AutoCompleteTextView>(R.id.editSex).text.toString()
                // Username is locked, no need to collect for update
            }
            1 -> { // Contact
                updates["email"] = view.findViewById<TextInputEditText>(R.id.editEmail).text.toString()
                updates["contact_number"] = view.findViewById<TextInputEditText>(R.id.editPhone).text.toString()
            }
            2 -> { // Residence
                updates["address_line"] = view.findViewById<TextInputEditText>(R.id.editAddressLine).text.toString()
                updates["barangay"] = view.findViewById<TextInputEditText>(R.id.editBarangay).text.toString()
                updates["city"] = view.findViewById<TextInputEditText>(R.id.editCity).text.toString()
                updates["province"] = view.findViewById<TextInputEditText>(R.id.editProvince).text.toString()
                updates["region"] = view.findViewById<TextInputEditText>(R.id.editRegion).text.toString()
            }
            3 -> { // Details
                updates["occupation"] = view.findViewById<TextInputEditText>(R.id.editOccupation).text.toString()
                updates["medical_history"] = view.findViewById<TextInputEditText>(R.id.editMedical).text.toString()
                updates["emergency_contact_name"] = view.findViewById<TextInputEditText>(R.id.editEmergencyName).text.toString()
                updates["emergency_contact_number"] = view.findViewById<TextInputEditText>(R.id.editEmergencyPhone).text.toString()
            }
        }
    }

    private fun prefillPage(position: Int, view: View) {
        val intent = activity?.intent ?: return
        when (position) {
            0 -> {
                view.findViewById<TextInputEditText>(R.id.editUsername).setText(intent.getStringExtra("user_name"))
                view.findViewById<TextInputEditText>(R.id.editFirstName).setText(intent.getStringExtra("first_name"))
                view.findViewById<TextInputEditText>(R.id.editLastName).setText(intent.getStringExtra("last_name"))
                view.findViewById<TextInputEditText>(R.id.editMiddleName).setText(intent.getStringExtra("middle_name"))
                
                val bdate = intent.getStringExtra("birth_date")
                val editBirth = view.findViewById<TextInputEditText>(R.id.editBirthDate)
                editBirth.setText(bdate)
                
                // AGE RULING: Lock Birth Date if already set
                if (!bdate.isNullOrEmpty()) {
                    editBirth.isEnabled = false
                    editBirth.alpha = 0.6f
                    view.findViewById<TextInputLayout>(R.id.layoutBirthDate).apply {
                        isEnabled = false
                        helperText = "Locked for security"
                    }
                }

                view.findViewById<AutoCompleteTextView>(R.id.editSex).setText(intent.getStringExtra("sex"), false)
                
                setupBasicPageListeners(view)
            }
            1 -> {
                view.findViewById<TextInputEditText>(R.id.editEmail).setText(intent.getStringExtra("email"))
                view.findViewById<TextInputEditText>(R.id.editPhone).setText(intent.getStringExtra("contact_number"))
            }
            2 -> {
                val addr = intent.getStringExtra("address_line") ?: intent.getStringExtra("address") ?: ""
                view.findViewById<TextInputEditText>(R.id.editAddressLine).setText(addr)
                view.findViewById<TextInputEditText>(R.id.editBarangay).setText(intent.getStringExtra("barangay"))
                view.findViewById<TextInputEditText>(R.id.editCity).setText(intent.getStringExtra("city"))
                view.findViewById<TextInputEditText>(R.id.editProvince).setText(intent.getStringExtra("province"))
                view.findViewById<TextInputEditText>(R.id.editRegion).setText(intent.getStringExtra("region"))
            }
            3 -> {
                view.findViewById<TextInputEditText>(R.id.editOccupation).setText(intent.getStringExtra("occupation"))
                view.findViewById<TextInputEditText>(R.id.editMedical).setText(intent.getStringExtra("medical_history"))
                view.findViewById<TextInputEditText>(R.id.editEmergencyName).setText(intent.getStringExtra("emergency_contact_name"))
                view.findViewById<TextInputEditText>(R.id.editEmergencyPhone).setText(intent.getStringExtra("emergency_contact_number"))
            }
            4 -> {
                view.findViewById<TextInputEditText>(R.id.editMemberCode).setText(intent.getStringExtra("member_code"))
            }
        }
        
        // Re-apply branding to the new page
        val themeColorStr = GymManager.getThemeColor(requireContext())
        if (!themeColorStr.isNullOrEmpty()) {
            val themeColor = Color.parseColor(themeColorStr)
            tintTextsByColor(view as ViewGroup, Color.parseColor("#A855F7"), themeColor)
            tintInputLayouts(view, themeColor)
        }
    }

    private fun setupBasicPageListeners(view: View) {
        val editBirth = view.findViewById<TextInputEditText>(R.id.editBirthDate)
        val editSex = view.findViewById<AutoCompleteTextView>(R.id.editSex)
        
        val sexOptions = arrayOf("Male", "Female")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sexOptions)
        editSex.setAdapter(adapter)

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
    }

    private fun applyBranding(root: View) {
        val themeColorStr = GymManager.getThemeColor(requireContext()) ?: return
        val themeColor = Color.parseColor(themeColorStr)
        val themeList = ColorStateList.valueOf(themeColor)
        
        root.findViewById<TextView>(R.id.sheetSubtitle)?.setTextColor(themeColor)
        tabLayout.setSelectedTabIndicatorColor(themeColor)
        btnSave.backgroundTintList = themeList
        pbSaving.indeterminateTintList = themeList

        // Optional: Modernize Discard button to be less "Hard Red" if it clashes
        root.findViewById<MaterialButton>(R.id.btnCancelEdit)?.apply {
            // Keep it red but maybe slightly more theme-aware or just consistent
            setTextColor(Color.parseColor("#EF4444")) 
        }
        
        // Fix Background Reflection: 
        // Ensure the background color from tenant settings is actually used
        val bgColorStr = GymManager.getBgColor(requireContext())
        if (!bgColorStr.isNullOrEmpty()) {
            try {
                val bgColor = Color.parseColor(bgColorStr)
                val shape = android.graphics.drawable.GradientDrawable()
                shape.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                shape.setColor(bgColor)
                val radius = (28 * resources.displayMetrics.density)
                shape.cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
                root.background = shape
            } catch (e: Exception) {
                // Fallback if color string is invalid
            }
        }
    }

    private fun tintTextsByColor(view: ViewGroup, targetColor: Int, themeColor: Int) {
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (child is TextView && child.currentTextColor == targetColor) {
                child.setTextColor(themeColor)
            } else if (child is ViewGroup) {
                tintTextsByColor(child, targetColor, themeColor)
            }
        }
    }

    private fun tintInputLayouts(view: View, themeColor: Int) {
        if (view is TextInputLayout) {
            view.boxStrokeColor = themeColor
            view.hintTextColor = ColorStateList.valueOf(themeColor)
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                tintInputLayouts(view.getChildAt(i), themeColor)
            }
        }
    }

    private fun submitProfileUpdates(updates: Map<String, Any>) {
        val ctx = context ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                pbSaving.visibility = View.VISIBLE
                btnSave.isEnabled = false
                btnSave.alpha = 0.5f
            }

            try {
                val api = RetrofitClient.getApi(GymManager.getBypassCookie(ctx), GymManager.getBypassUA(ctx))
                val response = api.updateProfile(updates)

                withContext(Dispatchers.Main) {
                    pbSaving.visibility = View.GONE
                    btnSave.isEnabled = true
                    btnSave.alpha = 1.0f

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(ctx, "Member Profile updated successfully!", Toast.LENGTH_LONG).show()
                        (activity as? LandingActivity)?.updateUserData(updates.mapValues { it.value.toString() })
                        onSavedListener?.invoke()
                        dismiss()
                    } else {
                        Toast.makeText(ctx, response.body()?.message ?: "Update failed", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbSaving.visibility = View.GONE
                    btnSave.isEnabled = true
                    btnSave.alpha = 1.0f
                    Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    inner class ProfilePagerAdapter : RecyclerView.Adapter<ProfilePagerAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layoutRes = when (viewType) {
                0 -> R.layout.page_profile_basic
                1 -> R.layout.page_profile_contact
                2 -> R.layout.page_profile_residence
                3 -> R.layout.page_profile_health
                else -> R.layout.page_profile_account
            }
            val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            pageViews[position] = holder.itemView
            prefillPage(position, holder.itemView)
        }

        override fun getItemViewType(position: Int): Int = position
        override fun getItemCount(): Int = 5

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            it.setBackgroundResource(android.R.color.transparent)
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
