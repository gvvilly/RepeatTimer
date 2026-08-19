package com.example.repeattimer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class TimerRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
        color = 0xFF252B29.toInt()
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
        color = 0xFF8BC4A8.toInt()
    }
    private var progress = 1f

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val pad = 16f
        val left = (width - size) / 2f + pad
        val top = (height - size) / 2f + pad
        val right = (width + size) / 2f - pad
        val bottom = (height + size) / 2f - pad
        val oval = RectF(left, top, right, bottom)
        canvas.drawArc(oval, 0f, 360f, false, trackPaint)
        canvas.drawArc(oval, -90f, 360f * progress, false, progressPaint)
    }
}
