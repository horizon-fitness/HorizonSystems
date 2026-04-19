package com.example.horizonsystems

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
        val cardHomePlan: MaterialCardView = view.findViewById(R.id.cardHomePlan)
        val cvHomePlanBadge: MaterialCardView = view.findViewById(R.id.cvHomePlanBadge)
        val tvHomePlanBadge: TextView = view.findViewById(R.id.tvHomePlanBadge)
        val tvHomePlanName: TextView = view.findViewById(R.id.tvHomePlanName)
        val tvHomePlanPrice: TextView = view.findViewById(R.id.tvHomePlanPrice)
        val tvHomePlanCycle: TextView = view.findViewById(R.id.tvHomePlanCycle)
        val tvHomePlanDesc: TextView = view.findViewById(R.id.tvHomePlanDesc)
        val layoutFeatures: LinearLayout = view.findViewById(R.id.layoutHomePlanFeatures)
        val btnSelectPlan: MaterialButton = view.findViewById(R.id.btnHomeSelectPlan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plan_horizontal, parent, false)
        return PlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val plan = plans[position]
        val context = holder.itemView.context
        
        // Dynamic Branding Fetch
        val themeColorStr = GymManager.getThemeColor(context)
        val iconColorStr = GymManager.getIconColor(context)
        val textColorStr = GymManager.getTextColor(context)
        val cardColorStr = GymManager.getCardColor(context)
        val isAutoCard = GymManager.getAutoCardTheme(context) == "1"

        val themeColor = try { Color.parseColor(themeColorStr) } catch (e: Exception) { Color.parseColor("#8c2bee") }
        val iconColor = try { Color.parseColor(iconColorStr) } catch (e: Exception) { Color.parseColor("#A1A1AA") }
        val textColor = try { Color.parseColor(textColorStr) } catch (e: Exception) { Color.parseColor("#D1D5DB") }
        
        val cardSurface = if (isAutoCard) {
            Color.argb(13, Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor))
        } else {
            try { Color.parseColor(cardColorStr) } catch(e: Exception) { Color.parseColor("#141216") }
        }

        // 1. Surfacing
        holder.cardHomePlan.setCardBackgroundColor(cardSurface)
        holder.cardHomePlan.setStrokeColor(ColorStateList.valueOf(themeColor).withAlpha(40))

        // 2. Plan Info
        holder.tvHomePlanName.text = plan.name
        holder.tvHomePlanPrice.text = NumberFormat.getInstance(Locale.US).format(plan.price)
        holder.tvHomePlanCycle.text = plan.billingCycle ?: "/term"
        holder.tvHomePlanDesc.text = plan.description ?: "Unlock premium gym access."
        
        // 3. Badge Logic
        if (!plan.badgeText.isNullOrEmpty()) {
            holder.cvHomePlanBadge.visibility = View.VISIBLE
            holder.tvHomePlanBadge.text = plan.badgeText
            holder.cvHomePlanBadge.setCardBackgroundColor(ColorStateList.valueOf(themeColor).withAlpha(40))
        } else {
            holder.cvHomePlanBadge.visibility = View.GONE
        }

        // 4. Features
        holder.layoutFeatures.removeAllViews()
        val featuresList = plan.features?.split("\n", ",")?.filter { it.isNotBlank() } ?: emptyList()
        featuresList.take(4).forEach { feature -> // Limited list for horizontal cleanliness
            val featuredRow = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, null) as TextView
            featuredRow.apply {
                text = feature.trim()
                setTextColor(textColor)
                alpha = 0.8f
                textSize = 12f
                setPadding(0, 12, 0, 12)
                
                // Check Icon
                val checkIcon = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_check_circle)
                checkIcon?.setTint(iconColor)
                checkIcon?.setBounds(0, 0, 42, 42)
                setCompoundDrawablesRelative(checkIcon, null, null, null)
                compoundDrawablePadding = 20
            }
            holder.layoutFeatures.addView(featuredRow)
        }

        // 5. Action Button
        if (!canPurchase) {
            holder.btnSelectPlan.text = "LOCKED"
            holder.btnSelectPlan.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#333333"))
            holder.btnSelectPlan.setTextColor(Color.GRAY)
            holder.btnSelectPlan.setOnClickListener {
                android.widget.Toast.makeText(context, "You already have an active membership.", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            holder.btnSelectPlan.text = "Select ${plan.name}"
            holder.btnSelectPlan.backgroundTintList = ColorStateList.valueOf(themeColor)
            holder.btnSelectPlan.setTextColor(Color.WHITE)
            holder.btnSelectPlan.setOnClickListener { onPlanSelected(plan) }
        }
    }

    override fun getItemCount(): Int = plans.size
}
