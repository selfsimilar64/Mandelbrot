package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.BaseAdapter
import android.widget.TextView


class ColorPaletteAdapter(
        private val context: Context,
        private var palettes: List<ColorPalette>
) : BaseAdapter() {

    private var inflter: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return palettes.size
    }

    override fun getItem(i: Int): Any? {
        return palettes[i]
    }

    override fun getItemId(i: Int): Long {
        return 0
    }

    override fun getView(i: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflter.inflate(R.layout.color_palette_spinner_item, null)
        val paletteNameText = view.findViewById<TextView>(R.id.paletteNameText)
        val gradientView = view.findViewById<View>(R.id.gradientView)
        paletteNameText.text = palettes[i].name
        gradientView.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                palettes[i].getColors(context.resources, palettes[i].colors)
        )
        return view
    }

}