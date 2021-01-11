package com.selfsimilartech.fractaleye

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.cardview.widget.CardView
import androidx.core.view.children
import kotlinx.android.synthetic.main.image_fragment.*


class ImageFragment : MenuFragment() {

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

        return inflater.inflate(R.layout.image_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        val act = activity as MainActivity
        val f = act.f
        val fsv = act.fsv
        val sc = act.sc


        fitToViewportButton.setOnClickListener {

            if (!fitToViewportButton.isChecked && sc.aspectRatio != AspectRatio.RATIO_SCREEN) aspectDefaultButton.performClick()

            sc.fitToViewport = fitToViewportButton.isChecked
            act.updateSurfaceViewLayout()

        }
        fitToViewportButton.isChecked = sc.fitToViewport


        resolutionBar.showGradient = true
        resolutionBar.gradientStartProgress = Resolution.NUM_VALUES_FREE - 1
        resolutionBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            var prevProgress = 0

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.e("SETTINGS", "progress: $progress")
                updateResolutionText(Resolution.working[progress], sc.aspectRatio)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                prevProgress = resolutionBar.progress
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                val newRes = Resolution.working[resolutionBar.progress]

                if (sc.resolution != newRes) {

                    if (resolutionBar.progress > resolutionBar.gradientStartProgress && !sc.goldEnabled) {
                        resolutionBar.progress = prevProgress
                        act.showUpgradeScreen()
                    }
                    else {

                        sc.resolution = newRes
                        if (fsv.r.isRendering) fsv.r.interruptRender = true
                        fsv.r.fgResolutionChanged = true
                        fsv.r.renderToTex = true
                        fsv.requestRender()

                    }

                }

            }

        })
        resolutionBar.max = Resolution.NUM_VALUES_WORKING() - 1
        //Log.e("SETTINGS", "resolution ordinal: ${sc.resolution.ordinal}")
        resolutionBar.progress = Resolution.working.indexOf(sc.resolution)
        updateResolutionText(sc.resolution, sc.aspectRatio)


//        renderBackgroundSwitch.isChecked = sc.renderBackground
//        renderBackgroundSwitch.setOnCheckedChangeListener { _, isChecked ->
//
//            sc.renderBackground = isChecked
//
//            fsv.r.renderBackgroundChanged = true
//            fsv.r.renderToTex = isChecked
//            fsv.requestRender()
//
//        }


//        fitToViewportSwitch.isChecked = sc.fitToViewport
//        fitToViewportSwitch.setOnCheckedChangeListener { _, isChecked ->
//
//            sc.fitToViewport = isChecked
//            act.updateSurfaceViewLayout()
//
//        }



        val subMenuButtonListener = { layout: View, button: Button ->
            View.OnClickListener {
                showLayout(layout)
                alphaButton(button)
            }
        }

        var currentAspect : CardView? = null
        val aspectRatioButtonListener = { card: CardView, button: ImageButton, ratio: AspectRatio ->
            View.OnClickListener {

                if (ratio.goldFeature && !sc.goldEnabled) act.showUpgradeScreen()
                if (!sc.fitToViewport && ratio != AspectRatio.RATIO_SCREEN) fitToViewportButton.performClick()
                sc.aspectRatio = ratio

                currentAspect?.setCardBackgroundColor(resources.getColor(R.color.menuDark5, null))
                (currentAspect?.children?.first() as? ImageButton)?.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.unselectedTint, null))

                card.setCardBackgroundColor(resources.getColor(R.color.menuDark8, null))
                button.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.selectedTint, null))

                currentAspect = card
                updateResolutionText(sc.resolution, sc.aspectRatio)
                act.onAspectRatioChanged()

            }
        }

        aspect11Button.setOnClickListener(aspectRatioButtonListener(aspect11Card, aspect11Button, AspectRatio.RATIO_1_1))
        aspect45Button.setOnClickListener(aspectRatioButtonListener(aspect45Card, aspect45Button, AspectRatio.RATIO_4_5))
        aspect57Button.setOnClickListener(aspectRatioButtonListener(aspect57Card, aspect57Button, AspectRatio.RATIO_5_7))
        aspect23Button.setOnClickListener(aspectRatioButtonListener(aspect23Card, aspect23Button, AspectRatio.RATIO_2_3))
//        aspect169Button.setOnClickListener(aspectRatioButtonListener(aspect169Card, aspect169Button, AspectRatio.RATIO_16_9))
        aspect916Button.setOnClickListener(aspectRatioButtonListener(aspect916Card, aspect916Button, AspectRatio.RATIO_9_16))
//        aspect21Button.setOnClickListener(aspectRatioButtonListener(aspect21Card, aspect21Button, AspectRatio.RATIO_2_1))
        aspect12Button.setOnClickListener(aspectRatioButtonListener(aspect12Card, aspect12Button, AspectRatio.RATIO_1_2))
//        aspect13Button.setOnClickListener(aspectRatioButtonListener(aspect13Card, aspect13Button, AspectRatio.RATIO_1_3))
        aspectDefaultButton.setOnClickListener(aspectRatioButtonListener(aspectDefaultCard, aspectDefaultButton, AspectRatio.RATIO_SCREEN))

        val aspectCards = listOf(
                aspectDefaultCard,
                aspect11Card,
                aspect45Card,
                aspect57Card,
                aspect23Card,
                aspect916Card,
                aspect12Card
        )
        val aspectButtons = listOf(
                aspectDefaultButton,
                aspect11Button,
                aspect45Button,
                aspect57Button,
                aspect23Button,
                aspect916Button,
                aspect12Button
        )
        val goldAspects = listOf(
                aspect45Button,
                aspect57Button,
                aspect23Button,
                aspect916Button,
                aspect12Button
        )
        goldAspects.forEach {
            it.showGradient = true
            it.imageTintList = ColorStateList.valueOf(Color.WHITE)
        }



        resolutionButton.setOnClickListener(subMenuButtonListener(resolutionLayout, resolutionButton))
        aspectRatioButton.setOnClickListener(subMenuButtonListener(aspectRatioLayout, aspectRatioButton))


        currentAspect = aspectCards[AspectRatio.all.indexOf(sc.aspectRatio)]
        aspectButtons[AspectRatio.all.indexOf(sc.aspectRatio)]?.performClick()

        currentLayout = resolutionLayout
        currentButton = resolutionButton
        resolutionLayout.hide()
        aspectRatioLayout.hide()


        resolutionButton.performClick()


        if (sc.goldEnabled) onGoldEnabled()

        super.onViewCreated(v, savedInstanceState)

    }

    fun updateResolutionText(res: Resolution, ratio: AspectRatio) {

        val dims = Point(res.w, res.h)
        if (ratio.r > AspectRatio.RATIO_SCREEN.r) dims.x = (dims.y / ratio.r).toInt()
        else if (ratio.r < AspectRatio.RATIO_SCREEN.r) dims.y = (dims.x * ratio.r).toInt()

        resolutionDimensionsText.text = "${dims.x} x ${dims.y}"
    }

    fun onGoldEnabled() {
        resolutionBar.showGradient = false
        val goldAspects = listOf(
                aspect45Button,
                aspect57Button,
                aspect23Button,
                aspect916Button,
                aspect12Button
        )
        goldAspects.forEach {
            it.showGradient = false
            it.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.unselectedTint, null))
        }
    }

}