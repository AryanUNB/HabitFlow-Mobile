package ca.unb.mobiledev.habitflow

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Small offline dataset, saved atomically by SharedPreferences before updating the UI. */
class HabitRepository(context: Context) {
    private val preferences = context.getSharedPreferences("habitflow", Context.MODE_PRIVATE)
    fun load(): List<Habit> {
        val array = JSONArray(preferences.getString("habits", "[]"))
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val dates = item.getJSONArray("completions")
            Habit(item.getString("id"), item.getString("name"), item.getInt("target"),
                item.getString("category"), (0 until dates.length()).map {
                    LocalDate.parse(dates.getString(it))
                }.toSet())
        }
    }
    fun save(habits: List<Habit>): Boolean {
        val array = JSONArray()
        habits.forEach { habit -> array.put(JSONObject().apply {
            put("id", habit.id); put("name", habit.name); put("target", habit.target)
            put("category", habit.category)
            put("completions", JSONArray(habit.completions.sorted().map { it.toString() }))
        }) }
        return preferences.edit().putString("habits", array.toString()).commit()
    }
}
