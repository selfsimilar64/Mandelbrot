package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import androidx.core.graphics.toRectF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


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
fun Rect.scale(delta: Int) : Rect {
    val out = Rect(this)
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

    var fsv : FractalSurfaceView? = null

    var viewWidth : Int = 0
    var viewHeight : Int = 0

    var consumeTouch = false

    var highlightColors = intArrayOf(
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
        set(value) {
            field = value
            onHighlightRectChanged()
        }

    var gradientShader = SweepGradient(
            0f, 0f,
            highlightColors,
            null
    )

    var theta = 0f

    val highlight = Rect()
    var highlightCornerRadius = resources.getDimension(R.dimen.paramMenuCornerRadius)
    //val highlightCornerRadius = 0f
    val strokeWidth = resources.getDimension(R.dimen.highlightStrokeWidth)

    override fun onSizeChanged(xNew: Int, yNew: Int, xOld: Int, yOld: Int) {
        super.onSizeChanged(xNew, yNew, xOld, yOld)

        Log.d("HIGHLIGHT", "size changed to ($xNew, $yNew)")
        viewWidth = xNew
        viewHeight = yNew

    }

    override fun draw(canvas: Canvas) {

        super.draw(canvas)
//
//        val path = Path()
//        path.fillType = Path.FillType.EVEN_ODD
//
//        path.addRect(RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat()), Path.Direction.CW)
//
//        if (!highlight.isEmpty) {
//            path.addRoundRect(
//                    highlight.toRectF().scale(strokeWidth / 2f),
//                    highlightCornerRadius,
//                    highlightCornerRadius,
//                    Path.Direction.CCW
//            )
//        }
//
//        val paint = Paint()
//        paint.color = resources.getColor(R.color.tutorialBackground, null)
//        canvas.drawPath(path, paint)

        if (!highlight.isEmpty) {

            canvas.run {
                Log.d("HIGHLIGHT", "translate: (${quadCoords[0]}, ${quadCoords[1]}), scale: $quadScale")
                rotate(quadRotation)
                translate(quadCoords[0], quadCoords[1])
                scale(quadScale, quadScale)
            }

            val paint = Paint()

//            paint.color = Color.RED
//            canvas.drawCircle(highlight.centerX().toFloat(), highlight.centerY().toFloat(), 5f, paint)

            paint.color = Color.BLACK
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6.dp(context).toFloat()
            // canvas.drawRect(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat(), paint)

            canvas.drawRoundRect(highlight.toRectF(), highlightCornerRadius, highlightCornerRadius, paint)

            paint.color = resources.getColor(R.color.white, null)
            paint.shader = gradientShader
            paint.strokeWidth = strokeWidth

            canvas.drawRoundRect(highlight.toRectF(), highlightCornerRadius, highlightCornerRadius, paint)

        }

    }



    fun clearHighlight() {
        highlight.set(Rect())
    }

    fun onHighlightRectChanged() {
        gradientShader = SweepGradient(
                highlight.centerX().toFloat(),
                highlight.centerY().toFloat(),
                highlightColors,
                null
        )
        val mat = Matrix()
        gradientShader.run {
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

    fun highlightView(v: View, scale: Int = 0) {
        val loc = intArrayOf(0, 0)
        v.getLocationOnScreen(loc)
        val rect = Rect()
        v.getGlobalVisibleRect(rect)
        highlightRect(rect, scale)
    }

    fun highlightViewGroup(v: ViewGroup, scale: Int = 0) {
        val loc = intArrayOf(0, 0)
        v.getLocationOnScreen(loc)
        val rect = Rect()
        v.getGlobalVisibleRect(rect)
        highlightRect(rect, scale)
    }

    fun highlightRect(rect: Rect, scale: Int = 0) {
        highlight.set(rect.scale(scale))
        onHighlightRectChanged()
    }

    fun highlightFromPosition(position: Position, tx: Double, ty: Double) {
        val dx = (ty - position.y)/position.zoom * viewWidth
        val dy = (tx - position.x)/position.zoom * viewWidth
        Log.d("HIGHLIGHT", "dx: $dx, dy: $dy, viewWidth: $viewWidth, viewHeight: $viewHeight")
        highlightRect(Rect(
            viewWidth/2  + dx.roundToInt() - highlightCornerRadius.toInt(),
            viewHeight/2 + dy.roundToInt() - highlightCornerRadius.toInt(),
            viewWidth/2  + dx.roundToInt() + highlightCornerRadius.toInt(),
            viewHeight/2 + dy.roundToInt() + highlightCornerRadius.toInt(),
        ))
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




    private val prevFocus = floatArrayOf(0f, 0f)
    private var prevAngle = 0f
    private var prevFocalLen = 1f
    private var initFocalLen = 1f

    val quadCoords = floatArrayOf(0f, 0f)
    val quadFocus = floatArrayOf(0f, 0f)
    var quadScale = 1f
    var quadRotation = 0f

    fun setQuadFocus(screenPos: FloatArray) {

        // update texture quad coordinates
        // convert focus coordinates from screen space to quad space

        quadFocus[0] = screenPos[0]
        quadFocus[1] = screenPos[1]

        // Log.v("SURFACE VIEW", "quadFocus: (${quadFocus[0]}, ${quadFocus[1]})")

    }
    fun translate(dScreenPos: FloatArray) {

        // update texture quad coordinates
        val dQuadPos = floatArrayOf(
            dScreenPos[0],
            dScreenPos[1]
        )

        quadCoords[0] += dQuadPos[0]
        quadFocus[0] += dQuadPos[0]
        quadCoords[1] += dQuadPos[1]
        quadFocus[1] += dQuadPos[1]

    }
    fun translate(dx: Float, dy: Float) {

        quadCoords[0] += dx
        quadFocus[0] += dx
        quadCoords[1] += dy
        quadFocus[1] += dy

    }
    fun zoom(dZoom: Float) {

        quadCoords[0] -= quadFocus[0]
        quadCoords[1] -= quadFocus[1]

        quadCoords[0] *= dZoom
        quadCoords[1] *= dZoom

        quadCoords[0] += quadFocus[0]
        quadCoords[1] += quadFocus[1]

        quadScale *= dZoom

    }
    private fun rotate(p: FloatArray, theta: Float) : FloatArray {

        val sinTheta = sin(theta)
        val cosTheta = cos(theta)
        return floatArrayOf(
            p[0] * cosTheta - p[1] * sinTheta,
            p[0] * sinTheta + p[1] * cosTheta
        )

    }
    fun rotate(dTheta: Float) {

        quadCoords[0] -= quadFocus[0]
        quadCoords[1] -= quadFocus[1]

        val ratio = AspectRatio.RATIO_SCREEN.r
        val rotatedQuadCoords = rotate(floatArrayOf(quadCoords[0], quadCoords[1] * ratio.toFloat()), dTheta)
        quadCoords[0] = rotatedQuadCoords[0]
        quadCoords[1] = rotatedQuadCoords[1]
        quadCoords[1] /= ratio.toFloat()

        quadCoords[0] += quadFocus[0]
        quadCoords[1] += quadFocus[1]

        //Log.v("RR", "quadCoords: (${quadCoords[0]}, ${quadCoords[1]})")

        quadRotation += dTheta

    }
    fun resetQuad() {

        quadCoords[0] = 0f
        quadCoords[1] = 0f

//        quadFocus[0] = 0f
//        quadFocus[1] = 0f

        quadScale = 1f
        quadRotation = 0f

    }
    fun updateQuad() {

//        val vert1 = rotate(floatArrayOf(-quadScale, quadScale * aspect), quadRotation)
//        val vert2 = rotate(floatArrayOf(-quadScale, -quadScale * aspect), quadRotation)
//        val vert3 = rotate(floatArrayOf(quadScale, -quadScale * aspect), quadRotation)
//        val vert4 = rotate(floatArrayOf(quadScale, quadScale * aspect), quadRotation)
//
//        vert1[1] /= aspect
//        vert2[1] /= aspect
//        vert3[1] /= aspect
//        vert4[1] /= aspect
//
//        // create float array of quad coordinates
//        val quadVertices = floatArrayOf(
//            vert1[0] + quadCoords[0], vert1[1] + quadCoords[1], 0f,     // top left
//            vert2[0] + quadCoords[0], vert2[1] + quadCoords[1], 0f,     // bottom left
//            vert3[0] + quadCoords[0], vert3[1] + quadCoords[1], 0f,     // bottom right
//            vert4[0] + quadCoords[0], vert4[1] + quadCoords[1], 0f)     // top right

    }

    override fun onTouchEvent(e: MotionEvent): Boolean {

//        return if (consumeTouch) {
//            mgd.onTouchEvent(event)
//            fsv?.onTouchEvent(event)
//            true
//        } else false

        if (consumeTouch) {
            when (e.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    val focus = e.focus()
                    prevFocus[0] = focus[0]
                    prevFocus[1] = focus[1]
                    setQuadFocus(focus)

                }
                MotionEvent.ACTION_POINTER_DOWN -> {

                    val focus = e.focus()

                    prevAngle = atan2(e.getY(0) - e.getY(1), e.getX(1) - e.getX(0))
                    setQuadFocus(focus)

                    prevFocus[0] = focus[0]
                    prevFocus[1] = focus[1]
                    prevFocalLen = e.focalLength()
                    initFocalLen = prevFocalLen

                }
                MotionEvent.ACTION_MOVE -> {

                    val focus = e.focus()
                    val dx: Float = focus[0] - prevFocus[0]
                    val dy: Float = focus[1] - prevFocus[1]
                    val focalLen = e.focalLength()
                    val dFocalLen = focalLen / prevFocalLen

                    // translate
                    translate(floatArrayOf(dx, dy))

                    if (e.pointerCount > 1) {

                        // zoom
                        zoom(dFocalLen)

                        // rotate
                        val angle = atan2(e.getY(0) - e.getY(1), e.getX(1) - e.getX(0))
                        val dtheta = angle - prevAngle
//                    rotate(dtheta)

                    }

                    prevFocus[0] = focus[0]
                    prevFocus[1] = focus[1]
                    prevFocalLen = focalLen

                }
                MotionEvent.ACTION_POINTER_UP -> {

                    when (e.getPointerId(e.actionIndex)) {
                        0 -> {
                            prevFocus[0] = e.getX(1)
                            prevFocus[1] = e.getY(1)
                        }
                        1 -> {
                            prevFocus[0] = e.getX(0)
                            prevFocus[1] = e.getY(0)
                        }
                    }

                }
                MotionEvent.ACTION_UP -> {}

            }
            fsv?.onTouchEvent(e)
        }

        return consumeTouch

    }

}