package com.selfsimilartech.fractaleye

import android.view.View
import android.widget.ImageButton
import java.util.*

class PositionChangeOnLongClickListener(

        val fsv: FractalSurfaceView,
        val transformFractal: () -> Unit,
        val transformQuad: () -> Unit,
        val updateLayout: () -> Unit

) : View.OnLongClickListener {
    override fun onLongClick(v: View): Boolean {

        fsv.r.apply {

            if (fsv.r.isRendering) fsv.r.interruptRender = true
            if (sc.continuousPosRender) fsv.r.renderProfile = RenderProfile.CONTINUOUS

            val timer = Timer()
            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    if (v.isPressed) {  // if button still pressed keep zooming
                        fsv.r.checkThresholdCross { transformFractal() }
                        if (sc.continuousPosRender) fsv.r.renderToTex = true
                        else transformQuad()
                        act.runOnUiThread { updateLayout() }
                    } else {  // cancel zoom
                        timer.cancel()
                        fsv.r.renderProfile = RenderProfile.DISCRETE
                        fsv.r.renderToTex = true
                    }
                    fsv.requestRender()
                }
            }, 0L, 33L)

        }

        return true

    }
}