package com.selfsimilartech.fractaleye

import android.os.Handler
import android.os.Looper
import android.view.View
import java.util.*

class ParamChangeOnLongClickListener(

        val fsv: FractalSurfaceView,
        val transformFractal: () -> Unit,
        val updateLayout: () -> Unit

) : View.OnLongClickListener {

    val handler = Handler(Looper.getMainLooper())

    override fun onLongClick(v: View): Boolean {

        if (fsv.r.isRendering) fsv.r.interruptRender = true
        if (SettingsConfig.continuousParamRender) fsv.r.renderProfile = RenderProfile.CONTINUOUS

        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (v.isPressed) {  // if button still pressed keep zooming
                    transformFractal()
                    if (SettingsConfig.continuousParamRender) fsv.r.renderToTex = true
                    handler.post { updateLayout() }
                } else {  // cancel zoom
                    timer.cancel()
                    fsv.r.renderProfile = RenderProfile.DISCRETE
                    fsv.r.renderToTex = true
                }
                fsv.requestRender()
            }
        }, 0L, 33L)


        return true

    }

}