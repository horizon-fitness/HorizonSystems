package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import android.widget.CheckBox
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import androidx.fragment.app.Fragment
import com.example.horizonsystems.utils.ThemeUtils
import com.example.horizonsystems.utils.GymManager
import com.example.horizonsystems.utils.DialogUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class WorkoutFragment : Fragment() {

    // Stats State Variables
    private var activeMinutes = 45
    private var loggedCount = 4
    private var caloriesBurned = 350

    // UI References
    private lateinit var tvMinutes: TextView
    private lateinit var tvLogged: TextView
    private lateinit var tvCalories: TextView
    
    private lateinit var chkEx1: CheckBox
    private lateinit var chkEx2: CheckBox
    private lateinit var chkEx3: CheckBox
    private lateinit var chkEx4: CheckBox

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_workout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI stats references
        tvMinutes = view.findViewById(R.id.tvWorkoutStatMinutes)
        tvLogged = view.findViewById(R.id.tvWorkoutStatCompleted)
        tvCalories = view.findViewById(R.id.tvWorkoutStatCalories)

        // Checkboxes
        chkEx1 = view.findViewById(R.id.chkExercise1)
        chkEx2 = view.findViewById(R.id.chkExercise2)
        chkEx3 = view.findViewById(R.id.chkExercise3)
        chkEx4 = view.findViewById(R.id.chkExercise4)

        // Setup dynamic interactive behavior for check-list items
        val checkListener = { _: View ->
            updateLoggedCountFromCheckboxes()
        }
        chkEx1.setOnClickListener(checkListener)
        chkEx2.setOnClickListener(checkListener)
        chkEx3.setOnClickListener(checkListener)
        chkEx4.setOnClickListener(checkListener)

        // Handle Log Routine Button Click
        view.findViewById<MaterialButton>(R.id.btnLogRoutine)?.setOnClickListener {
            // Count completed checklist exercises
            val completedList = listOf(chkEx1, chkEx2, chkEx3, chkEx4).filter { it.isChecked }
            if (completedList.isEmpty()) {
                Toast.makeText(context, "Please complete at least one exercise to log the session!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Increment stats on routine log
            activeMinutes += 30
            caloriesBurned += 220
            
            tvMinutes.text = activeMinutes.toString()
            tvCalories.text = caloriesBurned.toString()

            // Reset checklist
            chkEx1.isChecked = false
            chkEx2.isChecked = false
            chkEx3.isChecked = false
            chkEx4.isChecked = false
            updateLoggedCountFromCheckboxes()

            DialogUtils.showConfirmationDialog(
                requireContext(),
                "Workout Logged!",
                "Great job! You have logged today's routine successfully. +30 Min Active, +220 Calories Burned!"
            )
        }

        // Handle Add Custom Exercise Button Click
        val etExerciseName = view.findViewById<TextInputEditText>(R.id.etExerciseName)
        val etSets = view.findViewById<TextInputEditText>(R.id.etSets)
        val etReps = view.findViewById<TextInputEditText>(R.id.etReps)

        view.findViewById<MaterialButton>(R.id.btnAddCustomLog)?.setOnClickListener {
            val name = etExerciseName?.text?.toString()?.trim() ?: ""
            val setsStr = etSets?.text?.toString()?.trim() ?: ""
            val repsStr = etReps?.text?.toString()?.trim() ?: ""

            if (name.isEmpty() || setsStr.isEmpty() || repsStr.isEmpty()) {
                Toast.makeText(context, "Please fill in all custom exercise details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sets = setsStr.toIntOrNull()
            val reps = repsStr.toIntOrNull()

            if (sets == null || reps == null || sets <= 0 || reps <= 0) {
                Toast.makeText(context, "Sets and Reps must be valid positive numbers", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Success adding custom exercise log
            loggedCount++
            tvLogged.text = loggedCount.toString()

            // Visual feedback Dialog
            DialogUtils.showConfirmationDialog(
                requireContext(),
                "Exercise Logged!",
                "Successfully logged custom exercise: $name ($sets sets x $reps reps)!"
            )

            // Clear inputs
            etExerciseName.setText("")
            etSets.setText("")
            etReps.setText("")
        }

        // Apply theme/branding
        applyBranding(view)
        ThemeUtils.applyThemeToView(view)
    }

    private fun updateLoggedCountFromCheckboxes() {
        val completedCount = listOf(chkEx1, chkEx2, chkEx3, chkEx4).count { it.isChecked }
        loggedCount = 4 + completedCount
        tvLogged.text = loggedCount.toString()
    }

    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        val bgColorStr = GymManager.getBgColor(ctx)
        
        if (!themeColorStr.isNullOrEmpty()) {
            try {
                val themeColor = android.graphics.Color.parseColor(themeColorStr)
                view.findViewById<TextView>(R.id.tvWorkoutHeaderSubtitle)?.setTextColor(themeColor)
                view.findViewById<TextView>(R.id.tvRoutineFocus)?.setTextColor(themeColor)

                // Cards stroke color
                val themeColorStateList = android.content.res.ColorStateList.valueOf(themeColor)
                val strokeColor = themeColorStateList.withAlpha(50) // Subtle branded border

                val cards = listOf(R.id.cardTodayRoutine, R.id.cardAddCustom)
                cards.forEach { id ->
                    val card = view.findViewById<MaterialCardView>(id)
                    card?.setStrokeColor(strokeColor)
                }

                // Log buttons background tints
                val btnLogRoutine = view.findViewById<MaterialButton>(R.id.btnLogRoutine)
                val btnAddCustomLog = view.findViewById<MaterialButton>(R.id.btnAddCustomLog)
                btnLogRoutine?.backgroundTintList = themeColorStateList
                btnAddCustomLog?.backgroundTintList = themeColorStateList

            } catch (e: Exception) {}
        }

        if (!bgColorStr.isNullOrEmpty()) {
            try {
                val bgColor = android.graphics.Color.parseColor(bgColorStr)
                view.findViewById<View>(R.id.workoutRoot)?.setBackgroundColor(bgColor)
            } catch (e: Exception) {}
        }
    }
}
