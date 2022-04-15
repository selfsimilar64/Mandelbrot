package com.selfsimilartech.fractaleye

import android.content.Context
import android.view.View
import android.widget.Button
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.selfsimilartech.fractaleye.databinding.ParamButtonBinding

class ParamSelector(
    val context: Context,
    val backgroundId : Int,
    val list: List<ParamButton>
    ) {

    init { select(list.getOrNull(0)) }

    private var selectedPos = 0

    fun getSelection() : ParamButton = list[selectedPos]

    fun isSelected(v: ParamButton) : Boolean {
        return selectedPos == list.indexOf(v)
    }

    fun select(v: ParamButton?) {
        list.getOrNull(selectedPos)?.run {
            background = null
            hideText()
        }
        selectedPos = list.indexOf(v)
        list.getOrNull(selectedPos)?.run {
            background = ResourcesCompat.getDrawable(context.resources, backgroundId, null)
            showText()
        }
    }

//    fun prev() {
//        if (list.count { it.root.isVisible } > 1) {
//            do { selectedPos = (selectedPos - 1).mod(list.size) } while (!list[selectedPos].root.isVisible)
//            list[selectedPos].root.performClick()
//        }
//    }
//
//    fun next() {
//        if (list.count { it.root.isVisible } > 1) {
//            do { selectedPos = (selectedPos + 1).mod(list.size) } while (!list[selectedPos].root.isVisible)
//            list[selectedPos].root.performClick()
//        }
//    }

}