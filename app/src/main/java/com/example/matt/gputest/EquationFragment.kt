package com.example.matt.gputest

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.content.Context.INPUT_METHOD_SERVICE
import android.view.inputmethod.InputMethodManager
import kotlin.math.pow


class EquationFragment : Fragment() {

    private lateinit var callback : OnParamChangeListener
    private lateinit var complexMapSpinner : Spinner

    lateinit var config : EquationConfig

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {

        Log.d("EQUATION FRAGMENT", "creating...")
        val v = inflater.inflate(R.layout.equation_fragment, container, false)

        complexMapSpinner = v.findViewById(R.id.complexMapSpinner)
        complexMapSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = parent?.getItemAtPosition(position).toString()
                callback.onEquationParamsChanged(
                        "map",
                        ComplexMap.all[item]?.invoke(resources) ?: config.map()
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
                savedInstanceState?.getString("map") ?: config.map().name)
        )

        val xCoordEdit = v.findViewById<EditText>(R.id.xCoordEdit)
        val yCoordEdit = v.findViewById<EditText>(R.id.yCoordEdit)
        val scaleSignificandEdit = v.findViewById<EditText>(R.id.scaleSignificandEdit)
        val scaleExponentEdit = v.findViewById<EditText>(R.id.scaleExponentEdit)
        val bailoutSignificandEdit = v.findViewById<EditText>(R.id.bailoutSignificandEdit)
        val bailoutExponentEdit = v.findViewById<EditText>(R.id.bailoutExponentEdit)

        val editListenerNext = {
            editText: EditText, nextEditText: EditText, key: String, value: (w: TextView)->Any -> TextView.OnEditorActionListener {
                w, actionId, event -> when (actionId) {
                    EditorInfo.IME_ACTION_NEXT -> {
                        callback.onEquationParamsChanged(key, value(w))
                        editText.clearFocus()
                        editText.isSelected = false

                        nextEditText.requestFocus()
                        true
                    }
                    else -> {
                        Log.d("EQUATION FRAGMENT", "some other action")
                        false
                    }
                }
            }
        }
        val editListenerDone = {
            editText: EditText, key: String, value: (w: TextView)->Any -> TextView.OnEditorActionListener {
                w, actionId, event -> when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        callback.onEquationParamsChanged(key, value(w))
                        val imm = v.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(v.windowToken, 0)
                        editText.clearFocus()
                        editText.isSelected = false
                        v.findViewById<LinearLayout>(R.id.positionParams).requestLayout()
                        true
                    }
                    else -> {
                        Log.d("EQUATION FRAGMENT", "some other action")
                        false
                    }
                }
            }
        }

        xCoordEdit.setOnEditorActionListener(editListenerNext(
                xCoordEdit,
                yCoordEdit,
                "coords") { w: TextView ->
            doubleArrayOf(
                    w.text.toString().toDouble(),
                    config.coords()[1]
            )
        }
        )
        yCoordEdit.setOnEditorActionListener(editListenerNext(
                yCoordEdit,
                scaleSignificandEdit,
                "coords") { w: TextView ->
            doubleArrayOf(
                    config.coords()[0],
                    w.text.toString().toDouble()
            )
        }
        )
        scaleSignificandEdit.setOnEditorActionListener(editListenerNext(
                scaleSignificandEdit,
                scaleExponentEdit,
                "scale") { w: TextView ->
            val aspectRatio = config.scale()[1]/config.scale()[0]
            val s = "${w.text}e${scaleExponentEdit.text}".toDouble()
            doubleArrayOf(s, s*aspectRatio)
        }
        )
        scaleExponentEdit.setOnEditorActionListener(editListenerNext(
                scaleExponentEdit,
                bailoutSignificandEdit,
                "scale") { w: TextView ->
            val aspectRatio = config.scale()[1]/config.scale()[0]
            val s = "${scaleSignificandEdit.text}e${w.text}".toDouble()
            doubleArrayOf(s, s*aspectRatio)
        }
        )
        bailoutSignificandEdit.setOnEditorActionListener(editListenerNext(
                bailoutSignificandEdit,
                bailoutExponentEdit,
                "bailoutRadius") { w: TextView -> "${w.text}e${bailoutExponentEdit.text}".toFloat() }
        )
        bailoutExponentEdit.setOnEditorActionListener(editListenerDone(
                bailoutExponentEdit,
                "bailoutRadius") { w: TextView -> "${bailoutSignificandEdit.text}e${w.text}".toFloat() }
        )



        val maxIterBar = v.findViewById<SeekBar>(R.id.maxIterBar)
        maxIterBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val p = seekBar.progress.toFloat() / 100.0f
                callback.onEquationParamsChanged(
                    "maxIter",
                    ((2.0.pow(5) - 1)*(1.0f - p) + (2.0.pow(11) - 1)*p).toInt()
                )
            }

        })
        maxIterBar.progress = 20

        return v
    }

    fun setOnParamChangeListener(callback: OnParamChangeListener) {
        this.callback = callback
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("EQUATION FRAGMENT", "...created")
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        Log.d("EQUATION FRAGMENT", "resuming...")
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("map", complexMapSpinner.selectedItem.toString())
        super.onSaveInstanceState(outState)
    }

    interface OnParamChangeListener {
        fun onEquationParamsChanged(key: String, value: Any)
    }

}