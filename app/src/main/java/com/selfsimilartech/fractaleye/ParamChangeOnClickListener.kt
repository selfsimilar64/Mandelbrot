package com.selfsimilartech.fractaleye

import android.view.View

class ParamChangeOnClickListener(

        val fsv: FractalSurfaceView,
        val transformFractal: () -> Unit,
        val updateLayout: () -> Unit

) : View.OnClickListener {

    override fun onClick(v: View) {
        fsv.r.apply {

            if (isRendering) interruptRender = true
            transformFractal()

            if (sc.continuousParamRender) {
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
        }
        updateLayout()
    }

}