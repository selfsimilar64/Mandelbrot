package com.selfsimilartech.fractaleye

import android.animation.LayoutTransition
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import kotlinx.android.synthetic.main.shape_fragment.*
import kotlinx.android.synthetic.main.complex_param.view.*
import kotlinx.android.synthetic.main.continuous_sensitivity_layout.view.*
import kotlinx.android.synthetic.main.real_param.view.*
import kotlinx.android.synthetic.main.list_layout.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.math.log
import kotlin.math.pow



class ShapeFragment : MenuFragment() {



    private var realParamSeekBarListener : SeekBar.OnSeekBarChangeListener? = null
    private lateinit var shapeListAdapter : ListAdapter<Shape>
    var onTutorialReqMet = {}


    private fun loadNavButtons(views: List<View>) {

        for (i in 0 until shapeNavButtons.childCount) shapeNavButtons.getChildAt(i).hide()
        views.forEach { it.show() }

    }


    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.shape_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        super.onViewCreated(v, savedInstanceState)



        var customShape = Shape(name = "q", latex = "$$")
        var savedCustomName = ""
        var savedCustomLatex = ""
        var savedCustomLoopSingle = ""
        var savedCustomLoopDual = ""
        var prevSelectedShapeIndex = 0

        shapeLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        val previewListNavButtons = listOf(
                // shapeListViewTypeButton,
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

                fsv.r.checkThresholdCross { f.shape.juliaMode = juliaModeButton.isChecked }

                if (f.shape.juliaMode) {

                    seedParamButton.hide()
                    juliaParamButton.show()
                    juliaParamButton.performClick()

                    if (f.shape.numParamsInUse == 1) {
                        fsv.r.reaction = Reaction.SHAPE
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
                act.updatePositionLayout()

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
        shapeMathQuill.setBackgroundColor(Color.TRANSPARENT)
        // shapeMathQuill.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)


        realShapeParam.apply {

            realParamResetButton.setOnClickListener(resetListener)
            uValue2.setOnEditorActionListener(editListener(null) { w: TextView ->
                val result = "${w.text}".formatToDouble()
                val param = f.shape.params.active
                if (result != null) {
                    param.u = if (sc.restrictParams) param.clamp(result) else result
                    fsv.r.renderToTex = true
                }
                w.text = param.u.format(REAL_PARAM_DIGITS)
            })
            realParamSensitivity.sensitivityValue.setOnEditorActionListener(editListener(null) { w: TextView ->

                val param = f.shape.params.active
                val result = w.text.toString().formatToDouble()
                if (result != null) param.sensitivity = result
                w.text = "%d".format(param.sensitivity.toInt())

            })
            paramAdjustLeftButton.setOnClickListener(ParamChangeOnClickListener(fsv,
                transformFractal = { f.shape.params.active.apply { u -= sensitivityFactor/RealParam.ADJUST_DISCRETE } },
                updateLayout = { updateParamText() }
            ))
            paramAdjustLeftButton.setOnLongClickListener(ParamChangeOnLongClickListener(fsv,
                transformFractal = { f.shape.params.active.apply { u -= sensitivityFactor/RealParam.ADJUST_CONTINUOUS } },
                updateLayout = { updateParamText() }
            ))
            paramAdjustRightButton.setOnClickListener(ParamChangeOnClickListener(fsv,
                transformFractal = { f.shape.params.active.apply { u += sensitivityFactor/RealParam.ADJUST_DISCRETE } },
                updateLayout = { updateParamText() }
            ))
            paramAdjustRightButton.setOnLongClickListener(ParamChangeOnLongClickListener(fsv,
                transformFractal = { f.shape.params.active.apply { u += sensitivityFactor/RealParam.ADJUST_CONTINUOUS } },
                updateLayout = { updateParamText() }
            ))

        }
        complexShapeParam.apply {

            uValue.setOnEditorActionListener(editListener(vValue) { w: TextView ->
                val result = "${w.text}".formatToDouble()
                val param = f.shape.params.active
                if (result != null) {
                    param.u = result
                    fsv.r.renderToTex = true
                }
                w.text = param.u.format(COMPLEX_PARAM_DIGITS)
            })
            vValue.setOnEditorActionListener(editListener(null) { w: TextView ->
                val result = "${w.text}".formatToDouble()
                val param = f.shape.params.active
                if (param is ComplexParam) {
                    if (result != null) {
                        param.v = result
                        fsv.r.renderToTex = true
                    }
                    w.text = param.v.format(COMPLEX_PARAM_DIGITS)
                }
            })
            uLock.setOnClickListener(lockListener(0))
            vLock.setOnClickListener(lockListener(1))
            complexParamResetButton.setOnClickListener(resetListener)
            complexParamSensitivity.sensitivityValue.setOnEditorActionListener(editListener(null) { w: TextView ->

                val param = f.shape.params.active
                val result = w.text.toString().formatToDouble()
                if (result != null) param.sensitivity = result
                w.text = "%d".format(param.sensitivity.toInt())

            })

        }


        maxIterBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

                val p = (seekBar.progress.toDouble() / maxIterBar.max).pow(0.75)
                val iter = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW).toInt()
                // if (iter > 5000) iterWarningIcon.show() else iterWarningIcon.invisible()
                f.shape.maxIter = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW).toInt() - 1
                maxIterValue.setText("%d".format(iter))
                if (fsv.r.renderProfile == RenderProfile.CONTINUOUS) {
                    fsv.r.renderToTex = true
                    fsv.requestRender()
                }
                if (fsv.doingTutorial && f.shape.maxIter > 499) onTutorialReqMet()

            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {

                if (fsv.r.isRendering) fsv.r.interruptRender = true

                fsv.r.renderProfile = RenderProfile.CONTINUOUS

                val p = (seekBar.progress.toDouble() / maxIterBar.max).pow(0.75)
                f.shape.maxIter = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW).toInt() - 1
                maxIterValue.setText("%d".format(f.shape.maxIter))

            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {

                fsv.r.renderProfile = RenderProfile.DISCRETE
                fsv.r.renderToTex = true
                fsv.requestRender()

                val p = (seekBar.progress.toDouble() / maxIterBar.max).pow(0.75)
                val newIter = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW).toInt() - 1

                // save state on iteration increase
                if (newIter > f.shape.maxIter) act.bookmarkAsPreviousFractal()

                f.shape.maxIter = newIter
                maxIterValue.setText("%d".format(f.shape.maxIter))

                // Log.d("FRACTAL EDIT FRAGMENT", "maxIter: ${f.shape.maxIter}")

            }

        })
        maxIterBar.progress = (((log(f.shape.maxIter.toDouble(), 2.0) - ITER_MIN_POW)/(ITER_MAX_POW - ITER_MIN_POW)).pow(4.0/3.0)*maxIterBar.max).toInt()
        maxIterValue.setText("%d".format(f.shape.maxIter))
        maxIterValue.setOnEditorActionListener(editListener(null) {
            val result = "${it.text}".formatToDouble()?.toInt()
            if (result != null) {
                f.shape.maxIter = result
                fsv.r.renderToTex = true
            }
            maxIterValue.setText("%d".format(f.shape.maxIter))
            maxIterBar.progress = ((log(f.shape.maxIter.toDouble(), 2.0) - ITER_MIN_POW)/(ITER_MAX_POW - ITER_MIN_POW)*maxIterBar.max).toInt()
        })
        maxIterBar.showWarning = true
        // iterWarningIcon.invisible()

        customShapeName.setOnEditorActionListener(editListener(null) {
            customShape.name = customShapeName.text.toString()
        })



        shapePreviewImage.setImageResource(f.shape.thumbnailId)
        shapePreviewText.text = f.shape.name



        if (f.shape.juliaMode) juliaModeButton.isChecked = true
        juliaModeButton.setOnClickListener(juliaListener)
        juliaModeButton.setOnLongClickListener {

            if (f.shape.juliaMode) {

                f.shape.positions.default.x = f.shape.params.julia.u
                f.shape.positions.default.y = f.shape.params.julia.v

            } else {

                f.shape.params.julia.setFrom(ComplexParam(f.shape.position.x, f.shape.position.y))

            }
            juliaModeButton.performClick()

        }




        val shapeListLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val onEditConfirm = { adapter: ListAdapter<Shape>, item: ListItem<Shape> ->

            shapeListLayout.hide()
            customShapeLayout.show()
            eqnErrorIndicator.invisible()
            loadNavButtons(customShapeNavButtons)
            act.uiSetHeight(UiLayoutHeight.TALLER)
            fsv.r.renderProfile = RenderProfile.DISCRETE
            fsv.r.reaction = Reaction.COLOR

            // save values in case of cancel
            item.t.apply {
                savedCustomName = name
                savedCustomLatex = latex
                savedCustomLoopSingle = customLoopSingle
                savedCustomLoopDual = customLoopDual
            }

            customShape = item.t
            fsv.r.checkThresholdCross {
                f.shape = customShape
                f.texture = if (f.shape.isConvergent) Texture.converge else Texture.escapeSmooth
                f.shape.reset()
            }

            customShapeName.setText(customShape.name)
            shapeMathQuill.setLatex(customShape.latex)
            setCustomLoop(customShape.latex, customShape)

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

                        item.t.release()
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
                            f.shape = getItem(activatedPos)!!.t
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
        val onDuplicateShape = { adapter: ListAdapter<Shape>, item: ListItem<Shape> ->

            if (!sc.goldEnabled) act.showUpgradeScreen()
            else {

                act.uiSetHeight(UiLayoutHeight.TALLER)
                shapeListLayout.hide()
                customShapeLayout.show()
                loadNavButtons(customShapeNavButtons)
                fsv.r.reaction = Reaction.COLOR
                fsv.r.renderProfile = RenderProfile.DISCRETE

                customShape = item.t.clone(resources)

                customShapeName?.setText(customShape.name)
                shapeMathQuill.setLatex(customShape.latex)
                setCustomLoop(customShape.latex, customShape)

                prevSelectedShapeIndex = Shape.all.indexOf(f.shape)
                f.shape = customShape
                f.texture = Texture.escapeSmooth

                fsv.r.renderShaderChanged = true
                fsv.r.renderToTex = true
                fsv.requestRender()

            }

        }


        val emptyFavorite = ListItem(Shape.emptyFavorite, ListHeader.FAVORITE, R.layout.list_item_linear_empty_favorite)
        val emptyCustom = ListItem(Shape.emptyCustom, ListHeader.CUSTOM, R.layout.list_item_linear_empty_custom)
        val listItems = arrayListOf<ListItem<Shape>>()

        
        
        Shape.all.forEach { listItems.add(

                ListItem(
                        it,
                        if (it.hasCustomId || it == Shape.emptyCustom) ListHeader.CUSTOM else ListHeader.DEFAULT,
                        R.layout.other_list_item

                ).apply {

                    if (it.isFavorite) {
                        val favorite = ListItem(
                                it,
                                ListHeader.FAVORITE,
                                R.layout.other_list_item,
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
                onDuplicateShape,
                emptyFavorite,
                emptyCustom
        )
        shapeListLayout.list.apply {
            adapter = shapeListAdapter
            setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                val firstVisiblePos = shapeListLayoutManager.findFirstCompletelyVisibleItemPosition()
                highlightListHeader(shapeListAdapter, when {
                    firstVisiblePos < shapeListAdapter.getGlobalPositionOf(shapeListAdapter.headerItems[1]) -> 0
                    firstVisiblePos < shapeListAdapter.getGlobalPositionOf(shapeListAdapter.headerItems[2]) -> 1
                    else -> 2
                })
            }
            layoutManager = shapeListLayoutManager
        }
        shapeListAdapter.apply {
            //isLongPressDragEnabled = true

            mode = SelectableAdapter.Mode.SINGLE
            showAllHeaders()
            //setAnimationOnForwardScrolling(true)
            //setAnimationOnReverseScrolling(true)
        }
        shapeListAdapter.mItemClickListener = FlexibleAdapter.OnItemClickListener { view, position ->

            if (shapeListAdapter.getItemViewType(position) !in nonClickableViewTypes) {

                val firstVisiblePos = shapeListLayoutManager.findFirstCompletelyVisibleItemPosition()
                val lastVisiblePos = shapeListLayoutManager.findLastCompletelyVisibleItemPosition()
                if (position + 1 > lastVisiblePos) shapeListLayout.list.smoothSnapToPosition(position + 1, LinearSmoothScroller.SNAP_TO_END)
                else if (position - 1 < firstVisiblePos) shapeListLayout.list.smoothSnapToPosition(position - 1)

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

                        fsv.r.checkThresholdCross { f.shape = newShape }

                        fsv.r.renderShaderChanged = true
                        fsv.r.renderToTex = true
                        fsv.requestRender()

                        act.updateTextureEditTexts()
                        act.updatePositionLayout()

                    }

                }
                true //Important!

            }
            else false

        }



        val shapeKeyListLayoutManager = GridLayoutManager(context, 2, GridLayoutManager.HORIZONTAL, false)
        val shapeKeyListItems = arrayListOf<ShapeKeyListItem>()

        Expr.numbers.forEach    { expr -> shapeKeyListItems.add(ShapeKeyListItem(expr, ShapeKeyListHeader.numbers))      }
        Expr.basic.forEach      { expr -> shapeKeyListItems.add(ShapeKeyListItem(expr, ShapeKeyListHeader.basic))        }
        Expr.trig.forEach       { expr -> shapeKeyListItems.add(ShapeKeyListItem(expr, ShapeKeyListHeader.trigonometry)) }

        val shapeKeyListAdapter = FlexibleAdapter(shapeKeyListItems)
        shapeKeyList.apply {
            adapter = shapeKeyListAdapter
            setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                val firstVisiblePos = shapeKeyListLayoutManager.findFirstCompletelyVisibleItemPosition()
                shapeKeyListAdapter.let { adapter ->
                    highlightKeyListHeader(adapter, adapter.headerItems.indexOf(adapter.getSectionHeader(firstVisiblePos)) ?: 0)
                }
            }
            layoutManager = shapeKeyListLayoutManager
        }
        shapeKeyListAdapter.mItemClickListener = FlexibleAdapter.OnItemClickListener { _, position ->

            val expr = shapeKeyListAdapter.getItem(position)?.expr ?: Expr.z
            shapeMathQuill.enterExpr(expr)
            shapeMathQuill.getLatex { setCustomLoop(it, customShape) }
            true

        }
        shapeKeyListLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (shapeKeyListAdapter.getItemViewType(position) in nonClickableViewTypes) 2 else 1
            }
        }
        shapeKeyListAdapter.showAllHeaders()

        listOf(
                // keyboardVariablesButton,
                keyboardNumbersButton,
                keyboardBasicButton,
                keyboardTrigButton
        ).forEachIndexed { i, b ->
            b.setOnClickListener { shapeKeyList.smoothSnapToPosition(shapeKeyListAdapter.getGlobalPositionOf(shapeKeyListAdapter.headerItems[i])) }
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



        val shapeParamButtonListener = { button: GradientButton, paramIndex: Int ->
            View.OnClickListener {

                if (button.showGradient && !sc.goldEnabled) act.showUpgradeScreen()
                else {
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

        shapeListLayout.apply {
            listFavoritesButton.setOnClickListener {
                list.smoothSnapToPosition(shapeListAdapter.getGlobalPositionOf(shapeListAdapter.headerItems[0]))
            }
            listCustomButton.setOnClickListener {
                list.smoothSnapToPosition(shapeListAdapter.getGlobalPositionOf(shapeListAdapter.headerItems[1]))
            }
            listDefaultButton.setOnClickListener {
                list.smoothSnapToPosition(shapeListAdapter.getGlobalPositionOf(shapeListAdapter.headerItems[2]))
            }
        }
        
        shapeListButton.setOnClickListener {

            if (shapeListAdapter.activatedPos == 0 && Shape.custom.isNotEmpty()) {
                handler.postDelayed({

                    act.showThumbnailRenderDialog()

                    // render custom shape thumbnails
                    fsv.r.renderProfile = RenderProfile.SHAPE_THUMB
                    fsv.r.renderThumbnails = true
                    fsv.requestRender()

                }, BUTTON_CLICK_DELAY_LONG)
            }

            handler.postDelayed({

                showLayout(shapeListLayout)
                fsv.r.reaction = Reaction.NONE

                shapeSubMenuButtons.hide()
                act.hideMenuToggleButton()
                act.hideCategoryButtons()

                shapeNavButtons.show()
                loadNavButtons(previewListNavButtons)
                shapeListAdapter.apply {
                    (if (selectedPositions.isEmpty()) getFirstPositionOf(f.shape) else activatedPos).let {
                        setActivatedPosition(it)
                        recyclerView?.scrollToPosition(it)
                    }
                }

                act.uiSetHeight(UiLayoutHeight.TALL)

            }, BUTTON_CLICK_DELAY_SHORT)

        }
        shapePreviewLayout.setOnClickListener {
            handler.postDelayed({

                shapeSubMenuButtons.hide()
                shapePreviewLayout.hide()
                act.hideCategoryButtons()

                shapeNavButtons.show()
                shapeListLayout.show()

                loadNavButtons(previewListNavButtons)

                act.uiSetHeight(UiLayoutHeight.TALL)


            }, BUTTON_CLICK_DELAY_MED)
        }

        maxIterButton.setOnClickListener {
            subMenuButtonListener(maxIterLayout, maxIterButton).onClick(it)
            fsv.r.reaction = Reaction.NONE
        }
        // maxIterButton.setOnClickListener(subMenuButtonListener(maxIterLayout, maxIterButton))
        // sensitivityButton.setOnClickListener(subMenuButtonListener(sensitivityLayout, sensitivityButton))

        shapeListDoneButton.setOnClickListener {
            handler.postDelayed({

                if (fsv.r.isRendering) fsv.r.pauseRender = true

                if (!act.uiIsClosed()) act.uiSetHeight(UiLayoutHeight.SHORT) else MainActivity.EditMode.SHAPE.onMenuClosed(act)

                shapeListButton.setImageBitmap(f.shape.thumbnail)

                shapeListLayout.hide()
                shapeSubMenuButtons.show()
                shapeNavButtons.hide()
                act.showCategoryButtons()
                act.showMenuToggleButton()
                maxIterButton.performClick()

                updateLayout()

            }, BUTTON_CLICK_DELAY_SHORT)
        }

        customShapeNewButton.setOnClickListener {
            handler.postDelayed({

                act.uiSetHeight(UiLayoutHeight.TALLER)
                shapeListLayout.hide()
                customShapeLayout.show()
                loadNavButtons(customShapeNavButtons)
                fsv.r.reaction = Reaction.COLOR
                fsv.r.renderProfile = RenderProfile.DISCRETE

                customShape = Shape.createNewCustom(resources)

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

            if (customShape.hasCustomId) {
                // revert changes
                customShape.apply {
                    name = savedCustomName
                    latex = savedCustomLatex
                    customLoopSingle = savedCustomLoopSingle
                    customLoopDual = savedCustomLoopDual
                }
            } else {
                // select previous shape
                customShape.release()
                f.shape = Shape.all[prevSelectedShapeIndex]
            }
            fsv.r.reaction = Reaction.NONE
            fsv.r.renderShaderChanged = true
            fsv.r.renderToTex = true
            fsv.requestRender()

            customShapeLayout.hide()
            shapeListLayout.show()
            loadNavButtons(previewListNavButtons)
            act.uiSetHeight(UiLayoutHeight.TALL)
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
                                        customShape.customLoopSingle,
                                        customShape.customLoopDual
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
                                val item = ListItem(customShape, ListHeader.CUSTOM, R.layout.other_list_item)
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
                        shapeListLayout.show()
                        loadNavButtons(previewListNavButtons)
                        act.uiSetHeight(UiLayoutHeight.TALL)

                        shapeListLayout.list.adapter?.notifyDataSetChanged()

                    }, BUTTON_CLICK_DELAY_SHORT)
                }

            }

        }


        zKey.setOnClickListener(keyListener(Expr.z))
        cKey.setOnClickListener(keyListener(Expr.c))
        prevKey.setOnClickListener { shapeMathQuill.enterKeystroke("Left") }
        nextKey.setOnClickListener { shapeMathQuill.enterKeystroke("Right") }
        deleteKey.setOnClickListener {
            shapeMathQuill.enterKeystroke("Backspace")
            shapeMathQuill.getLatex { setCustomLoop(it, customShape) }
        }
        leftParenKey.setOnClickListener(keyListener(Expr.leftParen))
        rightParenKey.setOnClickListener(keyListener(Expr.rightParen))
//        parensKey.setOnClickListener(keyListener(Expr.parens))

        juliaParamButton.setOnClickListener {
            showLayout(complexShapeParam)
            alphaButton(juliaParamButton)
            // shapeResetButton.show()
            f.shape.params.active = f.shape.params.julia
            fsv.r.reaction = Reaction.SHAPE
            loadActiveParam()
            val r = Rect()
            juliaParamButton.getGlobalVisibleRect(r)
            Log.e("SHAPE", "r: $r")
        }
        seedParamButton.setOnClickListener {
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


        shapeListLayout.hide()
        customShapeLayout.hide()
        shapeNavButtons.hide()


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

        if (f.shape.hasCustomId) shapeListButton.setImageBitmap(f.shape.thumbnail)
        else                     shapeListButton.setImageResource(f.shape.thumbnailId)

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
            // juliaDivider.hide()
        } else {
            juliaModeButton.show()
            // juliaDivider.show()
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

        maxIterValue.setText("%d".format(f.shape.maxIter))
        maxIterBar.progress = (((log(f.shape.maxIter.toDouble(), 2.0) - ITER_MIN_POW)/(ITER_MAX_POW - ITER_MIN_POW)).pow(4.0/3.0)*maxIterBar.max).toInt()

        loadActiveParam()

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

    fun updateParamText() {
        val param = f.shape.params.active
        if (param is ComplexParam) {
            complexShapeParam.apply {
                uValue.setText(param.u.format(COMPLEX_PARAM_DIGITS))
                vValue.setText(param.v.format(COMPLEX_PARAM_DIGITS))
            }
        }
        else {
            realShapeParam.apply {
                uValue2.setText(param.u.format(REAL_PARAM_DIGITS))
            }
        }
    }
    fun loadActiveParam() {
        val param = f.shape.params.active
        if (param is ComplexParam) {
            complexShapeParam.apply {
                uValue.setText(param.u.format(COMPLEX_PARAM_DIGITS))
                uLock.isChecked = param.uLocked
                vValue.setText(param.v.format(COMPLEX_PARAM_DIGITS))
                vLock.isChecked = param.vLocked
                complexParamSensitivity.sensitivityValue.setText("%d".format(param.sensitivity.toInt()))
            }
        }
        else {
            realShapeParam.apply {
                uValue2.setText(param.u.format(REAL_PARAM_DIGITS))
                realParamSensitivity.sensitivityValue.setText("%d".format(param.sensitivity.toInt()))
            }
        }
    }

    fun highlightListHeader(adapter: ListAdapter<Shape>, index: Int) {
        adapter.apply {
            listOf(
                    shapeListLayout.listFavoritesButton,
                    shapeListLayout.listCustomButton,
                    shapeListLayout.listDefaultButton
            ).forEachIndexed { i, b ->
                b.setTextColor(resources.getColor(if (index == i) R.color.colorDarkText else R.color.colorDarkTextMuted, null))
            }
        }
    }

    fun highlightKeyListHeader(adapter: FlexibleAdapter<*>, index: Int) {
        adapter.apply {
            listOf(
                    // keyboardVariablesButton,
                    keyboardNumbersButton,
                    keyboardBasicButton,
                    keyboardTrigButton
            ).forEachIndexed { i, b ->
                b.setTextColor(resources.getColor(if (index == i) R.color.highlight else R.color.toggleButtonUnselected, null))
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
                shape.customLoopSingle = sf
                shape.customLoopDual = df
                fsv.r.renderShaderChanged = true
                fsv.r.renderToTex = true
                fsv.requestRender()
            }
        }
    }

    private fun parseEquation(input: String) : ArrayList<Expr>? {

        val out = arrayListOf<Expr>()
        var str = input


        // initial processing
        str = str.replace("\\\\", "")       // remove all \\
                .replace("left|", "modulus(")
                .replace("right|", ")")
                .replace("left", "")      // remove all left
                .replace("right", "")     // remove all right
                .replace(Regex("operatorname\\{[A-Za-z]+\\}")) { result -> result.value.substring(13 until result.value.length - 1) }


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


        str = str
                .replace(" ", "")
                .replace("{", "(")
                .replace("}", ")")


        // parse string into expressions
        while (str.isNotEmpty()) {
            var end = minOf(str.length, Expr.MAX_EXPR_LEN)
            while (end > 0) {
                val expr = Expr.valueOf(str.substring(0 until end))
                if (expr != null) {
                    // Log.e("SHAPE", "expr found: ${str.substring(0 until end)}")
                    out.add(expr)
                    str = str.removeRange(0 until end)
                    break
                }
                else {
                    if (end == 1) {
                        Log.e("SHAPE", "no matching expression: $str")
                        return null
                    } else end--
                }
            }
        }


        // implicit negative
        out.forEachIndexed { i, expr ->
            if (expr == Expr.sub && (i == 0 || out[i - 1] in listOf(Expr.add, Expr.sub, Expr.mult, Expr.div, Expr.leftParen))) {
                // Log.d("SHAPE", "prev expr: ${out[i - 1]}")
                out[i] = Expr.neg
            }
        }

        // implicit multiply
        var i = 1
        var outSize = out.size
        while (i < outSize) {
            if (out[i - 1] !is Operator && (out[i] == Expr.i || !out[i].isConstant)) {
                if (out[i] !is Operator || out[i].run { this is Operator && numArgs == 1 }) {
                    if (!(out[i - 1] == Expr.leftParen || out[i] == Expr.rightParen)) {
                        // Log.d("SHAPE", "implicit multiply: ${out[i - 1]} -> ${out[i]}")
                        out.add(i, Expr.mult)
                        outSize++
                    }
                }
            }
            i++
        }

        // Log.d("SHAPE", "parsed equation: ${out.joinToString(" ")}")
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

            // Log.d("SHAPE", "stack: ${stack.joinToString("  ", transform = { expr -> "$expr(${expr.precision.name[0]})" })}")

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