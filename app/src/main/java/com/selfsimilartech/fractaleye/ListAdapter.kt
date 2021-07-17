package com.selfsimilartech.fractaleye

import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IHeader
import eu.davidea.flexibleadapter.items.ISectionable

class ListAdapter< T : Customizable >(

        items: List<ListItem<T>>?,
        val onEdit          : (a: ListAdapter<T>, ListItem<T>) -> Unit,
        val onDelete        : (a: ListAdapter<T>, ListItem<T>) -> Unit,
        val onDuplicate     : (a: ListAdapter<T>, ListItem<T>) -> Unit,
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
        return getSectionItems(ListHeader.FAVORITE)
    }
    fun getPositionInFavorites(listItem: ListItem<T>) : Int {
        getSectionItems(ListHeader.FAVORITE).forEach { item ->
            if ((item as ListItem<T>).t == listItem.t) return getGlobalPositionOf(item)
        }
        return -1
    }
    fun addItemToFavorites(item: ListItem<T>, nonFavorite: ListItem<T>, index: Int) : ListItem<T>? {
        nonFavorite.compliment = item
        item.compliment = nonFavorite
        val pos = emptyFavoritePosition()
        if (pos != -1) removeItem(pos)
        return getItem(addItemToSection(item, ListHeader.FAVORITE, index))
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
        if (getFavoriteItems().isEmpty()) addItemToSection(emptyFavorite, ListHeader.FAVORITE, 0)
    }


    fun emptyCustomPosition() : Int {
        return getGlobalPositionOf(emptyCustom)
    }
    fun getCustomItems(): MutableList<ISectionable<RecyclerView.ViewHolder, IHeader<*>>> {
        return getSectionItems(ListHeader.CUSTOM)
    }
    fun addItemToCustom(item: ListItem<T>, index: Int) : Int {
        val pos = emptyCustomPosition()
        if (pos != -1) removeItem(pos)
        return addItemToSection(item, ListHeader.CUSTOM, index)
    }
    fun removeItemFromCustom(item: ListItem<T>) {
        val itemPos = getGlobalPositionOf(item)
        // if (itemPos == activatedPos) setActivatedPosition()
        removeItem(itemPos)
        item.compliment?.run {
            removeItem(getGlobalPositionOf(this))
        }
        if (getCustomItems().isEmpty()) addItemToSection(emptyCustom, ListHeader.CUSTOM, 0)
        if (getFavoriteItems().isEmpty()) addItemToSection(emptyFavorite, ListHeader.FAVORITE, 0)
    }

    fun getDefaultItems(): MutableList<ISectionable<RecyclerView.ViewHolder, IHeader<*>>> {
        return getSectionItems(ListHeader.DEFAULT)
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
    fun setActivatedPositionNoToggle(position: Int) {
        activatedPos = position
    }

    fun onGoldEnabled() {
        notifyDataSetChanged()
    }

    fun updateItems(t: T) {
        getAllItemsOf(t).forEach { updateItem(it) }
    }

}