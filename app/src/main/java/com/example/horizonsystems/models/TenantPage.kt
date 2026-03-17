package com.example.horizonsystems.models

import com.google.gson.annotations.SerializedName

data class TenantPage(
    @SerializedName("page_id") val pageId: Int,
    @SerializedName("gym_id") val gymId: Int,
    @SerializedName("tenant_code") val tenantCode: String?,
    @SerializedName("page_slug") val pageSlug: String,
    @SerializedName("page_title") val pageTitle: String,
    @SerializedName("logo_path") val logoPath: String?,
    @SerializedName("banner_image") val bannerImage: String?,
    @SerializedName("theme_color") val themeColor: String,
    @SerializedName("bg_color") val bgColor: String?,
    @SerializedName("font_family") val fontFamily: String?,
    @SerializedName("about_text") val aboutText: String?,

    @SerializedName("contact_text") val contactText: String?,
    @SerializedName("app_download_link") val appDownloadLink: String?,
    @SerializedName("gym_name") val gymName: String?,
    @SerializedName("gym_email") val gymEmail: String?,
    @SerializedName("gym_contact") val gymContact: String?
)
