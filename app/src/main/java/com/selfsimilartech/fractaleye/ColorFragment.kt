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
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.woxthebox.draglistview.DragListView
import com.woxthebox.draglistview.DragListView.DragListListener
import kotlinx.android.synthetic.main.color_fragment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt


class ColorFragment : MenuFragment() {

    private lateinit var act : MainActivity
    private lateinit var f : Fractal
    private lateinit var fsv : FractalSurfaceView
    private lateinit var sc : SettingsConfig

    private fun loadNavButtons(buttons: List<Button>) {

        colorNavBar.removeAllViews()
        buttons.forEach { colorNavBar.addView(it) }

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
        var prevSelectedPaletteIndex = 0
        var customPalette = ColorPalette(name="", colors = arrayListOf())

        val previewListNavButtons = listOf(
                colorListViewTypeButton,
                newCustomPaletteButton.apply { setProFeature(true) },
                colorPreviewListDoneButton
        ).filter { BuildConfig.PAID_VERSION || !it.isProFeature() }
        val previewListNavButtonsCustom = listOf(
                colorListViewTypeButton,
                customPaletteDeleteButton,
                newCustomPaletteButton,
                colorPreviewListDoneButton
        )

        //Log.e("COLOR FRAGMENT", "${previewListNavButtons.size}")
        val customPaletteNavButtons = listOf(
                customPaletteRandomizeButton,
                customPaletteCancelButton,
                customPaletteDoneButton
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





        val colorPreviewListWidth = fsv.r.screenRes.x -
                2*resources.getDimension(R.dimen.categoryPagerMarginHorizontal) -
                resources.getDimension(R.dimen.colorPreviewListMarginEnd) -
                resources.getDimension(R.dimen.navButtonSize)
        val colorPreviewGridWidth = resources.getDimension(R.dimen.colorPreviewSize) +
                2*resources.getDimension(R.dimen.previewGridPaddingHorizontal)
        //Log.e("COLOR FRAGMENT", "colorPreviewListWidth: $colorPreviewListWidth")
        //Log.e("COLOR FRAGMENT", "colorPreviewGridWidth: $colorPreviewGridWidth")
        val spanCount = floor(colorPreviewListWidth.toDouble() / colorPreviewGridWidth).toInt()
        //Log.e("COLOR FRAGMENT", "spanCount: ${colorPreviewListWidth.toDouble() / colorPreviewGridWidth}")

        val colorPreviewListLinearManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val colorPreviewListLinearAdapter = ColorPaletteAdapter(ColorPalette.all, R.layout.color_preview_item_linear)
        val colorPreviewListGridManager = GridLayoutManager(context, spanCount)
        val colorPreviewListGridAdapter = ColorPaletteAdapter(ColorPalette.all, R.layout.color_preview_item_grid)
        when (sc.colorListViewType) {
            ListLayoutType.LINEAR -> {
                colorPreviewList.layoutManager = colorPreviewListLinearManager
                colorPreviewList.adapter = colorPreviewListLinearAdapter
            }
            ListLayoutType.GRID -> {
                colorPreviewList.layoutManager = colorPreviewListGridManager
                colorPreviewList.adapter = colorPreviewListGridAdapter
            }
        }


        updatePreview(f.palette)


        colorPreviewList.addOnItemTouchListener(
                RecyclerTouchListener(
                        v.context,
                        colorPreviewList,
                        object : ClickListener {

                            override fun onClick(view: View, position: Int) {
                                val newPalette : ColorPalette = try {
                                    ColorPalette.all[position]
                                } catch (e: ArrayIndexOutOfBoundsException) {
                                    Log.e("COLOR", "array index out of bounds -- list size: ${ColorPalette.all.size}, index: $position")
                                    act.showMessage(resources.getString(R.string.msg_error))
                                    f.palette
                                }
                                if (newPalette != f.palette) {

                                    if (BuildConfig.PAID_VERSION) {
                                        if (newPalette.isCustom != f.palette.isCustom) {
                                            loadNavButtons(if (f.palette.isCustom)
                                                previewListNavButtons else
                                                previewListNavButtonsCustom
                                            )
                                        }
                                    }

                                    f.palette = newPalette
                                    fsv.requestRender()

                                }
                            }

                            override fun onLongClick(view: View, position: Int) {}

                        }
                )
        )


        solidFillColorTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {}
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabSelected(tab: TabLayout.Tab) {

                f.solidFillColor = when (tab.position) {
                    0 -> R.color.black
                    1 -> R.color.white
                    else -> R.color.cyan
                }

                fsv.requestRender()

            }
        })
        solidFillColorTabs.getTabAt(1)?.select()







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
            (customColorsDragList.adapter as ColorPaletteDragAdapter).updateColor(activeColorIndex, newColor)

            val hsv = newColor.toHSV()
            customColorComponentEdit1.setText("%d".format(hue.toInt()))
            customColorComponentEdit2.setText("%d".format((100f*hsv[1]).roundToInt()))
            customColorComponentEdit3.setText("%d".format((100f*hsv[2]).roundToInt()))

            invalidate()
            fsv.requestRender()

        }}



        // HUE SELECTOR
        hueSelectorBackground.background = GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                getColors(resources, listOf(
                    R.color.hueslider1,
                    R.color.hueslider2,
                    R.color.hueslider3,
                    R.color.hueslider4,
                    R.color.hueslider5,
                    R.color.hueslider6,
                    R.color.hueslider7,
                    R.color.hueslider8,
                    R.color.hueslider9,
                    R.color.hueslider10,
                    R.color.hueslider11,
                    R.color.hueslider12,
                    R.color.hueslider1
        )))
        hueSelector.max = 359
        hueSelector.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                satValueSelector.hue = progress.toFloat()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })




        // HSV EDIT TEXTS
        customColorComponentEdit1.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()?.toFloat()
            if (result != null) {
                hueSelector.progress = result.toInt()
            }
            w.text = "%d".format(satValueSelector.hue.toInt())
        })
        customColorComponentEdit2.setOnEditorActionListener(editListener(null) { w: TextView ->
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
        customColorComponentEdit3.setOnEditorActionListener(editListener(null) { w: TextView ->
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
        palettePreviewButton.setOnClickListener(subMenuButtonListener(palettePreviewLayout, palettePreviewButton))
        palettePreviewLayout.setOnClickListener {
            handler.postDelayed({

                act.hideCategoryButtons()

                colorSubMenuButtons.hide()
                palettePreviewLayout.hide()
                act.hideCategoryButtons()

                colorPreviewListLayout.show()
                colorNavBar.show()

                loadNavButtons(previewListNavButtons)

                act.uiSetOpenTall()

                with (colorPreviewList.adapter as ColorPaletteAdapter) { if (isGridLayout) {
                    fsv.r.renderProfile = RenderProfile.COLOR_THUMB
                    fsv.r.renderThumbnails = true
                    if (!fsv.r.colorThumbsRendered) fsv.r.renderToTex = true
                    fsv.requestRender()
                }}

            }, BUTTON_CLICK_DELAY_LONG)
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

                colorPreviewGradient.foreground = f.palette.gradientDrawable
                colorPreviewName.text = f.palette.name

                if (!act.uiIsClosed()) act.uiSetOpen() else MainActivity.Category.COLOR.onMenuClosed(act)

                colorPreviewListLayout.hide()
                palettePreviewLayout.show()
                colorSubMenuButtons.show()
                colorNavBar.hide()
                act.showCategoryButtons()

                fsv.r.renderProfile = RenderProfile.MANUAL

            }, BUTTON_CLICK_DELAY_SHORT)
        }
        newCustomPaletteButton.setOnClickListener {
            handler.postDelayed({

                colorPreviewListLayout.hide()
                customPaletteLayout.show()
                loadNavButtons(customPaletteNavButtons)
                fsv.r.renderProfile = RenderProfile.MANUAL

                customPalette = ColorPalette(
                        name = "Custom Palette 1",
                        colors = ColorPalette.generateHighlightColors(4)
                ).apply { initialize(resources, Resolution.THUMB.scaleRes(fsv.r.screenRes)) }

                prevSelectedPaletteIndex = ColorPalette.all.indexOf(f.palette)
                ColorPalette.all.add(0, customPalette)
                f.palette = ColorPalette.all[0]
                fsv.requestRender()


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

            }, BUTTON_CLICK_DELAY_SHORT)
        }
        colorListViewTypeButton.setOnClickListener {

            sc.colorListViewType = ListLayoutType.values().run {
                get((sc.colorListViewType.ordinal + 1) % size)
            }

            when (sc.colorListViewType) {
                ListLayoutType.LINEAR -> {

                    colorPreviewList.layoutManager = colorPreviewListLinearManager
                    colorPreviewList.adapter = colorPreviewListLinearAdapter

                    fsv.r.renderProfile = RenderProfile.MANUAL

                }
                ListLayoutType.GRID -> {

                    colorPreviewList.layoutManager = colorPreviewListGridManager
                    colorPreviewList.adapter = colorPreviewListGridAdapter

                    fsv.r.renderProfile = RenderProfile.COLOR_THUMB
                    fsv.r.renderThumbnails = true
                    fsv.r.renderToTex = true
                    fsv.requestRender()

                }
            }

        }
        customPaletteDeleteButton.setOnClickListener {

            val deleteId = f.palette.customId

            GlobalScope.launch {
                act.db.colorPaletteDao().apply {
                    delete(findById(deleteId))
                }
            }

            val index = ColorPalette.all.indexOf(f.palette)
            ColorPalette.all.remove(f.palette)
            f.palette = ColorPalette.all[index]
            fsv.requestRender()
            colorPreviewList.adapter?.apply { notifyItemRemoved(index) }

        }

        customPaletteDoneButton.setOnClickListener {

            GlobalScope.launch {

                val colors = MutableList(8) { 0 }
                customPalette.colors.forEachIndexed { i: Int, c: Int ->
                    colors[i] = c
                }
                Log.e("COLOR", "custom palette size: ${customPalette.size}")

                act.db.colorPaletteDao().apply {
                    customPalette.customId = insert(ColorPaletteEntity(
                            name = customPalette.name,
                            size = customPalette.colors.size,
                            c1 = colors[0],
                            c2 = colors[1],
                            c3 = colors[2],
                            c4 = colors[3],
                            c5 = colors[4],
                            c6 = colors[5],
                            c7 = colors[6],
                            c8 = colors[7]
                    )).toInt()
                }

            }

            handler.postDelayed({

                act.uiSetHeight(resources.getDimension(R.dimen.uiLayoutHeight).toInt())

                customPaletteLayout.hide()
                palettePreviewLayout.show()
                colorSubMenuButtons.show()
                colorNavBar.hide()
                act.showCategoryButtons()

                colorPreviewList.adapter?.notifyDataSetChanged()
                updatePreview(f.palette)

            }, BUTTON_CLICK_DELAY_SHORT)
        }
        customPaletteCancelButton.setOnClickListener {
            handler.postDelayed({

                ColorPalette.all.removeAt(0)
                f.palette = ColorPalette.all[prevSelectedPaletteIndex]
                fsv.requestRender()

                act.uiSetOpen()

                customPaletteLayout.hide()
                palettePreviewLayout.show()
                colorSubMenuButtons.show()
                colorNavBar.hide()
                act.showCategoryButtons()

                updatePreview(f.palette)

            }, BUTTON_CLICK_DELAY_SHORT)
        }
        customPaletteRandomizeButton.setOnClickListener {
            val newColors = ColorPalette.generateColors(customPalette.colors.size)
            (customColorsDragList.adapter as ColorPaletteDragAdapter).updateColors(newColors)
            customPalette.colors = newColors
            customPalette.updateFlatPalette()
            fsv.requestRender()
        }
        newCustomColorButton.setOnClickListener {

            with (customColorsDragList.adapter as ColorPaletteDragAdapter) {

                if (customPalette.colors.size < ColorPalette.MAX_CUSTOM_PALETTE_COLORS) {

                    val newColor = randomColor()
                    customPalette.colors.add(newColor)
                    itemList.add(Pair(this.itemList.size.toLong(), newColor))
                    notifyDataSetChanged()

                    customPalette.updateFlatPalette()
                    fsv.requestRender()

                    if (customPalette.colors.size == ColorPalette.MAX_CUSTOM_PALETTE_COLORS) newCustomColorButton.hide()

                }

            }

        }



        currentLayout = palettePreviewLayout
        currentButton = palettePreviewButton
        palettePreviewLayout.hide()
        frequencyLayout.hide()
        phaseLayout.hide()
        //densityLayout.hide()
        solidFillLayout.hide()
        colorPreviewListLayout.hide()
        customPaletteLayout.hide()
        colorNavBar.hide()

        palettePreviewButton.performClick()

        act.updateColorEditTexts()
        super.onViewCreated(v, savedInstanceState)

    }

    fun updateFrequencyLayout() {
        frequencySeekBar.progress = ((f.frequency/100f).pow(0.5f)*frequencySeekBar.max).toInt()
    }
    fun updatePhaseLayout() {
        phaseSeekBar.progress = (f.phase*phaseSeekBar.max).toInt()
    }

}