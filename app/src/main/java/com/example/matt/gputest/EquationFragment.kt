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
import android.support.v7.widget.CardView
import android.view.inputmethod.InputMethodManager
import katex.hourglass.`in`.mathlib.MathView
import kotlin.math.pow


class EquationFragment : Fragment() {

    private lateinit var callback : OnParamChangeListener
    private lateinit var complexMapSpinner : Spinner
    private lateinit var textureAlgSpinner : Spinner
    private lateinit var colorPaletteSpinner : Spinner
    private lateinit var juliaModeSwitch : Switch

    lateinit var config : FractalConfig

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {

        val v = inflater.inflate(R.layout.equation_fragment, container, false)

        val mapParamEditRows = listOf<LinearLayout>(
                v.findViewById(R.id.p1EditRow),
                v.findViewById(R.id.p2EditRow),
                v.findViewById(R.id.p3EditRow),
                v.findViewById(R.id.p4EditRow)
        )
        val juliaLayout = v.findViewById<LinearLayout>(R.id.juliaLayout)
        val complexMapKatexLayout = v.findViewById<LinearLayout>(R.id.complexMapKatexLayout)
        val complexMapKatex = v.findViewById<MathView>(R.id.complexMapKatex)

        val textureSpinnerLayout = v.findViewById<LinearLayout>(R.id.textureSpinnerLayout)
        val textureParamEditRows = listOf<LinearLayout>(
                v.findViewById(R.id.q1EditRow),
                v.findViewById(R.id.q2EditRow)
        )

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

        val functionLayoutBody = v.findViewById<LinearLayout>(R.id.functionLayoutBody)
        val textureLayoutBody = v.findViewById<LinearLayout>(R.id.textureLayoutBody)
        val positionLayoutBody = v.findViewById<LinearLayout>(R.id.positionLayoutBody)
        val colorLayoutBody = v.findViewById<LinearLayout>(R.id.colorLayoutBody)

        val functionHeaderButton = v.findViewById<Button>(R.id.functionHeaderButton)
        val textureHeaderButton = v.findViewById<Button>(R.id.textureHeaderButton)
        val positionHeaderButton = v.findViewById<Button>(R.id.positionHeaderButton)
        val colorHeaderButton = v.findViewById<Button>(R.id.colorHeaderButton)

        functionHeaderButton.setOnClickListener(cardHeaderListener(functionLayoutBody))
        textureHeaderButton.setOnClickListener(cardHeaderListener(textureLayoutBody))
        positionHeaderButton.setOnClickListener(cardHeaderListener(positionLayoutBody))
        colorHeaderButton.setOnClickListener(cardHeaderListener(colorLayoutBody))

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



        val q1Edit = v.findViewById<EditText>(R.id.q1Edit)
        q1Edit.setText("%.3f".format(config.q1()))
        q1Edit.setOnEditorActionListener(editListenerDone(q1Edit, "q1") {
            w: TextView -> w.text.toString().toDouble()
        })

        val q2Edit = v.findViewById<EditText>(R.id.q2Edit)
        q2Edit.setText("%.3f".format(config.q2()))
        q2Edit.setOnEditorActionListener(editListenerDone(q2Edit, "q2") {
            w: TextView -> w.text.toString().toDouble()
        })


        colorPaletteSpinner = v.findViewById(R.id.colorPaletteSpinner)
        colorPaletteSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = parent?.getItemAtPosition(position).toString()
                callback.onEquationParamsChanged(
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

        val frequencyEdit = v.findViewById<EditText>(R.id.frequencyEdit)
        frequencyEdit.setText("%.5f".format(config.frequency()))
        frequencyEdit.setOnEditorActionListener(editListenerDone(frequencyEdit, "frequency") {
            it.text.toString().toFloat()
        })

        val phaseEdit = v.findViewById<EditText>(R.id.phaseEdit)
        phaseEdit.setText("%.5f".format(config.phase()))
        phaseEdit.setOnEditorActionListener(editListenerDone(frequencyEdit, "phase") {
            it.text.toString().toFloat()
        })


        for (i in 0 until NUM_MAP_PARAMS) { functionLayoutBody.removeView(mapParamEditRows[i]) }
        for (i in 0 until NUM_TEXTURE_PARAMS) { textureLayoutBody.removeView(textureParamEditRows[i]) }



        // JULIA MODE SWITCH
         val juliaListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
             val juliaParamIndex = config.numParamsInUse()
             Log.d("FRACTAL FRAGMENT", "juliaParamIndex: $juliaParamIndex")
             val juliaLayoutIndex = juliaLayout.indexOfChild(juliaModeSwitch)
             if (isChecked) {
                 juliaLayout.removeView(mapParamEditRows[juliaParamIndex])
                 juliaLayout.addView(mapParamEditRows[juliaParamIndex], juliaLayoutIndex + 1)
                 complexMapKatex.setDisplayText(config.map().katex.replace(Regex("(?<!a)c"), "P${juliaParamIndex + 1}"))
             } else {
                 juliaLayout.removeView(mapParamEditRows[juliaParamIndex - 1])
                 complexMapKatex.setDisplayText(config.map().katex)
             }
             callback.onEquationParamsChanged("juliaMode", isChecked)
         }
        juliaModeSwitch = v.findViewById(R.id.juliaModeSwitch)
        if (config.map().initJuliaMode) { functionLayoutBody.removeView(juliaLayout) }
        else { juliaModeSwitch.setOnCheckedChangeListener(juliaListener) }





        // COMPLEX MAP SELECTION
        complexMapSpinner = v.findViewById(R.id.complexMapSpinner)
        complexMapSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                val item = parent?.getItemAtPosition(position).toString()

                val nextMap = ComplexMap.all[item]?.invoke(resources) ?: ComplexMap.empty()
                if (nextMap != config.map()) {
                    if (juliaModeSwitch.isChecked && !config.map().initJuliaMode) {
                        val juliaParamIndex = config.numParamsInUse() - 1
                        Log.d("FRACTAL FRAGMENT", "params in use: ${config.numParamsInUse()}")
                        juliaModeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->  }
                        juliaModeSwitch.isChecked = false
                        juliaLayout.removeView(mapParamEditRows[juliaParamIndex])
                        config.params["juliaMode"] = nextMap.initJuliaMode
                    }
                    if (nextMap.initJuliaMode) { functionLayoutBody.removeView(juliaLayout) }
                    else {
                        if (functionLayoutBody.indexOfChild(juliaLayout) == -1) { functionLayoutBody.addView(juliaLayout) }
                        juliaModeSwitch.setOnCheckedChangeListener(juliaListener)
                    }

                }

                val mapLayoutIndex = functionLayoutBody.indexOfChild(complexMapKatexLayout)
                for (i in 0 until config.map().initParams.size) {
                    // Log.d("FRACTAL FRAGMENT", "MAP -- removing row ${i + 1}")
                    functionLayoutBody.removeView(mapParamEditRows[i])
                }
                for (i in 0 until (ComplexMap.all[item]?.invoke(resources) ?: ComplexMap.empty()).initParams.size) {
                    // Log.d("FRACTAL FRAGMENT", "MAP -- adding row ${i + 1} at index ${mapLayoutIndex + i + 1}")
                    functionLayoutBody.addView(mapParamEditRows[i], mapLayoutIndex + i + 1)
                }

                callback.onEquationParamsChanged(
                        "map",
                        ComplexMap.all[item]?.invoke(resources) ?: config.map()
                )

                complexMapKatex.setDisplayText(nextMap.katex)
                complexMapKatex.setTextSize((nextMap.katexSize * resources.displayMetrics.scaledDensity).toInt())

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

        val q1BarListener = object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val p = progress / 100.0
                val range = config.texture().initParams[0].second
                val length = range.upper - range.lower
                callback.onEquationParamsChanged("q1", p*length + range.lower)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        }

        textureAlgSpinner = v.findViewById(R.id.textureAlgSpinner)
        textureAlgSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = parent?.getItemAtPosition(position).toString()
                // textureAlgSpinner.requestLayout()

                callback.onEquationParamsChanged(
                        "texture",
                        TextureAlgorithm.all[item]?.invoke(resources) ?: config.texture()
                )
                for (i in 0 until NUM_TEXTURE_PARAMS) { textureLayoutBody.removeView(textureParamEditRows[i]) }
                val textureLayoutIndex = textureLayoutBody.indexOfChild(textureSpinnerLayout)
                for (i in 0 until config.texture().initParams.size) {
                    if (textureLayoutBody.indexOfChild(textureParamEditRows[i]) == -1) {
                        textureLayoutBody.addView(textureParamEditRows[i], textureLayoutIndex + i + 1)
                    }
                    (textureParamEditRows[i].getChildAt(0) as TextView).text = config.texture().initParams[i].first
                    if (i == 0) {
                        val q1Bar = v.findViewById<SeekBar>(R.id.q1Bar)
                        val range = config.texture().initParams[0].second
                        val q = (range.clamp(config.q1()) - range.lower) / (range.upper - range.lower) * 100
                        Log.d("FRACTAL FRAGMENT", "q: $q")
                        q1Bar?.progress = q.toInt()
                        q1Bar?.setOnSeekBarChangeListener(q1BarListener)
                        q1Edit.setText("%.3f".format(config.q1()))
                    }
                }
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

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
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