package com.selfsimilartech.fractaleye

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.jaredrummler.android.device.DeviceName
import kotlinx.android.synthetic.main.settings_fragment.*


class SettingsFragment : Fragment(R.layout.settings_fragment) {

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

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        val act = activity as MainActivity
        val f = Fractal.default
        val fsv = act.fsv
        val sc = SettingsConfig


        DeviceName.init(v.context)


//        val dontShowAgainView = layoutInflater.inflate(R.layout.alert_dialog_custom, null)
//        dontShowAgainView.dontShowCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
//            sc.showSlowDualflotDialog = !isChecked
//        }


//        upgradeButton.showGradient = true
//
//        if (!BuildConfig.DEV_VERSION) toggleGoldButton.hide()
//        toggleGoldButton.setOnClickListener {
//            sc.goldEnabled = !sc.goldEnabled
//            if (sc.goldEnabled) act.onGoldEnabled()
//            AlertDialog.Builder(act, R.style.AlertDialogCustom)
//                    .setIcon(R.drawable.wow)
//                    .setTitle(R.string.gold_enabled)
//                    .setMessage(R.string.gold_enabled_dscript)
//                    .setPositiveButton(android.R.string.ok, null)
//                    .show()
//        }
//        upgradeButton.setOnClickListener {
//            act.showUpgradeScreen()
//        }


//        consumePurchaseButton.setOnClickListener {
//            act.consumePurchase()
//        }


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



        continuousRenderSwitch.setOnCheckedChangeListener { _, isChecked ->
            sc.continuousPosRender = isChecked
            fsv.r.onContinuousPositionRenderChanged()
            if (sc.continuousPosRender && sc.renderBackground) {
                renderBackgroundSwitch.isChecked = false
                fsv.r.renderBackgroundChanged = true
            }
            renderBackgroundSwitch.isClickable = !sc.continuousPosRender
            renderBackgroundLayout.alpha = if (sc.continuousPosRender) 0.3f else 1f
            if (sc.continuousPosRender) fsv.requestRender()
        }
        continuousRenderSwitch.isChecked =
                savedInstanceState?.getBoolean("continuousRender")
                        ?: sc.continuousPosRender


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


//        fitToViewportSwitch.isChecked = sc.fitToViewport
//        fitToViewportSwitch.setOnCheckedChangeListener { _, isChecked ->
//
//            sc.fitToViewport = isChecked
//            act.updateSurfaceViewLayout()
//
//        }

        settingsDoneButton.setOnClickListener {
            act.closeSettingsMenu()
        }


//        showHintsSwitch.isChecked = sc.showHints
//        showHintsSwitch.setOnCheckedChangeListener { _, isChecked ->
//
//            sc.showHints = isChecked
//            act.updateHintVisibility()
//
//        }



        showChangelogLayout.setOnClickListener { act.showChangelog() }

        upgradeToGoldLayout.setOnClickListener { act.showUpgradeScreen() }

        aboutText1.text = resources.getString(R.string.about_info_1).format(BuildConfig.VERSION_NAME)

        emailLayout.setOnClickListener {

            var contentString = ""
            contentString += "Android Version: ${android.os.Build.VERSION.RELEASE}\n"
            contentString += "Device: ${DeviceName.getDeviceName() ?: android.os.Build.BRAND} (${android.os.Build.MODEL})\n"
            contentString += "Fractal Eye Version: ${BuildConfig.VERSION_NAME}\n\n"
            contentString += "Please describe your problem here and attach images/video of the problem occurring if possible:\n\n"

            val emailIntent = Intent(Intent.ACTION_SENDTO)
            emailIntent.apply {
                type = "message/rfc822"
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("selfaffinetech@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Fractal Eye Help")
                putExtra(Intent.EXTRA_TEXT, contentString)
                if (emailIntent.resolveActivity(act.packageManager) != null) startActivity(emailIntent)
            }

        }

        instagramLayout.setOnClickListener {

            val uri = Uri.parse("http://instagram.com/_u/fractaleye.app")
            val likeIng = Intent(Intent.ACTION_VIEW, uri)

            likeIng.setPackage("com.instagram.android")

            try {
                startActivity(likeIng)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://instagram.com/fractaleye.app")))
            }

        }


        chunkProfileTabs.getTabAt(sc.chunkProfile.ordinal)?.apply {
            view.setBackgroundColor(resources.getColor(R.color.menuDark7, null))
            select()
        }
        chunkProfileTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabReselected(tab: TabLayout.Tab?) {}

            override fun onTabUnselected(tab: TabLayout.Tab?) {

                tab?.view?.setBackgroundColor(resources.getColor(R.color.menuDark5, null))

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {

                tab?.view?.setBackgroundColor(resources.getColor(R.color.menuDark7, null))

                sc.chunkProfile = ChunkProfile.values()[tab?.position ?: 0]
                fsv.r.onChunkProfileChanged()

            }

        })


        splitTypeSwitch.isChecked = sc.useAlternateSplit
        splitTypeSwitch.setOnCheckedChangeListener { v, isChecked ->

            sc.useAlternateSplit = isChecked
            fsv.r.renderShaderChanged = true
            fsv.r.renderToTex = true
            fsv.requestRender()

        }


        allowSlowRendersSwitch.isChecked = sc.allowSlowDualfloat
        allowSlowRendersSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            sc.allowSlowDualfloat = isChecked
            fsv.r.checkThresholdCross(f.shape.position.zoom)
            if (f.shape.slowDualFloat && f.shape.position.zoom <= GpuPrecision.SINGLE.threshold) {
                fsv.r.renderToTex = true
                fsv.r.renderShaderChanged = true
                fsv.requestRender()
            }
        }
        allowSlowRendersHint.text = allowSlowRendersHint.text.toString().format(resources.getString(R.string.sine))



        if (sc.goldEnabled) onGoldEnabled()

        super.onViewCreated(v, savedInstanceState)

    }


    fun onGoldEnabled() {}

}