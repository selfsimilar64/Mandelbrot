package com.selfsimilartech.fractaleye

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.settings_fragment.*
import java.util.*


class SettingsFragment : MenuFragment() {

//    private fun createNotificationChannel() {
//        // Create the NotificationChannel, but only on API 26+ because
//        // the NotificationChannel class is new and not in the support library
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = "fractal notify"
//            val descriptionText = "remind about fractals"
//            val importance = NotificationManager.IMPORTANCE_HIGH
//            val channel = NotificationChannel("Fractal Eye", name, importance).apply {
//                description = descriptionText
//            }
//            // Register the channel with the system
//            val notificationManager: NotificationManager =
//                    (activity as MainActivity).getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
//    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {

        return inflater.inflate(R.layout.settings_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        val act = activity as MainActivity
        val f = act.f
        val fsv = act.fsv
        val sc = act.sc


        networkButton.setOnClickListener {

            //act.connectToServer()

        }


//        restartActivityButton.setOnClickListener {
//
//            act.recreate()
//
//        }
//        val nm = NotificationManagerCompat.from(context!!)
//        createNotificationChannel()
//        sendNotificationButton.setOnClickListener {
//
//            val builder = NotificationCompat.Builder(context!!, "Fractal Eye")
//                    .setSmallIcon(R.drawable.mandelbrot_icon)
//                    .setContentTitle("u need 2 make moar fractalz")
//                    .setContentText("click here 4 fractalz")
//                    .setPriority(NotificationCompat.PRIORITY_HIGH)
//            nm.notify(3, builder.build())
//
//        }


        hideNavBarSwtich.isChecked = sc.hideNavBar
        hideNavBarSwtich.setOnCheckedChangeListener { _, isChecked ->

            sc.hideNavBar = isChecked
            act.updateSystemUI()

        }



        resolutionBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val dimensions = Resolution.values()[progress].scaleRes(fsv.r.screenRes)
                resolutionDimensionsText.text = "${dimensions.x} x ${dimensions.y}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                val newRes = Resolution.values()[resolutionBar.progress]

                if (sc.resolution != newRes) {

                    sc.resolution = newRes
                    if (fsv.r.isRendering) fsv.r.interruptRender = true
                    fsv.r.fgResolutionChanged = true
                    fsv.r.renderToTex = true
                    fsv.requestRender()

                }

            }

        })
        resolutionBar.max = if (BuildConfig.PAID_VERSION) Resolution.NUM_VALUES_PRO - 1 else Resolution.NUM_VALUES_FREE - 1
        //Log.e("SETTINGS", "resolution ordinal: ${sc.resolution.ordinal}")
        resolutionBar.progress = sc.resolution.ordinal
        val dimensions = sc.resolution.scaleRes(fsv.r.screenRes)
        resolutionDimensionsText.text = "${dimensions.x} x ${dimensions.y}"


        continuousRenderSwitch.setOnCheckedChangeListener { _, isChecked ->
            sc.continuousRender = isChecked
            fsv.r.onContinuousRenderChanged()
            if (sc.continuousRender && sc.renderBackground) {
                renderBackgroundSwitch.isChecked = false
                fsv.r.renderBackgroundChanged = true
            }
            renderBackgroundSwitch.isClickable = !sc.continuousRender
            renderBackgroundLayout.alpha = if (sc.continuousRender) 0.3f else 1f
        }
        continuousRenderSwitch.isChecked =
                savedInstanceState?.getBoolean("continuousRender")
                        ?: sc.continuousRender


        displayParamsSwitch.isChecked =
                savedInstanceState?.getBoolean("displayParams")
                        ?: sc.displayParams
        displayParamsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sc.displayParams = isChecked
            act.updateDisplayParams(settingsChanged = true)
        }


        renderBackgroundSwitch.isChecked = sc.renderBackground
        renderBackgroundSwitch.setOnCheckedChangeListener { _, isChecked ->

            sc.renderBackground = isChecked

            fsv.r.renderBackgroundChanged = true
            fsv.r.renderToTex = isChecked
            fsv.requestRender()

        }


        fitToViewportSwitch.isChecked = sc.fitToViewport
        fitToViewportSwitch.setOnCheckedChangeListener { _, isChecked ->

            sc.fitToViewport = isChecked
            act.updateSurfaceViewLayout()

        }



//        showHintsSwitch.isChecked = sc.showHints
//        showHintsSwitch.setOnCheckedChangeListener { _, isChecked ->
//
//            sc.showHints = isChecked
//            act.updateHintVisibility()
//
//        }


        saveToFileButton.setOnClickListener {
            if (fsv.r.isRendering) act.showMessage(resources.getString(R.string.msg_save_wait))
            else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(v.context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(
                                act,
                                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                WRITE_STORAGE_REQUEST_CODE)
                    }
                    else {
                        fsv.r.renderProfile = RenderProfile.SAVE
                        fsv.requestRender()
                    }
                }
                else {
                    fsv.r.renderProfile = RenderProfile.SAVE
                    fsv.requestRender()
                }
            }
        }
//        renderButton.setOnClickListener {
//            fsv.r.renderToTex = true
//            fsv.requestRender()
//        }




        val subMenuButtonListener = { layout: View, button: Button ->
            View.OnClickListener {
                if (layout == renderOptionsLayout || layout == displayOptionsLayout) {
                    act.uiSetHeight(resources.getDimension(R.dimen.uiLayoutHeightTall).toInt())
                }
                else if (currentLayout == renderOptionsLayout || currentLayout == displayOptionsLayout) {
                    act.uiSetHeight(resources.getDimension(R.dimen.uiLayoutHeight).toInt())
                }
                showLayout(layout)
                alphaButton(button)
            }
        }



        resolutionButton.setOnClickListener(subMenuButtonListener(resolutionLayout, resolutionButton))
        renderOptionsButton.setOnClickListener(subMenuButtonListener(renderOptionsLayout, renderOptionsButton))
        displayOptionsButton.setOnClickListener(subMenuButtonListener(displayOptionsLayout, displayOptionsButton))



        currentLayout = resolutionLayout
        currentButton = resolutionButton
        resolutionLayout.hide()
        renderOptionsLayout.hide()
        displayOptionsLayout.hide()


        resolutionButton.performClick()



        super.onViewCreated(v, savedInstanceState)

    }


}