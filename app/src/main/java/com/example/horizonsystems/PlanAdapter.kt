package com.example.horizonsystems

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.MembershipPlan
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.Locale

class PlanAdapter(
    private val plans: List<MembershipPlan>,
    private val canPurchase: Boolean = true,
    private val onPlanSelected: (MembershipPlan) -> Unit
) : RecyclerView.Adapter<PlanAdapter.PlanViewHolder>() {

    class PlanViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardPlan: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.cardPlan)
        val tvPlanName: TextView = view.findViewById(R.id.tvPlanName)
        val tvBillingCycle: TextView = view.findViewById(R.id.tvBillingCycle)
        val tvPlanPrice: TextView = view.findViewById(R.id.tvPlanPrice)
        val layoutFeatures: LinearLayout = view.findViewById(R.id.layoutPlanFeatures)
        val btnChoosePlan: MaterialButton = view.findViewById(R.id.btnChoosePlan)
        val planDivider: View = view.findViewById(R.id.planDivider)
        val tvPlanBadge: TextView = view.findViewById(R.id.tvPlanBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_membership_plan, parent, false)
        return PlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val plan = plans[position]
        val context = holder.itemView.context
        val themeColorStr = GymManager.getThemeColor(context)
        val themeColor = try { Color.parseColor(themeColorStr) } catch (e: Exception) { Color.parseColor("#A855F7") }

        holder.tvPlanName.text = plan.name
        holder.tvBillingCycle.text = plan.billingCycle ?: "Recurring Billing"
        holder.tvBillingCycle.setTextColor(themeColor)
        
        val formatter = NumberFormat.getInstance(Locale.US)
        holder.tvPlanPrice.text = formatter.format(plan.price)

        // Card Appearance Synchronization
        val cardColorStr = GymManager.getCardColor(context)
        val isAutoCard = GymManager.getAutoCardTheme(context) == "1"
        
        val cardColor = if (isAutoCard) {
            // Replicate web logic: themeColor with 5% alpha
            val r = Color.red(themeColor)
            val g = Color.green(themeColor)
            val b = Color.blue(themeColor)
            Color.argb(13, r, g, b)
        } else {
            try { Color.parseColor(cardColorStr) } catch(e: Exception) { Color.parseColor("#0D0D10") }
        }

        // Featured Card Styling (No Scaling to prevent clipping & ensure mobile feel)
        if (!plan.badgeText.isNullOrEmpty()) {
            holder.tvPlanBadge.text = plan.badgeText
            holder.tvPlanBadge.visibility = View.VISIBLE
            holder.tvPlanBadge.setBackgroundTintList(ColorStateList.valueOf(themeColor))
            
            holder.cardPlan.setStrokeColor(ColorStateList.valueOf(themeColor))
            holder.cardPlan.setStrokeWidth(4) // Thicker border for featured
            
            // For featured cards, we still use the synchronized background
            holder.cardPlan.setCardBackgroundColor(ColorStateList.valueOf(cardColor))
        } else {
            holder.tvPlanBadge.visibility = View.GONE
            holder.cardPlan.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#1AFFFFFF")))
            holder.cardPlan.setStrokeWidth(2)
            holder.cardPlan.setCardBackgroundColor(ColorStateList.valueOf(cardColor))
        }

        // Branding
        holder.planDivider.setBackgroundColor(themeColor)
        holder.btnChoosePlan.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#1AFFFFFF")))
        
        // Features
        holder.layoutFeatures.removeAllViews()
        val featuresList = plan.features?.split("\n", ",")?.filter { it.isNotBlank() } ?: emptyList()
        featuresList.forEach { feature ->
            val tvFeature = TextView(context).apply {
                text = feature.trim()
                setTextColor(Color.parseColor("#9CA3AF")) // Gray 400
                textSize = 12f
                alpha = 1.0f
                setPadding(0, 0, 0, 10)
                gravity = android.view.Gravity.CENTER_VERTICAL
                
                // Add Check Circle Icon
                val checkIcon = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_check_circle)
                checkIcon?.setTint(themeColor)
                checkIcon?.setBounds(0, 0, 42, 42) // Approx 16dp
                setCompoundDrawablesRelative(checkIcon, null, null, null)
                compoundDrawablePadding = 16
            }
            holder.layoutFeatures.addView(tvFeature)
        }

        holder.btnChoosePlan.setOnClickListener { 
            if (canPurchase) onPlanSelected(plan) 
        }
        
        if (!canPurchase) {
            holder.btnChoosePlan.isEnabled = false
            holder.btnChoosePlan.text = "PLAN ALREADY ACTIVE"
            holder.btnChoosePlan.alpha = 0.5f
            holder.btnChoosePlan.setIconResource(0) // Hide icon
        } else {
            holder.btnChoosePlan.isEnabled = true
            holder.btnChoosePlan.alpha = 1.0f
            holder.btnChoosePlan.text = "SELECT THIS PLAN"
            holder.btnChoosePlan.setIconResource(R.drawable.ic_check_circle)
        }

        // Button Hover/Click Effect Simulation via State
        holder.btnChoosePlan.setIconTint(ColorStateList.valueOf(themeColor))
        if (canPurchase) {
            holder.btnChoosePlan.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        v.setBackgroundColor(themeColor)
                        (v as MaterialButton).setIconTint(ColorStateList.valueOf(Color.WHITE))
                        v.setStrokeColor(ColorStateList.valueOf(themeColor))
                        v.setTextColor(Color.WHITE)
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        v.setBackgroundColor(Color.parseColor("#1AFFFFFF"))
                        (v as MaterialButton).setIconTint(ColorStateList.valueOf(themeColor))
                        v.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#1AFFFFFF")))
                        v.setTextColor(Color.WHITE)
                    }
                }
                false
            }
        } else {
            holder.btnChoosePlan.setOnTouchListener(null)
        }
    }

    override fun getItemCount() = plans.size
}
