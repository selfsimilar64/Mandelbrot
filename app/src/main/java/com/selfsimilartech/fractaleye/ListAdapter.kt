package com.selfsimilartech.fractaleye

import eu.davidea.flexibleadapter.FlexibleAdapter

class ListAdapter<T> (

        val items           : ArrayList<ListItem<T>>,
        val onEdit          : (a: ListAdapter<T>, ListItem<T>) -> Unit,
        val onDelete        : (a: ListAdapter<T>, ListItem<T>) -> Unit,
        val onDuplicate     : (a: ListAdapter<T>, ListItem<T>) -> Unit,
        val emptyFavorite   : ListItem<T>,
        val emptyCustom     : ListItem<T>

) : FlexibleAdapter<ListItem<T>>(items)  where T : Customizable {

    var activatedPos = 0

    var updateActivePosition : (() -> Unit)? = null

    private var filter: ListItemType? = null

    fun filterItems(newFilter: ListItemType?) {
        filter = newFilter
        when (filter) {
            null -> updateDataSet(items)
            ListItemType.DEFAULT, ListItemType.CUSTOM -> updateDataSet(items.filter { item -> item.type == filter })
            ListItemType.FAVORITE -> updateDataSet(items.filter { item -> item.t.isFavorite })
        }
    }

    override fun addItem(item: ListItem<T>): Boolean {
        filterItems(null)
        items.add(0, item)
        val r = super.addItem(item)
        filterItems(ListItemType.CUSTOM)
        return r
    }

    override fun removeItem(position: Int) {
        val previousFilter = filter
        filterItems(null)
        items.removeAt(position)
        super.removeItem(position)
        filterItems(previousFilter)
    }


//    override fun addItem(item: ListItem<T>): Boolean {
//        onPostFilterListener = {
//            onPostFilterListener = null
//            super.addItem(item)
//            setFilter(getFilter(ListItemType::class.java))
//            filterItems()
//        }
//        setFilter(null)
//        filterItems()
//        return true
//    }

    override fun onPostFilter() {
        super.onPostFilter()
        updateActivePosition?.invoke()
    }

    override fun onPostUpdate() {
        super.onPostUpdate()
        // updateActivePosition?.invoke()
    }



    fun getActivatedItem() : ListItem<T>? = getItem(activatedPos)

    fun setActivatedPosition(t: T) {
        setActivatedPosition(currentItems.indexOfFirst { item -> item.t == t })
        currentItems.forEachIndexed { i, item ->
            if (item.t == t) setActivatedPosition(i)
        }
    }



    fun emptyCustomPosition() : Int {
        return getGlobalPositionOf(emptyCustom)
    }

    fun addItemToCustom(item: ListItem<T>, index: Int) : Int {
        addItem(index, item)
        return index
    }

    fun removeItemFromCustom(item: ListItem<T>) {
        val itemPos = getGlobalPositionOf(item)
        // if (itemPos == activatedPos) setActivatedPosition()
        removeItem(itemPos)
        if (currentItems.none { it.t.isCustom() }) addItem(0, emptyCustom)
        if (currentItems.none { it.t.isFavorite }) addItem(0, emptyFavorite)
    }

    fun getFirstPositionOf(t: T) : Int {
        currentItems.forEach { item ->
            if ((item.t as Customizable) == t) return getGlobalPositionOf(item)
        }
        return -1
    }

    fun getAllPositionsOf(t: T) : List<Int> {
        val positions = arrayListOf<Int>()
        currentItems.forEachIndexed { i, item ->
            if ((item.t as Customizable) == t) positions.add(i)
        }
        return positions
    }

    fun getAllItemsOf(t: T) : List<ListItem<T>> {
        val items = arrayListOf<ListItem<T>>()
        currentItems.forEach { item ->
            if ((item.t as Customizable) == t) items.add(item)
        }
        return items
    }

    fun setActivatedPosition(position: Int) {
        if (position == -1) {
            removeSelection(activatedPos)
        } else {
            activatedPos = position
            toggleSelection(position) //Important!
        }
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