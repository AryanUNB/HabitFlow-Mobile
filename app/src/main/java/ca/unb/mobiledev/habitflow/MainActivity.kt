
package ca.unb.mobiledev.habitflow

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : Activity() {

    private lateinit var repository: HabitRepository

    private val handler = Handler(Looper.getMainLooper())

    private var splashDeadline = 0L
    private var entryVisible = false
    private var guestSelected = false

    private var removeSystemSplash: Runnable? = null

    private val openEntry = Runnable {
        showEntry()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = HabitRepository(this)

        guestSelected =
            savedInstanceState?.getBoolean("guest_selected") ?: false

        entryVisible =
            savedInstanceState?.getBoolean("entry_visible") ?: false

        splashDeadline =
            savedInstanceState?.getLong("splash_deadline")
                ?: (SystemClock.elapsedRealtime() + SPLASH_DURATION_MS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splash ->
                if (SystemClock.elapsedRealtime() < splashDeadline) {
                    splash.addView(
                        FloatingHabitsView(this),
                        android.widget.FrameLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )

                    val remove = Runnable {
                        splash.remove()
                    }

                    removeSystemSplash = remove

                    handler.postDelayed(
                        remove,
                        (splashDeadline - SystemClock.elapsedRealtime())
                            .coerceAtLeast(0L)
                    )
                } else {
                    splash.remove()
                }
            }

            showEntry()
        } else if (entryVisible) {
            showEntry()
        } else {
            setContentView(R.layout.activity_splash)
            fitSystemBars()
        }
    }

    override fun onStart() {
        super.onStart()

        if (!entryVisible) {
            handler.postDelayed(
                openEntry,
                (splashDeadline - SystemClock.elapsedRealtime())
                    .coerceAtLeast(0L)
            )
        }
    }

    override fun onStop() {
        handler.removeCallbacks(openEntry)
        super.onStop()
    }

    override fun onDestroy() {
        removeSystemSplash?.let {
            handler.removeCallbacks(it)
            it.run()
        }

        removeSystemSplash = null
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("guest_selected", guestSelected)
        outState.putLong("splash_deadline", splashDeadline)
        outState.putBoolean("entry_visible", entryVisible)
        super.onSaveInstanceState(outState)
    }

    private fun showEntry() {
        entryVisible = true

        if (guestSelected) {
            showDashboard()
            return
        }

        setTheme(R.style.Theme_HabitFlow)
        setContentView(R.layout.activity_welcome)
        fitSystemBars()

        findViewById<Button>(
            R.id.continue_guest
        ).setOnClickListener {
            guestSelected = true
            showDashboard()
        }
    }

    private fun showDashboard() {
        entryVisible = true

        setTheme(R.style.Theme_HabitFlow)

        window.statusBarColor =
            android.graphics.Color.rgb(243, 247, 244)

        window.navigationBarColor =
            android.graphics.Color.rgb(243, 247, 244)

        setContentView(R.layout.activity_main)
        fitSystemBars()

        requestNotificationPermission()
        HabitReminderScheduler.schedule(this)

        // Add a new habit
        findViewById<Button>(
            R.id.add_habit
        ).setOnClickListener {
            startActivity(
                Intent(this, AddHabitActivity::class.java)
            )
        }

        // Weekly statistics
        findViewById<Button>(
            R.id.open_stats
        ).setOnClickListener {
            startActivity(
                Intent(this, StatsActivity::class.java)
            )
        }

        // Water Tracker
        findViewById<Button>(
            R.id.open_water_tracker
        ).setOnClickListener {
            startActivity(
                Intent(this, WaterTrackerActivity::class.java)
            )
        }

        // Step Counter
        findViewById<Button>(
            R.id.open_step_tracker
        ).setOnClickListener {
            startActivity(
                Intent(this, StepTrackerActivity::class.java)
            )
        }

        // Feature 10: Achievements & Rewards
        findViewById<Button>(
            R.id.open_achievements
        ).setOnClickListener {
            startActivity(
                Intent(this, AchievementsActivity::class.java)
            )
        }

        render()
    }

    private fun requestNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }

    override fun onResume() {
        super.onResume()

        if (entryVisible && guestSelected) {
            render()
        }
    }

    private companion object {
        const val SPLASH_DURATION_MS = 4_000L
    }

    private fun categoryIcon(category: String): String {
        return when (category.trim().lowercase()) {
            "study" -> "📚"
            "recovery" -> "🌿"
            "relationships" -> "❤️"
            "health" -> "💪"
            "water", "hydration" -> "💧"
            "fitness", "exercise" -> "🏃"
            "sleep" -> "🌙"
            else -> "🎯"
        }
    }

    private fun render() {
        val today = LocalDate.now()
        val habits = repository.load()

        findViewById<TextView>(R.id.date).text =
            today.format(
                DateTimeFormatter.ofPattern("EEEE, MMMM d")
            )

        val completed = habits.count {
            today in it.completions
        }

        findViewById<TextView>(
            R.id.progress_label
        ).text =
            "$completed of ${habits.size} habits completed today"

        findViewById<ProgressBar>(
            R.id.today_progress
        ).progress = if (habits.isEmpty()) {
            0
        } else {
            completed * 100 / habits.size
        }

        val list = findViewById<LinearLayout>(
            R.id.habit_list
        )

        list.removeAllViews()

        if (habits.isEmpty()) {
            list.addView(TextView(this).apply {
                text =
                    "Start small. Add your first habit and take the first step today."
                textSize = 17f
            })
        }

        habits.forEach { habit ->

            val row = layoutInflater.inflate(
                R.layout.item_habit,
                list,
                false
            )

            // Habit name
            row.findViewById<TextView>(
                R.id.habit_name
            ).text = habit.name

            // Category icon
            row.findViewById<TextView>(
                R.id.habit_icon
            ).text = categoryIcon(habit.category)

            // Weekly progress
            val weeklyCompleted =
                HabitStats.count(habit, today)

            row.findViewById<TextView>(
                R.id.habit_detail
            ).text =
                "$weeklyCompleted/${habit.target} days this week" +
                        if (habit.category.isBlank()) {
                            ""
                        } else {
                            " • ${habit.category}"
                        }

            val progressPercent =
                if (habit.target > 0) {
                    (weeklyCompleted * 100 / habit.target)
                        .coerceIn(0, 100)
                } else {
                    0
                }

            row.findViewById<ProgressBar>(
                R.id.habit_progress
            ).apply {
                max = 100
                progress = progressPercent

                contentDescription =
                    "${habit.name}: $progressPercent percent of weekly goal"
            }

            // Daily completion checkbox
            row.findViewById<CheckBox>(
                R.id.habit_complete
            ).apply {
                isChecked = today in habit.completions

                setOnCheckedChangeListener { _, checked ->

                    val currentDate = LocalDate.now()

                    val updated = repository.load().map {
                        if (it.id != habit.id) {
                            it
                        } else {
                            it.copy(
                                completions =
                                    if (checked) {
                                        it.completions + currentDate
                                    } else {
                                        it.completions - currentDate
                                    }
                            )
                        }
                    }

                    if (!repository.save(updated)) {
                        Toast.makeText(
                            this@MainActivity,
                            "Could not save. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    render()
                }
            }

            // Edit existing habit
            row.findViewById<Button>(
                R.id.edit_habit
            ).apply {
                contentDescription = "Edit ${habit.name}"

                setOnClickListener {
                    val editIntent = Intent(
                        this@MainActivity,
                        AddHabitActivity::class.java
                    ).apply {
                        putExtra(
                            AddHabitActivity.EXTRA_EDIT_HABIT_ID,
                            habit.id
                        )
                    }

                    startActivity(editIntent)
                }
            }

            // Delete habit
            row.findViewById<Button>(
                R.id.delete_habit
            ).apply {
                contentDescription = getString(
                    R.string.delete_habit_accessibility,
                    habit.name
                )

                setOnClickListener {
                    confirmDelete(habit)
                }
            }

            list.addView(row)
        }
    }

    private fun confirmDelete(habit: Habit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_habit_title)
            .setMessage(
                getString(
                    R.string.delete_habit_message,
                    habit.name
                )
            )
            .setNegativeButton(
                R.string.cancel,
                null
            )
            .setPositiveButton(
                R.string.delete_habit
            ) { _, _ ->

                val remaining = repository.load()
                    .filterNot { it.id == habit.id }

                if (!repository.save(remaining)) {
                    Toast.makeText(
                        this,
                        R.string.save_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }

                render()
            }
            .show()
    }
}
