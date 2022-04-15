package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.SweepGradient
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton

class ColorWheelView : AppCompatImageButton {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    private var width = 0f
    private var height = 0f

    val paint = Paint()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Log.e("SATVAL SELECTOR", "size changed")
        super.onSizeChanged(w, h, oldw, oldh)
        width = w.toFloat()
        height = h.toFloat()
        paint.shader = SweepGradient(
            width/2f, height/2f, resources.getIntArray(R.array.color_wheel), null
        )
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.run {
            drawCircle(width/2f, height/2f, 12.dp(context).toFloat(), paint)
        }
    }

}