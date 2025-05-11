package com.momentum.habittracker.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.momentum.habittracker.R

object ThemeUtils {
    private const val PREF_THEME = "pref_theme"
    private const val THEME_LIGHT = "light"
    private const val THEME_DARK = "dark"
    private const val THEME_SYSTEM = "system"

    fun applyTheme(activity: Activity) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
        when (sharedPref.getString(PREF_THEME, THEME_SYSTEM)) {
            THEME_LIGHT -> activity.setTheme(R.style.AppTheme_Light)
            THEME_DARK -> activity.setTheme(R.style.AppTheme_Dark)
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    activity.setTheme(R.style.AppTheme_System)
                } else {
                    activity.setTheme(R.style.AppTheme_Light)
                }
            }
        }
    }

    fun setupThemePreference(preference: ListPreference, context: Context) {
        preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val themeMode = newValue.toString()
            AppCompatDelegate.setDefaultNightMode(
                when (themeMode) {
                    THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
            true
        }
    }
}