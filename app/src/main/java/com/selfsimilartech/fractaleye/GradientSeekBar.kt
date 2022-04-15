package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.content.res.ResourcesCompat
import kotlin.math.roundToInt

open class GradientSeekBar(context: Context, attrs: AttributeSet?) : AppCompatSeekBar(context, attrs) {

    private var colors : IntArray
    var color : Int

    private var colorChangeListener : OnColorChangeListener? = null

    init {

        progressDrawable = ResourcesCompat.getDrawable(resources, R.drawable.seekbar_progress_palette_gradient, null)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GradientSeekBar)

        val colorsId = typedArray.getResourceId(R.styleable.GradientSeekBar_colors, 0)
        colors = resources.getIntArray(if (colorsId != 0) colorsId else R.array.color_wheel)
        color = colors[0]
        setThumbColor(color)

        (progressDrawable as LayerDrawable).run {
            setDrawableByLayerId(
                android.R.id.background,
                GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    colors
                ).apply { cornerRadius = 16.dp(context).toFloat() }
            )
//            setLayerHeight(
//                android.R.id.background, 16.dp(context)
//            )
        }
        setOnSeekBarChangeListener(null)

        typedArray.recycle()

    }


    fun setColors(newColors: IntArray) {
        colors = newColors
        (progressDrawable as LayerDrawable).run {
            setDrawableByLayerId(
                android.R.id.background,
                GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    newColors
                ).apply { cornerRadius = 8.dp(context).toFloat() }
            )
        }
        color = colors.interpolate(progress.toFloat()/max.toFloat())
        setThumbColor(color)
        if (thumb.alpha == 255) colorChangeListener?.onColorChanged(color)
    }

    fun setOnColorChangeListener(l: OnColorChangeListener) {
        colorChangeListener = l
    }

    final override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        super.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val newColor = colors.interpolate(progress.toFloat()/seekBar.max.toFloat())
                color = newColor
                setThumbColor(newColor)
                colorChangeListener?.onColorChanged(newColor)
                l?.onProgressChanged(seekBar, progress, fromUser)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                l?.onStartTrackingTouch(seekBar)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                l?.onStopTrackingTouch(seekBar)
            }
        })
    }

    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        onSizeChanged(width, height, 0, 0)
    }

    fun setThumbColor(newColor: Int) {
        (thumb as? GradientDrawable)?.run {
            setColor(newColor)
            setStroke(1.dp(context), if (newColor.luminance() < 0.75f) Color.WHITE else Color.BLACK)
        }
    }

}