package com.selfsimilartech.fractaleye

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import java.util.*

class ColorChangeOnLongClickListener(

    val fsv: FractalSurfaceView,
    val transformFractal: () -> Unit,
    val updateLayout: () -> Unit

) : View.OnLongClickListener {

    val handler = Handler(Looper.getMainLooper())

    override fun onLongClick(v: View): Boolean {

        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (v.isPressed) {  // if button still pressed keep changing
                    transformFractal()
                    handler.post { updateLayout() }
                } else {  // cancel change
                    timer.cancel()
                }
                fsv.requestRender()
            }
        }, 0L, (1000.0/SettingsConfig.targetFramerate).toLong())


        return true

    }
}