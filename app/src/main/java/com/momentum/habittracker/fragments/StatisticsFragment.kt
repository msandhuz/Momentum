package com.momentum.habittracker.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.momentum.habittracker.R
import com.momentum.habittracker.database.Habit
import com.momentum.habittracker.database.HabitRepository
import com.momentum.habittracker.databinding.FragmentStatisticsBinding
import kotlinx.coroutines.launch

class StatisticsFragment : Fragment() {
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val habitRepository by lazy { HabitRepository(requireContext()) }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()
        observeData()
    }

    private fun setupCharts() {
        binding.completionChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setHoleColor(Color.TRANSPARENT)
        }
        binding.streakChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            habitRepository.getAllHabits().collect { habits ->
                if (habits.isNotEmpty()) {
                    updateCharts(habits)
                    updateStats(habits)
                }
            }
        }
    }

    private fun updateCharts(habits: List<Habit>) {
        updateCompletionRateChart(habits)
        updateStreakDistributionChart(habits)
    }

    private fun updateCompletionRateChart(habits: List<Habit>) {
        val completedCount = habits.sumOf { it.completionDates?.size ?: 0 }
        val entries = listOf(
            PieEntry(completedCount.toFloat(), "Completed"),
            PieEntry((habits.size * 7 - completedCount).toFloat(), "Missed")
        )
        val dataSet = PieDataSet(entries, "Completion Rate").apply {
            colors = listOf(
                ContextCompat.getColor(requireContext(), R.color.colorPrimary),
                ContextCompat.getColor(requireContext(), R.color.colorAccent)
            )
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }
        binding.completionChart.data = PieData(dataSet)
        binding.completionChart.invalidate()
    }

    private fun updateStreakDistributionChart(habits: List<Habit>) {
        val streaks = habits.map { it.streak }
        val ranges = listOf(
            "1-3 days" to streaks.count { it in 1..3 },
            "4-7 days" to streaks.count { it in 4..7 },
            "8-14 days" to streaks.count { it in 8..14 },
            "15+ days" to streaks.count { it >= 15 }
        )
        val entries = ranges.mapIndexed { index, (_, count) ->
            BarEntry(index.toFloat(), count.toFloat())
        }
        val dataSet = BarDataSet(entries, "Streak Length").apply {
            color = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            valueTextSize = 12f
            valueTextColor = Color.BLACK
        }
        binding.streakChart.data = BarData(dataSet)
        binding.streakChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return ranges.getOrNull(value.toInt())?.first ?: ""
            }
        }
        binding.streakChart.invalidate()
    }

    private fun updateStats(habits: List<Habit>) {
        with(binding) {
            textViewTotalHabits.text = "Total Habits: ${habits.size}"
            textViewTotalCompletions.text = "Total Completions: ${habits.sumOf { it.completionDates?.size ?: 0 }}"
            textViewAverageStreak.text = "Average Streak: %.1f days".format(habits.map { it.streak }.average())
            textViewBestStreak.text = "Best Streak: ${habits.maxOfOrNull { it.streak } ?: 0} days"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}