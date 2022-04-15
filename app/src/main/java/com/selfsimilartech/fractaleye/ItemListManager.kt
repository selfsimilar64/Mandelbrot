package com.selfsimilartech.fractaleye

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.selfsimilartech.fractaleye.databinding.ListLayoutBinding

class ItemListManager<T>(

    val b             : ListLayoutBinding,
    buttonSelector    : ViewSelector,
    defaultItems      : ArrayList<T>,
    customItems       : ArrayList<T>,
    context           : Context

) where T: Customizable {

    private var type: ListItemType = ListItemType.DEFAULT

    private val selectionEnabled = defaultItems[0] !is Fractal

    var currentList: RecyclerView = b.defaultList

    private var newFavorites = mutableListOf<T>()
    private var newCustomItems = mutableListOf<T>()

    private fun updateIndicators() {
        b.favoritesListButtonIndicator.run {
            if (newFavorites.isEmpty()) hide() else {
                show()
                text = newFavorites.size.toString()
            }
        }
        b.customListButtonIndicator.run {
            if (newCustomItems.isEmpty()) hide() else {
                show()
                text = newCustomItems.size.toString()
            }
        }
    }


    init {

        val favoriteItems = ArrayList(customItems.plus(defaultItems).filter { item -> item.isFavorite })

        b.defaultList.run {
            adapter = NewListAdapter(defaultItems, this, "DEFAULT", selectionEnabled)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        b.customList.run {
            adapter = NewListAdapter(customItems, this, "CUSTOM", selectionEnabled)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        b.favoritesList.run {
            adapter = NewListAdapter(favoriteItems, this, "FAVORITES", selectionEnabled)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        b.defaultListButton.setOnClickListener {
            buttonSelector.select(b.defaultListButton)
            setType(ListItemType.DEFAULT)
        }
        b.customListButton.setOnClickListener {
            buttonSelector.select(b.customListButton)
            setType(ListItemType.CUSTOM)
        }
        b.favoritesListButton.setOnClickListener {
            buttonSelector.select(b.favoritesListButton)
            setType(ListItemType.FAVORITE)
        }

    }


    fun setType(newType: ListItemType) {
        type = newType
        when (type) {
            ListItemType.DEFAULT -> {

                b.emptyCustomLabel.hide()
                b.emptyFavoritesLabel.hide()

                b.defaultList.show()
                b.customList.hide()
                b.favoritesList.hide()

                currentList = b.defaultList

            }
            ListItemType.CUSTOM -> {

                if (customAdapter().itemCount == 0) b.emptyCustomLabel.show() else b.emptyCustomLabel.hide()
                b.emptyFavoritesLabel.hide()

                b.customList.show()
                b.defaultList.hide()
                b.favoritesList.hide()

                currentList = b.customList
                if (newCustomItems.isNotEmpty()) {
                    newCustomItems.removeAll { true }
                    updateIndicators()
                    b.customList.post { b.customList.scrollToPosition(0) }
                }

            }
            ListItemType.FAVORITE -> {

                if (favoritesAdapter().itemCount == 0) b.emptyFavoritesLabel.show() else b.emptyFavoritesLabel.hide()
                b.emptyCustomLabel.hide()

                b.favoritesList.show()
                b.defaultList.hide()
                b.customList.hide()

                currentList = b.favoritesList
                if (newFavorites.isNotEmpty()) {
                    newFavorites.removeAll { true }
                    updateIndicators()
                    b.favoritesList.post { b.favoritesList.scrollToPosition(0) }
                }

            }
        }
    }

    fun updateDataset(defaultItems: ArrayList<T>, customItems: ArrayList<T>) {

        val favoriteItems = ArrayList(customItems.plus(defaultItems).filter { item -> item.isFavorite })

        defaultAdapter()   .updateDataset(defaultItems)
        customAdapter()    .updateDataset(customItems)
        favoritesAdapter() .updateDataset(favoriteItems)

    }

    fun setOnItemActionListener(l: NewListAdapter.OnItemActionListener<T>) {
        defaultAdapter()   .setOnItemActionListener(l)
        customAdapter()    .setOnItemActionListener(l)
        favoritesAdapter() .setOnItemActionListener(l)
    }

    fun addNewItem(t: T) {
        customAdapter().run {
            addItem(t)
            if (itemCount == 1) b.emptyCustomLabel.hide()
        }
        if (selectionEnabled) setSelection(t, fromUser = false)
        if (type != ListItemType.CUSTOM) {
            newCustomItems.add(t)
            updateIndicators()
        }
    }

    fun updateItemFromAdd(t: T) {
        when (type) {
            ListItemType.DEFAULT -> {

            }
            ListItemType.CUSTOM -> {
                customAdapter().updateItem(t)
                b.customList.post { b.customList.scrollToPosition(0) }
                if (t.isFavorite) favoritesAdapter().updateItem(t)
            }
            ListItemType.FAVORITE -> {
                favoritesAdapter().updateItem(t)
                customAdapter().updateItem(t)
            }
        }
    }

    fun updateItemFromEdit(t: T) {
        when (type) {
            ListItemType.DEFAULT -> {
                defaultAdapter().updateItem(t)
                if (t.isFavorite) favoritesAdapter().updateItem(t)
            }
            ListItemType.CUSTOM -> {
                customAdapter().updateItem(t)
                if (t.isFavorite) favoritesAdapter().updateItem(t)
            }
            ListItemType.FAVORITE -> {
                favoritesAdapter().updateItem(t)
                customAdapter().updateItem(t)
            }
        }
    }

    fun updateCurrentItems() {
        when (type) {
            ListItemType.DEFAULT  -> defaultAdapter()   .updateAllItems()
            ListItemType.CUSTOM   -> customAdapter()    .updateAllItems()
            ListItemType.FAVORITE -> favoritesAdapter() .updateAllItems()
        }
    }

    fun updateAllItems() {
        defaultAdapter().updateAllItems()
        customAdapter().updateAllItems()
        favoritesAdapter().updateAllItems()
    }

    fun deleteItem(t: T) {
        when (type) {
            ListItemType.CUSTOM -> {
                customAdapter().run {
                    removeItem(t)
                    if (itemCount == 0) b.emptyCustomLabel.show()
                    if (selectionEnabled) {
                        val newItem = getSelectedItem()
                        if (newItem != null) {
                            setSelection(newItem, fromUser = false)
                        }
                    }
                }
                if (t.isFavorite) {
                    favoritesAdapter().removeItem(t)
                    newFavorites.remove(t)
                    updateIndicators()
                }
            }
            ListItemType.FAVORITE -> {
                favoritesAdapter().removeItem(t)
                customAdapter().removeItem(t)
            }
            else -> {}
        }
    }

    fun toggleFavorite(t: T, isSelected: Boolean) {
        when (type) {
            ListItemType.DEFAULT, ListItemType.CUSTOM -> {
                favoritesAdapter().run {
                    if (t.isFavorite) {
                        addItem(t)
                        newFavorites.add(t)
                        updateIndicators()
                        if (selectionEnabled && isSelected) setSelectedItem(t)
                    } else {
                        removeItem(t)
                        newFavorites.remove(t)
                        updateIndicators()
                    }
                }
            }
            ListItemType.FAVORITE -> {
                favoritesAdapter().run {
                    removeItem(t)
                    if (itemCount == 0) b.emptyFavoritesLabel.show()
                }
                if (t.isCustom()) customAdapter().updateItem(t) else defaultAdapter().updateItem(t)
            }
        }
    }

    fun getSelectedItem() : T? {
        return defaultAdapter().getSelectedItem() ?: customAdapter().getSelectedItem()
    }

    fun setSelection(t: T, fromUser: Boolean = true) {
        if (selectionEnabled) {
            when (type) {
                ListItemType.DEFAULT -> {
                    if (!fromUser) defaultAdapter().setSelectedItem(t)
                    customAdapter().clearSelection()
                    if (t.isFavorite) favoritesAdapter().setSelectedItem(t)
                }
                ListItemType.CUSTOM -> {
                    if (!fromUser) customAdapter().setSelectedItem(t)
                    defaultAdapter().clearSelection()
                    if (t.isFavorite) favoritesAdapter().setSelectedItem(t)
                }
                ListItemType.FAVORITE -> {
                    if (!fromUser) favoritesAdapter().setSelectedItem(t)
                    Log.d("MANAGER", "id: {${(t as Palette).id}}")
                    if (t.isCustom()) {
                        defaultAdapter().clearSelection()
                        customAdapter().setSelectedItem(t)
                    } else {
                        customAdapter().clearSelection()
                        defaultAdapter().setSelectedItem(t)
                    }
                }
            }
        }
    }



    private fun defaultAdapter()   : NewListAdapter<T> = b.defaultList   .adapter as NewListAdapter<T>
    private fun customAdapter()    : NewListAdapter<T> = b.customList    .adapter as NewListAdapter<T>
    private fun favoritesAdapter() : NewListAdapter<T> = b.favoritesList .adapter as NewListAdapter<T>

}