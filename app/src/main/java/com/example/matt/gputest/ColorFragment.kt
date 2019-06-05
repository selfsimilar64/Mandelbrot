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

    lateinit var initConfig : ColorConfig


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {

        val v = inflater.inflate(R.layout.color_fragment, container, false)



        colorAlgSpinner = v.findViewById(R.id.colorAlgSpinner)
        colorAlgSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = parent?.getItemAtPosition(position).toString()
                callback.onColorParamsChanged(
                        "algorithm",
                        ColorAlgorithm.all[item]?.invoke(resources) ?: initConfig.algorithm()
                )
            }

        }

        val colorAlgAdapter = ArrayAdapter<String>(
                v.context,
                android.R.layout.simple_spinner_item,
                List(ColorAlgorithm.all.size) { i: Int -> ColorAlgorithm.all.keys.elementAt(i) }
        )
        colorAlgAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorAlgSpinner.adapter = colorAlgAdapter
        Log.d("COLOR FRAGMENT", "algorithm is ${initConfig.algorithm().name}")
        colorAlgSpinner.setSelection(ColorAlgorithm.all.keys.indexOf(
                savedInstanceState?.getString("algorithm") ?: initConfig.algorithm().name)
        )
        return v
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
                "algorithm",
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