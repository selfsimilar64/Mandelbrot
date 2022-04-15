package com.selfsimilartech.fractaleye

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class BetterButton(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    init {

        val layoutInflater = LayoutInflater.from(context)
        layoutInflater.inflate(R.layout.button, this, true)


        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BetterButton)

        val textView = findViewById<TextView>(R.id.text)
        textView.text = typedArray.getString(R.styleable.BetterButton_android_text) ?: "text"

        val imageView = findViewById<ImageView>(R.id.image)
        imageView.setImageDrawable(typedArray.getDrawable(R.styleable.BetterButton_android_icon))

        typedArray.recycle()

    }

}