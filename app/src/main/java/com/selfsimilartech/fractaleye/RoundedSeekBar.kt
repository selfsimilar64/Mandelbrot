package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar

class RoundedSeekBar : AppCompatSeekBar {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    val trackRect = RectF()
    val trackPaint = Paint().also {
        it.color = Color.GRAY
    }

    val progressRect = RectF()
    val progressPaint = Paint().also {
        it.color = Color.WHITE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        trackRect.apply {
            right = w.toFloat()
            bottom = h.toFloat()
        }
        progressRect.apply {
            bottom = h.toFloat()
        }
    }

    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        if (trackRect != null) {
            progressRect.right = progress.toFloat() / max.toFloat() * trackRect.width()
        }
        invalidate()
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        // super.onDraw(canvas)
        canvas.apply {
            drawRoundRect(trackRect,    trackRect.height()/2f, trackRect.height()/2f, trackPaint)
            drawRoundRect(progressRect, trackRect.height()/2f, trackRect.height()/2f, progressPaint)
        }
    }

}