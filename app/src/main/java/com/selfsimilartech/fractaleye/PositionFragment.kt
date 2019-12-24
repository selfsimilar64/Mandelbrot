package com.selfsimilartech.fractaleye

import android.content.Context
import android.support.v4.app.Fragment
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.TabLayout
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*


class PositionFragment : Fragment() {

    // Store instance variables
    private lateinit var f: Fractal
    private lateinit var fsv: FractalSurfaceView

    // newInstance constructor for creating fragment with arguments
    fun passArguments(f: Fractal, fsv: FractalSurfaceView) {
        this.f = f
        this.fsv = fsv
    }

    fun String.formatToDouble() : Double? {
        var d : Double? = null
        try { d = this.toDouble() }
        catch (e: NumberFormatException) {
            val act = if (activity is MainActivity) activity as MainActivity else null
            act?.showMessage("Invalid number format")
        }
        return d
    }


    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.position_fragment, container, false)
        val act = if (activity is MainActivity) activity as MainActivity else null

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
                    val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    editText.clearFocus()
                    editText.isSelected = false
                }
                else -> {
                    Log.d("EQUATION FRAGMENT", "some other action")
                }
            }

            fsv.requestRender()
            editText.clearFocus()
            // act?.onWindowFocusChanged(true)
            true

        }}


        val xCoordEdit = v.findViewById<EditText>(R.id.xCoordEdit)
        // val xLock = v.findViewById<ToggleButton>(R.id.xLock)
        val yCoordEdit = v.findViewById<EditText>(R.id.yCoordEdit)
        // val yLock = v.findViewById<ToggleButton>(R.id.yLock)
        xCoordEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            f.position.x = w.text.toString().formatToDouble() ?: f.position.x
            w.text = "%.17f".format(f.position.x)
            fsv.r.renderToTex = true
        })
        yCoordEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            f.position.y = w.text.toString().formatToDouble() ?: f.position.y
            w.text = "%.17f".format(f.position.y)
            fsv.r.renderToTex = true
        })
        // xLock.setOnClickListener {
        //     f.position.xLocked = xLock.isChecked
        // }
        // yLock.setOnClickListener {
        //     f.position.yLocked = yLock.isChecked
        // }

        val scaleSignificandEdit = v.findViewById<EditText>(R.id.scaleSignificandEdit)
        val scaleExponentEdit = v.findViewById<EditText>(R.id.scaleExponentEdit)
        val scaleLock = v.findViewById<ToggleButton>(R.id.scaleLock)
        scaleSignificandEdit.setOnEditorActionListener(
                editListener(scaleExponentEdit) { w: TextView ->

                    val prevScale = f.position.scale

                    f.position.scale = "${w.text}e${scaleExponentEdit.text}".formatToDouble() ?: f.position.scale
                    val scaleStrings = "%e".format(f.position.scale).split("e")
                    w.text = "%.5f".format(scaleStrings[0].toFloat())

                })
        scaleExponentEdit.setOnEditorActionListener(
                editListener(null) { w: TextView ->

                    val prevScale = f.position.scale

                    f.position.scale = "${scaleSignificandEdit.text}e${w.text}".formatToDouble() ?: f.position.scale
                    val scaleStrings = "%e".format(f.position.scale).split("e")
                    w.text = "%d".format(scaleStrings[1].toInt())

                    fsv.checkThresholdCross(prevScale)
                    fsv.r.renderToTex = true

                })
        scaleLock.setOnClickListener {
            f.position.scaleLocked = scaleLock.isChecked
        }

        val rotationEdit = v.findViewById<EditText>(R.id.rotationEdit)
        val rotationLock = v.findViewById<ToggleButton>(R.id.rotationLock)
        rotationEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            f.position.rotation = w.text.toString().formatToDouble()?.inRadians() ?: f.position.rotation
            w.text = "%.0f".format(f.position.rotation * 180.0 / Math.PI)
            fsv.r.renderToTex = true
        })
        rotationLock.setOnClickListener {
            f.position.rotationLocked = rotationLock.isChecked
        }


        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val act = if (activity is MainActivity) activity as MainActivity else null
        act?.updatePositionEditTexts()
        super.onViewCreated(view, savedInstanceState)
    }

}