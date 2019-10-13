package com.selfsimilartech.fractaleye

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Rect
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v7.widget.CardView
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch


class SettingsFragment : Fragment() {

    private lateinit var callback : OnParamChangeListener
    
    lateinit var config : SettingsConfig
    private lateinit var resolutionTabs : TabLayout
    private lateinit var precisionTabs : TabLayout
    private lateinit var continuousRenderSwitch : Switch
    private lateinit var displayParamsSwitch : Switch

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
                Log.d("FRACTAL FRAGMENT", "intermediate height: $intermediateHeight")
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
                    Log.d("FRACTAL FRAGMENT", "scroll to: ${(cardBody.parent.parent as CardView).y.toInt()}")
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

        renderCardBody.layoutParams.height = 0
        uiCardBody.layoutParams.height = 0


        resolutionTabs = v.findViewById(R.id.resolutionTabs)
        resolutionTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                val res = tab.text.toString()
                callback.onSettingsParamsChanged("resolution", Resolution.valueOf(res))
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }

        })
        resolutionTabs.getTabAt(
            Resolution.valueOf(
                savedInstanceState?.getString("resolution")
                ?: config.resolution().name
            ).ordinal
        )?.select()


        continuousRenderSwitch = v.findViewById(R.id.continuousRenderSwitch)
        continuousRenderSwitch.setOnCheckedChangeListener {
            _, isChecked -> callback.onSettingsParamsChanged("continuousRender", isChecked)
        }
        continuousRenderSwitch.isChecked =
                savedInstanceState?.getBoolean("continuousRender")
                ?: config.continuousRender()


        val saveToFileButton = v.findViewById<Button>(R.id.saveToFileButton)
        saveToFileButton.setOnClickListener {
            callback.onSettingsParamsChanged("saveToFile", true)
        }
        val renderButton = v.findViewById<Button>(R.id.renderButton)
        renderButton.setOnClickListener {
            callback.onSettingsParamsChanged("render", true)
        }


        displayParamsSwitch = v.findViewById(R.id.displayParamsSwitch)
        displayParamsSwitch.setOnCheckedChangeListener { _, isChecked ->
            callback.onSettingsParamsChanged("displayParams", isChecked)
        }
        displayParamsSwitch.isChecked =
                savedInstanceState?.getBoolean("displayParams")
                ?: config.displayParams()


        precisionTabs = v.findViewById(R.id.precisionTabs)
        precisionTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                val p = tab.text.toString()
                callback.onSettingsParamsChanged("precision", Precision.valueOf(p))
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }

        })
        precisionTabs.getTabAt(2)?.select()

        return v
    }

    fun setOnParamChangeListener(callback: OnParamChangeListener) {
        this.callback = callback
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
                "resolution",
                resolutionTabs.getTabAt(resolutionTabs.selectedTabPosition)?.text.toString()
        )
        outState.putString(
                "precision",
                precisionTabs.getTabAt(precisionTabs.selectedTabPosition)?.text.toString()
        )
        outState.putBoolean(
                "continuousRender",
                continuousRenderSwitch.isChecked
        )
        outState.putBoolean(
                "displayParams",
                displayParamsSwitch.isChecked
        )
        super.onSaveInstanceState(outState)
    }

    interface OnParamChangeListener {
        fun onSettingsParamsChanged(key: String, value: Any)
    }

}