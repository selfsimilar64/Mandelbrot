package com.selfsimilartech.fractaleye

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import java.text.NumberFormat
import java.text.ParseException

interface OnCompleteListener {
    fun onComplete()
}

open class MenuFragment : Fragment() {

    lateinit var onCompleteListener : OnCompleteListener

    val f = Fractal.default
    val sc = SettingsConfig

    lateinit var act : MainActivity
    lateinit var fsv : FractalSurfaceView

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

    val editListener = { nextEditText: EditText?, setValueAndFormat: (w: EditText) -> Unit
        -> TextView.OnEditorActionListener { editText, actionId, _ ->

        when (actionId) {
            EditorInfo.IME_ACTION_NEXT -> {
                setValueAndFormat(editText as EditText)
                editText.clearFocus()
                editText.isSelected = false
                nextEditText?.requestFocus()
            }
            EditorInfo.IME_ACTION_DONE -> {
                setValueAndFormat(editText as EditText)
                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view?.windowToken, 0)
                editText.clearFocus()
                editText.isSelected = false
                fsv.requestRender()
            }
            else -> {
                Log.d("EQUATION FRAGMENT", "some other action")
            }
        }

        editText.clearFocus()
        act.updateSystemUI()
        true

    }}

    lateinit var currentLayout : View
    lateinit var currentButton : Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        act = requireActivity() as MainActivity
        fsv = act.fsv
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onCompleteListener = context as OnCompleteListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement OnCompleteListener")
        }
    }

    override fun onResume() {
        super.onResume()
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

    fun subMenuButtonListener(layout: View, button: Button, uiLayoutHeight: UiLayoutHeight = UiLayoutHeight.SHORT) : View.OnClickListener {
        return View.OnClickListener {
            act.uiSetHeight(uiLayoutHeight)
            showLayout(layout)
            alphaButton(button)
        }
    }

    open fun updateLayout() {}

}