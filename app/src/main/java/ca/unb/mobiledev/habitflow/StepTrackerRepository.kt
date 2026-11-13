
package ca.unb.mobiledev.habitflow

import android.content.Context
import java.time.LocalDate

class StepTrackerRepository(context: Context) {

    companion object {
        const val DEFAULT_GOAL = 10000
        const val MIN_GOAL = 1000
        const val MAX_GOAL = 100000
    }

    private val preferences = context.getSharedPreferences(
        "habitflow_steps",
        Context.MODE_PRIVATE
    )

    fun getGoal(): Int {
        return preferences.getInt(
            "daily_step_goal",
            DEFAULT_GOAL
        ).coerceIn(MIN_GOAL, MAX_GOAL)
    }

    fun saveGoal(goal: Int): Boolean {
        if (goal !in MIN_GOAL..MAX_GOAL) return false

        return preferences.edit()
            .putInt("daily_step_goal", goal)
            .commit()
    }

    fun getSteps(
        date: LocalDate = LocalDate.now()
    ): Long {
        return preferences.getLong(
            "steps_$date",
            0L
        ).coerceAtLeast(0L)
    }

    // Return daily step counts for the last seven days.
    // Days without recorded steps are included as zero.
    fun getSevenDayHistory(
        endDate: LocalDate = LocalDate.now()
    ): List<Pair<LocalDate, Long>> {
        return (6 downTo 0).map { daysAgo ->
            val date = endDate.minusDays(daysAgo.toLong())
            date to getSteps(date)
        }
    }

    // Calculate average daily steps over seven days.
    fun getSevenDayAverageSteps(
        endDate: LocalDate = LocalDate.now()
    ): Long {
        return getSevenDayHistory(endDate)
            .sumOf { it.second } / 7L
    }

    fun getSevenDayTotalSteps(
        endDate: LocalDate = LocalDate.now()
    ): Long {
        return getSevenDayHistory(endDate)
            .sumOf { it.second }
    }

    // Android's step counter reports cumulative steps
    // since the device last restarted.
    fun recordSensorReading(
        sensorTotal: Long,
        date: LocalDate = LocalDate.now()
    ): Long {
        if (sensorTotal < 0L) return getSteps(date)

        val dateString = date.toString()

        val previousDate = preferences.getString(
            "last_reading_date",
            null
        )

        val previousReading = preferences.getLong(
            "last_sensor_reading",
            -1L
        )

        val currentSteps = getSteps(date)

        val difference =
            if (
                previousDate == dateString &&
                previousReading >= 0L &&
                sensorTotal >= previousReading
            ) {
                sensorTotal - previousReading
            } else {
                0L
            }

        val updatedSteps =
            if (Long.MAX_VALUE - currentSteps < difference) {
                Long.MAX_VALUE
            } else {
                currentSteps + difference
            }

        val saved = preferences.edit()
            .putLong("steps_$dateString", updatedSteps)
            .putLong("last_sensor_reading", sensorTotal)
            .putString("last_reading_date", dateString)
            .commit()

        return if (saved) updatedSteps else currentSteps
    }
}
