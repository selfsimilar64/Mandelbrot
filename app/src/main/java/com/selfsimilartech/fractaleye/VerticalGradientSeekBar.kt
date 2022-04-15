package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import kotlin.math.roundToInt

class VerticalGradientSeekBar(context: Context, attrs: AttributeSet?) : GradientSeekBar(context, attrs) {


    @Synchronized
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredHeight, measuredWidth)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(h, w, oldh, oldw)
    }

    override fun onDraw(c: Canvas) {
        c.rotate(-90f)
        c.translate(-height.toFloat(), 0f)
        super.onDraw(c)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        progress = (max * (1f - event.y / height)).roundToInt()
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                onSizeChanged(width, height, 0, 0)
            }
        }
        return true
    }

}