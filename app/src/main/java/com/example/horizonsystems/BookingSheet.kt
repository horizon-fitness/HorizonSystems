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
    private var currentBasePrice = 100.0
    private var currentCoachFee = 0.0

    private lateinit var txtTotalPrice: TextView
    private lateinit var txtCoachFeeInfo: TextView
    private lateinit var editDuration: MaterialAutoCompleteTextView

    private val paymentResultLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(requireContext(), "Payment Successful! Booking Confirmed.", Toast.LENGTH_LONG).show()
            onBookingCreated?.invoke()
            dismiss()
        } else {
            Toast.makeText(requireContext(), "Payment Cancelled or Failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.sheet_booking, container, false)

        val editService = view.findViewById<MaterialAutoCompleteTextView>(R.id.bookService)
        val editCoach = view.findViewById<MaterialAutoCompleteTextView>(R.id.bookCoach)
        editDuration = view.findViewById(R.id.bookDuration)
        
        txtTotalPrice = view.findViewById(R.id.txtTotalPrice)
        txtCoachFeeInfo = view.findViewById(R.id.txtCoachFeeInfo)

        // Force dropdown on click
        editService.setOnClickListener { editService.showDropDown() }
        editCoach.setOnClickListener { editCoach.showDropDown() }
        editDuration.setOnClickListener { editDuration.showDropDown() }

        val editDate = view.findViewById<TextInputEditText>(R.id.bookDate)
        val editTime = view.findViewById<TextInputEditText>(R.id.bookTime)
        val btnSubmit = view.findViewById<View>(R.id.btnSubmitBooking)

        // Duration Setup
        val durations = listOf("1 Hour", "2 Hours", "3 Hours")
        editDuration.setAdapter(ArrayAdapter(requireContext(), R.layout.item_dropdown, durations))
        editDuration.setText("1 Hour", false)
        editDuration.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) { updatePrice() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Date Picker
        editDate.setOnClickListener {
            val constraintsBuilder = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Booking Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder.build())
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                editDate.setText(format.format(calendar.time))
            }
            datePicker.show(childFragmentManager, "BOOK_DATE_PICKER")
        }

        // Time Picker
        editTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, hour, minute ->
                // Rule: 7 AM (07:00) to 10 PM (22:00)
                if (hour < 7 || hour > 22 || (hour == 22 && minute > 0)) {
                    Toast.makeText(requireContext(), "Bookings are only available from 7 AM to 10 PM", Toast.LENGTH_LONG).show()
                    return@TimePickerDialog
                }
                
                val displayCalendar = Calendar.getInstance()
                displayCalendar.set(Calendar.HOUR_OF_DAY, hour)
                displayCalendar.set(Calendar.MINUTE, minute)
                val displayFormat = SimpleDateFormat("hh:mm a", Locale.US)
                editTime.setText(displayFormat.format(displayCalendar.time))
                editTime.tag = String.format("%02d:%02d:00", hour, minute)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        // Service Selection Logic
        editService.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val serviceName = s?.toString() ?: ""
                val isPT = serviceName.contains("Personal Training", ignoreCase = true)
                
                if (isPT) {
                    editCoach.text.clear()
                    fetchCoaches(editCoach, skipSelfTrain = true)
                } else {
                    selectedCoachId = null
                    editCoach.setText("General Workout (Self-Train)", false)
                    currentCoachFee = 0.0
                    updatePrice()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        editCoach.setOnItemClickListener { parent, _, position, _ ->
            val coachName = parent.getItemAtPosition(position) as String
            if (coachName.contains("Self-Train", ignoreCase = true)) {
                selectedCoachId = null
                currentCoachFee = 0.0
            } else {
                selectedCoachId = coaches.find { "${it.firstName} ${it.lastName}" == coachName }?.coachId
                currentCoachFee = 120.0
            }
            updatePrice()
        }

        btnSubmit.setOnClickListener {
            val date = editDate.text.toString()
            val time = editTime.tag?.toString() ?: ""
            val serviceName = editService.text.toString()
            if (serviceName.isEmpty() || date.isEmpty() || time.isEmpty()) {
                Toast.makeText(requireContext(), "Fill all booking details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val service = services.find { it.serviceName == serviceName } ?: GymService(1, serviceName, 100.0, 60)
            checkAvailabilityAndPay(service, date, time)
        }
        
        fetchServices(editService)
        updatePrice()
        
        ThemeUtils.applyThemeToView(view)

        return view
    }

    private fun updatePrice() {
        val durationText = editDuration.text.toString()
        val hours = durationText.filter { it.isDigit() }.toIntOrNull() ?: 1
        
        val total = (currentBasePrice + currentCoachFee) * hours
        txtTotalPrice.text = "₱%.2f".format(total)
        
        if (currentCoachFee > 0) {
            val themeColor = GymManager.getThemeColor(requireContext())
            txtCoachFeeInfo.text = "+₱120.00 COACH FEE / HR"
            txtCoachFeeInfo.setTextColor(android.graphics.Color.parseColor(themeColor))
        } else {
            txtCoachFeeInfo.text = "NO COACH FEE"
            txtCoachFeeInfo.setTextColor(android.graphics.Color.parseColor("#10B981"))
        }
    }

    private fun checkAvailabilityAndPay(service: GymService, date: String, time: String) {
        val context = requireContext()
        val userId = com.example.horizonsystems.utils.GymManager.getUserId(context)
        val gymId = com.example.horizonsystems.utils.GymManager.getTenantId(context)
        val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(context)
        val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(context)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.checkBookingAvailability(userId, gymId, date, time)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.available == true) {
                        initiatePayment(service, date, time)
                    } else {
                        Toast.makeText(context, response.body()?.message ?: "Session unavailable.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { initiatePayment(service, date, time) }
            }
        }
    }

    private fun fetchCoaches(spinner: MaterialAutoCompleteTextView, skipSelfTrain: Boolean = false) {
        val context = requireContext()
        val gymId = com.example.horizonsystems.utils.GymManager.getTenantId(context)
        val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(context)
        val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(context)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.getGymCoaches(gymId)
                withContext(Dispatchers.Main) {
                    coaches.clear()
                    val names = mutableListOf<String>()
                    if (!skipSelfTrain) names.add("General Workout (Self-Train)")
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.coaches?.let { 
                            coaches.addAll(it)
                            names.addAll(it.map { c -> "${c.firstName} ${c.lastName}" })
                        }
                    }
                    spinner.setAdapter(ArrayAdapter(context, R.layout.item_dropdown, names))
                }
            } catch (e: Exception) { Log.e("Booking", "Coach error: ${e.message}") }
        }
    }

    private fun fetchServices(spinner: MaterialAutoCompleteTextView) {
        val context = requireContext()
        val gymId = com.example.horizonsystems.utils.GymManager.getTenantId(context)
        val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(context)
        val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(context)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.getGymServices(gymId)
                withContext(Dispatchers.Main) {
                    services.clear()
                    if (response.isSuccessful) response.body()?.let { services.addAll(it) }
                    
                    if (services.isEmpty()) {
                        services.add(GymService(1, "Unlimited Gym Use", 100.0, 60))
                        services.add(GymService(2, "Personal Training", 150.0, 60))
                    }
                    spinner.setAdapter(ArrayAdapter(context, R.layout.item_dropdown, services.map { it.serviceName }))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    services.add(GymService(1, "Unlimited Gym Use", 100.0, 60))
                    services.add(GymService(2, "Personal Training", 150.0, 60))
                    spinner.setAdapter(ArrayAdapter(context, R.layout.item_dropdown, services.map { it.serviceName }))
                }
            }
        }
    }

    private fun initiatePayment(service: GymService, date: String, time: String) {
        val context = requireContext()
        val userId = com.example.horizonsystems.utils.GymManager.getUserId(context)
        val gymId = com.example.horizonsystems.utils.GymManager.getTenantId(context)
        
        val durationText = editDuration.text.toString()
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
                        val intentPayMongo = Intent(requireContext(), PayMongoActivity::class.java).apply {
                            putExtra("checkout_url", res.body()!!.data!!.attributes.checkoutUrl)
                        }
                        paymentResultLauncher.launch(intentPayMongo)
                    } else { Toast.makeText(requireContext(), "Payment Error", Toast.LENGTH_LONG).show() }
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "Network Error", Toast.LENGTH_SHORT).show() } }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
