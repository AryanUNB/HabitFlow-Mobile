
package ca.unb.mobiledev.habitflow

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class HabitRewards(
    val tokens: Long,
    val lifetimeCompletions: Int,
    val currentStreakDays: Int,
    val unlockedBadges: Set<String>
)

object RewardBadges {
    const val FIRST_STEP = "first_step"
    const val MOMENTUM = "momentum"
    const val HABIT_MASTER = "habit_master"
    const val CONSISTENCY_KING = "consistency_king"
    const val PERFECT_DAY = "perfect_day"
}

class HabitRepository(context: Context) {

    private val preferences = context.getSharedPreferences(
        "habitflow",
        Context.MODE_PRIVATE
    )

    private companion object {
        const val HABITS_KEY = "habits"
        const val REWARDS_INITIALIZED = "rewards_initialized"
        const val REWARDED_COMPLETIONS = "rewarded_completions"
        const val UNLOCKED_BADGES = "unlocked_reward_badges"
        const val TOKENS_PER_COMPLETION = 10L
    }

    fun load(): List<Habit> {
        val array = JSONArray(
            preferences.getString(HABITS_KEY, "[]")
        )

        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val dates = item.getJSONArray("completions")

            Habit(
                id = item.getString("id"),
                name = item.getString("name"),
                target = item.getInt("target"),
                category = item.getString("category"),
                completions = (0 until dates.length()).map {
                    LocalDate.parse(dates.getString(it))
                }.toSet()
            )
        }
    }

    private fun completionKeys(
        habits: List<Habit>
    ): Set<String> {
        return habits.flatMap { habit ->
            habit.completions.map { date ->
                "${habit.id}|$date"
            }
        }.toSet()
    }

    private fun rewardedKeys(): Set<String> {
        return preferences.getStringSet(
            REWARDED_COMPLETIONS,
            emptySet()
        )?.toSet() ?: emptySet()
    }

    private fun unlockedBadges(): Set<String> {
        return preferences.getStringSet(
            UNLOCKED_BADGES,
            emptySet()
        )?.toSet() ?: emptySet()
    }

    private fun completionDates(
        keys: Set<String>
    ): List<LocalDate> {
        return keys.mapNotNull { key ->
            runCatching {
                LocalDate.parse(
                    key.substringAfterLast("|")
                )
            }.getOrNull()
        }.distinct().sorted()
    }

    private fun longestStreak(
        dates: List<LocalDate>
    ): Int {
        var longest = 0
        var consecutive = 0
        var previous: LocalDate? = null

        dates.forEach { date ->
            consecutive = if (
                previous?.plusDays(1) == date
            ) {
                consecutive + 1
            } else {
                1
            }

            longest = maxOf(longest, consecutive)
            previous = date
        }

        return longest
    }

    private fun currentStreak(
        dates: List<LocalDate>
    ): Int {
        val completedDates = dates.toSet()
        val today = LocalDate.now()

        var date = if (today in completedDates) {
            today
        } else {
            today.minusDays(1)
        }

        var streak = 0

        while (date in completedDates) {
            streak++
            date = date.minusDays(1)
        }

        return streak
    }

    private fun calculateBadges(
        existing: Set<String>,
        keys: Set<String>,
        habits: List<Habit>
    ): Set<String> {
        val badges = existing.toMutableSet()

        if (keys.isNotEmpty()) {
            badges.add(RewardBadges.FIRST_STEP)
        }

        if (keys.size >= 10) {
            badges.add(RewardBadges.MOMENTUM)
        }

        if (keys.size >= 50) {
            badges.add(RewardBadges.HABIT_MASTER)
        }

        val dates = completionDates(keys)

        if (longestStreak(dates) >= 7) {
            badges.add(RewardBadges.CONSISTENCY_KING)
        }

        val today = LocalDate.now()

        if (
            habits.isNotEmpty() &&
            habits.all { today in it.completions }
        ) {
            badges.add(RewardBadges.PERFECT_DAY)
        }

        return badges
    }

    fun save(habits: List<Habit>): Boolean {
        val array = JSONArray()

        habits.forEach { habit ->
            array.put(
                JSONObject().apply {
                    put("id", habit.id)
                    put("name", habit.name)
                    put("target", habit.target)
                    put("category", habit.category)

                    put(
                        "completions",
                        JSONArray(
                            habit.completions.sorted().map {
                                it.toString()
                            }
                        )
                    )
                }
            )
        }

        val existingKeys =
            if (
                preferences.getBoolean(
                    REWARDS_INITIALIZED,
                    false
                )
            ) {
                rewardedKeys()
            } else {
                completionKeys(load())
            }

        // Each habit/date combination earns tokens once.
        // Previously earned rewards are retained.
        val allEarnedKeys =
            existingKeys + completionKeys(habits)

        val badges = calculateBadges(
            unlockedBadges(),
            allEarnedKeys,
            habits
        )

        // The array retains the user's custom habit order.
        return preferences.edit()
            .putString(HABITS_KEY, array.toString())
            .putStringSet(
                REWARDED_COMPLETIONS,
                allEarnedKeys
            )
            .putStringSet(
                UNLOCKED_BADGES,
                badges
            )
            .putBoolean(REWARDS_INITIALIZED, true)
            .commit()
    }

    fun getRewards(): HabitRewards {
        val habits = load()

        if (
            !preferences.getBoolean(
                REWARDS_INITIALIZED,
                false
            )
        ) {
            val keys = completionKeys(habits)

            val badges = calculateBadges(
                emptySet(),
                keys,
                habits
            )

            preferences.edit()
                .putStringSet(REWARDED_COMPLETIONS, keys)
                .putStringSet(UNLOCKED_BADGES, badges)
                .putBoolean(REWARDS_INITIALIZED, true)
                .commit()
        }

        val keys = rewardedKeys()
        val dates = completionDates(keys)

        return HabitRewards(
            tokens = keys.size.toLong() *
                    TOKENS_PER_COMPLETION,
            lifetimeCompletions = keys.size,
            currentStreakDays = currentStreak(dates),
            unlockedBadges = unlockedBadges()
        )
    }
}
