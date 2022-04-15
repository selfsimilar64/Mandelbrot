package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout

class HSVColorSelector(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val hueSelector             : GradientSeekBar
    private val saturationValueSelector : SaturationValueSelector

    private var color : Int

    private var listener : OnColorChangeListener? = null

//    val hueEdit : EditText
//    val satEdit : EditText
//    val valEdit : EditText

    init {

        val layoutInflater = LayoutInflater.from(context)
        layoutInflater.inflate(R.layout.custom_color_selector, this, true)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.HSVColorSelector)

        val colorIn = typedArray.getColor(R.styleable.HSVColorSelector_android_color, Color.BLACK)
        color = colorIn

        typedArray.recycle()

        hueSelector = findViewById(R.id.hueSelector)
        saturationValueSelector = findViewById(R.id.satValueSelector)

//        hueEdit = findViewById(R.id.hueEdit)
//        satEdit = findViewById(R.id.satEdit)
//        valEdit = findViewById(R.id.valEdit)

        hueSelector.setOnColorChangeListener(object : OnColorChangeListener {
            override fun onColorChanged(newColor: Int) {
                saturationValueSelector.setHue(newColor.hue())
            }
        })

        saturationValueSelector.setOnColorChangeListener(object : OnColorChangeListener {
            override fun onColorChanged(newColor: Int) {
                listener?.onColorChanged(newColor)
            }
        })

//        hueEdit.setOnEditorActionListener(EditListener.new { s ->
//            val result = s.formatToDouble()?.toFloat()
//            if (result != null) hueSelector.progress = (result/360f * hueSelector.max.toFloat()).toInt()
//            "%d".format(satValSelector.hue.roundToInt())
//        })
//        satEdit.setOnEditorActionListener(EditListener.new { s ->
//            val result = s.formatToDouble()?.toFloat()
//            if (result != null) satValSelector.setSat(result/100f)
//            val sat = (100f * satValSelector.sat).let { if (it.isNaN()) 0 else it.roundToInt() }
//            "%d".format(sat)
//        })
//        valEdit.setOnEditorActionListener(EditListener.new { s ->
//            val result = s.formatToDouble()?.toFloat()
//            if (result != null) satValSelector.setVal(result/100f)
//            val value = (100f * satValSelector.value).let { if (it.isNaN()) 0 else it.roundToInt() }
//            "%d".format(value)
//        })

    }

    companion object {
        val TAG = "HSV SELECTOR"
    }


    fun setColor(newColor: Int, useless: Boolean = false) {

        Log.d(TAG, "loading color: (${color.hue()}, ${color.sat()}, ${color.value()})")
        color = newColor

        hueSelector.progress = (color.hue()/360f * hueSelector.max.toFloat()).toInt()
        saturationValueSelector.setValues(color.sat(), color.value())

        listener?.onColorChanged(color)

    }

    fun setOnColorChangeListener(l: OnColorChangeListener) {
        listener = l
    }

}