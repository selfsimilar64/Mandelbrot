package com.selfsimilartech.fractaleye

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat

class HighlightToggleButton : GradientButton {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var isChecked = false
        set(value) {
            field = value
            foreground = if (isChecked) ResourcesCompat.getDrawable(resources, R.drawable.menu_button_highlight, null) else null
        }

    override fun performClick(): Boolean {
        val r = super.performClick()
        if (!isChecked) isChecked = true
        return r
    }

}