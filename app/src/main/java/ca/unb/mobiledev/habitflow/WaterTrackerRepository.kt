
package ca.unb.mobiledev.habitflow

import android.content.Context
import java.time.LocalDate

class WaterTrackerRepository(context: Context) {

    companion object {
        const val DEFAULT_GOAL_ML = 2000
        const val SERVING_ML = 250

        const val MIN_GOAL_ML = 250
        const val MAX_GOAL_ML = 10000
    }

    private val preferences = context.getSharedPreferences(
        "habitflow_water",
        Context.MODE_PRIVATE
    )

    fun getGoalMl(): Int {
        return preferences.getInt(
            "daily_goal_ml",
            DEFAULT_GOAL_ML
        ).coerceIn(MIN_GOAL_ML, MAX_GOAL_ML)
    }

    fun saveGoalMl(goalMl: Int): Boolean {
        if (goalMl !in MIN_GOAL_ML..MAX_GOAL_ML) {
            return false
        }

        return preferences.edit()
            .putInt("daily_goal_ml", goalMl)
            .commit()
    }

    fun getGlasses(
        date: LocalDate = LocalDate.now()
    ): Int {
        return preferences.getInt(
            "water_$date",
            0
        ).coerceAtLeast(0)
    }

    fun getWaterMl(
        date: LocalDate = LocalDate.now()
    ): Long {
        return getGlasses(date).toLong() * SERVING_ML
    }

    fun saveGlasses(
        glasses: Int,
        date: LocalDate = LocalDate.now()
    ): Boolean {
        return preferences.edit()
            .putInt(
                "water_$date",
                glasses.coerceAtLeast(0)
            )
            .commit()
    }

    fun addGlass(): Boolean {
        val current = getGlasses()

        if (current == Int.MAX_VALUE) {
            return false
        }

        return saveGlasses(current + 1)
    }

    fun removeGlass(): Boolean {
        val current = getGlasses()

        if (current <= 0) {
            return false
        }

        return saveGlasses(current - 1)
    }
}
