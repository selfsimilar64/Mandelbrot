package com.selfsimilartech.fractaleye

import android.graphics.drawable.ColorDrawable
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import kotlinx.android.synthetic.main.color_preview_item_linear.view.*
import java.util.ArrayList

class ColorPaletteAdapter(
        private val colorList: ArrayList<ColorPalette>,
        private val layoutId: Int
) : RecyclerView.Adapter<ColorPaletteAdapter.ColorPaletteHolder>() {

    val isGridLayout = layoutId == R.layout.color_preview_item_grid


    override fun getItemViewType(position: Int): Int {
        return if (isGridLayout) -1 else position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorPaletteHolder {
        val v = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ColorPaletteHolder(v, isGridLayout)
    }

    override fun onBindViewHolder(holder: ColorPaletteHolder, position: Int) {
        if (!isGridLayout && position % 2 == 1) {
            holder.v.layout.background = ColorDrawable(holder.v.resources.getColor(R.color.menu5, null))
        }
        holder.bindItems(colorList[position])
    }

    override fun getItemCount(): Int {
        return colorList.size
    }

    class ColorPaletteHolder(val v: View, private val layoutIsGrid: Boolean) : RecyclerView.ViewHolder(v) {

        fun bindItems(palette: ColorPalette) {

            val previewGradient = v.findViewById<CardView>(R.id.colorPreviewGradient)
            val previewText = v.findViewById<TextView>(R.id.colorPreviewText)
            previewGradient.foreground = palette.gradientDrawable
            previewText.text = palette.name

            if (layoutIsGrid) {
                val previewImage = v.findViewById<ImageView>(R.id.colorPreviewImage)
                previewImage.setImageBitmap(palette.thumbnail)
                previewImage.scaleType = ImageView.ScaleType.CENTER_CROP
            }





        }
    }

}