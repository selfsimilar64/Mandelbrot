package com.example.matt.gputest

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast



class ColorFragment : Fragment() {

    private lateinit var callback : OnParamChangeListener
    private lateinit var colorAlgSpinner : Spinner
    lateinit var initParams : Map<String, Any>

    private val colorAlgOptions : List<String> = arrayListOf(
            "Escape Time",
            "Escape Time Smooth",
            "Triangle Inequality Average",
            "Curvature Average",
            "Stripe Average",
            "Overlay Average"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {

        val v = inflater.inflate(R.layout.color_fragment, container, false)

        colorAlgSpinner = v.findViewById(R.id.colorAlgSpinner)
        colorAlgSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = parent?.getItemAtPosition(position).toString()
                callback.onColorParamsChanged("colorAlg", item)
            }

        }

        val colorAlgAdapter = ArrayAdapter<String>(
                v.context,
                android.R.layout.simple_spinner_item,
                colorAlgOptions
        )
        colorAlgAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorAlgSpinner.adapter = colorAlgAdapter
        colorAlgSpinner.setSelection(colorAlgOptions.indexOf(
                savedInstanceState?.getString("colorAlg") ?: initParams["colorAlg"])
        )
        return v
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
                "colorAlg",
                colorAlgSpinner.selectedItem.toString()
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