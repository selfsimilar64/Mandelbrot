package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.*
import android.util.AttributeSet

open class GradientTextView : androidx.appcompat.widget.AppCompatTextView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    private var viewWidth : Int = 0
    private var viewHeight : Int = 0

    var angle = 0f

    var showGradient = false
        set(value) {
            field = value
            invalidate()
        }

    private val rectPaint = Paint()
    private val goldColors = intArrayOf(
            R.color.gold1,
            R.color.gold2,
            R.color.gold3,
            R.color.gold4,
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

        // assuming width >= height
        gradient = LinearGradient(
                0f,
                0f,
                viewWidth.toFloat(),
                viewHeight.toFloat(),
                goldColors, null, Shader.TileMode.CLAMP
        )
        bmp = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        buffer = Canvas(bmp)
        bmpShader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        rectPaint.shader = ComposeShader(gradient, bmpShader, PorterDuff.Mode.MULTIPLY)
//        rectPaint.shader = gradient

    }

    override fun onDraw(canvas: Canvas?) {
        if (showGradient) {
            buffer.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR)
            super.onDraw(buffer)
            canvas?.apply {
                drawRect(rect, rectPaint)
            }
        }
        else super.onDraw(canvas)
    }

}