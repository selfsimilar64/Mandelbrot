package com.selfsimilartech.fractaleye

import android.view.View

class PositionChangeOnClickListener(

        val fsv: FractalSurfaceView,
        val transformFractal: () -> Unit,
        val transformQuad: () -> Unit,
        val updateLayout: () -> Unit

) : View.OnClickListener {

    override fun onClick(v: View) {
        fsv.r.apply {

            if (isRendering) interruptRender = true
            transformFractal()

            if (sc.continuousPosRender) {
                renderToTex = true
                renderProfile = RenderProfile.CONTINUOUS
                renderFinishedListener = object : RenderFinishedListener {
                    override fun onRenderFinished(buffer: ByteArray?) {
                        renderFinishedListener = null
                        renderProfile = RenderProfile.DISCRETE
                        renderToTex = true
                        fsv.requestRender()
                    }
                }
            } else {
                transformQuad()
                hasZoomed = true
                renderFinishedListener = object : RenderFinishedListener {
                    override fun onRenderFinished(buffer: ByteArray?) {
                        renderFinishedListener = null
                        renderToTex = true
                        fsv.requestRender()
                    }
                }
            }
            fsv.requestRender()
            act.runOnUiThread { updateLayout() }
        }
    }

}