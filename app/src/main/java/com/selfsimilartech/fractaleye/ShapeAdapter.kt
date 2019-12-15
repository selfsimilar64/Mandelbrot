package com.selfsimilartech.fractaleye

import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.support.v7.widget.RecyclerView

/**
 * Created by Belal on 6/19/2017.
 */

class ShapeAdapter(val shapeList: ArrayList<Shape>) : RecyclerView.Adapter<ShapeAdapter.ShapeHolder>() {

    //this method is returning the view for each item in the list
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShapeHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.preview_item, parent, false)
        return ShapeHolder(v)
    }

    //this method is binding the data on the list
    override fun onBindViewHolder(holder: ShapeHolder, position: Int) {
        holder.bindItems(shapeList[position])
    }

    //this method is giving the size of the list
    override fun getItemCount(): Int {
        return shapeList.size
    }

    //the class is hodling the list view
    class ShapeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(shape: Shape) {
            val previewImage = itemView.findViewById<ImageView>(R.id.previewImage)
            val previewText = itemView.findViewById<TextView>(R.id.previewText)
            previewImage.setImageResource(shape.icon)
            previewText.text = shape.name
        }
    }

}