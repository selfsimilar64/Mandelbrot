package com.selfsimilartech.fractaleye

import android.graphics.drawable.GradientDrawable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import java.util.ArrayList

class ColorPaletteAdapter(
        private val colorList: ArrayList<ColorPalette>
) : RecyclerView.Adapter<ColorPaletteAdapter.ColorPaletteHolder>() {



    //this method is returning the view for each item in the list
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorPaletteHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.color_preview_item, parent, false)
        return ColorPaletteHolder(v)
    }

    //this method is binding the data on the list
    override fun onBindViewHolder(holder: ColorPaletteHolder, position: Int) {
        holder.bindItems(colorList[position])
    }

    //this method is giving the size of the list
    override fun getItemCount(): Int {
        return colorList.size
    }

    //the class is holding the list view
    class ColorPaletteHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(palette: ColorPalette) {

            val previewImage = itemView.findViewById<ImageView>(R.id.colorPreviewImage)
            val previewPalette = itemView.findViewById<ImageView>(R.id.previewPalette)
            val previewText = itemView.findViewById<TextView>(R.id.colorPreviewText)

            previewPalette.setImageDrawable(GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    ColorPalette.getColors(itemView.resources, palette.ids)
            ))

            previewText.text = palette.name

            previewImage.setImageBitmap(palette.thumbnail)
            previewImage.scaleType = ImageView.ScaleType.CENTER_CROP

        }
    }

}