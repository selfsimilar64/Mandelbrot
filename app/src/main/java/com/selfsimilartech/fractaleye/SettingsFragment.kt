package com.selfsimilartech.fractaleye

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.jaredrummler.android.device.DeviceName
import com.selfsimilartech.fractaleye.databinding.FragmentSettingsBinding
import java.text.NumberFormat
import java.text.ParseException


class SettingsFragment : Fragment(R.layout.fragment_settings) {

    interface OnSettingsChangedListener {

        fun closeSettingsMenu()
        fun updateSystemBarsVisibility()
        fun enableUltraHighRes()
        fun disableUltraHighRes()
        fun updateButtonAlignment()

    }

    private var listener : OnSettingsChangedListener? = null

    lateinit var b : FragmentSettingsBinding

    private fun String.formatToDouble(showMsg: Boolean = true) : Double? {
        val nf = NumberFormat.getInstance()
        var d : Double? = null
        try { d = nf.parse(this)?.toDouble() }
        catch (e: ParseException) {
            if (showMsg) {
                val act = activity as MainActivity
                act.showMessage(resources.getString(R.string.msg_invalid_format))
            }
        }
        return d
    }

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        b = FragmentSettingsBinding.inflate(inflater, container, false)
        return b.root

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OnSettingsChangedListener
        } catch (castException: ClassCastException) {
            Log.e("SETTINGS", "the activity does not implement the interface")
        }
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        val act = activity as MainActivity
        val f = Fractal.default
        val fsv = act.fsv
        val sc = Settings


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


        b.hideNavBarSwtich.isChecked = sc.hideSystemBars
        b.hideNavBarSwtich.setOnCheckedChangeListener { _, isChecked ->

            sc.hideSystemBars = isChecked
            listener?.updateSystemBarsVisibility()

        }


        val buttonAlignmentSelector = ViewSelector(
            requireContext(),
            R.drawable.edit_mode_button_highlight,
            listOf(b.alignmentLeftButton, b.alignmentCenterButton, b.alignmentRightButton)
        )
        buttonAlignmentSelector.select(
            when (sc.buttonAlignment) {
                ButtonAlignment.LEFT -> b.alignmentLeftButton
                ButtonAlignment.CENTER -> b.alignmentCenterButton
                ButtonAlignment.RIGHT -> b.alignmentRightButton
            }
        )

        b.alignmentLeftButton.setOnClickListener {
            sc.buttonAlignment = ButtonAlignment.LEFT
            buttonAlignmentSelector.select(it)
            listener?.updateButtonAlignment()
        }
        b.alignmentCenterButton.setOnClickListener {
            sc.buttonAlignment = ButtonAlignment.CENTER
            buttonAlignmentSelector.select(it)
            listener?.updateButtonAlignment()
        }
        b.alignmentRightButton.setOnClickListener {
            sc.buttonAlignment = ButtonAlignment.RIGHT
            buttonAlignmentSelector.select(it)
            listener?.updateButtonAlignment()
        }



        b.continuousRenderSwitch.setOnClickListener {
            sc.continuousPosRender = b.continuousRenderSwitch.isChecked
            fsv.r.onContinuousPositionRenderChanged()
            if (sc.continuousPosRender && sc.renderBackground) {
                b.renderBackgroundSwitch.isChecked = false
                sc.renderBackground = false
                fsv.r.renderBackgroundChanged = true
            }
            b.renderBackgroundSwitch.isClickable = !sc.continuousPosRender
            b.renderBackgroundLayout.alpha = if (sc.continuousPosRender) 0.35f else 1f
            if (sc.continuousPosRender) fsv.requestRender()
        }
        if (sc.continuousPosRender) {
            b.renderBackgroundSwitch.isClickable = !sc.continuousPosRender
            b.renderBackgroundLayout.alpha = if (sc.continuousPosRender) 0.35f else 1f
        }

        b.continuousRenderSwitch.isChecked =
                savedInstanceState?.getBoolean("continuousRender")
                        ?: sc.continuousPosRender


        b.renderBackgroundSwitch.isChecked = sc.renderBackground
        b.renderBackgroundSwitch.setOnClickListener {

            sc.renderBackground = b.renderBackgroundSwitch.isChecked

            fsv.r.renderBackgroundChanged = true
            fsv.r.renderToTex = b.renderBackgroundSwitch.isChecked
            fsv.requestRender()

        }


        b.unrestrictedParamsSwitch.isChecked = !sc.restrictParams
        b.unrestrictedParamsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sc.restrictParams = !isChecked
        }


//        fitToViewportSwitch.isChecked = sc.fitToViewport
//        fitToViewportSwitch.setOnCheckedChangeListener { _, isChecked ->
//
//            sc.fitToViewport = isChecked
//            act.updateSurfaceViewLayout()
//
//        }

        b.settingsDoneButton.setOnClickListener {
            listener?.closeSettingsMenu()
        }


//        showHintsSwitch.isChecked = sc.showHints
//        showHintsSwitch.setOnCheckedChangeListener { _, isChecked ->
//
//            sc.showHints = isChecked
//            act.updateHintVisibility()
//
//        }


        val editListener = { nextEditText: EditText?, setValueAndFormat: (w: EditText) -> Unit
            -> TextView.OnEditorActionListener { editText, actionId, _ ->

            when (actionId) {
                EditorInfo.IME_ACTION_NEXT -> {
                    setValueAndFormat(editText as EditText)
                    editText.clearFocus()
                    editText.isSelected = false
                    nextEditText?.requestFocus()
                }
                EditorInfo.IME_ACTION_DONE -> {
                    setValueAndFormat(editText as EditText)
                    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view?.windowToken, 0)
                    editText.clearFocus()
                    editText.isSelected = false
                    fsv.requestRender()
                }
                else -> {
                    Log.d("EQUATION FRAGMENT", "some other action")
                }
            }

            editText.clearFocus()
            listener?.updateSystemBarsVisibility()
            true

        }}
        b.targetFramerateValue.setText(sc.targetFramerate.toString())
        b.targetFramerateValue.setOnEditorActionListener(editListener(null) {
            val result = b.targetFramerateValue.text.toString().formatToDouble()
            if (result != null) {
                sc.targetFramerate = result.toInt().clamp(MIN_FRAMERATE, MAX_FRAMERATE)
            }
            b.targetFramerateValue.setText(sc.targetFramerate.toString())
        })


//        b.chunkProfileTabs.getTabAt(sc.chunkProfile.ordinal)?.apply {
//            view.setBackgroundColor(resources.getColor(R.color.divider, null))
//            select()
//        }
//        b.chunkProfileTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//
//            override fun onTabReselected(tab: TabLayout.Tab?) {}
//
//            override fun onTabUnselected(tab: TabLayout.Tab?) {
//
//                tab?.view?.setBackgroundColor(resources.getColor(R.color.menuDarkPrimary, null))
//
//            }
//
//            override fun onTabSelected(tab: TabLayout.Tab?) {
//
//                tab?.view?.setBackgroundColor(resources.getColor(R.color.divider, null))
//
//                sc.chunkProfile = ChunkProfile.values()[tab?.position ?: 0]
//                fsv.r.onChunkProfileChanged()
//
//            }
//
//        })


        b.splitTypeSwitch.isChecked = sc.useAlternateSplit
        b.splitTypeSwitch.setOnClickListener { v ->

            sc.useAlternateSplit = b.splitTypeSwitch.isChecked
            fsv.r.renderShaderChanged = true
            fsv.r.renderToTex = true
            fsv.requestRender()

        }


        b.allowSlowRendersSwitch.isChecked = sc.allowSlowDualfloat
        b.allowSlowRendersSwitch.setOnClickListener { buttonView ->
            sc.allowSlowDualfloat = b.allowSlowRendersSwitch.isChecked
            fsv.r.checkThresholdCross()
            if (f.shape.slowDualFloat && f.shape.position.zoom <= GpuPrecision.SINGLE.threshold) {
                fsv.r.renderToTex = true
                fsv.r.renderShaderChanged = true
                fsv.requestRender()
            }
        }
        b.allowSlowRendersHint.text = b.allowSlowRendersHint.text.toString().format(resources.getString(R.string.sine))



        b.advancedSettingsLayout.run { if (sc.advancedSettingsEnabled) enable() else disable(toggleAlpha = true) }
        b.advancedSettingsSwitch.isChecked = sc.advancedSettingsEnabled
        b.advancedSettingsSwitch.setOnClickListener {
            sc.advancedSettingsEnabled = b.advancedSettingsSwitch.isChecked
            if (sc.advancedSettingsEnabled) {
                b.advancedSettingsLayout.enable()
            } else {
                b.advancedSettingsLayout.disable(toggleAlpha = true)
                if ( b.allowSlowRendersSwitch    .isChecked ) b.allowSlowRendersSwitch    .performClick()
                if ( b.unrestrictedParamsSwitch  .isChecked ) b.unrestrictedParamsSwitch  .performClick()
                if ( b.ultraHighResSwitch        .isChecked ) b.ultraHighResSwitch        .performClick()
            }
        }

        b.ultraHighResSwitch.isChecked = sc.ultraHighResolutions
        if (sc.ultraHighResolutions) listener?.enableUltraHighRes() else listener?.disableUltraHighRes()
        b.ultraHighResSwitch.setOnClickListener {
            sc.ultraHighResolutions = b.ultraHighResSwitch.isChecked
            if (sc.ultraHighResolutions) act.enableUltraHighRes() else act.disableUltraHighRes()
        }
        b.ultraHighResHint.text = b.ultraHighResHint.text.toString().format(
            Resolution.ultraHigh.last().w,
            Resolution.ultraHigh.last().h
        )



        if (sc.goldEnabled) onGoldEnabled()

        super.onViewCreated(v, savedInstanceState)

    }


    fun onGoldEnabled() {}

}