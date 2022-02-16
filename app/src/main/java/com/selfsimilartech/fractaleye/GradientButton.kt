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
    private val rect = Rect()
    private val goldColors = intArrayOf(
            R.color.gold1,
            R.color.gold1,
            R.color.gold2,
            R.color.gold3,
            R.color.gold4,
            R.color.gold5
    ).map { resources.getColor(it, null) }.toIntArray()

    private lateinit var gradient : LinearGradient
    private lateinit var bmp : Bitmap
    private lateinit var buffer : Canvas
    private lateinit var bmpShader : BitmapShader

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(0, 0, w, h)
        gradient = LinearGradient(
            0f, 0f, rect.width().toFloat(), rect.height().toFloat()/5f,
            goldColors, null, Shader.TileMode.CLAMP
        )
        bmp = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        buffer = Canvas(bmp)
        bmpShader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        rectPaint.shader = ComposeShader(gradient, bmpShader, PorterDuff.Mode.MULTIPLY)
        invalidate()
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