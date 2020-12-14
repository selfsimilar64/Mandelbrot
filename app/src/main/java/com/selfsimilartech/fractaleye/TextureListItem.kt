package com.selfsimilartech.fractaleye

import android.view.Gravity
import android.view.View
import android.widget.*
import android.widget.ToggleButton
import androidx.core.content.res.ResourcesCompat
import com.tubb.smrv.SwipeHorizontalMenuLayout
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder


class TextureListItem (

        val texture     : Texture,
        header          : ListHeader,
        layoutType      : ListLayoutType,
        goldEnabled     : Boolean = false,
        var disabled    : Boolean = false,
        isEmptyItem     : Boolean = false,
        compliment      : TextureListItem? = null

) : ListItem<Texture>(texture, header, layoutType, goldEnabled, isEmptyItem, compliment) {

    override fun equals(inObject: Any?): Boolean {
        if (inObject is TextureListItem) {
            return texture == inObject.texture && header == inObject.header
        }
        return false
    }

    override fun hashCode(): Int {
        return texture.hashCode()
    }

    override fun getLayoutRes(): Int {
        return when (layoutType) {
            ListLayoutType.LINEAR -> {
                when {
                    texture == Texture.emptyFavorite -> R.layout.list_item_linear_empty_favorite
                    texture == Texture.emptyCustom -> R.layout.list_item_linear_empty_custom
                    // texture.hasCustomId -> R.layout.shape_list_item_linear_custom
                    else -> R.layout.texture_list_item_linear_default
                }
            }
            ListLayoutType.GRID -> {
                when {
                    texture == Texture.emptyFavorite -> R.layout.list_item_linear_empty_favorite
                    texture == Texture.emptyCustom -> R.layout.list_item_linear_empty_custom
                    // texture.hasCustomId -> R.layout.texture_shape_list_item_grid_custom
                    else -> R.layout.texture_shape_list_item_grid_default
                }
            }
        }
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<*>?>?): TextureViewHolder {
        return TextureViewHolder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<*>?>, holder: FlexibleViewHolder,
                                position: Int,
                                payloads: List<Any>) {

        with (adapter as ListAdapter<Texture>) {

            val vh = holder as TextureViewHolder

            vh.name?.text = texture.name
            vh.favoriteButton?.isChecked = texture.isFavorite
            vh.favoriteButton?.setOnClickListener {
                holder.favoriteButton?.run { texture.isFavorite = isChecked }
                if (texture.isFavorite) {
                    addItemToFavorites(TextureListItem(
                            this@TextureListItem.texture,
                            ListHeader.favorites,
                            layoutType
                    ), this@TextureListItem, 0)
                    Toast.makeText(holder.contentView.context, "Added to Favorites!", Toast.LENGTH_SHORT).apply {
                        setGravity(
                                Gravity.BOTTOM, 0,
                                holder.contentView.resources.getDimension(R.dimen.menuButtonHeight).toInt() + 10
                        )
                        show()
                    }
                } else {
                    val posInFavorites = getPositionInFavorites(this@TextureListItem)
                    if (position == posInFavorites) holder.sml?.smoothCloseMenu()
                    removeItemFromFavorites(this@TextureListItem, position == posInFavorites)
                }
            }
//            if (texture.hasCustomId) {
//                holder.editButton?.setOnClickListener { onEdit(adapter, texture) }
//                holder.deleteButton?.setOnClickListener {
//
//                    AlertDialog.Builder(holder.contentView.context, R.style.AlertDialogCustom)
//                            .setTitle("${holder.contentView.context.resources.getString(R.string.delete)} ${texture.nameId}?")
//                            .setIcon(R.drawable.warning)
//                            .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
//                                holder.sml?.smoothCloseMenu()
//                                //removeItems(getAllPositionsOf(shape))
//                                removeItemFromCustom(this@TextureListItem)
//                                onDelete(adapter, texture)
//                            }
//                            .setNegativeButton(android.R.string.no, null)
//                            .show()
//
//                }
//                holder.image?.setImageBitmap(texture.thumbnail)
//            }
//            else {
//                holder.image?.setImageResource(texture.thumbnailId)
//            }
            holder.image?.apply{
                if (disabled) {
                    showGradient = t.goldFeature
                    setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.disabled, null))
                    scaleType = ImageView.ScaleType.FIT_XY
                }
                else {
                    showGradient = false
                    setImageBitmap(texture.thumbnail)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
            }
            holder.name?.setTextColor(holder.contentView.resources.getColor(
                    if (disabled) R.color.disabled
                    else          R.color.white,
            null))

            holder.sml?.isClickable = !disabled

            holder.name?.showGradient = texture.goldFeature && !goldEnabled
            // holder.image?.proFeature = texture.proFeature

        }

    }


    class TextureViewHolder(view: View, adapter: FlexibleAdapter<*>?) : FlexibleViewHolder(view, adapter) {

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
//        var editButton      : ImageButton?                = view.findViewById( R.id.editButton        )
//        var deleteButton    : ImageButton?                = view.findViewById( R.id.deleteButton      )



    }

}