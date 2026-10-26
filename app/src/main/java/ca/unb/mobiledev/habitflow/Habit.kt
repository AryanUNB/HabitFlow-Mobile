package ca.unb.mobiledev.habitflow

import java.time.LocalDate
import java.time.DayOfWeek

data class Habit(val id: String, val name: String, val target: Int, val category: String,
                 val completions: Set<LocalDate> = emptySet())

object HabitStats {
    fun weekStart(today: LocalDate): LocalDate = today.with(DayOfWeek.MONDAY)
    fun count(habit: Habit, today: LocalDate): Int = habit.completions.count {
        !it.isBefore(weekStart(today)) && !it.isAfter(today)
    }
    // Each habit contributes at most its own target to overall goal progress.
    fun percent(habits: List<Habit>, today: LocalDate): Int {
        val target = habits.sumOf { it.target }
        return if (target == 0) 0 else habits.sumOf { minOf(count(it, today), it.target) } * 100 / target
    }
}
