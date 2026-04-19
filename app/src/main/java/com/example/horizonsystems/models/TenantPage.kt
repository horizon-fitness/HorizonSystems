package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class TenantPage(
    @SerializedName("page_id") val pageId: Int?,
    @SerializedName("gym_id") val gymId: Int?,
    @SerializedName("tenant_code") val tenantCode: String?,
    @SerializedName("page_slug") val pageSlug: String?,
    @SerializedName("page_title") val pageTitle: String?,
    @SerializedName("logo_path") val logoPath: String?,
    @SerializedName("theme_color") val themeColor: String?,
    @SerializedName("icon_color") val iconColor: String?,
    @SerializedName("text_color") val textColor: String?,
    @SerializedName("bg_color") val bgColor: String?,
    @SerializedName("font_family") val fontFamily: String?,
    @SerializedName("about_text") val aboutText: String?,
    @SerializedName("card_color") val cardColor: String?,
    @SerializedName("auto_card_theme") val autoCardTheme: String?,

    @SerializedName("contact_text") val contactText: String?,
    @SerializedName("app_download_link") val appDownloadLink: String?,
    @SerializedName("banner_image") val bannerImage: String?,
    @SerializedName("gym_name") val gymName: String?,
    @SerializedName("gym_email") val gymEmail: String?,
    @SerializedName("gym_contact") val gymContact: String?,
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("is_suspended") val isSuspended: Boolean?,
    @SerializedName("is_fallback") val isFallback: Boolean?,
    @SerializedName("opening_time") val openingTime: String?,
    @SerializedName("closing_time") val closingTime: String?
)
