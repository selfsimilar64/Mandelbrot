package com.example.matt.gputest

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.settings_fragment.*


class SettingsFragment : Fragment() {

    private lateinit var callback : OnParamChangeListener
    lateinit var initParams : Map<String, String>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {

        val v = inflater.inflate(R.layout.settings_fragment, container, false)

        val resolutionTabs = v.findViewById<TabLayout>(R.id.resolutionTabs)
        resolutionTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                val res = tab.text.toString()
                callback.onSettingsParamsChanged("resolution", res)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }

        })
        resolutionTabs.getTabAt(
            Resolution.valueOf(
                savedInstanceState?.getString("resolution")
                ?: initParams["resolution"] ?: ""
            ).ordinal
        )?.select()

        val precisionTabs = v.findViewById<TabLayout>(R.id.precisionTabs)
        precisionTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                val p = tab.text.toString()
                callback.onSettingsParamsChanged("precision", p)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }

        })

        val p = savedInstanceState?.getString("precision") ?: initParams["precision"] ?: ""

        precisionTabs.getTabAt(
            if (p == "AUTO") 2 else Precision.valueOf(p).ordinal
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
                precisionTabs.getTabAt(precisionTabs.selectedTabPosition)?.text.toString())
        super.onSaveInstanceState(outState)
    }

    interface OnParamChangeListener {
        fun onSettingsParamsChanged(key: String, value: String)
    }

}