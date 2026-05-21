package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.example.horizonsystems.utils.GymManager
import com.example.horizonsystems.utils.ThemeUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper
    private lateinit var btnBack: ImageButton
    private lateinit var btnNext: MaterialButton
    private lateinit var stepIndicator: TextView
    private lateinit var weightEdit: android.widget.EditText
    private lateinit var targetWeightEdit: android.widget.EditText
    private lateinit var heightEdit: android.widget.EditText
    private lateinit var otherInjuryInputLayout: View
    private lateinit var otherInjuryEdit: android.widget.EditText
    private var isRestoring = false

    // User Data
    private var experienceLevel = ""
    private var weeklyCommitment = ""
    private var equipment = ""
    private var injuries = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding)

        val rootLayout = findViewById<View>(R.id.rootLayout)
        val cachedBg = GymManager.getBgColor(this)
        if (!cachedBg.isNullOrEmpty()) {
            try { rootLayout.setBackgroundColor(android.graphics.Color.parseColor(cachedBg)) } catch(e: Exception) {}
        }
        ThemeUtils.applyThemeToActivity(this)

        viewFlipper = findViewById(R.id.viewFlipper)
        btnBack = findViewById(R.id.btnBack)
        btnNext = findViewById(R.id.btnNext)
        stepIndicator = findViewById(R.id.onboardingStepIndicator)
        weightEdit = findViewById(R.id.weightEdit)
        targetWeightEdit = findViewById(R.id.targetWeightEdit)
        heightEdit = findViewById(R.id.heightEdit)
        otherInjuryInputLayout = findViewById(R.id.otherInjuryInputLayout)
        otherInjuryEdit = findViewById(R.id.otherInjuryEdit)

        btnBack.setOnClickListener {
            if (viewFlipper.displayedChild > 0) {
                viewFlipper.showPrevious()
                updateStepIndicator()
            } else {
                finish()
            }
        }

        btnNext.setOnClickListener {
            val currentChild = viewFlipper.displayedChild
            when (currentChild) {
                0 -> {
                    if (experienceLevel.isEmpty()) {
                        Toast.makeText(this, "Please select your experience level", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
                1 -> {
                    if (weeklyCommitment.isEmpty()) {
                        Toast.makeText(this, "Please select your weekly commitment", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
                2 -> {
                    // Step 3 (index 2) is physical metrics. This is optional, so we allow skipping!
                }
                3 -> {
                    if (equipment.isEmpty()) {
                        Toast.makeText(this, "Please select your equipment availability", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
                4 -> {
                    if (injuries.isEmpty()) {
                        Toast.makeText(this, "Please select an option or 'None'", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    if (injuries == "Others" && otherInjuryEdit.text.toString().trim().isEmpty()) {
                        Toast.makeText(this, "Please specify your limitation", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
            }

            if (viewFlipper.displayedChild < viewFlipper.childCount - 1) {
                viewFlipper.showNext()
                updateStepIndicator()
            } else {
                // Navigate to RegisterActivity
                val intent = Intent(this, RegisterActivity::class.java)
                
                val finalInjuries = if (injuries == "Others") {
                    otherInjuryEdit.text.toString().trim()
                } else {
                    injuries
                }
                
                // Pass the collected data to registration
                intent.putExtra("experience_level", experienceLevel)
                intent.putExtra("weekly_commitment", weeklyCommitment)
                intent.putExtra("equipment", equipment)
                intent.putExtra("injuries", finalInjuries)
                
                val currentWeight = weightEdit.text.toString().toDoubleOrNull()
                val targetWeight = targetWeightEdit.text.toString().toDoubleOrNull()
                val heightCm = heightEdit.text.toString().toDoubleOrNull()
                
                if (currentWeight != null) intent.putExtra("current_weight", currentWeight)
                if (targetWeight != null) intent.putExtra("target_weight", targetWeight)
                if (heightCm != null) intent.putExtra("height_cm", heightCm)
                
                startActivity(intent)
                finish()
            }
        }
        
        findViewById<View>(R.id.btnSkip)?.setOnClickListener {
            weightEdit.text?.clear()
            targetWeightEdit.text?.clear()
            heightEdit.text?.clear()
            viewFlipper.showNext()
            updateStepIndicator()
        }

        findViewById<View>(R.id.btnSignBack)?.setOnClickListener {
            finish()
        }
        
        setupSelectionLogic()
        setupGymLogo()
        
        val startStep = intent.getIntExtra("start_step", 1)
        if (startStep == 5) {
            isRestoring = true
            viewFlipper.displayedChild = 4 // step 5 is index 4
            
            experienceLevel = intent.getStringExtra("experience_level") ?: ""
            weeklyCommitment = intent.getStringExtra("weekly_commitment") ?: ""
            equipment = intent.getStringExtra("equipment") ?: ""
            injuries = intent.getStringExtra("injuries") ?: ""
            
            val cw = intent.getDoubleExtra("current_weight", -1.0)
            val tw = intent.getDoubleExtra("target_weight", -1.0)
            val hc = intent.getDoubleExtra("height_cm", -1.0)
            
            if (cw >= 0) weightEdit.setText(cw.toString())
            if (tw >= 0) targetWeightEdit.setText(tw.toString())
            if (hc >= 0) heightEdit.setText(hc.toString())
            
            highlightSavedSelections()
            isRestoring = false
        }
        
        updateStepIndicator()
        applyThemeColors()
    }

    private fun highlightSavedSelections() {
        // Experience Level
        when (experienceLevel) {
            "Beginner" -> findViewById<View>(R.id.cardBeginner)?.performClick()
            "Intermediate" -> findViewById<View>(R.id.cardIntermediate)?.performClick()
            "Advanced" -> findViewById<View>(R.id.cardAdvanced)?.performClick()
        }
        // Weekly Commitment
        when (weeklyCommitment) {
            "1-2 Days" -> findViewById<View>(R.id.card1to2)?.performClick()
            "3-4 Days" -> findViewById<View>(R.id.card3to4)?.performClick()
            "5+ Days" -> findViewById<View>(R.id.card5plus)?.performClick()
        }
        // Equipment
        when (equipment) {
            "Full Gym" -> findViewById<View>(R.id.cardFullGym)?.performClick()
            "Home Gym" -> findViewById<View>(R.id.cardHomeGym)?.performClick()
            "Bodyweight" -> findViewById<View>(R.id.cardBodyweight)?.performClick()
        }
        // Injuries
        when (injuries) {
            "None" -> findViewById<View>(R.id.cardNoInjuries)?.performClick()
            "Lower Back" -> findViewById<View>(R.id.cardLowerBack)?.performClick()
            "Knee" -> findViewById<View>(R.id.cardKnee)?.performClick()
            "Shoulder" -> findViewById<View>(R.id.cardShoulder)?.performClick()
            else -> if (injuries.isNotEmpty()) {
                findViewById<View>(R.id.cardOtherInjury)?.performClick()
                otherInjuryEdit.setText(injuries)
            }
        }
    }

    private fun setupGymLogo() {
        val gymLogoContainer = findViewById<View>(R.id.gymLogoContainer)
        val onboardingGymLogo = findViewById<android.widget.ImageView>(R.id.onboardingGymLogo)
        val logoUrl = GymManager.getGymLogo(this)

        if (!logoUrl.isNullOrEmpty()) {
            gymLogoContainer.visibility = View.VISIBLE
            GymManager.loadLogo(this, logoUrl, onboardingGymLogo)
        } else {
            val gymName = GymManager.getGymName(this)
            if (gymName.isNotEmpty() && gymName != "HORIZON SYSTEMS") {
                gymLogoContainer.visibility = View.VISIBLE
                onboardingGymLogo.setImageResource(R.drawable.ic_dumbbell)
                try {
                    val accent = android.graphics.Color.parseColor(GymManager.getThemeColor(this))
                    onboardingGymLogo.imageTintList = android.content.res.ColorStateList.valueOf(accent)
                } catch (e: Exception) {}
            }
        }
    }

    private fun updateStepIndicator() {
        val currentStep = viewFlipper.displayedChild + 1
        val totalSteps = viewFlipper.childCount
        
        val stepNames = arrayOf("Experience", "Commitment", "Metrics", "Equipment", "Injuries")
        val stepName = if (viewFlipper.displayedChild < stepNames.size) stepNames[viewFlipper.displayedChild] else ""
        
        stepIndicator.text = "Step $currentStep of $totalSteps: $stepName"

        if (currentStep == totalSteps) {
            btnNext.text = "Create Account"
        } else {
            btnNext.text = "Next Step"
        }

        findViewById<View>(R.id.onboardingFooter)?.visibility = if (currentStep == 1) View.VISIBLE else View.GONE
        findViewById<View>(R.id.btnSkip)?.visibility = if (currentStep == 3) View.VISIBLE else View.GONE
    }

    private fun setupSelectionLogic() {
        // Step 1: Experience
        setupCardGroup(
            listOf(R.id.cardBeginner, R.id.cardIntermediate, R.id.cardAdvanced),
            listOf("Beginner", "Intermediate", "Advanced")
        ) { selected -> experienceLevel = selected }

        // Step 2: Commitment
        setupCardGroup(
            listOf(R.id.card1to2, R.id.card3to4, R.id.card5plus),
            listOf("1-2 Days", "3-4 Days", "5+ Days")
        ) { selected -> weeklyCommitment = selected }

        // Step 4: Equipment
        setupCardGroup(
            listOf(R.id.cardFullGym, R.id.cardHomeGym, R.id.cardBodyweight),
            listOf("Full Gym", "Home Gym", "Bodyweight")
        ) { selected -> equipment = selected }

        // Step 5: Injuries
        setupCardGroup(
            listOf(R.id.cardNoInjuries, R.id.cardLowerBack, R.id.cardKnee, R.id.cardShoulder, R.id.cardOtherInjury),
            listOf("None", "Lower Back", "Knee", "Shoulder", "Others")
        ) { selected ->
            injuries = selected
            if (selected == "Others") {
                otherInjuryInputLayout.visibility = View.VISIBLE
                otherInjuryEdit.requestFocus()
            } else {
                otherInjuryInputLayout.visibility = View.GONE
                otherInjuryEdit.text?.clear()
            }
        }
    }

    private fun setupCardGroup(cardIds: List<Int>, values: List<String>, onSelected: (String) -> Unit) {
        val cards = cardIds.map { findViewById<MaterialCardView>(it) }
        val defaultBg = android.graphics.Color.parseColor("#1AFFFFFF")
        var accentColor = android.graphics.Color.parseColor("#8c2bee")
        try {
            accentColor = android.graphics.Color.parseColor(GymManager.getThemeColor(this))
        } catch (e: Exception) {}

        for (i in cards.indices) {
            cards[i].setOnClickListener {
                cards.forEach { it.setCardBackgroundColor(defaultBg); it.strokeWidth = 0 }
                cards[i].setCardBackgroundColor(accentColor)
                cards[i].strokeWidth = 4
                cards[i].strokeColor = android.graphics.Color.WHITE
                onSelected(values[i])
            }
        }
    }

    private fun applyThemeColors() {
        try {
            val color = android.graphics.Color.parseColor(GymManager.getThemeColor(this))
            btnNext.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            stepIndicator.setTextColor(color)
            findViewById<TextView>(R.id.btnSignBack)?.setTextColor(color)
        } catch (e: Exception) {}
    }
}
