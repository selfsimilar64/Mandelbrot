package com.selfsimilartech.fractaleye

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.woxthebox.draglistview.DragListView
import com.woxthebox.draglistview.DragListView.DragListListener
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import kotlinx.android.synthetic.main.color_fragment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IndexOutOfBoundsException
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt


class ColorFragment : MenuFragment() {

    private lateinit var act : MainActivity
    private lateinit var f : Fractal
    private lateinit var fsv : FractalSurfaceView
    private lateinit var sc : SettingsConfig

    private fun loadNavButtons(views: List<View>) {

        for (i in 0 until colorNavBar.childCount) colorNavBar.getChildAt(i).hide()
        views.forEach { it.show() }

    }
    private fun updatePreview(palette: ColorPalette) {

        colorPreviewName.text = palette.name
        colorPreviewGradient.foreground = palette.gradientDrawable

    }

    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.color_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        act = activity as MainActivity
        f = act.f
        fsv = act.fsv
        sc = act.sc


        if (!sc.goldEnabled) {
            newCustomColorButton.showGradient = true
            if (ColorPalette.custom.size == ColorPalette.MAX_CUSTOM_PALETTES_FREE) {
                customPaletteNewButton.showGradient = true
            }
        }

        FlexibleAdapter.enableLogs(eu.davidea.flexibleadapter.utils.Log.Level.DEBUG)


        colorLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        colorPreviewListLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

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
            editText.clearFocus()
            act.updateSystemUI()
            true

        }}

        val handler = Handler()
        var customPalette = ColorPalette(name="", colors = arrayListOf())
        var savedCustomColors = arrayListOf<Int>()

        val previewListNavButtons = listOf(
                colorListViewTypeButton,
                customPaletteNewButton,
                colorPreviewListDoneButton
        )
        val customPaletteNavButtons = listOf(
                customPaletteCancelButton,
                customPaletteDoneButton,
                divider9,
                customPaletteRandomizeButton,
                deleteCustomColorButton,
                newCustomColorButton
        )
        val nonClickableViewTypes = listOf(
                R.layout.list_header,
                R.layout.list_item_linear_empty_favorite,
                R.layout.list_item_linear_empty_custom
        )



        frequencyEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()?.toFloat()
            if (result != null) {
                f.frequency = result
            }
            w.text = "%.5f".format(f.frequency)
        })
        phaseEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()?.toFloat()
            if (result != null) {
                f.phase = result
            }
            w.text = "%.3f".format(f.phase)
        })
//        densityEdit.setOnEditorActionListener(editListener(null) {w: TextView ->
//            val result = w.text.toString().formatToDouble()?.toFloat()
//            if (result != null) {
//                f.density = result
//            }
//            w.text = "%.3f".format(f.density)
//        })
        customPaletteName.setOnEditorActionListener(editListener(null) { w: TextView ->

            customPalette.name = w.text.toString()

        })



        val colorPreviewListWidth = Resolution.SCREEN.w -
                2*resources.getDimension(R.dimen.categoryPagerMarginHorizontal) -
                resources.getDimension(R.dimen.colorPreviewListMarginEnd) -
                resources.getDimension(R.dimen.navButtonSize)
        val colorPreviewGridWidth = resources.getDimension(R.dimen.colorPreviewSize) +
                2*resources.getDimension(R.dimen.previewGridPaddingHorizontal)
        //Log.e("COLOR FRAGMENT", "colorPreviewListWidth: $colorPreviewListWidth")
        //Log.e("COLOR FRAGMENT", "colorPreviewGridWidth: $colorPreviewGridWidth")
        val spanCount = floor(colorPreviewListWidth.toDouble() / colorPreviewGridWidth).toInt()
        Log.e("COLOR FRAGMENT", "spanCount: $spanCount")


        colorButtonsScroll.setOnScrollChangeListener(scrollListener(
                colorButtonsScrollLayout,
                colorButtonsScroll,
                colorScrollArrowLeft,
                colorScrollArrowRight
        ))
        colorScrollArrowLeft.invisible()
        colorScrollArrowRight.invisible()


        val colorPreviewListLinearManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val colorPreviewListGridManager = GridLayoutManager(context, spanCount)

        val onEditCustomPalette = { adapter: ListAdapter<ColorPalette>, palette: ColorPalette ->

            colorPreviewListLayout.hide()
            customPaletteLayout.show()
            newCustomColorButton.show()
            loadNavButtons(customPaletteNavButtons)
            fsv.r.renderProfile = RenderProfile.DISCRETE

            f.palette = palette
            fsv.requestRender()

            customPalette = palette
            val customPaletteAdapter = ColorPaletteDragAdapter(
                    customPalette.colors,
                    R.layout.color_drag_item,
                    R.id.customColorLayout,
                    true
            ) { selectedItemIndex, color ->  // LINK COLOR
                satValueSelector.activeColorIndex = selectedItemIndex
                satValueSelector.loadFrom(color, update=true)
                hueSelector.progress = color.hue().toInt()
            }

            customColorsDragList.setAdapter(customPaletteAdapter, true)
            customPaletteAdapter.apply {
                linkColor(0, itemList[0].second)
            }
            customPaletteName.setText(customPalette.name)

            savedCustomColors = ArrayList(customPalette.colors)

        }
        val onDeleteCustomPalette = { adapter: ListAdapter<ColorPalette>, palette: ColorPalette ->

            val deleteId = palette.customId

            GlobalScope.launch {
                act.db.colorPaletteDao().apply {
                    delete(findById(deleteId))
                }
            }

            adapter.apply {
                setActivatedPosition(
                        getGlobalPositionOf(getFavoriteItems().getOrNull(0) ?: getDefaultItems()[1])
                )
                f.palette = (getItem(activatedPos) as? PaletteListItem)!!.palette
            }
            ColorPalette.all.remove(palette)
            ColorPalette.custom.remove(palette)
            if (!sc.goldEnabled && ColorPalette.custom.size < ColorPalette.MAX_CUSTOM_PALETTES_FREE) {
                customPaletteNewButton.showGradient = false
            }
            fsv.requestRender()

        }

        val emptyFavorite = PaletteListItem(ColorPalette.emptyFavorite, ListHeader.favorites, sc.colorListViewType, true)
        val emptyCustom = PaletteListItem(ColorPalette.emptyCustom, ListHeader.custom, sc.colorListViewType, true)
        val listItems = arrayListOf<PaletteListItem>()


        ColorPalette.all.forEach { listItems.add(

            PaletteListItem(
                    it,
                    if (it.hasCustomId || it == ColorPalette.emptyCustom) ListHeader.custom else ListHeader.default,
                    sc.colorListViewType,
                    it == ColorPalette.emptyCustom

            ).apply {

                if (it.isFavorite) {
                    val favorite = PaletteListItem(
                            it,
                            ListHeader.favorites,
                            sc.colorListViewType,
                            compliment = this
                    )
                    compliment = favorite
                    listItems.add(favorite)
                }

            })
        }
        if (ColorPalette.all.none { it.isFavorite }) listItems.add(emptyFavorite)
        if (ColorPalette.custom.isEmpty()) listItems.add(emptyCustom)
        listItems.sortBy { it.header.type }



        val paletteListAdapter = ListAdapter(
                listItems,
                onEditCustomPalette,
                onDeleteCustomPalette,
                emptyFavorite,
                emptyCustom
        )
        colorPreviewList.adapter = paletteListAdapter
        paletteListAdapter.apply {
            //isLongPressDragEnabled = true
            mode = SelectableAdapter.Mode.SINGLE
            showAllHeaders()
            //setActivatedPosition(getFirstPositionOf(f.palette))
            //setAnimationOnForwardScrolling(true)
            //setAnimationOnReverseScrolling(true)
        }
        paletteListAdapter.mItemClickListener = FlexibleAdapter.OnItemClickListener { view, position ->

            if (paletteListAdapter.getItemViewType(position) !in nonClickableViewTypes) {

                if (position != paletteListAdapter.activatedPos) {
                    paletteListAdapter.setActivatedPosition(position)
                }
                val newPalette: ColorPalette = try {
                    paletteListAdapter.getActivatedItem()?.t ?: f.palette
                } catch (e: IndexOutOfBoundsException) {
                    Log.e("COLOR", "array index out of bounds -- list size: ${ColorPalette.all.size}, index: $position")
                    act.showMessage(resources.getString(R.string.msg_error))
                    f.palette
                }

                if (newPalette != f.palette) {

                    f.palette = newPalette
                    fsv.requestRender()

                }
                true //Important!

            }
            else false

        }
        colorPreviewListGridManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (paletteListAdapter.getItemViewType(position) in nonClickableViewTypes) spanCount else 1
            }
        }
        colorPreviewList.layoutManager = when (sc.colorListViewType) {
            ListLayoutType.LINEAR -> colorPreviewListLinearManager
            ListLayoutType.GRID -> colorPreviewListGridManager
        }




        if (f.solidFillColor == Color.WHITE) solidFillColorTabs.getTabAt(1)?.select()
        else solidFillColorTabs.getTabAt(0)?.select()
        solidFillColorTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {}
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabSelected(tab: TabLayout.Tab) {

                f.solidFillColor = when (tab.position) {
                    0 -> Color.BLACK
                    1 -> Color.WHITE
                    else -> Color.BLACK
                }

                fsv.requestRender()

            }
        })







        // CUSTOM COLORS DRAG LIST
        val customColorsDragList = v.findViewById<DragListView>(R.id.customColorsDragList)
        customColorsDragList.setDragListListener(object : DragListListener {

            override fun onItemDragStarted(position: Int) {}
            override fun onItemDragging(itemPosition: Int, x: Float, y: Float) {}
            override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
                if (fromPosition != toPosition) {

                    customPalette.colors.add(toPosition, customPalette.colors.removeAt(fromPosition))
                    customPalette.updateFlatPalette()
                    with (customColorsDragList.adapter as ColorPaletteDragAdapter) {
                        //viewHolderList.add(toPosition, viewHolderList.removeAt(fromPosition))
                        selectedItemIndex = when (selectedItemIndex) {
                            fromPosition -> toPosition
                            in toPosition until fromPosition -> selectedItemIndex + 1
                            in fromPosition until toPosition -> selectedItemIndex - 1
                            else -> selectedItemIndex
                        }
                    }
                    fsv.requestRender()

                }
            }
        })
        customColorsDragList.setLayoutManager(LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false))



        // SATURATION-VALUE SELECTOR
        satValueSelector.apply { onUpdateActiveColor = { newColor: Int ->  // UPDATE ACTIVE COLOR

            customPalette.colors[activeColorIndex] = newColor
            customPalette.updateFlatPalette()
            customPaletteGradient.foreground = customPalette.gradientDrawable
            (customColorsDragList.adapter as ColorPaletteDragAdapter).updateColor(activeColorIndex, newColor)

            val hsv = newColor.toHSV()
            customColorHueEdit.setText("%d".format(hue.toInt()))
            customColorSaturationEdit.setText("%d".format((100f*hsv[1]).roundToInt()))
            customColorValueEdit.setText("%d".format((100f*hsv[2]).roundToInt()))

            invalidate()
            fsv.requestRender()

        }}



        // HUE SELECTOR
        hueSelectorBackground.background = GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP, resources.getIntArray(R.array.hueslider)
        )
        hueSelector.max = 359
        hueSelector.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                satValueSelector.hue = progress.toFloat()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })




        // HSV EDIT TEXTS
        customColorHueEdit.setOnEditorActionListener(editListener(customColorSaturationEdit) { w: TextView ->
            val result = w.text.toString().formatToDouble()?.toFloat()
            if (result != null) {
                hueSelector.progress = result.toInt()
            }
            w.text = "%d".format(satValueSelector.hue.toInt())
        })
        customColorSaturationEdit.setOnEditorActionListener(editListener(customColorValueEdit) { w: TextView ->
            val result = w.text.toString().formatToDouble()?.toFloat()
            if (result != null) {
                satValueSelector.loadFrom(Color.HSVToColor(floatArrayOf(
                        satValueSelector.hue,
                        result/100f,
                        satValueSelector.value()
                )), true, true)
            }
            w.text = "%d".format((100f*satValueSelector.sat()).roundToInt())
        })
        customColorValueEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()?.toFloat()
            if (result != null) {
                satValueSelector.loadFrom(Color.HSVToColor(floatArrayOf(
                        satValueSelector.hue,
                        satValueSelector.sat(),
                        result/100f
                )), true, true)
            }
            w.text = "%d".format((100f*satValueSelector.value()).roundToInt())
        })


        val subMenuButtonListener = { layout: View, button: Button ->
            View.OnClickListener {
                showLayout(layout)
                alphaButton(button)
            }
        }


        // LISTENERS
        paletteListButton.setOnClickListener {

            // ui changes
            handler.postDelayed({

                act.hideCategoryButtons()

                colorSubMenuButtons.hide()
                act.hideCategoryButtons()

                showLayout(colorPreviewListLayout)
                colorNavBar.show()
                loadNavButtons(previewListNavButtons)

                paletteListAdapter.apply {
                    if (selectedPositions.isEmpty()) {
                        Log.d("COLOR", "palette: ${f.palette.name}, first pos: ${getFirstPositionOf(f.palette)}")
                        setActivatedPosition(getFirstPositionOf(f.palette))
                    }
                }

                act.uiSetOpenTall()

            }, BUTTON_CLICK_DELAY_SHORT)

            // color thumbnail render
            handler.postDelayed({

                if (sc.colorListViewType == ListLayoutType.GRID) {
                    fsv.r.renderProfile = RenderProfile.COLOR_THUMB
                    fsv.r.renderThumbnails = true
                    if (!fsv.r.colorThumbsRendered) fsv.r.renderToTex = true
                    fsv.requestRender()
                }

            }, BUTTON_CLICK_DELAY_LONG)

        }
        palettePreviewLayout.setOnClickListener {
            handler.postDelayed({

                act.hideCategoryButtons()

                colorSubMenuButtons.hide()
                palettePreviewLayout.hide()
                act.hideCategoryButtons()

                colorPreviewListLayout.show()
                colorNavBar.show()

                loadNavButtons(previewListNavButtons)

                paletteListAdapter.apply {
                    if (selectedPositions.isEmpty()) setActivatedPosition(getFirstPositionOf(f.palette))
                }

                act.uiSetOpenTall()

                if (sc.colorListViewType == ListLayoutType.GRID) {
                    fsv.r.renderProfile = RenderProfile.COLOR_THUMB
                    fsv.r.renderThumbnails = true
                    if (!fsv.r.colorThumbsRendered) fsv.r.renderToTex = true
                    fsv.requestRender()
                }

            }, BUTTON_CLICK_DELAY_MED)
        }

        frequencyButton.setOnClickListener(subMenuButtonListener(frequencyLayout, frequencyButton))
        frequencySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val newFreq = (progress.toFloat()/frequencySeekBar.max).pow(2f) * 100f
                f.frequency = newFreq
                frequencyEdit.setText("%.5f".format(newFreq))
                fsv.requestRender()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })

        phaseButton.setOnClickListener(subMenuButtonListener(phaseLayout, phaseButton))
        phaseSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val newPhase = progress.toFloat()/phaseSeekBar.max
                f.phase = newPhase
                phaseEdit.setText("%.5f".format(newPhase))
                fsv.requestRender()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })

//        densityButton.setOnClickListener(subMenuButtonListener(densityLayout, densityButton))
//        densitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                val newDensity = progress.toFloat()/densitySeekBar.max*10f + 1f
//                f.density = newDensity
//                densityEdit.setText("%.3f".format(newDensity))
//                fsv.requestRender()
//            }
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
//        })

        colorAutofitButton.setOnClickListener {
            sc.autofitColorRange = colorAutofitButton.isChecked
            fsv.r.resetTextureMinMax = true
            fsv.r.calcNewTextureSpan = true
            if (colorAutofitButton.isChecked) {
                f.frequency = 1f
                fsv.r.renderToTex = true
            }
            else {
                f.frequency /= (fsv.r.textureMaxs.average() - fsv.r.textureMins.average()).toFloat()
            }
            updateFrequencyLayout()
            fsv.requestRender()
        }
        colorAutofitButton.isChecked = sc.autofitColorRange

        solidFillButton.setOnClickListener(subMenuButtonListener(solidFillLayout, solidFillButton))

        colorPreviewListDoneButton.setOnClickListener {
            handler.postDelayed({

                //colorPreviewGradient.foreground = f.palette.gradientDrawable
                //colorPreviewName.text = f.palette.name

                if (!act.uiIsClosed()) act.uiSetOpen() else MainActivity.Category.COLOR.onMenuClosed(act)

                frequencyButton.performClick()
                //palettePreviewLayout.show()
                colorSubMenuButtons.show()
                colorNavBar.hide()
                act.showCategoryButtons()

                fsv.r.renderProfile = RenderProfile.DISCRETE

            }, BUTTON_CLICK_DELAY_SHORT)
        }
        colorListViewTypeButton.setOnClickListener {

            sc.colorListViewType = ListLayoutType.values().run {
                get((sc.colorListViewType.ordinal + 1) % size)
            }
            paletteListAdapter.updateLayoutType(sc.colorListViewType)

            when (sc.colorListViewType) {
                ListLayoutType.LINEAR -> {

                    colorPreviewList.layoutManager = colorPreviewListLinearManager

                    fsv.r.renderProfile = RenderProfile.DISCRETE

                }
                ListLayoutType.GRID -> {

                    colorPreviewList.layoutManager = colorPreviewListGridManager
                    Log.e("COLOR", "${colorPreviewListGridManager.spanSizeLookup.getSpanSize(0)}")

                    fsv.r.renderProfile = RenderProfile.COLOR_THUMB
                    fsv.r.renderThumbnails = true
                    if (!fsv.r.colorThumbsRendered) fsv.r.renderToTex = true
                    fsv.requestRender()

                }
            }

        }

        customPaletteNewButton.setOnClickListener {
            if (ColorPalette.custom.size == ColorPalette.MAX_CUSTOM_PALETTES_FREE && !sc.goldEnabled) act.showUpgradeScreen()
            else {
                handler.postDelayed({

                    colorPreviewListLayout.hide()
                    customPaletteLayout.show()
                    newCustomColorButton.show()
                    loadNavButtons(customPaletteNavButtons)
                    fsv.r.renderProfile = RenderProfile.DISCRETE

                    customPalette = ColorPalette(
                            name = "%s %s %d".format(
                                    resources.getString(R.string.header_custom),
                                    resources.getString(R.string.palette),
                                    ColorPalette.nextCustomPaletteNum
                            ),
                            colors = ColorPalette.generateHighlightColors(if (sc.goldEnabled) 5 else 3)
                    ).apply { initialize(resources) }

                    // ColorPalette.all.add(0, customPalette)
                    f.palette = customPalette
                    fsv.requestRender()


                    val customPaletteAdapter = ColorPaletteDragAdapter(
                            customPalette.colors,
                            R.layout.color_drag_item,
                            R.id.customColorLayout,
                            true
                    ) { selectedItemIndex, color ->  // LINK COLOR
                        satValueSelector.activeColorIndex = selectedItemIndex
                        satValueSelector.loadFrom(color, update = true)
                        hueSelector.progress = color.hue().toInt()
                    }



                    customColorsDragList.setAdapter(customPaletteAdapter, true)
                    customPaletteAdapter.apply {
                        linkColor(0, itemList[0].second)
                    }
                    customPaletteName.setText(customPalette.name)

                }, BUTTON_CLICK_DELAY_SHORT)
            }
        }
        customPaletteDoneButton.setOnClickListener {
            if (ColorPalette.all.any {
                        if (customPalette.name == it.name) {
                            if (customPalette.hasCustomId) customPalette.customId != it.customId
                            else true
                        }
                        else false
                    }) {
                act.showMessage(resources.getString(R.string.msg_custom_name_duplicate).format(
                        resources.getString(R.string.palette)
                ))
            }
            else {

                if (customPalette.hasCustomId) {
                    // update existing palette in database
                    GlobalScope.launch {
                        act.db.colorPaletteDao().apply {
                            update(customPalette.toDatabaseEntity())
                        }
                        paletteListAdapter.getActivatedItem()?.run { paletteListAdapter.updateItem(this) }
                    }
                }
                else {

                    // add new palette to database
                    GlobalScope.launch {
                        act.db.colorPaletteDao().apply {
                            customPalette.customId = insert(customPalette.toDatabaseEntity()).toInt()
                        }
                        paletteListAdapter.apply {
                            val item = PaletteListItem(customPalette, ListHeader.custom, sc.colorListViewType)
                            setActivatedPosition(addItemToCustom(item, 0))
                        }
                        ColorPalette.nextCustomPaletteNum++
                    }

                    ColorPalette.all.add(0, customPalette)
                    ColorPalette.custom.add(0, customPalette)

                    if (sc.colorListViewType == ListLayoutType.GRID) {
                        fsv.r.renderProfile = RenderProfile.COLOR_THUMB
                        fsv.r.renderThumbnails = true
                        fsv.requestRender()
                    }

                }

                // update ui
                handler.postDelayed({

                    if (ColorPalette.custom.size == ColorPalette.MAX_CUSTOM_PALETTES_FREE && !sc.goldEnabled) {
                        customPaletteNewButton.showGradient = true
                    }
                    act.uiSetOpenTall()

                    customPaletteLayout.hide()
                    colorPreviewListLayout.show()
                    // palettePreviewLayout.show()
                    // colorSubMenuButtons.show()
                    loadNavButtons(previewListNavButtons)
                    // colorNavBar.hide()
                    // act.showCategoryButtons()

                    // colorPreviewList.adapter?.notifyDataSetChanged()
                    // updatePreview(f.palette)

                }, BUTTON_CLICK_DELAY_SHORT)
            }
        }
        customPaletteCancelButton.setOnClickListener {
            handler.postDelayed({

                if (customPalette.hasCustomId) {
                    // revert changes
                    customPalette.colors = ArrayList(savedCustomColors)
                    customPalette.updateFlatPalette()
                    paletteListAdapter.getActivatedItem()?.run { paletteListAdapter.updateItem(this) }
                }
                else {
                    // select previous palette
                    f.palette = (paletteListAdapter.getItem(paletteListAdapter.selectedPositions[0]) as? PaletteListItem)?.palette ?: f.palette
                    fsv.requestRender()
                }

                act.uiSetOpenTall()

                customPaletteLayout.hide()
                colorPreviewListLayout.show()
                loadNavButtons(previewListNavButtons)
                // palettePreviewLayout.show()
                // colorSubMenuButtons.show()
                // colorNavBar.hide()
                // act.showCategoryButtons()

                // updatePreview(f.palette)

            }, BUTTON_CLICK_DELAY_SHORT)
        }
        customPaletteRandomizeButton.setOnClickListener {
            val newColors = ColorPalette.generateColors(customPalette.colors.size)
            (customColorsDragList.adapter as ColorPaletteDragAdapter).updateColors(newColors)
            customPalette.colors = newColors
            customPalette.updateFlatPalette()
            customPaletteGradient.foreground = customPalette.gradientDrawable
            fsv.requestRender()
        }
        newCustomColorButton.setOnClickListener {

            if (customPalette.colors.size == ColorPalette.MAX_CUSTOM_COLORS_FREE && !sc.goldEnabled) act.showUpgradeScreen()
            else { with(customColorsDragList.adapter as ColorPaletteDragAdapter) {
                    if (customPalette.colors.size < ColorPalette.MAX_CUSTOM_COLORS_GOLD) {

                        val newColor = randomColor()
                        customPalette.colors.add(newColor)
                        addItem(itemCount, Pair(nextUniqueId, newColor))
                        // itemList.add(Pair(getNextUniqueId(), newColor))
                        notifyItemInserted(itemCount)

                        customPalette.updateFlatPalette()
                        customPaletteGradient.foreground = customPalette.gradientDrawable
                        fsv.requestRender()

                        when (customPalette.colors.size) {
                            ColorPalette.MAX_CUSTOM_COLORS_FREE -> {
                                if (!sc.goldEnabled) newCustomColorButton.showGradient = true
                                deleteCustomColorButton.enable()
                            }
                            ColorPalette.MAX_CUSTOM_COLORS_GOLD -> newCustomColorButton.disable()
                        }

                    }
                } }

        }
        deleteCustomColorButton.setOnClickListener {

            with (customColorsDragList.adapter as ColorPaletteDragAdapter) {

                customPalette.colors.removeAt(selectedItemIndex)
                removeItem(selectedItemIndex)
                // itemList.removeAt(selectedItemIndex)
                if (selectedItemIndex == itemList.size) selectedItemIndex--
                notifyDataSetChanged()

                customPalette.updateFlatPalette()
                customPaletteGradient.foreground = customPalette.gradientDrawable
                fsv.requestRender()

                when (customPalette.colors.size) {
                    ColorPalette.MAX_CUSTOM_COLORS_GOLD - 1 -> newCustomColorButton.enable()
                    ColorPalette.MAX_CUSTOM_COLORS_FREE - 1 -> {
                        if (!sc.goldEnabled) newCustomColorButton.showGradient = false
                        deleteCustomColorButton.disable()
                    }
                }

            }

        }


        
        palettePreviewLayout.hide()
        frequencyLayout.hide()
        phaseLayout.hide()
        //densityLayout.hide()
        solidFillLayout.hide()
        colorPreviewListLayout.hide()
        customPaletteLayout.hide()
        colorNavBar.hide()

        currentLayout = frequencyLayout
        currentButton = frequencyButton
        showLayout(frequencyLayout)
        alphaButton(frequencyButton)

        act.updateColorEditTexts()

        if (sc.goldEnabled) onGoldEnabled()

        super.onViewCreated(v, savedInstanceState)

    }


    fun onGoldEnabled() {
        customPaletteNewButton.showGradient = false
        newCustomColorButton.showGradient = false
        deleteCustomColorButton.show()
    }

    fun updateFrequencyLayout() {
        frequencySeekBar?.progress = ((f.frequency/100f).pow(0.5f)*frequencySeekBar.max).toInt()
    }
    fun updatePhaseLayout() {
        phaseSeekBar?.progress = (f.phase*phaseSeekBar.max).toInt()
    }

}