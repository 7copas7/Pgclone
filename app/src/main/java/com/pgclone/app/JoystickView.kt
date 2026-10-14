package com.pgclone.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paintOuter = Paint().apply {
        color = Color.parseColor("#44FFFFFF")
        style = Paint.Style.FILL
    }
    private val paintInner = Paint().apply {
        color = Color.parseColor("#AAFFFFFF")
        style = Paint.Style.FILL
    }

    private var centerX = 0f
    private var centerY = 0f
    private var outerRadius = 0f
    private var innerRadius = 0f
    private var thumbX = 0f
    private var thumbY = 0f
    private var moveListener: ((angle: Int, strength: Int) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        centerX = w / 2f
        centerY = h / 2f
        outerRadius = minOf(w, h) / 2f * 0.9f
        innerRadius = outerRadius * 0.4f
        thumbX = centerX
        thumbY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(centerX, centerY, outerRadius, paintOuter)
        canvas.drawCircle(thumbX, thumbY, innerRadius, paintInner)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val dist = sqrt(dx * dx + dy * dy)
                val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                    .toInt() + 360) % 360
                val strength = (minOf(dist, outerRadius) / outerRadius * 100).toInt()

                if (dist < outerRadius) {
                    thumbX = event.x
                    thumbY = event.y
                } else {
                    val rad = atan2(dy.toDouble(), dx.toDouble())
                    thumbX = centerX + (outerRadius * cos(rad)).toFloat()
                    thumbY = centerY + (outerRadius * sin(rad)).toFloat()
                }

                moveListener?.invoke(angle, strength)
            }
            MotionEvent.ACTION_UP -> {
                thumbX = centerX
                thumbY = centerY
                moveListener?.invoke(0, 0)
            }
        }
        invalidate()
        return true
    }

    fun setOnMoveListener(listener: (angle: Int, strength: Int) -> Unit) {
        moveListener = listener
    }
}
