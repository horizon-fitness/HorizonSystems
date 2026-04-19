package com.example.horizonsystems

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.horizonsystems.models.*
import com.example.horizonsystems.network.RetrofitClient
import com.example.horizonsystems.network.PayMongoApi
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.textfield.TextInputEditText
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.widget.ImageView
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import com.example.horizonsystems.utils.ThemeUtils
import com.example.horizonsystems.utils.GymManager

class BookingSheet : BottomSheetDialogFragment() {

    var onBookingCreated: (() -> Unit)? = null
    private val services = mutableListOf<GymService>()
    private val coaches = mutableListOf<Coach>()
    private var selectedCoachId: Int? = null
    private var currentBasePrice = 0.0
    private var currentCoachFee = 0.0
    var preSelectedServiceId: Int? = null

    private lateinit var txtTotalPrice: TextView
    private lateinit var txtCoachFeeInfo: TextView
    private lateinit var editDuration: MaterialAutoCompleteTextView

    private val paymentResultLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val ctx = context ?: return@registerForActivityResult
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(ctx, "Payment Successful! Booking Confirmed.", Toast.LENGTH_LONG).show()
            onBookingCreated?.invoke()
            dismiss()
        } else {
            Toast.makeText(ctx, "Payment Cancelled or Failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = try {
            inflater.inflate(R.layout.sheet_booking, container, false)
        } catch (e: Exception) {
            Log.e("BookingSheet", "Inflation error: ${e.message}")
            null
        } ?: return null

        val editService = view.findViewById<MaterialAutoCompleteTextView>(R.id.bookService)
        val editCoach = view.findViewById<MaterialAutoCompleteTextView>(R.id.bookCoach)
        editDuration = view.findViewById(R.id.bookDuration)
        
        txtTotalPrice = view.findViewById(R.id.txtTotalPrice)
        txtCoachFeeInfo = view.findViewById(R.id.txtCoachFeeInfo)

        // Force dropdown on click + Instant responsiveness
        editService?.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            setOnClickListener { showDropDown() }
        }
        editCoach?.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            setOnClickListener { showDropDown() }
        }
        editDuration?.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            setOnClickListener { showDropDown() }
        }

        // Load Coaches immediately with an instant-response placeholder
        editCoach?.setAdapter(ArrayAdapter(requireContext(), R.layout.item_dropdown, listOf("Loading coaches...")))
        fetchCoaches(editCoach, skipSelfTrain = true)

        val editDate = view.findViewById<TextInputEditText>(R.id.bookDate)
        val editTime = view.findViewById<TextInputEditText>(R.id.bookTime)

        // Duration Setup
        val durations = listOf("1 Hour", "2 Hours", "3 Hours", "4 Hours", "5 Hours")
        val ctx = context ?: return view
        editDuration?.apply {
            setAdapter(ArrayAdapter(ctx, R.layout.item_dropdown, durations))
            // Limit dropdown to 3 items (~192dp height as each item is roughly 64dp)
            val itemHeight = (64 * resources.displayMetrics.density).toInt()
            dropDownHeight = itemHeight * 3
        }
        editDuration?.setText("1 Hour", false)
        editDuration?.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) { updatePrice() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Date Picker
        editDate?.setOnClickListener {
            val constraintsBuilder = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Booking Date")
                .setTheme(com.example.horizonsystems.utils.CalendarUtils.getCalendarTheme(ctx))
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder.build())
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dateStr = format.format(calendar.time)
                editDate.setText(dateStr)
                
                // Refresh coaches based on selected date (Day Off Logic)
                if (editCoach != null) {
                    editCoach.setText("", false)
                    selectedCoachId = null
                    currentCoachFee = 0.0
                    updatePrice()
                    fetchCoaches(editCoach, date = dateStr)
                }
            }
            datePicker.show(childFragmentManager, "BOOK_DATE_PICKER")
        }

        // Time Picker
        editTime?.setOnClickListener {
            val calendar = Calendar.getInstance()
            val ctx = context ?: return@setOnClickListener
            
            // Fetch dynamic gym hours
            val openingStr = GymManager.getOpeningTime(ctx) // e.g. "07:00:00"
            val closingStr = GymManager.getClosingTime(ctx) // e.g. "21:00:00"
            
            fun parseToMinutes(timeStr: String): Int {
                return try {
                    val parts = timeStr.split(":")
                    val h = parts[0].toInt()
                    val m = parts[1].toInt()
                    h * 60 + m
                } catch (e: Exception) { 0 }
            }
            
            fun formatForDisplay(timeStr: String): String {
                return try {
                    val inputFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
                    val outputFormat = SimpleDateFormat("hh:mm a", Locale.US)
                    val date = inputFormat.parse(timeStr)
                    if (date != null) outputFormat.format(date) else timeStr
                } catch (e: Exception) { timeStr }
            }

            val openMinutes = parseToMinutes(openingStr)
            val closeMinutes = parseToMinutes(closingStr)
            val displayOpen = formatForDisplay(openingStr)
            val displayClose = formatForDisplay(closingStr)

            TimePickerDialog(ctx, { _, hour, minute ->
                val selectedMinutes = hour * 60 + minute
                
                // 1. Gym Hours Validation
                if (selectedMinutes < openMinutes || selectedMinutes > closeMinutes) {
                    Toast.makeText(ctx, "Bookings are only available from $displayOpen to $displayClose", Toast.LENGTH_LONG).show()
                    return@TimePickerDialog
                }

                // 2. Past Time Validation (if Today)
                try {
                    val dateStr = editDate?.text.toString()
                    if (dateStr.isNotEmpty()) {
                        val manilaTz = TimeZone.getTimeZone("Asia/Manila")
                        val now = Calendar.getInstance(manilaTz)
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = manilaTz }.format(now.time)
                        
                        if (dateStr == todayStr) {
                            val currentMinuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                            if (selectedMinutes < currentMinuteOfDay) {
                                Toast.makeText(ctx, "You cannot book a time that has already passed today.", Toast.LENGTH_LONG).show()
                                return@TimePickerDialog
                            }
                        }
                    }
                } catch (e: Exception) {}
                
                val displayCalendar = Calendar.getInstance()
                displayCalendar.set(Calendar.HOUR_OF_DAY, hour)
                displayCalendar.set(Calendar.MINUTE, minute)
                val displayFormat = SimpleDateFormat("hh:mm a", Locale.US)
                editTime.setText(displayFormat.format(displayCalendar.time))
                editTime.tag = String.format("%02d:%02d:00", hour, minute)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        // Service Selection Logic
        editService?.setOnItemClickListener { parent, _, position, _ ->
            val serviceName = parent.getItemAtPosition(position) as String
            val selectedService = services.find { it.serviceName == serviceName }
            
            selectedService?.let {
                currentBasePrice = it.price
                updatePrice()
            }
        }

        editCoach?.setOnItemClickListener { parent, _, position, _ ->
            val coachName = parent.getItemAtPosition(position) as String
            val coach = coaches.find { "${it.firstName} ${it.lastName}" == coachName }
            selectedCoachId = coach?.coachId
            // Use the real session_rates from the coach model (0.0 if null/not set)
            currentCoachFee = coach?.sessionRates ?: 0.0
            updatePrice()
        }

        val cbAgreement = view.findViewById<android.widget.CheckBox>(R.id.cbBookingAgreement)
        val btnSubmit = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSubmitBooking)

        cbAgreement?.setOnCheckedChangeListener { _, isChecked ->
            btnSubmit?.isEnabled = isChecked
            btnSubmit?.alpha = if (isChecked) 1.0f else 0.3f
        }

        view.findViewById<View>(R.id.btnCancelBooking)?.setOnClickListener { dismiss() }

        btnSubmit?.setOnClickListener {
            val dateStr = editDate?.text.toString()
            val timeStr = editTime?.tag?.toString() ?: ""
            val serviceName = editService?.text.toString()
            
            if (serviceName.isEmpty() || dateStr.isEmpty() || timeStr.isEmpty()) {
                Toast.makeText(context ?: return@setOnClickListener, "Fill all booking details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Real-time Validation (Manila Time)
            try {
                val manilaTz = TimeZone.getTimeZone("Asia/Manila")
                val now = Calendar.getInstance(manilaTz)
                
                val selected = Calendar.getInstance(manilaTz)
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                sdf.timeZone = manilaTz
                val selectedDate = sdf.parse("$dateStr $timeStr")
                
                if (selectedDate != null) {
                    selected.time = selectedDate
                    if (selected.before(now)) {
                        Toast.makeText(context ?: return@setOnClickListener, "You cannot book a time that has already passed today.", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                }
            } catch (e: Exception) {
                Log.e("BookingSheet", "Time Validation Error: ${e.message}")
            }

            val service = services.find { it.serviceName == serviceName } ?: GymService(1, serviceName,     currentBasePrice)
            checkAvailabilityAndPay(service, dateStr, timeStr)
        }
        
        if (editService != null) fetchServices(editService)
        
        if (::txtTotalPrice.isInitialized) txtTotalPrice.text = "----"
        updatePrice()
        
        ThemeUtils.applyThemeToView(view)
        applyBranding(view)

        return view
    }


    override fun onStart() {
        super.onStart()
        // Ensure the dialog's window background is transparent so rounded corners of bg_bottom_sheet show
        dialog?.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            it.setBackgroundResource(android.R.color.transparent)
        }
    }

    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        val cardColorStr = GymManager.getCardColor(ctx)
        val bgColorStr = GymManager.getBgColor(ctx)
        val isAutoCard = GymManager.getAutoCardTheme(ctx) == "1"

        try {
            val themeColor = if (!themeColorStr.isNullOrEmpty()) android.graphics.Color.parseColor(themeColorStr) else android.graphics.Color.parseColor("#8c2bee")
            
            // 1. Titles & Buttons
            view.findViewById<TextView>(R.id.sheetSubTitle)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.tvBookingTermsHeader)?.setTextColor(themeColor)
            
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSubmitBooking)?.let {
                it.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor)
            }

            // 1b. Checkbox Tint
            view.findViewById<android.widget.CheckBox>(R.id.cbBookingAgreement)?.buttonTintList = 
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#80FFFFFF"))

            // 2. Card Appearance Synchronization
            val cardColor = if (isAutoCard) {
                val r = android.graphics.Color.red(themeColor)
                val g = android.graphics.Color.green(themeColor)
                val b = android.graphics.Color.blue(themeColor)
                android.graphics.Color.argb(13, r, g, b) 
            } else {
                try { android.graphics.Color.parseColor(cardColorStr) } catch(e: Exception) { android.graphics.Color.parseColor("#141216") }
            }

            // 2a. Apply to Root Modal Background (Solid to prevent bleed-through/ghosting)
            view.findViewById<android.widget.LinearLayout>(R.id.rootSheetContainer)?.let { root ->
                val shape = android.graphics.drawable.GradientDrawable()
                shape.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                
                // Use a solid background for the main sheet to ensure readability and fix "see-thru" bug
                val solidBg = try { android.graphics.Color.parseColor(bgColorStr) } 
                              catch(e: Exception) { android.graphics.Color.parseColor("#151518") }
                shape.setColor(solidBg)
                
                val radius = (28 * ctx.resources.displayMetrics.density)
                shape.cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
                root.background = shape
            }

            // 2b. Apply to internal card
            view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardPriceSummary)?.apply {
                setCardBackgroundColor(cardColor)
                setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1AFFFFFF")))
            }

            // 3. Empty State Branding (Matching Text Color as requested)
            // Using a muted white/secondary text style instead of the theme color
            view.findViewById<ImageView>(R.id.ivEmptyServicesBooking)?.imageTintList = 
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#80FFFFFF"))
            view.findViewById<TextView>(R.id.tvNoServicesBooking)?.setTextColor(android.graphics.Color.parseColor("#80FFFFFF"))

        } catch (e: Exception) {
            Log.e("BookingSheet", "Branding Error: ${e.message}")
        }
    }

    private fun updatePrice() {
        val durationText = if (::editDuration.isInitialized) editDuration.text.toString() else "1 Hour"
        val hours = durationText.filter { it.isDigit() }.toIntOrNull() ?: 1
        
        if (currentBasePrice > 0) {
            val total = (currentBasePrice + currentCoachFee) * hours
            if (::txtTotalPrice.isInitialized) txtTotalPrice.text = "₱%.2f".format(total)
        } else {
            if (::txtTotalPrice.isInitialized) txtTotalPrice.text = "----"
        }
        
        val themeColorStr = context?.let { GymManager.getThemeColor(it) }
        val themeColor = try {
            if (!themeColorStr.isNullOrEmpty()) android.graphics.Color.parseColor(themeColorStr) else android.graphics.Color.parseColor("#8c2bee")
        } catch (e: Exception) { android.graphics.Color.parseColor("#8c2bee") }

        if (currentCoachFee > 0) {
            if (::txtCoachFeeInfo.isInitialized) {
                txtCoachFeeInfo.text = "+₱${"%.2f".format(currentCoachFee)} COACH FEE / HR"
                txtCoachFeeInfo.setTextColor(themeColor)
            }
        } else {
            if (::txtCoachFeeInfo.isInitialized) {
                txtCoachFeeInfo.text = "NO COACH FEE"
                txtCoachFeeInfo.setTextColor(android.graphics.Color.parseColor("#10B981"))
            }
        }
    }

    private fun checkAvailabilityAndPay(service: GymService, date: String, time: String) {
        val ctx = context ?: return
        val userId = com.example.horizonsystems.utils.GymManager.getUserId(ctx)
        val gymId = com.example.horizonsystems.utils.GymManager.getTenantId(ctx)
        val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(ctx)
        val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(ctx)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.checkBookingAvailability(userId, gymId, date, time, coachId = selectedCoachId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.available == true) {
                        initiatePayment(service, date, time)
                    } else {
                        Toast.makeText(ctx, response.body()?.message ?: "Session unavailable.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { initiatePayment(service, date, time) }
            }
        }
    }

    private fun fetchCoaches(spinner: MaterialAutoCompleteTextView?, skipSelfTrain: Boolean = false, date: String? = null) {
        val ctx = context ?: return
        val spinnerView = spinner ?: return
        val gymId = com.example.horizonsystems.utils.GymManager.getTenantId(ctx)
        val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(ctx)
        val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(ctx)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.getGymCoaches(gymId, date)
                withContext(Dispatchers.Main) {
                    coaches.clear()
                    val names = mutableListOf<String>()
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val body = response.body()
                        Log.d("BookingSheet", "Fetched ${body?.coaches?.size ?: 0} coaches for gym $gymId")
                        body?.coaches?.let { 
                            coaches.addAll(it)
                            names.addAll(it.map { c -> "${c.firstName} ${c.lastName}" })
                        }
                    } else {
                        Log.e("BookingSheet", "Coach API Error: ${response.code()} - ${response.errorBody()?.string()}")
                    }

                    if (names.isEmpty()) {
                        names.add("No coaches available")
                    }
                    
                    spinnerView.setAdapter(ArrayAdapter(ctx, R.layout.item_dropdown, names))
                    // Limit dropdown height
                    val itemHeight = (64 * ctx.resources.displayMetrics.density).toInt()
                    spinnerView.dropDownHeight = if (names.size > 3) itemHeight * 3 else android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                }
            } catch (e: Exception) { Log.e("Booking", "Coach error: ${e.message}") }
        }
    }

    private fun fetchServices(spinner: MaterialAutoCompleteTextView) {
        val ctx = context ?: return
        val gymId = com.example.horizonsystems.utils.GymManager.getTenantId(ctx)
        val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(ctx)
        val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(ctx)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.getGymServices(gymId)
                withContext(Dispatchers.Main) {
                    services.clear()
                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("BookingSheet", "Fetched ${body?.size ?: 0} services for gym $gymId")
                        body?.let { services.addAll(it) }
                    } else {
                        Log.e("BookingSheet", "API Response Error: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                    
                    val emptyState = view?.findViewById<View>(R.id.emptyStateServicesBooking)
                    if (services.isEmpty()) {
                        Log.w("BookingSheet", "No services available in the catalog for gymId: $gymId")
                        emptyState?.visibility = View.VISIBLE
                        spinner.hint = "No services available"
                    } else {
                        emptyState?.visibility = View.GONE
                    }
                    
                    spinner.setAdapter(ArrayAdapter(ctx, R.layout.item_dropdown, services.map { it.serviceName }))
                    
                    // Auto-select if pre-selected ID is set
                    preSelectedServiceId?.let { id ->
                        val selectedService = services.find { it.serviceId == id }
                        if (selectedService != null) {
                            spinner.setText(selectedService.serviceName, false)
                            currentBasePrice = selectedService.price
                            updatePrice()
                        }
                    }

                    // Limit dropdown height
                    val itemHeight = (64 * ctx.resources.displayMetrics.density).toInt()
                    spinner.dropDownHeight = if (services.size > 3) itemHeight * 3 else android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("BookingSheet", "API Error fetching services: ${e.message}")
                    spinner.setAdapter(ArrayAdapter(ctx, R.layout.item_dropdown, emptyList<String>()))
                    val itemHeight = (64 * ctx.resources.displayMetrics.density).toInt()
                    spinner.dropDownHeight = itemHeight * 3
                }
            }
        }
    }

    private fun initiatePayment(service: GymService, date: String, time: String) {
        val ctx = context ?: return
        val userId = com.example.horizonsystems.utils.GymManager.getUserId(ctx)
        val gymId = com.example.horizonsystems.utils.GymManager.getTenantId(ctx)
        
        val durationText = if (::editDuration.isInitialized) editDuration.text.toString() else "1 Hour"
        val hours = durationText.filter { it.isDigit() }.toIntOrNull() ?: 1
        val amountPesos = (currentBasePrice + currentCoachFee) * hours
        val amountCentavos = (amountPesos * 100).toInt()
        val amountDecimal = "%.2f".format(amountPesos)

        val salt = "FitPlatform_Secure_2026!"
        val coachIdPart = if (selectedCoachId != null) selectedCoachId.toString() else ""
        val sigInput = "$gymId$userId${service.serviceId}$date$time$amountDecimal$coachIdPart$salt"
        val sig = MessageDigest.getInstance("SHA-256").digest(sigInput.toByteArray()).joinToString("") { "%02x".format(it) }

        val baseUrl = "https://horizonfitnesscorp.gt.tc/api"
        val successUrl = "$baseUrl/booking_success_redirect.php?gym_id=$gymId&user_id=$userId&service_id=${service.serviceId}&date=$date&time=$time&amount=$amountDecimal&coach_id=$coachIdPart&sig=$sig"

        val checkoutRequest = CheckoutSessionRequest(
            data = CheckoutData(
                attributes = CheckoutAttributes(
                    successUrl = successUrl,
                    cancelUrl = "$baseUrl/payment_cancel.php",
                    billing = Billing(
                        name = (activity?.intent?.getStringExtra("first_name") ?: "Member"),
                        email = activity?.intent?.getStringExtra("email") ?: "customer@horizonsystems.com",
                        phone = activity?.intent?.getStringExtra("contact_number") ?: "09170000000"
                    ),
                    lineItems = listOf(LineItem(amount = amountCentavos, name = "Booking: ${service.serviceName} (${hours}Hr)")),
                    description = "Booking for ${service.serviceName} on $date"
                )
            )
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val payApi = PayMongoApi.create()
                val res = payApi.createCheckoutSession(PayMongoApi.getAuthHeader(), checkoutRequest)
                withContext(Dispatchers.Main) {
                    if (res.isSuccessful && res.body()?.data != null) {
                        val intentPayMongo = Intent(ctx, PayMongoActivity::class.java).apply {
                            putExtra("checkout_url", res.body()!!.data!!.attributes.checkoutUrl)
                        }
                        paymentResultLauncher.launch(intentPayMongo)
                    } else { Toast.makeText(ctx, "Payment Error", Toast.LENGTH_LONG).show() }
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { Toast.makeText(ctx, "Network Error", Toast.LENGTH_SHORT).show() } }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
