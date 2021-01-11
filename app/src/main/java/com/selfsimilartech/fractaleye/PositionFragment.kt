package com.selfsimilartech.fractaleye

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlinx.android.synthetic.main.position_fragment.*
import java.util.*
import kotlin.math.pow


class PositionFragment : MenuFragment() {

    private var rotationSeekBarListener : SeekBar.OnSeekBarChangeListener? = null


    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.position_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        val act = activity as MainActivity
        val f = act.f
        val fsv = act.fsv
        val sc = act.sc

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

        xEdit.setOnEditorActionListener(editListener(yEdit) { w: TextView ->
            val result = w.text.toString().formatToDouble()
            if (result != null) {
                f.position.x = result
                if (fsv.r.isRendering) fsv.r.interruptRender = true
                fsv.r.renderToTex = true
            }
            w.text = "%.17f".format(f.position.x)

        })
        yEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()
            if (result != null) {
                f.position.y = result
                if (fsv.r.isRendering) fsv.r.interruptRender = true
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
                            f.position.zoom = result3
                            fsv.r.renderToTex = true
                            if (fsv.r.isRendering) fsv.r.interruptRender = true
                        }
                    }
                    else {
                        act.showMessage(resources.getString(R.string.msg_invalid_format))
                    }
                    val scaleStrings = "%e".format(Locale.US, f.position.zoom).split("e")
                    w.text = "%.5f".format(scaleStrings[0].toFloat())
                    scaleExponentEdit.setText("%d".format(scaleStrings[1].toInt()))
                })
        scaleExponentEdit.setOnEditorActionListener(
                editListener(null) { w: TextView ->

                    val prevScale = f.position.zoom

                    val result1 = scaleSignificandEdit.text.toString().formatToDouble(false)
                    val result2 = w.text.toString().formatToDouble(false)
                    val result3 = "${scaleSignificandEdit.text}e${w.text}".formatToDouble(false)
                    if (result1 != null && result2 != null && result3 != null) {
                        if (result3.isInfinite() || result3.isNaN()) {
                            act.showMessage(resources.getString(R.string.msg_num_out_range))
                        }
                        else {
                            f.position.zoom = result3
                            if (fsv.r.isRendering) fsv.r.interruptRender = true
                            fsv.r.renderToTex = true
                        }
                    }
                    else {
                        act.showMessage(resources.getString(R.string.msg_invalid_format))
                    }
                    val scaleStrings = "%e".format(Locale.US, f.position.zoom).split("e")
                    scaleSignificandEdit.setText("%.5f".format(scaleStrings[0].toFloat()))
                    w.text = "%d".format(scaleStrings[1].toInt())

                    fsv.r.checkThresholdCross(prevScale)

                })
        scaleLock.setOnClickListener {
            f.position.zoomLocked = scaleLock.isChecked
        }

        rotationEdit.setOnEditorActionListener(
                editListener(null) { w: TextView ->
                    val result = w.text.toString().formatToDouble()?.inRadians()
                    if (result != null) {
                        f.position.rotation = result
                        fsv.r.renderToTex = true
                        if (fsv.r.isRendering) fsv.r.interruptRender = true
                    }
                    w.text = "%.1f".format(f.position.rotation.inDegrees())
                })
        rotationLock.setOnClickListener {
            f.position.rotationLocked = rotationLock.isChecked
        }


        resetPositionButton.setOnClickListener {

            val prevZoom = f.position.zoom
            f.position.reset()
            fsv.r.checkThresholdCross(prevZoom)
            act.updatePositionEditTexts()
            fsv.r.calcNewTextureSpan = true
            fsv.r.renderToTex = true
            if (fsv.r.isRendering) fsv.r.interruptRender = true
            fsv.requestRender()

        }

        val subMenuButtonListener = { layout: View, button: Button ->
            View.OnClickListener {
                showLayout(layout)
                alphaButton(button)
            }
        }

        xyButton.setOnClickListener(        subMenuButtonListener(xyLayout,         xyButton        ))
        zoomButton.setOnClickListener(      subMenuButtonListener(zoomLayout,       zoomButton      ))
        zoomSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            var prevZoom = 1.0
            var previousZoomFactor = 1f

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                val zoomFactor = 10f.pow(progress.toFloat()/(zoomSeekBar.max/2f) - 1f)

                if (sc.continuousPosRender) {
                    f.position.zoom(zoomFactor/previousZoomFactor)
                    if (fsv.r.renderProfile == RenderProfile.CONTINUOUS) fsv.r.renderToTex = true
                }
                else fsv.r.zoom(zoomFactor/previousZoomFactor)
                // if (sc.continuousPosRender) fsv.r.checkThresholdCross(prevZoom)
                previousZoomFactor = zoomFactor

                val scaleStrings = "%e".format(Locale.US, f.position.zoom).split("e")
                scaleSignificandEdit.setText("%.5f".format(scaleStrings[0].toFloat()))
                scaleExponentEdit.setText("%d".format(scaleStrings[1].toInt()))

                if (fsv.r.isRendering && zoomSeekBar.progress != zoomSeekBar.max/2) fsv.r.interruptRender = true
                fsv.requestRender()

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                //previousZoom = f.position.scale.toFloat()
                if (sc.continuousPosRender) {
                    // fsv.r.beginContinuousRender = true
                    fsv.r.renderProfile = RenderProfile.CONTINUOUS
                }
                previousZoomFactor = 1f
                prevZoom = f.position.zoom
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

                if (sc.continuousPosRender) {
                    fsv.r.renderProfile = RenderProfile.DISCRETE
                }
                else {
                    val zoomFactor = 10f.pow(seekBar.progress.toFloat()/(zoomSeekBar.max/2f) - 1f)
                    f.position.zoom(zoomFactor)
                }
                fsv.r.checkThresholdCross(prevZoom)
                fsv.r.renderToTex = true
                fsv.requestRender()

                previousZoomFactor = 1f
                zoomSeekBar.progress = zoomSeekBar.max/2
            }

        })
        rotationButton.setOnClickListener(  subMenuButtonListener(rotationLayout,   rotationButton  ))
        rotationSeekBarListener = object : SeekBar.OnSeekBarChangeListener {

            var prevTheta = 0.0

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                val newTheta = ((progress/(rotationSeekBar.max/2).toDouble() - 1.0)*180.0).inRadians()
                rotationEdit.setText("%.1f".format(newTheta.inDegrees()))
                if (sc.continuousPosRender) {
                    f.position.rotation = newTheta
                    if (fsv.r.renderProfile == RenderProfile.CONTINUOUS) fsv.r.renderToTex = true
                    if (fsv.r.isRendering) fsv.r.interruptRender = true
                }
                else {
                    fsv.r.rotate((prevTheta - newTheta).toFloat())
                    prevTheta = newTheta
                }
                fsv.requestRender()

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

                if (sc.continuousPosRender) {
                    fsv.r.renderProfile = RenderProfile.CONTINUOUS
                    prevTheta = f.position.rotation
                }

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

                if (sc.continuousPosRender) fsv.r.renderProfile = RenderProfile.DISCRETE
                else {
                    val newTheta = ((seekBar.progress/(rotationSeekBar.max/2).toDouble() - 1.0)*180.0).inRadians()
                    f.position.rotation = newTheta
                }
                fsv.requestRender()

            }

        }
        rotationSeekBar.setOnSeekBarChangeListener(rotationSeekBarListener)


        xyLayout.hide()
        zoomLayout.hide()
        rotationLayout.hide()
        currentLayout = xyLayout
        currentButton = xyButton
        xyButton.performClick()


        act.updatePositionEditTexts()
        super.onViewCreated(v, savedInstanceState)

    }

    fun updateRotationLayout() {
        val act = activity as? MainActivity
        if (act != null) {
            val f = act.f
            rotationEdit.setText("%.1f".format(f.position.rotation.inDegrees()))
            rotationSeekBar.setOnSeekBarChangeListener(null)
            rotationSeekBar.progress = (rotationSeekBar.max * (f.position.rotation.inDegrees() / 360.0 + 0.5)).toInt()
            rotationSeekBar.setOnSeekBarChangeListener(rotationSeekBarListener)
        }
    }

}