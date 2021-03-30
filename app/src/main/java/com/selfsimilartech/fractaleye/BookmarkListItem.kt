package com.selfsimilartech.fractaleye

import android.app.AlertDialog
import android.view.Gravity
import android.view.View
import android.widget.*
import android.widget.ToggleButton
import com.tubb.smrv.SwipeHorizontalMenuLayout
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder


class BookmarkListItem (

        val fractal : Fractal,
        header      : ListHeader,
        layoutType  : ListLayoutType,
        goldEnabled : Boolean = false,
        isEmptyItem : Boolean = false,
        compliment  : BookmarkListItem? = null

) : ListItem<Fractal>(fractal, header, layoutType, goldEnabled, isEmptyItem, compliment) {

    override fun equals(inObject: Any?): Boolean {
        if (inObject is BookmarkListItem) {
            return fractal == inObject.fractal && header == inObject.header
        }
        return false
    }

    override fun hashCode(): Int {
        return fractal.hashCode()
    }

    override fun getLayoutRes(): Int {
        return when (layoutType) {
            ListLayoutType.LINEAR -> {
                when {
                    fractal == Fractal.emptyFavorite -> R.layout.list_item_linear_empty_favorite
                    fractal == Fractal.emptyCustom -> R.layout.list_item_linear_empty_custom
                    fractal.hasCustomId -> R.layout.shape_list_item_linear_custom
                    else -> R.layout.shape_list_item_linear_default
                }
            }
            ListLayoutType.GRID -> {
                when {
                    fractal == Fractal.emptyFavorite -> R.layout.list_item_linear_empty_favorite
                    fractal == Fractal.emptyCustom -> R.layout.list_item_linear_empty_custom
                    fractal.hasCustomId -> R.layout.texture_shape_list_item_grid_custom
                    else -> R.layout.texture_shape_list_item_grid_default
                }
            }
        }
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<*>?>?): PresetViewHolder {
        return PresetViewHolder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<*>?>, holder: FlexibleViewHolder,
                                position: Int,
                                payloads: List<Any>) {

        with (adapter as ListAdapter<Fractal>) {

            val vh = holder as PresetViewHolder

            vh.name?.text = fractal.name
            vh.favoriteButton?.isChecked = fractal.isFavorite
            vh.favoriteButton?.setOnClickListener {
                holder.favoriteButton?.run { fractal.isFavorite = isChecked }
                if (fractal.isFavorite) {
                    addItemToFavorites(BookmarkListItem(
                            this@BookmarkListItem.fractal,
                            ListHeader.favorites,
                            layoutType,
                            goldEnabled
                    ), this@BookmarkListItem, 0)
                    Toast.makeText(holder.contentView.context, "Added to Favorites!", Toast.LENGTH_SHORT).apply {
                        setGravity(Gravity.BOTTOM, 0, holder.contentView.resources.getDimension(R.dimen.menuButtonHeight).toInt() + 10)
                        show()
                    }
                } else {
                    val posInFavorites = getPositionInFavorites(this@BookmarkListItem)
                    if (position == posInFavorites) holder.sml?.smoothCloseMenu()
                    removeItemFromFavorites(this@BookmarkListItem, position == posInFavorites)
                }
            }
            if (fractal.hasCustomId) {
                holder.editButton?.setOnClickListener {

                    adapter.setActivatedPosition(position)
                    onEdit(adapter, this@BookmarkListItem)
                    holder.sml?.smoothCloseMenu()

                }
                holder.deleteButton?.setOnClickListener {

                    holder.sml?.smoothCloseMenu()
                    onDelete(adapter, this@BookmarkListItem)

                }
                holder.image?.setImageBitmap(fractal.thumbnail)
            }
            else {
                holder.image?.setImageResource(fractal.thumbnailId)
            }

            holder.name?.showGradient = fractal.goldFeature && !SettingsConfig.goldEnabled
            holder.image?.scaleType = ImageView.ScaleType.CENTER_CROP

        }

    }


    class PresetViewHolder(view: View, adapter: FlexibleAdapter<*>?) : FlexibleViewHolder(view, adapter) {

//        override fun scrollAnimators(animators: MutableList<Animator>, position: Int, isForward: Boolean) {
//            AnimatorHelper.alphaAnimator(animators, itemView, 0.5f)
//        }

        override fun getActivationElevation(): Float {
            return 20f
        }

        var sml             : SwipeHorizontalMenuLayout?  = view.findViewById( R.id.sml               )
        var name            : GradientTextView?           = view.findViewById( R.id.previewText       )
        var image           : GradientImageView?          = view.findViewById( R.id.previewImage      )
        var favoriteButton  : ToggleButton?               = view.findViewById( R.id.favoriteButton    )
        var editButton      : ImageButton?                = view.findViewById( R.id.editButton        )
        var deleteButton    : ImageButton?                = view.findViewById( R.id.deleteButton      )



    }

}