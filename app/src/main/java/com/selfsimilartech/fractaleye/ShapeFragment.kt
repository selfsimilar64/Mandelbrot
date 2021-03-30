package com.selfsimilartech.fractaleye

import android.animation.LayoutTransition
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import kotlinx.android.synthetic.main.shape_fragment.*
import kotlinx.android.synthetic.main.complex_param.view.*
import kotlinx.android.synthetic.main.real_param.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.pow



class ShapeFragment : MenuFragment() {



    private var realParamSeekBarListener : SeekBar.OnSeekBarChangeListener? = null
    private lateinit var shapeListAdapter : ListAdapter<Shape>


    private fun loadNavButtons(buttons: List<Button>) {

        shapeNavButtons.removeAllViews()
        buttons.forEach { shapeNavButtons.addView(it) }

    }


    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.shape_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        super.onViewCreated(v, savedInstanceState)



        var customShape = Shape(name = "q", latex = "$$")
        var prevSelectedShapeIndex = 0

        shapeLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        shapePreviewListLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        val previewListNavButtons = listOf(
                shapeListViewTypeButton,
                customShapeNewButton,
                shapeListDoneButton
        )
        val customShapeNavButtons = listOf(
                customShapeCancelButton,
                customShapeDoneButton.apply { showGradient = true }
        )
        val nonClickableViewTypes = listOf(
                R.layout.list_header,
                R.layout.list_item_linear_empty_favorite,
                R.layout.list_item_linear_empty_custom
        )

        val handler = Handler(Looper.getMainLooper())
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
            val lock = it as ToggleButton
            val param = f.shape.params.active
            if (j == 0) param.uLocked = lock.isChecked
            else if (j == 1 && param is ComplexParam) {
                param.vLocked = lock.isChecked
            }
        }}
        val linkListener = View.OnClickListener {
            val link = it as ToggleButton
            link.foregroundTintList = ColorStateList.valueOf(resources.getColor(
                    if (link.isChecked) R.color.white else R.color.colorDarkSelected, null
            ))
            val param = f.shape.params.active
            if (param is ComplexParam) param.linked = link.isChecked
            if (link.isChecked) {
                loadActiveParam()
                fsv.r.renderToTex = true
                fsv.requestRender()
            }
        }
        val juliaListener = View.OnClickListener {

            if (f.shape != Shape.mandelbrot && !sc.goldEnabled) act.showUpgradeScreen()
            else {

                val prevScale = f.shape.position.zoom
                f.shape.juliaMode = juliaModeButton.isChecked
                fsv.r.checkThresholdCross(prevScale)

                if (f.shape.juliaMode) {

                    seedParamButton.hide()
                    juliaParamButton.show()
                    juliaParamButton.performClick()

                    if (f.shape.numParamsInUse == 1) {
                        fsv.r.reaction = Reaction.SHAPE
                        act.showTouchIcon()
                    }

                } else {

                    if (currentButton == juliaParamButton) {
                        if (f.shape.numParamsInUse > 0) shapeParamButton1.performClick()
                        else {
                            maxIterButton.performClick()
                            // shapeResetButton.hide()
                        }
                    }
                    if (f.shape.juliaSeed) seedParamButton.hide() else seedParamButton.show()
                    juliaParamButton.hide()

                }

                loadActiveParam()
                act.updatePositionEditTexts()
                act.updateDisplayParams(reactionChanged = true)

                fsv.r.renderShaderChanged = true
                fsv.r.renderToTex = true
                fsv.requestRender()

            }

        }
        val keyListener = { expr: Expr -> View.OnClickListener {

            //customShape.katex = customShape.katex.replace(cursor, expr.katex)
            Log.e("SHAPE", "expr: ${expr.latex}")
            shapeMathQuill.enterExpr(expr)
            shapeMathQuill.getLatex { setCustomLoop(it, customShape) }

        }}
        val rateListener = View.OnClickListener {

            if (f.shape.params.active.sensitivity != null) {
                if (f.shape.params.active is ComplexParam) showLayout(realShapeParam)
                f.shape.params.active = f.shape.params.active.sensitivity!!
                realShapeParam.realRateButton.apply {
                    setText(android.R.string.ok)
                    setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.done, 0, 0)
                }
            }
            else if (f.shape.params.active.parent != null) {
                if (f.shape.params.active.parent is ComplexParam) showLayout(complexShapeParam)
                f.shape.params.active = f.shape.params.active.parent!!
                realShapeParam.realRateButton.apply {
                    setText(R.string.sensitivity)
                    setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.sensitivity, 0, 0)
                }
            }

            loadActiveParam()

        }
        val resetListener = View.OnClickListener {

            f.shape.params.active.reset()
            loadActiveParam()
            fsv.r.renderToTex = true
            fsv.requestRender()

        }



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



        realShapeParam.apply {

            realParamSeekBarListener = object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val param = f.shape.params.active
                    param.u = (progress.toDouble()/realParamSeekBar.max)*(param.uRange.upper - param.uRange.lower) + param.uRange.lower
                    uEdit2.setText("%.3f".format(param.u))
                    if (sc.continuousParamRender && f.shape.params.active.isPrimary) {
                        fsv.r.renderToTex = true
                        fsv.requestRender()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    if (sc.continuousParamRender && f.shape.params.active.isPrimary) {
                        fsv.r.renderProfile = RenderProfile.CONTINUOUS
                        fsv.r.renderToTex = true
                        fsv.requestRender()
                    }
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (sc.continuousParamRender) fsv.r.renderProfile = RenderProfile.DISCRETE
                    if (f.shape.params.active.isPrimary) {
                        fsv.r.renderToTex = true
                        fsv.requestRender()
                    }
                }

            }
            realParamSeekBar.setOnSeekBarChangeListener(realParamSeekBarListener)
            realRateButton.setOnClickListener(rateListener)
            realResetButton.setOnClickListener(resetListener)
            uEdit2.setOnEditorActionListener(editListener(null) { w: TextView ->
                val result = "${w.text}".formatToDouble()
                val param = f.shape.params.active
                if (result != null) {
                    param.u = result
                    fsv.r.renderToTex = true
                }
                w.text = "%.3f".format((f.shape.params.active.u))
                if (param is ComplexParam && param.linked) {
                    vEdit.setText("%.3f".format((param.v)))
                }
                realParamSeekBar.progress = (realParamSeekBar.max * (param.u - param.uRange.lower) / (param.uRange.upper - param.uRange.lower)).toInt()
            })

        }
        complexShapeParam.apply {

            val activeParam = f.shape.params.active

            uEdit.setText("%.8f".format(activeParam.u))
            if (activeParam is ComplexParam) vEdit.setText("%.8f".format(activeParam.v))

            uEdit.setOnEditorActionListener(editListener(vEdit) { w: TextView ->
                val result = "${w.text}".formatToDouble()
                val param = f.shape.params.active
                if (result != null) {
                    param.u = result
                    fsv.r.renderToTex = true
                }
                w.text = "%.8f".format((f.shape.params.active.u))
                if (param is ComplexParam && param.linked) {
                    vEdit.setText("%.8f".format((param.v)))
                }
            })
            vEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
                val result = "${w.text}".formatToDouble()
                val param = f.shape.params.active
                if (param is ComplexParam) {
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
            complexResetButton.setOnClickListener(resetListener)
            complexRateButton.setOnClickListener(rateListener)

        }


        shapeButtonsScroll.setOnScrollChangeListener(scrollListener(
                shapeButtonsScrollLayout,
                shapeButtonsScroll,
                shapeScrollArrowLeft,
                shapeScrollArrowRight
        ))
        shapeScrollArrowLeft.invisible()
        shapeScrollArrowRight.invisible()


        maxIterBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

                val p = seekBar.progress.toDouble() / maxIterBar.max
                val iter = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW).toInt()
                if (iter > 5000) iterWarningIcon.show() else iterWarningIcon.hide()
                f.shape.maxIter = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW).toInt() - 1
                maxIterEdit.setText("%d".format(iter))
                if (fsv.r.renderProfile == RenderProfile.CONTINUOUS) {
                    fsv.r.renderToTex = true
                    fsv.requestRender()
                }

            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {

                if (fsv.r.isRendering) fsv.r.interruptRender = true

                fsv.r.renderProfile = RenderProfile.CONTINUOUS

                val p = seekBar.progress.toDouble() / maxIterBar.max
                f.shape.maxIter = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW).toInt() - 1
                maxIterEdit.setText("%d".format(f.shape.maxIter))

            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {

                fsv.r.renderProfile = RenderProfile.DISCRETE
                fsv.r.renderToTex = true
                fsv.requestRender()

                val p = seekBar.progress.toDouble() / maxIterBar.max
                val newIter = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW).toInt() - 1

                // save state on iteration increase
                if (newIter > f.shape.maxIter) act.bookmarkAsPreviousFractal()

                f.shape.maxIter = newIter
                maxIterEdit.setText("%d".format(f.shape.maxIter))

                // Log.d("FRACTAL EDIT FRAGMENT", "maxIter: ${f.shape.maxIter}")

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
        maxIterBar.showWarning = true
        iterWarningIcon.hide()

        customShapeName.setOnEditorActionListener(editListener(null) {
            customShape.name = customShapeName.text.toString()
        })



        shapePreviewImage.setImageResource(f.shape.thumbnailId)
        shapePreviewText.text = f.shape.name



        if (f.shape.juliaMode) juliaModeButton.isChecked = true
        juliaModeButton.setOnClickListener(juliaListener)



        val previewListWidth = Resolution.SCREEN.w - 2*resources.getDimension(R.dimen.categoryPagerMarginHorizontal) - resources.getDimension(R.dimen.navButtonSize)
        val previewGridWidth = resources.getDimension(R.dimen.textureShapePreviewSize) + 2*resources.getDimension(R.dimen.previewGridPaddingHorizontal)
        val spanCount = floor(previewListWidth.toDouble() / previewGridWidth).toInt()
        val shapePreviewListLinearManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val shapePreviewListGridManager = GridLayoutManager(context, spanCount)


        val onEditConfirm = { adapter: ListAdapter<Shape>, item: ListItem<Shape> ->

            shapePreviewListLayout.hide()
            customShapeLayout.show()
            eqnErrorIndicator.invisible()
            loadNavButtons(customShapeNavButtons)
            fsv.r.renderProfile = RenderProfile.DISCRETE
            fsv.r.reaction = Reaction.COLOR


            customShape = item.t
            f.shape = customShape
            f.texture = if (f.shape.isConvergent) Texture.converge else Texture.escapeSmooth
            val prevZoom = f.shape.position.zoom
            f.shape.reset()
            fsv.r.checkThresholdCross(prevZoom)

            customShapeName.setText(customShape.name)
            shapeMathQuill.setLatex(customShape.latex)

            fsv.r.renderShaderChanged = true
            fsv.r.renderToTex = true
            fsv.requestRender()

        }
        val onEditCustomShape = { adapter: ListAdapter<Shape>, item: ListItem<Shape> ->

            if (Fractal.bookmarks.any { it.shape == item.t }) {
                val dialog = AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom)
                        .setIcon(R.drawable.warning)
                        .setTitle("${resources.getString(R.string.edit)} ${item.t.name}?")
                        .setMessage(resources.getString(R.string.edit_shape_bookmark_warning).format(
                                Fractal.bookmarks.count { it.shape == item.t }
                        ))
                        .setPositiveButton(R.string.edit) { dialog, which -> onEditConfirm(adapter, item) }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
            }
            else onEditConfirm(adapter, item)

        }
        val onDeleteCustomShape = { adapter: ListAdapter<Shape>, item: ListItem<Shape> ->

            val dialog = AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom)
                    .setTitle("${resources.getString(R.string.delete)} ${item.t.name}?")
                    .setMessage(resources.getString(R.string.delete_shape_bookmark_warning).format(
                            Fractal.bookmarks.count { it.shape == item.t }
                    ))
                    .setIcon(R.drawable.warning)
                    .setPositiveButton(R.string.delete) { dialog, whichButton ->

                        adapter.removeItemFromCustom(item)
                        Fractal.bookmarks.filter { it.shape == item.t }.forEach { bookmark ->
                            File(requireContext().filesDir.path + bookmark.thumbnailPath).delete()
                            act.db.fractalDao().apply {
                                delete(findById(bookmark.customId))
                            }
                        }

                        val deleteId = item.t.id

                        GlobalScope.launch {
                            act.db.shapeDao().apply {
                                delete(findById(deleteId))
                            }
                        }

                        adapter.apply {
                            setActivatedPosition(
                                    getGlobalPositionOf(getFavoriteItems().getOrNull(0) ?: getDefaultItems()[1])
                            )
                            f.shape = (getItem(activatedPos) as? ShapeListItem)!!.shape
                        }
                        Shape.all.remove(item.t)
                        Shape.custom.remove(item.t)

                        fsv.r.renderShaderChanged = true
                        fsv.r.renderToTex = true
                        fsv.requestRender()

                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()

        }


        val emptyFavorite = ShapeListItem(Shape.emptyFavorite, ListHeader.favorites, sc.shapeListViewType)
        val emptyCustom = ShapeListItem(Shape.emptyCustom, ListHeader.custom, sc.shapeListViewType)
        val listItems = arrayListOf<ShapeListItem>()

        
        
        Shape.all.forEach { listItems.add(

                ShapeListItem(
                        it,
                        if (it.hasCustomId || it == Shape.emptyCustom) ListHeader.custom else ListHeader.default,
                        sc.shapeListViewType,
                        sc.goldEnabled,
                        it == Shape.emptyCustom

                ).apply {

                    if (it.isFavorite) {
                        val favorite = ShapeListItem(
                                it,
                                ListHeader.favorites,
                                sc.shapeListViewType,
                                sc.goldEnabled,
                                compliment = this
                        )
                        compliment = favorite
                        listItems.add(favorite)
                    }

                })

        }
        if (Shape.all.none { it.isFavorite }) listItems.add(emptyFavorite)
        if (Shape.custom.isEmpty()) listItems.add(emptyCustom)
        listItems.sortBy { it.header.type }
        
        
        
        shapeListAdapter = ListAdapter(
                listItems,
                onEditCustomShape,
                onDeleteCustomShape,
                emptyFavorite,
                emptyCustom
        )
        shapePreviewList.adapter = shapeListAdapter
        shapeListAdapter.apply {
            //isLongPressDragEnabled = true
            mode = SelectableAdapter.Mode.SINGLE
            showAllHeaders()
            //setAnimationOnForwardScrolling(true)
            //setAnimationOnReverseScrolling(true)
        }
        shapeListAdapter.mItemClickListener = FlexibleAdapter.OnItemClickListener { view, position ->

            if (shapeListAdapter.getItemViewType(position) !in nonClickableViewTypes) {

                val prevActivatedPosition = shapeListAdapter.activatedPos
                if (position != shapeListAdapter.activatedPos) shapeListAdapter.setActivatedPosition(position)
                val newShape: Shape = try {
                    shapeListAdapter.getActivatedItem()?.t ?: f.shape
                } catch (e: IndexOutOfBoundsException) {
                    Log.e("SHAPE", "array index out of bounds -- index: $position")
                    act.showMessage(resources.getString(R.string.msg_error))
                    f.shape
                }

                if (newShape != f.shape) {

                    if (newShape.goldFeature && !sc.goldEnabled) {
                        shapeListAdapter.setActivatedPosition(prevActivatedPosition)
                        act.showUpgradeScreen()
                    }
                    else {

                        // reset texture if not compatible with new shape
                        if (!newShape.compatTextures.contains(f.texture)) {
                            f.texture = if (newShape == Shape.kleinian) Texture.escape else { if (newShape.isConvergent) Texture.converge else Texture.escapeSmooth }
                            act.onTextureChanged()
                        }

                        val prevScale = f.shape.position.zoom
                        f.shape = newShape
                        // Log.d("SHAPE FRAGMENT", "shape is now ${f.shape.name}")

                        fsv.r.checkThresholdCross(prevScale)

                        fsv.r.renderShaderChanged = true
                        fsv.r.renderToTex = true
                        fsv.requestRender()

                        act.updateTextureEditTexts()
                        act.updatePositionEditTexts()

                    }

                }
                true //Important!

            }
            else false

        }
        shapePreviewListGridManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (shapeListAdapter.getItemViewType(position) in nonClickableViewTypes) spanCount else 1
            }
        }
        shapePreviewList.layoutManager = when (sc.shapeListViewType) {
            ListLayoutType.LINEAR -> shapePreviewListLinearManager
            ListLayoutType.GRID -> shapePreviewListGridManager
        }
        
        



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
                text = if (param.name == "") "Param ${index + 1}" else param.name
            }
        }
        if (f.shape.juliaMode) juliaParamButton.show() else juliaParamButton.hide()




        val subMenuButtonListener = { layout: View, button: Button ->
            View.OnClickListener {
                act.hideTouchIcon()
                showLayout(layout)
                alphaButton(button)
                // shapeResetButton.hide()
            }
        }
        val shapeParamButtonListener = { button: GradientButton, paramIndex: Int ->
            View.OnClickListener {

                if (button.showGradient && !sc.goldEnabled) act.showUpgradeScreen()
                else {
                    act.showTouchIcon()
                    fsv.r.reaction = Reaction.SHAPE
                    f.shape.params.active = f.shape.params.list[paramIndex]
                    showLayout(if (f.shape.params.list[paramIndex] is ComplexParam) complexShapeParam else realShapeParam)
                    // shapeResetButton.show()
                    alphaButton(button)
                    loadActiveParam()
                }

            }
        }


        // CLICK LISTENERS
        shapeListButton.setOnClickListener {
            if (shapeListAdapter.activatedPos == 0 && Shape.custom.isNotEmpty()) {
                handler.postDelayed({

                    act.showThumbnailDialog()

                    // render custom shape thumbnails
                    fsv.r.renderProfile = RenderProfile.SHAPE_THUMB
                    fsv.r.renderThumbnails = true
                    fsv.requestRender()

                }, BUTTON_CLICK_DELAY_LONG)
            }
            handler.postDelayed({

                act.hideTouchIcon()
                showLayout(shapePreviewListLayout)
                fsv.r.reaction = Reaction.NONE

                shapeSubMenuButtons.hide()
                act.hideCategoryButtons()

                shapeNavButtons.show()
                loadNavButtons(previewListNavButtons)
                shapeListAdapter.apply {
                    (if (selectedPositions.isEmpty()) getFirstPositionOf(f.shape) else activatedPos).let {
                        setActivatedPosition(it)
                        recyclerView?.scrollToPosition(it)
                    }
                }

                act.uiSetOpenTall()

            }, BUTTON_CLICK_DELAY_SHORT)
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


            }, BUTTON_CLICK_DELAY_MED)
        }

        maxIterButton.setOnClickListener(subMenuButtonListener(maxIterLayout, maxIterButton))
        // sensitivityButton.setOnClickListener(subMenuButtonListener(sensitivityLayout, sensitivityButton))

        shapeListViewTypeButton.setOnClickListener {

            sc.shapeListViewType = ListLayoutType.values().run {
                get((sc.shapeListViewType.ordinal + 1) % size)
            }

            shapeListAdapter.updateLayoutType(sc.shapeListViewType)

            when (sc.shapeListViewType) {

                ListLayoutType.LINEAR -> {
                    shapePreviewList.layoutManager = shapePreviewListLinearManager
                }
                ListLayoutType.GRID -> {
                    shapePreviewList.layoutManager = shapePreviewListGridManager
                }

            }

        }
        shapeListDoneButton.setOnClickListener {
            handler.postDelayed({

                if (fsv.r.isRendering) fsv.r.pauseRender = true

                if (!act.uiIsClosed()) act.uiSetOpen() else MainActivity.Category.SHAPE.onMenuClosed(act)


                shapePreviewListLayout.hide()
                //shapePreviewLayout.show()
                shapeSubMenuButtons.show()
                shapeNavButtons.hide()
                act.showCategoryButtons()
                maxIterButton.performClick()

                updateLayout()

            }, BUTTON_CLICK_DELAY_SHORT)
        }

        customShapeNewButton.setOnClickListener {
            handler.postDelayed({

                shapePreviewListLayout.hide()
                customShapeLayout.show()
                loadNavButtons(customShapeNavButtons)
                fsv.r.reaction = Reaction.COLOR
                fsv.r.renderProfile = RenderProfile.DISCRETE

                val postfix = infixToPostfix(parseEquation("z^2 + c")!!)
                customShape = Shape(
                        name = "%s %s %d".format(
                                resources.getString(R.string.header_custom),
                                resources.getString(R.string.shape),
                                Shape.nextCustomShapeNum
                        ),
                        latex = "z^2 + c",
                        loop = "customshape_loop(z1, c)",
                        customLoopSF = "csqr(z) + c",
                        customLoopDF = "cadd(csqr(z), c)",
                        positions = PositionList(Position(zoom = 5e0)),
                        hasDualFloat = true
                )

                customShapeName?.setText(customShape.name)
                shapeMathQuill.setLatex(customShape.latex)

                prevSelectedShapeIndex = Shape.all.indexOf(f.shape)
                f.shape = customShape
                f.texture = Texture.escapeSmooth

                fsv.r.renderShaderChanged = true
                fsv.r.renderToTex = true
                fsv.requestRender()

            }, BUTTON_CLICK_DELAY_SHORT)
        }
        customShapeCancelButton.setOnClickListener {

            if (!customShape.hasCustomId) {
                f.shape = Shape.all[prevSelectedShapeIndex]
                fsv.r.reaction = Reaction.NONE
                fsv.r.renderShaderChanged = true
                fsv.r.renderToTex = true
                fsv.requestRender()
            }

            customShapeLayout.hide()
            shapePreviewListLayout.show()
            loadNavButtons(previewListNavButtons)
            //act.showCategoryButtons()
            
        }
        customShapeDoneButton.setOnClickListener {

            if (!sc.goldEnabled) act.showUpgradeScreen()
            else {

                if (Shape.all.any {
                            if (customShape.name == it.name) {
                                if (customShape.hasCustomId) customShape.id != it.id
                                else true
                            } else false
                        }) {
                    act.showMessage(resources.getString(R.string.msg_custom_name_duplicate).format(
                            resources.getString(R.string.shape)
                    ))
                } else if (eqnErrorIndicator.isVisible()) {
                    act.showMessage(resources.getString(R.string.msg_eqn_error))
                } else {

                    GlobalScope.launch {
                        if (customShape.hasCustomId) {
                            // update existing shape in database
                            act.db.shapeDao().apply {
                                update(
                                        customShape.id,
                                        customShape.name,
                                        customShape.latex,
                                        customShape.customLoopSF,
                                        customShape.customLoopDF
                                )
                            }

                            fsv.r.renderProfile = RenderProfile.SHAPE_THUMB
                            fsv.requestRender()

                            // update list items if applicable (icon, latex)

                        } else {

                            // add new shape to database
                            act.db.shapeDao().apply {
                                customShape.id = insert(customShape.toDatabaseEntity()).toInt()
                                customShape.hasCustomId = true
                                Log.e("SHAPE", "new custom id: ${customShape.id}")
                                customShape.initialize(resources)
                                fsv.r.renderProfile = RenderProfile.SHAPE_THUMB
                                fsv.requestRender()
                            }

                            // add item to list adapter and select
                            shapeListAdapter.apply {
                                val item = ShapeListItem(customShape, ListHeader.custom, sc.shapeListViewType)
                                setActivatedPosition(addItemToCustom(item, 0))
                            }

                            Shape.all.add(0, customShape)
                            Shape.custom.add(0, customShape)
                            Shape.nextCustomShapeNum++

                        }
                    }


                    fsv.r.reaction = Reaction.NONE

                    // update ui
                    handler.postDelayed({

                        shapeMathQuill.getLatex { customShape.latex = it }

                        customShapeLayout.hide()
                        shapePreviewListLayout.show()
                        loadNavButtons(previewListNavButtons)

                        shapePreviewList.adapter?.notifyDataSetChanged()

                    }, BUTTON_CLICK_DELAY_SHORT)
                }

            }

        }





        numpadButton.setOnClickListener {
            operatorSpecialKeys.hide()
            // constantKeys.hide()
            numberKeys.show()
        }
        functionsButton.setOnClickListener {
            operatorSpecialKeys.show()
            // constantKeys.hide()
            numberKeys.hide()
        }

        zKey.setOnClickListener(keyListener(Expr.z))
        cKey.setOnClickListener(keyListener(Expr.c))
        plusKey.setOnClickListener(keyListener(Expr.add))
        minusKey.setOnClickListener(keyListener(Expr.sub))
        timesKey.setOnClickListener(keyListener(Expr.mult))
        divKey.setOnClickListener(keyListener(Expr.div))

        powKey.setOnClickListener(keyListener(Expr.pow))
        sqrKey.setOnClickListener(keyListener(Expr.sqr))
        cubeKey.setOnClickListener(keyListener(Expr.cube))
        quadKey.setOnClickListener(keyListener(Expr.quad))
        inverseKey.setOnClickListener(keyListener(Expr.inv))
        sqrtKey.setOnClickListener(keyListener(Expr.sqrt))
        modulusKey.setOnClickListener(keyListener(Expr.mod))
        conjKey.setOnClickListener(keyListener(Expr.conj))
        argKey.setOnClickListener(keyListener(Expr.arg))
        absKey.setOnClickListener(keyListener(Expr.abs))
        rabsKey.setOnClickListener(keyListener(Expr.rabs))
        iabsKey.setOnClickListener(keyListener(Expr.iabs))
//        signKey.setOnClickListener(keyListener(Expr.sign))

        sinKey.setOnClickListener(keyListener(Expr.sin))
        cosKey.setOnClickListener(keyListener(Expr.cos))
        tanKey.setOnClickListener(keyListener(Expr.tan))
        cscKey.setOnClickListener(keyListener(Expr.csc))
        secKey.setOnClickListener(keyListener(Expr.sec))
        cotKey.setOnClickListener(keyListener(Expr.cot))

        asinKey.setOnClickListener(keyListener(Expr.asin))
        acosKey.setOnClickListener(keyListener(Expr.acos))
        atanKey.setOnClickListener(keyListener(Expr.atan))
        acscKey.setOnClickListener(keyListener(Expr.acsc))
        asecKey.setOnClickListener(keyListener(Expr.asec))
        acotKey.setOnClickListener(keyListener(Expr.acot))

        sinhKey.setOnClickListener(keyListener(Expr.sinh))
        coshKey.setOnClickListener(keyListener(Expr.cosh))
        tanhKey.setOnClickListener(keyListener(Expr.tanh))
        cschKey.setOnClickListener(keyListener(Expr.csch))
        sechKey.setOnClickListener(keyListener(Expr.sech))
        cothKey.setOnClickListener(keyListener(Expr.coth))

        asinhKey.setOnClickListener(keyListener(Expr.asinh))
        acoshKey.setOnClickListener(keyListener(Expr.acosh))
        atanhKey.setOnClickListener(keyListener(Expr.atanh))
        acschKey.setOnClickListener(keyListener(Expr.acsch))
        asechKey.setOnClickListener(keyListener(Expr.asech))
        acothKey.setOnClickListener(keyListener(Expr.acoth))

//        lnKey.setOnClickListener(keyListener(Expr.ln))
//        log2Key.setOnClickListener(keyListener(Expr.log2))
//        log10Key.setOnClickListener(keyListener(Expr.log10))

//        expKey.setOnClickListener(keyListener(Expr.exp))
//        exp2Key.setOnClickListener(keyListener(Expr.exp2))
//        exp10Key.setOnClickListener(keyListener(Expr.exp10))

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
            showLayout(complexShapeParam)
            alphaButton(juliaParamButton)
            // shapeResetButton.show()
            f.shape.params.active = f.shape.params.julia
            fsv.r.reaction = Reaction.SHAPE
            loadActiveParam()
        }
        seedParamButton.setOnClickListener {
            act.showTouchIcon()
            showLayout(complexShapeParam)
            alphaButton(seedParamButton)
            // shapeResetButton.show()
            f.shape.params.active = f.shape.params.seed
            fsv.r.reaction = Reaction.SHAPE
            loadActiveParam()
        }
        shapeParamButtonList.forEachIndexed { index, button ->
            button.setOnClickListener(shapeParamButtonListener(button, index))
        }


        shapePreviewLayout.hide()
        maxIterLayout.hide()
        realShapeParam.hide()
        complexShapeParam.hide()
        sensitivityLayout.hide()
        // shapeResetButton.hide()
        // sensitivityButton.hide()
        operatorSpecialKeys.hide()

        if (!BuildConfig.DEV_VERSION) {
            listOf(
                    asinKey, acosKey, atanKey, acscKey, asecKey, acotKey,
                    asinhKey, acoshKey, atanhKey, acschKey, asechKey, acothKey
            ).forEach { it.hide() }
        }



        shapePreviewListLayout.hide()
        customShapeLayout.hide()
        shapeNavButtons.hide()

        p1Key.hide()
        p2Key.hide()
        constantsButton.hide()



        currentLayout = maxIterLayout
        currentButton = maxIterButton
        showLayout(maxIterLayout)
        alphaButton(maxIterButton)

        // if (sc.goldEnabled) onGoldEnabled()

    }


    override fun updateLayout() {

        val shapeParamButtonList = listOf(
                shapeParamButton1,
                shapeParamButton2,
                shapeParamButton3,
                shapeParamButton4
        )

        // update parameter display
        shapeParamButtonList.forEach { it.hide() }
        f.shape.params.list.forEachIndexed { index, param ->
            shapeParamButtonList[index].apply {
                text = if (param.name == "") "Param ${index + 1}" else param.name
                if (!param.devFeature || BuildConfig.DEV_VERSION) show()
                showGradient = param.goldFeature && !sc.goldEnabled
            }
        }


        // update juliaModeButton

        if (f.shape.juliaModeInit) {
            juliaModeButton.hide()
            juliaDivider.hide()
        } else {
            juliaModeButton.show()
            juliaDivider.show()
        }

        if (f.shape != Shape.mandelbrot && !sc.goldEnabled) {
            juliaModeButton.showGradient = true
            juliaModeButton.alpha = 1f
        } else {
            juliaModeButton.showGradient = false
            juliaModeButton.alpha = 0.5f
        }

        juliaModeButton.apply {
            isChecked = f.shape.juliaMode
            if (isChecked) juliaParamButton.show() else juliaParamButton.hide()
        }
        if (f.shape.juliaMode || f.shape.juliaSeed) seedParamButton.hide() else seedParamButton.show()

        maxIterEdit.setText("%d".format(f.shape.maxIter))
        maxIterBar.progress = ((log(f.shape.maxIter.toDouble(), 2.0) - ITER_MIN_POW)/(ITER_MAX_POW - ITER_MIN_POW)*maxIterBar.max).toInt()

    }

    fun onGoldEnabled() {
        Log.d("SHAPE", "onGoldEnabled")
        shapeListAdapter.notifyDataSetChanged()
        customShapeDoneButton.showGradient = false
        juliaModeButton.showGradient = false
        listOf(
                shapeParamButton1,
                shapeParamButton2,
                shapeParamButton3,
                shapeParamButton4
        ).forEach { it.showGradient = false }
    }

    fun loadActiveParam() {
        val param = f.shape.params.active
        if (param is ComplexParam) {
            complexShapeParam.apply {
                uEdit.setText("%.8f".format(param.u))
                uLock.isChecked = param.uLocked
                vEdit.setText("%.8f".format(param.v))
                vLock.isChecked = param.vLocked
                linkParamButton.isChecked = param.linked
                linkParamButton.foregroundTintList = ColorStateList.valueOf(resources.getColor(
                        if (linkParamButton.isChecked) R.color.white else R.color.colorDarkSelected, null
                ))
            }
        }
        else {
            realShapeParam.apply {
                uEdit2.setText("%.3f".format(param.u))
                realParamSeekBar.setOnSeekBarChangeListener(null)
                realParamSeekBar.max = if (param.discrete) (param.uRange.upper - param.uRange.lower).toInt() else 2000
                realParamSeekBar.progress = (realParamSeekBar.max * (param.u - param.uRange.lower) / (param.uRange.upper - param.uRange.lower)).toInt()
                realParamSeekBar.setOnSeekBarChangeListener(realParamSeekBarListener)
            }
        }
    }

    private fun setCustomLoop(latex: String, shape: Shape) {
        val parsed = parseEquation(latex)
        if (parsed == null) eqnErrorIndicator.show()
        else {
            val postfixSingle = infixToPostfix(parsed)
            val postfixDual = ArrayList(postfixSingle.map {
                if (it is Operator || it.isConstant) it
                else Expr(it).apply { precision = Precision.DUAL }
            })
            Log.e("SHAPE", "postfixSingle: ${postfixSingle.joinToString(" ", transform = { expr -> "${expr.str}(${expr.precision.name[0]})" })}")
            Log.e("SHAPE", "postfixDual: ${postfixDual.joinToString(" ", transform = { expr -> "${expr.str}(${expr.precision.name[0]})" })}")
            val sf = postfixToGlsl(postfixSingle)
            Log.e("SHAPE", "customLoopSF: $sf")
            val df = postfixToGlsl(postfixDual)
            Log.e("SHAPE", "customLoopDF: $df")
            if (sf == null || df == null) {
                eqnErrorIndicator.show()
            } else {
                eqnErrorIndicator.invisible()
                shape.latex = latex
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


        str = str.replace("\\\\", "")       // remove all \\
                .replace("left|", "modulus(")
                .replace("right|", ")")
                .replace("left", "")      // remove all left
                .replace("right", "")     // remove all right
                .replace(Regex("operatorname\\{[A-Za-z]+\\}")) { result -> result.value.substring(13 until result.value.length - 1) }

        Log.e("SHAPE", "str: $str")

        // replace all \\frac{}{} with ({}/{})
        do {
            val start = str.indexOf("frac")
            Log.e("SHAPE", "str: $str")
            Log.e("SHAPE", "start: $start")
            if (start != -1) {

                var left = 0
                var right = 0
                var middle = 0
                var end = 0
                var middleFound = false
                var endFound = false

                var i = start + 5
                while (i < str.length) {
                    when (str[i]) {
                        '}' -> {
                            if (left == right) {
                                if (!middleFound) {
                                    middle = i
                                    middleFound = true
                                    i++
                                }
                                else {
                                    end = i
                                    endFound = true
                                }
                            }
                            else right += 1
                        }
                        '{' -> left += 1
                    }
                    if (endFound) {
                        str = str
                                .replaceRange( end,    end    + 1, "})"  )
                                .replaceRange( middle, middle + 2, "}/{" )
                                .replaceRange( start,  start  + 4, "("   )
                        break
                    }
                    i++
                }
            }
            else break
        } while (str.contains("frac"))

        // replace all \\cdot with *
        while (true) {
            val start = str.indexOf("cdot")
            if (start != -1) {
                str = str.replaceRange(start, start + 4, "*")
            } else break
        }

        str
                .replace(" ", "")
                .replace("^", " ^ ")
                .replace("*", " * ")
                .replace("/", " / ")
                .replace("+", " + ")
                .replace("-", " - ")
                .replace("(", " ( ")
                .replace(")", " ) ")
                .replace("{", " ( ")
                .replace("}", " ) ")
                .replace("z", " z ")
                .replace(Regex("(?<!arc)sin(?!h)"), " sin ")
                .replace(Regex("(?<!arc)cos(?!h)"), " cos ")
                .replace(Regex("(?<!arc)tan(?!h)"), " tan ")
                .replace(Regex("(?<!arc)csc(?!h)"), " csc ")
                .replace(Regex("(?<!arc)sec(?!h)"), " sec ")
                .replace(Regex("(?<!arc)cot(?!h)"), " cot ")
                .replace(Regex("arcsin(?!h)"), " arcsin ")
                .replace(Regex("arccos(?!h)"), " arccos ")
                .replace(Regex("arctan(?!h)"), " arctan ")
                .replace(Regex("arccsc(?!h)"), " arccsc ")
                .replace(Regex("arcsec(?!h)"), " arcsec ")
                .replace(Regex("arccot(?!h)"), " arccot ")
                .replace(Regex("(?<!arc)sinh"), " sinh ")
                .replace(Regex("(?<!arc)cosh"), " cosh ")
                .replace(Regex("(?<!arc)tanh"), " tanh ")
                .replace(Regex("(?<!arc)csch"), " csch ")
                .replace(Regex("(?<!arc)sech"), " sech ")
                .replace(Regex("(?<!arc)coth"), " coth ")
                .replace(Regex("arcsinh"), " arcsinh ")
                .replace(Regex("arccosh"), " arccosh ")
                .replace(Regex("arctanh"), " arctanh ")
                .replace(Regex("arccsch"), " arccsch ")
                .replace(Regex("arcsech"), " arcsech ")
                .replace(Regex("arccoth"), " arccoth ")
                // .replace("i", " i ")
                // .replace(Regex("c(?![os])"), " c ")
                .replace(Regex("(\\d*\\.)?\\d+"))  { result -> " ${result.value} "}
                .replace("  ", " ")
                .trim()
                .split(" ")
                .forEach {
                    val expr = Expr.valueOf(it)
                    if (expr != null) out.add(expr)
                    else return null
                }

        // implicit negative
        out.forEachIndexed { i, expr ->
            if (expr == Expr.sub && (i == 0 || out[i - 1] in listOf(Expr.add, Expr.sub, Expr.mult, Expr.div, Expr.leftParen))) {
                out[i] = Expr.neg
            }
        }

        // implicit multiply
        var i = 1
        var outSize = out.size
        while (i < outSize) {
            if (out[i - 1] !is Operator && (out[i].str == "I" || !out[i].isConstant)) {
                if (out[i] !is Operator || out[i].run { this is Operator && numArgs == 1 }) {
                    if (!(out[i - 1] == Expr.leftParen || out[i] == Expr.rightParen)) {
                        Log.e("SHAPE", "implicit multiply: ${out[i - 1]} -> ${out[i]}")
                        out.add(i, Expr.mult)
                        outSize++
                    }
                }
            }
            i++
        }

        Log.e("SHAPE", "parsed equation: ${out.joinToString(" ")}")
        // Log.e("SHAPE", "out size: ${out.size}")
        return out

    }

    private fun infixToPrefix(exprs: ArrayList<Expr>) : ArrayList<Expr> {

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
        out.reverse()
        return out

    }

    private fun infixToPostfix(exprs: ArrayList<Expr>) : ArrayList<Expr> {

        val stack = Stack<Expr>()
        val out = arrayListOf<Expr>()

        for (expr in exprs) {
            when (expr) {
                is Operator -> {
                    while (stack.isNotEmpty()) {
                        val top = stack.peek()
                        if (top == Expr.leftParen || (top is Operator && expr.order < top.order)) break
                        out.add(stack.pop())
                    }
                    stack.push(expr)
                }
                Expr.leftParen -> stack.push(expr)
                Expr.rightParen -> {
                    while (stack.isNotEmpty()) {
                        if (stack.peek() == Expr.leftParen) {
                            stack.pop()
                            break
                        }
                        out.add(stack.pop())
                    }
                }
                else -> out.add(expr)
            }
        }

        while (stack.isNotEmpty()) {
            // if (stack.peek() == Expr.leftParen) return null
            out.add(stack.pop())
        }

        return out

    }

    private fun postfixToGlsl(input: ArrayList<Expr>) : String? {

        val exprs = ArrayList(input)
        val stack = Stack<Expr>()

        while (exprs.isNotEmpty()) {

            Log.e("SHAPE", "stack: ${stack.joinToString("  ", transform = { expr -> "$expr(${expr.precision.name[0]})" })}")

            when (val expr = exprs.removeAt(0)) {
                is Operator -> {
                    val args = arrayListOf<Expr>()
                    for (i in 0 until expr.numArgs) {
                        if (stack.isEmpty()) return null
                        args.add(0, stack.pop())
                    }
                    stack.push(expr.of(args))
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