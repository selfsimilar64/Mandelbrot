package com.selfsimilartech.fractaleye

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton

class SensitivityButton : AppCompatImageButton {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var param : Sensitive = PositionParam.ZOOM
        set(value) {
            field = value
            setImageResource(value.sensitivity.iconId)
        }

    override fun performClick(): Boolean {
        param.sensitivity = param.sensitivity.next()
        setImageResource(param.sensitivity.iconId)
        return super.performClick()
    }

}