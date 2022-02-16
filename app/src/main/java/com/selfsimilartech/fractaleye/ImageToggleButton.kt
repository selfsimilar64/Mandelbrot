package com.selfsimilartech.fractaleye

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageButton

class ImageToggleButton : androidx.appcompat.widget.AppCompatImageButton {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    enum class Mode { ALPHA, IMAGE, NONE }

    var mode = Mode.ALPHA

    val uncheckedAlpha = 128
    val checkedAlpha = 255

    var uncheckedImageId = -1
    var checkedImageId = -1

    var isChecked = false
        set(value) {
            field = value
            when (mode) {
                Mode.ALPHA -> drawable?.alpha = if (value) checkedAlpha else uncheckedAlpha
                Mode.IMAGE -> setImageResource(if (value) checkedImageId else uncheckedImageId)
                Mode.NONE -> {}
            }
        }

    override fun performClick(): Boolean {
        isChecked = !isChecked
        return super.performClick()
    }

}