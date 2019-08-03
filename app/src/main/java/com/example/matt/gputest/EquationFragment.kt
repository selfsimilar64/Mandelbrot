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
    private lateinit var textureAlgSpinner : Spinner
    private lateinit var juliaModeSwitch : Switch

    lateinit var config : FractalConfig

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {

        Log.d("EQUATION FRAGMENT", "creating...")
        val v = inflater.inflate(R.layout.equation_fragment, container, false)

        val algContainer = v.findViewById<LinearLayout>(R.id.algContainer)
        val complexMapRow = v.findViewById<LinearLayout>(R.id.complexMapRow)
        val functionLayout = v.findViewById<LinearLayout>(R.id.functionLayout)
        val textureRow = v.findViewById<LinearLayout>(R.id.textureRow)
        val paramEditRows = listOf<LinearLayout>(
                v.findViewById(R.id.p1EditRow),
                v.findViewById(R.id.p2EditRow),
                v.findViewById(R.id.p3EditRow),
                v.findViewById(R.id.p4EditRow)
        )



        val editListenerNext = {
            editText: EditText, nextEditText: EditText, key: String, value: (w: TextView)-> Any -> TextView.OnEditorActionListener {
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


        // POSITION EDIT FIELDS
        val xCoordEdit = v.findViewById<EditText>(R.id.xCoordEdit)
        val yCoordEdit = v.findViewById<EditText>(R.id.yCoordEdit)
        xCoordEdit.setText("%.17f".format(config.coords()[0]))
        yCoordEdit.setText("%.17f".format(config.coords()[1]))
        xCoordEdit.setOnEditorActionListener(editListenerNext(xCoordEdit, yCoordEdit, "coords") { w: TextView ->
            doubleArrayOf(
                    w.text.toString().toDouble(),
                    config.coords()[1]
            )
        })
        yCoordEdit.setOnEditorActionListener(editListenerDone(yCoordEdit, "coords") { w: TextView ->
            doubleArrayOf(
                    config.coords()[0],
                    w.text.toString().toDouble()
            )
        })

        val scaleStrings = "%e".format(config.scale()[0]).split("e")
        val scaleSignificandEdit = v.findViewById<EditText>(R.id.scaleSignificandEdit)
        val scaleExponentEdit = v.findViewById<EditText>(R.id.scaleExponentEdit)
        scaleSignificandEdit.setText(scaleStrings[0])
        scaleExponentEdit.setText(scaleStrings[1])
        scaleSignificandEdit.setOnEditorActionListener(
                editListenerNext(scaleSignificandEdit, scaleExponentEdit, "scale") { w: TextView ->
                    val aspectRatio = config.scale()[1]/config.scale()[0]
                    val s = "${w.text}e${scaleExponentEdit.text}".toDouble()
                    doubleArrayOf(s, s*aspectRatio)
                })
        scaleExponentEdit.setOnEditorActionListener(
                editListenerDone(scaleExponentEdit, "scale") { w: TextView ->
                    val aspectRatio = config.scale()[1]/config.scale()[0]
                    val s = "${scaleSignificandEdit.text}e${w.text}".toDouble()
                    doubleArrayOf(s, s*aspectRatio)
                })

        val bailoutStrings = "%e".format(config.bailoutRadius()).split("e")
        val bailoutSignificandEdit = v.findViewById<EditText>(R.id.bailoutSignificandEdit)
        val bailoutExponentEdit = v.findViewById<EditText>(R.id.bailoutExponentEdit)
        bailoutSignificandEdit.setText(bailoutStrings[0])
        bailoutExponentEdit.setText(bailoutStrings[1])
        bailoutSignificandEdit.setOnEditorActionListener(
                editListenerNext(bailoutSignificandEdit, bailoutExponentEdit, "bailoutRadius") {
                    w: TextView -> "${w.text}e${bailoutExponentEdit.text}".toFloat()
                })
        bailoutExponentEdit.setOnEditorActionListener(
                editListenerDone(bailoutExponentEdit, "bailoutRadius") {
                    w: TextView -> "${bailoutSignificandEdit.text}e${w.text}".toFloat()
                })


        // PARAMETER EDIT FIELDS
        val p1xEdit = v.findViewById<EditText>(R.id.p1xEdit)
        val p1yEdit = v.findViewById<EditText>(R.id.p1yEdit)
        p1xEdit.setText("%.8f".format(config.p1()[0]))
        p1yEdit.setText("%.8f".format(config.p1()[1]))
        p1xEdit.setOnEditorActionListener(editListenerNext(p1xEdit, p1yEdit, "p1") {
            w: TextView -> doubleArrayOf("${w.text}".toDouble(), "${p1yEdit.text}".toDouble())
        })
        p1yEdit.setOnEditorActionListener(editListenerDone(p1yEdit, "p1") {
            w: TextView -> doubleArrayOf("${p1xEdit.text}".toDouble(), "${w.text}".toDouble())
        })

        val p2xEdit = v.findViewById<EditText>(R.id.p2xEdit)
        val p2yEdit = v.findViewById<EditText>(R.id.p2yEdit)
        p2xEdit.setText("%.8f".format(config.p2()[0]))
        p2yEdit.setText("%.8f".format(config.p2()[1]))
        p2xEdit.setOnEditorActionListener(editListenerNext(p2xEdit, p2yEdit, "p2") {
            w: TextView -> doubleArrayOf("${w.text}".toDouble(), "${p2yEdit.text}".toDouble())
        })
        p2yEdit.setOnEditorActionListener(editListenerDone(p1yEdit, "p2") {
            w: TextView -> doubleArrayOf("${p2xEdit.text}".toDouble(), "${w.text}".toDouble())
        })

        val p3xEdit = v.findViewById<EditText>(R.id.p3xEdit)
        val p3yEdit = v.findViewById<EditText>(R.id.p3yEdit)
        p3xEdit.setText("%.8f".format(config.p3()[0]))
        p3yEdit.setText("%.8f".format(config.p3()[1]))
        p3xEdit.setOnEditorActionListener(editListenerNext(p3xEdit, p3yEdit, "p3") {
            w: TextView -> doubleArrayOf("${w.text}".toDouble(), "${p3yEdit.text}".toDouble())
        })
        p3yEdit.setOnEditorActionListener(editListenerDone(p1yEdit, "p3") {
            w: TextView -> doubleArrayOf("${p3xEdit.text}".toDouble(), "${w.text}".toDouble())
        })

        val p4xEdit = v.findViewById<EditText>(R.id.p4xEdit)
        val p4yEdit = v.findViewById<EditText>(R.id.p4yEdit)
        p4xEdit.setText("%.8f".format(config.p4()[0]))
        p4yEdit.setText("%.8f".format(config.p4()[1]))
        p4xEdit.setOnEditorActionListener(editListenerNext(p4xEdit, p4yEdit, "p4") {
            w: TextView -> doubleArrayOf("${w.text}".toDouble(), "${p4yEdit.text}".toDouble())
        })
        p4yEdit.setOnEditorActionListener(editListenerDone(p1yEdit, "p4") {
            w: TextView -> doubleArrayOf("${p4xEdit.text}".toDouble(), "${w.text}".toDouble())
        })





        for (i in 0 until 4) {
            // Log.d("FRACTAL FRAGMENT", "removing param row ${i + 1}")
            functionLayout.removeView(paramEditRows[i])
        }



        // JULIA MODE SWITCH
         val juliaListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
             callback.onEquationParamsChanged("juliaMode", isChecked)
             val juliaParamIndex = config.numParamsInUse() - 1
             Log.d("FRACTAL FRAGMENT", "juliaParamIndex: $juliaParamIndex")
             val juliaSwitchLayoutIndex = algContainer.indexOfChild(juliaModeSwitch)
             if (isChecked) {
                 algContainer.removeView(paramEditRows[juliaParamIndex])
                 algContainer.addView(paramEditRows[juliaParamIndex], juliaSwitchLayoutIndex + 1)
             } else {
                 algContainer.removeView(paramEditRows[juliaParamIndex + 1])
             }
         }
        juliaModeSwitch = v.findViewById(R.id.juliaModeSwitch)
        if (config.map().initJuliaMode) { algContainer.removeView(juliaModeSwitch) }
        else { juliaModeSwitch.setOnCheckedChangeListener(juliaListener) }


        // COMPLEX MAP SELECTION
        complexMapSpinner = v.findViewById(R.id.complexMapSpinner)
        complexMapSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = parent?.getItemAtPosition(position).toString()
                val nextMap = ComplexMap.all[item]?.invoke(resources) ?: ComplexMap.empty()
                if (nextMap != config.map()) {
                    if (juliaModeSwitch.isChecked && !config.map().initJuliaMode) {
                        val juliaParamIndex = config.numParamsInUse() - 1
                        juliaModeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->  }
                        juliaModeSwitch.isChecked = false
                        algContainer.removeView(paramEditRows[juliaParamIndex])
                        config.params["juliaMode"] = false
                        juliaModeSwitch.setOnCheckedChangeListener(juliaListener)
                    }
                }
                val mapLayoutIndex = functionLayout.indexOfChild(complexMapRow)
                for (i in 0 until config.map().initMapParams.size) {
                    // Log.d("FRACTAL FRAGMENT", "MAP -- removing row ${i + 1}")
                    functionLayout.removeView(paramEditRows[i])
                }
                for (i in 0 until (ComplexMap.all[item]?.invoke(resources) ?: ComplexMap.empty()).initMapParams.size) {
                    // Log.d("FRACTAL FRAGMENT", "MAP -- adding row ${i + 1} at index ${mapLayoutIndex + i + 1}")
                    functionLayout.addView(paramEditRows[i], mapLayoutIndex + i + 1)
                }
                callback.onEquationParamsChanged(
                        "map",
                        ComplexMap.all[item]?.invoke(resources) ?: config.map()
                )
            }

        }

        val complexMapAdapter = ArrayAdapter(
                v.context,
                android.R.layout.simple_spinner_item,
                List(ComplexMap.all.size) { i: Int -> ComplexMap.all.keys.elementAt(i) }
        )
        complexMapAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        complexMapSpinner.adapter = complexMapAdapter
        complexMapSpinner.setSelection(ComplexMap.all.keys.indexOf(
                savedInstanceState?.getString("map") ?: config.map().name)
        )

        // TEXTURE SELECTION
        textureAlgSpinner = v.findViewById(R.id.textureAlgSpinner)
        textureAlgSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = parent?.getItemAtPosition(position).toString()
                textureAlgSpinner.requestLayout()
                callback.onEquationParamsChanged(
                        "texture",
                        TextureAlgorithm.all[item]?.invoke(resources) ?: config.texture()
                )
            }

        }

        val textureAlgAdapter = ArrayAdapter(
                v.context,
                android.R.layout.simple_spinner_item,
                List(TextureAlgorithm.all.size) { i: Int -> TextureAlgorithm.all.keys.elementAt(i) }
        )
        textureAlgAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        textureAlgSpinner.adapter = textureAlgAdapter
        textureAlgSpinner.setSelection(TextureAlgorithm.all.keys.indexOf(
                savedInstanceState?.getString("texture") ?: config.texture().name)
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("map", complexMapSpinner.selectedItem.toString())
        outState.putString("texture", textureAlgSpinner.selectedItem.toString())
        outState.putBoolean("juliaMode", juliaModeSwitch.isChecked)
        super.onSaveInstanceState(outState)
    }

    interface OnParamChangeListener {
        fun onEquationParamsChanged(key: String, value: Any)
    }

}