package com.example.matt.gputest

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner


class ColorFragment : Fragment() {

    private lateinit var callback : OnParamChangeListener
    private lateinit var colorPaletteSpinner : Spinner

    lateinit var config : ColorConfig


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {

        val v = inflater.inflate(R.layout.color_fragment, container, false)


        colorPaletteSpinner = v.findViewById(R.id.colorPaletteSpinner)
        colorPaletteSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = parent?.getItemAtPosition(position).toString()
                callback.onColorParamsChanged(
                        "palette",
                        ColorPalette.all[item] ?: config.palette()
                )
            }

        }

        val colorPaletteAdapter = ArrayAdapter(
                v.context,
                android.R.layout.simple_spinner_item,
                List(ColorPalette.all.size) { i: Int -> ColorPalette.all.keys.elementAt(i) }
        )
        colorPaletteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorPaletteSpinner.adapter = colorPaletteAdapter
        colorPaletteSpinner.setSelection(ColorPalette.all.keys.indexOf(
                savedInstanceState?.getString("palette") ?: config.palette().name)
        )
        
        return v
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
                "palette",
                colorPaletteSpinner.selectedItem.toString()
        )
        super.onSaveInstanceState(outState)
    }

    fun setOnParamChangeListener(callback: OnParamChangeListener) {
        this.callback = callback
    }

    interface OnParamChangeListener {
        fun onColorParamsChanged(key: String, value: Any)
    }

}