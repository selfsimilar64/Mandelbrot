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
import com.selfsimilartech.fractaleye.databinding.FragmentColorBinding
import com.woxthebox.draglistview.DragListView
import com.woxthebox.draglistview.DragListView.DragListListener
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IndexOutOfBoundsException
import kotlin.math.*


class ColorFragment : MenuFragment() {

    lateinit var b : FragmentColorBinding
    
    lateinit var db : AppDatabase
    

    private fun loadNavButtons(views: List<View>) {

        for (i in 0 until b.colorNavBar.childCount) b.colorNavBar.getChildAt(i).hide()
        views.forEach { it.show() }

    }

    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        b = FragmentColorBinding.inflate(inflater, container, false)
        return b.root
        // return inflater.inflate(R.layout.color_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        super.onViewCreated(v, savedInstanceState)

        db = AppDatabase.getInstance(context ?: act.applicationContext)
        

        if (!sc.goldEnabled) {
            b.addColorButton.showGradient = true
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
            b.removeColorButton,
            b.addColorButton
        )
        val nonClickableViewTypes = listOf(
            R.layout.list_header,
            R.layout.list_item_linear_empty_favorite,
            R.layout.list_item_linear_empty_custom
        )


        updateGradient()

        b.frequencyValue.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()
            if (result != null) {
                f.color.frequency = result
            }
            w.text = "%.3f".format(f.color.frequency)
        })
        b.phaseValue.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()
            if (result != null) {
                f.color.phase = result
            }
            w.text = "%.3f".format(f.color.phase)
        })
        b.densityValue.setOnEditorActionListener(editListener(null) {w: TextView ->
            val result = w.text.toString().formatToDouble()
            if (result != null) {
                f.color.density = result
            }
            w.text = "%.3f".format(f.color.density)
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
            b.addColorButton.show()
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
            val customPaletteAdapter = CustomColorDragAdapter(
                    customPalette.colors,
                    R.layout.color_drag_item,
                    R.id.colorView,
                    true
            ) { selectedItemIndex, color ->  // LINK COLOR
                b.colorSelector.apply {
                    satValueSelector.linkedColorIndex = selectedItemIndex
                    satValueSelector.setColor(color)
//                    hueSelector.hue = color.hue().toInt()
                }
            }

            b.colorList.setAdapter(customPaletteAdapter, true)
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
                        setActivatedPosition(0)
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
                b.addColorButton.show()
                loadNavButtons(customPaletteNavButtons)
                fsv.r.renderProfile = RenderProfile.DISCRETE

                customPalette = item.t.clone(resources)

                f.palette = customPalette
                fsv.requestRender()


                val customPaletteAdapter = CustomColorDragAdapter(
                        customPalette.colors,
                        R.layout.color_drag_item,
                        R.id.colorView,
                        true
                ) { selectedItemIndex, color ->  // LINK COLOR
                    b.colorSelector.apply {
                        satValueSelector.linkedColorIndex = selectedItemIndex
                        satValueSelector.setColor(color)
//                        hueSelector.progress = color.hue().toInt()
                    }
                }

                b.colorList.setAdapter(customPaletteAdapter, true)
                customPaletteAdapter.apply {
                    linkColor(0, itemList[0].second)
                }
                b.customPaletteName.setText(customPalette.name)

            }

        }

        val emptyFavorite = ListItem(Palette.emptyFavorite, ListItemType.FAVORITE, R.layout.list_item_linear_empty_favorite)
        val emptyCustom = ListItem(Palette.emptyCustom, ListItemType.CUSTOM, R.layout.list_item_linear_empty_custom)
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
                    if (it.hasCustomId || it == Palette.emptyCustom) ListItemType.CUSTOM else ListItemType.DEFAULT,
                    R.layout.palette_list_item,

                    ))
            }
            if (Palette.all.none { it.isFavorite }) listItems.add(emptyFavorite)
            if (Palette.custom.isEmpty()) listItems.add(emptyCustom)

            val paletteListAdapter = ListAdapter(
                listItems,
                onEditCustomPalette,
                onDeleteCustomPalette,
                onDuplicatePalette,
                emptyFavorite,
                emptyCustom
            )
            b.paletteListLayout.apply {
                defaultList.adapter = paletteListAdapter
                defaultList.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                    val firstVisiblePos = paletteListLayoutManager.findFirstCompletelyVisibleItemPosition()
                    highlightListItemType(paletteListAdapter, when {
                        firstVisiblePos < paletteListAdapter.getGlobalPositionOf(paletteListAdapter.headerItems[1]) -> 0
                        firstVisiblePos < paletteListAdapter.getGlobalPositionOf(paletteListAdapter.headerItems[2]) -> 1
                        else -> 2
                    })
                }
                defaultList.layoutManager = paletteListLayoutManager
                defaultList.setHasFixedSize(true)
            }
            paletteListAdapter.apply {
                mItemClickListener = FlexibleAdapter.OnItemClickListener { view, position ->

                    if (paletteListAdapter.getItemViewType(position) !in nonClickableViewTypes) {

                        val firstVisiblePos = paletteListLayoutManager.findFirstCompletelyVisibleItemPosition()
                        val lastVisiblePos = paletteListLayoutManager.findLastCompletelyVisibleItemPosition()

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


            Log.e("COLOR", "palette load sucessful!")

        }

//        colorPreviewListGridManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
//            override fun getSpanSize(position: Int): Int {
//                return if (paletteListAdapter.getItemViewType(position) in nonClickableViewTypes) spanCount else 1
//            }
//        }




        // CUSTOM COLORS DRAG LIST
        val colorList = v.findViewById<DragListView>(R.id.colorList)
        colorList.setDragListListener(object : DragListListener {
            override fun onItemDragStarted(position: Int) {}
            override fun onItemDragging(itemPosition: Int, x: Float, y: Float) {}
            override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
                if (fromPosition != toPosition) {

                    customPalette.colors.add(toPosition, customPalette.colors.removeAt(fromPosition))
                    customPalette.updateFlatPalette()
                    with (colorList.adapter as CustomColorDragAdapter) {
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
        colorList.setLayoutManager(LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false))



        b.colorSelector.apply {

            // SATURATION-VALUE SELECTOR
            satValueSelector.apply {
                onUpdateLinkedColor = { newColor: Int ->  // UPDATE ACTIVE COLOR

                    customPalette.colors[linkedColorIndex] = newColor
                    customPalette.updateFlatPalette()
                    // customPaletteGradient.foreground = customPalette.gradientDrawable
                    (colorList.adapter as CustomColorDragAdapter).updateColor(linkedColorIndex, newColor)

                    val hsv = newColor.toHSV()
//                    val h = hue.let { if (it.isNaN()) 0 else it.roundToInt() }
//                    val s = (100f * hsv[1]).let { if (it.isNaN()) 0 else it.roundToInt() }
//                    val value = (100f * hsv[2]).let { if (it.isNaN()) 0 else it.roundToInt()}

//                    b.customColorHueEdit.setText("%d".format(h))
//                    b.customColorSaturationEdit.setText("%d".format(s))
//                    b.customColorValueEdit.setText("%d".format(value))

                    invalidate()
                    fsv.requestRender()

                }
            }

            // HUE SELECTOR
//            hueSelectorBackground.background = GradientDrawable(
//                    GradientDrawable.Orientation.BOTTOM_TOP, resources.getIntArray(R.array.hueslider)
//            )
//            hueSelector.max = 359
//            hueSelector.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                    satValueSelector.hue = progress.toFloat()
//                }
//                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
//                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
//
//            })

            // HSV EDIT TEXTS
            b.customColorHueEdit.setOnEditorActionListener(editListener(b.customColorSaturationEdit) { w: TextView ->
                val result = w.text.toString().formatToDouble()?.toFloat()
                if (result != null) {
//                    hueSelector.progress = result.toInt()
                }
//                w.text = "%d".format(satValueSelector.hue.toInt())
            })
            b.customColorSaturationEdit.setOnEditorActionListener(editListener(b.customColorValueEdit) { w: TextView ->
                val result = w.text.toString().formatToDouble()?.toFloat()
                if (result != null) {
//                    satValueSelector.setColor(Color.HSVToColor(floatArrayOf(
//                            satValueSelector.hue,
//                            result / 100f,
//                            satValueSelector.value
//                    )))
                }
//                val s = (100f * satValueSelector.sat).let { if (it.isNaN()) 0 else it.roundToInt() }
//                w.text = "%d".format(s)
            })
            b.customColorValueEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
                val result = w.text.toString().formatToDouble()?.toFloat()
                if (result != null) {
//                    satValueSelector.setColor(Color.HSVToColor(floatArrayOf(
//                            satValueSelector.hue,
//                            satValueSelector.sat,
//                            result / 100f
//                    )))
                }
//                val value = (100f * satValueSelector.value).let { if (it.isNaN()) 0 else it.roundToInt() }
//                w.text = "%d".format(value)
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

            getPaletteListAdapter()?.apply {
                (if (selectedPositions.isEmpty()) getFirstPositionOf(f.palette) else activatedPos).let {
                    setActivatedPosition(it)
                    recyclerView?.scrollToPosition(it)
                }
            }


            // color thumbnail render
            handler.postDelayed({

                fsv.r.renderProfile = RenderProfile.COLOR_THUMB
                fsv.r.renderAllThumbnails = true
                fsv.requestRender()

            }, BUTTON_CLICK_DELAY_MED)

        }

        b.frequencyButton.setOnClickListener(subMenuButtonListener(b.frequencyLayout, b.frequencyButton))
        b.frequencySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newFreq = (progress.toDouble()/b.frequencySeekBar.max).pow(2.0) * 100.0
                    f.color.frequency = newFreq
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
                    val newPhase = progress.toDouble() / b.phaseSeekBar.max
                    f.color.phase = newPhase
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
                    val newDensity = 3.0 * progress.toDouble() / b.densitySeekBar.max
                    f.color.density = newDensity
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

                f.color.density = 0.0
                b.densityButton.hide()
                b.frequencyButton.performClick()

                // adjust frequency and phase to match old fit

                val M = fsv.r.textureSpan.max()
                val m = fsv.r.textureSpan.min()
                val L = M - m
                val prevFreq = f.color.frequency
                val prevPhase = f.color.phase

                fsv.r.setTextureSpan(0f, 1f)

                f.color.frequency = prevFreq/L
                f.color.phase = prevPhase - prevFreq*m/L
                Log.e("COLOR", "frequency set ${f.color.frequency}")

            }
            updateFrequencyLayout()
            updatePhaseLayout()
            fsv.requestRender()

        }
        b.colorAutofitButton.isChecked = sc.autofitColorRange
        if (b.colorAutofitButton.isChecked && f.texture.usesDensity) b.densityButton.show() else b.densityButton.hide()


        b.fillColorButton.setOnClickListener {
            subMenuButtonListener(b.miniColorPickerLayout, b.fillColorButton, UiLayoutHeight.MED).onClick(it)
            loadAccentColor()
        }
        b.outlineColorButton.setOnClickListener {
            subMenuButtonListener(b.miniColorPickerLayout, b.outlineColorButton, UiLayoutHeight.MED).onClick(it)
            loadAccentColor()
        }

        (b.fillColorButton.compoundDrawables[1] as? GradientDrawable)?.setColor(f.color.fillColor)
        (b.outlineColorButton.compoundDrawables[1] as? GradientDrawable)?.setColor(f.color.outlineColor)

        b.accentColorPicker.apply {
            satValueSelector.onUpdateLinkedColor = { c ->
                b.accentColor?.setBackgroundColor(c)
                when (button) {
                    b.fillColorButton -> f.color.fillColor = c
                    b.outlineColorButton -> f.color.outlineColor = c
                }
                satValueSelector.invalidate()
                (button.compoundDrawables[1] as? GradientDrawable)?.setColor(c)
                fsv.requestRender()
            }
//            hueSelectorBackground.background = GradientDrawable(
//                    GradientDrawable.Orientation.BOTTOM_TOP, resources.getIntArray(R.array.hueslider)
//            )
//            hueSelector.max = 359
//            hueSelector.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                    satValueSelector.hue = progress.toFloat()
//                }
//                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
//                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
//            })
        }

        b.paletteListDoneButton.setOnClickListener {

            b.paletteGradient.foreground = f.palette.gradientDrawable
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
                b.addColorButton.show()
                loadNavButtons(customPaletteNavButtons)
                fsv.r.renderProfile = RenderProfile.DISCRETE

                customPalette = Palette(
                        name = "%s %s %d".format(
                                resources.getString(R.string.header_custom),
                                resources.getString(R.string.palette),
                                Palette.nextCustomPaletteNum
                        ),
                        colors = Palette.generateSequentialColors(if (sc.goldEnabled) 5 else 3)
                ).apply { initialize(resources) }

                // ColorPalette.all.add(0, customPalette)
                f.palette = customPalette
                fsv.requestRender()


                val customPaletteAdapter = CustomColorDragAdapter(
                        customPalette.colors,
                        R.layout.color_drag_item,
                        R.id.colorView,
                        true
                ) { selectedItemIndex, color ->  // LINK COLOR
                    b.colorSelector.apply {
                        satValueSelector.linkedColorIndex = selectedItemIndex
                        satValueSelector.setColor(color)
//                        hueSelector.progress = color.hue().toInt()
                    }
                }



                colorList.setAdapter(customPaletteAdapter, true)
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
                                val item = ListItem(customPalette, ListItemType.CUSTOM, R.layout.palette_list_item)
                                setActivatedPosition(addItemToCustom(item, 0))
                            }
                            Palette.nextCustomPaletteNum++
                        }

                        Palette.all.add(0, customPalette)
                        Palette.custom.add(0, customPalette)

                        if (sc.colorListViewType == ListLayoutType.GRID) {
                            fsv.r.renderProfile = RenderProfile.COLOR_THUMB
                            fsv.r.renderAllThumbnails = true
                            fsv.requestRender()
                        }

                    }

                    // update ui
                    if (Palette.custom.size == Palette.MAX_CUSTOM_PALETTES_FREE && !sc.goldEnabled) {
                        b.customPaletteNewButton.showGradient = true
                    }

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

            b.customPaletteLayout.hide()
            b.paletteListLayout.root.show()
            loadNavButtons(previewListNavButtons)
            updateGradient()

        }

        b.customPaletteRandomizeButton.setOnClickListener {
            val newColors = Palette.generateColors(customPalette.colors.size)
            (colorList.adapter as CustomColorDragAdapter).updateColors(newColors)
            customPalette.colors = newColors
            customPalette.updateFlatPalette()
            b.customPaletteGradient.foreground = customPalette.gradientDrawable
            fsv.requestRender()
        }
        b.addColorButton.setOnClickListener {

            if (customPalette.colors.size == Palette.MAX_CUSTOM_COLORS_FREE && !sc.goldEnabled) act.showUpgradeScreen()
            else { with(colorList.adapter as CustomColorDragAdapter) {
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
                                if (!sc.goldEnabled) b.addColorButton.showGradient = true
                                b.removeColorButton.enable()
                            }
                            Palette.MAX_CUSTOM_COLORS_GOLD -> b.addColorButton.disable()
                        }

                    }
                } }

        }
        b.removeColorButton.setOnClickListener {

            with (colorList.adapter as CustomColorDragAdapter) {

                customPalette.colors.removeAt(selectedItemIndex)
                removeItem(selectedItemIndex)
                // itemList.removeAt(selectedItemIndex)
                if (selectedItemIndex == itemList.size) selectedItemIndex--
                notifyDataSetChanged()

                customPalette.updateFlatPalette()
                b.customPaletteGradient.foreground = customPalette.gradientDrawable
                fsv.requestRender()

                when (customPalette.colors.size) {
                    Palette.MAX_CUSTOM_COLORS_GOLD - 1 -> b.addColorButton.enable()
                    Palette.MAX_CUSTOM_COLORS_FREE - 1 -> {
                        if (!sc.goldEnabled) b.addColorButton.showGradient = false
                        b.removeColorButton.disable()
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

        b.outlineColorButton.hide()
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
        (b.fillColorButton.compoundDrawables[1] as? GradientDrawable)?.setColor(f.color.fillColor)
        (b.outlineColorButton.compoundDrawables[1] as? GradientDrawable)?.setColor(f.color.outlineColor)
        if (f.texture.usesAccent) b.outlineColorButton.show() else b.outlineColorButton.hide()
        if (button == b.fillColorButton || button == b.outlineColorButton) loadAccentColor()

        updateFrequencyLayout()
        updatePhaseLayout()
        updateDensityLayout()

    }

    override fun updateValues() {
        b.frequencyValue.setText("%.3f".format(f.color.frequency))
        b.phaseValue.setText("%.3f".format(f.color.phase))
        b.densityValue.setText("%.3f".format(f.color.density))
    }

    override fun onGoldEnabled() {
        b.customPaletteNewButton.showGradient = false
        b.addColorButton.showGradient = false
    }

    fun getPaletteListAdapter() : ListAdapter<Palette>? {
        return b.paletteListLayout.defaultList.adapter as? ListAdapter<Palette>
    }

    fun highlightListItemType(adapter: ListAdapter<Palette>, index: Int) {
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
                b.fillColorButton -> f.color.fillColor
                b.outlineColorButton -> f.color.outlineColor
                else -> Color.RED
            }
            satValueSelector.setColor(color)
//            hueSelector.progress = color.hue().toInt()
        }
    }

    fun updateGradient() {
        b.paletteGradient.foreground = f.palette.gradientDrawable
    }

    fun updateFrequencyLayout() {
        b.frequencySeekBar.progress = ((f.color.frequency/100.0).pow(0.5)*b.frequencySeekBar.max).toInt()
        b.frequencyValue.setText("%.3f".format(f.color.frequency))
    }

    fun updatePhaseLayout() {
        b.phaseSeekBar.progress = (f.color.phase*b.phaseSeekBar.max).toInt()
        b.phaseValue.setText("%.3f".format(f.color.phase))
    }

    fun updateDensityLayout() {
        b.densitySeekBar.progress = (f.color.density/3.0*b.densitySeekBar.max).toInt()
        b.densityValue.setText("%.3f".format(f.color.density))
    }

}