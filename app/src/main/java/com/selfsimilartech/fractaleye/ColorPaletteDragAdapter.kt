package com.selfsimilartech.fractaleye

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woxthebox.draglistview.DragItemAdapter
import kotlinx.android.synthetic.main.color_drag_item.view.*
import java.util.ArrayList

class ColorPaletteDragAdapter(
        colors: ArrayList<Int>,
        private val layoutId: Int,
        private val grabHandleId: Int,
        private val dragOnLongPress: Boolean,
        val linkColor: (activeColorIndex: Int, color: Int) -> Unit
) : DragItemAdapter<Pair<Long, Int>, ColorPaletteDragAdapter.ColorPaletteDragHolder>() {

    init {

        itemList = (MutableList(colors.size) { i -> i.toLong() }).zip(colors)
        // linkColor(0, itemList[0].second)
        // itemList.forEachIndexed { index, pair -> Log.e("DRAG ADAPTER", "item $index: <${pair.first}, ${pair.second}>") }

    }

    var selectedItemIndex = 0
        set(value) {
            field = value
            linkColor(value, itemList[value].second)
        }

    fun updateColor(index: Int, newColor: Int) {
        //Log.e("DRAG ADAPTER", "updating color at index $index")
        itemList[index] = Pair(itemList[index].first, newColor)
        notifyItemChanged(index)
    }
    fun updateColors(newColors: ArrayList<Int>) {
        itemList = (MutableList(newColors.size) { i -> i.toLong() }).zip(newColors)
        linkColor(selectedItemIndex, itemList[selectedItemIndex].second)
    }

    override fun getUniqueItemId(position: Int): Long {
        return itemList[position].first
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorPaletteDragHolder {
        Log.e("DRAG ADAPTER", "view holder created")
        val v = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        val holder = ColorPaletteDragHolder(v)
        return holder
    }

    override fun onBindViewHolder(holder: ColorPaletteDragHolder, position: Int) {
        //Log.e("DRAG ADAPTER", "item bound")
        super.onBindViewHolder(holder, position)

        holder.v.customColorSquare.setBackgroundColor(itemList[position].second)
        holder.v.customColorLayout.setBackgroundColor(
                if (position == selectedItemIndex) Color.WHITE
                else itemList[position].second
        )
        holder.v.tag = itemList[position]

    }

    inner class ColorPaletteDragHolder(val v: View) : ViewHolder(v, grabHandleId, dragOnLongPress) {

        override fun onItemClicked(view: View?) {

            val selectedItemIndexPrev = selectedItemIndex
            selectedItemIndex = itemList.map { it.first }.indexOf((v.tag as Pair<*, *>).first)

            if (selectedItemIndex != selectedItemIndexPrev) {

                // link palette color to color selector
                Log.e("DRAG ADAPTER", "selectedItemIndex: $selectedItemIndex")
                linkColor(selectedItemIndex, itemList[selectedItemIndex].second)

                // notifyItemChanged(selectedItemIndexPrev)
                // notifyItemChanged(selectedItemIndex)
                notifyDataSetChanged()
                super.onItemClicked(view)

            }

        }

    }

}