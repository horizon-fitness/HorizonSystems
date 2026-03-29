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
import com.example.horizonsystems.models.*
import com.example.horizonsystems.network.RetrofitClient
import com.example.horizonsystems.network.PayMongoApi
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class BookingSheet : BottomSheetDialogFragment() {

    var onBookingCreated: (() -> Unit)? = null
    private val services = mutableListOf<GymService>()

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
        val editDate = view.findViewById<TextInputEditText>(R.id.bookDate)
        val editTime = view.findViewById<TextInputEditText>(R.id.bookTime)
        val btnSubmit = view.findViewById<View>(R.id.btnSubmitBooking)
        val btnCancel = view.findViewById<View>(R.id.btnCancelBooking)

        // Date Picker
        editDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Booking Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
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
                val timeStr = String.format("%02d:%02d:00", hour, minute)
                editTime.setText(timeStr)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        // Fetch Services
        fetchServices(editService)

        btnCancel.setOnClickListener { dismiss() }

        btnSubmit.setOnClickListener {
            val date = editDate.text.toString()
            val time = editTime.text.toString()
            val serviceName = editService.text.toString()

            if (serviceName.isEmpty() || date.isEmpty() || time.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all booking details", Toast.LENGTH_SHORT).show()
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
                            services.add(GymService(1, "Unlimited Gym Use", 500.0, 60))
                            services.add(GymService(2, "Personal Training", 1500.0, 60))
                        }

                        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, services.map { it.serviceName })
                        spinner.setAdapter(adapter)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    services.add(GymService(1, "Unlimited Gym Use", 500.0, 60))
                    services.add(GymService(2, "Personal Training", 1500.0, 60))
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
        // Sig format: gymId + userId + serviceId + date + time + amount + salt
        val sigInput = "$gymId$userId${service.serviceId}$date$time$amountDecimal$salt"
        val sig = MessageDigest.getInstance("SHA-256")
            .digest(sigInput.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val baseUrl = "https://horizonfitnesscorp.gt.tc/api"
        val successUrl = "$baseUrl/booking_success_redirect.php?gym_id=$gymId&user_id=$userId&service_id=${service.serviceId}&date=$date&time=$time&amount=$amountDecimal&sig=$sig"

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
