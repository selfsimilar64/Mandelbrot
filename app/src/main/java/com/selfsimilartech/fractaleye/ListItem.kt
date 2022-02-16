package com.selfsimilartech.fractaleye

import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import android.widget.ToggleButton
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFilterable
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import java.io.Serializable


open class ListItem<T> (

        val t: T,
        val type: ListItemType,
        val layoutResId : Int = -1

) : AbstractFlexibleItem<ListItem.ItemViewHolder>(), IFilterable<ListItemType> where T : Customizable {



    override fun equals(other: Any?): Boolean {
        if (other is ListItem<*>) {
            return t == other.t && type == other.type
        }
        return false
    }

    override fun hashCode(): Int {
        return t.hashCode()
    }

    override fun getLayoutRes(): Int { return layoutResId }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<*>?>?): ItemViewHolder? {
        return ItemViewHolder(view, adapter)
    }

    override fun bindViewHolder(

        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?,
        holder: ItemViewHolder?,
        position: Int,
        payloads: MutableList<Any>?

    ) {

        holder as ItemViewHolder
        adapter as ListAdapter<T>

        holder.options?.hide()
        holder.layout?.setOnLongClickListener {
            holder.options?.show()
            true
        }
        holder.options?.setOnClickListener {
            holder.options?.hide()
        }
        if (t.isCustom()) {
            holder.editButton?.show()
            holder.deleteButton?.show()
        } else {
            holder.editButton?.hide()
            holder.deleteButton?.hide()
        }


        holder.name?.text = t.name
        holder.name?.showGradient = t.goldFeature && !SettingsConfig.goldEnabled
        when {
            t is Shape && !t.isCustom() -> holder.image?.setImageResource(t.thumbnailId)
            t is Texture                -> holder.image?.setImageBitmap(t.thumbnail)
            else                        -> holder.image?.setImageBitmap(t.thumbnail)
        }
        holder.image?.scaleType = ImageView.ScaleType.CENTER_CROP

        holder.favoriteButton?.run {
            uncheckedImageId = R.drawable.unstarred
            checkedImageId = R.drawable.starred
            mode = ImageToggleButton.Mode.IMAGE
            isChecked = t.isFavorite
        }
        holder.favoriteButton?.setOnClickListener {

            holder.favoriteButton?.run { t.isFavorite = isChecked }
            if (t.isFavorite) {
                // adapter.addItemToFavorites(ListItem(t, ListItemType.FAVORITE, layoutResId, this), this, 0)
                Toast.makeText(holder.contentView.context, "Added to Favorites!", Toast.LENGTH_SHORT).apply {
                    setGravity(Gravity.CENTER, 0, 0)
                    show()
                }
            } else {
                // adapter.removeItemFromFavorites(this, type == ListItemType.FAVORITE)
            }
            holder.options?.hide()

        }
        if (t.isCustom()) {

            holder.editButton?.setOnClickListener {

                adapter.setActivatedPosition(position)
                holder.options?.hide()
                adapter.onEdit(adapter, this)

            }
            holder.deleteButton?.setOnClickListener {

                holder.options?.hide()
                adapter.onDelete(adapter, this)

            }

        }
        if (t is Fractal || t is Texture || (t is Shape && t.latex == "")) holder.copyButton?.hide()
        else  {
            holder.copyButton?.show()
            holder.copyButton?.showGradient = !SettingsConfig.goldEnabled
            holder.copyButton?.setOnClickListener {
                holder.options?.hide()
                adapter.onDuplicate(adapter, this)
            }
        }

        if (t is Palette) holder.gradient?.foreground = t.gradientDrawable

    }


    open class ItemViewHolder(view: View, adapter: FlexibleAdapter<*>?) : FlexibleViewHolder(view, adapter) {

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

    override fun filter(constraint: ListItemType?) : Boolean {
        return when (constraint) {
            ListItemType.DEFAULT, ListItemType.CUSTOM -> type == constraint
            ListItemType.FAVORITE -> t.isFavorite
            else -> false
        }
    }


}