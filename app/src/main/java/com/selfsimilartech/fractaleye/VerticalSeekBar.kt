package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.SeekBar


class VerticalSeekBar : SeekBar {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    @Synchronized
    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        onSizeChanged(width, height, 0, 0)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(h, w, oldh, oldw)
    }

    @Synchronized
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredHeight, measuredWidth)
    }

    override fun onDraw(c: Canvas) {
        c.rotate(-90f)
        c.translate(-height.toFloat(), 0f)
        super.onDraw(c)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                parent.requestDisallowInterceptTouchEvent(true)
                progress = max - (max * event.y / height).toInt()
                onSizeChanged(width, height, 0, 0)
            }
            MotionEvent.ACTION_CANCEL-> {
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }
}