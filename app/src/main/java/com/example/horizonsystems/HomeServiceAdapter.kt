package com.example.horizonsystems

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.GymService
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.Locale

class HomeServiceAdapter(
    private val services: List<GymService>,
    private val onServiceSelected: (GymService) -> Unit
) : RecyclerView.Adapter<HomeServiceAdapter.HomeServiceViewHolder>() {

    class HomeServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardHomeService: MaterialCardView = view.findViewById(R.id.cardHomeService)
        val tvServiceName: TextView = view.findViewById(R.id.tvServiceName)
        val tvServicePrice: TextView = view.findViewById(R.id.tvServicePrice)
        val tvServicePricePrefix: TextView = view.findViewById(R.id.tvServicePricePrefix)
        val tvServicePriceSuffix: TextView = view.findViewById(R.id.tvServicePriceSuffix)
        val tvServiceDescription: TextView = view.findViewById(R.id.tvServiceDescription)
        val cvHomeServiceBadge: MaterialCardView = view.findViewById(R.id.cvHomeServiceBadge)
        val btnBookServiceAction: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btnBookServiceAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeServiceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_home_service, parent, false)
        return HomeServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeServiceViewHolder, position: Int) {
        val service = services[position]
        val context = holder.itemView.context
        
        // Dynamic Branding
        val themeColorStr = GymManager.getThemeColor(context)
        val themeColor = try { Color.parseColor(themeColorStr) } catch (e: Exception) { Color.parseColor("#8c2bee") }
        
        val isAutoCard = GymManager.getAutoCardTheme(context) == "1"
        val cardColorStr = GymManager.getCardColor(context)
        val cardSurface = if (isAutoCard) {
            Color.argb(13, Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor))
        } else {
            try { Color.parseColor(cardColorStr) } catch(e: Exception) { Color.parseColor("#141216") }
        }

        // 1. Data Population
        holder.tvServiceName.text = service.serviceName
        holder.tvServicePrice.text = NumberFormat.getInstance(Locale.US).format(service.price)
        holder.tvServiceDescription.text = if (!service.description.isNullOrBlank()) service.description else "No description"
        
        // 2. Branding Application
        holder.btnBookServiceAction.backgroundTintList = ColorStateList.valueOf(themeColor)
        holder.cvHomeServiceBadge.setCardBackgroundColor(ColorStateList.valueOf(themeColor).withAlpha(40))
        holder.cardHomeService.setCardBackgroundColor(cardSurface)
        holder.cardHomeService.setStrokeColor(ColorStateList.valueOf(themeColor).withAlpha(40))

        // 3. Click Logic
        holder.itemView.setOnClickListener { onServiceSelected(service) }
        holder.btnBookServiceAction.setOnClickListener { onServiceSelected(service) }
    }

    override fun getItemCount() = services.size
}
