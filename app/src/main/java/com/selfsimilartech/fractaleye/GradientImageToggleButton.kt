package com.selfsimilartech.fractaleye

import android.content.Context
import android.util.AttributeSet

class GradientImageToggleButton : GradientImageButton, Toggleable {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override var isChecked = false
        set(value) {
            field = value
            if (!showGradient) {
                drawable?.alpha = if (value) 255 else 127
            }
        }

    override fun performClick(): Boolean {
        isChecked = !isChecked
        return super.performClick()
    }

}