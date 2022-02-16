package com.selfsimilartech.fractaleye

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.selfsimilartech.fractaleye.databinding.ColorDragItemBinding
import com.woxthebox.draglistview.DragItemAdapter

class CustomColorDragAdapter(
        colors: ArrayList<Int>,
        private val layoutId: Int,
        private val grabHandleId: Int,
        private val dragOnLongPress: Boolean,
        val linkColor: (activeColorIndex: Int, color: Int) -> Unit
) : DragItemAdapter<Pair<Long, Int>, CustomColorDragAdapter.ColorPaletteDragHolder>() {

    init {

        itemList = (MutableList(colors.size) { i -> i.toLong() }).zip(colors)
        // linkColor(0, itemList[0].second)
        // itemList.forEachIndexed { index, pair -> Log.e("DRAG ADAPTER", "item $index: <${pair.first}, ${pair.second}>") }

    }

    var nextUniqueId = 0L
        get() {
            field++
            Log.d("DRAG ADAPTER", "next unique id: $field")
            return field
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
        nextUniqueId = newColors.size.toLong()
        selectedItemIndex = 0
        linkColor(selectedItemIndex, itemList[selectedItemIndex].second)
    }

    override fun addItem(pos: Int, item: Pair<Long, Int>?) {
        if (mItemList != null && mItemList.size >= pos) {
            mItemList.add(pos, item)
            notifyDataSetChanged()
        }
        // itemList.add(pos, item)
        // notifyItemInserted(pos)
    }

    override fun removeItem(pos: Int) : Any {
        if (mItemList != null && mItemList.size > pos && pos >= 0) {
            val item: Any = mItemList.removeAt(pos)
            notifyDataSetChanged()
            return item
        }
        return Any()
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

        holder.b.colorView.setCardBackgroundColor(itemList[position].second)
        holder.b.colorView.radius =
            if (position == selectedItemIndex) 20.dp(holder.v.context).toFloat()
            else 0f
        holder.v.tag = itemList[position]

    }

    inner class ColorPaletteDragHolder(val v: View) : ViewHolder(v, grabHandleId, dragOnLongPress) {

        val b = ColorDragItemBinding.bind(v)

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