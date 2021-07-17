package com.selfsimilartech.fractaleye

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.position_fragment.*
import kotlinx.android.synthetic.main.discrete_sensitivity_layout.view.*
import java.util.*


class PositionFragment : MenuFragment() {


    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.position_fragment, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        positionLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        resetPositionButton.setOnClickListener {

            fsv.r.checkThresholdCross { f.shape.position.reset() }
            act.updatePositionLayout()
            fsv.r.calcNewTextureSpan = true
            fsv.r.renderToTex = true
            if (fsv.r.isRendering) fsv.r.interruptRender = true
            fsv.requestRender()

        }

        shiftHorizontalButton.setOnClickListener{
            subMenuButtonListener(xLayout, shiftHorizontalButton).onClick(it)
            shiftHorizontalSensitivity.sensitivityButton.setImageResource(sc.shiftSensitivity.iconId)
        }
        shiftVerticalButton.setOnClickListener {
            subMenuButtonListener(yLayout, shiftVerticalButton).onClick(it)
            shiftVerticalSensitivity.sensitivityButton.setImageResource(sc.shiftSensitivity.iconId)
        }
        zoomButton.setOnClickListener(      subMenuButtonListener(zoomLayout,       zoomButton      ))
        rotateButton.setOnClickListener(  subMenuButtonListener(rotationLayout,   rotateButton  ))




        // SHIFT HORIZONTAL LAYOUT
        xValue.setOnEditorActionListener(editListener(yValue) { w: TextView ->
            val result = w.text.toString().formatToDouble()
            if (result != null) {
                f.shape.position.x = result
                if (fsv.r.isRendering) fsv.r.interruptRender = true
                fsv.r.renderToTex = true
            }
            w.text = "%.17f".format(f.shape.position.x)

        })
        shiftHorizontalSensitivity.sensitivityButton.apply {
            setOnClickListener {
                sc.shiftSensitivity = sc.shiftSensitivity.next()
                setImageResource(sc.shiftSensitivity.iconId)
            }
        }
        shiftLeftButton.setOnClickListener(PositionChangeOnClickListener(fsv,
                transformFractal = { f.shape.position.translate(-sc.shiftSensitivity.shiftDiscrete, 0f) },
                transformQuad = { fsv.r.translate(-2f*sc.shiftSensitivity.shiftDiscrete, 0f) },
                updateLayout = { updateShiftLayouts() }
        ))
        shiftLeftButton.setOnLongClickListener(PositionChangeOnLongClickListener(fsv,
                transformFractal = { f.shape.position.translate(-sc.shiftSensitivity.shiftContinuous, 0f) },
                transformQuad = { fsv.r.translate(-2f*sc.shiftSensitivity.shiftContinuous, 0f) },
                updateLayout = { updateShiftLayouts() }
        ))
        shiftRightButton.setOnClickListener(PositionChangeOnClickListener(fsv,
                transformFractal = { f.shape.position.translate(sc.shiftSensitivity.shiftDiscrete, 0f) },
                transformQuad = { fsv.r.translate(2f*sc.shiftSensitivity.shiftDiscrete, 0f) },
                updateLayout = { updateShiftLayouts() }
        ))
        shiftRightButton.setOnLongClickListener(PositionChangeOnLongClickListener(fsv,
                transformFractal = { f.shape.position.translate(sc.shiftSensitivity.shiftContinuous, 0f) },
                transformQuad = { fsv.r.translate(2f*sc.shiftSensitivity.shiftContinuous, 0f) },
                updateLayout = { updateShiftLayouts() }
        ))



        // SHIFT VERTICAL LAYOUT
        yValue.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()
            if (result != null) {
                f.shape.position.y = result
                if (fsv.r.isRendering) fsv.r.interruptRender = true
                fsv.r.renderToTex = true
            }
            w.text = "%.17f".format(f.shape.position.y)
        })
        shiftVerticalSensitivity.sensitivityButton.apply {
            setOnClickListener {
                sc.shiftSensitivity = sc.shiftSensitivity.next()
                setImageResource(sc.shiftSensitivity.iconId)
            }
        }
        shiftUpButton.setOnClickListener(PositionChangeOnClickListener(fsv,
                transformFractal = { f.shape.position.translate(0f, -sc.shiftSensitivity.shiftDiscrete) },
                transformQuad = { fsv.r.translate(0f, 2f*sc.shiftSensitivity.shiftDiscrete) },
                updateLayout = { updateShiftLayouts() }
        ))
        shiftUpButton.setOnLongClickListener(PositionChangeOnLongClickListener(fsv,
                transformFractal = { f.shape.position.translate(0f, -sc.shiftSensitivity.shiftContinuous) },
                transformQuad = { fsv.r.translate(0f, 2f*sc.shiftSensitivity.shiftContinuous) },
                updateLayout = { updateShiftLayouts() }
        ))
        shiftDownButton.setOnClickListener(PositionChangeOnClickListener(fsv,
                transformFractal = { f.shape.position.translate(0f, sc.shiftSensitivity.shiftDiscrete) },
                transformQuad = { fsv.r.translate(0f, -2f*sc.shiftSensitivity.shiftDiscrete) },
                updateLayout = { updateShiftLayouts() }
        ))
        shiftDownButton.setOnLongClickListener(PositionChangeOnLongClickListener(fsv,
                transformFractal = { f.shape.position.translate(0f, sc.shiftSensitivity.shiftContinuous) },
                transformQuad = { fsv.r.translate(0f, -2f*sc.shiftSensitivity.shiftContinuous) },
                updateLayout = { updateShiftLayouts() }
        ))



        // ZOOM LAYOUT
        zoomSignificandValue.setOnEditorActionListener(
                editListener(zoomExponentValue) { w: TextView ->
                    val result1 = w.text.toString().formatToDouble(false)
                    val result2 = zoomExponentValue.text.toString().formatToDouble(false)
                    val result3 = "${w.text}e${zoomExponentValue.text}".formatToDouble(false)
                    if (result1 != null && result2 != null && result3 != null) {
                        if (result3.isInfinite() || result3.isNaN()) {
                            act.showMessage(resources.getString(R.string.msg_num_out_range))
                        }
                        else {
                            f.shape.position.zoom = result3
                            fsv.r.renderToTex = true
                            fsv.r.calcNewTextureSpan = true
                            if (fsv.r.isRendering) fsv.r.interruptRender = true
                        }
                    }
                    else {
                        act.showMessage(resources.getString(R.string.msg_invalid_format))
                    }
                    val scaleStrings = "%e".format(Locale.US, f.shape.position.zoom).split("e")
                    w.text = "%.2f".format(scaleStrings[0].toFloat())
                    zoomExponentValue.setText("%d".format(scaleStrings[1].toInt()))
                })
        zoomExponentValue.setOnEditorActionListener(
                editListener(null) { w: TextView ->

                    val result1 = zoomSignificandValue.text.toString().formatToDouble(false)
                    val result2 = w.text.toString().formatToDouble(false)
                    val result3 = "${zoomSignificandValue.text}e${w.text}".formatToDouble(false)
                    if (result1 != null && result2 != null && result3 != null) {
                        if (result3.isInfinite() || result3.isNaN()) {
                            act.showMessage(resources.getString(R.string.msg_num_out_range))
                        }
                        else {
                            fsv.r.checkThresholdCross { f.shape.position.zoom = result3 }
                            if (fsv.r.isRendering) fsv.r.interruptRender = true
                            fsv.r.renderToTex = true
                            fsv.r.calcNewTextureSpan = true
                        }
                    }
                    else {
                        act.showMessage(resources.getString(R.string.msg_invalid_format))
                    }
                    val scaleStrings = "%e".format(Locale.US, f.shape.position.zoom).split("e")
                    zoomSignificandValue.setText("%.2f".format(scaleStrings[0].toFloat()))
                    w.text = "%d".format(scaleStrings[1].toInt())

                })
        zoomSensitivity.sensitivityButton.apply {
            setOnClickListener {
                sc.zoomSensitivity = sc.zoomSensitivity.next()
                setImageResource(sc.zoomSensitivity.iconId)
            }
        }
        zoomInButton.setOnClickListener(PositionChangeOnClickListener(fsv,
                transformFractal  = { fsv.r.checkThresholdCross { f.shape.position.zoom(sc.zoomSensitivity.zoomDiscrete) } },
                transformQuad     = { fsv.r.zoom(sc.zoomSensitivity.zoomDiscrete) },
                updateLayout      = { updateZoomLayout() }
        ))
        zoomInButton.setOnLongClickListener(PositionChangeOnLongClickListener(fsv,
                transformFractal  = { fsv.r.checkThresholdCross { f.shape.position.zoom(sc.zoomSensitivity.zoomContinuous) } },
                transformQuad     = { fsv.r.zoom(sc.zoomSensitivity.zoomContinuous) },
                updateLayout      = { updateZoomLayout() }
        ))
        zoomOutButton.setOnClickListener(PositionChangeOnClickListener(fsv,
                transformFractal  = { fsv.r.checkThresholdCross { f.shape.position.zoom(1f/sc.zoomSensitivity.zoomDiscrete) } },
                transformQuad     = { fsv.r.zoom(1f/sc.zoomSensitivity.zoomDiscrete) },
                updateLayout      = { updateZoomLayout() }
        ))
        zoomOutButton.setOnLongClickListener(PositionChangeOnLongClickListener(fsv,
                transformFractal  = { fsv.r.checkThresholdCross { f.shape.position.zoom(1f/sc.zoomSensitivity.zoomContinuous) } },
                transformQuad     = { fsv.r.zoom(1f/sc.zoomSensitivity.zoomContinuous) },
                updateLayout      = { updateZoomLayout() }
        ))


        // ROTATION LAYOUT
        rotationValue.setOnEditorActionListener(
                editListener(null) { w: TextView ->
                    val result = w.text.toString().formatToDouble()?.inRadians()
                    if (result != null) {
                        f.shape.position.rotation = result
                        fsv.r.renderToTex = true
                        if (fsv.r.isRendering) fsv.r.interruptRender = true
                    }
                    w.text = "%.1f".format(f.shape.position.rotation.inDegrees())
                })
        rotationLock.setOnClickListener {
            f.shape.position.rotationLocked = rotationLock.isChecked
        }
        rotationSensitivity.sensitivityButton.apply {
            setOnClickListener {
                sc.rotationSensitivity = sc.rotationSensitivity.next()
                setImageResource(sc.rotationSensitivity.iconId)
            }
        }
        rotateLeftButton.setOnClickListener(PositionChangeOnClickListener(fsv,
                transformFractal = { f.shape.position.rotate(sc.rotationSensitivity.rotationDiscrete) },
                transformQuad = { fsv.r.rotate(sc.rotationSensitivity.rotationDiscrete) },
                updateLayout = { updateRotationLayout() }
        ))
        rotateLeftButton.setOnLongClickListener(PositionChangeOnLongClickListener(fsv,
                transformFractal = { f.shape.position.rotate(sc.rotationSensitivity.rotationContinuous) },
                transformQuad = { fsv.r.rotate(sc.rotationSensitivity.rotationContinuous) },
                updateLayout = { updateRotationLayout() }
        ))
        rotateRightButton.setOnClickListener(PositionChangeOnClickListener(fsv,
                transformFractal = { f.shape.position.rotate(-sc.rotationSensitivity.rotationDiscrete) },
                transformQuad = { fsv.r.rotate(-sc.rotationSensitivity.rotationDiscrete) },
                updateLayout = { updateRotationLayout() }
        ))
        rotateRightButton.setOnLongClickListener(PositionChangeOnLongClickListener(fsv,
                transformFractal = { f.shape.position.rotate(-sc.rotationSensitivity.rotationContinuous) },
                transformQuad = { fsv.r.rotate(-sc.rotationSensitivity.rotationContinuous) },
                updateLayout = { updateRotationLayout() }
        ))



        xLayout.hide()
        yLayout.hide()
        zoomLayout.hide()
        rotationLayout.hide()
        currentLayout = zoomLayout
        currentButton = zoomButton
        alphaButton(zoomButton)
        showLayout(zoomLayout)
        // zoomButton.performClick()


        act.updatePositionLayout()

    }

    override fun updateLayout() {
        updateShiftLayouts()
        updateRotationLayout()
        updateZoomLayout()
    }
    fun updateShiftLayouts() {
        xValue?.setText("%.17f".format(f.shape.position.x))
        yValue?.setText("%.17f".format(f.shape.position.y))
    }
    fun updateRotationLayout() {
        rotationValue?.setText("%.1f".format(f.shape.position.rotation.inDegrees()))
    }
    fun updateZoomLayout() {
        val zoomStrings = "%e".format(Locale.US, f.shape.position.zoom).split("e")
        zoomSignificandValue?.setText("%.2f".format(zoomStrings.getOrNull(0)?.toFloat() ?: 1f))
        zoomExponentValue?.setText("%d".format(zoomStrings.getOrNull(1)?.toInt() ?: 2))
    }

}