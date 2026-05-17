package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.horizonsystems.utils.ThemeUtils
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.button.MaterialButton
import java.util.Locale

class BmiFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bmi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply visual glassmorphism and dynamic branding
        ThemeUtils.applyThemeToView(view)
        applyBranding(view)
        
        // Populate default values
        loadMemberStats(view)

        // BMI Calculator Actions
        val etWeight = view.findViewById<EditText>(R.id.etCalcWeight)
        val etHeight = view.findViewById<EditText>(R.id.etCalcHeight)
        val btnCalculate = view.findViewById<MaterialButton>(R.id.btnCalculateBmi)

        btnCalculate?.setOnClickListener {
            val weightStr = etWeight?.text?.toString()?.trim()
            val heightStr = etHeight?.text?.toString()?.trim()

            if (weightStr.isNullOrEmpty() || heightStr.isNullOrEmpty()) {
                Toast.makeText(context, "Please enter both weight and height", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val weight = weightStr.toDouble()
                val heightCm = heightStr.toDouble()

                if (weight <= 0 || heightCm <= 0) {
                    Toast.makeText(context, "Height and weight must be greater than zero", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // BMI = weight (kg) / (height (m) ^ 2)
                val heightM = heightCm / 100.0
                val bmi = weight / (heightM * heightM)

                // Update visual metrics
                updateBmiDisplay(view, bmi, heightCm, weight)
                Toast.makeText(context, "Body metrics updated successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Invalid input. Please enter valid decimal numbers", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMemberStats(view: View) {
        val height = 175.0
        val weight = 68.5
        val bmi = weight / ((height / 100.0) * (height / 100.0))
        
        updateBmiDisplay(view, bmi, height, weight)
    }

    private fun updateBmiDisplay(view: View, bmi: Double, height: Double, weight: Double) {
        val tvBmiValue = view.findViewById<TextView>(R.id.tvBmiValue)
        val tvBmiHeight = view.findViewById<TextView>(R.id.tvBmiHeight)
        val tvBmiWeight = view.findViewById<TextView>(R.id.tvBmiWeight)
        val tvBmiStatusLabel = view.findViewById<TextView>(R.id.tvBmiStatusLabel)
        val tvBmiAdvice = view.findViewById<TextView>(R.id.tvBmiAdvice)

        tvBmiValue?.text = String.format(Locale.US, "%.1f", bmi)
        tvBmiHeight?.text = String.format(Locale.US, "%.1f cm", height)
        tvBmiWeight?.text = String.format(Locale.US, "%.1f kg", weight)

        val status: String
        val colorHex: String
        val advice: String

        if (bmi < 18.5) {
            status = "UNDERWEIGHT"
            colorHex = "#3B82F6" // Blue
            advice = "Your BMI suggests you are underweight. Consider focusing on nutrient-dense calorie plans and strength splits in the gym to build lean mass."
        } else if (bmi < 25.0) {
            status = "NORMAL"
            colorHex = "#10B981" // Emerald Green
            advice = "Excellent! You are in the healthy BMI weight range. Maintain your consistent training schedule, core exercises, and hydration plan!"
        } else if (bmi < 30.0) {
            status = "OVERWEIGHT"
            colorHex = "#F59E0B" // Orange
            advice = "Your BMI suggests you are in the overweight range. A structured split of active cardio sessions and high-intensity interval training (HIIT) is highly recommended."
        } else {
            status = "OBESE"
            colorHex = "#EF4444" // Red
            advice = "Your BMI suggests you are in the obese category. We recommend consultation with your trainer to structure a low-impact calorie deficit split to safely burn body fat."
        }

        tvBmiStatusLabel?.text = status
        try {
            tvBmiStatusLabel?.setTextColor(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {}
        tvBmiAdvice?.text = advice
    }

    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        
        if (!themeColorStr.isNullOrEmpty()) {
            try {
                val themeColor = android.graphics.Color.parseColor(themeColorStr)
                view.findViewById<TextView>(R.id.tvBmiHeaderSubtitle)?.setTextColor(themeColor)
                
                val btnCalculate = view.findViewById<MaterialButton>(R.id.btnCalculateBmi)
                btnCalculate?.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor)
            } catch (e: Exception) {}
        }
    }
}
