package com.selfsimilartech.fractaleye

import android.animation.LayoutTransition
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.ToggleButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.shape_fragment.*
import java.util.*
import kotlin.math.log
import kotlin.math.pow



class ShapeFragment : MenuFragment() {

    private lateinit var act : MainActivity
    private lateinit var f : Fractal
    private lateinit var fsv : FractalSurfaceView
    private lateinit var sc : SettingsConfig


    private fun loadNavButtons(buttons: List<Button>) {

        shapeNavButtons.removeAllViews()
        buttons.forEach { shapeNavButtons.addView(it) }

    }


    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.shape_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        act = activity as MainActivity
        f = act.f
        fsv = act.fsv
        sc = act.sc

        var customShape = Shape(name = "q", latex = "$$")
        var prevSelectedShapeIndex : Int

        shapeLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        shapePreviewListLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        val shapeList = if (BuildConfig.PAID_VERSION) Shape.all else Shape.all.filter { shape -> !shape.isProFeature }
        val previewListNavButtons = listOf(
                shapeListViewTypeButton,
                newCustomShapeButton.apply { setProFeature(true) },
                shapeDoneButton,
                customShapeEditButton.apply { setProFeature(true) }
        ).filter { BuildConfig.PAID_VERSION || !it.isProFeature() }
        val customShapeNavButtons = listOf(
                customShapeCancelButton,
                customShapeDoneButton
        )

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
                loadActiveParam()
                fsv.r.renderToTex = true
                fsv.requestRender()
            }
        }
        val juliaListener = View.OnClickListener {

            //juliaModeButton.alpha = if (juliaModeButton.isChecked) 1f else 0.5f

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

            //Log.d("SHAPE FRAGMENT", "numParamsInUse: ${f.shape.numParamsInUse}")


            //Log.e("SHAPE FRAGMENT", "julia mode: ${f.shape.juliaMode}")

            loadActiveParam()
            act.updatePositionEditTexts()
            act.updateDisplayParams(reactionChanged = true)

            fsv.r.renderShaderChanged = true
            fsv.r.renderToTex = true
            fsv.requestRender()

        }
        val keyListener = { expr: Expr -> View.OnClickListener {

            //customShape.katex = customShape.katex.replace(cursor, expr.katex)
            Log.e("SHAPE", "expr: ${expr.katex}")
            shapeMathQuill.enterExpr(expr)
            shapeMathQuill.getLatex { setCustomLoop(it, customShape) }

        }}



        if (BuildConfig.PAID_VERSION) {
            shapeMathQuill.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                defaultTextEncodingName = "utf-8"
            }
            val mathQuillHtml = readHtml("mathquill.html")
            shapeMathQuill.loadDataWithBaseURL("file:///android_asset/", mathQuillHtml, "text/html", "UTF-8", null)
        }




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

        shapeResetButton.setOnClickListener {

            f.shape.params.active.reset()
            loadActiveParam()
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
                f.shape.maxIter = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW).toInt() - 1
                maxIterEdit.setText("%d".format(f.shape.maxIter))

                Log.d("FRACTAL EDIT FRAGMENT", "maxIter: ${f.shape.maxIter}")
                // f.shape.maxIter = ((2.0.pow(5) - 1)*(1.0f - p) + (2.0.pow(12) - 1)*p).toInt()
                fsv.r.renderToTex = true
                fsv.requestRender()

            }

        })
        maxIterBar.progress = ((log(f.shape.maxIter.toDouble(), 2.0) - ITER_MIN_POW)/(ITER_MAX_POW - ITER_MIN_POW)*maxIterBar.max).toInt()
        maxIterEdit.setText("%d".format(f.shape.maxIter))
        maxIterEdit.setOnEditorActionListener(editListener(null) {
            val result = "${it.text}".formatToDouble()?.toInt()
            if (result != null) {
                f.shape.maxIter = result
                fsv.r.renderToTex = true
            }
            maxIterEdit.setText("%d".format(f.shape.maxIter))
            maxIterBar.progress = ((log(f.shape.maxIter.toDouble(), 2.0) - ITER_MIN_POW)/(ITER_MAX_POW - ITER_MIN_POW)*maxIterBar.max).toInt()
        })



        shapePreviewImage.setImageResource(f.shape.icon)
        shapePreviewText.text = f.shape.name



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
                            Log.d("SHAPE FRAGMENT", "shape is now ${f.shape.name}")

                            if (f.shape.isCustom) customShapeEditButton.show() else customShapeEditButton.hide()

                            if (f.shape.juliaModeInit) juliaModeButton.hide() else juliaModeButton.show()

                            fsv.r.checkThresholdCross(prevScale)

                            fsv.r.renderShaderChanged = true
                            fsv.r.renderToTex = true
                            fsv.requestRender()

                            act.updateTextureEditTexts()
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




        val subMenuButtonListener = { layout: View, button: Button ->
            View.OnClickListener {
                act.hideTouchIcon()
                showLayout(layout)
                alphaButton(button)
                shapeResetButton.hide()
            }
        }
        val shapeParamButtonListener = { button: Button, paramIndex: Int ->
            View.OnClickListener {
                act.showTouchIcon()
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

            act.hideTouchIcon()
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

                loadNavButtons(previewListNavButtons)

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

                if (fsv.r.isRendering) fsv.r.pauseRender = true

                if (!act.uiIsClosed()) act.uiSetOpen() else MainActivity.Category.SHAPE.onMenuClosed(act)


                shapePreviewImage.setImageResource(f.shape.icon)
                shapePreviewText.text = f.shape.name

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
                    //Log.e("SHAPE", "juliaModeButton isChecked: $isChecked")
                    alpha = if (isChecked) 1f else 0.5f
                    //Log.e("SHAPE", "juliaModeButton alpha: $alpha")
                    if (isChecked) juliaParamButton.show() else juliaParamButton.hide()
                }

                maxIterEdit.setText("%d".format(f.shape.maxIter))
                maxIterBar.progress = ((log(f.shape.maxIter.toDouble(), 2.0) - ITER_MIN_POW)/(ITER_MAX_POW - ITER_MIN_POW)*maxIterBar.max).toInt()


            }, BUTTON_CLICK_DELAY_SHORT)
        }

        newCustomShapeButton.setOnClickListener {
            handler.postDelayed({

                shapePreviewListLayout.hide()
                customShapeLayout.show()
                loadNavButtons(customShapeNavButtons)
                fsv.r.renderProfile = RenderProfile.MANUAL

                val postfix = infixToPostfix(parseEquation("z*z*z + c")!!)
                customShape = Shape(
                        name = "Custom Shape 1",
                        latex = "$$",
                        customId = 0,
                        customLoopSF = postfixToGlsl(postfix, GpuPrecision.SINGLE)!!,
                        customLoopDF = postfixToGlsl(postfix, GpuPrecision.DUAL)!!,
                        positions = PositionList(Position(zoom = 5e0)),
                        hasDualFloat = true
                )


                prevSelectedShapeIndex = shapeList.indexOf(f.shape)
                Shape.all.add(0, customShape)
                f.shape = Shape.all[0]

                fsv.r.renderShaderChanged = true
                fsv.r.renderToTex = true
                fsv.requestRender()

            }, BUTTON_CLICK_DELAY_SHORT)
        }
        customShapeEditButton.setOnClickListener {
            handler.postDelayed({

                shapePreviewListLayout.hide()
                customShapeLayout.show()
                loadNavButtons(customShapeNavButtons)
                fsv.r.renderProfile = RenderProfile.MANUAL

                customShape = f.shape
                shapeMathQuill.setLatex(customShape.latex)

            }, BUTTON_CLICK_DELAY_SHORT)
        }
        customShapeCancelButton.setOnClickListener {
            
            
            
        }
        customShapeDoneButton.setOnClickListener {
            handler.postDelayed({

                shapeMathQuill.getLatex { customShape.latex = it }

                act.uiSetHeight(resources.getDimension(R.dimen.uiLayoutHeight).toInt())

                customShapeLayout.hide()
                shapePreviewLayout.show()
                shapeSubMenuButtons.show()
                shapeNavButtons.hide()
                act.showCategoryButtons()

                shapePreviewList.adapter?.notifyDataSetChanged()

            }, BUTTON_CLICK_DELAY_SHORT)
        }

        zKey.setOnClickListener(keyListener(Expr.z))
        cKey.setOnClickListener(keyListener(Expr.c))
        plusKey.setOnClickListener(keyListener(Expr.add))
        minusKey.setOnClickListener(keyListener(Expr.sub))
        timesKey.setOnClickListener(keyListener(Expr.mult))
        divKey.setOnClickListener(keyListener(Expr.div))
        powKey.setOnClickListener(keyListener(Expr.pow))
        sqrKey.setOnClickListener(keyListener(Expr.sqr))
        inverseKey.setOnClickListener(keyListener(Expr.inv))
        sqrtKey.setOnClickListener(keyListener(Expr.sqrt))
        modulusKey.setOnClickListener(keyListener(Expr.mod))
        conjKey.setOnClickListener(keyListener(Expr.conj))
        argKey.setOnClickListener(keyListener(Expr.arg))

        zeroKey.setOnClickListener(keyListener(Expr.zero))
        oneKey.setOnClickListener(keyListener(Expr.one))
        twoKey.setOnClickListener(keyListener(Expr.two))
        threeKey.setOnClickListener(keyListener(Expr.three))
        fourKey.setOnClickListener(keyListener(Expr.four))
        fiveKey.setOnClickListener(keyListener(Expr.five))
        sixKey.setOnClickListener(keyListener(Expr.six))
        sevenKey.setOnClickListener(keyListener(Expr.seven))
        eightKey.setOnClickListener(keyListener(Expr.eight))
        nineKey.setOnClickListener(keyListener(Expr.nine))
        decimalKey.setOnClickListener(keyListener(Expr.decimal))
        iKey.setOnClickListener(keyListener(Expr.i))

        prevKey.setOnClickListener { shapeMathQuill.enterKeystroke("Left") }
        nextKey.setOnClickListener { shapeMathQuill.enterKeystroke("Right") }
        deleteKey.setOnClickListener {
            shapeMathQuill.enterKeystroke("Backspace")
            shapeMathQuill.getLatex { setCustomLoop(it, customShape) }
        }
        parensKey.setOnClickListener(keyListener(Expr.parens))

        juliaParamButton.setOnClickListener {
            act.showTouchIcon()
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
        customShapeLayout.hide()
        shapeNavButtons.hide()



        //shapePreviewButton.performClick()
        showLayout(shapePreviewLayout)
        alphaButton(shapePreviewButton)



        //act.updateShapeEditTexts()
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


    private fun setCustomLoop(str: String, shape: Shape) {
        val parsed = parseEquation(str)
        if (parsed == null) eqnErrorIndicator.show()
        else {
            val postfix = infixToPostfix(parsed)
            Log.e("SHAPE", "postfix: ${postfix.joinToString(" ")}")
            val sf = postfixToGlsl(postfix, GpuPrecision.SINGLE)
            val df = postfixToGlsl(postfix, GpuPrecision.DUAL)
            if (sf == null || df == null) {
                eqnErrorIndicator.show()
            } else {
                Log.e("SHAPE", "customLoopSF: $sf")
                Log.e("SHAPE", "customLoopDF: $df")
                eqnErrorIndicator.invisible()
                shape.customLoopSF = sf
                shape.customLoopDF = df
                fsv.r.renderShaderChanged = true
                fsv.r.renderToTex = true
                fsv.requestRender()
            }
        }
    }

    private fun parseEquation(input: String) : ArrayList<Expr>? {

        val out = arrayListOf<Expr>()
        var str = input


        str = str.replace("\\\\", "\\")     // replace all \\ with \
                .replace("\\left", "")      // remove all \left
                .replace("\\right", "")     // remove all \right

        Log.e("SHAPE", "str: $str")

        // replace all \\frac{}{} with {}/{}
        while (true) {
            val start = str.indexOf("\\frac")
            //Log.e("SHAPE", "str: $str")
            //Log.e("SHAPE", "start: $start")
            if (start != -1) {
                var left = 0
                var right = 0
                var middleFound = false
                for (i in start + 7 until str.length) {
                    //Log.e("SHAPE", "str[i]: ${str[i]}, left: $left, right: $right")
                    when (str[i]) {
                        '}' -> {
                            if (left == right) {
                                //Log.e("SHAPE", "right bracket at $i")
                                str = str.replaceRange(i, i+2, "}/{")
                                str = str.removeRange(start, start + 5)
                                middleFound = true
                                //Log.e("SHAPE", "str: $str")
                            }
                            else right += 1
                        }
                        '{' -> left += 1
                    }
                    if (middleFound) break
                }
            }
            else break
        }

        // replace all \\cdot with *
        while (true) {
            val start = str.indexOf("\\cdot")
            if (start != -1) {
                str = str.replaceRange(start, start + 5, "*")
            } else break
        }

        str.replace(" ", "")
                .replace("^", " ^ ")
                .replace("*", " * ")
                .replace("/", " / ")
                .replace("+", " + ")
                .replace("-", " - ")
                .replace("(", " ( ")
                .replace(")", " ) ")
                .replace("{", " ( ")
                .replace("}", " ) ")
                .replace("  ", " ")
                .trim()
                .split(" ")
                .forEach {
                    val expr = Expr.valueOf(it)
                    if (expr != null) out.add(expr)
                    else return null
                }

        out.forEachIndexed { i, expr ->
            if (expr == Expr.sub && (i == 0 || out[i - 1] in listOf(Expr.mult, Expr.div, Expr.leftParen))) {
                out[i] = Expr.neg
            }
        }

        Log.e("SHAPE", "parsed equation: ${out.joinToString(" ")}")
        // Log.e("SHAPE", "out size: ${out.size}")
        return out

    }

    private fun infixToPostfix(exprs: ArrayList<Expr>) : ArrayList<Expr> {

        // val expr = input.reversed()
        // z*(2.0*z - 1.0) + (c - 1.0)^2.0
        exprs.reverse()
        exprs.forEachIndexed { i, expr ->
            exprs[i] = when (expr) {
                Expr.leftParen -> Expr.rightParen
                Expr.rightParen -> Expr.leftParen
                else -> expr
            }
        }
        exprs.add(Expr.rightParen)
        val stack = Stack<Expr>().apply { push(Expr.leftParen) }
        val out = arrayListOf<Expr>()

        while (stack.isNotEmpty()) {

            //Log.e("SHAPE", "expr: " + exprs.joinToString(separator = ""))
            //Log.e("SHAPE", "stack: " + stack.joinToString(separator = " "))
            //Log.e("SHAPE", "out: " + out.joinToString(separator = " "))

            when (val element = exprs.removeAt(0)) {
                is Operator -> {
                    while (stack.peek() != Expr.leftParen && (stack.peek() as Operator).order <= element.order) {
                        out.add(stack.pop())
                    }
                    stack.add(element)
                }
                Expr.leftParen -> stack.push(element)
                Expr.rightParen -> {
                    while (stack.peek() != Expr.leftParen) out.add(stack.pop())
                    stack.pop()
                }
                else -> out.add(element)  // operand
            }
        }

        // Log.e("SHAPE", "out: " + out.joinToString(separator = " "))
        return out

    }

    private fun postfixToGlsl(input: ArrayList<Expr>, precision: GpuPrecision) : String? {

        val exprs = ArrayList(input)
        val stack = Stack<Expr>()

        while (exprs.isNotEmpty()) {

            //Log.e("SHAPE", "stack: ${stack.joinToString()}")

            when (val expr = exprs.removeAt(0)) {
                is Operator -> {
                    val args = arrayListOf<Expr>()
                    for (i in 0 until expr.numArgs) {
                        if (stack.isEmpty()) return null
                        args.add(stack.pop())
                    }
                    stack.push(expr.of(args, precision == GpuPrecision.DUAL))
                }
                else -> stack.push(expr)
            }

        }

        return when (stack.size) {
            0 -> null
            1 -> stack.pop().str
            else -> null
        }

    }

    private fun readHtml(file: String) : String {

        var str = ""
        val br = act.resources.assets.open(file).bufferedReader()
        var line: String?

        while (br.readLine().also { line = it } != null) str += line + "\n"
        br.close()

        //Log.d("RENDERER", str)
        //Log.e("RENDERER", "color shader length: ${str.length}")

        return str

    }

}