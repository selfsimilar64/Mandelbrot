package com.selfsimilartech.fractaleye

import android.content.Context
import android.view.View
import android.widget.Button
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible

class ViewSelector(
    val context: Context,
    val backgroundId : Int,
    val list: List<View>,
    private var selectedPos: Int = 0
) {

    init {
        select(list[selectedPos])
    }

    fun getSelection() : View = list[selectedPos]

    fun isSelected(v: View) : Boolean {
        return selectedPos == list.indexOf(v)
    }

    fun select(v: View?) {
        list.getOrNull(selectedPos)?.isActivated = false
        selectedPos = list.indexOf(v)
        list.getOrNull(selectedPos)?.isActivated = true
    }

    fun prev() {
        if (list.count { it.isVisible } > 1) {
            do { selectedPos = (selectedPos - 1).mod(list.size) } while (!list[selectedPos].isVisible)
            list[selectedPos].performClick()
        }
    }

    fun next() {
        if (list.count { it.isVisible } > 1) {
            do { selectedPos = (selectedPos + 1).mod(list.size) } while (!list[selectedPos].isVisible)
            list[selectedPos].performClick()
        }
    }

}