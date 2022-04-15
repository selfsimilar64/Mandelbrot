package com.selfsimilartech.fractaleye

import android.os.Handler
import android.os.Looper
import android.view.View

class PositionChangeOnClickListener(

        val fsv: FractalSurfaceView,
        val transformFractal: () -> Unit,
        val transformQuad: () -> Unit,
        val updateLayout: () -> Unit

) : View.OnClickListener {

    val handler = Handler(Looper.getMainLooper())

    override fun onClick(v: View) {

            if (fsv.r.isRendering) fsv.r.interruptRender = true
            transformFractal()

            if (Settings.continuousPosRender) {

                fsv.r.renderProfile = RenderProfile.CONTINUOUS
                fsv.r.renderToTex = true
                fsv.r.renderFinishedListener = object : RenderFinishedListener {
                    override fun onRenderFinished(buffer: ByteArray?) {
                        fsv.r.renderFinishedListener = null
                        fsv.r.renderProfile = RenderProfile.DISCRETE
                        fsv.r.renderToTex = true
                        fsv.requestRender()
                    }
                }
                fsv.requestRender()

            } else {
                transformQuad()
                fsv.r.hasZoomed = true
                fsv.r.renderFinishedListener = object : RenderFinishedListener {
                    override fun onRenderFinished(buffer: ByteArray?) {
                        fsv.r.renderFinishedListener = null
                        fsv.r.renderToTex = true
                        fsv.requestRender()
                    }
                }
                fsv.requestRender()
            }

        updateLayout()
    }

}