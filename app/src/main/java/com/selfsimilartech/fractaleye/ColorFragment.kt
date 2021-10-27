package com.selfsimilartech.fractaleye

import android.animation.LayoutTransition
import android.content.Context
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.selfsimilartech.fractaleye.databinding.ColorFragmentBinding
import com.woxthebox.draglistview.DragListView
import com.woxthebox.draglistview.DragListView.DragListListener
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IndexOutOfBoundsException
import kotlin.math.*


class ColorFragment : MenuFragment() {

    lateinit var b : ColorFragmentBinding
    
    lateinit var db : AppDatabase
    

    private fun loadNavButtons(views: List<View>) {

        for (i in 0 until b.colorNavBar.childCount) b.colorNavBar.getChildAt(i).hide()
        views.forEach { it.show() }

    }

    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        b = ColorFragmentBinding.inflate(inflater, container, false)
        return b.root
        // return inflater.inflate(R.layout.color_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        super.onViewCreated(v, savedInstanceState)

        db = AppDatabase.getInstance(context ?: act.applicationContext)
        

        if (!sc.goldEnabled) {
            b.customColorAddButton.showGradient = true
            if (Palette.custom.size == Palette.MAX_CUSTOM_PALETTES_FREE) {
                b.customPaletteNewButton.showGradient = true
            }
        }



        b.colorLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        // paletteListLayout.listLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        val handler = Handler(Looper.getMainLooper())
        var customPalette = Palette(name="", colors = arrayListOf())
        var customPaletteListIndex = -1
        var savedCustomName = ""
        var savedCustomColors = arrayListOf<Int>()

        val previewListNavButtons = listOf(
            b.customPaletteNewButton,
            b.paletteListDoneButton
        )
        val customPaletteNavButtons = listOf(
            b.customPaletteCancelButton,
            b.customPaletteDoneButton,
            b.customPaletteRandomizeButton,
            b.customColorRemoveButton,
            b.customColorAddButton
        )
        val nonClickableViewTypes = listOf(
            R.layout.list_header,
            R.layout.list_item_linear_empty_favorite,
            R.layout.list_item_linear_empty_custom
        )


        updateGradient()

        b.frequencyValue.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()?.toFloat()
            if (result != null) {
                f.frequency = result
            }
            w.text = "%.3f".format(f.frequency)
        })
        b.phaseValue.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()?.toFloat()
            if (result != null) {
                f.phase = result
            }
            w.text = "%.3f".format(f.phase)
        })
        b.densityValue.setOnEditorActionListener(editListener(null) {w: TextView ->
            val result = w.text.toString().formatToDouble()?.toFloat()
            if (result != null) {
                f.density = result
            }
            w.text = "%.3f".format(f.density)
        })
        b.customPaletteName.setOnEditorActionListener(editListener(null) { w: TextView ->

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

            b.paletteListLayout.root.hide()
            b.customPaletteLayout.show()
            b.customColorAddButton.show()
            loadNavButtons(customPaletteNavButtons)
            fsv.r.renderProfile = RenderProfile.DISCRETE

            f.palette = item.t
            fsv.requestRender()

            item.t.apply {
                savedCustomName = name
                savedCustomColors = ArrayList(colors)
            }

            customPalette = item.t
            customPaletteListIndex = adapter.getGlobalPositionOf(item)
            val customPaletteAdapter = ColorPaletteDragAdapter(
                    customPalette.colors,
                    R.layout.color_drag_item,
                    R.id.customColorLayout,
                    true
            ) { selectedItemIndex, color ->  // LINK COLOR
                b.customPaletteColorPicker.apply {
                    satValueSelector.activeColorIndex = selectedItemIndex
                    satValueSelector.loadFrom(color, update = true)
                    hueSelector.progress = color.hue().toInt()
                }
            }

            b.customColorsDragList.setAdapter(customPaletteAdapter, true)
            customPaletteAdapter.apply {
                linkColor(0, itemList[0].second)
            }
            b.customPaletteName.setText(customPalette.name)

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
                    .create()
                    .showImmersive(b.root)
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

                    viewLifecycleOwner.lifecycleScope.launch {
                        adapter.removeItemFromCustom(item)
                        Fractal.bookmarks.filter { it.palette == item.t }.forEach {
                            it.palette = Palette.eye
                            db.fractalDao().update(it.customId, it.palette.id, 0)
                        }

                        // item.t.release()
                        val deleteId = item.t.id
                        db.colorPaletteDao().apply { delete(findById(deleteId)) }

                    }

                    adapter.apply {
                        setActivatedPosition(
                                getGlobalPositionOf(getFavoriteItems().getOrNull(0) ?: getDefaultItems()[1])
                        )
                        f.palette = (getItem(activatedPos))?.t ?: Palette.eye
                    }
                    Palette.all.remove(item.t)
                    Palette.custom.remove(item.t)
                    if (!sc.goldEnabled && Palette.custom.size < Palette.MAX_CUSTOM_PALETTES_FREE) {
                        b.customPaletteNewButton.showGradient = false
                    }
                    fsv.requestRender()

                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .showImmersive(b.root)

        }
        val onDuplicatePalette = { adapter: ListAdapter<Palette>, item: ListItem<Palette> ->

            if (!sc.goldEnabled) act.showUpgradeScreen()
            else {

                b.paletteListLayout.root.hide()
                b.customPaletteLayout.show()
                b.customColorAddButton.show()
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
                    b.customPaletteColorPicker.apply {
                        satValueSelector.activeColorIndex = selectedItemIndex
                        satValueSelector.loadFrom(color, update = true)
                        hueSelector.progress = color.hue().toInt()
                    }
                }

                b.customColorsDragList.setAdapter(customPaletteAdapter, true)
                customPaletteAdapter.apply {
                    linkColor(0, itemList[0].second)
                }
                b.customPaletteName.setText(customPalette.name)

            }

        }

        val emptyFavorite = ListItem(Palette.emptyFavorite, ListHeader.FAVORITE, R.layout.list_item_linear_empty_favorite)
        val emptyCustom = ListItem(Palette.emptyCustom, ListHeader.CUSTOM, R.layout.list_item_linear_empty_custom)
        val listItems = arrayListOf<ListItem<Palette>>()



        viewLifecycleOwner.lifecycleScope.launch {

            Log.e("COLOR", "loading palettes...")

            val sp = requireContext().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)

            // load custom palettes
            db.colorPaletteDao().apply {
                getAll().forEach {
                    Palette.custom.add(0, Palette(
                        name = if (it.name == "") resources.getString(R.string.error) else it.name,
                        id = it.id,
                        hasCustomId = true,
                        colors = java.util.ArrayList(
                            arrayListOf(
                                it.c1, it.c2, it.c3,
                                it.c4, it.c5, it.c6,
                                it.c7, it.c8, it.c9,
                                it.c10, it.c11, it.c12
                            ).slice(0 until it.size)
                        ),
                        isFavorite = it.starred
                    ))
                    Palette.custom[0].initialize(resources)
                    Log.d("MAIN", "custom palette ${Palette.custom[0].name}, id: ${Palette.custom[0].id}")
                }
            }
            Palette.all.addAll(0, Palette.custom)
            f.palette = Palette.all.find { it.id == sp.getInt(PALETTE, Palette.night.id) } ?: Palette.eye
            updateGradient()


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
            b.paletteListLayout.apply {
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
                mItemClickListener = FlexibleAdapter.OnItemClickListener { view, position ->

                    if (paletteListAdapter.getItemViewType(position) !in nonClickableViewTypes) {

                        val firstVisiblePos = paletteListLayoutManager.findFirstCompletelyVisibleItemPosition()
                        val lastVisiblePos = paletteListLayoutManager.findLastCompletelyVisibleItemPosition()
                        if (position + 1 > lastVisiblePos) b.paletteListLayout.list.smoothSnapToPosition(position + 1, LinearSmoothScroller.SNAP_TO_END)
                        else if (position - 1 < firstVisiblePos) b.paletteListLayout.list.smoothSnapToPosition(position - 1)

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
                            act.updateCrashKeys()
                            fsv.requestRender()

                        }
                        true //Important!

                    }
                    else false

                }
                mode = SelectableAdapter.Mode.SINGLE
                showAllHeaders()
            }
            b.paletteListLayout.apply {
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

            Log.e("COLOR", "palette load sucessful!")

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



        b.customPaletteColorPicker.apply {

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

                    b.customColorHueEdit.setText("%d".format(h))
                    b.customColorSaturationEdit.setText("%d".format(s))
                    b.customColorValueEdit.setText("%d".format(value))

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
            b.customColorHueEdit.setOnEditorActionListener(editListener(b.customColorSaturationEdit) { w: TextView ->
                val result = w.text.toString().formatToDouble()?.toFloat()
                if (result != null) {
                    hueSelector.progress = result.toInt()
                }
                w.text = "%d".format(satValueSelector.hue.toInt())
            })
            b.customColorSaturationEdit.setOnEditorActionListener(editListener(b.customColorValueEdit) { w: TextView ->
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
            b.customColorValueEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
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

        b.paletteListButton.setOnClickListener {

            crashlytics().updateLastAction(Action.PALETTE_CHANGE)

            // ui changes
            act.hideCategoryButtons()
            act.hideHeaderButtons()
            b.colorSubMenuButtons.hide()
            b.paletteListLayout.root.show()
            b.colorNavBar.show()
            act.hideMenuToggleButton()
            layout.hide()
            loadNavButtons(previewListNavButtons)
            act.uiSetHeight(UiLayoutHeight.TALL)

            getPaletteListAdapter()?.apply {
                (if (selectedPositions.isEmpty()) getFirstPositionOf(f.palette) else activatedPos).let {
                    setActivatedPosition(it)
                    recyclerView?.scrollToPosition(it)
                }
            }


            // color thumbnail render
            handler.postDelayed({

                fsv.r.renderProfile = RenderProfile.COLOR_THUMB
                fsv.r.renderThumbnails = true
                if (!fsv.r.colorThumbsRendered) fsv.r.renderToTex = true
                fsv.requestRender()

            }, BUTTON_CLICK_DELAY_MED)

        }

        b.frequencyButton.setOnClickListener(subMenuButtonListener(b.frequencyLayout, b.frequencyButton))
        b.frequencySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newFreq = (progress.toFloat()/b.frequencySeekBar.max).pow(2f) * 100f
                    f.frequency = newFreq
                    Log.e("COLOR", "frequency set to $newFreq")
                    b.frequencyValue.setText("%.3f".format(newFreq))
                    fsv.requestRender()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })

        b.phaseButton.setOnClickListener(subMenuButtonListener(b.phaseLayout, b.phaseButton))
        b.phaseSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newPhase = progress.toFloat() / b.phaseSeekBar.max
                    f.phase = newPhase
                    b.phaseValue.setText("%.3f".format(newPhase))
                    fsv.requestRender()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })

        b.densityButton.setOnClickListener(subMenuButtonListener(b.densityLayout, b.densityButton))
        b.densitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newDensity = 3f * progress.toFloat() / b.densitySeekBar.max
                    f.density = newDensity
                    b.densityValue.setText("%.3f".format(newDensity))
                    fsv.requestRender()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        b.colorAutofitButton.setOnClickListener {

            sc.autofitColorRange = b.colorAutofitButton.isChecked
            fsv.r.autofitColorSelected = b.colorAutofitButton.isChecked
            fsv.r.calcNewTextureSpan = true
            if (b.colorAutofitButton.isChecked) {

                if (f.texture.usesDensity) {
                    b.densityButton.show()
                    b.densityButton.performClick()
                }
                fsv.r.renderToTex = true

            }
            else {

                f.density = 0f
                b.densityButton.hide()
                b.frequencyButton.performClick()

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
        b.colorAutofitButton.isChecked = sc.autofitColorRange
        if (b.colorAutofitButton.isChecked && f.texture.usesDensity) b.densityButton.show() else b.densityButton.hide()


        b.accentColor1Button.setOnClickListener {
            subMenuButtonListener(b.miniColorPickerLayout, b.accentColor1Button, UiLayoutHeight.MED).onClick(it)
            loadAccentColor()
        }
        b.accentColor2Button.setOnClickListener {
            subMenuButtonListener(b.miniColorPickerLayout, b.accentColor2Button, UiLayoutHeight.MED).onClick(it)
            loadAccentColor()
        }

        (b.accentColor1Button.compoundDrawables[1] as? GradientDrawable)?.setColor(f.accent1)
        (b.accentColor2Button.compoundDrawables[1] as? GradientDrawable)?.setColor(f.accent2)

        b.accentColorPicker.apply {
            satValueSelector.onUpdateActiveColor = { c ->
                b.accentColor?.setBackgroundColor(c)
                when (button) {
                    b.accentColor1Button -> f.accent1 = c
                    b.accentColor2Button -> f.accent2 = c
                }
                satValueSelector.invalidate()
                (button.compoundDrawables[1] as? GradientDrawable)?.setColor(c)
                fsv.requestRender()
            }
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
        }

        b.paletteListDoneButton.setOnClickListener {

            b.paletteGradient.foreground = f.palette.gradientDrawable
            act.uiSetHeight(UiLayoutHeight.SHORT)
            b.colorSubMenuButtons.show()
            b.colorNavBar.hide()
            b.paletteListLayout.root.hide()

            setCurrentLayout(b.frequencyLayout)
            setCurrentButton(b.frequencyButton)
            act.apply {
                showCategoryButtons()
                showMenuToggleButton()
                showHeaderButtons()
            }

            fsv.r.renderProfile = RenderProfile.DISCRETE

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

        b.customPaletteNewButton.setOnClickListener {
            if (Palette.custom.size == Palette.MAX_CUSTOM_PALETTES_FREE && !sc.goldEnabled) act.showUpgradeScreen()
            else {
                crashlytics().updateLastAction(Action.PALETTE_CREATE)

                b.paletteListLayout.root.hide()
                b.customPaletteLayout.show()
                b.customColorAddButton.show()
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
                    b.customPaletteColorPicker.apply {
                        satValueSelector.activeColorIndex = selectedItemIndex
                        satValueSelector.loadFrom(color, update = true)
                        hueSelector.progress = color.hue().toInt()
                    }
                }



                customColorsDragList.setAdapter(customPaletteAdapter, true)
                customPaletteAdapter.apply {
                    linkColor(0, itemList[0].second)
                }
                b.customPaletteName.setText(customPalette.name)

            }
        }
        b.customPaletteDoneButton.setOnClickListener {
            when {
                Palette.all.any {
                    if (customPalette.name == it.name) {
                        if (customPalette.hasCustomId) customPalette.id != it.id
                        else true
                    } else false
                } -> {
                    act.showMessage(resources.getString(R.string.msg_custom_name_duplicate).format(
                        resources.getString(R.string.palette)
                    ))
                }
                customPalette.name == "" -> {
                    act.showMessage(resources.getString(R.string.msg_empty_name))
                }
                else -> {

                    if (customPalette.hasCustomId) {
                        // update existing palette in database
                        viewLifecycleOwner.lifecycleScope.launch {
                            db.colorPaletteDao().apply {
                                update(customPalette.toDatabaseEntity())
                            }
                            getPaletteListAdapter()?.notifyItemChanged(customPaletteListIndex)
                            // paletteListAdapter.getActivatedItem()?.run { paletteListAdapter.updateItem(this) }
                        }
                    }
                    else {

                        // add new palette to database
                        GlobalScope.launch {
                            db.colorPaletteDao().apply {
                                customPalette.id = insert(customPalette.toDatabaseEntity()).toInt()
                                customPalette.hasCustomId = true
                            }
                            getPaletteListAdapter()?.apply {
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
                    if (Palette.custom.size == Palette.MAX_CUSTOM_PALETTES_FREE && !sc.goldEnabled) {
                        b.customPaletteNewButton.showGradient = true
                    }
                    act.uiSetHeight(UiLayoutHeight.TALL)

                    b.customPaletteLayout.hide()
                    b.paletteListLayout.root.show()
                    // palettePreviewLayout.show()
                    // colorSubMenuButtons.show()
                    loadNavButtons(previewListNavButtons)
                    // colorNavBar.hide()
                    // act.showCategoryButtons()

                }
            }
        }
        b.customPaletteCancelButton.setOnClickListener {

            if (customPalette.hasCustomId) {
                // revert changes
                customPalette.apply {
                    name = savedCustomName
                    colors = ArrayList(savedCustomColors)
                    updateFlatPalette()
                }
                getPaletteListAdapter()?.getActivatedItem()?.run { getPaletteListAdapter()?.updateItem(this) }
                Log.d("COLOR", "cancel edit")
            }
            else {
                // select previous palette
                customPalette.release()
                getPaletteListAdapter()?.let {
                    f.palette = it.getItem(it.selectedPositions[0])?.t ?: f.palette
                }
                Log.d("COLOR", "cancel new")
            }
            fsv.requestRender()

            act.uiSetHeight(UiLayoutHeight.TALL)
            b.customPaletteLayout.hide()
            b.paletteListLayout.root.show()
            loadNavButtons(previewListNavButtons)
            updateGradient()

        }

        b.customPaletteRandomizeButton.setOnClickListener {
            val newColors = Palette.generateColors(customPalette.colors.size)
            (customColorsDragList.adapter as ColorPaletteDragAdapter).updateColors(newColors)
            customPalette.colors = newColors
            customPalette.updateFlatPalette()
            b.customPaletteGradient.foreground = customPalette.gradientDrawable
            fsv.requestRender()
        }
        b.customColorAddButton.setOnClickListener {

            if (customPalette.colors.size == Palette.MAX_CUSTOM_COLORS_FREE && !sc.goldEnabled) act.showUpgradeScreen()
            else { with(customColorsDragList.adapter as ColorPaletteDragAdapter) {
                    if (customPalette.colors.size < Palette.MAX_CUSTOM_COLORS_GOLD) {

                        val newColor = randomColor()
                        customPalette.colors.add(newColor)
                        addItem(itemCount, Pair(nextUniqueId, newColor))
                        // itemList.add(Pair(getNextUniqueId(), newColor))
                        notifyItemInserted(itemCount)

                        customPalette.updateFlatPalette()
                        b.customPaletteGradient.foreground = customPalette.gradientDrawable
                        fsv.requestRender()

                        when (customPalette.colors.size) {
                            Palette.MAX_CUSTOM_COLORS_FREE -> {
                                if (!sc.goldEnabled) b.customColorAddButton.showGradient = true
                                b.customColorRemoveButton.enable()
                            }
                            Palette.MAX_CUSTOM_COLORS_GOLD -> b.customColorAddButton.disable()
                        }

                    }
                } }

        }
        b.customColorRemoveButton.setOnClickListener {

            with (customColorsDragList.adapter as ColorPaletteDragAdapter) {

                customPalette.colors.removeAt(selectedItemIndex)
                removeItem(selectedItemIndex)
                // itemList.removeAt(selectedItemIndex)
                if (selectedItemIndex == itemList.size) selectedItemIndex--
                notifyDataSetChanged()

                customPalette.updateFlatPalette()
                b.customPaletteGradient.foreground = customPalette.gradientDrawable
                fsv.requestRender()

                when (customPalette.colors.size) {
                    Palette.MAX_CUSTOM_COLORS_GOLD - 1 -> b.customColorAddButton.enable()
                    Palette.MAX_CUSTOM_COLORS_FREE - 1 -> {
                        if (!sc.goldEnabled) b.customColorAddButton.showGradient = false
                        b.customColorRemoveButton.disable()
                    }
                }

            }

        }



        b.frequencyLayout.hide()
        b.phaseLayout.hide()
        b.densityLayout.hide()
        b.miniColorPickerLayout.hide()
        b.paletteListLayout.root.hide()
        b.customPaletteLayout.hide()
        b.colorNavBar.hide()

        b.accentColor2Button.hide()
        b.densityButton.hide()

        layout = b.frequencyLayout
        button = b.frequencyButton
        setCurrentLayout(b.frequencyLayout)
        setCurrentButton(b.frequencyButton)

        updateLayout()
        crashlytics().setCustomKey(CRASH_KEY_FRAG_COLOR_CREATED, true)

    }

    override fun updateLayout() {

        b.colorAutofitButton.isChecked = sc.autofitColorRange
        if (b.colorAutofitButton.isChecked && f.texture.usesDensity) b.densityButton.show()
        else b.densityButton.hide()

        updateGradient()
        (b.accentColor1Button.compoundDrawables[1] as? GradientDrawable)?.setColor(f.accent1)
        (b.accentColor2Button.compoundDrawables[1] as? GradientDrawable)?.setColor(f.accent2)
        if (f.texture.usesAccent) b.accentColor2Button.show() else b.accentColor2Button.hide()
        if (button == b.accentColor1Button || button == b.accentColor2Button) loadAccentColor()

        updateFrequencyLayout()
        updatePhaseLayout()
        updateDensityLayout()

    }

    override fun updateValues() {
        b.frequencyValue.setText("%.3f".format(f.frequency))
        b.phaseValue.setText("%.3f".format(f.phase))
        b.densityValue.setText("%.3f".format(f.density))
    }

    override fun onGoldEnabled() {
        b.customPaletteNewButton.showGradient = false
        b.customColorAddButton.showGradient = false
    }

    fun getPaletteListAdapter() : ListAdapter<Palette>? {
        return b.paletteListLayout.list.adapter as? ListAdapter<Palette>
    }

    fun highlightListHeader(adapter: ListAdapter<Palette>, index: Int) {
        adapter.apply {
            listOf(
                b.paletteListLayout.listFavoritesButton,
                b.paletteListLayout.listCustomButton,
                b.paletteListLayout.listDefaultButton
            ).forEachIndexed { i, b ->
                b.setTextColor(resources.getColor(if (index == i) R.color.colorDarkText else R.color.colorDarkTextMuted, null))
            }
        }
    }

    fun loadAccentColor() {
        b.accentColorPicker.apply {
            val color = when (button) {
                b.accentColor1Button -> f.accent1
                b.accentColor2Button -> f.accent2
                else -> Color.RED
            }
            satValueSelector.loadFrom(color, update = true)
            hueSelector.progress = color.hue().toInt()
        }
    }

    fun updateGradient() {
        b.paletteGradient.foreground = f.palette.gradientDrawable
    }

    fun updateFrequencyLayout() {
        b.frequencySeekBar.progress = ((f.frequency/100f).pow(0.5f)*b.frequencySeekBar.max).toInt()
        b.frequencyValue.setText("%.3f".format(f.frequency))
    }

    fun updatePhaseLayout() {
        b.phaseSeekBar.progress = (f.phase*b.phaseSeekBar.max).toInt()
        b.phaseValue.setText("%.3f".format(f.phase))
    }

    fun updateDensityLayout() {
        b.densitySeekBar.progress = (f.density/3.0*b.densitySeekBar.max).toInt()
        b.densityValue.setText("%.3f".format(f.density))
    }

}