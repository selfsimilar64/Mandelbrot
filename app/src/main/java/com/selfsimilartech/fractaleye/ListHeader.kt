package com.selfsimilartech.fractaleye

import android.content.res.Resources
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder

class ListHeader(

        private val nameId          : Int,
        val type                    : Int,
        private val descriptionId   : Int = -1

        ) : AbstractHeaderItem<ListHeader.HeaderViewHolder>() {

    var name = ""
    var description = ""

    companion object {

        val FAVORITE = ListHeader(R.string.header_favorites, 0)
        val CUSTOM = ListHeader(R.string.header_custom, 1, R.string.header_custom_descript)
        val DEFAULT = ListHeader(R.string.header_default, 2)

        val all = arrayListOf(
                FAVORITE,
                CUSTOM,
                DEFAULT
        )

    }

    fun initialize(res: Resources) {
        name = res.getString(nameId)
        if (descriptionId != -1) description = res.getString(descriptionId)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?, holder: HeaderViewHolder, position: Int, payloads: MutableList<Any>?) {
        holder.icon.setImageResource(when (type) {
            0 -> R.drawable.starred_no_color
            1 -> R.drawable.custom
            2 -> R.drawable.list_view
            else -> R.drawable.cancel
        })
    }

    override fun equals(other: Any?): Boolean {
        return name.hashCode() == other.hashCode()
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?): ListHeader.HeaderViewHolder {
        return HeaderViewHolder(view, adapter)
    }

    override fun getLayoutRes(): Int {
        return R.layout.list_header
    }

    inner class HeaderViewHolder(view: View, adapter: FlexibleAdapter<*>?) : FlexibleViewHolder(view, adapter) {

        var icon        : ImageView = view.findViewById(R.id.headerIcon)

    }

}