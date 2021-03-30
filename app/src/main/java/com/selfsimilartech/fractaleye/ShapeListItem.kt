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


class ShapeListItem (

        val shape   : Shape,
        header      : ListHeader,
        layoutType  : ListLayoutType,
        goldEnabled : Boolean = false,
        isEmptyItem : Boolean = false,
        compliment  : ShapeListItem? = null

) : ListItem<Shape>(shape, header, layoutType, goldEnabled, isEmptyItem, compliment) {

    override fun equals(inObject: Any?): Boolean {
        if (inObject is ShapeListItem) {
            return shape == inObject.shape && header == inObject.header
        }
        return false
    }

    override fun hashCode(): Int {
        return shape.hashCode()
    }

    override fun getLayoutRes(): Int {
        return when (layoutType) {
            ListLayoutType.LINEAR -> {
                when {
                    shape == Shape.emptyFavorite -> R.layout.list_item_linear_empty_favorite
                    shape == Shape.emptyCustom -> R.layout.list_item_linear_empty_custom
                    shape.hasCustomId -> R.layout.shape_list_item_linear_custom
                    else -> R.layout.shape_list_item_linear_default
                }
            }
            ListLayoutType.GRID -> {
                when {
                    shape == Shape.emptyFavorite -> R.layout.list_item_linear_empty_favorite
                    shape == Shape.emptyCustom -> R.layout.list_item_linear_empty_custom
                    shape.hasCustomId -> R.layout.texture_shape_list_item_grid_custom
                    else -> R.layout.texture_shape_list_item_grid_default
                }
            }
        }
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<*>?>?): ShapeViewHolder {
        return ShapeViewHolder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<*>?>, holder: FlexibleViewHolder,
                                position: Int,
                                payloads: List<Any>) {

        with (adapter as ListAdapter<Shape>) {

            val vh = holder as ShapeViewHolder

            vh.name?.text = shape.name
            vh.favoriteButton?.isChecked = shape.isFavorite
            vh.favoriteButton?.setOnClickListener {
                holder.favoriteButton?.run { shape.isFavorite = isChecked }
                if (shape.isFavorite) {
                    addItemToFavorites(ShapeListItem(
                            this@ShapeListItem.shape,
                            ListHeader.favorites,
                            layoutType,
                            goldEnabled
                    ), this@ShapeListItem, 0)
                    Toast.makeText(holder.contentView.context, "Added to Favorites!", Toast.LENGTH_SHORT).apply {
                        setGravity(
                                Gravity.BOTTOM, 0,
                                holder.contentView.resources.getDimension(R.dimen.menuButtonHeight).toInt() + 10
                        )
                        show()
                    }
                } else {
                    val posInFavorites = getPositionInFavorites(this@ShapeListItem)
                    if (position == posInFavorites) holder.sml?.smoothCloseMenu()
                    removeItemFromFavorites(this@ShapeListItem, position == posInFavorites)
                }
            }
            if (shape.hasCustomId) {
                holder.editButton?.setOnClickListener {

                    adapter.setActivatedPosition(position)
                    onEdit(adapter, this@ShapeListItem)
                    holder.sml?.smoothCloseMenu()

                }
                holder.deleteButton?.setOnClickListener {

                    holder.sml?.smoothCloseMenu()
                    onDelete(adapter, this@ShapeListItem)

                }
                holder.image?.setImageBitmap(shape.thumbnail)
            }
            else {
                holder.image?.setImageResource(shape.thumbnailId)
            }

            holder.name?.showGradient = shape.goldFeature && !SettingsConfig.goldEnabled
            holder.image?.showGradient = shape.goldFeature && !SettingsConfig.goldEnabled
            holder.image?.scaleType = ImageView.ScaleType.CENTER_CROP

        }

    }


    class ShapeViewHolder(view: View, adapter: FlexibleAdapter<*>?) : FlexibleViewHolder(view, adapter) {

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