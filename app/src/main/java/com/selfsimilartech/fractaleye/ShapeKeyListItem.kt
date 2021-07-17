package com.selfsimilartech.fractaleye

import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import android.widget.ToggleButton
import androidx.appcompat.view.menu.MenuView
import androidx.cardview.widget.CardView
import androidx.core.view.setPadding
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder


class ShapeKeyListItem (

        val expr: Expr,
        header: ShapeKeyListHeader

) : AbstractSectionableItem<FlexibleViewHolder, ShapeKeyListHeader>(header) {

    override fun equals(other: Any?): Boolean {
        return other is ShapeKeyListItem && other.expr == expr
    }

    override fun hashCode(): Int {
        return expr.hashCode()
    }

    override fun getLayoutRes(): Int {
        return when (header.type) {
            ShapeKeyListHeader.trigonometry.type -> R.layout.shape_key_list_item_long
            else -> R.layout.shape_key_list_item
        }
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<*>?>?): FlexibleViewHolder {
        return ShapeKeyViewHolder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<*>?>, holder: FlexibleViewHolder,
                                position: Int,
                                payloads: List<Any>) {

        (holder as ShapeKeyViewHolder).image.setImageResource(expr.imgId)

    }


    inner class ShapeKeyViewHolder(view: View, adapter: FlexibleAdapter<*>?) : FlexibleViewHolder(view, adapter) {

        val image : ImageView = view.findViewById(R.id.shapeKeyImage)

    }


}