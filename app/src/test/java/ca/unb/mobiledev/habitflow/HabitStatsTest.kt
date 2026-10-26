package ca.unb.mobiledev.habitflow

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitStatsTest {
    private val friday = LocalDate.of(2026, 10, 23)
    @Test fun weekExcludesPreviousSundayAndFutureDates() {
        val habit = Habit("1", "Read", 5, "", setOf(friday.minusDays(5), friday.minusDays(4), friday, friday.plusDays(1)))
        assertEquals(2, HabitStats.count(habit, friday))
        assertEquals(LocalDate.of(2026, 10, 19), HabitStats.weekStart(friday))
        assertEquals(LocalDate.of(2026, 10, 19), HabitStats.weekStart(friday.plusDays(2)))
    }
    @Test fun goalsAreCappedIndividually() {
        val habit = Habit("1", "Read", 1, "", setOf(friday, friday.minusDays(1)))
        assertEquals(50, HabitStats.percent(listOf(habit, Habit("2", "Walk", 1, "")), friday))
        assertEquals(0, HabitStats.percent(emptyList(), friday))
    }
    @Test fun completionsAreUniqueAndCanBeUndone() {
        val dates = setOf(friday) + friday
        assertEquals(1, dates.size)
        assertEquals(emptySet<LocalDate>(), dates - friday)
    }
}
