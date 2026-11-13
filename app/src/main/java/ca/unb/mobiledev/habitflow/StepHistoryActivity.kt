
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
import android.widget.ScrollView
import android.widget.TextView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class StepHistoryActivity : Activity() {

    private lateinit var repository: StepTrackerRepository

    private val darkGreen = Color.rgb(21, 62, 53)
    private val green = Color.rgb(39, 133, 104)
    private val backgroundColor = Color.rgb(243, 247, 244)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun createCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(20).toFloat()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = StepTrackerRepository(this)
        createScreen()
    }

    override fun onResume() {
        super.onResume()

        if (::repository.isInitialized) {
            createScreen()
        }
    }

    private fun createScreen() {

        val history = repository.getSevenDayHistory()
        val average = repository.getSevenDayAverageSteps()
        val total = repository.getSevenDayTotalSteps()
        val goal = repository.getGoal()

        val goalsAchieved = history.count { (_, steps) ->
            steps >= goal.toLong()
        }

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(backgroundColor)
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(32))
        }

        container.addView(Button(this).apply {
            text = "← Back"
            isAllCaps = false
            setOnClickListener { finish() }
        })

        container.addView(TextView(this).apply {
            text = "Step History"
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            setTextColor(darkGreen)
            setPadding(0, dp(22), 0, dp(6))
        })

        container.addView(TextView(this).apply {
            text = "Your walking activity over the last 7 days"
            textSize = 15f
            setTextColor(Color.DKGRAY)
        })

        // Weekly statistics
        val statsCard = createCard()

        container.addView(
            statsCard,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(22)
            }
        )

        statsCard.addView(TextView(this).apply {
            text = "Average Daily Steps"
            textSize = 15f
            setTextColor(Color.GRAY)
        })

        statsCard.addView(TextView(this).apply {
            text = String.format(
                Locale.getDefault(),
                "%,d steps",
                average
            )
            textSize = 29f
            setTypeface(null, Typeface.BOLD)
            setTextColor(darkGreen)
            setPadding(0, dp(8), 0, dp(14))
        })

        statsCard.addView(TextView(this).apply {
            text = "7-day total: " +
                    String.format(Locale.getDefault(), "%,d", total)
            textSize = 16f
            setTextColor(darkGreen)
        })

        statsCard.addView(TextView(this).apply {
            text = "Daily goal: " +
                    String.format(Locale.getDefault(), "%,d", goal)
            textSize = 15f
            setTextColor(Color.GRAY)
            setPadding(0, dp(8), 0, 0)
        })

        statsCard.addView(TextView(this).apply {
            text = "Goal achieved: $goalsAchieved / 7 days"
            textSize = 15f
            setTextColor(green)
            setPadding(0, dp(8), 0, 0)
        })

        // Seven-day bar chart
        container.addView(TextView(this).apply {
            text = "Daily Step Count"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(darkGreen)
            setPadding(0, dp(28), 0, dp(12))
        })

        val chartCard = createCard()

        container.addView(
            chartCard,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        chartCard.addView(
            SevenDayStepChartView(this, history, goal),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(280)
            )
        )

        chartCard.addView(TextView(this).apply {
            text = "Dashed line = your daily step goal"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
            setPadding(0, dp(12), 0, 0)
        })

        // Daily breakdown
        container.addView(TextView(this).apply {
            text = "Daily Breakdown"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(darkGreen)
            setPadding(0, dp(26), 0, dp(12))
        })

        val dateFormatter = DateTimeFormatter.ofPattern(
            "EEE, MMM d",
            Locale.getDefault()
        )

        history.asReversed().forEach { (date, steps) ->

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(15), dp(16), dp(15))

                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(12).toFloat()
                }
            }

            row.addView(
                TextView(this).apply {
                    text = date.format(dateFormatter)
                    textSize = 15f
                    setTextColor(darkGreen)
                },
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            row.addView(TextView(this).apply {
                text = String.format(
                    Locale.getDefault(),
                    "%,d steps",
                    steps
                )
                textSize = 15f
                setTypeface(null, Typeface.BOLD)

                setTextColor(
                    if (steps >= goal.toLong()) green
                    else darkGreen
                )
            })

            container.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(6)
                }
            )
        }

        container.addView(TextView(this).apply {
            text = "The average includes all seven days, " +
                    "including days with no recorded steps. " +
                    "Steps are currently recorded only while " +
                    "the step tracker is active."
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(16), 0, 0)
        })

        scrollView.addView(container)
        setContentView(scrollView)
        fitSystemBars()
    }
}

private class SevenDayStepChartView(
    context: Context,
    private val history: List<Pair<LocalDate, Long>>,
    private val goal: Int
) : View(context) {

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    private fun sp(value: Float): Float =
        value * resources.displayMetrics.scaledDensity

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(49, 158, 194)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(224, 232, 227)
        strokeWidth = dp(1f)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(75, 92, 84)
        textSize = sp(10f)
        textAlign = Paint.Align.CENTER
    }

    private val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = sp(9f)
        textAlign = Paint.Align.RIGHT
    }

    private val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(39, 133, 104)
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        pathEffect = android.graphics.DashPathEffect(
            floatArrayOf(dp(6f), dp(4f)),
            0f
        )
    }

    private fun compactNumber(value: Long): String {
        return if (value >= 1000L) {
            String.format(
                Locale.getDefault(),
                "%.1fk",
                value / 1000.0
            )
        } else {
            value.toString()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (history.isEmpty()) return

        val left = dp(43f)
        val right = width.toFloat() - dp(12f)
        val top = dp(25f)
        val bottom = height.toFloat() - dp(43f)

        val chartWidth = right - left
        val chartHeight = bottom - top

        if (chartWidth <= 0f || chartHeight <= 0f) {
            return
        }

        val maximum = maxOf(
            goal.toLong(),
            history.maxOfOrNull { it.second } ?: 0L,
            1000L
        ).toFloat() * 1.1f

        // Horizontal guide lines and Y-axis labels
        for (i in 0..4) {
            val fraction = i / 4f
            val y = bottom - fraction * chartHeight

            canvas.drawLine(
                left, y, right, y, gridPaint
            )

            val value = (maximum * fraction).toLong()

            canvas.drawText(
                compactNumber(value),
                left - dp(7f),
                y + dp(4f),
                axisLabelPaint
            )
        }

        // Dashed daily goal line
        val goalY = bottom -
                (goal / maximum) * chartHeight

        canvas.drawLine(
            left, goalY,
            right, goalY,
            goalPaint
        )

        val slotWidth = chartWidth / history.size
        val barWidth = slotWidth * 0.55f

        val formatter = DateTimeFormatter.ofPattern(
            "EEE",
            Locale.getDefault()
        )

        history.forEachIndexed { index, (date, steps) ->

            val centerX = left +
                    slotWidth * (index + 0.5f)

            val barHeight =
                steps.toFloat() / maximum * chartHeight

            val barTop = bottom -
                    barHeight.coerceIn(0f, chartHeight)

            if (steps > 0L) {
                barPaint.color =
                    if (steps >= goal.toLong()) {
                        Color.rgb(39, 133, 104)
                    } else {
                        Color.rgb(49, 158, 194)
                    }

                canvas.drawRoundRect(
                    RectF(
                        centerX - barWidth / 2f,
                        barTop,
                        centerX + barWidth / 2f,
                        bottom
                    ),
                    dp(5f),
                    dp(5f),
                    barPaint
                )
            }

            // Day of the week
            canvas.drawText(
                date.format(formatter),
                centerX,
                bottom + dp(22f),
                labelPaint
            )

            // Step count above each bar
            canvas.drawText(
                compactNumber(steps),
                centerX,
                (barTop - dp(8f))
                    .coerceAtLeast(dp(12f)),
                labelPaint
            )
        }
    }
}
