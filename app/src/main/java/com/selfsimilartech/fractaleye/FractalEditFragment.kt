package com.selfsimilartech.fractaleye

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.Rect
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.CardView
import android.util.TypedValue
import android.view.inputmethod.InputMethodManager
import katex.hourglass.`in`.mathlib.MathView
import kotlin.math.pow


class FractalEditFragment : Fragment() {

    private lateinit var callback : OnParamChangeListener
    private lateinit var complexMapSpinner : Spinner
    private lateinit var textureAlgSpinner : Spinner
    private lateinit var colorPaletteSpinner : Spinner
    private lateinit var juliaModeSwitch : Switch

    lateinit var config : FractalConfig

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {

        val v = inflater.inflate(R.layout.fractal_fragment, container, false)

        val mapParamLayouts = listOf<ConstraintLayout>(
                v.findViewById(R.id.p1Layout),
                v.findViewById(R.id.p2Layout),
                v.findViewById(R.id.p3Layout),
                v.findViewById(R.id.p4Layout)
        )
        val juliaLayout = v.findViewById<LinearLayout>(R.id.juliaLayout)
        val complexMapKatexLayout = v.findViewById<LinearLayout>(R.id.complexMapKatexLayout)
        val complexMapKatex = v.findViewById<MathView>(R.id.complexMapKatex)

        val textureSpinnerLayout = v.findViewById<LinearLayout>(R.id.textureSpinnerLayout)
        val textureParamEditRows = listOf<LinearLayout>(
                v.findViewById(R.id.q1EditRow),
                v.findViewById(R.id.q2EditRow)
        )

        val fractalScroll = v.findViewById<ScrollView>(R.id.fractalScroll)
        val cardHeaderListener = { cardBody: LinearLayout -> View.OnClickListener {

            val card = cardBody.parent.parent as CardView
            val initScrollY = fractalScroll.scrollY
            val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15f, context?.resources?.displayMetrics).toInt()

            val hStart : Int
            val hEnd : Int


            if (cardBody.layoutParams.height == 0) {

                // measure card body height
                val matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec((cardBody.parent as View).width, View.MeasureSpec.EXACTLY)
                val wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                cardBody.measure(matchParentMeasureSpec, wrapContentMeasureSpec)

                hStart = 0
                hEnd = cardBody.measuredHeight
                (it as Button).setCompoundDrawablesWithIntrinsicBounds(null, null, resources.getDrawable(R.drawable.arrow_collapse, null), null)

            }
            else {

                hStart = cardBody.height
                hEnd = 0
                (it as Button).setCompoundDrawablesWithIntrinsicBounds(null, null, resources.getDrawable(R.drawable.arrow_expand, null), null)

            }

            val anim = ValueAnimator.ofInt(hStart, hEnd)
            anim.addUpdateListener { animation ->
                val intermediateHeight = animation?.animatedValue as Int
                Log.d("FRACTAL FRAGMENT", "intermediate height: $intermediateHeight")
                cardBody.layoutParams.height = intermediateHeight
                cardBody.requestLayout()

                if (hStart == 0) {  // only scroll on card expansion

                    // get scrollView dimensions and card coordinates in scrollView
                    val scrollBounds = Rect()
                    fractalScroll.getDrawingRect(scrollBounds)
                    var top = 0f
                    var temp = card as View
                    while (temp !is ScrollView){
                        top += (temp).y
                        temp = temp.parent as View
                    }
                    val bottom = (top + card.height).toInt()

                    if (scrollBounds.top > top - px) {  // card top is not visible
                        fractalScroll.scrollY = ((1f - animation.animatedFraction)*initScrollY).toInt() + (animation.animatedFraction*(top - px)).toInt()  // scroll to top of card
                    }
                    else {  // card top is visible
                        if (scrollBounds.bottom < bottom && card.height < fractalScroll.height - px) { // card bottom is not visible and there is space between card top and scrollView top
                            fractalScroll.scrollY = bottom - fractalScroll.height  // scroll to show bottom
                        }
                    }
                    Log.d("FRACTAL FRAGMENT", "scroll to: ${(cardBody.parent.parent as CardView).y.toInt()}")
                }


            }

            // change cardBody height back to wrap_content after expansion
            if (cardBody.layoutParams.height == 0) {
                anim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                        cardBody.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                    }
                })
            }

            anim.duration = 250
            anim.start()

        }}

        val functionCardBody = v.findViewById<LinearLayout>(R.id.functionCardBody)
        val textureCardBody = v.findViewById<LinearLayout>(R.id.textureCardBody)
        val positionCardBody = v.findViewById<LinearLayout>(R.id.positionCardBody)
        val colorCardBody = v.findViewById<LinearLayout>(R.id.colorCardBody)

        val functionHeaderButton = v.findViewById<Button>(R.id.functionHeaderButton)
        val textureHeaderButton = v.findViewById<Button>(R.id.textureHeaderButton)
        val positionHeaderButton = v.findViewById<Button>(R.id.positionHeaderButton)
        val colorHeaderButton = v.findViewById<Button>(R.id.colorHeaderButton)

        functionHeaderButton.setOnClickListener(cardHeaderListener(functionCardBody))
        textureHeaderButton.setOnClickListener(cardHeaderListener(textureCardBody))
        positionHeaderButton.setOnClickListener(cardHeaderListener(positionCardBody))
        colorHeaderButton.setOnClickListener(cardHeaderListener(colorCardBody))

        functionCardBody.layoutParams.height = 0
        textureCardBody.layoutParams.height = 0
        colorCardBody.layoutParams.height = 0
        positionCardBody.layoutParams.height = 0

        val editListenerNext = {
            editText: EditText, nextEditText: EditText, key: String, value: (w: TextView)-> Any -> TextView.OnEditorActionListener {
            w, actionId, _ -> when (actionId) {
            EditorInfo.IME_ACTION_NEXT -> {
                callback.onFractalParamsChanged(key, value(w))
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
            w, actionId, _ -> when (actionId) {
            EditorInfo.IME_ACTION_DONE -> {
                callback.onFractalParamsChanged(key, value(w))
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
        val lockListener = { i: Int, j: Int -> View.OnClickListener {
            val lock = it as ToggleButton
            when (j) {
                0 -> (config.params["p$i"] as ComplexMap.Param).uLocked = lock.isChecked
                1 -> (config.params["p$i"] as ComplexMap.Param).vLocked = lock.isChecked
            }
        }}





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
        scaleExponentEdit.setText("%d".format(scaleStrings[1].toInt()))
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
        bailoutExponentEdit.setText("%d".format(bailoutStrings[1].toInt()))
        bailoutSignificandEdit.setOnEditorActionListener(
                editListenerNext(bailoutSignificandEdit, bailoutExponentEdit, "bailoutRadius") {
                    w: TextView -> "${w.text}e${bailoutExponentEdit.text}".toFloat()
                })
        bailoutExponentEdit.setOnEditorActionListener(
                editListenerDone(bailoutExponentEdit, "bailoutRadius") {
                    w: TextView -> "${bailoutSignificandEdit.text}e${w.text}".toFloat()
                })

        val rotationEdit = v.findViewById<EditText>(R.id.rotationEdit)
        rotationEdit.setText("%.0f".format(config.rotation() * 180.0 / Math.PI))
        rotationEdit.setOnEditorActionListener(editListenerDone(rotationEdit, "rotation") {
            w: TextView -> w.text.toString().toDouble() * Math.PI / 180.0
        })





        // PARAMETER EDIT FIELDS
        val mapParamEditTexts = listOf<Pair<Pair<EditText, EditText>, Pair<ToggleButton, ToggleButton>>>(
                Pair(
                    Pair(
                        v.findViewById(R.id.u1Edit),
                        v.findViewById(R.id.v1Edit)),
                    Pair(
                        v.findViewById(R.id.u1Lock),
                        v.findViewById(R.id.v1Lock))),
                Pair(
                    Pair(
                        v.findViewById(R.id.u2Edit),
                        v.findViewById(R.id.v2Edit)),
                    Pair(
                        v.findViewById(R.id.u2Lock),
                        v.findViewById(R.id.v2Lock))),
                Pair(
                    Pair(
                        v.findViewById(R.id.u3Edit),
                        v.findViewById(R.id.v3Edit)),
                    Pair(
                        v.findViewById(R.id.u3Lock),
                        v.findViewById(R.id.v3Lock))),
                Pair(
                    Pair(
                        v.findViewById(R.id.u4Edit),
                        v.findViewById(R.id.v4Edit)),
                    Pair(
                        v.findViewById(R.id.u4Lock),
                        v.findViewById(R.id.v4Lock)))
        )
        mapParamEditTexts.forEachIndexed { i, pair1 ->
            pair1.first.first.setText("%.8f".format((config.params["p${i + 1}"] as ComplexMap.Param).getU()))
            pair1.first.second.setText("%.8f".format((config.params["p${i + 1}"] as ComplexMap.Param).getV()))
            pair1.first.first.setOnEditorActionListener(editListenerNext(pair1.first.first, pair1.first.second, "p${i + 1}") {
                w: TextView ->
                ComplexMap.Param(
                        "${w.text}".toDouble(),
                        "${pair1.first.second.text}".toDouble(),
                        (config.params["p${i + 1}"] as ComplexMap.Param).uLocked,
                        (config.params["p${i + 1}"] as ComplexMap.Param).vLocked
                )
            })
            pair1.first.second.setOnEditorActionListener(editListenerDone(pair1.first.second, "p${i + 1}") {
                w: TextView -> ComplexMap.Param(
                        "${pair1.first.first.text}".toDouble(),
                        "${w.text}".toDouble(),
                        (config.params["p${i + 1}"] as ComplexMap.Param).uLocked,
                        (config.params["p${i + 1}"] as ComplexMap.Param).vLocked
                )
            })
            pair1.second.first.setOnClickListener(lockListener(i + 1, 0))
            pair1.second.second.setOnClickListener(lockListener(i + 1, 1))
        }


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



        // COLOR PALETTE SELECTION
        colorPaletteSpinner = v.findViewById(R.id.colorPaletteSpinner)
        colorPaletteSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = parent?.getItemAtPosition(position) as ColorPalette
                callback.onFractalParamsChanged("palette", item)
            }

        }

        val adapter = ColorPaletteAdapter(
                v.context,
                List(ColorPalette.all.size) { i: Int -> ColorPalette.all.values.elementAt(i).invoke(resources) }
        )
        // adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorPaletteSpinner.adapter = adapter
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


        for (i in 0 until NUM_MAP_PARAMS) { functionCardBody.removeView(mapParamLayouts[i]) }
        for (i in 0 until NUM_TEXTURE_PARAMS) { textureCardBody.removeView(textureParamEditRows[i]) }



        // JULIA MODE SWITCH
         val juliaListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
             val juliaParamIndex = config.numParamsInUse()
             Log.d("FRACTAL FRAGMENT", "juliaParamIndex: $juliaParamIndex")
             val juliaLayoutIndex = juliaLayout.indexOfChild(juliaModeSwitch)
             if (isChecked) {
                 juliaLayout.removeView(mapParamLayouts[juliaParamIndex])
                 juliaLayout.addView(mapParamLayouts[juliaParamIndex], juliaLayoutIndex + 1)
                 complexMapKatex.setDisplayText(config.map().katex.format("P${config.numParamsInUse() + 1}"))
             } else {
                 juliaLayout.removeView(mapParamLayouts[juliaParamIndex - 1])
                 complexMapKatex.setDisplayText(config.map().katex.format("c"))
             }
             callback.onFractalParamsChanged("juliaMode", isChecked)
         }
        juliaModeSwitch = v.findViewById(R.id.juliaModeSwitch)
        if (config.map().initJuliaMode) { functionCardBody.removeView(juliaLayout) }
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
                        juliaModeSwitch.setOnCheckedChangeListener { _, _ ->  }
                        juliaModeSwitch.isChecked = false
                        juliaLayout.removeView(mapParamLayouts[juliaParamIndex])
                        config.params["juliaMode"] = nextMap.initJuliaMode
                    }
                    if (nextMap.initJuliaMode) { functionCardBody.removeView(juliaLayout) }
                    else {
                        if (functionCardBody.indexOfChild(juliaLayout) == -1) { functionCardBody.addView(juliaLayout) }
                        juliaModeSwitch.setOnCheckedChangeListener(juliaListener)
                    }

                }

                val mapLayoutIndex = functionCardBody.indexOfChild(complexMapKatexLayout)
                for (i in 0 until config.map().initParams.size) {
                    // Log.d("FRACTAL FRAGMENT", "MAP -- removing row ${i + 1}")
                    functionCardBody.removeView(mapParamLayouts[i])
                }
                for (i in 0 until (ComplexMap.all[item]?.invoke(resources) ?: ComplexMap.empty()).initParams.size) {
                    // Log.d("FRACTAL FRAGMENT", "MAP -- adding row ${i + 1} at index ${mapLayoutIndex + i + 1}")
                    functionCardBody.addView(mapParamLayouts[i], mapLayoutIndex + i + 1)
                }

                callback.onFractalParamsChanged(
                        "map",
                        ComplexMap.all[item]?.invoke(resources) ?: config.map()
                )

                // set katex
                complexMapKatex.setDisplayText(nextMap.katex.format("c"))
                //complexMapKatex.setTextSize((nextMap.katexSize * resources.displayMetrics.scaledDensity).toInt())

                // set map param locks
                mapParamEditTexts.forEachIndexed { i, pair ->
                    if (i < config.map().initParams.size) {
                        pair.second.first.isChecked = config.map().initParams[i].uLocked
                        pair.second.second.isChecked = config.map().initParams[i].vLocked
                    }
                }

                // set compatible textures
                val textureAlgAdapter = ArrayAdapter(
                        v.context,
                        android.R.layout.simple_spinner_item,
                        config.map().textures
                )
                textureAlgAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                textureAlgSpinner.adapter = textureAlgAdapter
                val textureIndex = config.map().textures.indexOf(config.texture().name)
                textureAlgSpinner.setSelection(if (textureIndex == -1) 0 else textureIndex)

            }

        }

        val complexMapAdapter = ComplexMapAdapter(
                v.context,
                List(ComplexMap.all.size) { i: Int -> ComplexMap.all.values.elementAt(i).invoke(resources) }
        )
        // complexMapAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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
                callback.onFractalParamsChanged("q1", p*length + range.lower)
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

                callback.onFractalParamsChanged(
                        "texture",
                        Texture.all[item]?.invoke(resources) ?: config.texture()
                )
                for (i in 0 until NUM_TEXTURE_PARAMS) { textureCardBody.removeView(textureParamEditRows[i]) }
                val textureLayoutIndex = textureCardBody.indexOfChild(textureSpinnerLayout)
                for (i in config.texture().initParams.indices) {
                    if (textureCardBody.indexOfChild(textureParamEditRows[i]) == -1) {
                        textureCardBody.addView(textureParamEditRows[i], textureLayoutIndex + i + 1)
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
            config.map().textures
        )
        textureAlgAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        textureAlgSpinner.adapter = textureAlgAdapter
        textureAlgSpinner.setSelection(Texture.all.keys.indexOf(
                savedInstanceState?.getString("texture") ?: config.texture().name)
        )



        val maxIterBar = v.findViewById<SeekBar>(R.id.maxIterBar)
        maxIterBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val p = seekBar.progress.toFloat() / 100.0f
                callback.onFractalParamsChanged(
                    "maxIter",
                    ((2.0.pow(5) - 1)*(1.0f - p) + (2.0.pow(12) - 1)*p).toInt()
                )
            }

        })
        maxIterBar.progress = (100.0*(config.maxIter().toDouble() - 2.0.pow(5) + 1.0) / (2.0.pow(12) - 1.0)).toInt()

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
        fun onFractalParamsChanged(key: String, value: Any)
    }

}