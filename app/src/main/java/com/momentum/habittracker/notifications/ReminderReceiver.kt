package com.momentum.habittracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.momentum.habittracker.database.HabitRepository
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("HABIT_ID") ?: return
        val habitTitle = intent.getStringExtra("HABIT_TITLE") ?: return

        // Show notification
        NotificationHelper.showHabitReminder(context, habitId, habitTitle)

        // Reschedule for next day if needed
        runBlocking {
            val habitRepository = HabitRepository(context) // Use context instead of 'this'
            val habit = habitRepository.getHabitById(habitId)
            habit?.reminderTime?.let { reminderTime ->
                try {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                    // Check for exact alarm permission on Android 12+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (!alarmManager.canScheduleExactAlarms()) {
                            Log.w("ReminderReceiver", "Cannot schedule exact alarms - missing permission")
                            return@runBlocking
                        }
                    }

                    val newIntent = Intent(context, ReminderReceiver::class.java).apply {
                        putExtra("HABIT_ID", habitId)
                        putExtra("HABIT_TITLE", habit.name)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        habitId.hashCode(),
                        newIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val timeParts = reminderTime.split(":")
                    if (timeParts.size != 2) return@runBlocking

                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = System.currentTimeMillis()
                        add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                        set(Calendar.MINUTE, timeParts[1].toInt())
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        }
                    } catch (securityException: SecurityException) {
                        Log.e("ReminderReceiver", "Failed to schedule exact alarm", securityException)
                    }
                } catch (e: Exception) {
                    Log.e("ReminderReceiver", "Error scheduling reminder", e)
                }
            }
        }
    }
}