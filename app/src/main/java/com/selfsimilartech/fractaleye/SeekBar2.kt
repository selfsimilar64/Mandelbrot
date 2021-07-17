package com.selfsimilartech.fractaleye

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.SeekBar
import kotlin.math.abs

@SuppressLint("AppCompatCustomView")
open class SeekBar2 : SeekBar {

    private var viewWidth : Int = 0
    private var viewHeight : Int = 0

    private var progressRect = Rect()
    private var gradientRect = Rect()
    private var warningRect = Rect()
    private var paint = Paint()
    private var gradientPaint = Paint()
    private var warningPaint = Paint().apply {
        color = Color.HSVToColor(floatArrayOf(45f, 0.8f, 0.95f))
    }
    private var seekbarHeight = 0
    private var thumbSize = 0f

    private val goldColors = intArrayOf(
            R.color.gold1,
            R.color.gold2,
            R.color.gold3,
            R.color.gold4,
            R.color.gold5
    ).map{ resources.getColor(it, null) }.toIntArray()

    var showGradient : Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    var gradientStartProgress : Int = 0

    var showWarning : Boolean = false

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        progressRect = Rect()
        paint = Paint()
        seekbarHeight = 6
        thumbSize = resources.getDimension(R.dimen.satValueSelectorRadius)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)


    override fun onSizeChanged(xNew: Int, yNew: Int, xOld: Int, yOld: Int) {
        super.onSizeChanged(xNew, yNew, xOld, yOld)

        viewWidth = xNew
        viewHeight = yNew

        progressRect = Rect(
                0, 0,
                viewWidth,
                viewHeight
        )

        // assuming width >= height
        gradientPaint.shader = LinearGradient(
                gradientStartProgress * viewWidth.toFloat() / max - thumbOffset,
                0f,
                viewWidth.toFloat() - thumbOffset,
                viewHeight.toFloat(),
                goldColors, null, Shader.TileMode.CLAMP
        )

    }


    @Synchronized
    override fun onDraw(canvas: Canvas) {

        progressRect[0 + thumbOffset, height / 2 - seekbarHeight / 2, width - thumbOffset] = height / 2 + seekbarHeight / 2
        paint.color = Color.GRAY
        canvas.drawRect(progressRect, paint)

        if (showGradient) {
            gradientRect.set(progressRect)
            gradientRect.left = gradientStartProgress * gradientRect.width() / max + 2*thumbSize.toInt()
            canvas.drawRect(gradientRect, gradientPaint)
        }

        if (showWarning) {
            warningRect.set(progressRect)
            warningRect.left = (0.75 * progressRect.width() + 2*thumbSize.toInt()).toInt()
            canvas.drawRect(warningRect, warningPaint)
        }

        progressRect.set(
                thumbOffset,
                height/2 - seekbarHeight/2,
                (progress.toFloat()/max*(width - 4f*thumbSize) + 2f*thumbSize).toInt(),
                height/2 + seekbarHeight/2
        )

        paint.color = resources.getColor(R.color.highlight, null)
        canvas.drawRect(progressRect, paint)
        canvas.drawCircle(
                progress.toFloat()/max*(width - 4f*thumbSize) + 2f*thumbSize,
                height/2f,
                thumbSize,
                paint
        )

    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN) {
            stopNestedScroll()
            return true
        }
        return super.onTouchEvent(e)
    }

}
