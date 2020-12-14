package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class SatValueSelectorView : View {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }


    var onUpdateActiveColor: (newColor: Int) -> Unit = {}

    var hue = 0f
        set(value) {
            field = value
            updateRectShader()
            updateActiveColor()
        }
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
    private val selectorPaint3 = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }
    private val selectorPaint4 = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val rect = Rect(
            0, 0,
            resources.getDimension(R.dimen.satValueSelectorLayoutWidth).toInt(),
            resources.getDimension(R.dimen.satValueSelectorLayoutHeight).toInt()
    )
    private val selectorPos = Point(0, 0)
    private val selectorRadius = resources.getDimension(R.dimen.satValueSelectorRadius)
    private val selectorRadiusDiff = resources.getDimension(R.dimen.satValueSelectorRadiusDiff)



    fun sat() : Float = selectorPos.x / rect.width().toFloat()
    fun value() : Float = 1f - (selectorPos.y / rect.height().toFloat())
    fun loadFrom(color: Int, invalidate: Boolean = false, update: Boolean = false) {

        selectorPos.set(
                (color.sat()*rect.width()).toInt(),
                ((1f - color.value())*rect.height()).toInt()
        )
        selectorPaint4.color = color
        if (invalidate) invalidate()
        if (update) updateActiveColor()

    }

    private fun updateActiveColor() {

        val newColor = Color.HSVToColor(floatArrayOf(hue, sat(), value()))

        selectorPaint4.color = newColor
        onUpdateActiveColor(newColor)

    }

    private fun updateRectShader() {

        val valueGradient = LinearGradient(
                0f, 0f, 0f, rect.height().toFloat(),
                Color.WHITE, Color.BLACK,
                Shader.TileMode.CLAMP
        )
        val satGradient = LinearGradient(
                0f, 0f, rect.width().toFloat(), 0f,
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
        rect.set(0, 0, w, h)
        updateRectShader()
    }

    override fun onDraw(canvas: Canvas) {
        //Log.e("SATVAL SELECTOR", "drawing")
        super.onDraw(canvas)
        canvas.apply {
            drawRect(rect, rectPaint)
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

        //Log.e("SATVAL SELECTOR", "touching")

        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {

                parent.requestDisallowInterceptTouchEvent(true)
                val clippedX = min(max(event.x, 0f), rect.width().toFloat())
                val clippedY = min(max(event.y, 0f), rect.height().toFloat())
                selectorPos.set(clippedX.toInt(), clippedY.toInt())
                updateActiveColor()

            }
            MotionEvent.ACTION_CANCEL -> {

                parent.requestDisallowInterceptTouchEvent(false)

            }
        }
        return true

    }

}