package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.res.ResourcesCompat

class AccentButton(context: Context, attrs: AttributeSet?) : AppCompatImageButton(context, attrs) {

    private var color = Color.BLACK

    init {

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AccentButton)
        setColor(typedArray.getColor(R.styleable.AccentButton_android_color, Color.BLACK))
        typedArray.recycle()

        background = ResourcesCompat.getDrawable(resources, R.drawable.toggleable_highlight_background, null)

    }

    fun getColor() : Int {
        return color
    }

    fun setColor(newColor: Int) {
        color = newColor
        (drawable as GradientDrawable).let {
            it.setColor(color)
            it.setStroke(1.dp(context), Color.GRAY)
        }
    }

}