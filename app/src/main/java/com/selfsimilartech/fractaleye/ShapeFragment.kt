package com.selfsimilartech.fractaleye

import android.content.Context
import android.content.res.ColorStateList
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.ToggleButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.log
import kotlin.math.pow
import kotlinx.android.synthetic.main.shape_fragment.*
import java.text.NumberFormat
import java.text.ParseException


class ShapeFragment : MenuFragment() {

    private lateinit var act : MainActivity
    private lateinit var f : Fractal
    private lateinit var fsv : FractalSurfaceView
    private lateinit var sc : SettingsConfig


    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.shape_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        act = activity as MainActivity
        f = act.f
        fsv = act.fsv
        sc = act.sc
        val shapeList = if (BuildConfig.PAID_VERSION) Shape.all else Shape.all.filter { shape -> !shape.proFeature }

        val handler = Handler()
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
                    Log.d("SHAPE FRAGMENT", "some other action")
                }
            }

            fsv.requestRender()
            editText.clearFocus()
            act.updateSystemUI()
            true

        }}
        val lockListener = { j: Int -> View.OnClickListener {
            val lock = it as android.widget.ToggleButton
            val param = f.shape.params.active
            if (j == 0) param.uLocked = lock.isChecked
            else if (j == 1 && param is Shape.ComplexParam) {
                param.vLocked = lock.isChecked
            }
        }}
        val linkListener = View.OnClickListener {
            val link = it as ToggleButton
            link.foregroundTintList = ColorStateList.valueOf(resources.getColor(
                    if (link.isChecked) R.color.white else R.color.colorDarkSelected, null
            ))
            val param = f.shape.params.active
            if (param is Shape.ComplexParam) param.linked = link.isChecked
            if (link.isChecked) {
                act.updateShapeEditTexts()
                fsv.r.renderToTex = true
                fsv.requestRender()
            }
        }
        val juliaListener = View.OnClickListener {

            juliaModeButton.alpha = if (juliaModeButton.isChecked) 1f else 0.5f

            val prevScale = f.position.zoom
            f.shape.juliaMode = juliaModeButton.isChecked
            f.position = if (juliaModeButton.isChecked) f.shape.positions.julia else f.shape.positions.default
            fsv.r.checkThresholdCross(prevScale)

            if (f.shape.juliaMode) {

                juliaParamButton.show()
                juliaParamButton.performClick()

                // complexMapKatex.setDisplayText(resources.getString(f.shape.katex).format("P${f.shape.numParamsInUse + 1}"))
//                shapeParamButtons.addTab(shapeParamButtons.newTab().setText(resources.getString(R.string.julia)))
//                shapeParamButtons.getTabAt(shapeParamButtons.tabCount - 1)?.select()
//                handler.postDelayed({
//                    shapeLayoutScroll.smoothScrollTo(0, shapeParamLayout.y.toInt())
//                }, BUTTON_CLICK_DELAY_LONG)
                if (f.shape.numParamsInUse == 1) {
                    fsv.r.reaction = Reaction.SHAPE
                    act.showTouchIcon()
                }
            }
            else {

                if (currentButton == juliaParamButton) {
                    if (f.shape.numParamsInUse > 0) shapeParamButton1.performClick()
                    else {
                        shapePreviewButton.performClick()
                        shapeResetButton.hide()
                    }
                }
                juliaParamButton.hide()

//                if (f.shape.numParamsInUse == 0) shapeParamLayout.visibility = ConstraintLayout.GONE
//                // complexMapKatex.setDisplayText(resources.getString(f.shape.katex).format("c"))
//                if (f.shape.numParamsInUse == 0) fsv.r.reaction = Reaction.NONE

            }

            Log.d("SHAPE FRAGMENT", "numParamsInUse: ${f.shape.numParamsInUse}")


            Log.e("SHAPE FRAGMENT", "julia mode: ${f.shape.juliaMode}")

            act.updateShapeEditTexts()
            act.updatePositionEditTexts()
            act.updateDisplayParams(reactionChanged = true)

            fsv.r.renderShaderChanged = true
            fsv.r.renderToTex = true
            fsv.requestRender()

        }

        val shapeParamButtonValues = listOf(R.string.param1, R.string.param2, R.string.param3)
        realParamSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val param = f.shape.params.active
                param.u = (progress.toDouble()/realParamSeekBar.max)*(param.uRange.upper - param.uRange.lower) + param.uRange.lower
                uEdit2.setText("%.3f".format(param.u))
                fsv.r.renderToTex = true
                fsv.requestRender()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })

//        shapeParamButtons.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//
//            override fun onTabSelected(tab: TabLayout.Tab) {
//
//                f.shape.params.active =
//                        if (tab.text.toString() == resources.getString(R.string.julia)) f.shape.params.julia
//                        else f.shape.params.at(tab.position)
//
//                val param = f.shape.params.active
//                if (complexParamLayout.isHidden() && param is Shape.ComplexParam) {
//                    realParamLayout.hide()
//                    complexParamLayout.show()
//                }
//                else if (realParamLayout.isHidden() && param !is Shape.ComplexParam) {
//                    complexParamLayout.hide()
//                    realParamLayout.show()
//                }
//
//
//                if (param is Shape.ComplexParam) {
//                    uEdit.setText("%.8f".format(param.u))
//                    uLock.isChecked = param.uLocked
//                    vEdit.setText("%.8f".format(param.v))
//                    vLock.isChecked = param.vLocked
//                    linkParamButton.isChecked = param.linked
//                    linkParamButton.foregroundTintList = ColorStateList.valueOf(resources.getColor(
//                            if (linkParamButton.isChecked) R.color.white else R.color.colorDarkSelected, null
//                    ))
//                }
//                else {
//                    uEdit2.setText("%.3f".format(param.u))
//                    realParamSeekBar.progress = (100.0*(param.u - param.uRange.lower)/(param.uRange.upper - param.uRange.lower)).toInt()
//                }
//
//                act.showTouchIcon()
//
//            }
//
//            override fun onTabUnselected(tab: TabLayout.Tab) {
//
//            }
//
//            override fun onTabReselected(tab: TabLayout.Tab) { onTabSelected(tab) }
//
//        })
        shapeResetButton.setOnClickListener {

            f.shape.params.active.reset()
            loadActiveParam()
            act.updateShapeEditTexts()
            fsv.r.renderToTex = true
            fsv.requestRender()

        }




        val activeParam = f.shape.params.active
        uEdit.setText("%.8f".format(activeParam.u))
        if (activeParam is Shape.ComplexParam) {
            vEdit.setText("%.8f".format(activeParam.v))
        }
        uEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = "${w.text}".formatToDouble()
            val param = f.shape.params.active
            if (result != null) {
                param.u = result
                fsv.r.renderToTex = true
            }
            w.text = "%.8f".format((f.shape.params.active.u))
            if (param is Shape.ComplexParam && param.linked) {
                vEdit.setText("%.8f".format((param.v)))
            }
        })
        uEdit2.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = "${w.text}".formatToDouble()
            val param = f.shape.params.active
            if (result != null) {
                param.u = result
                fsv.r.renderToTex = true
            }
            w.text = "%.3f".format((f.shape.params.active.u))
            if (param is Shape.ComplexParam && param.linked) {
                vEdit.setText("%.3f".format((param.v)))
            }
            realParamSeekBar.progress = (realParamSeekBar.max*(param.u - param.uRange.lower)/(param.uRange.upper - param.uRange.lower)).toInt()
        })
        vEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = "${w.text}".formatToDouble()
            val param = f.shape.params.active
            if (param is Shape.ComplexParam) {
                if (result != null) {
                    param.v = result
                    fsv.r.renderToTex = true
                }
                w.text = "%.8f".format((param.v))
            }
        })
        uLock.setOnClickListener(lockListener(0))
        vLock.setOnClickListener(lockListener(1))
        linkParamButton.setOnClickListener(linkListener)


        maxIterBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

                val p = seekBar.progress.toDouble() / maxIterBar.max
                val iter = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW).toInt()
                maxIterEdit.setText("%d".format(iter))

            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {

                val p = seekBar.progress.toDouble() / maxIterBar.max
                f.maxIter = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW).toInt()
                maxIterEdit.setText("%d".format(f.maxIter))

                Log.d("FRACTAL EDIT FRAGMENT", "maxIter: ${f.maxIter}")
                // f.maxIter = ((2.0.pow(5) - 1)*(1.0f - p) + (2.0.pow(12) - 1)*p).toInt()
                fsv.r.renderToTex = true
                fsv.requestRender()

            }

        })
        maxIterBar.progress = ((log(f.maxIter.toDouble(), 2.0) - ITER_MIN_POW)/(ITER_MAX_POW - ITER_MIN_POW)*maxIterBar.max).toInt()
        maxIterEdit.setText("%d".format(f.maxIter))
        maxIterEdit.setOnEditorActionListener(editListener(null) {
            val result = "${it.text}".formatToDouble()?.toInt()
            if (result != null) {
                f.maxIter = result
                fsv.r.renderToTex = true
            }
            maxIterEdit.setText("%d".format(f.maxIter))
        })



        shapePreviewImage.setImageResource(f.shape.icon)
        shapePreviewText.text = resources.getString(f.shape.name)



//        if (f.shape.juliaMode) juliaLayout.visibility = LinearLayout.GONE
        if (f.shape.juliaMode) juliaModeButton.isChecked = true
        juliaModeButton.setOnClickListener(juliaListener)


        // create and set preview list adapter/manager
        val shapePreviewGridAdapter = ShapeAdapter(shapeList, R.layout.texture_shape_preview_item_grid)
        val shapePreviewGridManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
        val shapePreviewLinearAdapter = ShapeAdapter(shapeList, R.layout.shape_preview_item_linear)
        val shapePreviewLinearManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        when (sc.shapeListViewType) {

            ListLayoutType.GRID -> {
                shapePreviewList.adapter = shapePreviewGridAdapter
                shapePreviewList.layoutManager = shapePreviewGridManager
            }
            ListLayoutType.LINEAR -> {
                shapePreviewList.adapter = shapePreviewLinearAdapter
                shapePreviewList.layoutManager = shapePreviewLinearManager
            }

        }

        shapePreviewList.addOnItemTouchListener(RecyclerTouchListener(
                v.context,
                shapePreviewList,
                object : ClickListener {

                    override fun onClick(view: View, position: Int) {

                        if (shapeList[position] != f.shape) {


                            // reset texture if not compatible with new shape
                            if (!shapeList[position].compatTextures.contains(f.texture)) {
                                f.texture = Texture.exponentialSmoothing
                                act.updateTexturePreviewName()
                            }

                            val prevScale = f.position.zoom
                            f.shape = shapeList[position]
                            Log.d("SHAPE FRAGMENT", "shape is now ${resources.getString(f.shape.name)}")

                            if (f.shape.juliaModeInit) juliaModeButton.hide() else juliaModeButton.show()

                            fsv.r.checkThresholdCross(prevScale)

                            fsv.r.renderShaderChanged = true
                            fsv.r.renderToTex = true
                            fsv.requestRender()

                            act.updatePositionEditTexts()

                        }

                    }
                    override fun onLongClick(view: View, position: Int) {}

                }
        ))



        val shapeParamButtonList = listOf(
                shapeParamButton1,
                shapeParamButton2,
                shapeParamButton3,
                shapeParamButton4
        )
        shapeParamButtonList.forEach { it.hide() }
        f.shape.params.list.forEachIndexed { index, param ->
            shapeParamButtonList[index].apply {
                show()
                text = param.name
            }
        }
        if (f.shape.juliaMode) juliaParamButton.show() else juliaParamButton.hide()
        //if (f.shape.numParamsInUse == 0) shapeParamLayout.visibility = ConstraintLayout.GONE




        val subMenuButtonListener = { layout: View, button: Button ->
            View.OnClickListener {
                showLayout(layout)
                alphaButton(button)
                shapeResetButton.hide()
            }
        }
        val shapeParamButtonListener = { button: Button, paramIndex: Int ->
            View.OnClickListener {
                fsv.r.reaction = Reaction.SHAPE
                f.shape.params.active = f.shape.params.list[paramIndex]
                showLayout(if (f.shape.params.list[paramIndex] is Shape.ComplexParam) complexParamLayout else realParamLayout)
                shapeResetButton.show()
                alphaButton(button)
                loadActiveParam()
            }
        }


        // CLICK LISTENERS
        shapePreviewButton.setOnClickListener {

            showLayout(shapePreviewLayout)
            alphaButton(shapePreviewButton)
            shapeResetButton.hide()
            fsv.r.reaction = Reaction.NONE

        }
        shapePreviewLayout.setOnClickListener {
            handler.postDelayed({

                shapeSubMenuButtons.hide()
                shapePreviewLayout.hide()
                act.hideCategoryButtons()

                shapeNavButtons.show()
                shapePreviewListLayout.show()

                act.uiSetHeight(resources.getDimension(R.dimen.uiLayoutHeightTall).toInt())


            }, BUTTON_CLICK_DELAY_LONG)
        }

        maxIterButton.setOnClickListener(subMenuButtonListener(maxIterLayout, maxIterButton))

        shapeListViewTypeButton.setOnClickListener {

            sc.shapeListViewType = ListLayoutType.values().run {
                get((sc.shapeListViewType.ordinal + 1) % size)
            }

            when (sc.shapeListViewType) {

                ListLayoutType.LINEAR -> {
                    shapePreviewList.adapter = shapePreviewLinearAdapter
                    shapePreviewList.layoutManager = shapePreviewLinearManager
                }
                ListLayoutType.GRID -> {
                    shapePreviewList.adapter = shapePreviewGridAdapter
                    shapePreviewList.layoutManager = shapePreviewGridManager
                }

            }

        }
        shapeDoneButton.setOnClickListener {
            handler.postDelayed({

                act.uiSetHeight(resources.getDimension(R.dimen.uiLayoutHeight).toInt())


                shapePreviewImage.setImageResource(f.shape.icon)
                shapePreviewText.text = resources.getString(f.shape.name)

                shapePreviewListLayout.hide()
                shapePreviewLayout.show()
                shapeSubMenuButtons.show()
                shapeNavButtons.hide()
                act.showCategoryButtons()

                // update parameter display
                shapeParamButtonList.forEach { it.hide() }
                f.shape.params.list.forEachIndexed { index, param ->
                    shapeParamButtonList[index].apply {
                        text = param.name
                        show()
                    }
                }


                // update juliaModeButton
                juliaModeButton.apply {
                    isChecked = f.shape.juliaMode
                    Log.e("SHAPE", "juliaModeButton isChecked: $isChecked")
                    alpha = if (isChecked) 1f else 0.5f
                    Log.e("SHAPE", "juliaModeButton alpha: $alpha")
                    if (isChecked) juliaParamButton.show() else juliaParamButton.hide()
                }


            }, BUTTON_CLICK_DELAY_SHORT)
        }

        juliaParamButton.setOnClickListener {
            showLayout(complexParamLayout)
            alphaButton(juliaParamButton)
            shapeResetButton.show()
            f.shape.params.active = f.shape.params.julia
            fsv.r.reaction = Reaction.SHAPE
            loadActiveParam()
        }
        shapeParamButtonList.forEachIndexed { index, button ->
            button.setOnClickListener(shapeParamButtonListener(button, index))
        }


        currentLayout = shapePreviewLayout
        currentButton = shapePreviewButton
        shapePreviewLayout.hide()
        maxIterLayout.hide()
        realParamLayout.hide()
        complexParamLayout.hide()
        shapeResetButton.hide()

        shapePreviewListLayout.hide()
        shapeNavButtons.hide()



        //shapePreviewButton.performClick()
        showLayout(shapePreviewLayout)
        alphaButton(shapePreviewButton)



        act.updateShapeEditTexts()
        super.onViewCreated(v, savedInstanceState)

    }

    fun loadActiveParam() {
        val param = f.shape.params.active
        if (param is Shape.ComplexParam) {
            uEdit.setText("%.8f".format(param.u))
            uLock.isChecked = param.uLocked
            vEdit.setText("%.8f".format(param.v))
            vLock.isChecked = param.vLocked
            linkParamButton.isChecked = param.linked
            linkParamButton.foregroundTintList = ColorStateList.valueOf(resources.getColor(
                    if (linkParamButton.isChecked) R.color.white else R.color.colorDarkSelected, null
            ))
        }
        else {
            uEdit2.setText("%.3f".format(param.u))
            realParamSeekBar.progress = (realParamSeekBar.max*(param.u - param.uRange.lower)/(param.uRange.upper - param.uRange.lower)).toInt()
        }
    }

}