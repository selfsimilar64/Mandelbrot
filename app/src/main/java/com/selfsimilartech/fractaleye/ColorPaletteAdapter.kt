package com.selfsimilartech.fractaleye

import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.support.v7.widget.RecyclerView

/**
 * Created by Belal on 6/19/2017.
 */

class ColorPaletteAdapter(val colorList: ArrayList<ColorPalette>) : RecyclerView.Adapter<ColorPaletteAdapter.ColorPaletteHolder>() {

    //this method is returning the view for each item in the list
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorPaletteHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item, parent, false)
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

    //the class is hodling the list view
    class ColorPaletteHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(palette: ColorPalette) {
            val preview = itemView.findViewById<ImageView>(R.id.preview)
            val name = itemView.findViewById<TextView>(R.id.name)
            preview.setImageDrawable(GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    palette.getColors(itemView.resources, palette.colors)
            ))
            name.text = palette.name
        }
    }

}