
package ca.unb.mobiledev.habitflow

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Button
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class WaterHistoryActivity : Activity() {

    private lateinit var repository: WaterTrackerRepository

    private val darkGreen = Color.rgb(21, 62, 53)
    private val green = Color.rgb(39, 133, 104)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = WaterTrackerRepository(this)
        createScreen()
    }

    private fun createScreen() {
        val history = repository.getSevenDayHistory()
        val average = repository.getSevenDayAverageMl()
        val total = history.sumOf { it.second }
        val goal = repository.getGoalMl()

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(243, 247, 244))
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(32))
        }

        container.addView(Button(this).apply {
            text = "← Back"
            isAllCaps = false
            setOnClickListener { finish() }
        })

        container.addView(TextView(this).apply {
            text = "Water History"
            textSize = 29f
            setTextColor(darkGreen)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(22), 0, dp(6))
        })

        container.addView(TextView(this).apply {
            text = "Your hydration over the last 7 days"
            textSize = 16f
            setTextColor(Color.DKGRAY)
        })

        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(Color.WHITE)
        }

        container.addView(
            stats,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(22)
            }
        )

        stats.addView(TextView(this).apply {
            text = "Daily average"
            textSize = 15f
            setTextColor(Color.GRAY)
        })

        stats.addView(TextView(this).apply {
            text = "$average mL"
            textSize = 30f
            setTextColor(darkGreen)
            setTypeface(null, Typeface.BOLD)
        })

        stats.addView(TextView(this).apply {
            text = "7-day total: $total mL"
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(10), 0, 0)
        })

        stats.addView(TextView(this).apply {
            text = "Current daily goal: $goal mL"
            textSize = 14f
            setTextColor(Color.GRAY)
            setPadding(0, dp(6), 0, 0)
        })

        container.addView(TextView(this).apply {
            text = "Daily Water Intake"
            textSize = 21f
            setTextColor(darkGreen)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(28), 0, dp(12))
        })

        container.addView(
            SevenDayWaterChartView(this, history, goal),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(290)
            )
        )

        container.addView(TextView(this).apply {
            text = "Daily breakdown"
            textSize = 21f
            setTextColor(darkGreen)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(24), 0, dp(12))
        })

        val dateFormatter =
            DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

        history.asReversed().forEach { (date, amount) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setBackgroundColor(Color.WHITE)
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
                text = "$amount mL"
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(green)
            })

            container.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(3)
                }
            )
        }

        container.addView(TextView(this).apply {
            text = "Average includes all 7 days, including days with no logged water."
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(14), 0, 0)
        })

        scroll.addView(container)
        setContentView(scroll)
        fitSystemBars()
    }
}

private class SevenDayWaterChartView(
    context: Context,
    private val history: List<Pair<LocalDate, Long>>,
    private val goalMl: Int
) : View(context) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(49, 158, 194)
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(200, 212, 206)
        strokeWidth = 2f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(75, 92, 84)
        textSize = sp(11f)
        textAlign = Paint.Align.CENTER
    }

    private val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(39, 133, 104)
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
        pathEffect = android.graphics.DashPathEffect(
            floatArrayOf(dp(6f), dp(4f)),
            0f
        )
    }

    private val maxValue: Float = maxOf(
        goalMl.toLong(),
        history.maxOfOrNull { it.second } ?: 0L,
        250L
    ).toFloat()

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    private fun sp(value: Float): Float =
        value * resources.displayMetrics.scaledDensity

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        val left = dp(14f)
        val right = w - dp(14f)
        val top = dp(25f)
        val bottom = h - dp(45f)

        val chartHeight = bottom - top
        val chartWidth = right - left

        for (i in 0..4) {
            val y = bottom - chartHeight * i / 4f

            canvas.drawLine(
                left, y, right, y, axisPaint
            )
        }

        val goalY = bottom -
                (goalMl / maxValue) * chartHeight

        canvas.drawLine(
            left, goalY, right, goalY, goalPaint
        )

        val slotWidth = chartWidth / 7f
        val barWidth = slotWidth * 0.52f

        val formatter = DateTimeFormatter.ofPattern(
            "EEE", Locale.getDefault()
        )

        history.forEachIndexed { index, (date, amount) ->
            val centerX = left +
                    slotWidth * (index + 0.5f)

            val barHeight =
                (amount / maxValue * chartHeight)
                    .toFloat()
                    .coerceAtMost(chartHeight)

            val barTop = bottom - barHeight

            if (amount > 0L) {
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

            canvas.drawText(
                date.format(formatter),
                centerX,
                bottom + dp(20f),
                labelPaint
            )

            val amountLabel =
                if (amount >= 1000L) {
                    String.format(
                        Locale.US,
                        "%.1fk",
                        amount / 1000.0
                    )
                } else {
                    amount.toString()
                }

            canvas.drawText(
                amountLabel,
                centerX,
                (barTop - dp(7f))
                    .coerceAtLeast(dp(12f)),
                labelPaint
            )
        }
    }
}
