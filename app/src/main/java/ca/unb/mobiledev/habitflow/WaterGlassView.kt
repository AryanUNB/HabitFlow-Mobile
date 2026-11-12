
package ca.unb.mobiledev.habitflow

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos

class WaterGlassView(context: Context) : View(context) {

    private var consumedMl = 0L
    private var goalMl = 2000

    private var fillLevel = 0f
    private var wavePhase = 0f
    private var splashProgress = -1f

    private var fillAnimator: ValueAnimator? = null
    private var waveAnimator: ValueAnimator? = null
    private var splashAnimator: ValueAnimator? = null

    private val darkGreen = Color.rgb(21, 62, 53)

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    private val glassPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = Color.rgb(45, 115, 140)
        style = Paint.Style.STROKE
        strokeWidth = dp(4f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val waterPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = Color.rgb(55, 175, 230)
    }

    private val emptyPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = Color.rgb(230, 248, 253)
    }

    private val surfacePaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = Color.WHITE
        alpha = 120
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }

    private val textPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = darkGreen
        textSize = dp(29f)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val splashPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = Color.rgb(54, 178, 232)
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int
    ) {
        super.onSizeChanged(w, h, oldw, oldh)

        waterPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h.toFloat(),
            intArrayOf(
                Color.rgb(149, 225, 249),
                Color.rgb(66, 185, 237),
                Color.rgb(25, 135, 215)
            ),
            null,
            Shader.TileMode.CLAMP
        )
    }

    fun setProgress(
        glasses: Int,
        newGoalMl: Int,
        animate: Boolean = true
    ) {
        consumedMl = glasses.toLong() * 250L
        goalMl = newGoalMl.coerceAtLeast(1)

        val target = (
                consumedMl.toFloat() / goalMl
                ).coerceIn(0f, 1f)

        contentDescription =
            "$consumedMl of $goalMl millilitres consumed"

        fillAnimator?.cancel()

        if (!animate) {
            fillLevel = target
            invalidate()
            return
        }

        fillAnimator = ValueAnimator.ofFloat(
            fillLevel,
            target
        ).apply {
            duration = 850L
            interpolator = DecelerateInterpolator()

            addUpdateListener {
                fillLevel = it.animatedValue as Float
                invalidate()
            }

            start()
        }
    }

    fun startAnimations() {
        if (waveAnimator?.isRunning == true) return

        waveAnimator = ValueAnimator.ofFloat(
            0f,
            (2.0 * PI).toFloat()
        ).apply {
            duration = 2600L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()

            addUpdateListener {
                wavePhase = it.animatedValue as Float
                invalidate()
            }

            start()
        }
    }

    fun animateOverflow() {
        splashAnimator?.cancel()

        splashProgress = 0f

        splashAnimator = ValueAnimator.ofFloat(
            0f,
            1f
        ).apply {
            duration = 1200L
            interpolator = LinearInterpolator()

            addUpdateListener {
                splashProgress = it.animatedValue as Float
                invalidate()
            }

            start()
        }
    }

    fun stopAnimations() {
        fillAnimator?.cancel()
        waveAnimator?.cancel()
        splashAnimator?.cancel()

        fillAnimator = null
        waveAnimator = null
        splashAnimator = null

        splashProgress = -1f
    }

    private fun surfaceY(
        x: Float,
        base: Float,
        width: Float
    ): Float {
        val phase = x / width

        val first = sin(
            phase * 2f * PI.toFloat() + wavePhase
        )

        val second = sin(
            phase * 4f * PI.toFloat() -
                    wavePhase * 0.6f
        )

        val amplitude = dp(3.5f) * fillLevel

        return base +
                first * amplitude +
                second * amplitude * 0.3f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (w <= 0f || h <= 0f) return

        val top = h * 0.18f
        val bottom = h * 0.86f

        val left = w * 0.23f
        val right = w * 0.77f

        val glassPath = Path().apply {
            moveTo(left, top)
            lineTo(right, top)

            lineTo(
                w * 0.675f,
                bottom - dp(12f)
            )

            quadTo(
                w * 0.67f,
                bottom,
                w * 0.62f,
                bottom
            )

            lineTo(w * 0.38f, bottom)

            quadTo(
                w * 0.33f,
                bottom,
                w * 0.325f,
                bottom - dp(12f)
            )

            close()
        }

        canvas.save()
        canvas.clipPath(glassPath)

        canvas.drawRect(
            0f,
            top,
            w,
            bottom,
            emptyPaint
        )

        if (fillLevel > 0f) {
            val baseY =
                bottom - (bottom - top) * fillLevel

            val waterPath = Path()
            val surfacePath = Path()

            val steps = 48

            for (i in 0..steps) {
                val x = w * i / steps
                val y = surfaceY(x, baseY, w)

                if (i == 0) {
                    waterPath.moveTo(x, y)
                    surfacePath.moveTo(x, y)
                } else {
                    waterPath.lineTo(x, y)
                    surfacePath.lineTo(x, y)
                }
            }

            waterPath.lineTo(
                w,
                bottom + dp(12f)
            )

            waterPath.lineTo(
                0f,
                bottom + dp(12f)
            )

            waterPath.close()

            canvas.drawPath(
                waterPath,
                waterPaint
            )

            canvas.drawPath(
                surfacePath,
                surfacePaint
            )
        }

        canvas.restore()

        canvas.drawPath(
            glassPath,
            glassPaint
        )

        canvas.drawLine(
            left - dp(3f),
            top,
            right + dp(3f),
            top,
            glassPaint
        )

        val percent = (
                consumedMl.toDouble() * 100.0 / goalMl
                ).toInt()

        textPaint.color =
            if (fillLevel >= 0.55f) {
                Color.WHITE
            } else {
                darkGreen
            }

        canvas.drawText(
            "$percent%",
            w / 2f,
            h * 0.58f,
            textPaint
        )

        if (splashProgress >= 0f) {
            drawSplash(
                canvas,
                right,
                top,
                h
            )
        }
    }

    private fun drawSplash(
        canvas: Canvas,
        right: Float,
        top: Float,
        height: Float
    ) {
        val progress = splashProgress

        if (progress < 0f || progress >= 1f) {
            return
        }

        val intensity = sin(
            progress * PI.toFloat()
        ).coerceAtLeast(0f)

        val wave = Path().apply {
            moveTo(
                right - dp(18f),
                top + dp(2f)
            )

            cubicTo(
                right - dp(6f),
                top - dp(18f) * intensity,
                right + dp(10f),
                top - dp(14f) * intensity,
                right + dp(16f),
                top + dp(7f)
            )
        }

        val wavePaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = Color.rgb(55, 180, 235)
            style = Paint.Style.STROKE
            strokeWidth = dp(5f)
            strokeCap = Paint.Cap.ROUND
            alpha = (intensity * 240).toInt()
        }

        canvas.drawPath(
            wave,
            wavePaint
        )

        for (i in 0 until 12) {
            val delay = i * 0.045f

            val t = (
                    (progress - delay) / 0.55f
                    )

            if (t <= 0f || t >= 1f) {
                continue
            }

            val arc = sin(
                t * PI.toFloat()
            )

            val x =
                right +
                        dp(8f) +
                        t * dp(28f) +
                        arc * dp(9f)

            val y =
                top -
                        arc * dp(24f) +
                        t * t * height * 0.58f

            val fade = (1f - t)
                .coerceIn(0f, 1f)

            splashPaint.alpha =
                (fade * 240f).toInt()

            val radius = dp(
                2f + (i % 3)
            )

            canvas.drawOval(
                x - radius,
                y - radius * 1.5f,
                x + radius,
                y + radius * 1.5f,
                splashPaint
            )
        }

        splashPaint.alpha = 255
    }

    override fun onDetachedFromWindow() {
        stopAnimations()
        super.onDetachedFromWindow()
    }
}
