package com.momentum.habittracker.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.momentum.habittracker.database.HabitRepository
import com.momentum.habittracker.databinding.ActivityHabitDetailBinding
import com.momentum.habittracker.utils.ThemeUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HabitDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHabitDetailBinding
    private val habitRepository by lazy { HabitRepository(applicationContext) }
    private var habitId: String = ""
    private var currentHabit: com.momentum.habittracker.database.Habit? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.applyTheme(this)
        binding = ActivityHabitDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        habitId = intent.getStringExtra("HABIT_ID") ?: ""
        if (habitId.isEmpty()) {
            finish()
            return
        }

        setupToolbar()
        observeHabit()
        setupButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun observeHabit() {
        lifecycleScope.launch {
            habitRepository.getAllHabits().collect { habits ->
                currentHabit = habits.firstOrNull { it.id == habitId }
                currentHabit?.let { habit ->
                    binding.textViewTitle.text = habit.name
                    binding.textViewDescription.text = habit.description
                    binding.textViewCategory.text = habit.category
                    binding.textViewFrequency.text = habit.frequency
                    binding.textViewStreak.text = "Current streak: ${habit.streak} days"

                    if (habit.reminderTime != null) {
                        binding.textViewReminder.text = "Reminder at ${habit.reminderTime}"
                    } else {
                        binding.textViewReminder.text = "No reminder set"
                    }

                    val today = getCurrentDate()
                    val isCompletedToday = habit.completionDates?.contains(today) ?: false
                    binding.buttonComplete.text = if (isCompletedToday) "Completed Today" else "Mark as Complete"
                    binding.buttonComplete.isEnabled = !isCompletedToday
                }
            }
        }
    }

    private fun setupButtons() {
        binding.buttonComplete.setOnClickListener {
            markHabitAsComplete()
        }

        binding.buttonEdit.setOnClickListener {
            val intent = Intent(this, AddEditHabitActivity::class.java).apply {
                putExtra("HABIT_ID", habitId)
            }
            startActivity(intent)
        }

        binding.buttonDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun markHabitAsComplete() {
        currentHabit?.let { habit ->
            val today = getCurrentDate()
            val updatedCompletionDates = habit.completionDates?.toMutableList() ?: mutableListOf()
            updatedCompletionDates.add(today)

            val newStreak = calculateNewStreak(updatedCompletionDates)

            val updatedHabit = habit.copy(
                completionDates = updatedCompletionDates,
                streak = newStreak
            )

            lifecycleScope.launch {
                habitRepository.saveHabit(updatedHabit) // Firebase will update existing document with same ID
                binding.buttonComplete.text = "Completed Today"
                binding.buttonComplete.isEnabled = false
                binding.textViewStreak.text = "Current streak: $newStreak days"
            }
        }
    }

    private fun calculateNewStreak(completionDates: List<String>): Int {
        val sortedDates = completionDates.sortedDescending()
        var streak = 1

        if (sortedDates.size < 2) return streak

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var previousDate = dateFormat.parse(sortedDates[0]) ?: return streak

        for (i in 1 until sortedDates.size) {
            val currentDate = dateFormat.parse(sortedDates[i]) ?: continue
            val diff = ((previousDate.time - currentDate.time) / (1000 * 60 * 60 * 24)).toInt()

            if (diff == 1) {
                streak++
                previousDate = currentDate
            } else if (diff > 1) {
                break
            }
        }

        return streak
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Habit")
            .setMessage("Are you sure you want to delete this habit?")
            .setPositiveButton("Delete") { _, _ ->
                deleteHabit()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteHabit() {
        lifecycleScope.launch {
            currentHabit?.let {
                habitRepository.deleteHabit(it.id)
                finish()
            }
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}