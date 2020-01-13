package com.selfsimilartech.fractaleye

import android.content.Context
import android.support.v4.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlinx.android.synthetic.main.position_fragment.*
import java.text.NumberFormat
import java.text.ParseException
import java.util.*
import kotlin.math.roundToInt


class PositionFragment : Fragment() {

    // Store instance variables
    private lateinit var f: Fractal
    private lateinit var fsv: FractalSurfaceView
    private val nf = NumberFormat.getInstance()

    // newInstance constructor for creating fragment with arguments
    fun passArguments(f: Fractal, fsv: FractalSurfaceView) {
        this.f = f
        this.fsv = fsv
    }

    private fun String.formatToDouble(showMsg: Boolean = true) : Double? {
        var d : Double? = null
        try { d = nf.parse(this)?.toDouble() }
        catch (e: ParseException) {
            if (showMsg) {
                val act = if (activity is MainActivity) activity as MainActivity else null
                act?.showMessage(resources.getString(R.string.msg_invalid_format))
            }
        }
        return d
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val act = if (activity is MainActivity) activity as MainActivity else null
        if (!this::f.isInitialized) f = act!!.f
        if (!this::fsv.isInitialized) fsv = act!!.fsv
        super.onCreate(savedInstanceState)
    }


    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.position_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

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
                    fsv.requestRender()
                }
                else -> {
                    Log.d("EQUATION FRAGMENT", "some other action")
                }
            }

            editText.clearFocus()
            // act?.onWindowFocusChanged(true)
            true

        }}

        xEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()
            if (result != null) {
                f.position.x = result
                fsv.r.renderToTex = true
            }
            w.text = "%.17f".format(f.position.x)

        })
        yEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()
            if (result != null) {
                f.position.y = result
                fsv.r.renderToTex = true
            }
            w.text = "%.17f".format(f.position.y)
        })
        // xLock.setOnClickListener {
        //     f.position.xLocked = xLock.isChecked
        // }
        // yLock.setOnClickListener {
        //     f.position.yLocked = yLock.isChecked
        // }

        scaleSignificandEdit.setOnEditorActionListener(
                editListener(scaleExponentEdit) { w: TextView ->
                    val result1 = w.text.toString().formatToDouble(false)
                    val result2 = scaleExponentEdit.text.toString().formatToDouble(false)
                    val result3 = "${w.text}e${scaleExponentEdit.text}".formatToDouble(false)
                    if (result1 != null && result2 != null && result3 != null) {
                        f.position.scale = result3
                        fsv.r.renderToTex = true
                    }
                    else {
                        act?.showMessage(resources.getString(R.string.msg_invalid_format))
                    }
                    val scaleStrings = "%e".format(Locale.US, f.position.scale).split("e")
                    w.text = "%.5f".format(scaleStrings[0].toFloat())
                    scaleExponentEdit.setText("%d".format(scaleStrings[1].toInt()))
                })
        scaleExponentEdit.setOnEditorActionListener(
                editListener(null) { w: TextView ->

                    val prevScale = f.position.scale

                    val result1 = scaleSignificandEdit.text.toString().formatToDouble(false)
                    val result2 = w.text.toString().formatToDouble(false)
                    val result3 = "${scaleSignificandEdit.text}e${w.text}".formatToDouble(false)
                    if (result1 != null && result2 != null && result3 != null) {
                        f.position.scale = result3
                        fsv.r.renderToTex = true
                    }
                    else {
                        act?.showMessage(resources.getString(R.string.msg_invalid_format))
                    }
                    val scaleStrings = "%e".format(Locale.US, f.position.scale).split("e")
                    scaleSignificandEdit.setText("%.5f".format(scaleStrings[0].toFloat()))
                    w.text = "%d".format(scaleStrings[1].toInt())

                    fsv.checkThresholdCross(prevScale)

                })
        scaleLock.setOnClickListener {
            f.position.scaleLocked = scaleLock.isChecked
        }

        rotationEdit.setOnEditorActionListener(
                editListener(null) { w: TextView ->
                    val result = w.text.toString().formatToDouble()?.inRadians()
                    if (result != null) {
                        f.position.rotation = result
                        fsv.r.renderToTex = true
                    }
                    w.text = "%d".format(f.position.rotation.inDegrees().roundToInt())
                })
        rotationLock.setOnClickListener {
            f.position.rotationLocked = rotationLock.isChecked
        }

        act?.updatePositionEditTexts()
        super.onViewCreated(v, savedInstanceState)

    }

}