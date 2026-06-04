package com.example.lostfound.util

import android.util.TypedValue
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.example.lostfound.R
import com.example.lostfound.service.SessionManager

object ThemeToggleBinding {

    fun bind(button: ImageButton, activity: AppCompatActivity) {
        button.scaleType = ImageView.ScaleType.FIT_CENTER
        button.adjustViewBounds = false
        button.cropToPadding = false
        val ripple = TypedValue()
        if (activity.theme.resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless,
                ripple,
                true
            )
        ) {
            button.setBackgroundResource(ripple.resourceId)
        }
        refreshIcon(button, activity)
        button.setOnClickListener {
            val sessionManager = SessionManager(activity)
            ThemeHelper.setDarkMode(activity, !sessionManager.isDarkMode())
        }
    }

    fun refreshIcon(button: ImageButton, activity: AppCompatActivity) {
        val isDark = SessionManager(activity).isDarkMode()
        if (isDark) {
            // Flipped icon: filled side points toward light mode.
            button.setImageResource(R.drawable.ic_theme_light)
            button.scaleX = 1f
            button.contentDescription = activity.getString(R.string.switch_to_light_mode)
        } else {
            button.setImageResource(R.drawable.ic_theme_dark)
            button.scaleX = 1f
            button.contentDescription = activity.getString(R.string.switch_to_dark_mode)
        }
        ImageViewCompat.setImageTintList(button, null)
        button.invalidate()
    }

    fun refreshForFragment(fragment: Fragment) {
        val activity = fragment.activity as? AppCompatActivity ?: return
        fragment.view?.findViewById<ImageButton>(R.id.darkModeButton)?.let {
            refreshIcon(it, activity)
        }
    }

    fun refreshForActivity(activity: AppCompatActivity) {
        activity.findViewById<ImageButton>(R.id.darkModeButton)?.let {
            refreshIcon(it, activity)
        }
    }
}
