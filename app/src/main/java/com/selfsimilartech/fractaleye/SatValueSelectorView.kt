package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class SatValueSelectorView : View {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }


    lateinit var hueSelector : HueSelector

    private val cornerRadius = 18.dp(context).toFloat()
    private val rectOffset = 1.35f*cornerRadius*(1f - sqrt(2f)/2f)
    var onUpdateLinkedColor: (newColor: Int) -> Unit = {}

    var hue = 0f
    var linkedColorIndex = 0

    private val rectPaint = Paint()
    private val selectorPaint1 = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }
    private val selectorPaint2 = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val selectorPaint3 = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }
    private val selectorPaint4 = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val rect = RectF(
            0f, 0f,
            resources.getDimension(R.dimen.satValueSelectorLayoutWidth),
            resources.getDimension(R.dimen.satValueSelectorLayoutHeight)
    )
    private val selectorPos = Point(0, 0)
    private val selectorRadius = resources.getDimension(R.dimen.satValueSelectorRadius)
    private val selectorRadiusDiff = resources.getDimension(R.dimen.satValueSelectorRadiusDiff)



    var sat = 0f
    var value  = 1f
    private val color = { Color.HSVToColor(floatArrayOf(hue, sat, value)) }

    fun setHue(newHue: Float, updateLinkedColor: Boolean = true) {
        hue = newHue
        selectorPaint4.color = color()
        updateRectShader()
        if (updateLinkedColor) onUpdateLinkedColor(color())
    }

    fun setSat(newSat: Float, updateLinkedColor: Boolean = true) {
        sat = newSat
        selectorPaint4.color = color()
        updateRectShader()
        updateSelectorPos()
        if (updateLinkedColor) onUpdateLinkedColor(color())
    }

    fun setVal(newVal: Float, updateLinkedColor: Boolean = true) {
        value = newVal
        selectorPaint4.color = color()
        updateRectShader()
        updateSelectorPos()
        if (updateLinkedColor) onUpdateLinkedColor(color())
    }

    fun setColor(color: Int, updateLinkedColor: Boolean = true) {

        Log.d("SATVAL", "loading color: (${color.hue()}, ${color.sat()}, ${color.value()})")

        hueSelector.setHue(color.hue(), updateLinkedColor)
        sat = color.sat()
        value = color.value()

        updateSelectorPos()
        Log.d("SATVAL", "selectorPos: (${selectorPos.x}, ${selectorPos.y})")

        selectorPaint4.color = color
        if (updateLinkedColor) onUpdateLinkedColor(color)

    }



    private fun updateSelectorPos() {
        selectorPos.set(
            ((1f - sat)*rectOffset + sat*(rect.width() - rectOffset)).toInt(),
            (value*rectOffset + (1f - value)*(rect.height() - rectOffset)).toInt()
        )
    }

    private fun updateRectShader() {

        val valueGradient = LinearGradient(
                rectOffset, rectOffset, rectOffset, rect.height() - rectOffset,
                Color.WHITE, Color.BLACK,
                Shader.TileMode.CLAMP
        )
        val satGradient = LinearGradient(
                rectOffset, rectOffset, rect.width() - rectOffset, rectOffset,
                Color.WHITE, Color.HSVToColor(floatArrayOf(hue, 1f, 1f)),
                Shader.TileMode.CLAMP
        )
        val mergedGradient = ComposeShader(valueGradient, satGradient, PorterDuff.Mode.MULTIPLY)
        rectPaint.shader = mergedGradient
        invalidate()

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Log.e("SATVAL SELECTOR", "size changed")
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        updateRectShader()
        updateSelectorPos()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            drawRoundRect(rect, cornerRadius, cornerRadius, rectPaint)
            drawCircle(
                    selectorPos.x.toFloat(),
                    selectorPos.y.toFloat(),
                    selectorRadius,
                    selectorPaint1
            )
            drawCircle(
                    selectorPos.x.toFloat(),
                    selectorPos.y.toFloat(),
                    selectorRadius - selectorRadiusDiff,
                    selectorPaint2
            )
            drawCircle(
                    selectorPos.x.toFloat(),
                    selectorPos.y.toFloat(),
                    selectorRadius - 3*selectorRadiusDiff,
                    selectorPaint3
            )
            drawCircle(
                    selectorPos.x.toFloat(),
                    selectorPos.y.toFloat(),
                    selectorRadius - 4*selectorRadiusDiff,
                    selectorPaint4
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        // Log.e("SATVAL SELECTOR", "touching")

        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {

                val clippedX = min(max(event.x, rectOffset), rect.width() - rectOffset)
                val clippedY = min(max(event.y, rectOffset), rect.height() - rectOffset)
                selectorPos.set(clippedX.roundToInt(), clippedY.roundToInt())
                Log.d("SATVAL", "selectorPos: (${selectorPos.x}, ${selectorPos.y})")

                sat = (clippedX - rectOffset) / (rect.width() - 2f*rectOffset)
                value = 1f - ((clippedY - rectOffset) / (rect.height() - 2f*rectOffset))
                Log.d("SATVAL", "color: ($hue, $sat, $value)")

                val newColor = color()
                selectorPaint4.color = newColor
                onUpdateLinkedColor(newColor)
                invalidate()

            }
        }
        return true

    }

}