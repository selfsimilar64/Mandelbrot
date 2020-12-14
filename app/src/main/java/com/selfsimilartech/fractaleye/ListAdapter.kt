package com.selfsimilartech.fractaleye

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IHeader
import eu.davidea.flexibleadapter.items.ISectionable

class ListAdapter<T>(

        items: List<ListItem<T>>?,
        val onEdit          : (a: ListAdapter<T>, t: T) -> Unit,
        val onDelete        : (a: ListAdapter<T>, t: T) -> Unit,
        val emptyFavorite   : ListItem<T>,
        val emptyCustom     : ListItem<T>

) : FlexibleAdapter<ListItem<T>>(items) {

    var activatedPos = 0


    override fun addItemToSection(sectionable: ISectionable<*, out IHeader<*>>, header: IHeader<*>?, index: Int): Int {
        val newItemPos = super.addItemToSection(sectionable, header, index)
        if (newItemPos <= activatedPos) activatedPos++
        return newItemPos
    }

    override fun removeItem(position: Int) {
        if (position < activatedPos) activatedPos--
        super.removeItem(position)
    }

    override fun updateItem(item: ListItem<T>) {
        super.updateItem(item)
        item.compliment?.run { super.updateItem(this) }
    }

    fun sortSection(section: ListHeader) {
        val items = getSectionItems(section)

    }

    fun getActivatedItem() : ListItem<T>? = getItem(activatedPos)
    fun getAllItems() : List<ISectionable<RecyclerView.ViewHolder, IHeader<*>>> {
        return getFavoriteItems().plus(getCustomItems()).plus(getDefaultItems())
    }

    fun emptyFavoritePosition() : Int {
        return getGlobalPositionOf(emptyFavorite)
    }
    fun getFavoriteItems(): MutableList<ISectionable<RecyclerView.ViewHolder, IHeader<*>>> {
        return getSectionItems(ListHeader.favorites)
    }
    fun getPositionInFavorites(listItem: ListItem<T>) : Int {
        getSectionItems(ListHeader.favorites).forEach { item ->
            if ((item as ListItem<T>).t == listItem.t) return getGlobalPositionOf(item)
        }
        return -1
    }
    fun addItemToFavorites(item: ListItem<T>, nonFavorite: ListItem<T>, index: Int) : ListItem<T>? {
        nonFavorite.compliment = item
        item.compliment = nonFavorite
        val pos = emptyFavoritePosition()
        if (pos != -1) removeItem(pos)
        return getItem(addItemToSection(item, ListHeader.favorites, index))
    }
    fun removeItemFromFavorites(item: ListItem<T>, favorite: Boolean) {

        if (favorite) {  // if star was unchecked on item in favorites
            val itemPos = getGlobalPositionOf(item)
            if (itemPos == activatedPos) setActivatedPosition(getGlobalPositionOf(item.compliment))
            removeItem(itemPos)
            item.compliment?.run {
                updateItem(this)
                compliment = null
            }
        }
        else {  // if star was unchecked on default/custom item
            item.compliment?.run { removeItem(getGlobalPositionOf(this)) }
        }
        item.compliment = null
        if (getFavoriteItems().isEmpty()) addItemToSection(emptyFavorite, ListHeader.favorites, 0)
    }


    fun emptyCustomPosition() : Int {
        return getGlobalPositionOf(emptyCustom)
    }
    fun getCustomItems(): MutableList<ISectionable<RecyclerView.ViewHolder, IHeader<*>>> {
        return getSectionItems(ListHeader.custom)
    }
    fun addItemToCustom(item: ListItem<T>, index: Int) : Int {
        val pos = emptyCustomPosition()
        if (pos != -1) removeItem(pos)
        return addItemToSection(item, ListHeader.custom, index)
    }
    fun removeItemFromCustom(item: ListItem<T>) {
        val itemPos = getGlobalPositionOf(item)
        // if (itemPos == activatedPos) setActivatedPosition()
        removeItem(itemPos)
        item.compliment?.run {
            removeItem(getGlobalPositionOf(this))
        }
        if (getCustomItems().isEmpty()) addItemToSection(emptyCustom, ListHeader.custom, 0)
    }

    fun getDefaultItems(): MutableList<ISectionable<RecyclerView.ViewHolder, IHeader<*>>> {
        return getSectionItems(ListHeader.default)
    }

    fun getFirstPositionOf(t: T) : Int {
        ListHeader.all.forEach {
            getSectionItems(it).forEach { item ->
                if ((item as ListItem<T>).t == t) return getGlobalPositionOf(item)
            }
        }
        return -1
    }

    fun getAllPositionsOf(t: T) : List<Int> {
        val positions = arrayListOf<Int>()
        ListHeader.all.forEach {
            getSectionItems(it).forEach { item ->
                if ((item as ListItem<T>).t == t) positions.add(getGlobalPositionOf(item))
            }
        }
        return positions
    }

    fun getAllItemsOf(t: T) : List<ListItem<T>> {
        val items = arrayListOf<ListItem<T>>()
        ListHeader.all.forEach {
            getSectionItems(it).forEach { item ->
                if ((item as ListItem<T>).t == t) items.add(item)
            }
        }
        return items
    }
    fun setActivatedPosition(position: Int) {
        activatedPos = position
        toggleSelection(position) //Important!
    }

    fun updateLayoutType(newType: ListLayoutType) {

        ListHeader.all.forEach { header ->
            getSectionItems(header).forEach {
                Log.e("ADAPTER", "view type: ${it.itemViewType}")
                (it as ListItem<T>).layoutType = newType
            }
        }

    }

    fun onGoldEnabled() {

        ListHeader.all.forEach { header ->
            getSectionItems(header).forEach {
                (it as ListItem<T>).goldEnabled = true
            }
        }
        notifyDataSetChanged()

    }

    fun updateItems(t: T) {
        getAllItemsOf(t).forEach { updateItem(it) }
    }

}