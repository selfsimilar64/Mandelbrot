package com.selfsimilartech.fractaleye

import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import java.text.NumberFormat
import java.text.ParseException

open class MenuFragment : Fragment() {

    val nf = NumberFormat.getInstance()
    fun String.formatToDouble(showMsg: Boolean = true) : Double? {
        var d : Double? = null
        try { d = nf.parse(this)?.toDouble() }
        catch (e: ParseException) {
            if (showMsg) {
                val act =activity as MainActivity
                act.showMessage(resources.getString(R.string.msg_invalid_format))
            }
        }
        return d
    }

    lateinit var currentLayout : View
    lateinit var currentButton : Button

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