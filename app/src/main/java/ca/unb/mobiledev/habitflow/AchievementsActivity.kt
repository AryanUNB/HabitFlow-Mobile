
package ca.unb.mobiledev.habitflow

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
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
import java.util.Locale
import kotlin.math.min

private data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val requirement: String
)

class AchievementsActivity : Activity() {

    private lateinit var repository: HabitRepository

    private val pageColor = Color.rgb(243, 247, 244)
    private val darkGreen = Color.rgb(21, 62, 53)
    private val cardColor = Color.rgb(28, 30, 36)
    private val gold = Color.rgb(242, 203, 125)

    private val achievements = listOf(
        AchievementDefinition(
            RewardBadges.FIRST_STEP,
            "First Step",
            "Every journey starts somewhere.",
            "Complete your first habit."
        ),
        AchievementDefinition(
            RewardBadges.MOMENTUM,
            "Momentum",
            "You're building something great.",
            "Reach 10 lifetime habit completions."
        ),
        AchievementDefinition(
            RewardBadges.HABIT_MASTER,
            "Habit Master",
            "A true commitment to your goals.",
            "Reach 50 lifetime habit completions."
        ),
        AchievementDefinition(
            RewardBadges.CONSISTENCY_KING,
            "Consistency King",
            "Seven days of showing up.",
            "Complete at least one habit on 7 consecutive days."
        ),
        AchievementDefinition(
            RewardBadges.PERFECT_DAY,
            "Perfect Day",
            "Everything on your list, completed.",
            "Complete every active habit in one day."
        )
    )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = HabitRepository(this)
    }

    override fun onResume() {
        super.onResume()
        renderScreen()
    }

    private fun roundedBackground(
        color: Int,
        radius: Int = 20
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun renderScreen() {
        val rewards = repository.getRewards()
        val earnedCount = achievements.count {
            it.id in rewards.unlockedBadges
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(pageColor)
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(36))
        }

        container.addView(Button(this).apply {
            text = "← Back"
            isAllCaps = false
            setOnClickListener { finish() }
        })

        container.addView(TextView(this).apply {
            text = "My Achievements"
            textSize = 29f
            setTypeface(null, Typeface.BOLD)
            setTextColor(darkGreen)
            setPadding(0, dp(22), 0, dp(6))
        })

        container.addView(TextView(this).apply {
            text = "Every achievement tells your story."
            textSize = 15f
            setTextColor(Color.DKGRAY)
        })

        // Token summary
        val summaryCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(24))
            background = roundedBackground(cardColor)
        }

        container.addView(
            summaryCard,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(22)
            }
        )

        summaryCard.addView(TextView(this).apply {
            text = "TOTAL TOKENS EARNED"
            textSize = 12f
            letterSpacing = 0.12f
            setTextColor(Color.LTGRAY)
        })

        summaryCard.addView(TextView(this).apply {
            text = String.format(
                Locale.getDefault(),
                "%,d",
                rewards.tokens
            )
            textSize = 40f
            setTypeface(null, Typeface.BOLD)
            setTextColor(gold)
            setPadding(0, dp(8), 0, dp(8))
        })

        summaryCard.addView(TextView(this).apply {
            text = "10 tokens per unique habit completion"
            textSize = 13f
            setTextColor(Color.LTGRAY)
        })

        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(22), 0, 0)
        }

        addStat(
            statsRow,
            "$earnedCount / ${achievements.size}",
            "Badges earned"
        )

        addStat(
            statsRow,
            "${rewards.currentStreakDays}",
            "Current streak"
        )

        summaryCard.addView(statsRow)

        container.addView(TextView(this).apply {
            text = "Achievement Collection"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(darkGreen)
            setPadding(0, dp(26), 0, dp(6))
        })

        container.addView(TextView(this).apply {
            text = "Tap a medal to see how it's earned."
            textSize = 14f
            setTextColor(Color.GRAY)
        })

        // Two-column badge gallery.
        achievements.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            container.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(14)
                }
            )

            pair.forEach { achievement ->
                val earned =
                    achievement.id in rewards.unlockedBadges

                val badgeCard = createBadgeCard(
                    achievement,
                    earned
                )

                row.addView(
                    badgeCard,
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        marginStart = dp(5)
                        marginEnd = dp(5)
                    }
                )
            }

            if (pair.size == 1) {
                row.addView(
                    View(this),
                    LinearLayout.LayoutParams(0, 1, 1f)
                )
            }
        }

        container.addView(TextView(this).apply {
            text = "Earned achievements remain unlocked. " +
                    "Tokens are cumulative and cannot be earned twice " +
                    "for the same habit on the same day."
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(24), 0, 0)
        })

        scroll.addView(container)
        setContentView(scroll)
        fitSystemBars()
    }

    private fun addStat(
        parent: LinearLayout,
        value: String,
        title: String
    ) {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        column.addView(TextView(this).apply {
            text = value
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        })

        column.addView(TextView(this).apply {
            text = title
            textSize = 12f
            setTextColor(Color.LTGRAY)
        })

        parent.addView(
            column,
            LinearLayout.LayoutParams(0, -2, 1f)
        )
    }

    private fun createBadgeCard(
        achievement: AchievementDefinition,
        earned: Boolean
    ): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            minimumHeight = dp(218)

            setPadding(
                dp(8), dp(15), dp(8), dp(18)
            )

            background = roundedBackground(cardColor)
            isClickable = true
            isFocusable = true

            setOnClickListener {
                showAchievementDetails(achievement, earned)
            }
        }

        card.addView(
            AchievementMedalView(
                this,
                achievement.id,
                earned
            ),
            LinearLayout.LayoutParams(
                dp(120),
                dp(120)
            )
        )

        card.addView(TextView(this).apply {
            text = achievement.title
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(
                if (earned) Color.WHITE
                else Color.LTGRAY
            )
            setPadding(0, dp(9), 0, 0)
        })

        card.addView(TextView(this).apply {
            text = if (earned) "EARNED" else "LOCKED"
            textSize = 11f
            letterSpacing = 0.1f
            gravity = Gravity.CENTER
            setTextColor(
                if (earned) gold
                else Color.GRAY
            )
            setPadding(0, dp(7), 0, 0)
        })

        return card
    }

    private fun showAchievementDetails(
        achievement: AchievementDefinition,
        earned: Boolean
    ) {
        AlertDialog.Builder(this)
            .setTitle(achievement.title)
            .setMessage(
                achievement.description + "\n\n" +
                        "How to earn:\n" +
                        achievement.requirement + "\n\n" +
                        "Status: " +
                        if (earned) "Unlocked" else "Locked"
            )
            .setPositiveButton("Close", null)
            .show()
    }
}

/**
 * Custom illustrated medallions.
 * Artwork is drawn directly on Canvas,
 * so no external image assets are required.
 */
private class AchievementMedalView(
    context: Context,
    private val badgeId: String,
    private val unlocked: Boolean
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val gold = Color.rgb(251, 216, 147)
    private val silver = Color.rgb(200, 229, 245)
    private val copper = Color.rgb(240, 168, 121)
    private val grey = Color.rgb(125, 127, 135)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val side = min(width, height).toFloat()
        if (side <= 0f) return

        canvas.save()
        canvas.translate(
            (width - side) / 2f,
            (height - side) / 2f
        )
        canvas.scale(side / 200f, side / 200f)

        val accent = if (!unlocked) {
            grey
        } else {
            when (badgeId) {
                RewardBadges.FIRST_STEP -> copper
                RewardBadges.MOMENTUM -> silver
                else -> gold
            }
        }

        // Metallic outer ring.
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            35f, 25f, 165f, 175f,
            intArrayOf(
                accent,
                Color.rgb(90, 80, 79),
                accent
            ),
            null,
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(100f, 100f, 83f, paint)
        paint.shader = null

        // Dark engraved face.
        paint.color =
            if (unlocked) Color.rgb(39, 39, 49)
            else Color.rgb(62, 63, 68)

        canvas.drawCircle(100f, 100f, 71f, paint)

        // Delicate inner gold outline.
        paint.color = accent
        paint.alpha = 185
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f

        canvas.drawCircle(100f, 100f, 65f, paint)

        paint.style = Paint.Style.FILL
        paint.alpha = 255

        // Engraved decorative stars.
        drawSparkle(canvas, 57f, 60f, 5f, accent)
        drawSparkle(canvas, 147f, 71f, 4f, accent)
        drawSparkle(canvas, 151f, 132f, 3f, accent)

        when (badgeId) {
            RewardBadges.FIRST_STEP ->
                drawFootsteps(canvas, accent)

            RewardBadges.MOMENTUM ->
                drawLightning(canvas, accent)

            RewardBadges.HABIT_MASTER ->
                drawCrown(canvas, accent)

            RewardBadges.CONSISTENCY_KING ->
                drawSeven(canvas, accent)

            RewardBadges.PERFECT_DAY ->
                drawMoon(canvas, accent)
        }

        if (!unlocked) {
            drawLock(canvas)
        }

        canvas.restore()
    }

    private fun drawSparkle(
        canvas: Canvas,
        x: Float,
        y: Float,
        radius: Float,
        color: Int
    ) {
        val path = Path().apply {
            moveTo(x, y - radius)
            lineTo(x + radius * 0.25f, y - radius * 0.25f)
            lineTo(x + radius, y)
            lineTo(x + radius * 0.25f, y + radius * 0.25f)
            lineTo(x, y + radius)
            lineTo(x - radius * 0.25f, y + radius * 0.25f)
            lineTo(x - radius, y)
            lineTo(x - radius * 0.25f, y - radius * 0.25f)
            close()
        }

        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
    }

    private fun drawFootsteps(
        canvas: Canvas,
        color: Int
    ) {
        paint.color = color

        canvas.save()
        canvas.rotate(-25f, 80f, 103f)

        canvas.drawOval(
            RectF(66f, 91f, 86f, 121f),
            paint
        )
        canvas.drawCircle(72f, 84f, 5f, paint)
        canvas.drawCircle(81f, 82f, 4f, paint)
        canvas.drawCircle(89f, 86f, 3f, paint)

        canvas.restore()

        canvas.save()
        canvas.rotate(25f, 120f, 103f)

        canvas.drawOval(
            RectF(112f, 75f, 132f, 105f),
            paint
        )
        canvas.drawCircle(116f, 68f, 5f, paint)
        canvas.drawCircle(125f, 66f, 4f, paint)
        canvas.drawCircle(133f, 70f, 3f, paint)

        canvas.restore()
    }

    private fun drawLightning(
        canvas: Canvas,
        color: Int
    ) {
        val lightning = Path().apply {
            moveTo(108f, 56f)
            lineTo(78f, 104f)
            lineTo(101f, 104f)
            lineTo(90f, 148f)
            lineTo(132f, 91f)
            lineTo(107f, 91f)
            close()
        }

        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawPath(lightning, paint)

        drawSparkle(canvas, 67f, 112f, 6f, color)
        drawSparkle(canvas, 133f, 68f, 5f, color)
    }

    private fun drawCrown(
        canvas: Canvas,
        color: Int
    ) {
        val crown = Path().apply {
            moveTo(59f, 89f)
            lineTo(74f, 111f)
            lineTo(85f, 75f)
            lineTo(101f, 105f)
            lineTo(117f, 75f)
            lineTo(128f, 111f)
            lineTo(143f, 89f)
            lineTo(132f, 137f)
            lineTo(70f, 137f)
            close()
        }

        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawPath(crown, paint)

        paint.color = Color.rgb(95, 65, 62)

        canvas.drawCircle(101f, 116f, 5f, paint)
        canvas.drawCircle(82f, 123f, 3f, paint)
        canvas.drawCircle(120f, 123f, 3f, paint)
    }

    private fun drawSeven(
        canvas: Canvas,
        color: Int
    ) {
        paint.color = color
        paint.typeface = Typeface.create(
            Typeface.SERIF,
            Typeface.BOLD
        )
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 78f

        canvas.drawText("7", 100f, 127f, paint)

        paint.textSize = 15f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("DAYS", 100f, 145f, paint)

        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawMoon(
        canvas: Canvas,
        color: Int
    ) {
        // Golden moon with a dark circular overlap.
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawCircle(92f, 102f, 43f, paint)

        paint.color = Color.rgb(39, 39, 49)
        canvas.drawCircle(109f, 85f, 39f, paint)

        drawSparkle(canvas, 123f, 79f, 9f, color)
        drawSparkle(canvas, 143f, 105f, 5f, color)
        drawSparkle(canvas, 104f, 59f, 4f, color)
    }

    private fun drawLock(canvas: Canvas) {
        paint.color = Color.rgb(47, 48, 54)
        paint.style = Paint.Style.FILL

        canvas.drawRoundRect(
            RectF(130f, 139f, 169f, 169f),
            6f,
            6f,
            paint
        )

        paint.color = Color.LTGRAY
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f

        canvas.drawArc(
            RectF(140f, 127f, 159f, 150f),
            180f,
            180f,
            false,
            paint
        )

        paint.style = Paint.Style.FILL
        canvas.drawCircle(149.5f, 155f, 3f, paint)
    }
}
