
package ca.unb.mobiledev.habitflow

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class StatsActivity : Activity() {

    private val darkGreen = Color.rgb(21, 62, 53)
    private val green = Color.rgb(39, 133, 104)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_stats)
        fitSystemBars()

        findViewById<Button>(R.id.back).setOnClickListener {
            finish()
        }

        displayStatistics()
    }

    override fun onResume() {
        super.onResume()
        displayStatistics()
    }

    private fun createCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL

            setPadding(
                dp(18),
                dp(20),
                dp(18),
                dp(20)
            )

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(20).toFloat()
            }
        }
    }

    private fun displayStatistics() {
        val habits = HabitRepository(this).load()
        val today = LocalDate.now()
        val start = HabitStats.weekStart(today)

        // Preserve existing weekly statistics.
        val weeklyPercent = HabitStats.percent(habits, today)

        val weeklyCounts = (0..6).map { offset ->
            habits.count { habit ->
                start.plusDays(offset.toLong()) in habit.completions
            }
        }

        val weeklyCompletions = habits.sumOf { habit ->
            HabitStats.count(habit, today)
        }

        findViewById<TextView>(R.id.week_summary).text =
            "Week of ${
                start.format(
                    DateTimeFormatter.ofPattern("MMM d")
                )
            } • Monday–Sunday\n" +
                    "$weeklyCompletions completions • " +
                    "$weeklyPercent% of weekly goals\n" +
                    "Each habit contributes up to its weekly target."

        findViewById<WeeklyChartView>(R.id.chart).apply {
            values = weeklyCounts
            percentage = weeklyPercent

            contentDescription =
                "Weekly goals $weeklyPercent percent. " +
                        weeklyCounts.mapIndexed { index, count ->
                            "${
                                start.plusDays(index.toLong()).dayOfWeek
                            }: $count"
                        }.joinToString(", ")
        }

        val goals = findViewById<LinearLayout>(R.id.goals)
        goals.removeAllViews()

        // Today's habit completion statistics.
        val completedToday = habits.count { habit ->
            today in habit.completions
        }

        val totalHabits = habits.size

        val dailyPercent =
            if (totalHabits == 0) {
                0
            } else {
                completedToday * 100 / totalHabits
            }

        val donutCard = createCard()

        donutCard.addView(TextView(this).apply {
            text = "Today's Habit Completion"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(darkGreen)
            gravity = Gravity.CENTER
        })

        donutCard.addView(
            DailyHabitDonutView(
                this,
                dailyPercent,
                completedToday,
                totalHabits
            ),
            LinearLayout.LayoutParams(
                dp(210),
                dp(210)
            ).apply {
                topMargin = dp(12)
            }
        )

        donutCard.addView(TextView(this).apply {
            text = "$completedToday of $totalHabits habits completed"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(darkGreen)
            setTypeface(null, Typeface.BOLD)
        })

        donutCard.addView(TextView(this).apply {
            text = if (totalHabits == 0) {
                "Add your first habit to begin tracking."
            } else if (completedToday == totalHabits) {
                "All habits completed today!"
            } else {
                "${totalHabits - completedToday} habits remaining today"
            }

            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
            setPadding(0, dp(8), 0, 0)
        })

        goals.addView(
            donutCard,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(18)
            }
        )

        // Daily completion history for the last seven days.
        val dailyHistory = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())

            val count = habits.count { habit ->
                date in habit.completions
            }

            date to count
        }

        val barCard = createCard()

        barCard.addView(TextView(this).apply {
            text = "Daily Habit Completions"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(darkGreen)
            gravity = Gravity.CENTER
        })

        barCard.addView(TextView(this).apply {
            text = "Last 7 days"
            textSize = 14f
            setTextColor(Color.GRAY)
            setPadding(0, dp(6), 0, dp(14))
        })

        barCard.addView(
            DailyHabitBarChartView(
                this,
                dailyHistory,
                totalHabits
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(250)
            )
        )

        goals.addView(
            barCard,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(22)
            }
        )

        // Preserve the individual weekly habit statistics.
        goals.addView(TextView(this).apply {
            text = "Individual Habit Progress"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(darkGreen)
            setPadding(0, dp(8), 0, dp(12))
        })

        if (habits.isEmpty()) {
            goals.addView(TextView(this).apply {
                text = "Add a habit to start tracking your progress."
                textSize = 16f
                setTextColor(Color.GRAY)
            })
        }

        habits.forEach { habit ->
            val completed = HabitStats.count(habit, today)

            goals.addView(TextView(this).apply {
                text =
                    "${habit.name}\n" +
                            "$completed of ${habit.target} days completed"

                textSize = 17f
                setTextColor(darkGreen)

                setPadding(
                    dp(4),
                    dp(12),
                    dp(4),
                    dp(12)
                )
            })
        }
    }
}

// Donut chart showing today's completion percentage.
private class DailyHabitDonutView(
    context: Context,
    private val percentage: Int,
    private val completed: Int,
    private val total: Int
) : View(context) {

    private val trackPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = Color.rgb(225, 236, 229)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = Color.rgb(39, 133, 104)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = Color.rgb(21, 62, 53)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val subtitlePaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = Color.GRAY
        textAlign = Paint.Align.CENTER
    }

    private val bounds = RectF()

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    private fun sp(value: Float): Float =
        value * resources.displayMetrics.scaledDensity

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        val stroke = dp(18f)
        val padding = stroke / 2f + dp(10f)

        trackPaint.strokeWidth = stroke
        progressPaint.strokeWidth = stroke

        bounds.set(
            padding,
            padding,
            width.toFloat() - padding,
            height.toFloat() - padding
        )

        canvas.drawArc(
            bounds,
            0f,
            360f,
            false,
            trackPaint
        )

        if (percentage > 0) {
            canvas.drawArc(
                bounds,
                -90f,
                360f * percentage / 100f,
                false,
                progressPaint
            )
        }

        textPaint.textSize = sp(36f)

        canvas.drawText(
            "$percentage%",
            centerX,
            centerY + dp(4f),
            textPaint
        )

        subtitlePaint.textSize = sp(14f)

        canvas.drawText(
            "$completed / $total completed",
            centerX,
            centerY + dp(29f),
            subtitlePaint
        )
    }
}

// Bar chart showing the number of habits
// completed on each of the last seven days.
private class DailyHabitBarChartView(
    context: Context,
    private val history: List<Pair<LocalDate, Int>>,
    private val totalHabits: Int
) : View(context) {

    private val barPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = Color.rgb(49, 158, 194)
    }

    private val gridPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = Color.rgb(225, 236, 229)
        strokeWidth = 2f
    }

    private val labelPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = Color.rgb(75, 92, 84)
        textAlign = Paint.Align.CENTER
    }

    private val bounds = RectF()

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    private fun sp(value: Float): Float =
        value * resources.displayMetrics.scaledDensity

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (history.isEmpty()) return

        val left = dp(12f)
        val right = width.toFloat() - dp(12f)
        val top = dp(28f)
        val bottom = height.toFloat() - dp(42f)

        val chartWidth = right - left
        val chartHeight = bottom - top

        if (chartWidth <= 0f || chartHeight <= 0f) {
            return
        }

        val maximum = maxOf(
            1,
            totalHabits,
            history.maxOfOrNull { it.second } ?: 0
        )

        // Horizontal grid lines.
        for (i in 0..4) {
            val y = bottom -
                    chartHeight * i / 4f

            canvas.drawLine(
                left,
                y,
                right,
                y,
                gridPaint
            )
        }

        val slotWidth = chartWidth / history.size
        val barWidth = slotWidth * 0.55f

        val dayFormatter =
            DateTimeFormatter.ofPattern(
                "EEE",
                Locale.getDefault()
            )

        history.forEachIndexed { index, (date, count) ->
            val centerX =
                left + slotWidth * (index + 0.5f)

            val barHeight =
                count.toFloat() / maximum * chartHeight

            val barTop = bottom - barHeight

            barPaint.color =
                if (date == LocalDate.now()) {
                    Color.rgb(39, 133, 104)
                } else {
                    Color.rgb(49, 158, 194)
                }

            if (count > 0) {
                bounds.set(
                    centerX - barWidth / 2f,
                    barTop,
                    centerX + barWidth / 2f,
                    bottom
                )

                canvas.drawRoundRect(
                    bounds,
                    dp(5f),
                    dp(5f),
                    barPaint
                )
            }

            labelPaint.textSize = sp(13f)

            canvas.drawText(
                count.toString(),
                centerX,
                (barTop - dp(8f))
                    .coerceAtLeast(dp(14f)),
                labelPaint
            )

            labelPaint.textSize = sp(11f)

            canvas.drawText(
                date.format(dayFormatter),
                centerX,
                bottom + dp(22f),
                labelPaint
            )
        }
    }
}
