package com.selfsimilartech.fractaleye

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class EnumSelector(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    interface OnSelectionChangeListener {
        fun onSelectionChange(newSelection: Int)
    }

    private val viewSelector : ViewSelector
    private var listener : OnSelectionChangeListener? = null
    private val numOptions: Int

    init {

        val layoutInflater = LayoutInflater.from(context)
        layoutInflater.inflate(R.layout.enum_selector, this, true)

        val buttons = listOf<Button>(
            findViewById(R.id.optionButton1),
            findViewById(R.id.optionButton2),
            findViewById(R.id.optionButton3),
            findViewById(R.id.optionButton4),
            findViewById(R.id.optionButton5)
        )
        val title = findViewById<TextView>(R.id.title)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.EnumSelector)

        numOptions = typedArray.getInt(R.styleable.EnumSelector_options, 3)

        buttons.forEachIndexed { index, button ->
            if (index < numOptions) button.show() else button.hide()
        }

        buttons[0].text = typedArray.getString(R.styleable.EnumSelector_option1) ?: "opt1"
        buttons[1].text = typedArray.getString(R.styleable.EnumSelector_option2) ?: "opt2"
        buttons[2].text = typedArray.getString(R.styleable.EnumSelector_option3) ?: "opt3"
        buttons[3].text = typedArray.getString(R.styleable.EnumSelector_option4) ?: "opt4"
        buttons[4].text = typedArray.getString(R.styleable.EnumSelector_option5) ?: "opt5"
        title.text = typedArray.getString(R.styleable.EnumSelector_android_title) ?: "title"

        viewSelector = ViewSelector(
            context, R.drawable.edit_mode_button_highlight, buttons,
            typedArray.getInt(R.styleable.EnumSelector_selectedPosition, 0)
        )

        typedArray.recycle()

        buttons.forEachIndexed { index, button ->
            button.setOnClickListener {
                viewSelector.select(button)
                listener?.onSelectionChange(index)
            }
        }

    }

    fun setOnSelectionChangeListener(l: OnSelectionChangeListener) {
        listener = l
    }

}