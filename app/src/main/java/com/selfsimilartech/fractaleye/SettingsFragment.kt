package com.selfsimilartech.fractaleye

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
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

        val cardHeaderListener = { cardBody: LinearLayout -> View.OnClickListener {
            if (cardBody.layoutParams.height == 0) {
                cardBody.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                (it as Button).setCompoundDrawablesWithIntrinsicBounds(null, null, resources.getDrawable(R.drawable.arrow_collapse, null), null)
                Log.w("FRACTAL", "was 0")
            }
            else {
                cardBody.layoutParams.height = 0
                (it as Button).setCompoundDrawablesWithIntrinsicBounds(null, null, resources.getDrawable(R.drawable.arrow_expand, null), null)
                Log.w("FRACTAL", "was open")
            }
            cardBody.requestLayout()
            cardBody.invalidate()
        }}

        val renderCardBody = v.findViewById<LinearLayout>(R.id.renderCardBody)
        val uiCardBody = v.findViewById<LinearLayout>(R.id.uiCardBody)

        val renderHeaderButton = v.findViewById<Button>(R.id.renderHeaderButton)
        val uiHeaderButton = v.findViewById<Button>(R.id.uiHeaderButton)

        renderHeaderButton.setOnClickListener(cardHeaderListener(renderCardBody))
        uiHeaderButton.setOnClickListener(cardHeaderListener(uiCardBody))

        renderHeaderButton.performClick()
        uiHeaderButton.performClick()


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
        precisionTabs.getTabAt(
                Precision.valueOf(
                        savedInstanceState?.getString("precision")
                                ?: config.precision().name
                ).ordinal
        )?.select()

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