package com.momentum.habittracker.activities

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.momentum.habittracker.R
import com.momentum.habittracker.database.HabitRepository
import com.momentum.habittracker.databinding.ActivityStatisticsBinding
import com.momentum.habittracker.utils.DateUtils
import com.momentum.habittracker.utils.ThemeUtils
import kotlinx.coroutines.launch
import java.util.*

class StatisticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatisticsBinding
    private val habitRepository by lazy { HabitRepository(applicationContext) }
    private val categoryDataMap = mutableMapOf<String, Int>()
    private val weeklyCompletionMap = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeUtils.applyTheme(this)

        setupToolbar()
        setupCharts()
        observeData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Statistics"
        }
    }

    private fun setupCharts() {
        binding.pieChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
        }

        binding.barChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisRight.isEnabled = false
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            // Observe all habits for category breakdown
            habitRepository.getAllHabits().collect { habits ->
                updateCategoryData(habits)
                updateWeeklyCompletionData(habits)
            }
        }
    }

    private fun updateCategoryData(habits: List<com.momentum.habittracker.database.Habit>) {
        categoryDataMap.clear()
        resources.getStringArray(R.array.habit_categories).forEach { category ->
            val count = habits.count { it.category == category }
            categoryDataMap[category] = count
        }
        updateCategoryChart()
    }

    private fun updateWeeklyCompletionData(habits: List<com.momentum.habittracker.database.Habit>) {
        weeklyCompletionMap.clear()
        (0..6).map {
            DateUtils.getDateString(
                Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -it) }.time
            )
        }.forEach { date ->
            val count = habits.count { habit ->
                habit.completionDates?.contains(date) ?: false
            }
            weeklyCompletionMap[date] = count
        }
        updateWeeklyCompletionChart()
    }

    private fun updateCategoryChart() {
        val entries = categoryDataMap.map { (category, count) ->
            PieEntry(count.toFloat(), category)
        }
        val dataSet = PieDataSet(entries, "Habits by Category").apply {
            colors = getCategoryColors()
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }
        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.invalidate()
    }

    private fun updateWeeklyCompletionChart() {
        val sortedDates = weeklyCompletionMap.keys.sorted()
        val entries = sortedDates.mapIndexed { index, date ->
            BarEntry(index.toFloat(), weeklyCompletionMap[date]?.toFloat() ?: 0f)
        }
        val dataSet = BarDataSet(entries, "Completed Habits").apply {
            color = ContextCompat.getColor(this@StatisticsActivity, R.color.colorPrimary)
            valueTextSize = 12f
            valueTextColor = Color.BLACK
        }
        binding.barChart.data = BarData(dataSet)
        binding.barChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return sortedDates.getOrNull(value.toInt())?.substring(5) ?: ""
            }
        }
        binding.barChart.invalidate()
    }

    private fun getCategoryColors(): List<Int> {
        return listOf(
            ContextCompat.getColor(this, R.color.colorPrimary),
            ContextCompat.getColor(this, R.color.colorAccent),
            ContextCompat.getColor(this, R.color.colorPrimaryDark),
            ContextCompat.getColor(this, R.color.teal_200),
            ContextCompat.getColor(this, R.color.purple_200)
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