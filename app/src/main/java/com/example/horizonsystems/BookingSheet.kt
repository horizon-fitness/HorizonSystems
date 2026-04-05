package com.example.horizonsystems

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import android.widget.LinearLayout
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class BookingSheet : BottomSheetDialogFragment() {

    var onBookingCreated: (() -> Unit)? = null
    private val services = mutableListOf<GymService>()
    private val coaches = mutableListOf<Coach>()
    private var selectedCoachId: Int? = null

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

        val editService = view.findViewById<AutoCompleteTextView>(R.id.bookService)
        val editCoach = view.findViewById<AutoCompleteTextView>(R.id.bookCoach)
        val layoutCoach = view.findViewById<LinearLayout>(R.id.layoutCoach)
        val editDate = view.findViewById<TextInputEditText>(R.id.bookDate)
        val editTime = view.findViewById<TextInputEditText>(R.id.bookTime)
        val btnSubmit = view.findViewById<View>(R.id.btnSubmitBooking)
        val btnCancel = view.findViewById<View>(R.id.btnCancelBooking)

        // Date Picker (Disable Past Dates)
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

        // Time Picker (12-Hour Format)
        editTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, hour, minute ->
                val displayCalendar = Calendar.getInstance()
                displayCalendar.set(Calendar.HOUR_OF_DAY, hour)
                displayCalendar.set(Calendar.MINUTE, minute)
                
                // For user display: 02:00 PM
                val displayFormat = SimpleDateFormat("hh:mm a", Locale.US)
                editTime.setText(displayFormat.format(displayCalendar.time))
                
                // Tag it with the 24h format for API later
                editTime.tag = String.format("%02d:%02d:00", hour, minute)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        // Service Matcher for Coach Selection
        editService.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val isPT = s?.toString()?.contains("Personal Training", ignoreCase = true) == true
                layoutCoach.visibility = if (isPT) View.VISIBLE else View.GONE
                if (isPT) fetchCoaches(editCoach)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        editCoach.setOnItemClickListener { parent, _, position, _ ->
            val coachName = parent.getItemAtPosition(position) as String
            selectedCoachId = coaches.find { "${it.firstName} ${it.lastName}" == coachName }?.coachId
        }

        // Fetch Services
        fetchServices(editService)

        btnCancel.setOnClickListener { dismiss() }

        btnSubmit.setOnClickListener {
            val date = editDate.text.toString()
            val time = editTime.tag?.toString() ?: "" // Use the 24h tag
            val serviceName = editService.text.toString()
            val isPT = serviceName.contains("Personal Training", ignoreCase = true)

            if (serviceName.isEmpty() || date.isEmpty() || time.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all booking details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isPT && selectedCoachId == null) {
                Toast.makeText(requireContext(), "Please select a coach for Personal Training", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val service = services.find { it.serviceName == serviceName }
            if (service == null) {
                Toast.makeText(requireContext(), "Invalid service selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            initiatePayment(service, date, time)
        }

        return view
    }

    private fun fetchCoaches(spinner: AutoCompleteTextView) {
        val gymId = activity?.intent?.getIntExtra("gym_id", -1) ?: -1
        if (gymId == -1) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi()
                val response = api.getGymCoaches(gymId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        coaches.clear()
                        response.body()?.coaches?.let { coaches.addAll(it) }
                        
                        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, coaches.map { "${it.firstName} ${it.lastName}" })
                        spinner.setAdapter(adapter)
                    }
                }
            } catch (e: Exception) {
                // Ignore silent
            }
        }
    }

    private fun fetchServices(spinner: AutoCompleteTextView) {
        val gymId = activity?.intent?.getIntExtra("gym_id", -1) ?: -1
        if (gymId == -1) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi()
                val response = api.getGymServices(gymId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        services.clear()
                        response.body()?.let { services.addAll(it) }
                        
                        // Fake services for demo if DB is empty
                        if (services.isEmpty()) {
                            services.add(GymService(1, "Unlimited Gym Use", 100.0, 60))
                            services.add(GymService(2, "Personal Training", 150.0, 60))
                        }

                        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, services.map { it.serviceName })
                        spinner.setAdapter(adapter)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    services.add(GymService(1, "Unlimited Gym Use", 100.0, 60))
                    services.add(GymService(2, "Personal Training", 150.0, 60))
                    val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, services.map { it.serviceName })
                    spinner.setAdapter(adapter)
                }
            }
        }
    }

    private fun initiatePayment(service: GymService, date: String, time: String) {
        val intent = activity?.intent
        val userId = intent?.getIntExtra("user_id", -1) ?: -1
        val gymId = intent?.getIntExtra("gym_id", -1) ?: -1
        val userEmail = intent?.getStringExtra("email") ?: "customer@horizonsystems.com"
        val userName = (intent?.getStringExtra("first_name") ?: "") + " " + (intent?.getStringExtra("last_name") ?: "")
        val userPhone = intent?.getStringExtra("contact_number") ?: "09170000000"

        if (userId == -1 || gymId == -1) {
            Toast.makeText(requireContext(), "Auth Session Error", Toast.LENGTH_SHORT).show()
            return
        }

        // Amount calculation as per request: 150 for Personal Training, 100 for others
        val isPersonalTraining = service.serviceName.contains("Personal Training", ignoreCase = true)
        val amountPesos = if (isPersonalTraining) 150 else 100
        val amountCentavos = amountPesos * 100
        val amountDecimal = "%.2f".format(amountPesos.toDouble())

        // Security: Generate Signature for Backend Verification
        val salt = "FitPlatform_Secure_2026!"
        // Sig format: gym_id + user_id + service_id + date + time + amount + coach_id (if any) + salt
        val coachIdPart = if (selectedCoachId != null) selectedCoachId.toString() else ""
        val sigInput = "$gymId$userId${service.serviceId}$date$time$amountDecimal$coachIdPart$salt"
        val sig = MessageDigest.getInstance("SHA-256")
            .digest(sigInput.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val baseUrl = "https://horizonfitnesscorp.gt.tc/api"
        val successUrl = "$baseUrl/booking_success_redirect.php?gym_id=$gymId&user_id=$userId&service_id=${service.serviceId}&date=$date&time=$time&amount=$amountDecimal&coach_id=$coachIdPart&sig=$sig"

        val checkoutRequest = CheckoutSessionRequest(
            data = CheckoutData(
                attributes = CheckoutAttributes(
                    successUrl = successUrl,
                    cancelUrl = "$baseUrl/payment_cancel.php",
                    billing = Billing(
                        name = if (userName.trim().isNotEmpty()) userName else "Horizon Member",
                        email = userEmail,
                        phone = userPhone
                    ),
                    lineItems = listOf(LineItem(amount = amountCentavos, name = "Gym Booking: ${service.serviceName}")),
                    description = "Booking Payment for ${service.serviceName} on $date"
                )
            )
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val payApi = PayMongoApi.create()
                val response = payApi.createCheckoutSession(PayMongoApi.getAuthHeader(), checkoutRequest)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        val checkoutUrl = response.body()!!.data!!.attributes.checkoutUrl
                        val intentPayMongo = Intent(requireContext(), PayMongoActivity::class.java).apply {
                            putExtra("checkout_url", checkoutUrl)
                        }
                        paymentResultLauncher.launch(intentPayMongo)
                    } else {
                        Log.e("PayMongo", "API Error: ${response.errorBody()?.string()}")
                        Toast.makeText(requireContext(), "Payment Gateway Error", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("PayMongo", "Network Exception: ${e.message}")
                    Toast.makeText(requireContext(), "Connection Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
