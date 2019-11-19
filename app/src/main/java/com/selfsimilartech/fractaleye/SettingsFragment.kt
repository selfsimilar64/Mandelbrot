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


class SettingsFragment : Fragment() {
    
    lateinit var f : Fractal
    lateinit var fsv : FractalSurfaceView
    lateinit var sc : SettingsConfig

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {

        val v = inflater.inflate(R.layout.settings_fragment, container, false)

        val settingsScroll = v.findViewById<ScrollView>(R.id.settingsScroll)
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
                (it as Button).setCompoundDrawablesWithIntrinsicBounds(null, null, resources.getDrawable(R.drawable.arrow_collapse, null), null)

            }
            else {

                hStart = cardBody.height
                hEnd = 0
                (it as Button).setCompoundDrawablesWithIntrinsicBounds(null, null, resources.getDrawable(R.drawable.arrow_expand, null), null)

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

        val renderCardBody = v.findViewById<LinearLayout>(R.id.renderCardBody)
        val uiCardBody = v.findViewById<LinearLayout>(R.id.uiCardBody)

        val renderHeaderButton = v.findViewById<Button>(R.id.renderHeaderButton)
        val uiHeaderButton = v.findViewById<Button>(R.id.uiHeaderButton)

        renderHeaderButton.setOnClickListener(cardHeaderListener(renderCardBody))
        uiHeaderButton.setOnClickListener(cardHeaderListener(uiCardBody))

//        renderCardBody.layoutParams.height = 0
//        uiCardBody.layoutParams.height = 0


        val resolutionTabs = v.findViewById<TabLayout>(R.id.resolutionTabs)
        resolutionTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                val res = tab.text.toString()
                sc.resolution = Resolution.valueOf(res)
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
            ).ordinal
        )?.select()


        val continuousRenderSwitch = v.findViewById<Switch>(R.id.continuousRenderSwitch)
        continuousRenderSwitch.setOnCheckedChangeListener { _, isChecked ->
            sc.continuousRender = isChecked
        }
        continuousRenderSwitch.isChecked =
                savedInstanceState?.getBoolean("continuousRender")
                ?: sc.continuousRender


        val saveToFileButton = v.findViewById<Button>(R.id.saveToFileButton)
        saveToFileButton.setOnClickListener {
            if (fsv.r.isRendering) {
                val toast = Toast.makeText(v.context, "Please wait for the image to finish rendering", Toast.LENGTH_SHORT)
                toast.show()
            }
            else {
                if (ContextCompat.checkSelfPermission(v.context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions((activity as MainActivity),
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            WRITE_STORAGE_REQUEST_CODE)
                }
                else {
                    fsv.r.saveImage = true
                    fsv.requestRender()
                    val toast = Toast.makeText(v.context, "Image saved to Gallery", Toast.LENGTH_SHORT)
                    toast.show()
                }
            }
        }
        val renderButton = v.findViewById<Button>(R.id.renderButton)
        renderButton.setOnClickListener {
            fsv.r.renderToTex = true
            fsv.requestRender()
        }


        val displayParamsSwitch = v.findViewById<Switch>(R.id.displayParamsSwitch)
        displayParamsSwitch.isChecked =
                savedInstanceState?.getBoolean("displayParams")
                ?: sc.displayParams
        displayParamsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sc.displayParams = isChecked
            (activity as MainActivity).updateDisplayParams(fsv.reaction, settingsChanged = true)
        }



        val precisionTabs = v.findViewById<TabLayout>(R.id.precisionTabs)
        precisionTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                val p = tab.text.toString()
                if (p == "AUTO") {
                    Log.d("SETTINGS FRAGMENT", "auto selected")
                    sc.autoPrecision = true
                    fsv.checkThresholdCross(f.map.position.scale)
                }
                else {
                    sc.precision = Precision.valueOf(p)
                    sc.autoPrecision = false
                }
                fsv.r.renderShaderChanged = true
                fsv.r.renderToTex = true
                fsv.requestRender()

            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }

        })
        if (sc.autoPrecision) precisionTabs.getTabAt(2)?.select()
        else precisionTabs.getTabAt(
                 Precision.valueOf(
                     savedInstanceState?.getString("precision")
                     ?: sc.precision.name
                 ).ordinal
             )?.select()

        return v
    }


    interface OnParamChangeListener {
        fun onSettingsParamsChanged(key: String, value: Any)
    }

}