package com.selfsimilartech.fractaleye

import android.content.Context
import android.util.AttributeSet
import android.widget.Button

class ToggleButton : androidx.appcompat.widget.AppCompatButton {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var isChecked = false
        set(value) {
            field = value
            alpha = if (isChecked) 1f else 0.5f
        }

    override fun performClick(): Boolean {
        isChecked = !isChecked
        return super.performClick()
    }

}