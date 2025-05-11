package com.momentum.habittracker.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.Date

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getCurrentDate(): String {
        return dateFormat.format(Calendar.getInstance().time)
    }

    fun getDateString(date: Date): String {
        return dateFormat.format(date)
    }

    fun getDayName(date: String): String {
        val calendar = Calendar.getInstance().apply {
            time = dateFormat.parse(date) ?: return ""
        }
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
    }

    fun getDisplayDate(date: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return try {
            outputFormat.format(inputFormat.parse(date) ?: return date)
        } catch (e: Exception) {
            date
        }
    }
}