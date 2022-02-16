package com.selfsimilartech.fractaleye

import android.content.Context
import android.view.View
import android.widget.Button
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible

class ParamSelector(
    val context: Context,
    val backgroundId : Int,
    val list: List<View>
    ) {

    init {
        applyBackground(list[0])
    }

    private var selectedPos = 0
        set (value) {
            if (field != value) {
                removeBackground(list.getOrNull(field))
                applyBackground(list.getOrNull(value))
                field = value
            }
        }

    fun getSelection() : View = list[selectedPos]

    fun isSelected(v: View) : Boolean {
        return selectedPos == list.indexOf(v)
    }

    fun select(v: View?) {
        selectedPos = list.indexOf(v)
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

    private fun applyBackground(v: View?) {
        v?.background = ResourcesCompat.getDrawable(context.resources, backgroundId, null)
    }

    private fun removeBackground(v: View?) {
        v?.background = null
    }

}