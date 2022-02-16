package com.selfsimilartech.fractaleye

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.selfsimilartech.fractaleye.databinding.PaletteListItemBinding

class NewListAdapter<T> (

    private val items            : ArrayList<T>,
    private val recycler         : RecyclerView,
    private val tag              : String,
    private val selectionEnabled : Boolean

) : RecyclerView.Adapter<NewListAdapter.ItemViewHolder<T>>() where T : Customizable {

    interface OnItemActionListener<T> {
        fun onClick(t: T) : Boolean
        fun onEdit(t: T)
        fun onDelete(t: T)
        fun onDuplicate(t: T)
        fun onToggleFavorite(t: T, isSelected: Boolean)
    }


    private val viewHolders = arrayListOf<ItemViewHolder<T>>()

    private var onItemActionListener : OnItemActionListener<T>? = null

    fun setOnItemActionListener(l: OnItemActionListener<T>) {
        onItemActionListener = l
    }



    open class ItemViewHolder<T>(view: View) : RecyclerView.ViewHolder(view) {

        var t: T? = null

        var layout          : FrameLayout?          = view.findViewById( R.id.listItemLayout)
        var content         : LinearLayout?         = view.findViewById( R.id.listItemContentLayout )
        var options         : LinearLayout?         = view.findViewById( R.id.listItemOptionsLayout )
        var name            : GradientTextView?     = view.findViewById( R.id.listItemName     )
        var image           : GradientImageView?    = view.findViewById( R.id.listItemImage    )
        var favoriteButton  : ImageToggleButton?    = view.findViewById( R.id.favoriteButton       )
        var editButton      : ImageButton?          = view.findViewById( R.id.editButton           )
        var deleteButton    : ImageButton?          = view.findViewById( R.id.deleteButton         )
        var copyButton      : GradientImageButton?  = view.findViewById( R.id.copyButton            )

        var gradient        : CardView?        = view.findViewById( R.id.colorPreviewGradient )  // only for palettes

    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : ItemViewHolder<T> {
        val newHolder = ItemViewHolder<T>(PaletteListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false).root)
        viewHolders.add(newHolder)
        return newHolder
    }

    override fun onBindViewHolder(holder: ItemViewHolder<T>, position: Int) {

        val item = items[position]
        holder.t = item

        holder.itemView.isActivated = selectionEnabled && position == selectedPosition
        holder.itemView.setOnClickListener {

            if (SettingsConfig.goldEnabled || !item.goldFeature) {
                val listPosition = items.indexOf(item)
                if (listPosition != -1) recycler.smoothSnapToPosition(listPosition, items.size)
                if (selectionEnabled) setSelectedPosition(listPosition, holder.itemView)
            }
            onItemActionListener?.onClick(item)

        }


        holder.options?.hide()
        holder.layout?.setOnLongClickListener {
            holder.options?.show()
            true
        }
        holder.options?.setOnClickListener {
            holder.options?.hide()
        }

        if (item.isCustom()) {
            holder.editButton?.show()
            holder.deleteButton?.show()
        } else {
            holder.editButton?.hide()
            holder.deleteButton?.hide()
        }


        holder.name?.text = item.name
        holder.name?.showGradient = item.goldFeature && !SettingsConfig.goldEnabled
        when {
            item is Shape && !item.isCustom() -> holder.image?.setImageResource(item.thumbnailId)
            else                              -> holder.image?.setImageBitmap(item.thumbnail)
        }
        holder.image?.scaleType = ImageView.ScaleType.CENTER_CROP
        holder.image?.showGradient = item is Shape && item.goldFeature && !SettingsConfig.goldEnabled

        holder.favoriteButton?.run {
            uncheckedImageId = R.drawable.unstarred
            checkedImageId = R.drawable.starred
            mode = ImageToggleButton.Mode.IMAGE
            isChecked = item.isFavorite
        }
        holder.favoriteButton?.setOnClickListener {

            holder.favoriteButton?.run { item.isFavorite = isChecked }
            onItemActionListener?.onToggleFavorite(item, items.indexOf(item) == selectedPosition)
            holder.options?.hide()

        }
        if (item.isCustom()) {

            holder.editButton?.setOnClickListener {

                holder.options?.hide()
                onItemActionListener?.onEdit(item)

            }
            holder.deleteButton?.setOnClickListener {

                holder.options?.hide()
                onItemActionListener?.onDelete(item)

            }

        }
        if (item is Fractal || item is Texture || (item is Shape && item.latex == "")) holder.copyButton?.hide()
        else  {
            holder.copyButton?.show()
            holder.copyButton?.showGradient = !SettingsConfig.goldEnabled
            holder.copyButton?.setOnClickListener {
                holder.options?.hide()
                onItemActionListener?.onDuplicate(item)
            }
        }

        if (item is Palette) {
            holder.gradient?.foreground = item.gradientDrawable
        } else {
            holder.gradient?.hide()
        }

    }



    fun addItem(item: T) {
        items.add(0, item)
        notifyItemInserted(0)
        selectedPosition++
    }

    fun removeItem(position: Int) {
        if (position >= 0 && position < items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
            if (position < selectedPosition) selectedPosition--
        }
    }

    fun removeItem(t: T) {
        val position = items.indexOf(t)
        if (position != -1) {
            items.removeAt(position)
            notifyItemRemoved(position)
            if (position < selectedPosition) selectedPosition--
            if (items.isEmpty()) selectedPosition = -1
        }
    }

    fun updateItem(position: Int) {
        notifyItemChanged(position)
    }

    fun updateItem(t: T) {
        val position = items.indexOf(t)
        if (position != -1) notifyItemChanged(position)
    }

    fun updateAllItems() {
        for (i in items.indices) notifyItemChanged(i)
    }

    fun getItem(position: Int) : T? {
        return items.getOrNull(position)
    }

    fun getPosition(item: T) : Int {
        return items.indexOf(item)
    }

    fun updateDataset(newItems: ArrayList<T>) {
        items.removeAll { true }
        items.addAll(newItems)
        notifyDataSetChanged()
    }




    private var selectedPosition = -1

    fun getSelectedPosition() : Int {
        return selectedPosition
    }

    fun clearSelection() {
        selectedPosition = -1
        viewHolders.forEach { holder ->
            holder.itemView.isActivated = false
        }
    }

    private fun setSelectedPosition(position: Int, v: View) {
        if (position != selectedPosition) {
            clearSelection()
            selectedPosition = position
            v.isActivated = true
        }
    }

    fun setSelectedItem(t: T) {
        clearSelection()
        val position = items.indexOf(t)
        Log.d(tag, "setting selection to {$position}")
        if (position != selectedPosition) {
            selectedPosition = position
            viewHolders.forEach { holder ->
                if (holder.t == t) holder.itemView.isActivated = true
            }
        }
    }

    fun getSelectedItem() : T? {
        return items.getOrNull(selectedPosition)
    }




    fun setActivatedPosition(other: T) {
        setActivatedPosition(items.indexOfFirst { item -> item == other })
        items.forEachIndexed { i, item ->
            if (item == other) setActivatedPosition(i)
        }
    }

    fun setActivatedPosition(position: Int) {
//        if (position == -1) {
//            removeSelection(activatedPos)
//        } else {
//            activatedPos = position
//            toggleSelection(position) //Important!
//        }
    }

    fun onGoldEnabled() {
        notifyDataSetChanged()
    }


}