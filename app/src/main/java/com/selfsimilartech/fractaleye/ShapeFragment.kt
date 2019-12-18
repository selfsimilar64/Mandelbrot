package com.selfsimilartech.fractaleye

import android.content.Context
import android.support.v4.app.Fragment
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.TabLayout
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlin.math.log
import kotlin.math.pow


class ShapeFragment : Fragment() {

    // Store instance variables
    private lateinit var f: Fractal
    private lateinit var fsv: FractalSurfaceView

    // newInstance constructor for creating fragment with arguments
    fun passArguments(f: Fractal, fsv: FractalSurfaceView) {
        this.f = f
        this.fsv = fsv
    }

    fun String.formatToDouble() : Double? {
        var d : Double? = null
        try { d = this.toDouble() }
        catch (e: NumberFormatException) {
            val toast = Toast.makeText(context, "Invalid number format", Toast.LENGTH_LONG)
            toast.setGravity(Gravity.BOTTOM, 0, 20)
            toast.show()
        }
        return d
    }

    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.shape_fragment, container, false)


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
                    val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
        val lockListener = { j: Int -> View.OnClickListener {
            val lock = it as ToggleButton
            when (j) {
                0 -> f.shape.activeParam.uLocked = lock.isChecked
                1 -> f.shape.activeParam.vLocked = lock.isChecked
            }
        }}

        val shapeParamButtonValues = listOf(R.string.param1, R.string.param2, R.string.param3)

        val shapeParamButtons = v.findViewById<TabLayout>(R.id.shapeParamButtons)
        shapeParamButtons.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            val uEdit = v.findViewById<EditText>(R.id.uEdit)
            val vEdit = v.findViewById<EditText>(R.id.vEdit)
            val uLock = v.findViewById<ToggleButton>(R.id.uLock)
            val vLock = v.findViewById<ToggleButton>(R.id.vLock)

            override fun onTabSelected(tab: TabLayout.Tab) {

                val i = tab.text?.get(1)?.toString()?.toInt() ?: 1
                f.shape.activeParam = f.shape.params[i - 1]

                val param = f.shape.activeParam
                uEdit.setText("%.8f".format(param.u))
                vEdit.setText("%.8f".format(param.v))
                uLock.isChecked = param.uLocked
                vLock.isChecked = param.vLocked

            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) { onTabSelected(tab) }

        })

        val layout = v.findViewById<LinearLayout>(R.id.shapeLayout)
        val preview = v.findViewById<LinearLayout>(R.id.shapePreview)
        val previewImage = (preview.getChildAt(0) as CardView).getChildAt(0) as ImageView
        val previewText = preview.getChildAt(1) as TextView
        val previewList = v.findViewById<RecyclerView>(R.id.shapePreviewList)
        val shapeParamMenu = v.findViewById<ConstraintLayout>(R.id.shapeParamLayout)
        val content = v.findViewById<LinearLayout>(R.id.shapeContent)

        layout.removeView(previewList)
        previewList.visibility = RecyclerView.VISIBLE





        val uEdit = v.findViewById<EditText>(R.id.uEdit)
        val vEdit = v.findViewById<EditText>(R.id.vEdit)
        val uLock = v.findViewById<ToggleButton>(R.id.uLock)
        val vLock = v.findViewById<ToggleButton>(R.id.vLock)
        uEdit.setText("%.8f".format(f.shape.activeParam.u))
        vEdit.setText("%.8f".format(f.shape.activeParam.v))
        uEdit.setOnEditorActionListener(editListener(vEdit) { w: TextView ->
            f.shape.activeParam.u = "${w.text}".formatToDouble() ?: f.shape.activeParam.u
            w.text = "%.8f".format((f.shape.activeParam.u))
            fsv.r.renderToTex = true
        })
        vEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            f.shape.activeParam.v = "${w.text}".formatToDouble() ?: f.shape.activeParam.v
            w.text = "%.8f".format((f.shape.activeParam.v))
            fsv.r.renderToTex = true
        })
        uLock.setOnClickListener(lockListener(0))
        vLock.setOnClickListener(lockListener(1))


        val maxIterBar = v.findViewById<SeekBar>(R.id.maxIterBar)
        val maxIterEdit = v.findViewById<EditText>(R.id.maxIterEdit)
        maxIterBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

                val p = seekBar.progress.toDouble() / 100.0
                val iter = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW).toInt()
                maxIterEdit.setText("%d".format(iter))

            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {

                val p = seekBar.progress.toDouble() / 100.0
                f.maxIter = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW).toInt()
                maxIterEdit.setText("%d".format(f.maxIter))

                Log.d("FRACTAL EDIT FRAGMENT", "maxIter: ${f.maxIter}")
                // f.maxIter = ((2.0.pow(5) - 1)*(1.0f - p) + (2.0.pow(12) - 1)*p).toInt()
                fsv.r.renderToTex = true
                fsv.requestRender()

            }

        })
        maxIterBar.progress = ((log(f.maxIter.toDouble(), 2.0) - ITER_MIN_POW)/(ITER_MAX_POW - ITER_MIN_POW)*100.0).toInt()
        maxIterEdit.setText("%d".format(f.maxIter))
        maxIterEdit.setOnEditorActionListener(editListener(null) {
            f.maxIter = "${it.text}".formatToDouble()?.toInt() ?: f.maxIter
            fsv.r.renderToTex = true
        })



        previewImage.setImageResource(f.shape.icon)
        previewText.text = f.shape.name
        preview.setOnClickListener {
            content.visibility = LinearLayout.GONE
            layout.addView(previewList)
        }



        val juliaLayout = v.findViewById<LinearLayout>(R.id.juliaLayout)
        val juliaModeSwitch = v.findViewById<Switch>(R.id.juliaModeSwitch)
        val juliaListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->

            val prevScale = f.position.scale
            f.juliaMode = isChecked
            fsv.checkThresholdCross(prevScale)

            if (f.juliaMode) {

                shapeParamMenu.visibility = ConstraintLayout.VISIBLE
                // complexMapKatex.setDisplayText(resources.getString(f.shape.katex).format("P${f.numParamsInUse + 1}"))
                shapeParamButtons.addTab(shapeParamButtons.newTab().setText(shapeParamButtonValues[f.numParamsInUse - 1]))
                if (f.numParamsInUse == 1) {
                    fsv.reaction = Reaction.SHAPE
                    (activity as MainActivity).showTouchIcon()
                }

            }
            else {

                shapeParamButtons.removeTabAt(shapeParamButtons.tabCount - 1)
                if (f.numParamsInUse == 0) shapeParamMenu.visibility = ConstraintLayout.GONE
                // complexMapKatex.setDisplayText(resources.getString(f.shape.katex).format("c"))
                if (f.numParamsInUse == 0) fsv.reaction = Reaction.POSITION

            }

            // updateShapeParamEditTexts(f.numParamsInUse)

            (activity as MainActivity).updateDisplayParams(reactionChanged = true)

            fsv.r.renderShaderChanged = true
            fsv.r.renderToTex = true
            fsv.requestRender()

        }
        if (f.shape.juliaMode) juliaLayout.visibility = LinearLayout.GONE
        juliaModeSwitch.setOnCheckedChangeListener(juliaListener)


        previewList.adapter = ShapeAdapter(Shape.all)
        previewList.addOnItemTouchListener(
                RecyclerTouchListener(
                        v.context,
                        previewList,
                        object : ClickListener {

                            override fun onClick(view: View, position: Int) {

                                f.shape = Shape.all[position]
                                previewImage.setImageResource(f.shape.icon)
                                previewText.text = f.shape.name
                                layout.removeView(previewList)
                                content.visibility = LinearLayout.VISIBLE

                                // update juliaModeSwitch
                                if (f.shape.juliaMode) {
                                    juliaLayout.visibility = LinearLayout.GONE
                                }
                                else {
                                    juliaLayout.visibility = LinearLayout.VISIBLE
                                    if (juliaModeSwitch.isChecked) {
                                        juliaModeSwitch.setOnCheckedChangeListener { _, _ -> }
                                        juliaModeSwitch.isChecked = false
                                        juliaModeSwitch.setOnCheckedChangeListener(juliaListener)
                                    }
                                }

                                if (f.numParamsInUse == 0) fsv.reaction = Reaction.POSITION

                                // update parameter display
                                shapeParamMenu.visibility = if (f.shape.numParams == 0) ConstraintLayout.GONE else ConstraintLayout.VISIBLE
                                shapeParamButtons.removeAllTabs()
                                for (i in 0 until f.shape.numParams) shapeParamButtons.addTab(
                                        shapeParamButtons.newTab().setText(shapeParamButtonValues[i])
                                )

                                fsv.r.renderShaderChanged = true
                                fsv.r.renderToTex = true
                                fsv.requestRender()

                                Log.e("MAIN ACTIVITY", "clicked shape: ${f.shape.name}")

                            }

                            override fun onLongClick(view: View, position: Int) {}

                        }
                )
        )


        shapeParamButtons.removeAllTabs()
        for (i in 0 until f.shape.numParams) shapeParamButtons.addTab(
                shapeParamButtons.newTab().setText(shapeParamButtonValues[i])
        )
        if (f.shape.numParams == 0) shapeParamMenu.visibility = ConstraintLayout.GONE



        return v
    }

}