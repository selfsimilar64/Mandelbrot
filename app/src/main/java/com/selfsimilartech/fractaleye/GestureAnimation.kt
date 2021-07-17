package com.selfsimilartech.fractaleye

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

class GestureAnimation : View {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    companion object {

        const val FADE_PROP = 0.175f
        const val ANIM_DURATION = 1450L

    }

    val fastOutSlowIn = FastOutSlowInInterpolator()

    val innerCenterColor = resources.getColor(R.color.gestureIndicatorInnerCenterColor, null)
    val innerEdgeColor = resources.getColor(R.color.gestureIndicatorInnerEdgeColor, null)

    var viewWidth = 0
    var viewHeight = 0

    val c1 = PointF()
    val c2 = PointF()

    var hideCircle2 = true

    val circleRadius = resources.getDimension(R.dimen.gestureCircleRadius)
    val finalAlpha = 1f

    val c1InnerGradient = Paint()
    val c2InnerGradient = Paint()


    val stroke = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 3f
    }

    val fadeIn: ValueAnimator = ValueAnimator.ofInt(0, 200).apply {
        duration = 200
    }
    val fadeOut: ValueAnimator = ValueAnimator.ofInt(200, 0).apply {
        duration = 200
    }

    private val swipeVerticalAnim = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            super.applyTransformation(interpolatedTime, t)
            when {
                interpolatedTime == 0f -> {
                    c1.apply {
                        x = viewWidth/2f
                        y = 0.3f*viewHeight
                    }
                }
                interpolatedTime < FADE_PROP -> {
                    this@GestureAnimation.alpha = finalAlpha*interpolatedTime/FADE_PROP
                }
                interpolatedTime < (1.0 - FADE_PROP) -> {
                    val s = fastOutSlowIn.getInterpolation((interpolatedTime - FADE_PROP)/(1f - FADE_PROP))
                    c1.y = ((1f - s)*0.3f + s*0.7f)*viewHeight
                }
                else -> {
                    this@GestureAnimation.alpha = (1f - (interpolatedTime - (1f - FADE_PROP))/FADE_PROP)*finalAlpha
                }
            }
            updateCirclePaints()
            invalidate()
        }
    }.apply {
        duration = ANIM_DURATION
        repeatCount = Animation.INFINITE
    }

    private val pinchAnim = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            super.applyTransformation(interpolatedTime, t)
            when {
                interpolatedTime == 0f -> {
                    c1.apply {
                        x = viewWidth/2f + circleRadius
                        y = viewHeight/2f - circleRadius
                    }
                    c2.apply {
                        x = viewWidth/2f - circleRadius
                        y = viewHeight/2f + circleRadius
                    }
                }
                interpolatedTime < FADE_PROP -> {
                    this@GestureAnimation.alpha = finalAlpha*interpolatedTime/FADE_PROP
                }
                interpolatedTime < (1.0 - FADE_PROP) -> {
                    val s = fastOutSlowIn.getInterpolation((interpolatedTime - FADE_PROP)/(1f - FADE_PROP))
                    c1.apply {
                        x = viewWidth/2f + circleRadius + s*0.35f*viewWidth/2f
                        y = viewHeight/2f - circleRadius - s*0.35f*viewHeight/2f
                    }
                    c2.apply {
                        x = viewWidth/2f - circleRadius - s*0.35f*viewWidth/2f
                        y = viewHeight/2f + circleRadius + s*0.35f*viewHeight/2f
                    }
                }
                else -> {
                    this@GestureAnimation.alpha = (1f - (interpolatedTime - (1f - FADE_PROP))/FADE_PROP)*finalAlpha
                }
            }
            updateCirclePaints()
            invalidate()
        }
    }.apply {
        duration = ANIM_DURATION
        repeatCount = Animation.INFINITE
    }

    private val swipeHorizontalAnim = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            super.applyTransformation(interpolatedTime, t)
            when {
                interpolatedTime == 0f -> {
                    c1.apply {
                        x = 0.3f*viewWidth
                        y = viewHeight/2f
                    }
                }
                interpolatedTime < FADE_PROP -> {
                    this@GestureAnimation.alpha = finalAlpha*interpolatedTime/FADE_PROP
                }
                interpolatedTime < (1.0 - FADE_PROP) -> {
                    val s = fastOutSlowIn.getInterpolation((interpolatedTime - FADE_PROP)/(1f - FADE_PROP))
                    c1.x = ((1f - s)*0.3f + s*0.7f)*viewWidth
                }
                else -> {
                    this@GestureAnimation.alpha = (1f - (interpolatedTime - (1f - FADE_PROP))/FADE_PROP)*finalAlpha
                }
            }
            updateCirclePaints()
            invalidate()
        }
    }.apply {
        duration = ANIM_DURATION
        repeatCount = Animation.INFINITE
    }

    private val swipeDiagonalAnim = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            super.applyTransformation(interpolatedTime, t)
            when {
                interpolatedTime == 0f -> {
                    c1.apply {
                        x = 0.7f*viewWidth
                        y = 0.7f*viewHeight
                    }
                }
                interpolatedTime < FADE_PROP -> {
                    this@GestureAnimation.alpha = finalAlpha*interpolatedTime/FADE_PROP
                }
                interpolatedTime < (1.0 - FADE_PROP) -> {
                    val s = fastOutSlowIn.getInterpolation((interpolatedTime - FADE_PROP)/(1f - FADE_PROP))
                    c1.x = ((1f - s)*0.7f + s*0.3f)*viewWidth
                    c1.y = ((1f - s)*0.7f + s*0.3f)*viewHeight
                }
                else -> {
                    this@GestureAnimation.alpha = (1f - (interpolatedTime - (1f - FADE_PROP))/FADE_PROP)*finalAlpha
                }
            }
            updateCirclePaints()
            invalidate()
        }
    }.apply {
        duration = ANIM_DURATION
        repeatCount = Animation.INFINITE
    }

    fun updateCirclePaints() {

        c1InnerGradient.shader = RadialGradient(c1.x, c1.y, circleRadius, innerCenterColor, innerEdgeColor, Shader.TileMode.MIRROR)
        c2InnerGradient.shader = RadialGradient(c2.x, c2.y, circleRadius, innerCenterColor, innerEdgeColor, Shader.TileMode.MIRROR)

    }

    fun stopAnim() {
        alpha = 0f
        animation?.cancel()
        animation = null
    }

    fun startSwipeHorizontalAnim() {
        hideCircle2 = true
        animation?.cancel()
        animation = swipeHorizontalAnim.apply { start() }
    }

    fun startPinchAnim() {
        hideCircle2 = false
        animation?.cancel()
        animation = pinchAnim.apply { start() }
    }

    fun startSwipeVerticalAnim() {
        hideCircle2 = true
        animation?.cancel()
        animation = swipeVerticalAnim.apply { start() }
    }

    fun startSwipeDiagonalAnim() {
        hideCircle2 = true
        animation?.cancel()
        animation = swipeDiagonalAnim.apply { start() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(c1.x, c1.y, circleRadius, c1InnerGradient)
        canvas.drawCircle(c1.x, c1.y, circleRadius, stroke)
        if (!hideCircle2) {
            canvas.drawCircle(c2.x, c2.y, circleRadius, c2InnerGradient)
            canvas.drawCircle(c2.x, c2.y, circleRadius, stroke)
        }
    }

    override fun onSizeChanged(xNew: Int, yNew: Int, xOld: Int, yOld: Int) {
        super.onSizeChanged(xNew, yNew, xOld, yOld)

        viewWidth = xNew
        viewHeight = yNew

        // circle1.x = viewWidth/0.75f

    }


}