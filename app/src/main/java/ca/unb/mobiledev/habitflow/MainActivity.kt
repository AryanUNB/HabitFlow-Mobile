package ca.unb.mobiledev.habitflow

import android.app.AlertDialog
import android.app.Activity
import android.content.Intent
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
    private val openEntry = Runnable { showEntry() }
    private var guestSelected = false
    private var removeSystemSplash: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = HabitRepository(this)
        guestSelected = savedInstanceState?.getBoolean("guest_selected") ?: false
        entryVisible = savedInstanceState?.getBoolean("entry_visible") ?: false
        splashDeadline = savedInstanceState?.getLong("splash_deadline")
            ?: (SystemClock.elapsedRealtime() + SPLASH_DURATION_MS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Animate on the actual system splash rather than displaying a second screen.
            splashScreen.setOnExitAnimationListener { splash ->
                if (SystemClock.elapsedRealtime() < splashDeadline) {
                    splash.addView(FloatingHabitsView(this), android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT))
                    val remove = Runnable { splash.remove() }
                    removeSystemSplash = remove
                    handler.postDelayed(remove,
                        (splashDeadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
                } else splash.remove()
            }
            showEntry()
        } else if (entryVisible) showEntry() else {
            setContentView(R.layout.activity_splash)
            fitSystemBars()
        }
    }

    override fun onStart() {
        super.onStart()
        if (!entryVisible) {
            handler.postDelayed(openEntry,
                (splashDeadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
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
        findViewById<Button>(R.id.continue_guest).setOnClickListener {
            guestSelected = true
            showDashboard()
        }
    }

    private fun showDashboard() {
        entryVisible = true
        setTheme(R.style.Theme_HabitFlow)
        window.statusBarColor = android.graphics.Color.rgb(243, 247, 244)
        window.navigationBarColor = android.graphics.Color.rgb(243, 247, 244)
        setContentView(R.layout.activity_main)
        fitSystemBars()
        findViewById<Button>(R.id.add_habit).setOnClickListener { startActivity(Intent(this, AddHabitActivity::class.java)) }
        findViewById<Button>(R.id.open_stats).setOnClickListener { startActivity(Intent(this, StatsActivity::class.java)) }
        render()
    }

    override fun onResume() {
        super.onResume()
        if (entryVisible && guestSelected) render()
    }

    private companion object {
        const val SPLASH_DURATION_MS = 4_000L
    }

    private fun render() {
        val today = LocalDate.now()
        val habits = repository.load()
        findViewById<TextView>(R.id.date).text = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
        val completed = habits.count { today in it.completions }
        findViewById<TextView>(R.id.progress_label).text = "$completed of ${habits.size} habits completed today"
        findViewById<ProgressBar>(R.id.today_progress).progress = if (habits.isEmpty()) 0 else completed * 100 / habits.size
        val list = findViewById<LinearLayout>(R.id.habit_list)
        list.removeAllViews()
        if (habits.isEmpty()) list.addView(TextView(this).apply {
            text = "Start small. Add your first habit and take the first step today."
            textSize = 17f
        })
        habits.forEach { habit ->
            val row = layoutInflater.inflate(R.layout.item_habit, list, false)
            row.findViewById<TextView>(R.id.habit_name).text = habit.name
            row.findViewById<TextView>(R.id.habit_detail).text =
                "${HabitStats.count(habit, today)}/${habit.target} days this week" + if (habit.category.isBlank()) "" else " • ${habit.category}"
            row.findViewById<CheckBox>(R.id.habit_complete).apply {
                isChecked = today in habit.completions
                setOnCheckedChangeListener { _, checked ->
                    val currentDate = LocalDate.now()
                    val updated = repository.load().map {
                        if (it.id != habit.id) it else it.copy(completions =
                            if (checked) it.completions + currentDate else it.completions - currentDate)
                    }
                    if (!repository.save(updated)) Toast.makeText(this@MainActivity, "Could not save. Please try again.", Toast.LENGTH_LONG).show()
                    render()
                }
            }
            row.findViewById<Button>(R.id.delete_habit).apply {
                contentDescription = getString(R.string.delete_habit_accessibility, habit.name)
                setOnClickListener { confirmDelete(habit) }
            }
            list.addView(row)
        }
    }

    private fun confirmDelete(habit: Habit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_habit_title)
            .setMessage(getString(R.string.delete_habit_message, habit.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete_habit) { _, _ ->
                // Read current data so other changes are preserved when removing this habit.
                val remaining = repository.load().filterNot { it.id == habit.id }
                if (!repository.save(remaining)) {
                    Toast.makeText(this, R.string.save_failed, Toast.LENGTH_LONG).show()
                }
                render()
            }
            .show()
    }

}
