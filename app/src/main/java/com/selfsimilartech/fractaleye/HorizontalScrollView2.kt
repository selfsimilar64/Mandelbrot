package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.widget.HorizontalScrollView
import androidx.core.content.res.ResourcesCompat


fun drawableToBitmap(drawable: Drawable, height: Int): Bitmap? {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.BLACK)
    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    drawable.draw(canvas)
    return bitmap
}

class HorizontalScrollView2 : HorizontalScrollView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private var viewWidth : Int = 0
    private var viewHeight : Int = 0

    private val leftArrowRect = Rect()
    private val leftArrowPaint = Paint()
    private var leftArrow : Bitmap? = null
    private var leftArrowShader : BitmapShader? = null

    private var rightArrow : Bitmap? = null


    override fun onSizeChanged(xNew: Int, yNew: Int, xOld: Int, yOld: Int) {
        super.onSizeChanged(xNew, yNew, xOld, yOld)

        viewWidth = xNew
        viewHeight = yNew

        leftArrow = drawableToBitmap(ResourcesCompat.getDrawable(resources, R.drawable.scroll_indicator_left, null)!!, viewHeight)
        leftArrowShader = BitmapShader(leftArrow!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        leftArrowPaint.shader = leftArrowShader

        rightArrow = drawableToBitmap(ResourcesCompat.getDrawable(resources, R.drawable.scroll_indicator_right, null)!!, viewHeight)


    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            if (scrollX > 0)          drawBitmap(leftArrow!!, scrollX.toFloat(), 0f, null)
            val pos = scrollX.toFloat() + viewWidth - rightArrow!!.width
            Log.e("SCROLLVIEW", "viewWidth: $viewWidth, scrollX: $scrollX, rightArrow width: ${rightArrow!!.width}, rightArrow position: $pos")
            if (scrollX < viewWidth) drawBitmap(rightArrow!!, pos, 0f, null)
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        leftArrowRect.set(l, 0, l + leftArrow!!.width, viewHeight)
    }



}