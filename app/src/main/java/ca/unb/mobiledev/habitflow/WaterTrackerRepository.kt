
package ca.unb.mobiledev.habitflow

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class WaterIntakeEntry(
    val id: String,
    val amountMl: Int,
    val timestamp: String?
)

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

    private fun logKey(date: LocalDate): String =
        "water_logs_$date"

    private fun countKey(date: LocalDate): String =
        "water_$date"

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

    // Convert previously saved totals into individual entries.
    // Their original times are unknown, so timestamps remain null.
    private fun migrateIfNeeded(date: LocalDate): Boolean {
        if (preferences.contains(logKey(date))) {
            return true
        }

        val previousCount = preferences.getInt(
            countKey(date),
            0
        ).coerceAtLeast(0)

        val entries = JSONArray()

        repeat(previousCount) {
            entries.put(
                JSONObject().apply {
                    put("id", UUID.randomUUID().toString())
                    put("amountMl", SERVING_ML)
                    put("timestamp", JSONObject.NULL)
                }
            )
        }

        return preferences.edit()
            .putString(logKey(date), entries.toString())
            .commit()
    }

    private fun readEntries(
        date: LocalDate
    ): List<WaterIntakeEntry> {
        val json = JSONArray(
            preferences.getString(logKey(date), "[]")
        )

        return (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)

            WaterIntakeEntry(
                id = item.getString("id"),
                amountMl = item.getInt("amountMl"),
                timestamp = if (item.isNull("timestamp")) {
                    null
                } else {
                    item.getString("timestamp")
                }
            )
        }
    }

    private fun saveEntries(
        date: LocalDate,
        entries: List<WaterIntakeEntry>
    ): Boolean {
        val array = JSONArray()

        entries.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("amountMl", entry.amountMl)

                    put(
                        "timestamp",
                        entry.timestamp ?: JSONObject.NULL
                    )
                }
            )
        }

        // Save both the logs and the existing daily count
        // together to preserve compatibility.
        return preferences.edit()
            .putString(logKey(date), array.toString())
            .putInt(countKey(date), entries.size)
            .commit()
    }

    fun getEntries(
        date: LocalDate = LocalDate.now()
    ): List<WaterIntakeEntry> {
        if (!migrateIfNeeded(date)) {
            return emptyList()
        }

        return readEntries(date)
    }

    fun getGlasses(
        date: LocalDate = LocalDate.now()
    ): Int {
        if (!migrateIfNeeded(date)) {
            return preferences.getInt(
                countKey(date),
                0
            ).coerceAtLeast(0)
        }

        return readEntries(date).size
    }

    fun getWaterMl(
        date: LocalDate = LocalDate.now()
    ): Long {
        return if (migrateIfNeeded(date)) {
            readEntries(date).sumOf {
                it.amountMl.toLong()
            }
        } else {
            preferences.getInt(
                countKey(date),
                0
            ).coerceAtLeast(0).toLong() * SERVING_ML
        }
    }

    // Retain compatibility with the existing API.
    fun saveGlasses(
        glasses: Int,
        date: LocalDate = LocalDate.now()
    ): Boolean {
        if (!migrateIfNeeded(date)) return false

        val entries = readEntries(date).toMutableList()
        val desiredCount = glasses.coerceAtLeast(0)

        while (entries.size > desiredCount) {
            entries.removeAt(entries.lastIndex)
        }

        while (entries.size < desiredCount) {
            entries.add(
                WaterIntakeEntry(
                    id = UUID.randomUUID().toString(),
                    amountMl = SERVING_ML,
                    timestamp = null
                )
            )
        }

        return saveEntries(date, entries)
    }

    // Each new +250 mL tap gets its actual timestamp.
    fun addGlass(): Boolean {
        val date = LocalDate.now()

        if (!migrateIfNeeded(date)) {
            return false
        }

        val entries = readEntries(date).toMutableList()

        if (entries.size == Int.MAX_VALUE) {
            return false
        }

        entries.add(
            WaterIntakeEntry(
                id = UUID.randomUUID().toString(),
                amountMl = SERVING_ML,
                timestamp = Instant.now().toString()
            )
        )

        return saveEntries(date, entries)
    }

    // Delete only the selected water entry.
    fun deleteEntry(
        entryId: String,
        date: LocalDate = LocalDate.now()
    ): Boolean {
        if (!migrateIfNeeded(date)) return false

        val entries = readEntries(date)

        if (entries.none { it.id == entryId }) {
            return false
        }

        return saveEntries(
            date,
            entries.filterNot { it.id == entryId }
        )
    }

    // Seven days of water intake, including zero-intake days.
    fun getSevenDayHistory(
        endDate: LocalDate = LocalDate.now()
    ): List<Pair<LocalDate, Long>> {
        return (6 downTo 0).map { daysAgo ->
            val date = endDate.minusDays(daysAgo.toLong())
            date to getWaterMl(date)
        }
    }

    fun getSevenDayAverageMl(
        endDate: LocalDate = LocalDate.now()
    ): Long {
        return getSevenDayHistory(endDate)
            .sumOf { it.second } / 7L
    }
}
