package com.selfsimilartech.fractaleye

import android.content.Context
import androidx.fragment.app.Fragment
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

    private val nf = NumberFormat.getInstance()

    private fun String.formatToDouble(showMsg: Boolean = true) : Double? {
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


    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.position_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        val act = activity as MainActivity
        val f = act.f
        val fsv = act.fsv

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
            act.updateSystemUI()
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
                        if (result3.isInfinite() || result3.isNaN()) {
                            act.showMessage(resources.getString(R.string.msg_num_out_range))
                        }
                        else {
                            f.position.scale = result3
                            fsv.r.renderToTex = true
                        }
                    }
                    else {
                        act.showMessage(resources.getString(R.string.msg_invalid_format))
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
                        if (result3.isInfinite() || result3.isNaN()) {
                            act.showMessage(resources.getString(R.string.msg_num_out_range))
                        }
                        else {
                            f.position.scale = result3
                            fsv.r.renderToTex = true
                        }
                    }
                    else {
                        act.showMessage(resources.getString(R.string.msg_invalid_format))
                    }
                    val scaleStrings = "%e".format(Locale.US, f.position.scale).split("e")
                    scaleSignificandEdit.setText("%.5f".format(scaleStrings[0].toFloat()))
                    w.text = "%d".format(scaleStrings[1].toInt())

                    fsv.r.checkThresholdCross(prevScale)

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


        positionResetButton.setOnClickListener {

            f.position.reset()
            act.updatePositionEditTexts()
            fsv.r.renderToTex = true
            fsv.requestRender()

        }


        act.updatePositionEditTexts()
        super.onViewCreated(v, savedInstanceState)

    }

}