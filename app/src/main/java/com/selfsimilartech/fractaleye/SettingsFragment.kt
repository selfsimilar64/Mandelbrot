package com.selfsimilartech.fractaleye

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlin.math.roundToInt
import kotlinx.android.synthetic.main.settings_fragment.*


class SettingsFragment : Fragment() {
    
    lateinit var f : Fractal
    lateinit var fsv : FractalSurfaceView
    lateinit var sc : SettingsConfig

    fun passArguments(f: Fractal, fsv: FractalSurfaceView, sc: SettingsConfig) {
        this.f = f
        this.fsv = fsv
        this.sc = sc
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val act = if (activity is MainActivity) activity as MainActivity else null
        if (!this::f.isInitialized) f = act!!.f
        if (!this::fsv.isInitialized) fsv = act!!.fsv
        if (!this::sc.isInitialized) sc = act!!.sc
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {

        return inflater.inflate(R.layout.settings_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        val act = activity as? MainActivity ?: null

        val cardHeaderListener = { cardBody: LinearLayout -> View.OnClickListener {

            val card = cardBody.parent.parent as CardView
            val initScrollY = settingsScroll.scrollY
            val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15f, context?.resources?.displayMetrics).toInt()

            val hStart : Int
            val hEnd : Int


            if (cardBody.layoutParams.height == 0) {

                // measure card body height
                val matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec((cardBody.parent as View).width, View.MeasureSpec.EXACTLY)
                val wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                cardBody.measure(matchParentMeasureSpec, wrapContentMeasureSpec)

                hStart = 0
                hEnd = cardBody.measuredHeight
                (it as Button).setCompoundDrawablesWithIntrinsicBounds(null, null, resources.getDrawable(R.drawable.expand, null), null)

            }
            else {

                hStart = cardBody.height
                hEnd = 0
                (it as Button).setCompoundDrawablesWithIntrinsicBounds(null, null, resources.getDrawable(R.drawable.collapse, null), null)

            }

            val anim = ValueAnimator.ofInt(hStart, hEnd)
            anim.addUpdateListener { animation ->
                val intermediateHeight = animation?.animatedValue as Int
                // Log.d("FRACTAL FRAGMENT", "intermediate height: $intermediateHeight")
                cardBody.layoutParams.height = intermediateHeight
                cardBody.requestLayout()

                if (hStart == 0) {  // only scroll on card expansion

                    // get scrollView dimensions and card coordinates in scrollView
                    val scrollBounds = Rect()
                    settingsScroll.getDrawingRect(scrollBounds)
                    var top = 0f
                    var temp = card as View
                    while (temp !is ScrollView){
                        top += (temp).y
                        temp = temp.parent as View
                    }
                    val bottom = (top + card.height).toInt()

                    if (scrollBounds.top > top - px) {  // card top is not visible
                        settingsScroll.scrollY = ((1f - animation.animatedFraction)*initScrollY).toInt() + (animation.animatedFraction*(top - px)).toInt()  // scroll to top of card
                    }
                    else {  // card top is visible
                        if (scrollBounds.bottom < bottom && card.height < settingsScroll.height - px) { // card bottom is not visible and there is space between card top and scrollView top
                            settingsScroll.scrollY = bottom - settingsScroll.height  // scroll to show bottom
                        }
                    }
                    // Log.d("FRACTAL FRAGMENT", "scroll to: ${(cardBody.parent.parent as CardView).y.toInt()}")
                }


            }

            // change cardBody height back to wrap_content after expansion
            if (cardBody.layoutParams.height == 0) {
                anim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                        cardBody.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                    }
                })
            }

            anim.duration = 250
            anim.start()

        }}


        resolutionTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                val res = tab.text.toString()
                sc.resolution = Resolution.valueOf(res)
                val width = (fsv.screenRes[0].toFloat()/sc.resolution.scale.toFloat()).roundToInt()
                val height = (fsv.screenRes[1].toFloat()/sc.resolution.scale.toFloat()).roundToInt()
                resolutionDimensionsText.text = "$width x $height"
                fsv.r.resolutionChanged = true
                fsv.r.renderToTex = true
                fsv.requestRender()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }

        })
        resolutionTabs.getTabAt(
                Resolution.valueOf(
                        savedInstanceState?.getString("resolution")
                                ?: sc.resolution.name
                ).ordinal - 1
        )?.select()


        continuousRenderSwitch.setOnCheckedChangeListener { _, isChecked ->
            sc.continuousRender = isChecked
        }
        continuousRenderSwitch.isChecked =
                savedInstanceState?.getBoolean("continuousRender")
                        ?: sc.continuousRender


        saveToFileButton.setOnClickListener {
            if (fsv.r.isRendering) act?.showMessage("Please wait for the image to finish rendering")
            else {
                if (ContextCompat.checkSelfPermission(v.context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            act!!,
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            WRITE_STORAGE_REQUEST_CODE)
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


        displayParamsSwitch.isChecked =
                savedInstanceState?.getBoolean("displayParams")
                        ?: sc.displayParams
        displayParamsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sc.displayParams = isChecked
            act?.updateDisplayParams(settingsChanged = true)
        }



        precisionTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {

                val p = tab.text.toString()
                if (p == "AUTO") {
                    Log.d("SETTINGS FRAGMENT", "auto selected")
                    sc.autoPrecision = true
                    fsv.checkThresholdCross(f.position.scale)
                }
                else {
                    sc.precision = Precision.valueOf(p)
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