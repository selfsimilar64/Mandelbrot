package com.selfsimilartech.fractaleye

import android.view.View

class ColorChangeOnClickListener (

    val fsv: FractalSurfaceView,
    val transformFractal: () -> Unit,
    val updateLayout: () -> Unit

) : View.OnClickListener {

    override fun onClick(v: View) {
        fsv.r.run {
            transformFractal()
            fsv.requestRender()
        }
        updateLayout()
    }

}