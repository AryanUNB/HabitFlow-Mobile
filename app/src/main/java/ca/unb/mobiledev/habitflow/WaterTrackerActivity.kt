
package ca.unb.mobiledev.habitflow

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class WaterTrackerActivity : Activity() {

    private lateinit var repository: WaterTrackerRepository
    private lateinit var waterGlass: WaterGlassView
    private lateinit var progressText: TextView
    private lateinit var messageText: TextView
    private lateinit var goalText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var addButton: Button
    private lateinit var celebration: WaterConfettiView

    private val darkGreen = Color.rgb(21, 62, 53)
    private val green = Color.rgb(39, 133, 104)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = WaterTrackerRepository(this)

        createScreen()
        updateProgress(false)
    }

    private fun createScreen() {

        val root = FrameLayout(this)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(243, 247, 244))
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(32))
        }

        val backButton = Button(this).apply {
            text = "← Back"
            isAllCaps = false
            setOnClickListener { finish() }
        }

        container.addView(backButton)

        val title = TextView(this).apply {
            text = "Water Tracker"
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            setTextColor(darkGreen)
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(8))
        }

        container.addView(title)

        container.addView(TextView(this).apply {
            text = "Stay hydrated, one sip at a time!"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(20), dp(18), dp(20))

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(22).toFloat()
            }
        }

        container.addView(
            card,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(24)
            }
        )

        progressText = TextView(this).apply {
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(darkGreen)
            setTypeface(null, Typeface.BOLD)
        }

        card.addView(progressText)

        goalText = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
            setPadding(0, dp(6), 0, dp(12))
        }

        card.addView(goalText)

        progressBar = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 1000

            progressTintList =
                ColorStateList.valueOf(green)

            progressBackgroundTintList =
                ColorStateList.valueOf(
                    Color.rgb(225, 236, 229)
                )
        }

        card.addView(
            progressBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(12)
            )
        )

        waterGlass = WaterGlassView(this)

        card.addView(
            waterGlass,
            LinearLayout.LayoutParams(
                dp(260),
                dp(270)
            ).apply {
                topMargin = dp(10)
            }
        )

        messageText = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(darkGreen)
            setPadding(0, dp(4), 0, dp(18))
        }

        card.addView(messageText)

        addButton = Button(this).apply {
            text = "+250 mL"
            textSize = 19f
            isAllCaps = false
            setTextColor(Color.WHITE)

            background = GradientDrawable().apply {
                setColor(green)
                cornerRadius = dp(16).toFloat()
            }

            setOnClickListener {
                if (repository.addGlass()) {
                    updateProgress(true)

                    val consumed = repository.getWaterMl()
                    val goal = repository.getGoalMl()

                    if (consumed >= goal.toLong()) {
                        celebration.play(waterGlass)

                        if (consumed > goal.toLong()) {
                            waterGlass.animateOverflow()
                        }
                    }
                } else {
                    Toast.makeText(
                        this@WaterTrackerActivity,
                        "Unable to save water intake.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        card.addView(
            addButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)
            )
        )

        val editGoalButton = Button(this).apply {
            text = "Change Daily Goal"
            isAllCaps = false
            textSize = 15f
            setTextColor(green)

            setOnClickListener {
                showGoalDialog()
            }
        }

        card.addView(
            editGoalButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply {
                topMargin = dp(10)
            }
        )

        container.addView(TextView(this).apply {
            text = "Water intake is saved automatically."
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
            setPadding(0, dp(20), 0, 0)
        })

        scrollView.addView(container)

        root.addView(
            scrollView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        celebration = WaterConfettiView(this).apply {
            isClickable = false
            isFocusable = false
            importantForAccessibility =
                View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        root.addView(
            celebration,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(root)
        fitSystemBars()
    }

    private fun showGoalDialog() {

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setText(repository.getGoalMl().toString())
            selectAll()
            hint = "Daily goal in mL"
        }

        val holder = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(8))
            addView(input)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Set Your Daily Water Goal")
            .setMessage(
                "Default: 2,000 mL. Choose a goal from 250 to 10,000 mL."
            )
            .setView(holder)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val newGoal = input.text.toString()
                    .trim()
                    .toIntOrNull()

                if (
                    newGoal == null ||
                    newGoal !in
                    WaterTrackerRepository.MIN_GOAL_ML..
                    WaterTrackerRepository.MAX_GOAL_ML
                ) {
                    input.error =
                        "Enter a value from 250 to 10,000"

                    return@setOnClickListener
                }

                if (repository.saveGoalMl(newGoal)) {
                    dialog.dismiss()
                    updateProgress(false)
                } else {
                    Toast.makeText(
                        this,
                        "Could not save your goal.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialog.show()
    }

    private fun updateProgress(animate: Boolean) {

        val glasses = repository.getGlasses()
        val consumed = repository.getWaterMl()
        val goal = repository.getGoalMl()

        waterGlass.setProgress(
            glasses,
            goal,
            animate
        )

        progressText.text = "$consumed mL"
        goalText.text = "Daily goal: $goal mL"

        val percentage =
            consumed.toDouble() / goal * 100.0

        val newProgress = (percentage * 10.0)
            .coerceIn(0.0, 1000.0)
            .toInt()

        progressBar.setProgress(
            newProgress,
            animate
        )

        messageText.text = when {
            consumed == 0L ->
                "Ready to start your hydration goal?"

            consumed < goal.toLong() ->
                "${percentage.toInt()}% of your daily goal"

            consumed == goal.toLong() ->
                "Goal achieved! Great work!"

            else ->
                "${consumed - goal} mL above your goal!"
        }

        addButton.isEnabled =
            glasses < Int.MAX_VALUE
    }

    override fun onResume() {
        super.onResume()

        if (::waterGlass.isInitialized) {
            updateProgress(false)
            waterGlass.startAnimations()
        }
    }

    override fun onPause() {

        if (::waterGlass.isInitialized) {
            waterGlass.stopAnimations()
        }

        if (::celebration.isInitialized) {
            celebration.stop()
        }

        super.onPause()
    }
}

/**
 * Celebration with physically inspired falling confetti
 * and flowing, curling party streamers.
 *
 * Runs above the tracker without blocking touches.
 */
private class WaterConfettiView(
    context: Context
) : View(context) {

    private data class ConfettiPiece(
        val side: Int,
        val velocityX: Float,
        val velocityY: Float,
        val delay: Float,
        val rotation: Float,
        val spin: Float,
        val width: Float,
        val height: Float,
        val color: Int,
        val flutterPhase: Float,
        val circular: Boolean
    )

    private data class PartyStreamer(
        val side: Int,
        val velocityX: Float,
        val velocityY: Float,
        val delay: Float,
        val phase: Float,
        val color: Int,
        val curl: Float
    )

    private val colors = intArrayOf(
        Color.rgb(255, 184, 45),
        Color.rgb(255, 95, 126),
        Color.rgb(46, 183, 117),
        Color.rgb(54, 171, 242),
        Color.rgb(164, 113, 244),
        Color.rgb(255, 140, 61),
        Color.rgb(60, 208, 185)
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val ribbonPath = Path()

    private var pieces = emptyList<ConfettiPiece>()
    private var streamers = emptyList<PartyStreamer>()

    private var progress = -1f
    private var animator: ValueAnimator? = null

    private var sourceLeftX = 0f
    private var sourceRightX = 0f
    private var sourceY = 0f

    private val durationSeconds = 2.4f

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    /**
     * Start two confetti bursts near the animated water glass.
     */
    fun play(anchor: View) {
        animator?.cancel()

        val anchorLocation = IntArray(2)
        val overlayLocation = IntArray(2)

        anchor.getLocationOnScreen(anchorLocation)
        getLocationOnScreen(overlayLocation)

        val anchorLeft =
            (anchorLocation[0] - overlayLocation[0]).toFloat()

        val anchorTop =
            (anchorLocation[1] - overlayLocation[1]).toFloat()

        sourceLeftX =
            anchorLeft + anchor.width * 0.14f

        sourceRightX =
            anchorLeft + anchor.width * 0.86f

        sourceY =
            anchorTop + anchor.height * 0.48f

        val random = Random.Default

        // Generate individual confetti particles.
        pieces = List(90) { index ->

            val side = if (index % 2 == 0) 0 else 1

            val direction =
                if (side == 0) 1f else -1f

            val velocityX = dp(
                direction * (30f + random.nextFloat() * 95f) +
                        (random.nextFloat() - 0.5f) * 180f
            )

            val velocityY = -dp(
                145f + random.nextFloat() * 215f
            )

            ConfettiPiece(
                side = side,
                velocityX = velocityX,
                velocityY = velocityY,
                delay = random.nextFloat() * 0.13f,
                rotation = random.nextFloat() * 360f,
                spin = (random.nextFloat() - 0.5f) * 1100f,
                width = dp(3.5f + random.nextFloat() * 3.5f),
                height = dp(6f + random.nextFloat() * 6f),
                color = colors[index % colors.size],
                flutterPhase = random.nextFloat() *
                        (2f * PI.toFloat()),
                circular = index % 8 == 0
            )
        }

        // Colored curling party streamers.
        streamers = List(7) { index ->

            val side = if (index % 2 == 0) 0 else 1
            val direction =
                if (side == 0) 1f else -1f

            PartyStreamer(
                side = side,
                velocityX = dp(
                    direction * (
                            45f + random.nextFloat() * 85f
                            )
                ),
                velocityY = -dp(
                    170f + random.nextFloat() * 100f
                ),
                delay = index * 0.045f,
                phase = random.nextFloat() *
                        2f * PI.toFloat(),
                color = colors[(index + 2) % colors.size],
                curl = dp(
                    8f + random.nextFloat() * 8f
                )
            )
        }

        progress = 0f

        val animation = ValueAnimator.ofFloat(
            0f,
            1f
        ).apply {
            duration = 2400L
            interpolator = LinearInterpolator()
        }

        animator = animation

        animation.addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }

        animation.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(
                    animation: Animator
                ) {
                    if (animator === animation) {
                        progress = -1f
                        animator = null
                        invalidate()
                    }
                }
            }
        )

        animation.start()
    }

    fun stop() {
        animator?.cancel()
        animator = null
        progress = -1f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (progress < 0f || progress >= 1f) {
            return
        }

        val time = progress * durationSeconds

        // Draw ribbons behind the confetti.
        drawPartyStreamers(canvas, time)

        // Draw free-falling confetti in front.
        drawConfetti(canvas, time)
    }

    private fun drawConfetti(
        canvas: Canvas,
        elapsed: Float
    ) {
        val gravity = dp(300f)

        for (piece in pieces) {
            val time = elapsed - piece.delay

            if (time <= 0f) continue

            val originX =
                if (piece.side == 0) {
                    sourceLeftX
                } else {
                    sourceRightX
                }

            // Horizontal air resistance.
            val x =
                originX +
                        piece.velocityX * time /
                        (1f + 0.18f * time) +
                        sin(
                            time * 8f + piece.flutterPhase
                        ) * dp(6f)

            // Gravity gradually pulls each piece down.
            val y =
                sourceY +
                        piece.velocityY * time +
                        0.5f * gravity * time * time

            // Fade near the end of the animation.
            val fade = if (time > 1.7f) {
                ((2.4f - time) / 0.7f)
                    .coerceIn(0f, 1f)
            } else {
                1f
            }

            paint.color = piece.color
            paint.alpha =
                (fade * 255f).toInt().coerceIn(0, 255)
            paint.style = Paint.Style.FILL

            // Tumbling changes apparent particle width.
            val tumble = cos(
                time * 11f + piece.flutterPhase
            )

            val visibleWidth =
                piece.width *
                        (0.30f + 0.70f * kotlin.math.abs(tumble))

            canvas.save()

            canvas.rotate(
                piece.rotation + piece.spin * time,
                x,
                y
            )

            if (piece.circular) {
                canvas.drawCircle(
                    x,
                    y,
                    visibleWidth * 0.75f,
                    paint
                )
            } else {
                canvas.drawRoundRect(
                    x - visibleWidth / 2f,
                    y - piece.height / 2f,
                    x + visibleWidth / 2f,
                    y + piece.height / 2f,
                    dp(1f),
                    dp(1f),
                    paint
                )
            }

            canvas.restore()
        }

        paint.alpha = 255
    }

    private fun drawPartyStreamers(
        canvas: Canvas,
        elapsed: Float
    ) {
        val gravity = dp(195f)

        for (streamer in streamers) {
            val time = elapsed - streamer.delay

            if (time <= 0f) continue

            val originX =
                if (streamer.side == 0) {
                    sourceLeftX
                } else {
                    sourceRightX
                }

            // A streamer trails behind its moving tip.
            val trailStart = (
                    time - 0.85f
                    ).coerceAtLeast(0f)

            ribbonPath.reset()

            val samples = 32

            for (i in 0..samples) {
                val fraction = i.toFloat() / samples

                val t =
                    trailStart +
                            (time - trailStart) * fraction

                // Broad arc with gravity.
                val baseX =
                    originX +
                            streamer.velocityX * t /
                            (1f + 0.22f * t)

                val baseY =
                    sourceY +
                            streamer.velocityY * t +
                            0.5f * gravity * t * t

                // Continuous curls that unravel as the ribbon flies.
                val curlX = sin(
                    t * 17f + streamer.phase
                ) * streamer.curl *
                        (t * 2.5f).coerceIn(0f, 1f)

                val curlY = cos(
                    t * 17f + streamer.phase
                ) * streamer.curl * 0.55f

                val x = baseX + curlX
                val y = baseY + curlY

                if (i == 0) {
                    ribbonPath.moveTo(x, y)
                } else {
                    ribbonPath.lineTo(x, y)
                }
            }

            val fade = if (time > 1.75f) {
                ((2.4f - time) / 0.65f)
                    .coerceIn(0f, 1f)
            } else {
                1f
            }

            ribbonPaint.color = streamer.color
            ribbonPaint.alpha =
                (fade * 230f).toInt().coerceIn(0, 255)
            ribbonPaint.strokeWidth = dp(3.2f)

            canvas.drawPath(
                ribbonPath,
                ribbonPaint
            )
        }

        ribbonPaint.alpha = 255
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }
}
