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


class EquationFragment : Fragment() {

    private lateinit var callback : OnParamChangeListener
    private lateinit var complexMapSpinner : Spinner

    lateinit var initConfig : EquationConfig

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {

        val v = inflater.inflate(R.layout.equation_fragment, container, false)

        complexMapSpinner = v.findViewById(R.id.complexMapSpinner)
        complexMapSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = parent?.getItemAtPosition(position).toString()
                Log.d("TEST", "item is -$item-")
                callback.onEquationParamsChanged(
                        "map",
                        ComplexMap.all[item]?.invoke(resources) ?: initConfig.map()
                )
            }

        }

        val complexMapAdapter = ArrayAdapter<String>(
                v.context,
                android.R.layout.simple_spinner_item,
                List(ComplexMap.all.size) { i: Int -> ComplexMap.all.keys.elementAt(i) }
        )
        complexMapAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        complexMapSpinner.adapter = complexMapAdapter
        complexMapSpinner.setSelection(ComplexMap.all.keys.indexOf(
                savedInstanceState?.getString("map") ?: initConfig.map().name)
        )

        return v
    }

    fun setOnParamChangeListener(callback: OnParamChangeListener) {
        this.callback = callback
    }

    override fun onSaveInstanceState(outState: Bundle) {

        super.onSaveInstanceState(outState)
    }

    interface OnParamChangeListener {
        fun onEquationParamsChanged(key: String, value: Any)
    }

}