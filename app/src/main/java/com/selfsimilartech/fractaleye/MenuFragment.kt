package com.selfsimilartech.fractaleye

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import java.text.NumberFormat
import java.text.ParseException

interface OnCompleteListener {
    fun onComplete()
}

open class MenuFragment : Fragment() {

    lateinit var onCompleteListener : OnCompleteListener

    val nf = NumberFormat.getInstance()
    fun String.formatToDouble(showMsg: Boolean = true) : Double? {
        var d : Double? = null
        try { d = nf.parse(this)?.toDouble() }
        catch (e: ParseException) {
            if (showMsg) {
                val act = activity as MainActivity
                act.showMessage(resources.getString(R.string.msg_invalid_format))
            }
        }
        return d
    }

    lateinit var currentLayout : View
    lateinit var currentButton : Button

    val scrollListener = { layout: LinearLayout, scroll: HorizontalScrollView, leftArrow: View, rightArrow: View -> View.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->

        val minScroll = 5
        val maxScroll = layout.width - scroll.width - 5

        leftArrow.apply {
            if      (minScroll in scrollX until oldScrollX) invisible()
            else if (minScroll in oldScrollX until scrollX) show()
        }
        rightArrow.apply {
            if      (maxScroll in oldScrollX until scrollX) invisible()
            else if (maxScroll in scrollX until oldScrollX) show()
        }

    }}


    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onCompleteListener = context as OnCompleteListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement OnCompleteListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onCompleteListener.onComplete()
    }


    fun showLayout(newLayout: View) {
        currentLayout.hide()
        newLayout.show()
        currentLayout = newLayout
    }
    fun alphaButton(newButton: Button) {
        currentButton.alpha = 0.5f
        newButton.alpha = 1f
        currentButton = newButton
    }

}