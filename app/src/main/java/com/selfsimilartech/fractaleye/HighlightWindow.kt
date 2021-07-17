package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.*
import androidx.core.graphics.toRectF


fun RectF.scale(delta: Float) : RectF {
    val out = RectF(this)
    out.apply {
        left += delta
        right -= delta
        top += delta
        bottom -= delta
    }
    return out
}


class HighlightWindow : androidx.appcompat.widget.AppCompatImageView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    var viewWidth : Int = 0
    var viewHeight : Int = 0

    var consumeTouch = false
    var isRequirementSatisfied : () -> Boolean = { false }
    var onRequirementSatisfied : () -> Unit = { }

    private val appColors = intArrayOf(
            R.color.fe1,
            R.color.fe1,
            R.color.fe2,
            R.color.fe3,
            R.color.fe4,
            R.color.fe5,
            R.color.fe6,
            R.color.fe7,
            R.color.fe7,
            R.color.fe5,
            R.color.fe4,
            R.color.fe3,
            R.color.fe2,
            R.color.fe1,
            R.color.fe1
    ).map{ resources.getColor(it, null) }.toIntArray()

    var gradientShader = SweepGradient(
            0f, 0f,
            appColors,
            null
    )

    var theta = 0f

    val highlight = Rect()
    var highlightCornerRadius = resources.getDimension(R.dimen.highlightCornerRadius2)
    //val highlightCornerRadius = 0f
    val strokeWidth = resources.getDimension(R.dimen.highlightStrokeWidth)

    override fun onSizeChanged(xNew: Int, yNew: Int, xOld: Int, yOld: Int) {
        super.onSizeChanged(xNew, yNew, xOld, yOld)

        viewWidth = xNew
        viewHeight = yNew

    }

    override fun draw(canvas: Canvas) {

        super.draw(canvas)

        val path = Path()
        path.fillType = Path.FillType.EVEN_ODD

        // This will create a full screen rectangle.
        path.addRect(RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat()), Path.Direction.CW)

        if (!highlight.isEmpty) {
            path.addRoundRect(
                    highlight.toRectF().scale(strokeWidth / 2f),
                    highlightCornerRadius,
                    highlightCornerRadius,
                    Path.Direction.CCW
            )
        }

        val paint = Paint()
        paint.color = resources.getColor(R.color.tutorialBackground, null)
        canvas.drawPath(path, paint)

        if (!highlight.isEmpty) {

            paint.color = resources.getColor(R.color.white, null)
            paint.shader = gradientShader
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth

            canvas.drawRoundRect(highlight.toRectF().scale(strokeWidth / 2f), highlightCornerRadius, highlightCornerRadius, paint)

            paint.color = Color.BLACK
            paint.shader = null
            paint.strokeWidth = 4f

            canvas.drawRoundRect(highlight.toRectF().scale(strokeWidth), highlightCornerRadius, highlightCornerRadius, paint)

        }

    }

    fun clearHighlight() {
        highlight.set(Rect())
    }

    fun onHighlightRectChanged() {
        gradientShader = SweepGradient(
                highlight.centerX().toFloat(),
                highlight.centerY().toFloat(),
                appColors,
                null
        )
        val mat = Matrix()
        gradientShader.apply {
            getLocalMatrix(mat)
            mat.postScale(1f, highlight.run { height().toFloat()/width() }, highlight.centerX().toFloat(), highlight.centerY().toFloat())
            setLocalMatrix(mat)
        }
    }

    fun updateGradientShader() {
        val mat = Matrix()
        gradientShader.apply {
            getLocalMatrix(mat)
            mat.postTranslate(-highlight.centerX().toFloat(), -highlight.centerY().toFloat())
            mat.postScale(1f, highlight.run { width().toFloat()/height() })
            mat.postRotate(2.5f)
            mat.postScale(1f, highlight.run { height().toFloat()/width() })
            mat.postTranslate(highlight.centerX().toFloat(), highlight.centerY().toFloat())
            setLocalMatrix(mat)
        }
    }

    fun highlightView(v: View) {
        val loc = intArrayOf(0, 0)
        v.getLocationOnScreen(loc)
        val rect = Rect()
        v.getGlobalVisibleRect(rect)
        highlightRect(rect)
    }

    fun highlightRect(rect: Rect) {
        highlight.set(rect)
        onHighlightRectChanged()
    }

    fun startHighlightAnimation() {
        val anim = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                super.applyTransformation(interpolatedTime, t)
                theta = interpolatedTime*360f
                updateGradientShader()
                invalidate()
            }
        }
        animation = anim
        anim.apply {
            interpolator = LinearInterpolator()
            duration = 1250L
            repeatCount = Animation.INFINITE
            start()
        }
    }

    fun stopHighlightAnimation() {
        animation.cancel()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when {
            consumeTouch -> return true
            highlight.contains(e.x.toInt(), e.y.toInt()) -> {
                if (isRequirementSatisfied()) {
                    onRequirementSatisfied()
                    return true
                }
                return false
            }
            else -> return true
        }
    }

}