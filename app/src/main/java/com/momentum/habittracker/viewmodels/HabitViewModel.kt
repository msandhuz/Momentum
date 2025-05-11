package com.momentum.habittracker.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.momentum.habittracker.database.Habit
import com.momentum.habittracker.database.HabitRepository
import kotlinx.coroutines.launch

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val habitRepository = HabitRepository(application)
    val habits = habitRepository.getAllHabits()

    fun saveHabit(name: String, frequency: String) {
        viewModelScope.launch {
            habitRepository.saveHabit(Habit(name = name, frequency = frequency))
        }
    }
}