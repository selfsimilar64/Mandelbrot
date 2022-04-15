package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

open class ParamButton(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val textView : TextView
    private val imageView : ImageView
    private val toggleable : Boolean

    init {

        val layoutInflater = LayoutInflater.from(context)
        layoutInflater.inflate(R.layout.param_button, this, true)

        textView = findViewById(R.id.text)
        imageView = findViewById(R.id.icon)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ParamButton)
        val text = typedArray.getString(R.styleable.ParamButton_android_text) ?: resources.getString(R.string.plasma)
        val drawable = typedArray.getDrawable(R.styleable.ParamButton_android_drawable) ?: ResourcesCompat.getDrawable(resources, R.drawable.parameter, null)!!

        toggleable = typedArray.getBoolean(R.styleable.ParamButton_toggleable, true)

        textView.text = text
        imageView.setImageDrawable(drawable)

        typedArray.recycle()

    }

    override fun setActivated(activated: Boolean) {
        if (toggleable) {
            if (activated) showText() else hideText()
        }
        super.setActivated(activated)
    }

    fun hideText() {
        textView.hide()
    }

    fun showText() {
        textView.show()
    }

    fun setImageResource(id: Int) {
        imageView.setImageResource(id)
    }

    fun setText(id: Int) {
        textView.setText(id)
    }

}