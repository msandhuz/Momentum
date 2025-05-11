package com.momentum.habittracker.adapters

import com.momentum.habittracker.utils.DateUtils
import com.momentum.habittracker.R
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.util.*

class CalendarAdapter(
    private val context: Context,
    private var month: Int,
    private var year: Int,
    private val onDateClick: (String) -> Unit
) : BaseAdapter() {

    private val calendar = Calendar.getInstance().apply { set(year, month, 1) }
    private var daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    private var firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    private val completionDates = mutableSetOf<String>()
    private val today = DateUtils.getCurrentDate()

    var displayMonth = month
    var displayYear = year
    private val firstDayOffset = if (firstDayOfWeek == Calendar.SUNDAY) 0 else firstDayOfWeek - 1

    private inner class ViewHolder(view: View) {
        val dayTextView: TextView = view.findViewById(R.id.textViewDay)
        val dotView: View = view.findViewById(R.id.viewDot)
    }

    fun updateCompletionDates(newDates: List<String>) {
        completionDates.clear()
        completionDates.addAll(newDates)
        notifyDataSetChanged()
    }

    fun prevMonth() {
        calendar.add(Calendar.MONTH, -1)
        updateMonthData()
    }

    fun nextMonth() {
        calendar.add(Calendar.MONTH, 1)
        updateMonthData()
    }

    private fun updateMonthData() {
        displayMonth = calendar.get(Calendar.MONTH)
        displayYear = calendar.get(Calendar.YEAR)
        daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        notifyDataSetChanged()
    }

    override fun getCount() = 42 // Fixed grid size (7 columns × 6 rows)

    override fun getItem(position: Int): Any? = null
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_calendar_day, parent, false)
        val holder = convertView?.tag as? ViewHolder ?: ViewHolder(view).also { view.tag = it }

        val day = position - firstDayOffset + 1
        val isCurrentMonth = day in 1..daysInMonth

        if (isCurrentMonth) {
            val dateStr = "%04d-%02d-%02d".format(displayYear, displayMonth + 1, day)

            with(holder) {
                dayTextView.text = day.toString()
                dayTextView.visibility = View.VISIBLE

                if (dateStr == today) {
                    dayTextView.setBackgroundResource(R.drawable.circle_today)
                    dayTextView.setTextColor(Color.WHITE)
                } else {
                    dayTextView.background = null
                    dayTextView.setTextColor(ContextCompat.getColor(context, R.color.primaryText))
                }

                dotView.visibility = if (completionDates.contains(dateStr)) View.VISIBLE else View.INVISIBLE
                view.setOnClickListener { onDateClick(dateStr) }
            }
        } else {
            with(holder) {
                dayTextView.visibility = View.INVISIBLE
                dotView.visibility = View.GONE
                view.setOnClickListener(null)
            }
        }

        return view
    }
}