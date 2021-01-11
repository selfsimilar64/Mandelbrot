package com.selfsimilartech.fractaleye

import android.animation.LayoutTransition
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import com.google.android.material.tabs.TabLayout
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.ToggleButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import kotlinx.android.synthetic.main.complex_param.view.*
import kotlinx.android.synthetic.main.real_param.view.*
import kotlinx.android.synthetic.main.texture_fragment.*
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.math.floor


class TextureFragment : MenuFragment() {

    private lateinit var act : MainActivity
    private lateinit var f : Fractal
    private lateinit var fsv : FractalSurfaceView
    private lateinit var sc : SettingsConfig

    private var realParamSeekBarListener : SeekBar.OnSeekBarChangeListener? = null
    private lateinit var textureListAdapter : ListAdapter<Texture>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.texture_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        Log.e("TEXTURE", "!! onViewCreated start !!")

        act = activity as MainActivity
        f = act.f
        fsv = act.fsv
        sc = act.sc

        textureLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        texturePreviewListLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        val lockListener = { j: Int -> View.OnClickListener {
            val lock = it as android.widget.ToggleButton
            val param = f.texture.activeParam
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
            val param = f.texture.activeParam
            if (param is ComplexParam) param.linked = link.isChecked
            if (link.isChecked) {
                loadActiveParam()
                fsv.r.renderToTex = true
                fsv.requestRender()
            }
        }
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
                    fsv.requestRender()
                }
                else -> {
                    Log.d("EQUATION FRAGMENT", "some other action")
                }
            }

            editText.clearFocus()
            act.updateSystemUI()
            true

        }}
        val rateListener = View.OnClickListener {
            if (f.texture.activeParam.sensitivity != null) {
                if (f.texture.activeParam is ComplexParam) showLayout(realTextureParam)
                f.texture.activeParam = f.texture.activeParam.sensitivity!!
                realTextureParam.realRateButton.apply {
                    setText(android.R.string.ok)
                    setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.done, 0, 0)
                }
            }
            else if (f.texture.activeParam.parent != null) {
                if (f.texture.activeParam.parent is ComplexParam) showLayout(complexTextureParam)
                f.texture.activeParam = f.texture.activeParam.parent!!
                realTextureParam.realRateButton.apply {
                    setText(R.string.sensitivity)
                    setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.sensitivity, 0, 0)
                }
            }
            loadActiveParam()
        }
        val resetListener = View.OnClickListener {
            f.texture.activeParam.reset()
            loadActiveParam()
            fsv.r.renderToTex = true
            fsv.requestRender()
        }

        val handler = Handler()
        val nonClickableViewTypes = listOf(
                R.layout.list_header,
                R.layout.list_item_linear_empty_favorite,
                R.layout.list_item_linear_empty_custom
        )


        val previewListWidth = Resolution.SCREEN.w - 2*resources.getDimension(R.dimen.categoryPagerMarginHorizontal) - resources.getDimension(R.dimen.navButtonSize)
        val previewGridWidth = resources.getDimension(R.dimen.textureShapePreviewSize) + 2*resources.getDimension(R.dimen.previewGridPaddingHorizontal)
        //Log.e("COLOR FRAGMENT", "texturePreviewListWidth: $previewListWidth")
        //Log.e("COLOR FRAGMENT", "texturePreviewGridWidth: $previewGridWidth")
        val spanCount = floor(previewListWidth.toDouble() / previewGridWidth).toInt()
        //Log.e("COLOR FRAGMENT", "spanCount: ${previewListWidth.toDouble() / previewGridWidth}")


        val textureParamButtons = listOf(
                textureParamButton1,
                textureParamButton2,
                textureParamButton3,
                textureParamButton4
        )


        realTextureParam.apply {

            uEdit2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            uEdit2.setOnEditorActionListener(editListener(null) { w: TextView ->

                val param = f.texture.activeParam
                val result = w.text.toString().formatToDouble()
                if (result != null) {
                    param.u = result
                    fsv.r.renderToTex = true
                }
                // param.setProgressFromValue()

                w.text = param.toString()
                realTextureParam.realParamSeekBar.progress = (param.getProgress() * realTextureParam.realParamSeekBar.max.toDouble()).toInt()
                // Log.d("TEXTURE FRAGMENT", "param progress : ${param.progress}")
                // Log.d("TEXTURE FRAGMENT", "qBar max : ${qBar.max}")

            })
            realParamSeekBarListener = object : SeekBar.OnSeekBarChangeListener {

                fun updateEditTextOnly() {

                    // update EditText but not param
                    val param = f.texture.activeParam
                    val progressNormal = realParamSeekBar.progress.toDouble() / realParamSeekBar.max
                    uEdit2.setText(param.toString(param.getValueFromProgress(progressNormal)))

                }

                fun updateEditTextAndParam() {

                    // update EditText and param -- then render
                    val param = f.texture.activeParam
                    param.setValueFromProgress(realParamSeekBar.progress.toDouble() / realParamSeekBar.max)
                    uEdit2.setText(param.toString())

                    if (param.isPrimary) fsv.r.renderToTex = true
                    fsv.requestRender()

                }

                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                    if (sc.continuousParamRender) updateEditTextAndParam()
                    else updateEditTextOnly()

                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    if (sc.continuousParamRender && f.texture.activeParam.isPrimary) {
                        fsv.r.renderProfile = RenderProfile.CONTINUOUS
                        fsv.r.renderToTex = true
                    }
                }
                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                    if (sc.continuousParamRender) fsv.r.renderProfile = RenderProfile.DISCRETE
                    updateEditTextAndParam()

                }

            }
            realParamSeekBar.setOnSeekBarChangeListener(realParamSeekBarListener)
            realRateButton.setOnClickListener(rateListener)
            realResetButton.setOnClickListener(resetListener)
            if (f.texture.params.isNotEmpty()) {
                realParamSeekBar.max = if (f.texture.params[0].discrete)
                    f.texture.params[0].interval.toInt() else 5000
            }

        }
        complexTextureParam.apply {

            val activeParam = f.texture.activeParam

            uEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            vEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)

            uEdit.setText("%.8f".format(activeParam.u))
            if (activeParam is ComplexParam) vEdit.setText("%.8f".format(activeParam.v))

            uEdit.setOnEditorActionListener(editListener(vEdit) { w: TextView ->
                val result = "${w.text}".formatToDouble()
                val param = f.texture.activeParam
                if (result != null) {
                    param.u = result
                    fsv.r.renderToTex = true
                }
                w.text = "%.8f".format((f.texture.activeParam.u))
                if (param is ComplexParam && param.linked) {
                    vEdit.setText("%.8f".format((param.v)))
                }
            })
            vEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
                val result = "${w.text}".formatToDouble()
                val param = f.texture.activeParam
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


        textureButtonsScroll.setOnScrollChangeListener(scrollListener(
                textureButtonsScrollLayout,
                textureButtonsScroll,
                textureScrollArrowLeft,
                textureScrollArrowRight
        ))
        textureScrollArrowLeft.invisible()
        textureScrollArrowRight.invisible()



        val previewListLinearManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val previewListGridManager = GridLayoutManager(context, spanCount, GridLayoutManager.VERTICAL, false)

        val onEditCustomTexture = { adapter: ListAdapter<Texture>, texture: Texture -> }
        val onDeleteCustomTexture = { adapter: ListAdapter<Texture>, texture: Texture -> }

        val emptyFavorite = TextureListItem(Texture.emptyFavorite, ListHeader.favorites, sc.textureListViewType)
        val emptyCustom = TextureListItem(Texture.emptyCustom, ListHeader.custom, sc.textureListViewType)
        val listItems = getTextureListItems()



        textureListAdapter = ListAdapter(
                listItems,
                onEditCustomTexture,
                onDeleteCustomTexture,
                emptyFavorite,
                emptyCustom
        )
        texturePreviewList.adapter = textureListAdapter
        textureListAdapter.apply {
            //isLongPressDragEnabled = true
            mode = SelectableAdapter.Mode.SINGLE
            showAllHeaders()
            //setAnimationOnForwardScrolling(true)
            //setAnimationOnReverseScrolling(true)
        }
        textureListAdapter.mItemClickListener = FlexibleAdapter.OnItemClickListener { view, position ->

            if (textureListAdapter.getItemViewType(position) !in nonClickableViewTypes) {

                val prevActivatedPosition = textureListAdapter.activatedPos
                if (position != textureListAdapter.activatedPos) textureListAdapter.setActivatedPosition(position)
                val newTexture: Texture = try {
                    textureListAdapter.getActivatedItem()?.t ?: f.texture
                } catch (e: IndexOutOfBoundsException) {
                    Log.e("texture", "array index out of bounds -- index: $position")
                    act.showMessage(resources.getString(R.string.msg_error))
                    f.texture
                }

                if (newTexture != f.texture) {

                    if (newTexture.goldFeature && !sc.goldEnabled) {
                        textureListAdapter.setActivatedPosition(prevActivatedPosition)
                        act.showUpgradeScreen()
                    }
                    else {
                        if (fsv.r.isRendering) fsv.r.interruptRender = true

                        f.texture = newTexture

                        fsv.r.calcNewTextureSpan = true

                        fsv.r.renderShaderChanged = true
                        fsv.r.renderToTex = true
                        fsv.requestRender()

                        loadActiveParam()
                    }

                }
                true //Important!

            }
            else false

        }
        previewListGridManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (textureListAdapter.getItemViewType(position) in nonClickableViewTypes) spanCount else 1
            }
        }
        texturePreviewList.layoutManager = when (sc.textureListViewType) {
            ListLayoutType.LINEAR -> previewListLinearManager
            ListLayoutType.GRID -> previewListGridManager
        }





        bailoutSignificandEdit.setOnEditorActionListener(
                editListener(bailoutExponentEdit) { w: TextView ->
                    val result1 = w.text.toString().formatToDouble(false)
                    val result2 = bailoutExponentEdit.text.toString().formatToDouble(false)
                    val result3 = "${w.text}e${bailoutExponentEdit.text}".formatToDouble(false)?.toFloat()
                    if (result1 != null && result2 != null && result3 != null) {
                        if (result3.isInfinite() || result3.isNaN()) {
                            act.showMessage(resources.getString(R.string.msg_num_out_range))
                        }
                        else {
                            f.bailoutRadius = result3
                            fsv.r.renderToTex = true
                        }
                    }
                    else {
                        act.showMessage(resources.getString(R.string.msg_invalid_format))
                    }
                    val bailoutStrings = "%e".format(Locale.US, f.bailoutRadius).split("e")
                    w.text = "%.2f".format(bailoutStrings[0].toFloat())
                    bailoutExponentEdit.setText("%d".format(bailoutStrings[1].toInt()))
                })
        bailoutExponentEdit.setOnEditorActionListener(
                editListener(null) { w: TextView ->
                    val result1 = bailoutSignificandEdit.text.toString().formatToDouble(false)
                    val result2 = w.text.toString().formatToDouble(false)
                    val result3 = "${bailoutSignificandEdit.text}e${w.text}".formatToDouble(false)?.toFloat()
                    if (result1 != null && result2 != null && result3 != null) {
                        if (result3.isInfinite() || result3.isNaN()) {
                            act.showMessage(resources.getString(R.string.msg_num_out_range))
                        }
                        else {
                            f.bailoutRadius = result3
                            fsv.r.renderToTex = true
                        }
                    }
                    else {
                        act.showMessage(resources.getString(R.string.msg_invalid_format))
                    }
                    val bailoutStrings = "%e".format(Locale.US, f.bailoutRadius).split("e")
                    bailoutSignificandEdit.setText("%.2f".format(bailoutStrings[0].toFloat()))
                    w.text = "%d".format(bailoutStrings[1].toInt())
                })


//        val bailoutStrings = "%e".format(Locale.US, f.bailoutRadius).split("e")
//        bailoutSignificandBar.progress = (bailoutSignificandBar.max*(bailoutStrings[0].toDouble() - 1.0)/8.99).toInt()
//        bailoutExponentBar.progress = bailoutStrings[1].toInt()
        loadEscapeRadius()

        bailoutSignificandBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                val sig = progress.toDouble()/bailoutSignificandBar.max*8.99 + 1.0
                val result1 = bailoutExponentEdit.text.toString().formatToDouble(false)
                val result2 = "${sig}e${bailoutExponentEdit.text}".formatToDouble(false)?.toFloat()
                if (result1 != null && result2 != null) {
                    if (result2.isInfinite() || result2.isNaN()) {
                        act.showMessage(resources.getString(R.string.msg_num_out_range))
                    }
                    else {
                        f.bailoutRadius = result2
                        if (fsv.r.renderProfile == RenderProfile.CONTINUOUS) {
                            fsv.r.renderToTex = true
                            fsv.requestRender()
                        }
                    }
                }
                else {
                    act.showMessage(resources.getString(R.string.msg_invalid_format))
                }
                val bailoutStrings = "%e".format(Locale.US, f.bailoutRadius).split("e")
                //bailoutSignificandEdit.setText("%.5f".format(bailoutStrings[0].toFloat()))
                bailoutSignificandEdit.setText("%.2f".format(bailoutStrings[0].toFloat()))

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (sc.continuousParamRender) fsv.r.renderProfile = RenderProfile.CONTINUOUS
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                fsv.r.renderProfile = RenderProfile.DISCRETE
                fsv.r.renderToTex = true
                fsv.requestRender()

            }

        })
        bailoutExponentBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                val result1 = bailoutSignificandEdit.text.toString().formatToDouble(false)
                val result2 = if (f.shape.isConvergent) {
                    "${bailoutSignificandEdit.text}e-$progress".formatToDouble(false)?.toFloat()
                }
                else {
                    "${bailoutSignificandEdit.text}e$progress".formatToDouble(false)?.toFloat()
                }
                if (result1 != null && result2 != null) {
                    if (result2.isInfinite() || result2.isNaN()) {
                        act.showMessage(resources.getString(R.string.msg_num_out_range))
                    }
                    else {
                        f.bailoutRadius = result2
                        if (fsv.r.renderProfile == RenderProfile.CONTINUOUS) {
                            fsv.r.renderToTex = true
                            fsv.requestRender()
                        }
                    }
                }
                else {
                    act.showMessage(resources.getString(R.string.msg_invalid_format))
                }
                val bailoutStrings = "%e".format(Locale.US, f.bailoutRadius).split("e")
                //bailoutSignificandEdit.setText("%.5f".format(bailoutStrings[0].toFloat()))
                bailoutExponentEdit.setText("%d".format(bailoutStrings[1].toInt()))

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (sc.continuousParamRender) fsv.r.renderProfile = RenderProfile.CONTINUOUS
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                fsv.r.renderProfile = RenderProfile.DISCRETE
                fsv.r.renderToTex = true
                fsv.requestRender()

            }

        })


        textureModeTabs.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {}
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabSelected(tab: TabLayout.Tab) {

                f.textureMode = TextureMode.values()[tab.position]
                fsv.requestRender()

            }
        })
        textureModeTabs.getTabAt(f.textureMode.ordinal)?.select()



        textureParamButtons.forEach { it.hide() }
        f.texture.params.forEachIndexed { index, param ->
            textureParamButtons[index].apply {
                show()
                text = if (param.name == "") "Param ${index + 1}" else param.name
            }
        }



        // CLICK LISTENERS
        textureListButton.setOnClickListener {

            with (texturePreviewList.adapter as ListAdapter<Texture>) {
                removeRange(0, itemCount)
                addItems(0, getTextureListItems())
                getDefaultItems().apply {
                    forEach {
                        if ((it as TextureListItem).disabled) {
                            moveItem(getGlobalPositionOf(it), getGlobalPositionOf(last()))
                        }
                    }
                }
                getFavoriteItems().apply {
                    forEach {
                        if ((it as TextureListItem).disabled) {
                            moveItem(getGlobalPositionOf(it), getGlobalPositionOf(last()))
                        }
                    }
                }
            }

            // ui changes
            handler.postDelayed({

                textureSubMenuButtons.hide()
                act.hideCategoryButtons()

                showLayout(texturePreviewListLayout)
                textureNavBar.show()

                act.uiSetOpenTall()

            }, BUTTON_CLICK_DELAY_SHORT)

            // texture thumbnail render
            handler.postDelayed({

                if (sc.textureListViewType == ListLayoutType.GRID && !fsv.r.textureThumbsRendered) {

                    act.showThumbnailDialog()

                    fsv.r.renderProfile = RenderProfile.TEXTURE_THUMB
                    fsv.r.renderThumbnails = true
                    fsv.requestRender()

                }

            }, BUTTON_CLICK_DELAY_LONG)

        }

        textureListViewTypeButton.setOnClickListener {

            sc.textureListViewType = ListLayoutType.values().run {
                get((sc.textureListViewType.ordinal + 1) % size)
            }

            textureListAdapter.updateLayoutType(sc.textureListViewType)

            when (sc.textureListViewType) {
                ListLayoutType.LINEAR -> {
                    texturePreviewList.layoutManager = previewListLinearManager
                }
                ListLayoutType.GRID -> {
                    texturePreviewList.layoutManager = previewListGridManager
//                    if (!fsv.r.textureThumbsRendered) {
//                        fsv.r.renderProfile = RenderProfile.TEXTURE_THUMB
//                        fsv.r.renderThumbnails = true
//                        fsv.requestRender()
//                    }
                }
            }

        }
        textureDoneButton.setOnClickListener {

            if (fsv.r.isRendering) fsv.r.pauseRender = true

            act.showCategoryButtons()
            if (!act.uiIsClosed()) act.uiSetOpen()
            else MainActivity.Category.TEXTURE.onMenuClosed(act)

            showLayout(textureModeLayout)
            textureSubMenuButtons.show()
            textureNavBar.hide()

            textureParamButtons.forEach { it.hide() }
            f.texture.params.forEachIndexed { index, param ->
                textureParamButtons[index].apply {
                    text = if (param.name == "") "Param ${index + 1}" else param.name
                    show()
                    showGradient = param.goldFeature && !sc.goldEnabled
                }
            }

            textureButtonsScroll.requestLayout()
            textureButtonsScrollLayout.requestLayout()
            textureButtonsScrollLayout.invalidate()

            handler.postDelayed({
                if (textureButtonsScroll.width < textureButtonsScrollLayout.width) {
                    textureScrollArrowRight.show()
                }
                else {
                    textureScrollArrowLeft.invisible()
                    textureScrollArrowRight.invisible()
                }
            }, 200L)

            fsv.r.renderProfile = RenderProfile.DISCRETE

        }



        val subMenuButtonListener = { layout: View, button: Button ->
            View.OnClickListener {
                fsv.r.reaction = Reaction.NONE
                showLayout(layout)
                alphaButton(button)
            }
        }
        val textureParamButtonListener = { button: GradientButton, paramIndex: Int ->
            View.OnClickListener {
                if (button.showGradient && !sc.goldEnabled) act.showUpgradeScreen()
                else {
                    act.showTouchIcon()
                    fsv.r.reaction = Reaction.TEXTURE
                    f.texture.activeParam = f.texture.params[paramIndex]
                    showLayout(if (f.texture.activeParam is ComplexParam) complexTextureParam else realTextureParam)
                    if (!button.showGradient) alphaButton(button)
                    loadActiveParam()
                }
            }
        }

        textureModeButton.setOnClickListener(subMenuButtonListener(textureModeLayout, textureModeButton))
        escapeRadiusButton.setOnClickListener {
            subMenuButtonListener(escapeRadiusLayout, escapeRadiusButton).onClick(null)
            loadEscapeRadius()
        }
        textureParamButtons.forEachIndexed { index, button ->
            button.setOnClickListener(textureParamButtonListener(button, index))
        }

//        binsButton.setOnClickListener {
//
//            fsv.r.reaction = Reaction.TEXTURE
//            f.texture.activeParam = f.texture.bins
//            showLayout(realTextureParam)
//            //shapeResetButton.show()
//            alphaButton(binsButton)
//            loadActiveParam()
//
//        }


        currentButton = textureModeButton
        currentLayout = textureModeLayout
        texturePreviewLayout.hide()
        textureModeLayout.hide()
        escapeRadiusLayout.hide()
        realTextureParam.hide()
        complexTextureParam.hide()

        texturePreviewListLayout.hide()
        textureNavBar.hide()


        showLayout(textureModeLayout)
        alphaButton(textureModeButton)


        super.onViewCreated(v, savedInstanceState)
        Log.e("TEXTURE", "!! onViewCreated end !!")
    
    }

    override fun onResume() {
        super.onResume()
        Log.e("TEXTURE", "!! onResume !!")
    }


    fun onGoldEnabled() {
        textureListAdapter.onGoldEnabled()
        listOf(
                textureParamButton1,
                textureParamButton2,
                textureParamButton3,
                textureParamButton4
        ).forEach { it.showGradient = false }
    }

    private fun getTextureListItems() : MutableList<ListItem<Texture>> {

        val listItems = mutableListOf<ListItem<Texture>>()
        val emptyFavorite = TextureListItem(Texture.emptyFavorite, ListHeader.favorites, sc.textureListViewType)
        val emptyCustom = TextureListItem(Texture.emptyCustom, ListHeader.custom, sc.textureListViewType)

        Texture.all.forEach { listItems.add(

                TextureListItem(
                        it, ListHeader.default, sc.textureListViewType, sc.goldEnabled,
                        disabled = !f.shape.compatTextures.contains(it)
                ).apply {

                    if (it.isFavorite) {
                        val favorite = TextureListItem(
                                it, ListHeader.favorites, sc.textureListViewType, sc.goldEnabled,
                                compliment = this,
                                disabled = !f.shape.compatTextures.contains(it)
                        )
                        compliment = favorite
                        listItems.add(favorite)
                    }

                }

        )}
        if (Texture.all.none { it.isFavorite }) listItems.add(emptyFavorite)
        // if (Shape.custom.isEmpty()) listItems.add(emptyCustom)
        listItems.sortBy { it.header.type }

        return listItems

    }

    private fun loadEscapeRadius() {
        val bailoutStrings = "%e".format(Locale.US, f.bailoutRadius).split("e")
        bailoutSignificandEdit?.setText("%.2f".format(bailoutStrings[0].toFloat()))
        bailoutExponentEdit?.setText("%d".format(bailoutStrings[1].toInt()))
        bailoutSignificandBar?.progress = ((bailoutStrings[0].toDouble() - 1.0)/8.99*bailoutSignificandBar.max).toInt()
        bailoutExponentBar?.progress = if (f.shape.isConvergent) -bailoutStrings[1].toInt() else bailoutStrings[1].toInt()
    }
    fun loadActiveParam() {
        val param = f.texture.activeParam
        if (param is ComplexParam) {
            complexTextureParam.apply {
                uEdit.setText("%.8f".format(param.u))
                uLock.isChecked = param.uLocked
                vEdit.setText("%.8f".format(param.v))
                vLock.isChecked = param.vLocked
                linkParamButton.isChecked = param.linked
                linkParamButton.foregroundTintList = ColorStateList.valueOf(resources.getColor(
                        if (linkParamButton.isChecked) R.color.white else R.color.colorDarkSelected, null
                ))
                // sensitivityButton.show()
            }
        }
        else {
            realTextureParam.apply {
                uEdit2.setText("%.3f".format(param.u))
                realParamSeekBar.setOnSeekBarChangeListener(null)
                realParamSeekBar.progress = (realParamSeekBar.max * (param.u - param.uRange.lower) / (param.uRange.upper - param.uRange.lower)).toInt()
                realParamSeekBar.setOnSeekBarChangeListener(realParamSeekBarListener)
                // sensitivityButton.hide()
            }
        }
    }

}