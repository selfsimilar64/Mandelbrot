package com.selfsimilartech.fractaleye

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.settings_fragment.*


class SettingsFragment : Fragment() {

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
            fsv.updateSystemUI()

        }



        resolutionBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val dimensions = Resolution.values()[progress].scaleRes(fsv.screenRes)
                resolutionDimensionsText.text = "${dimensions[0]} x ${dimensions[1]}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                val newRes = Resolution.values()[resolutionBar.progress]

                if (sc.resolution != newRes) {

                    sc.resolution = newRes
                    fsv.r.fgResolutionChanged = true
                    fsv.r.renderToTex = true
                    fsv.requestRender()

                }

            }

        })
        resolutionBar.max = if (BuildConfig.PAID_VERSION) Resolution.NUM_VALUES_PRO - 1 else Resolution.NUM_VALUES_FREE - 1
        resolutionBar.progress = sc.resolution.ordinal


        continuousRenderSwitch.setOnCheckedChangeListener { _, isChecked ->
            sc.continuousRender = isChecked
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

            fsv.r.bgResolutionChanged = true
            fsv.r.renderToTex = true
            fsv.requestRender()

        }


        fitToViewportSwitch.isChecked = sc.fitToViewport
        fitToViewportSwitch.setOnCheckedChangeListener { _, isChecked ->

            sc.fitToViewport = isChecked
            act.recalculateSurfaceViewLayout()

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
                        fsv.renderProfile = RenderProfile.SAVE
                        fsv.requestRender()
                    }
                }
                else {
                    fsv.renderProfile = RenderProfile.SAVE
                    fsv.requestRender()
                }
            }
        }
        renderButton.setOnClickListener {
            fsv.r.renderToTex = true
            fsv.requestRender()
        }



        precisionTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {

                if (tab.position == 2) {
                    Log.d("SETTINGS FRAGMENT", "auto selected")
                    sc.autoPrecision = true
                    fsv.checkThresholdCross(f.position.scale)
                }
                else {
                    sc.precision = Precision.values()[tab.position]
                    sc.autoPrecision = false
                }
                precisionBitsText.text = "${sc.precision.bits}-bit"
                fsv.r.renderShaderChanged = true
                fsv.r.renderToTex = true
                fsv.requestRender()

            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        if (sc.autoPrecision) precisionTabs.getTabAt(2)?.select()
        else precisionTabs.getTabAt(
                Precision.valueOf(
                        savedInstanceState?.getString("precision")
                                ?: sc.precision.name
                ).ordinal
        )?.select()

        super.onViewCreated(v, savedInstanceState)

    }


}