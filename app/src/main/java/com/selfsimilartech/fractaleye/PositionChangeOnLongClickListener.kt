package com.selfsimilartech.fractaleye

import android.os.Handler
import android.os.Looper
import android.view.View
import java.util.*

class PositionChangeOnLongClickListener(

    val fsv: FractalSurfaceView,
    val transformFractal: () -> Unit,
    val transformQuad: () -> Unit,
    val updateLayout: () -> Unit

) : View.OnLongClickListener {

    val handler = Handler(Looper.getMainLooper())

    override fun onLongClick(v: View): Boolean {

        if (fsv.r.isRendering) fsv.r.interruptRender = true
        if (Settings.continuousPosRender) fsv.r.renderProfile = RenderProfile.CONTINUOUS

        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (v.isPressed) {  // if button still pressed keep zooming
                    fsv.r.checkThresholdCross { transformFractal() }
                    if (Settings.continuousPosRender) fsv.r.renderToTex = true
                    else transformQuad()
                    handler.post { updateLayout() }
                } else {  // cancel zoom
                    timer.cancel()
                    fsv.r.renderProfile = RenderProfile.DISCRETE
                    fsv.r.renderToTex = true
                }
                fsv.requestRender()
            }
        }, 0L, (1000.0/Settings.targetFramerate).toLong())

        return true

    }

}