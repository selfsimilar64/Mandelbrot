package com.selfsimilartech.fractaleye

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.res.ResourcesCompat

class EditModeButton2 : AppCompatImageButton {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var mode : EditMode = EditMode.POSITION
    var isChecked = false
        set (value) {
            field = value
            if (value) {
                foreground = ResourcesCompat.getDrawable(resources, R.drawable.edit_mode_button_highlight, null)
            } else {
                foreground = null
            }
        }

}