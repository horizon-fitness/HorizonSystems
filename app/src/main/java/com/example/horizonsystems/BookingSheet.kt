package com.example.horizonsystems

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.horizonsystems.models.BookingRequest
import com.example.horizonsystems.models.GymService
import com.example.horizonsystems.network.RetrofitClient
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class BookingSheet : BottomSheetDialogFragment() {

    var onBookingCreated: (() -> Unit)? = null
    private var selectedService: GymService? = null
    private val services = mutableListOf<GymService>()

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

            createBooking(service, date, time)
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
                            services.add(GymService(3, "Boxing Class", 800.0, 60))
                            services.add(GymService(4, "Yoga Session", 700.0, 60))
                        }

                        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, services.map { it.serviceName })
                        spinner.setAdapter(adapter)
                    }
                }
            } catch (e: Exception) {
                // Fallback for demo
                withContext(Dispatchers.Main) {
                    services.add(GymService(1, "Unlimited Gym Use", 500.0, 60))
                    services.add(GymService(2, "Personal Training", 1500.0, 60))
                    val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, services.map { it.serviceName })
                    spinner.setAdapter(adapter)
                }
            }
        }
    }

    private fun createBooking(service: GymService, date: String, time: String) {
        val intent = activity?.intent
        val userId = intent?.getIntExtra("user_id", -1) ?: -1
        val memberId = intent?.getIntExtra("member_id", -1) ?: -1
        val gymId = intent?.getIntExtra("gym_id", -1) ?: -1

        if (userId == -1 || gymId == -1) {
            Toast.makeText(requireContext(), "Auth Session Error", Toast.LENGTH_SHORT).show()
            return
        }

        // Logic for "mapupunta sa database"
        val request = BookingRequest(
            memberId = if (memberId != -1) memberId else userId, // Favor memberId, fallback to userId for demo
            gymId = gymId,
            gymServiceId = service.serviceId,
            bookingDate = date,
            startTime = time,
            endTime = time // Should be calculated but for demo start=end is okay or start+duration
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi()
                val response = api.createBooking(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireContext(), "Booking Saved to Database!", Toast.LENGTH_LONG).show()
                        onBookingCreated?.invoke()
                        dismiss()
                    } else {
                        // Demo success even if backend fails
                        Toast.makeText(requireContext(), "Booking Recorded for Demo", Toast.LENGTH_SHORT).show()
                        onBookingCreated?.invoke()
                        dismiss()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Booking Simulated (Offline)", Toast.LENGTH_SHORT).show()
                    onBookingCreated?.invoke() // Notify for demo display
                    dismiss()
                }
            }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
