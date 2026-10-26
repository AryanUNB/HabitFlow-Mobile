package ca.unb.mobiledev.habitflow

import android.app.Activity
import android.os.Bundle
import android.widget.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StatsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        fitSystemBars()
        findViewById<Button>(R.id.back).setOnClickListener { finish() }
        val habits = HabitRepository(this).load()
        val today = LocalDate.now()
        val start = HabitStats.weekStart(today)
        val percent = HabitStats.percent(habits, today)
        val counts = (0..6).map { offset -> habits.count { start.plusDays(offset.toLong()) in it.completions } }
        findViewById<TextView>(R.id.week_summary).text =
            "Week of ${start.format(DateTimeFormatter.ofPattern("MMM d"))} • Monday–Sunday\n${habits.sumOf { HabitStats.count(it, today) }} completions • $percent% of weekly goals\nEach habit contributes up to its weekly target."
        findViewById<WeeklyChartView>(R.id.chart).apply {
            values = counts; percentage = percent
            contentDescription = "Weekly goals $percent percent. " + counts.mapIndexed { i, count -> "${start.plusDays(i.toLong()).dayOfWeek}: $count" }.joinToString(", ")
        }
        val goals = findViewById<LinearLayout>(R.id.goals)
        if (habits.isEmpty()) goals.addView(TextView(this).apply { text = "Add a habit to start tracking your progress." })
        habits.forEach { habit -> goals.addView(TextView(this).apply {
            text = "${habit.name}\n${HabitStats.count(habit, today)} of ${habit.target} days completed"
            textSize = 17f; setPadding(0, 20, 0, 20)
        }) }
    }
}
