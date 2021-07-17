package com.selfsimilartech.fractaleye

import android.content.res.Resources
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder

class ShapeKeyListHeader(

        private val nameId          : Int,
        private val iconId          : Int,
        val type                    : Int,
        private val descriptionId   : Int = -1

        ) : AbstractHeaderItem<ShapeKeyListHeader.HeaderViewHolder>() {

    var name = ""
    var description = ""

    companion object {

        val variables = ShapeKeyListHeader(R.string.shapekey_header_variables, R.drawable.aspect_youtube, 0)
        val numbers = ShapeKeyListHeader(R.string.shapekey_header_numbers, R.drawable.ic_123_icon, 1)
        val basic = ShapeKeyListHeader(R.string.shapekey_header_basic, R.drawable.shape_key_header_basic, 2, R.string.header_custom_descript)
        val trigonometry = ShapeKeyListHeader(R.string.shapekey_header_trig, R.drawable.functions, 3)

        val all = arrayListOf(
                variables,
                numbers,
                basic,
                trigonometry
        )

    }

    fun initialize(res: Resources) {
        name = res.getString(nameId)
        if (descriptionId != -1) description = res.getString(descriptionId)
    }

    override fun equals(other: Any?): Boolean {
        return name.hashCode() == other.hashCode()
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun getLayoutRes(): Int {
        return R.layout.list_header
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?, holder: HeaderViewHolder, position: Int, payloads: MutableList<Any>?) {
        val marginSize = holder.contentView?.resources?.getDimension(R.dimen.shapeKeyHeaderMargin)?.toInt() ?: 0
        (holder.layout.layoutParams as GridLayoutManager.LayoutParams).updateMargins(top = marginSize, bottom = marginSize)
        holder.icon.setImageResource(iconId)
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?): HeaderViewHolder {
        return HeaderViewHolder(view, adapter)
    }

    inner class HeaderViewHolder(view: View, adapter: FlexibleAdapter<*>?) : FlexibleViewHolder(view, adapter) {

        var layout : LinearLayout = view.findViewById(R.id.headerLayout)
        var icon   : ImageView    = view.findViewById(R.id.headerIcon)
    }

}