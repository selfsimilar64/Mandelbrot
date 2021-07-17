package com.selfsimilartech.fractaleye

import android.animation.LayoutTransition
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.*
import com.woxthebox.draglistview.DragListView
import com.woxthebox.draglistview.DragListView.DragListListener
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import kotlinx.android.synthetic.main.color_fragment.*
import kotlinx.android.synthetic.main.color_fragment.accentColorPicker
import kotlinx.android.synthetic.main.color_picker2.view.*
import kotlinx.android.synthetic.main.list_layout.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IndexOutOfBoundsException
import kotlin.math.*


class ColorFragment : MenuFragment() {

    private fun loadNavButtons(views: List<View>) {

        for (i in 0 until colorNavBar.childCount) colorNavBar.getChildAt(i).hide()
        views.forEach { it.show() }

    }

    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.color_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        super.onViewCreated(v, savedInstanceState)



        if (!sc.goldEnabled) {
            customColorAddButton.showGradient = true
            if (Palette.custom.size == Palette.MAX_CUSTOM_PALETTES_FREE) {
                customPaletteNewButton.showGradient = true
            }
        }



        colorLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        // paletteListLayout.listLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        val handler = Handler(Looper.getMainLooper())
        var customPalette = Palette(name="", colors = arrayListOf())
        var savedCustomName = ""
        var savedCustomColors = arrayListOf<Int>()

        val previewListNavButtons = listOf(
                customPaletteNewButton,
                paletteListDoneButton
        )
        val customPaletteNavButtons = listOf(
                customPaletteCancelButton,
                customPaletteDoneButton,
                customPaletteRandomizeButton,
                customColorRemoveButton,
                customColorAddButton
        )
        val nonClickableViewTypes = listOf(
                R.layout.list_header,
                R.layout.list_item_linear_empty_favorite,
                R.layout.list_item_linear_empty_custom
        )


        updateGradient()

        frequencyValue.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()?.toFloat()
            if (result != null) {
                f.frequency = result
            }
            w.text = "%.3f".format(f.frequency)
        })
        phaseValue.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()?.toFloat()
            if (result != null) {
                f.phase = result
            }
            w.text = "%.3f".format(f.phase)
        })
        densityValue.setOnEditorActionListener(editListener(null) {w: TextView ->
            val result = w.text.toString().formatToDouble()?.toFloat()
            if (result != null) {
                f.density = result
            }
            w.text = "%.3f".format(f.density)
        })
        customPaletteName.setOnEditorActionListener(editListener(null) { w: TextView ->

            customPalette.name = w.text.toString()

        })


//        colorButtonsScroll.setOnScrollChangeListener(scrollListener(
//                colorButtonsScrollLayout,
//                colorButtonsScroll,
//                colorScrollArrowLeft,
//                colorScrollArrowRight
//        ))
//        colorScrollArrowLeft.invisible()
//        colorScrollArrowRight.invisible()


        val paletteListLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val onEditConfirm = { adapter: ListAdapter<Palette>, item: ListItem<Palette> ->

            paletteListLayout.hide()
            customPaletteLayout.show()
            customColorAddButton.show()
            loadNavButtons(customPaletteNavButtons)
            fsv.r.renderProfile = RenderProfile.DISCRETE

            f.palette = item.t
            fsv.requestRender()

            item.t.apply {
                savedCustomName = name
                savedCustomColors = ArrayList(colors)
            }

            customPalette = item.t
            val customPaletteAdapter = ColorPaletteDragAdapter(
                    customPalette.colors,
                    R.layout.color_drag_item,
                    R.id.customColorLayout,
                    true
            ) { selectedItemIndex, color ->  // LINK COLOR
                customPaletteColorPicker.apply {
                    satValueSelector.activeColorIndex = selectedItemIndex
                    satValueSelector.loadFrom(color, update = true)
                    hueSelector.progress = color.hue().toInt()
                }
            }

            customColorsDragList.setAdapter(customPaletteAdapter, true)
            customPaletteAdapter.apply {
                linkColor(0, itemList[0].second)
            }
            customPaletteName.setText(customPalette.name)

        }
        val onEditCustomPalette = { adapter: ListAdapter<Palette>, item: ListItem<Palette> ->

            if (Fractal.bookmarks.any { it.palette == item.t }) {
                val dialog = AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom)
                        .setIcon(R.drawable.warning)
                        .setTitle("${resources.getString(R.string.edit)} ${item.t.name}?")
                        .setMessage(resources.getString(R.string.edit_palette_bookmark_warning).format(
                                Fractal.bookmarks.count { it.palette == item.t }
                        ))
                        .setPositiveButton(R.string.edit) { dialog, whichButton -> onEditConfirm(adapter, item) }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
            } else onEditConfirm(adapter, item)

        }
        val onDeleteCustomPalette = { adapter: ListAdapter<Palette>, item: ListItem<Palette> ->

            val dialog = AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom)
                    .setTitle("${resources.getString(R.string.delete)} ${item.t.name}?")
                    .setMessage(resources.getString(R.string.delete_palette_bookmark_warning).format(
                            Fractal.bookmarks.count { it.palette == item.t }
                    ))
                    .setIcon(R.drawable.warning)
                    .setPositiveButton(android.R.string.ok) { dialog, whichButton ->

                        adapter.removeItemFromCustom(item)
                        Fractal.bookmarks.filter { it.palette == item.t }.forEach {
                            it.palette = Palette.night
                            GlobalScope.launch {
                                act.db.fractalDao().update(it.customId, it.palette.id, 0)
                            }
                        }

                        item.t.release()
                        val deleteId = item.t.id

                        GlobalScope.launch {
                            act.db.colorPaletteDao().apply {
                                delete(findById(deleteId))
                            }
                        }

                        adapter.apply {
                            setActivatedPosition(
                                    getGlobalPositionOf(getFavoriteItems().getOrNull(0) ?: getDefaultItems()[1])
                            )
                            f.palette = (getItem(activatedPos))?.t ?: Palette.night
                        }
                        Palette.all.remove(item.t)
                        Palette.custom.remove(item.t)
                        if (!sc.goldEnabled && Palette.custom.size < Palette.MAX_CUSTOM_PALETTES_FREE) {
                            customPaletteNewButton.showGradient = false
                        }
                        fsv.requestRender()

                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()

        }
        val onDuplicatePalette = { adapter: ListAdapter<Palette>, item: ListItem<Palette> ->

            if (!sc.goldEnabled) act.showUpgradeScreen()
            else handler.postDelayed({

                    paletteListLayout.hide()
                    customPaletteLayout.show()
                    customColorAddButton.show()
                    loadNavButtons(customPaletteNavButtons)
                    fsv.r.renderProfile = RenderProfile.DISCRETE

                    customPalette = item.t.clone(resources)

                    f.palette = customPalette
                    fsv.requestRender()


                    val customPaletteAdapter = ColorPaletteDragAdapter(
                            customPalette.colors,
                            R.layout.color_drag_item,
                            R.id.customColorLayout,
                            true
                    ) { selectedItemIndex, color ->  // LINK COLOR
                        customPaletteColorPicker.apply {
                            satValueSelector.activeColorIndex = selectedItemIndex
                            satValueSelector.loadFrom(color, update = true)
                            hueSelector.progress = color.hue().toInt()
                        }
                    }

                    customColorsDragList.setAdapter(customPaletteAdapter, true)
                    customPaletteAdapter.apply {
                        linkColor(0, itemList[0].second)
                    }
                    customPaletteName.setText(customPalette.name)

                }, BUTTON_CLICK_DELAY_SHORT)
            Unit

        }

        val emptyFavorite = ListItem(Palette.emptyFavorite, ListHeader.FAVORITE, R.layout.list_item_linear_empty_favorite)
        val emptyCustom = ListItem(Palette.emptyCustom, ListHeader.CUSTOM, R.layout.list_item_linear_empty_custom)
        val listItems = arrayListOf<ListItem<Palette>>()


        Palette.all.forEach { listItems.add(

            ListItem(
                    it,
                    if (it.hasCustomId || it == Palette.emptyCustom) ListHeader.CUSTOM else ListHeader.DEFAULT,
                    R.layout.palette_list_item,

            ).apply {
                if (it.isFavorite) {
                    val favorite = ListItem(
                            it,
                            ListHeader.FAVORITE,
                            R.layout.palette_list_item,
                            compliment = this
                    )
                    compliment = favorite
                    listItems.add(favorite)
                }
            })
        }
        if (Palette.all.none { it.isFavorite }) listItems.add(emptyFavorite)
        if (Palette.custom.isEmpty()) listItems.add(emptyCustom)
        listItems.sortBy { it.header.type }



        val paletteListAdapter = ListAdapter(
                listItems,
                onEditCustomPalette,
                onDeleteCustomPalette,
                onDuplicatePalette,
                emptyFavorite,
                emptyCustom
        )
        paletteListLayout.apply {
            list.adapter = paletteListAdapter
            list.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                val firstVisiblePos = paletteListLayoutManager.findFirstCompletelyVisibleItemPosition()
                highlightListHeader(paletteListAdapter, when {
                    firstVisiblePos < paletteListAdapter.getGlobalPositionOf(paletteListAdapter.headerItems[1]) -> 0
                    firstVisiblePos < paletteListAdapter.getGlobalPositionOf(paletteListAdapter.headerItems[2]) -> 1
                    else -> 2
                })
            }
            list.layoutManager = paletteListLayoutManager
            list.setHasFixedSize(true)
        }
        paletteListAdapter.apply {
            mode = SelectableAdapter.Mode.SINGLE
            showAllHeaders()
        }

        paletteListAdapter.mItemClickListener = FlexibleAdapter.OnItemClickListener { view, position ->

            if (paletteListAdapter.getItemViewType(position) !in nonClickableViewTypes) {

                val firstVisiblePos = paletteListLayoutManager.findFirstCompletelyVisibleItemPosition()
                val lastVisiblePos = paletteListLayoutManager.findLastCompletelyVisibleItemPosition()
                if (position + 1 > lastVisiblePos) paletteListLayout.list.smoothSnapToPosition(position + 1, LinearSmoothScroller.SNAP_TO_END)
                else if (position - 1 < firstVisiblePos) paletteListLayout.list.smoothSnapToPosition(position - 1)

                if (position != paletteListAdapter.activatedPos) {
                    paletteListAdapter.setActivatedPosition(position)
                }
                val newPalette: Palette = try {
                    paletteListAdapter.getActivatedItem()?.t ?: f.palette
                } catch (e: IndexOutOfBoundsException) {
                    Log.e("COLOR", "array index out of bounds -- list size: ${Palette.all.size}, index: $position")
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

//        colorPreviewListGridManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
//            override fun getSpanSize(position: Int): Int {
//                return if (paletteListAdapter.getItemViewType(position) in nonClickableViewTypes) spanCount else 1
//            }
//        }




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



        customPaletteColorPicker.apply {

            // SATURATION-VALUE SELECTOR
            satValueSelector.apply {
                onUpdateActiveColor = { newColor: Int ->  // UPDATE ACTIVE COLOR

                    customPalette.colors[activeColorIndex] = newColor
                    customPalette.updateFlatPalette()
                    // customPaletteGradient.foreground = customPalette.gradientDrawable
                    (customColorsDragList.adapter as ColorPaletteDragAdapter).updateColor(activeColorIndex, newColor)

                    val hsv = newColor.toHSV()
                    val h = hue.let { if (it.isNaN()) 0 else it.roundToInt() }
                    val s = (100f * hsv[1]).let { if (it.isNaN()) 0 else it.roundToInt() }
                    val value = (100f * hsv[2]).let { if (it.isNaN()) 0 else it.roundToInt()}

                    customColorHueEdit.setText("%d".format(h))
                    customColorSaturationEdit.setText("%d".format(s))
                    customColorValueEdit.setText("%d".format(value))

                    invalidate()
                    fsv.requestRender()

                }
            }

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
                            result / 100f,
                            satValueSelector.value()
                    )), true, true)
                }
                val s = (100f * satValueSelector.sat()).let { if (it.isNaN()) 0 else it.roundToInt() }
                w.text = "%d".format(s)
            })
            customColorValueEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
                val result = w.text.toString().formatToDouble()?.toFloat()
                if (result != null) {
                    satValueSelector.loadFrom(Color.HSVToColor(floatArrayOf(
                            satValueSelector.hue,
                            satValueSelector.sat(),
                            result / 100f
                    )), true, true)
                }
                val value = (100f * satValueSelector.value()).let { if (it.isNaN()) 0 else it.roundToInt() }
                w.text = "%d".format(value)
            })

        }



        // LISTENERS


        paletteListLayout.apply {
            listFavoritesButton.setOnClickListener {
                list.smoothSnapToPosition(paletteListAdapter.getGlobalPositionOf(paletteListAdapter.headerItems[0]))
            }
            listCustomButton.setOnClickListener {
                list.smoothSnapToPosition(paletteListAdapter.getGlobalPositionOf(paletteListAdapter.headerItems[1]))
            }
            listDefaultButton.setOnClickListener {
                list.smoothSnapToPosition(paletteListAdapter.getGlobalPositionOf(paletteListAdapter.headerItems[2]))
            }
        }

        paletteListButton.setOnClickListener {

            // ui changes
            handler.postDelayed({

                act.hideCategoryButtons()

                colorSubMenuButtons.hide()
                paletteListLayout.show()
                colorNavBar.show()
                act.hideMenuToggleButton()
                currentLayout.hide()
                loadNavButtons(previewListNavButtons)

                paletteListAdapter.apply {
                    (if (selectedPositions.isEmpty()) getFirstPositionOf(f.palette) else activatedPos).let {
                        setActivatedPosition(it)
                        recyclerView?.scrollToPosition(it)
                    }
                }

                act.uiSetHeight(UiLayoutHeight.TALL)

            }, BUTTON_CLICK_DELAY_SHORT)

            // color thumbnail render
            handler.postDelayed({

                fsv.r.renderProfile = RenderProfile.COLOR_THUMB
                fsv.r.renderThumbnails = true
                if (!fsv.r.colorThumbsRendered) fsv.r.renderToTex = true
                fsv.requestRender()

            }, BUTTON_CLICK_DELAY_LONG)

        }
        palettePreviewLayout.setOnClickListener {
            handler.postDelayed({

                act.hideCategoryButtons()

                colorSubMenuButtons.hide()
                palettePreviewLayout.hide()
                act.hideCategoryButtons()

                paletteListLayout.show()
                colorNavBar.show()

                loadNavButtons(previewListNavButtons)

                paletteListAdapter.apply {
                    if (selectedPositions.isEmpty()) setActivatedPosition(getFirstPositionOf(f.palette))
                }

                act.uiSetHeight(UiLayoutHeight.TALL)

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
                if (fromUser) {
                    val newFreq = (progress.toFloat()/frequencySeekBar.max).pow(2f) * 100f
                    f.frequency = newFreq
                    Log.e("COLOR", "frequency set to $newFreq")
                    frequencyValue.setText("%.3f".format(newFreq))
                    fsv.requestRender()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })

        phaseButton.setOnClickListener(subMenuButtonListener(phaseLayout, phaseButton))
        phaseSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newPhase = progress.toFloat() / phaseSeekBar.max
                    f.phase = newPhase
                    phaseValue.setText("%.3f".format(newPhase))
                    fsv.requestRender()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })

        densityButton.setOnClickListener(subMenuButtonListener(densityLayout, densityButton))
        densitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newDensity = 3f * progress.toFloat() / densitySeekBar.max
                    f.density = newDensity
                    densityValue.setText("%.3f".format(newDensity))
                    fsv.requestRender()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        colorAutofitButton.setOnClickListener {

            sc.autofitColorRange = colorAutofitButton.isChecked
            fsv.r.autofitColorSelected = colorAutofitButton.isChecked
            fsv.r.calcNewTextureSpan = true
            if (colorAutofitButton.isChecked) {

                if (f.texture.usesDensity) {
                    densityButton.show()
                    densityButton.performClick()
                }
                fsv.r.renderToTex = true

            }
            else {

                f.density = 0f
                densityButton.hide()
                frequencyButton.performClick()

                // adjust frequency and phase to match old fit

                val M = fsv.r.textureSpan.max()
                val m = fsv.r.textureSpan.min()
                val L = M - m
                val prevFreq = f.frequency
                val prevPhase = f.phase

                fsv.r.setTextureSpan(0f, 1f)

                f.frequency = prevFreq/L
                f.phase = prevPhase - prevFreq*m/L
                Log.e("COLOR", "frequency set ${f.frequency}")

            }
            updateFrequencyLayout()
            updatePhaseLayout()
            fsv.requestRender()

        }
        colorAutofitButton.isChecked = sc.autofitColorRange
        if (colorAutofitButton.isChecked && f.texture.usesDensity) densityButton.show() else densityButton.hide()


        accentColor1Button.setOnClickListener {
            subMenuButtonListener(miniColorPickerLayout, accentColor1Button, UiLayoutHeight.MED).onClick(it)
            loadAccentColor()
        }
        accentColor2Button.setOnClickListener {
            subMenuButtonListener(miniColorPickerLayout, accentColor2Button, UiLayoutHeight.MED).onClick(it)
            loadAccentColor()
        }

        (accentColor1Button.compoundDrawables[1] as? GradientDrawable)?.setColor(f.accent1)
        (accentColor2Button.compoundDrawables[1] as? GradientDrawable)?.setColor(f.accent2)

        accentColorPicker.apply {
            satValueSelector?.onUpdateActiveColor = { c ->
                accentColor?.setBackgroundColor(c)
                when (currentButton) {
                    accentColor1Button -> f.accent1 = c
                    accentColor2Button -> f.accent2 = c
                }
                satValueSelector?.invalidate()
                (currentButton.compoundDrawables[1] as? GradientDrawable)?.setColor(c)
                fsv.requestRender()
            }
            hueSelectorBackground?.background = GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP, resources.getIntArray(R.array.hueslider)
            )
            hueSelector?.max = 359
            hueSelector?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    satValueSelector?.hue = progress.toFloat()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }


        paletteListDoneButton.setOnClickListener {
            handler.postDelayed({

                //colorPreviewGradient.foreground = f.palette.gradientDrawable
                //colorPreviewName.text = f.palette.name

                paletteGradient.foreground = f.palette.gradientDrawable
                if (!act.uiIsClosed()) act.uiSetHeight(UiLayoutHeight.SHORT) else MainActivity.EditMode.COLOR.onMenuClosed(act)

                // frequencyButton.performClick()
                // palettePreviewLayout.show()
                colorSubMenuButtons.show()
                colorNavBar.hide()
                paletteListLayout.hide()

                showLayout(frequencyLayout)
                alphaButton(frequencyButton)
                act.showCategoryButtons()
                act.showMenuToggleButton()

                fsv.r.renderProfile = RenderProfile.DISCRETE

            }, BUTTON_CLICK_DELAY_SHORT)
        }
//        paletteListViewTypeButton.setOnClickListener {
//
//            sc.colorListViewType = ListLayoutType.values().run {
//                get((sc.colorListViewType.ordinal + 1) % size)
//            }
//            paletteListAdapter.updateLayoutType(sc.colorListViewType)
//
//            when (sc.colorListViewType) {
//                ListLayoutType.LINEAR -> {
//
//                    paletteList.layoutManager = paletteListLayoutManager
//
//                    fsv.r.renderProfile = RenderProfile.DISCRETE
//
//                }
//                ListLayoutType.GRID -> {
//
//                    paletteList.layoutManager = colorPreviewListGridManager
//                    Log.e("COLOR", "${colorPreviewListGridManager.spanSizeLookup.getSpanSize(0)}")
//
//                    fsv.r.renderProfile = RenderProfile.COLOR_THUMB
//                    fsv.r.renderThumbnails = true
//                    if (!fsv.r.colorThumbsRendered) fsv.r.renderToTex = true
//                    fsv.requestRender()
//
//                }
//            }
//
//        }

        customPaletteNewButton.setOnClickListener {
            if (Palette.custom.size == Palette.MAX_CUSTOM_PALETTES_FREE && !sc.goldEnabled) act.showUpgradeScreen()
            else {
                handler.postDelayed({

                    paletteListLayout.hide()
                    customPaletteLayout.show()
                    customColorAddButton.show()
                    loadNavButtons(customPaletteNavButtons)
                    fsv.r.renderProfile = RenderProfile.DISCRETE

                    customPalette = Palette(
                            name = "%s %s %d".format(
                                    resources.getString(R.string.header_custom),
                                    resources.getString(R.string.palette),
                                    Palette.nextCustomPaletteNum
                            ),
                            colors = Palette.generateHighlightColors(if (sc.goldEnabled) 5 else 3)
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
                        customPaletteColorPicker.apply {
                            satValueSelector.activeColorIndex = selectedItemIndex
                            satValueSelector.loadFrom(color, update = true)
                            hueSelector.progress = color.hue().toInt()
                        }
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
            if (Palette.all.any {
                        if (customPalette.name == it.name) {
                            if (customPalette.hasCustomId) customPalette.id != it.id
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
                            customPalette.id = insert(customPalette.toDatabaseEntity()).toInt()
                            customPalette.hasCustomId = true
                        }
                        paletteListAdapter.apply {
                            val item = ListItem(customPalette, ListHeader.CUSTOM, R.layout.palette_list_item)
                            setActivatedPosition(addItemToCustom(item, 0))
                        }
                        Palette.nextCustomPaletteNum++
                    }

                    Palette.all.add(0, customPalette)
                    Palette.custom.add(0, customPalette)

                    if (sc.colorListViewType == ListLayoutType.GRID) {
                        fsv.r.renderProfile = RenderProfile.COLOR_THUMB
                        fsv.r.renderThumbnails = true
                        fsv.requestRender()
                    }

                }

                // update ui
                handler.postDelayed({

                    if (Palette.custom.size == Palette.MAX_CUSTOM_PALETTES_FREE && !sc.goldEnabled) {
                        customPaletteNewButton.showGradient = true
                    }
                    act.uiSetHeight(UiLayoutHeight.TALL)

                    customPaletteLayout.hide()
                    paletteListLayout.show()
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
                    customPalette.apply {
                        name = savedCustomName
                        colors = ArrayList(savedCustomColors)
                        updateFlatPalette()
                    }
                    paletteListAdapter.getActivatedItem()?.run { paletteListAdapter.updateItem(this) }
                    Log.d("COLOR", "cancel edit")
                }
                else {
                    // select previous palette
                    customPalette.release()
                    f.palette = (paletteListAdapter.getItem(paletteListAdapter.selectedPositions[0]))?.t ?: f.palette
                    Log.d("COLOR", "cancel new")
                }
                fsv.requestRender()

                act.uiSetHeight(UiLayoutHeight.TALL)
                customPaletteLayout.hide()
                paletteListLayout.show()
                loadNavButtons(previewListNavButtons)
                updateGradient()

            }, BUTTON_CLICK_DELAY_SHORT)
        }

        customPaletteRandomizeButton.setOnClickListener {
            val newColors = Palette.generateColors(customPalette.colors.size)
            (customColorsDragList.adapter as ColorPaletteDragAdapter).updateColors(newColors)
            customPalette.colors = newColors
            customPalette.updateFlatPalette()
            customPaletteGradient.foreground = customPalette.gradientDrawable
            fsv.requestRender()
        }
        customColorAddButton.setOnClickListener {

            if (customPalette.colors.size == Palette.MAX_CUSTOM_COLORS_FREE && !sc.goldEnabled) act.showUpgradeScreen()
            else { with(customColorsDragList.adapter as ColorPaletteDragAdapter) {
                    if (customPalette.colors.size < Palette.MAX_CUSTOM_COLORS_GOLD) {

                        val newColor = randomColor()
                        customPalette.colors.add(newColor)
                        addItem(itemCount, Pair(nextUniqueId, newColor))
                        // itemList.add(Pair(getNextUniqueId(), newColor))
                        notifyItemInserted(itemCount)

                        customPalette.updateFlatPalette()
                        customPaletteGradient.foreground = customPalette.gradientDrawable
                        fsv.requestRender()

                        when (customPalette.colors.size) {
                            Palette.MAX_CUSTOM_COLORS_FREE -> {
                                if (!sc.goldEnabled) customColorAddButton.showGradient = true
                                customColorRemoveButton.enable()
                            }
                            Palette.MAX_CUSTOM_COLORS_GOLD -> customColorAddButton.disable()
                        }

                    }
                } }

        }
        customColorRemoveButton.setOnClickListener {

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
                    Palette.MAX_CUSTOM_COLORS_GOLD - 1 -> customColorAddButton.enable()
                    Palette.MAX_CUSTOM_COLORS_FREE - 1 -> {
                        if (!sc.goldEnabled) customColorAddButton.showGradient = false
                        customColorRemoveButton.disable()
                    }
                }

            }

        }



        palettePreviewLayout.hide()
        frequencyLayout.hide()
        phaseLayout.hide()
        densityLayout.hide()
        miniColorPickerLayout.hide()
        paletteListLayout.hide()
        customPaletteLayout.hide()
        colorNavBar.hide()
        accentColorLayout.hide()

        accentColor2Button.hide()
        densityButton.hide()

        currentLayout = frequencyLayout
        currentButton = frequencyButton
        showLayout(frequencyLayout)
        alphaButton(frequencyButton)

        updateFrequencyLayout()
        updatePhaseLayout()
        updateDensityLayout()

        // if (sc.goldEnabled) onGoldEnabled()

    }

    override fun updateLayout() {

        colorAutofitButton.isChecked = sc.autofitColorRange
        if (colorAutofitButton.isChecked && f.texture.usesDensity) densityButton.show()
        else densityButton.hide()

        updateGradient()

        (accentColor1Button.compoundDrawables[1] as? GradientDrawable)?.setColor(f.accent1)
        (accentColor2Button.compoundDrawables[1] as? GradientDrawable)?.setColor(f.accent2)
        if (f.texture.usesAccent) accentColor2Button.show() else accentColor2Button.hide()
        if (currentButton == accentColor1Button || currentButton == accentColor2Button) loadAccentColor()

        updateFrequencyLayout()
        updatePhaseLayout()
        updateDensityLayout()

    }

    fun onGoldEnabled() {
        customPaletteNewButton.showGradient = false
        customColorAddButton.showGradient = false
    }

    fun highlightListHeader(adapter: ListAdapter<Palette>, index: Int) {
        adapter.apply {
            listOf(
                    paletteListLayout.listFavoritesButton,
                    paletteListLayout.listCustomButton,
                    paletteListLayout.listDefaultButton
            ).forEachIndexed { i, b ->
                b.setTextColor(resources.getColor(if (index == i) R.color.colorDarkText else R.color.colorDarkTextMuted, null))
            }
        }
    }
    fun loadAccentColor() {
        accentColorPicker.apply {
            val color = when (currentButton) {
                accentColor1Button -> f.accent1
                accentColor2Button -> f.accent2
                else -> Color.RED
            }
            satValueSelector?.loadFrom(color, update = true)
            hueSelector?.progress = color.hue().toInt()
        }
    }
    fun updateGradient() {
        paletteGradient?.foreground = f.palette.gradientDrawable
    }
    fun updateFrequencyLayout() {
        frequencySeekBar?.progress = ((f.frequency/100f).pow(0.5f)*frequencySeekBar.max).toInt()
        frequencyValue?.setText("%.3f".format(f.frequency))
    }
    fun updatePhaseLayout() {
        phaseSeekBar?.progress = (f.phase*phaseSeekBar.max).toInt()
        phaseValue?.setText("%.3f".format(f.phase))
    }
    fun updateDensityLayout() {
        densitySeekBar?.progress = (f.density/3.0*densitySeekBar.max).toInt()
        densityValue?.setText("%.3f".format(f.density))
    }

}