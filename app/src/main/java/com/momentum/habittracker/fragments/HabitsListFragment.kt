package com.momentum.habittracker.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.momentum.habittracker.R
import com.momentum.habittracker.activities.HabitDetailActivity
import com.momentum.habittracker.adapters.HabitsAdapter
import com.momentum.habittracker.database.HabitRepository
import com.momentum.habittracker.databinding.FragmentHabitsListBinding
import kotlinx.coroutines.launch

class HabitsListFragment : Fragment() {
    private var _binding: FragmentHabitsListBinding? = null
    private val binding get() = _binding!!
    private val habitRepository by lazy { HabitRepository(requireContext()) }
    private lateinit var habitsAdapter: HabitsAdapter
    private var allHabits: List<com.momentum.habittracker.database.Habit> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeHabits()
        setupCategoryFilter()
    }

    private fun setupRecyclerView() {
        habitsAdapter = HabitsAdapter { habit ->
            val intent = Intent(requireContext(), HabitDetailActivity::class.java).apply {
                putExtra("HABIT_ID", habit.id)
            }
            startActivity(intent)
        }

        binding.recyclerViewHabits.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = habitsAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun observeHabits() {
        lifecycleScope.launch {
            habitRepository.getAllHabits().collect { habits ->
                allHabits = habits
                if (habits.isEmpty()) {
                    binding.textViewEmpty.visibility = View.VISIBLE
                    binding.textViewEmpty.text = "No habits yet. Tap the + button to add one!"
                    binding.recyclerViewHabits.visibility = View.GONE
                } else {
                    binding.textViewEmpty.visibility = View.GONE
                    binding.recyclerViewHabits.visibility = View.VISIBLE
                    habitsAdapter.submitList(habits)
                }
            }
        }
    }

    private fun setupCategoryFilter() {
        val categories = listOf("All") + resources.getStringArray(R.array.habit_categories).toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerCategoryFilter.adapter = adapter
        binding.spinnerCategoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                habitsAdapter.submitList(
                    if (selectedCategory == "All") allHabits
                    else allHabits.filter { it.category == selectedCategory }
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}