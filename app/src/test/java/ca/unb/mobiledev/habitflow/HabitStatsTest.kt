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

    @Test fun weekSpansYearBoundary() {
        val newYear = LocalDate.of(2027, 1, 1)
        val habit = Habit("1", "Read", 5, "", setOf(
            LocalDate.of(2026, 12, 27), LocalDate.of(2026, 12, 28), newYear))
        assertEquals(LocalDate.of(2026, 12, 28), HabitStats.weekStart(newYear))
        assertEquals(2, HabitStats.count(habit, newYear))
    }

    @Test fun newWeekResetsProgressWithoutDeletingHistory() {
        val previousSunday = LocalDate.of(2026, 10, 25)
        val habit = Habit("1", "Walk", 3, "", setOf(previousSunday))
        assertEquals(1, HabitStats.count(habit, previousSunday))
        assertEquals(0, HabitStats.count(habit, previousSunday.plusDays(1)))
        assertEquals(setOf(previousSunday), habit.completions)
    }

    @Test fun overallProgressUsesTargetsRatherThanAveragePercentages() {
        val easyGoal = Habit("1", "Read", 1, "", setOf(friday))
        val largerGoal = Habit("2", "Walk", 7, "", setOf(friday))
        assertEquals(25, HabitStats.percent(listOf(easyGoal, largerGoal), friday))
    }

    @Test fun exceedingEveryTargetStopsAtOneHundredPercent() {
        val habit = Habit("1", "Walk", 1, "", setOf(friday, friday.minusDays(1)))
        assertEquals(100, HabitStats.percent(listOf(habit), friday))
    }

}
