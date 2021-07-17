package com.selfsimilartech.fractaleye

import android.widget.ImageView
import androidx.appcompat.view.menu.MenuView
import androidx.core.content.res.ResourcesCompat
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder

class TextureListItem(

        val texture: Texture,
        header: ListHeader,
        layoutResId: Int,
        var disabled : Boolean = false,
        compliment: TextureListItem? = null

) : ListItem<Texture>(texture, header, layoutResId, compliment) {

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<*>?>, holder: FlexibleViewHolder, position: Int, payloads: List<Any>) {

        super.bindViewHolder(adapter, holder, position, payloads)

        holder as ItemViewHolder

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

        holder.layout?.isClickable = !disabled

    }

}