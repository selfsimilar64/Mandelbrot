package com.selfsimilartech.fractaleye

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import java.text.NumberFormat
import java.text.ParseException

interface OnCompleteListener {
    fun onComplete()
}

abstract class MenuFragment : Fragment() {

    lateinit var onCompleteListener : OnCompleteListener

    val f = Fractal.default
    val sc = Settings

    lateinit var act : MainActivity
    lateinit var fsv : FractalSurfaceView

    fun String.formatToDouble(showMsg: Boolean = true) : Double? {
        val nf = NumberFormat.getInstance()
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
            else -> {}
        }

        editText.clearFocus()
        true

    }}

    lateinit var layout : View
    lateinit var button : Button


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // retainInstance = true
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

    override fun onStart() {
        super.onStart()
        onCompleteListener.onComplete()
    }

    override fun onResume() {
        super.onResume()
        Log.e("MENU", "$this -- fragment resumed")
    }


    fun setCurrentLayout(newLayout: View?) {
        layout.hide()
        newLayout?.show()
        newLayout?.let { layout = it }
    }

    fun setCurrentButton(newButton: Button?) {
        button.alpha = 0.5f
        newButton?.alpha = 1f
        newButton?.let { button = it }
    }

    fun subMenuButtonListener(layout: View, button: Button, uiLayoutHeight: UiLayoutHeight = UiLayoutHeight.SHORT) : View.OnClickListener {
        return View.OnClickListener {
            setCurrentLayout(layout)
            setCurrentButton(button)
        }
    }

    abstract fun updateLayout()

    abstract fun onGoldEnabled()

    abstract fun updateValues()

}