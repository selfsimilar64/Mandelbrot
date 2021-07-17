package com.selfsimilartech.fractaleye

import android.view.View
import androidx.cardview.widget.CardView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder


class PaletteListItem (

        val palette: Palette,
        header : ListHeader,
        layoutResId : Int = -1,
        compliment : PaletteListItem? = null

) : ListItem<Palette>(palette, header, layoutResId, compliment) {

    /**
     * When an item is equals to another?
     * Write your own concept of equals, mandatory to implement or use
     * default java implementation (return this == o;) if you don't have unique IDs!
     * This will be explained in the "Item interfaces" Wiki page.
     */
    override fun equals(other: Any?): Boolean {
        if (other is PaletteListItem) {
            return palette == other.palette && header == other.header
        }
        return false
    }

    override fun hashCode(): Int {
        return palette.hashCode()
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<*>?>?): PaletteViewHolder {
        return PaletteViewHolder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<*>?>,
                                holder: FlexibleViewHolder,
                                position: Int,
                                payloads: List<Any>) {

        holder as PaletteViewHolder

        super.bindViewHolder(adapter, holder, position, payloads)
        holder.gradient?.foreground = palette.gradientDrawable

    }


    class PaletteViewHolder(view: View, adapter: FlexibleAdapter<*>?) : ItemViewHolder(view, adapter) {

        // var gradient        : CardView?        = view.findViewById( R.id.colorPreviewGradient )

    }

}