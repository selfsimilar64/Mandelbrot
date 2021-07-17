package com.selfsimilartech.fractaleye

import android.content.Context
import android.util.AttributeSet

class ToggleButton : GradientButton {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var isChecked = false
        set(value) {
            field = value
            alpha = if (isChecked || showGradient) 1f else 0.4f
        }

    override fun performClick(): Boolean {
        isChecked = !isChecked
        return super.performClick()
    }

}