package com.selfsimilartech.fractaleye

import android.view.View
import android.widget.ImageButton
import java.util.*

class ParamChangeOnLongClickListener(

        val fsv: FractalSurfaceView,
        val transformFractal: () -> Unit,
        val updateLayout: () -> Unit

) : View.OnLongClickListener {
    override fun onLongClick(v: View): Boolean {

        fsv.r.apply {

            if (fsv.r.isRendering) fsv.r.interruptRender = true
            if (sc.continuousParamRender) fsv.r.renderProfile = RenderProfile.CONTINUOUS

            val timer = Timer()
            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    if (v.isPressed) {  // if button still pressed keep zooming
                        transformFractal()
                        if (sc.continuousParamRender) fsv.r.renderToTex = true
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