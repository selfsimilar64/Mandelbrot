package com.selfsimilartech.fractaleye

import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.shape_preview_item_linear.view.*


class TextureAdapter(
        var textureList: List<Texture>,
        val layoutId: Int
) : RecyclerView.Adapter<TextureAdapter.TextureHolder>() {

    val isGridLayout = layoutId == R.layout.texture_shape_preview_item_grid


    override fun getItemViewType(position: Int): Int {
        return if (isGridLayout) -1 else position
    }

    //this method is returning the view for each item in the list
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextureHolder {
        val v = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return TextureHolder(v)
    }

    //this method is binding the data on the list
    override fun onBindViewHolder(holder: TextureHolder, position: Int) {
        if (!isGridLayout && position % 2 == 1) {
            holder.v.layout.setBackgroundColor(holder.v.resources.getColor(R.color.menu5, null))
        }
        holder.bindItems(textureList[position])
    }

    //this method is giving the size of the list
    override fun getItemCount(): Int {
        return textureList.size
    }

    //the class is holding the list view
    inner class TextureHolder(val v: View) : RecyclerView.ViewHolder(v) {

        fun bindItems(texture: Texture) {

            // Log.d("TEXTURE ADAPTER", "binding texture ${texture.name}")

            val previewText = v.findViewById<TextView>(R.id.previewText)
            previewText.text = v.resources.getString(texture.displayName)

            if (isGridLayout) {
                val previewImage = v.findViewById<ImageView>(R.id.previewImage)
                previewImage.setImageBitmap(texture.thumbnail)
                previewImage.scaleType = ImageView.ScaleType.CENTER_CROP
            }

        }

    }

}