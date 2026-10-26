package ca.unb.mobiledev.habitflow

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** A native Canvas chart: no external chart library or network dependency. */
class WeeklyChartView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var values: List<Int> = List(7) { 0 }
    var percentage: Int = 0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val cx = width / 2f
        val cy = 60 * d
        val radius = 40 * d
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 10 * d
        paint.color = Color.rgb(220, 232, 225)
        canvas.drawCircle(cx, cy, radius, paint)
        paint.color = Color.rgb(36, 118, 89)
        canvas.drawArc(RectF(cx-radius, cy-radius, cx+radius, cy+radius), -90f, percentage * 3.6f, false, paint)
        paint.style = Paint.Style.FILL; paint.textAlign = Paint.Align.CENTER
        paint.textSize = 22 * d
        canvas.drawText("$percentage%", cx, cy + 8 * d, paint)
        val bottom = height - 32 * d
        val max = maxOf(1, values.maxOrNull() ?: 1)
        val step = width / 7f
        val labels = listOf("M", "T", "W", "T", "F", "S", "S")
        values.forEachIndexed { i, value ->
            val x = step * (i + 0.5f)
            val top = bottom - value.toFloat() / max * 100 * d
            paint.color = Color.rgb(36, 118, 89)
            canvas.drawRoundRect(x-step*0.28f, top, x+step*0.28f, bottom, 4*d, 4*d, paint)
            paint.color = Color.rgb(21, 62, 53); paint.textSize = 14*d
            canvas.drawText(value.toString(), x, top-6*d, paint)
            canvas.drawText(labels[i], x, bottom+22*d, paint)
        }
    }
}
