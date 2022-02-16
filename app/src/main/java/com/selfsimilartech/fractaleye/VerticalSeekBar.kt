package com.selfsimilartech.fractaleye

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.SeekBar
import kotlin.math.roundToInt


class VerticalSeekBar : androidx.appcompat.widget.AppCompatSeekBar {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    private var seekBarChangeListener: OnSeekBarChangeListener? = null

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        seekBarChangeListener = l
    }

    @Synchronized
    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        onSizeChanged(width, height, 0, 0)
    }

    override fun setProgress(newProgress: Int, animate: Boolean) {
        if (animate) {
            ValueAnimator.ofInt(progress, newProgress).apply {
                duration = 200L
                addUpdateListener {
                    progress = it.animatedValue as Int
                    onSizeChanged(width, height, 0, 0)
                }
            }.start()
        } else {
            progress = newProgress
            onSizeChanged(width, height, 0, 0)
        }
    }

//    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
//        super.onSizeChanged(h, w, oldh, oldw)
//    }

//    @Synchronized
//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
//        setMeasuredDimension(measuredHeight, measuredWidth)
//    }

//    override fun onDraw(c: Canvas) {
//        c.rotate(-90f)
//        c.translate(-height.toFloat(), 0f)
//        super.onDraw(c)
//    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        progress = (max * event.x / width).roundToInt()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (seekBarChangeListener != null) seekBarChangeListener?.onStartTrackingTouch(this)
            }
            MotionEvent.ACTION_MOVE -> {
                if (seekBarChangeListener != null) seekBarChangeListener?.onProgressChanged(this, progress, true)
                onSizeChanged(width, height, 0, 0)
            }
            MotionEvent.ACTION_UP -> {
                if (seekBarChangeListener != null) seekBarChangeListener?.onStopTrackingTouch(this)
            }
            MotionEvent.ACTION_CANCEL -> {}
        }
        return true
    }

}