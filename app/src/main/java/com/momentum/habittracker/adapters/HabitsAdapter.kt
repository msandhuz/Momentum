package com.momentum.habittracker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.momentum.habittracker.database.Habit
import com.momentum.habittracker.databinding.ItemHabitBinding
import com.momentum.habittracker.R
import com.momentum.habittracker.utils.DateUtils

class HabitsAdapter(
    private val onItemClick: (Habit) -> Unit
) : ListAdapter<Habit, HabitsAdapter.HabitViewHolder>(HabitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val binding = ItemHabitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HabitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = getItem(position)
        holder.bind(habit, onItemClick)
    }

    class HabitViewHolder(private val binding: ItemHabitBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(habit: Habit, onItemClick: (Habit) -> Unit) {
            with(binding) {
                // Changed from habit.title to habit.name
                textViewTitle.text = habit.name
                textViewCategory.text = habit.category
                textViewFrequency.text = habit.frequency
                textViewStreak.text = itemView.context.getString(R.string.streak_days, habit.streak)

                // Set category color
                val categoryColor = when (habit.category) {
                    itemView.context.getString(R.string.category_health) -> R.color.health_category
                    itemView.context.getString(R.string.category_productivity) -> R.color.productivity_category
                    itemView.context.getString(R.string.category_wellness) -> R.color.wellness_category
                    else -> R.color.other_category
                }

                viewCategoryColor.setBackgroundResource(categoryColor)

                // Check if completed today
                val today = DateUtils.getCurrentDate()
                val isCompletedToday = habit.completionDates?.contains(today) ?: false

                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        itemView.context,
                        if (isCompletedToday) R.color.completed_background else R.color.card_background
                    )
                )

                root.setOnClickListener { onItemClick(habit) }
            }
        }
    }
}

class HabitDiffCallback : DiffUtil.ItemCallback<Habit>() {
    override fun areItemsTheSame(oldItem: Habit, newItem: Habit): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Habit, newItem: Habit): Boolean {
        return oldItem == newItem
    }
}