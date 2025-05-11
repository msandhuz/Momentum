package com.momentum.habittracker.activities

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.momentum.habittracker.adapters.CalendarAdapter
import com.momentum.habittracker.database.HabitRepository
import com.momentum.habittracker.databinding.ActivityCalendarBinding
import com.momentum.habittracker.utils.ThemeUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private lateinit var calendarAdapter: CalendarAdapter
    private val habitRepository by lazy { HabitRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.applyTheme(this)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCalendar()
        observeHabits()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Calendar View"
        }
    }

    private fun setupCalendar() {
        val calendar = Calendar.getInstance()
        calendarAdapter = CalendarAdapter(
            this,
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.YEAR)
        ) { date ->
            showHabitsForDate(date)
        }

        binding.gridViewCalendar1.adapter = calendarAdapter
        updateMonthTitle()

        binding.buttonPrevMonth1.setOnClickListener {
            calendarAdapter.prevMonth()
            updateMonthTitle()
        }

        binding.buttonNextMonth1.setOnClickListener {
            calendarAdapter.nextMonth()
            updateMonthTitle()
        }
    }

    private fun updateMonthTitle() {
        binding.textViewMonth1.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            .format(Calendar.getInstance().apply {
                set(calendarAdapter.displayYear, calendarAdapter.displayMonth, 1)
            }.time)
    }

    private fun observeHabits() {
        lifecycleScope.launch {
            habitRepository.getAllHabits().collect { habits ->
                calendarAdapter.updateCompletionDates(
                    habits.flatMap { habit ->
                        habit.completionDates ?: emptyList()
                    }.distinct()
                )
            }
        }
    }

    private fun showHabitsForDate(date: String) {
        lifecycleScope.launch {
            habitRepository.getAllHabits().collect { habits ->
                val habitsForDate = habits.filter { habit ->
                    habit.completionDates?.contains(date) ?: false
                }

                if (habitsForDate.isNotEmpty()) {
                    AlertDialog.Builder(this@CalendarActivity)
                        .setTitle("Habits on $date")
                        .setMessage(habitsForDate.joinToString("\n") { it.name })
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .show()
                } else {
                    Toast.makeText(
                        this@CalendarActivity,
                        "No habits completed on $date",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            onBackPressedDispatcher.onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}