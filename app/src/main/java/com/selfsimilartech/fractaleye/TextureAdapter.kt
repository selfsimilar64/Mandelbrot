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

class TextureAdapter(private val textureList: ArrayList<Texture>) : RecyclerView.Adapter<TextureAdapter.TextureHolder>() {

    //this method is returning the view for each item in the list
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextureHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.preview_item, parent, false)
        return TextureHolder(v)
    }

    //this method is binding the data on the list
    override fun onBindViewHolder(holder: TextureHolder, position: Int) {
        holder.bindItems(textureList[position])
    }

    //this method is giving the size of the list
    override fun getItemCount(): Int {
        return textureList.size
    }

    //the class is holding the list view
    class TextureHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(texture: Texture) {

            val previewImage = itemView.findViewById<ImageView>(R.id.previewImage)
            val previewText = itemView.findViewById<TextView>(R.id.previewText)
            previewText.text = texture.name
            previewImage.setImageBitmap(texture.thumbnail)
            previewImage.scaleType = ImageView.ScaleType.CENTER_CROP

        }
    }

}