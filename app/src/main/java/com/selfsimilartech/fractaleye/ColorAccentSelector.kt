package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatImageButton

class ColorAccentSelector(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    val paletteSeekBar          : GradientSeekBar
    val blackAccentButton       : AccentButton
    val whiteAccentButton       : AccentButton
    val customAccentButton      : AccentButton
    val editCustomAccentButton  : AppCompatImageButton

    val quickAccentLayout : LinearLayout

    val colorSelector : HSVColorSelector

    private val viewSelector : ViewSelector

    var color : Int = Color.BLACK

    private var colorChangeListener : OnColorChangeListener? = null

    interface OnCustomAccentActionListener {
        fun onEditStart()
    }
    private var customAccentActionListener : OnCustomAccentActionListener? = null


    companion object {
        const val TAG = "ACCENT SELECTOR"
    }
    init {

        val layoutInflater = LayoutInflater.from(context)
        layoutInflater.inflate(R.layout.color_accent_selector, this, true)

        paletteSeekBar     = findViewById(R.id.palette_seek_bar)
        blackAccentButton  = findViewById(R.id.black_accent_button)
        whiteAccentButton  = findViewById(R.id.white_accent_button)
        customAccentButton = findViewById(R.id.custom_accent_button)
        editCustomAccentButton = findViewById(R.id.edit_custom_accent_button)

        quickAccentLayout = findViewById(R.id.quick_accents)

        colorSelector = findViewById(R.id.color_selector)
        colorSelector.setColor(resources.getColor(R.color.accent2, null))
        colorSelector.setOnColorChangeListener(object : OnColorChangeListener {
            override fun onColorChanged(newColor: Int) {
                customAccentButton.setColor(newColor)
                colorChangeListener?.onColorChanged(newColor)
            }
        })


        viewSelector = ViewSelector(context, R.drawable.settings, listOf(blackAccentButton, whiteAccentButton, customAccentButton))

        paletteSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                viewSelector.select(null)
                paletteSeekBar.thumb.alpha = 255
                Log.v(TAG, "thumb set")
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        paletteSeekBar.thumb.alpha = 0

        listOf(blackAccentButton, whiteAccentButton, customAccentButton).forEach { button ->
            button.setOnClickListener {
                paletteSeekBar.thumb.alpha = 0
                color = button.getColor()
                viewSelector.select(button)
                colorChangeListener?.onColorChanged(color)
            }
        }

    }


    fun setOnColorChangedListener(l: OnColorChangeListener) {
        colorChangeListener = l
        paletteSeekBar.setOnColorChangeListener(l)
    }

    fun setOnEditCustomAccentListener(l: OnCustomAccentActionListener) {
        customAccentActionListener = l
        editCustomAccentButton.setOnClickListener {
            customAccentActionListener?.onEditStart()
            openColorSelector()
            customAccentButton.performClick()
        }
    }

    private fun openColorSelector() {
        quickAccentLayout.hide()
        colorSelector.show()
    }

    fun closeColorSelector() {
        quickAccentLayout.show()
        colorSelector.hide()
    }


}