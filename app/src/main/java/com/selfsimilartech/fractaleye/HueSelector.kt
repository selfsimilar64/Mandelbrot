package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class HueSelector : View {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)


    lateinit var satValueSelector : SatValueSelectorView

    private val cornerRadius = 12.dp(context).toFloat()
//    private val rectOffset = cornerRadius*(1f - sqrt(2f)/2f)
    private val rectOffset = 0f
    var onUpdateActiveColor: (newColor: Int) -> Unit = {}

    private var hue = 0f
    var activeColorIndex = 0

    private val rectPaint = Paint()
    private val selectorPaint1 = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }
    private val selectorPaint2 = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val rect = RectF(
            0f, 0f,
            resources.getDimension(R.dimen.satValueSelectorLayoutWidth),
            resources.getDimension(R.dimen.satValueSelectorLayoutHeight)
    )
    private var selectorPos = 0f
    private val selectorWidth = 2.dp(context)
    private val selectorWidthDiff = 1.dp(context)


    fun setHue(newHue: Float, update: Boolean = true) {
        hue = newHue
        satValueSelector.setHue(newHue, update)
        updateRectShader()
        updateSelectorPos()
    }

    fun setSatValueSelectorView(v: SatValueSelectorView) {
        satValueSelector = v
        satValueSelector.hueSelector = this
    }

    private fun updateSelectorPos() {
        val h = hue / 360f
        selectorPos = (1f - h)*(rectOffset + selectorWidth) + h*(rect.width() - rectOffset - selectorWidth)
    }

    private fun updateRectShader() {

        rectPaint.shader = LinearGradient(
            rectOffset, rectOffset, rect.width() - rectOffset, rectOffset,
            resources.getIntArray(R.array.hueslider), null,
            Shader.TileMode.CLAMP
        )
        invalidate()

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Log.e("SATVAL SELECTOR", "size changed")
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        updateSelectorPos()
        updateRectShader()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            drawRoundRect(rect, cornerRadius, cornerRadius, rectPaint)
            drawRect(selectorPos - selectorWidth - selectorWidthDiff, 0f, selectorPos + selectorWidth + selectorWidthDiff, rect.height(), selectorPaint1)
            drawRect(selectorPos - selectorWidth, 0f, selectorPos + selectorWidth, rect.height(), selectorPaint2)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        // Log.e("SATVAL SELECTOR", "touching")

        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {

                val leftEdge = rectOffset + selectorWidth
                val rightEdge = rect.width() - rectOffset - selectorWidth
                val clippedX = min(max(event.x, leftEdge), rightEdge)
                val newHue = (clippedX - leftEdge) / (rect.width() - leftEdge) * 360f
                setHue(newHue)
                selectorPos = clippedX
                invalidate()

            }
        }
        return true

    }

}