package com.selfsimilartech.fractaleye

import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.shape_preview_item_linear.view.*

/**
 * Created by Belal on 6/19/2017.
 */

class ShapeAdapter(
        val shapeList: List<Shape>,
        val layoutId: Int
) : RecyclerView.Adapter<ShapeAdapter.ShapeHolder>() {

    val isGridLayout = layoutId == R.layout.texture_shape_preview_item_grid


    override fun getItemViewType(position: Int): Int {
        return if (isGridLayout) -1 else position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShapeHolder {
        val v = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ShapeHolder(v)
    }

    override fun onBindViewHolder(holder: ShapeHolder, position: Int) {
        if (!isGridLayout && position % 2 == 1) {
            holder.v.layout.background = ColorDrawable(holder.v.resources.getColor(R.color.menu5, null))
        }
        holder.bindItems(shapeList[position])
    }

    override fun getItemCount(): Int {
        return shapeList.size
    }

    inner class ShapeHolder(val v: View) : RecyclerView.ViewHolder(v) {

        fun bindItems(shape: Shape) {

            v.previewImage.setImageResource(shape.icon)
            v.previewText.text = v.resources.getString(shape.name)
            if (!isGridLayout) {
                //v.shapeKatex.setDisplayText(v.resources.getString(shape.katex).format("c"))
            }

        }
    }

}