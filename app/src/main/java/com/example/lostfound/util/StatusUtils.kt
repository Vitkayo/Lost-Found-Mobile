package com.example.lostfound.util

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import com.example.lostfound.R

object StatusUtils {

    fun normalizeStatus(context: Context, status: String?): String {
        return when (status?.lowercase(java.util.Locale.getDefault())) {
            "lost" -> context.getString(R.string.status_lost)
            "found" -> context.getString(R.string.status_found)
            "claimed" -> context.getString(R.string.status_claimed)
            else -> status?.replaceFirstChar { it.uppercase() } ?: "Unknown"
        }
    }

    fun applyStatusBadge(context: Context, status: String?, badgeView: android.widget.TextView) {
        val normalized = normalizeStatus(context, status)
        badgeView.text = normalized

        val (bgRes, textColor) = when (status?.lowercase()) {
            "lost" -> R.drawable.bg_status_lost to R.color.status_lost_text
            "found" -> R.drawable.bg_status_found to R.color.status_found_text
            "claimed" -> R.drawable.bg_status_claimed to R.color.status_claimed_text
            else -> R.drawable.bg_status_default to R.color.on_surface_variant
        }

        badgeView.setBackgroundResource(bgRes)
        badgeView.setTextColor(ContextCompat.getColor(context, textColor))
    }

    fun matchesFilter(item: com.example.lostfound.model.Item, filter: String): Boolean {
        if (filter == "All") return true

        return when (filter) {
            "Lost" -> item.status.equals("lost", ignoreCase = true)
            "Found" -> item.status.equals("found", ignoreCase = true)
            "Electronics" -> matchesCategory(item.category, listOf("electronics", "phone", "laptop"))
            "Wallet" -> matchesCategory(item.category, listOf("wallet", "personal", "keys & wallets"))
            "Student ID" -> matchesCategory(
                item.category,
                listOf("id card", "id cards", "student id", "student id card")
            )
            "Keys" -> matchesCategory(item.category, listOf("keys", "key", "keys & wallets"))
            else -> item.category?.contains(filter, ignoreCase = true) == true ||
                item.title?.contains(filter, ignoreCase = true) == true
        }
    }

    private fun matchesCategory(category: String?, keywords: List<String>): Boolean {
        if (category.isNullOrBlank()) return false
        return keywords.any { category.contains(it, ignoreCase = true) }
    }

    fun matchesSearch(item: com.example.lostfound.model.Item, query: String): Boolean {
        if (query.isBlank()) return true
        return listOf(item.title, item.category, item.location)
            .any { it?.contains(query, ignoreCase = true) == true }
    }
}
