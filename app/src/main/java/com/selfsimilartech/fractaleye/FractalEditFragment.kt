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
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import katex.hourglass.`in`.mathlib.MathView
import kotlin.math.pow


class FractalEditFragment : Fragment() {


    lateinit var f : Fractal
    lateinit var fsv : FractalSurfaceView


    private fun formatStringToDouble(s: String) : Double? {
        var d : Double? = null
        try { d = s.toDouble() }
        catch (e: NumberFormatException) {
            val toast = Toast.makeText(context, "Invalid number format", Toast.LENGTH_LONG)
            toast.setGravity(Gravity.BOTTOM, 0, 20)
            toast.show()
        }
        return d
    }
    
    
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
                // Log.d("FRACTAL FRAGMENT", "intermediate height: $intermediateHeight")
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
                    // Log.d("FRACTAL FRAGMENT", "scroll to: ${(cardBody.parent.parent as CardView).y.toInt()}")
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

//        functionCardBody.layoutParams.height = 0
//        textureCardBody.layoutParams.height = 0
//        colorCardBody.layoutParams.height = 0
//        positionCardBody.layoutParams.height = 0

        val editListener = { nextEditText: EditText?, setValueAndFormat: (w: EditText) -> Unit
            -> TextView.OnEditorActionListener { editText, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_NEXT -> {
                    setValueAndFormat(editText as EditText)
                    editText.clearFocus()
                    editText.isSelected = false
                    nextEditText?.requestFocus()
                }
                EditorInfo.IME_ACTION_DONE -> {
                    setValueAndFormat(editText as EditText)
                    val imm = v.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    editText.clearFocus()
                    editText.isSelected = false
                }
                else -> {
                    Log.d("EQUATION FRAGMENT", "some other action")
                }
            }


            fsv.requestRender()
            true

        }}
        val lockListener = { i: Int, j: Int -> View.OnClickListener {
            val lock = it as ToggleButton
            when (j) {
                0 -> f.map.params[i].uLocked = lock.isChecked
                1 -> f.map.params[i].vLocked = lock.isChecked
            }
            Log.e("FRACTAL EDIT FRAGMENT", "param ${i+1} component ${j+1} locked ?= ${lock.isChecked}")
        }}









        // POSITION EDIT FIELDS

        val xCoordEdit = v.findViewById<EditText>(R.id.xCoordEdit)
        val yCoordEdit = v.findViewById<EditText>(R.id.yCoordEdit)
        xCoordEdit.setOnEditorActionListener(editListener(yCoordEdit) { w: TextView ->
            f.map.position.x = formatStringToDouble(w.text.toString()) ?: f.map.position.x
            w.text = "%.17f".format(f.map.position.x)
            fsv.r.renderToTex = true
        })
        yCoordEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            f.map.position.y = formatStringToDouble(w.text.toString()) ?: f.map.position.y
            w.text = "%.17f".format(f.map.position.y)
            fsv.r.renderToTex = true
        })

        val scaleSignificandEdit = v.findViewById<EditText>(R.id.scaleSignificandEdit)
        val scaleExponentEdit = v.findViewById<EditText>(R.id.scaleExponentEdit)
        scaleSignificandEdit.setOnEditorActionListener(
                editListener(scaleExponentEdit) { w: TextView ->

                    val prevScale = f.map.position.scale

                    f.map.position.scale = formatStringToDouble("${w.text}e${scaleExponentEdit.text}") ?: f.map.position.scale
                    val scaleStrings = "%e".format(f.map.position.scale).split("e")
                    w.text = "%.5f".format(scaleStrings[0].toFloat())

                    fsv.checkThresholdCross(prevScale)

                    fsv.r.renderToTex = true

                })
        scaleExponentEdit.setOnEditorActionListener(
                editListener(null) { w: TextView ->

                    val prevScale = f.map.position.scale

                    f.map.position.scale = formatStringToDouble("${scaleSignificandEdit.text}e${w.text}") ?: f.map.position.scale
                    val scaleStrings = "%e".format(f.map.position.scale).split("e")
                    w.text = "%d".format(scaleStrings[1].toInt())

                    fsv.checkThresholdCross(prevScale)

                    fsv.r.renderToTex = true

                })

        val rotationEdit = v.findViewById<EditText>(R.id.rotationEdit)
        rotationEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            f.map.position.rotation = formatStringToDouble(w.text.toString())?.times(Math.PI / 180.0) ?: f.map.position.rotation
            w.text = "%.0f".format(f.map.position.rotation * 180.0 / Math.PI)
            fsv.r.renderToTex = true
        })







        // SHAPE EDIT FIELDS

        val bailoutSignificandEdit = v.findViewById<EditText>(R.id.bailoutSignificandEdit)
        val bailoutExponentEdit = v.findViewById<EditText>(R.id.bailoutExponentEdit)
        bailoutSignificandEdit.setOnEditorActionListener(
                editListener(bailoutExponentEdit) { w: TextView ->
                    f.bailoutRadius = formatStringToDouble("${w.text}e${bailoutExponentEdit.text}")?.toFloat() ?: f.bailoutRadius
                    val bailoutStrings = "%e".format(f.bailoutRadius).split("e")
                    w.text = "%.5f".format(bailoutStrings[0].toFloat())
                    fsv.r.renderToTex = true
                })
        bailoutExponentEdit.setOnEditorActionListener(
                editListener(null) { w: TextView ->
                    f.bailoutRadius = formatStringToDouble("${bailoutSignificandEdit.text}e${w.text}")?.toFloat() ?: f.bailoutRadius
                    val bailoutStrings = "%e".format(f.bailoutRadius).split("e")
                    w.text = "%d".format(bailoutStrings[1].toInt())
                    fsv.r.renderToTex = true
                })


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
            pair1.first.first.setText("%.8f".format(f.map.params[i].u))
            pair1.first.second.setText("%.8f".format(f.map.params[i].v))
            pair1.first.first.setOnEditorActionListener(editListener(pair1.first.second) { w: TextView ->
                f.map.params[i].u = "${w.text}".toDouble()
                w.text = "%.8f".format((f.map.params[i].u))
                fsv.r.renderToTex = true
            })
            pair1.first.second.setOnEditorActionListener(editListener(null) { w: TextView ->
                f.map.params[i].v = "${w.text}".toDouble()
                w.text = "%.8f".format((f.map.params[i].v))
                fsv.r.renderToTex = true
            })
            pair1.second.first.setOnClickListener(lockListener(i, 0))
            pair1.second.second.setOnClickListener(lockListener(i, 1))
        }








        // COLOR PALETTE SELECTION

        val colorPaletteSpinner = v.findViewById<Spinner>(R.id.colorPaletteSpinner)
        colorPaletteSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                f.palette = parent?.getItemAtPosition(position) as ColorPalette
                fsv.requestRender()
            }

        }

        val adapter = ColorPaletteAdapter(v.context, ColorPalette.all.values.toList())

        // adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorPaletteSpinner.adapter = adapter
        colorPaletteSpinner.setSelection(ColorPalette.all.keys.indexOf(
                savedInstanceState?.getString("palette") ?: f.palette.name)
        )



        // COLOR EDIT FIELDS

        val frequencyEdit = v.findViewById<EditText>(R.id.frequencyEdit)
        frequencyEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            f.frequency = formatStringToDouble(w.text.toString())?.toFloat() ?: f.frequency
            w.text = "%.5f".format(f.frequency)
        })

        val phaseEdit = v.findViewById<EditText>(R.id.phaseEdit)
        phaseEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            f.phase = formatStringToDouble(w.text.toString())?.toFloat() ?: f.phase
            w.text = "%.5f".format(f.phase)
        })








        // JULIA MODE SWITCH

        val juliaModeSwitch = v.findViewById<Switch>(R.id.juliaModeSwitch)
        val juliaListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            val juliaParamIndex = f.numParamsInUse
            Log.d("FRACTAL EDIT FRAGMENT", "juliaParamIndex: $juliaParamIndex")
            val juliaLayoutIndex = juliaLayout.indexOfChild(juliaModeSwitch)
            if (isChecked) {
                 juliaLayout.removeView(mapParamLayouts[juliaParamIndex])
                 juliaLayout.addView(mapParamLayouts[juliaParamIndex], juliaLayoutIndex + 1)
                 complexMapKatex.setDisplayText(resources.getString(f.map.katex).format("P${f.numParamsInUse + 1}"))
                 (activity as MainActivity).addMapParams(1)
             } else {
                 juliaLayout.removeView(mapParamLayouts[juliaParamIndex - 1])
                 complexMapKatex.setDisplayText(resources.getString(f.map.katex).format("c"))
                 (activity as MainActivity).removeMapParams(1)
             }

            val prevScale = f.map.position.scale
            f.juliaMode = isChecked
            fsv.checkThresholdCross(prevScale)

            (activity as MainActivity).updateMapParamEditText(f.numParamsInUse)

            fsv.r.renderShaderChanged = true
            fsv.r.renderToTex = true
            fsv.requestRender()

        }
        if (f.map.juliaMode) { functionCardBody.removeView(juliaLayout) }
        else { juliaModeSwitch.setOnCheckedChangeListener(juliaListener) }







        // TEXTURE SELECTION

        val q1Bar = v.findViewById<SeekBar>(R.id.q1Bar)

        // TEXTURE PARAM EDIT FIELDS

        val q1Edit = v.findViewById<EditText>(R.id.q1Edit)
        q1Edit.setOnEditorActionListener(editListener(null) { w: TextView ->
            f.texture.params[0].t = formatStringToDouble(w.text.toString()) ?: f.texture.params[0].t
            w.text = "%.3f".format(f.texture.params[0].t)
            val range = f.texture.params[0].range
            val length = range.upper - range.lower
            q1Bar.progress = (100.0 * (f.texture.params[0].t - range.lower) / length).toInt()
            fsv.r.renderToTex = true
        })

        val q2Edit = v.findViewById<EditText>(R.id.q2Edit)
        q2Edit.setOnEditorActionListener(editListener(null) { w: TextView ->
            f.texture.params[1].t = formatStringToDouble(w.text.toString()) ?: f.texture.params[1].t
            w.text = "%.3f".format(f.texture.params[1].t)
            fsv.r.renderToTex = true
        })

        val q1BarListener = object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                val p = progress / 100.0
                val range = f.texture.params[0].range
                val length = range.upper - range.lower
                f.texture.params[0].t = p*length + range.lower
                (activity as MainActivity).updateTextureEditTexts()

                fsv.r.renderToTex = true
                fsv.requestRender()

            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        }

        val textureAlgSpinner = v.findViewById<Spinner>(R.id.textureAlgSpinner)
        textureAlgSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // textureAlgSpinner.requestLayout()

                f.texture = Texture.all[parent?.getItemAtPosition(position).toString()] ?: Texture.escape

                (activity as MainActivity).updateShapeEditTexts()

                fsv.r.renderShaderChanged = true
                fsv.r.renderToTex = true
                fsv.requestRender()

                for (i in 0 until NUM_TEXTURE_PARAMS) { textureCardBody.removeView(textureParamEditRows[i]) }
                val textureLayoutIndex = textureCardBody.indexOfChild(textureSpinnerLayout)
                for (i in 0 until f.texture.numParamsInUse) {
                    if (textureCardBody.indexOfChild(textureParamEditRows[i]) == -1) {
                        textureCardBody.addView(textureParamEditRows[i], textureLayoutIndex + i + 1)
                    }
                    (textureParamEditRows[i].getChildAt(0) as TextView).text = f.texture.params[i].name
                    if (i == 0) {
                        val range = f.texture.params[0].range
                        val q = (range.clamp(f.texture.params[0].t) - range.lower) / (range.upper - range.lower) * 100
                        Log.d("FRACTAL FRAGMENT", "q: $q")
                        q1Bar?.progress = q.toInt()
                        q1Bar?.setOnSeekBarChangeListener(q1BarListener)
                        q1Edit.setText("%.3f".format(f.texture.params[0].t))
                    }
                }
            }

        }

        val textureAlgAdapter = ArrayAdapter(
                v.context,
                android.R.layout.simple_spinner_item,
                f.map.textures
        )
        textureAlgAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        textureAlgSpinner.adapter = textureAlgAdapter
        textureAlgSpinner.setSelection(Texture.all.keys.indexOf(
                savedInstanceState?.getString("texture") ?: f.texture.name)
        )







        // COMPLEX MAP SELECTION

        val complexMapSpinner = v.findViewById<Spinner>(R.id.complexMapSpinner)
        complexMapSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                val item = parent?.getItemAtPosition(position).toString()
                val nextMap = ComplexMap.all[item] ?: ComplexMap.empty
                val mapChanged = nextMap != f.map

                if (mapChanged) {

                    if (juliaModeSwitch.isChecked && !nextMap.juliaMode) {
                        val juliaParamIndex = f.numParamsInUse - 1
                        Log.d("FRACTAL FRAGMENT", "params in use: ${f.numParamsInUse}")
                        juliaModeSwitch.setOnCheckedChangeListener { _, _ ->  }
                        juliaModeSwitch.isChecked = false
                        // juliaLayout.removeView(mapParamLayouts[juliaParamIndex])
                    }

                    if (nextMap.juliaMode) { functionCardBody.removeView(juliaLayout) }
                    else {
                        if (functionCardBody.indexOfChild(juliaLayout) == -1) { functionCardBody.addView(juliaLayout) }
                        juliaModeSwitch.setOnCheckedChangeListener(juliaListener)
                    }

                }

                val mapLayoutIndex = functionCardBody.indexOfChild(complexMapKatexLayout)
                for (i in 0 until NUM_MAP_PARAMS) {
                    // remove all map param layouts
                    // Log.e("FRACTAL EDIT FRAGMENT", "MAP -- removing row ${i + 1}")
                    functionCardBody.removeView(mapParamLayouts[i])
                    juliaLayout.removeView(mapParamLayouts[i])
                }
                for (i in 0 until nextMap.numParams) {
                    // Log.d("FRACTAL FRAGMENT", "MAP -- adding row ${i + 1} at index ${mapLayoutIndex + i + 1}")
                    functionCardBody.addView(mapParamLayouts[i], mapLayoutIndex + i + 1)
                }

                val prevScale = f.map.position.scale
                f.map = nextMap
                fsv.checkThresholdCross(prevScale)

                (activity as MainActivity).updateShapeEditTexts()
                (activity as MainActivity).updatePositionEditTexts()

                fsv.r.renderShaderChanged = true
                fsv.r.renderToTex = true
                fsv.requestRender()


                // set katex
                complexMapKatex.setDisplayText(resources.getString(nextMap.katex).format("c"))
                //complexMapKatex.setTextSize((nextMap.katexSize * resources.displayMetrics.scaledDensity).toInt())

                // set map param locks
                mapParamEditTexts.forEachIndexed { i, pair ->
                    if (i < f.map.params.size) {
                        pair.second.first.isChecked = f.map.params[i].uLocked
                        pair.second.second.isChecked = f.map.params[i].vLocked
                    }
                }

                // set compatible textures
                val adapter = ArrayAdapter(
                        v.context,
                        android.R.layout.simple_spinner_item,
                        f.map.textures
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                textureAlgSpinner.adapter = adapter
                val textureIndex = f.map.textures.indexOf(f.texture.name)
                textureAlgSpinner.setSelection(if (textureIndex == -1) 0 else textureIndex)

                // update overlay UI
                val uiQuick = (activity as MainActivity).findViewById<LinearLayout>(R.id.uiQuick)
                (activity as MainActivity).removeMapParams(uiQuick.childCount - 2)
                (activity as MainActivity).addMapParams(f.numParamsInUse)
                uiQuick.getChildAt(uiQuick.childCount - 1).performClick()

            }

        }

        val complexMapAdapter = ComplexMapAdapter(
                v.context,
                List(ComplexMap.all.size) { i: Int -> ComplexMap.all.values.elementAt(i) }
        )
        // complexMapAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        complexMapSpinner.adapter = complexMapAdapter
        complexMapSpinner.setSelection(ComplexMap.all.keys.indexOf(
                savedInstanceState?.getString("map") ?: f.map.name)
        )









        val maxIterBar = v.findViewById<SeekBar>(R.id.maxIterBar)
        maxIterBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {

                val p = seekBar.progress.toFloat() / 100.0f
                f.maxIter = ((2.0.pow(5) - 1)*(1.0f - p) + (2.0.pow(12) - 1)*p).toInt()
                fsv.r.renderToTex = true
                fsv.requestRender()

            }

        })
        maxIterBar.progress = (100.0*(f.maxIter.toDouble() - 2.0.pow(5) + 1.0) / (2.0.pow(12) - 1.0)).toInt()




        for (i in 0 until NUM_MAP_PARAMS) { functionCardBody.removeView(mapParamLayouts[i]) }
        for (i in 0 until NUM_TEXTURE_PARAMS) { textureCardBody.removeView(textureParamEditRows[i]) }



        return v

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).updateShapeEditTexts()
        (activity as MainActivity).updateTextureEditTexts()
        (activity as MainActivity).updateColorEditTexts()
        (activity as MainActivity).updatePositionEditTexts()
    }


    interface OnParamChangeListener {
        fun onFractalParamsChanged(key: String, value: Any)
    }

}