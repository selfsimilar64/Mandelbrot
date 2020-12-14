package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.*
import android.util.AttributeSet

open class GradientImageView : androidx.appcompat.widget.AppCompatImageView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private var viewWidth : Int = 0
    private var viewHeight : Int = 0

    var showGradient = false
        set(value) {
            field = value
            invalidate()
        }

    private val rectPaint = Paint()
    private val goldColors = intArrayOf(
            R.color.gold1,
            R.color.gold1,
            R.color.gold2,
            R.color.gold3,
            R.color.gold4,
            R.color.gold5,
            R.color.gold5
    ).map{ resources.getColor(it, null) }.toIntArray()

    private lateinit var rect : Rect
    private lateinit var gradient : LinearGradient
    private lateinit var bmp : Bitmap
    private lateinit var buffer : Canvas
    private lateinit var bmpShader : BitmapShader


    override fun onSizeChanged(xNew: Int, yNew: Int, xOld: Int, yOld: Int) {
        super.onSizeChanged(xNew, yNew, xOld, yOld)

        viewWidth = xNew
        viewHeight = yNew

        rect = Rect(
                0, 0,
                viewWidth,
                viewHeight
        )
        gradient = LinearGradient(
                0.2f*viewWidth.toFloat(),
                0f,
                0.7f*viewWidth.toFloat(),
                viewHeight.toFloat(),
                goldColors, null, Shader.TileMode.CLAMP
        )
        bmp = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        buffer = Canvas(bmp)
        bmpShader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        rectPaint.shader = ComposeShader(gradient, bmpShader, PorterDuff.Mode.MULTIPLY)

    }

    override fun onDraw(canvas: Canvas?) {
        if (showGradient) {
            buffer.drawColor(Color.BLACK)
            super.onDraw(buffer)
            canvas?.apply {
                // drawColor(Color.BLACK)
                drawRect(rect, rectPaint)
            }
        }
        else super.onDraw(canvas)
    }

}