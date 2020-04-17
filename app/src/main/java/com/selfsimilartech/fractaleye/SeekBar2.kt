package com.selfsimilartech.fractaleye

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.SeekBar

@SuppressLint("AppCompatCustomView")
class SeekBar2 : SeekBar {

    private var rect: Rect = Rect()
    private var paint: Paint = Paint()
    private var seekbarHeight = 0
    private var thumbSize = 0f

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        rect = Rect()
        paint = Paint()
        seekbarHeight = 6
        thumbSize = resources.getDimension(R.dimen.satValueSelectorRadius)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    @Synchronized
    override fun onDraw(canvas: Canvas) {

        rect[0 + thumbOffset, height / 2 - seekbarHeight / 2, width - thumbOffset] = height / 2 + seekbarHeight / 2
        paint.color = Color.GRAY
        canvas.drawRect(rect, paint)

        rect.set(
                thumbOffset,
                height/2 - seekbarHeight/2,
                (progress.toFloat()/max*(width - 4f*thumbSize) + 2f*thumbSize).toInt(),
                height/2 + seekbarHeight/2
        )
        paint.color = Color.WHITE
        canvas.drawRect(rect, paint)
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
