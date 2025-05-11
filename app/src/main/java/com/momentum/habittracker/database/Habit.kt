package com.momentum.habittracker.database

import android.icu.text.SimpleDateFormat
import java.util.*

data class Habit(
    val id: String = "", // Document ID from Firestore
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val frequency: String = "Daily", // Default value
    val streak: Int = 0,
    val reminderTime: String? = null, // Format: "HH:mm"
    val completionDates: List<String> = emptyList(), // Format: "yyyy-MM-dd"
    val createdAt: Date = Date() // Auto-set on creation
) {
    // Helper function to check if completed today
    fun isCompletedToday(): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Calendar.getInstance().time)
        return completionDates.contains(today)
    }
}