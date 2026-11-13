
package ca.unb.mobiledev.habitflow

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.text.NumberFormat

class StepTrackerActivity : Activity(), SensorEventListener {

    private lateinit var repository: StepTrackerRepository
    private lateinit var sensorManager: SensorManager

    private var stepSensor: Sensor? = null
    private var isListening = false
    private var goalCelebrated = false

    private lateinit var stepCountText: TextView
    private lateinit var goalText: TextView
    private lateinit var percentageText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var goalRing: StepGoalRingView

    private val darkGreen = Color.rgb(21, 62, 53)
    private val green = Color.rgb(39, 133, 104)

    private val formatter = NumberFormat.getIntegerInstance()

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = StepTrackerRepository(this)

        sensorManager = getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager

        stepSensor = sensorManager.getDefaultSensor(
            Sensor.TYPE_STEP_COUNTER
        )

        createScreen()
        updateProgress()
    }

    private fun createScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(243, 247, 244))
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(32))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        container.addView(Button(this).apply {
            text = "← Back"
            isAllCaps = false
            setOnClickListener { finish() }
        })

        container.addView(TextView(this).apply {
            text = "Step Counter"
            textSize = 30f
            setTextColor(darkGreen)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(28), 0, dp(10))
        })

        container.addView(TextView(this).apply {
            text = "Every step counts."
            textSize = 17f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(28), dp(24), dp(28))

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
                topMargin = dp(26)
            }
        )

        // Donut chart surrounding the shoe icon.
        val ringContainer = FrameLayout(this)

        goalRing = StepGoalRingView(this)

        ringContainer.addView(
            goalRing,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        ringContainer.addView(
            TextView(this).apply {
                text = "👟"
                textSize = 56f
                gravity = Gravity.CENTER
                contentDescription = "Step goal progress"
            },
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        card.addView(
            ringContainer,
            LinearLayout.LayoutParams(
                dp(190),
                dp(190)
            )
        )

        stepCountText = TextView(this).apply {
            textSize = 45f
            setTypeface(null, Typeface.BOLD)
            setTextColor(darkGreen)
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        }

        card.addView(stepCountText)

        card.addView(TextView(this).apply {
            text = "STEPS TODAY"
            textSize = 13f
            letterSpacing = 0.1f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
        })

        goalText = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(darkGreen)
            setPadding(0, dp(24), 0, dp(12))
        }

        card.addView(goalText)

        progressBar = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 1000
            progressTintList = ColorStateList.valueOf(green)
            progressBackgroundTintList =
                ColorStateList.valueOf(
                    Color.rgb(225, 236, 229)
                )
        }

        card.addView(
            progressBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(14)
            )
        )

        percentageText = TextView(this).apply {
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(green)
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(16))
        }

        card.addView(percentageText)

        card.addView(Button(this).apply {
            text = "Change Daily Goal"
            isAllCaps = false
            setTextColor(green)
            setOnClickListener { showGoalDialog() }
        })

        // Open the seven-day statistics screen.
        val historyButton = Button(this).apply {
            text = "View Weekly History"
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)

            background = GradientDrawable().apply {
                setColor(darkGreen)
                cornerRadius = dp(14).toFloat()
            }

            setOnClickListener {
                startActivity(
                    Intent(
                        this@StepTrackerActivity,
                        StepHistoryActivity::class.java
                    )
                )
            }
        }

        card.addView(
            historyButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply {
                topMargin = dp(12)
            }
        )

        statusText = TextView(this).apply {
            text = "Checking step sensor..."
            textSize = 14f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(10))
        }

        container.addView(statusText)

        container.addView(TextView(this).apply {
            text = "Keep your phone with you while walking to count steps."
            textSize = 13f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        })

        scroll.addView(container)
        setContentView(scroll)
        fitSystemBars()
    }

    private fun updateProgress() {
        val steps = repository.getSteps()
        val goal = repository.getGoal()

        val percentage = steps.toDouble() / goal * 100.0

        stepCountText.text = formatter.format(steps)
        goalText.text =
            "Daily goal: ${formatter.format(goal)} steps"

        progressBar.progress =
            (percentage * 10.0)
                .coerceIn(0.0, 1000.0)
                .toInt()

        // The ring fills to 100%.
        // The percentage label can show values above 100%.
        goalRing.setPercentage(
            percentage.toFloat()
        )

        percentageText.text = when {
            steps >= goal ->
                "Goal achieved! ${percentage.toInt()}%"

            else ->
                "${percentage.toInt()}% completed"
        }
    }

    private fun showGoalDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setText(repository.getGoal().toString())
            selectAll()
        }

        val holder = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(8))
            addView(input)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Set Daily Step Goal")
            .setMessage("Choose between 1,000 and 100,000 steps.")
            .setView(holder)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val goal = input.text.toString()
                    .trim()
                    .toIntOrNull()

                if (
                    goal == null ||
                    goal !in StepTrackerRepository.MIN_GOAL..
                    StepTrackerRepository.MAX_GOAL
                ) {
                    input.error = "Enter 1,000 to 100,000"
                    return@setOnClickListener
                }

                if (repository.saveGoal(goal)) {
                    goalCelebrated =
                        repository.getSteps() >= goal

                    updateProgress()
                    dialog.dismiss()
                } else {
                    Toast.makeText(
                        this,
                        "Could not save goal",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()

        updateProgress()

        goalCelebrated =
            repository.getSteps() >= repository.getGoal()

        startTracking()
    }

    private fun startTracking() {
        val sensor = stepSensor

        if (sensor == null) {
            statusText.text =
                "Step counter sensor unavailable on this device."
            return
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            checkSelfPermission(
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            statusText.text =
                "Physical activity permission required."

            requestPermissions(
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                2001
            )
            return
        }

        if (!isListening) {
            isListening = sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        statusText.text =
            if (isListening) {
                "Tracking steps using your phone's sensor"
            } else {
                "Unable to start step tracking"
            }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) {
            return
        }

        val total = event.values.firstOrNull()
            ?.toLong() ?: return

        val previousSteps = repository.getSteps()

        val steps = repository.recordSensorReading(total)
        updateProgress()

        val goal = repository.getGoal()

        if (
            previousSteps < goal &&
            steps >= goal &&
            !goalCelebrated
        ) {
            goalCelebrated = true

            Toast.makeText(
                this,
                "Daily step goal achieved!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
        // No special handling required.
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 2001) {
            if (
                grantResults.isNotEmpty() &&
                grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {
                startTracking()
            } else {
                statusText.text =
                    "Physical activity permission denied."
            }
        }
    }

    override fun onPause() {
        if (isListening) {
            sensorManager.unregisterListener(this)
            isListening = false
        }

        super.onPause()
    }
}

// Circular progress indicator around the shoe icon.
private class StepGoalRingView(
    context: Context
) : View(context) {

    private var percentage = 0f

    private val trackPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(225, 236, 229)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

    private val progressPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(39, 133, 104)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

    private val arcBounds = RectF()

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    fun setPercentage(value: Float) {
        percentage = value.coerceIn(0f, 100f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val stroke = dp(14f)
        val padding = stroke / 2f + dp(8f)

        trackPaint.strokeWidth = stroke
        progressPaint.strokeWidth = stroke

        arcBounds.set(
            padding,
            padding,
            width.toFloat() - padding,
            height.toFloat() - padding
        )

        canvas.drawArc(
            arcBounds,
            0f,
            360f,
            false,
            trackPaint
        )

        if (percentage > 0f) {
            canvas.drawArc(
                arcBounds,
                -90f,
                360f * percentage / 100f,
                false,
                progressPaint
            )
        }
    }
}
