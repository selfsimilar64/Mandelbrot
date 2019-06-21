package com.example.matt.gputest

import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.settings_fragment.*


class SettingsFragment : Fragment() {

    private lateinit var callback : OnParamChangeListener
    
    lateinit var config : SettingsConfig
    lateinit var resolutionTabs : TabLayout
    lateinit var precisionTabs : TabLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {

        val v = inflater.inflate(R.layout.settings_fragment, container, false)


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


        val continuousRenderSwitch = v.findViewById<Switch>(R.id.continuousRenderSwitch)
        continuousRenderSwitch.setOnCheckedChangeListener {
            buttonView, isChecked -> callback.onSettingsParamsChanged("continuousRender", isChecked)
        }
        continuousRenderSwitch.isChecked =
                savedInstanceState?.getBoolean("continuousRender")
                ?: config.continuousRender()

        val displayParamsSwitch = v.findViewById<Switch>(R.id.displayParamsSwitch)
        displayParamsSwitch.setOnCheckedChangeListener {
            buttonView, isChecked -> callback.onSettingsParamsChanged("displayParams", isChecked)
        }
        displayParamsSwitch.isChecked =
                savedInstanceState?.getBoolean("displayParams")
                ?: config.displayParams()

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