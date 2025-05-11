package com.momentum.habittracker.activities

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.momentum.habittracker.R
import com.momentum.habittracker.database.Habit
import com.momentum.habittracker.database.HabitRepository
import com.momentum.habittracker.databinding.ActivityAddEditHabitBinding
import com.momentum.habittracker.notifications.ReminderReceiver
import com.momentum.habittracker.utils.ThemeUtils
import kotlinx.coroutines.launch
import java.util.Calendar

class AddEditHabitActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEditHabitBinding
    private val habitRepository by lazy { HabitRepository(applicationContext) }
    private var habitId: String = ""
    private var existingHabit: Habit? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.applyTheme(this)
        binding = ActivityAddEditHabitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupFrequencySpinner()
        setupCategorySpinner()
        setupTimePicker()

        habitId = intent.getStringExtra("HABIT_ID") ?: ""
        if (habitId.isNotEmpty()) {
            loadHabitData()
        }

        binding.buttonSave.setOnClickListener {
            saveHabit()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupFrequencySpinner() {
        val frequencies = resources.getStringArray(R.array.habit_frequencies)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, frequencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFrequency.adapter = adapter
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.habit_categories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupTimePicker() {
        binding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            binding.timePickerReminder.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        binding.timePickerReminder.setIs24HourView(true)
    }

    private fun loadHabitData() {
        lifecycleScope.launch {
            habitRepository.getAllHabits().collect { habits ->
                existingHabit = habits.firstOrNull { it.id == habitId }
                existingHabit?.let { habit ->
                    binding.editTextTitle.setText(habit.name)
                    binding.editTextDescription.setText(habit.description)
                    binding.spinnerCategory.setSelection(getCategoryPosition(habit.category))
                    binding.spinnerFrequency.setSelection(getFrequencyPosition(habit.frequency))

                    habit.reminderTime?.let { reminderTime ->
                        binding.switchReminder.isChecked = true
                        val timeParts = reminderTime.split(":")
                        binding.timePickerReminder.hour = timeParts[0].toInt()
                        binding.timePickerReminder.minute = timeParts[1].toInt()
                    }
                }
            }
        }
    }

    private fun getCategoryPosition(category: String): Int {
        val categories = resources.getStringArray(R.array.habit_categories)
        return categories.indexOf(category)
    }

    private fun getFrequencyPosition(frequency: String): Int {
        val frequencies = resources.getStringArray(R.array.habit_frequencies)
        return frequencies.indexOf(frequency)
    }

    private fun saveHabit() {
        val name = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()
        val frequency = binding.spinnerFrequency.selectedItem.toString()

        val reminderTime = if (binding.switchReminder.isChecked) {
            "${binding.timePickerReminder.hour}:${binding.timePickerReminder.minute}"
        } else {
            null
        }

        if (name.isEmpty()) {
            binding.editTextTitle.error = "Title is required"
            return
        }

        val habit = if (habitId.isEmpty()) {
            Habit(
                name = name,
                description = description,
                category = category,
                frequency = frequency,
                reminderTime = reminderTime
            )
        } else {
            existingHabit?.copy(
                name = name,
                description = description,
                category = category,
                frequency = frequency,
                reminderTime = reminderTime
            ) ?: return
        }

        lifecycleScope.launch {
            try {
                if (habitId.isEmpty()) {
                    habitRepository.saveHabit(habit)
                } else {
                    // For updates, we need to implement update functionality in repository
                    // Currently just re-adding as new document
                    habitRepository.saveHabit(habit)
                }

                // Schedule reminder if needed
                if (reminderTime != null) {
                    scheduleReminder(habit)
                }

                finish()
            } catch (e: Exception) {
                // Handle error (show toast, etc.)
                e.printStackTrace()
            }
        }
    }

    private fun scheduleReminder(habit: Habit) {
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("HABIT_TITLE", habit.name)
            putExtra("HABIT_ID", habit.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            habit.id.hashCode(), // Using hashCode for unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, binding.timePickerReminder.hour)
            set(Calendar.MINUTE, binding.timePickerReminder.minute)
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
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}