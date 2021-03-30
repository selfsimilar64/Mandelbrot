package com.selfsimilartech.fractaleye

import android.app.AlertDialog
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import android.widget.ToggleButton
import androidx.cardview.widget.CardView
import com.tubb.smrv.SwipeHorizontalMenuLayout
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder


class PaletteListItem (

        val palette: Palette,
        header: ListHeader,
        layoutType: ListLayoutType,
        isEmptyItem : Boolean = false,
        compliment : PaletteListItem? = null

) : ListItem<Palette>(palette, header, layoutType, false, isEmptyItem, compliment) {

    /**
     * When an item is equals to another?
     * Write your own concept of equals, mandatory to implement or use
     * default java implementation (return this == o;) if you don't have unique IDs!
     * This will be explained in the "Item interfaces" Wiki page.
     */
    override fun equals(inObject: Any?): Boolean {
        if (inObject is PaletteListItem) {
            return palette == inObject.palette && header == inObject.header
        }
        return false
    }

    /**
     * You should implement also this method if equals() is implemented.
     * This method, if implemented, has several implications that Adapter handles better:
     * - The Hash, increases performance in big list during Update & Filter operations.
     * - You might want to activate stable ids via Constructor for RV, if your id
     * is unique (read more in the wiki page: "Setting Up Advanced") you will benefit
     * of the animations also if notifyDataSetChanged() is invoked.
     */
    override fun hashCode(): Int {
        return palette.hashCode()
    }

    /**
     * For the item type we need an int value: the layoutResID is sufficient.
     */
    override fun getLayoutRes(): Int {
        return when (layoutType) {
            ListLayoutType.LINEAR -> {
                when {
                    palette == Palette.emptyFavorite -> R.layout.list_item_linear_empty_favorite
                    palette == Palette.emptyCustom -> R.layout.list_item_linear_empty_custom
                    palette.hasCustomId -> R.layout.palette_list_item_linear_custom
                    else -> R.layout.palette_list_item_linear_default
                }
            }
            ListLayoutType.GRID -> {
                when {
                    palette == Palette.emptyFavorite -> R.layout.list_item_linear_empty_favorite
                    palette == Palette.emptyCustom -> R.layout.list_item_linear_empty_custom
                    palette.hasCustomId -> R.layout.palette_list_item_grid_custom
                    else -> R.layout.palette_list_item_grid_default
                }
            }
        }
    }

    /**
     * Delegates the creation of the ViewHolder to the user (AutoMap).
     * The inflated view is already provided as well as the Adapter.
     */
    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<*>?>?): PaletteViewHolder {
        return PaletteViewHolder(view, adapter)
    }

    /**
     * The Adapter and the Payload are provided to perform and get more specific
     * information.
     */
    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<*>?>, holder: FlexibleViewHolder,
                                position: Int,
                                payloads: List<Any>) {

        with (adapter as ListAdapter<Palette>) {

            val vh = holder as PaletteViewHolder

            vh.previewGradient?.foreground = palette.gradientDrawable
            vh.previewText?.text = palette.name
            vh.starToggle?.isChecked = palette.isFavorite
            vh.starToggle?.setOnClickListener {
                holder.starToggle?.run { palette.isFavorite = isChecked }
                if (palette.isFavorite) {
                    addItemToFavorites(PaletteListItem(
                            this@PaletteListItem.palette,
                            ListHeader.favorites,
                            layoutType,
                            goldEnabled
                    ), this@PaletteListItem, 0)
                    Toast.makeText(holder.contentView.context, "Added to Favorites!", Toast.LENGTH_SHORT).apply {
                        setGravity(Gravity.CENTER, 0, 0)
                        show()
                    }
                } else {
                    val posInFavorites = getPositionInFavorites(this@PaletteListItem)
                    if (position == posInFavorites) holder.sml?.smoothCloseMenu()
                    removeItemFromFavorites(this@PaletteListItem, position == posInFavorites)
                }
            }
            if (palette.hasCustomId) {
                holder.editButton?.setOnClickListener {

                    adapter.setActivatedPosition(position)
                    holder.sml?.smoothCloseMenu()
                    onEdit(adapter, this@PaletteListItem)

                }
                holder.deleteButton?.setOnClickListener {

                    holder.sml?.smoothCloseMenu()
                    onDelete(adapter, this@PaletteListItem)

                }
            }
            if (layoutType == ListLayoutType.GRID) {
                holder.previewImage?.setImageBitmap(palette.thumbnail)
                holder.previewImage?.scaleType = ImageView.ScaleType.CENTER_CROP
            }

        }

    }


    /**
     * The ViewHolder used by this item.
     * Extending from FlexibleViewHolder is recommended especially when you will use
     * more advanced features.
     */
    class PaletteViewHolder(view: View, adapter: FlexibleAdapter<*>?) : FlexibleViewHolder(view, adapter) {

//        override fun scrollAnimators(animators: MutableList<Animator>, position: Int, isForward: Boolean) {
//            AnimatorHelper.alphaAnimator(animators, itemView, 0.5f)
//        }

        override fun getActivationElevation(): Float {
            return 20f
        }

        var sml              : SwipeHorizontalMenuLayout?  = view.findViewById( R.id.sml                  )
        var previewGradient  : CardView?                   = view.findViewById( R.id.colorPreviewGradient )
        var previewText      : TextView?                   = view.findViewById( R.id.colorPreviewText     )
        var previewImage     : ImageView?                  = view.findViewById( R.id.colorPreviewImage    )
        var starToggle       : ToggleButton?               = view.findViewById( R.id.favoriteButton       )
        var editButton       : ImageButton?                = view.findViewById( R.id.editButton           )
        var deleteButton     : ImageButton?                = view.findViewById( R.id.deleteButton         )


    }

}