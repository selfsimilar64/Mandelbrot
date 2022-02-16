package com.selfsimilartech.fractaleye

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.children

class EditModeToggle : FrameLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private lateinit var main : ImageView
    private lateinit var buttonLayout : LinearLayout

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

        super.onLayout(changed, l, t, r, b)

        main = getChildAt(0) as ImageView
        buttonLayout = getChildAt(1) as LinearLayout
        layoutTransition.setDuration(200L)
        layoutTransition.disableTransitionType(LayoutTransition.CHANGING)

        // main.animation = AlphaAnimation()

    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val rect = Rect()
        main.getHitRect(rect)
        Log.e("EDIT", "hitrect: $rect, event: (${ev.x}, ${ev.y})")
        if (rect.contains(ev.x.toInt(), ev.y.toInt())) {
            Log.e("EDIT", "intercepted!")
            return true
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent?) : Boolean {

        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                main.hide()
                buttonLayout.show()
            }
            MotionEvent.ACTION_MOVE -> {

            }
            MotionEvent.ACTION_UP -> {
                main.show()
                buttonLayout.hide()
            }
        }

        return true

    }

}