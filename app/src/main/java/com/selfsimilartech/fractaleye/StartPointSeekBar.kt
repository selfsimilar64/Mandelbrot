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
class StartPointSeekBar : SeekBar {
    
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

        when {
            progress > max/2 ->  {
                rect.set(
                        width/2,
                        height/2 - seekbarHeight/2,
                        (width/2f - width/max.toFloat() * (max/2f - progress) - (progress/(max/2f) - 1f)*2f*thumbSize).toInt(),
                        height/2 + seekbarHeight/2
                )
                paint.color = Color.WHITE
                canvas.drawRect(rect, paint)
                canvas.drawCircle(
                        width/2f - width/max.toFloat() * (max/2f - progress) - (progress/(max/2f) - 1f)*2f*thumbSize,
                        height/2f,
                        thumbSize,
                        paint
                )
            }
            progress < max/2 -> {
                rect.set(
                        (width/2f - width/max.toFloat() * (max/2f - progress) + (1f - progress/(max/2f))*2f*thumbSize).toInt(),
                        height/2 - seekbarHeight/2,
                        width/2,
                        height/2 + seekbarHeight/2
                )
                paint.color = Color.WHITE
                canvas.drawRect(rect, paint)
                canvas.drawCircle(
                        width/2f - width/max.toFloat() * (max/2f - progress) + (1f - progress/(max/2f))*2f*thumbSize,
                        height/2f,
                        thumbSize,
                        paint
                )
            }
            progress == max/2 -> {
                paint.color = Color.WHITE
                canvas.drawCircle(
                        width/2f,
                        height/2f,
                        thumbSize,
                        paint
                )
            }
        }
        //super.onDraw(canvas);
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN) {
            stopNestedScroll()
            return true
        }
        return super.onTouchEvent(e)
    }

}
