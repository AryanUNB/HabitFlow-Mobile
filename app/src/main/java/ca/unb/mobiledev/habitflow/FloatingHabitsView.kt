package ca.unb.mobiledev.habitflow

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

/** Decorative habits stay around the edges so the central logo remains readable. */
class FloatingHabitsView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val emojis = listOf("💧", "📚", "🏊", "🚶", "📝", "🧘", "🍎", "🚴")
    private val horizontal = floatArrayOf(.18f, .73f, .88f, .16f, .77f, .34f, .12f, .88f)
    private val vertical = floatArrayOf(.18f, .13f, .35f, .72f, .81f, .88f, .40f, .65f)
    private val startedAt = SystemClock.elapsedRealtime()

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val animate = ValueAnimator.areAnimatorsEnabled()
        val seconds = if (animate) (SystemClock.elapsedRealtime() - startedAt) / 1000f else 0f
        paint.textSize = 30f * density
        paint.alpha = 200
        emojis.forEachIndexed { index, emoji ->
            val phase = seconds * 1.7f + index * .9f
            val x = width * horizontal[index] + sin(phase * .7f) * 8f * density
            val y = height * vertical[index] + sin(phase) * 14f * density
            canvas.save()
            canvas.rotate(sin(phase * .8f) * 9f, x, y)
            canvas.drawText(emoji, x, y, paint)
            canvas.restore()
        }
        if (animate && isAttachedToWindow && windowVisibility == VISIBLE) postInvalidateOnAnimation()
    }
}
