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

class SaturationValueSelector(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val cornerRadius : Float
    private val rectOffset : Float

    private var listener: OnColorChangeListener? = null

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
    private val rect = RectF()
    private val selectorPos = Point(0, 0)
    private val selectorRadius = resources.getDimension(R.dimen.satValueSelectorRadius)
    private val selectorRadiusDiff = resources.getDimension(R.dimen.satValueSelectorRadiusDiff)


    init {

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SaturationValueSelector)

        cornerRadius = typedArray.getDimension(R.styleable.SaturationValueSelector_android_radius, 0f)
        rectOffset = 1.35f*cornerRadius*(1f - sqrt(2f)/2f)

        typedArray.recycle()

        setLayerType(LAYER_TYPE_SOFTWARE, null)


    }


    var hue    = 0f
    var sat    = 0f
    var value  = 1f

    private val color = { Color.HSVToColor(floatArrayOf(hue, sat, value)) }


    fun setOnColorChangeListener(l: OnColorChangeListener) {
        listener = l
    }

    fun setHue(newHue: Float, updateLinkedColor: Boolean = true) {
        hue = newHue
        selectorPaint4.color = color()
        updateRectShader()
        if (updateLinkedColor) listener?.onColorChanged(color())
    }

    fun setValues(newSat: Float, newVal: Float) {
        sat = newSat
        value = newVal
        selectorPaint4.color = color()
        updateRectShader()
        updateSelectorPos()
        listener?.onColorChanged(color())
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
                listener?.onColorChanged(newColor)
                invalidate()

            }
        }
        return true

    }

}