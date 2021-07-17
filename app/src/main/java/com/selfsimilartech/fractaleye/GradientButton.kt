package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatButton

open class GradientButton : AppCompatButton {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    var showGradient = false
        set(value) {
            field = value
            invalidate()
        }

    private val rectPaint = Paint()
    private val rect = Rect(
            0, 0,
            resources.getDimension(R.dimen.menuButtonWidth).toInt(),
            resources.getDimension(R.dimen.menuButtonHeight).toInt()
    )
    private val goldColors = intArrayOf(
            R.color.gold1,
            R.color.gold1,
            R.color.gold2,
            R.color.gold3,
            R.color.gold4,
            R.color.gold5,
            R.color.gold5
    ).map{ resources.getColor(it, null) }.toIntArray()
    private val gradient = LinearGradient(
            0f, 0f, rect.width().toFloat(), rect.height().toFloat()/5f,
            goldColors, null, Shader.TileMode.CLAMP
    )
    private val bmp = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
    private val buffer = Canvas(bmp)
    private val bmpShader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

    init {

        rectPaint.shader = ComposeShader(gradient, bmpShader, PorterDuff.Mode.MULTIPLY)

    }

    override fun onDraw(canvas: Canvas?) {
        if (showGradient) {
            buffer.drawColor(Color.BLACK, PorterDuff.Mode.MULTIPLY)
            super.onDraw(buffer)
            canvas?.apply {
                // drawColor(Color.TRANSPARENT)
                drawRect(rect, rectPaint)
            }
        }
        else super.onDraw(canvas)
    }

}