package com.selfsimilartech.fractaleye

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.GestureDetectorCompat
import kotlin.math.abs

class InterceptableLinearLayout : LinearLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    private var intercept = false

    fun enable() {
        intercept = false
    }

    fun disable() {
        intercept = true
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        return intercept
    }

}