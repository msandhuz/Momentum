package com.momentum.habittracker

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.momentum.habittracker.database.Habit
import com.momentum.habittracker.database.HabitRepository
import com.momentum.habittracker.notifications.NotificationHelper
import com.momentum.habittracker.notifications.ReminderReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Calendar

class MomentumApp : Application() {
    private lateinit var habitRepository: HabitRepository
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialize repository with context
        habitRepository = HabitRepository(this)

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Schedule reminders
        scheduleReminders()
    }

    private fun scheduleReminders() {
        applicationScope.launch {
            try {
                habitRepository.getAllHabits().collect { habits ->
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    habits.forEach { habit ->
                        habit.reminderTime?.let { time ->
                            scheduleReminder(alarmManager, habit, time)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MomentumApp", "Error scheduling reminders", e)
            }
        }
    }

    private fun scheduleReminder(
        alarmManager: AlarmManager,
        habit: Habit,
        reminderTime: String
    ) {
        try {
            val intent = Intent(this, ReminderReceiver::class.java).apply {
                putExtra("HABIT_TITLE", habit.name)
                putExtra("HABIT_ID", habit.id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                habit.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val timeParts = reminderTime.split(":")
            if (timeParts.size != 2) return

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(Calendar.MINUTE, timeParts[1].toInt())
                set(Calendar.SECOND, 0)

                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e("MomentumApp", "Error scheduling reminder for ${habit.name}", e)
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }
}