package com.selfsimilartech.fractaleye

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import com.selfsimilartech.fractaleye.databinding.FragmentPositionBinding
import java.util.*
import eu.davidea.flexibleadapter.FlexibleAdapter
import kotlinx.coroutines.launch
import java.lang.IndexOutOfBoundsException


class PositionFragment : MenuFragment() {

    lateinit var b : FragmentPositionBinding
    
    lateinit var db : AppDatabase
    

    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        b = FragmentPositionBinding.inflate(inflater, container, false)
        return b.root
        // return inflater.inflate(R.layout.position_fragment, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        
        db = AppDatabase.getInstance(context ?: act.applicationContext)


        val nonClickableViewTypes = listOf(
            R.layout.list_header,
            R.layout.list_item_linear_empty_favorite,
            R.layout.list_item_linear_empty_custom
        )

        val handler = Handler(Looper.getMainLooper())


//        b.fitToViewportButton.setOnClickListener {
//
//            // if (!fitToViewportButton.isChecked && sc.aspectRatio != AspectRatio.RATIO_SCREEN) aspectDefaultButton.performClick()
//
//            sc.fitToViewport = b.fitToViewportButton.isChecked
//            act.updateFractalLayout()
//
//        }
//        b.fitToViewportButton.isChecked = sc.fitToViewport


        b.resolutionBar.showGradient = true
        b.resolutionBar.gradientStartProgress = Resolution.NUM_VALUES_FREE - 1
        b.resolutionBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            var prevProgress = 0

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Log.e("SETTINGS", "progress: $progress")
                updateResolutionText(Resolution.foregrounds[progress], sc.aspectRatio)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                prevProgress = b.resolutionBar.progress
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                val newRes = Resolution.foregrounds[b.resolutionBar.progress]

                if (sc.resolution != newRes) {

                    crashlytics().updateLastAction(Action.RESOLUTION_CHANGE)

                    if (b.resolutionBar.progress > b.resolutionBar.gradientStartProgress && !sc.goldEnabled) {
                        b.resolutionBar.progress = prevProgress
                        act.showUpgradeScreen()
                    }
                    else {

                        // save state on resolution increase
                        if (newRes.w > sc.resolution.w) act.bookmarkAsPreviousFractal()

                        sc.resolution = newRes
                        crashlytics().setCustomKey(CRASH_KEY_RESOLUTION, sc.resolution.toString())
                        if (fsv.r.isRendering) fsv.r.interruptRender = true
                        fsv.r.fgResolutionChanged = true
                        fsv.r.renderToTex = true
                        fsv.requestRender()

                    }

                }

            }

        })
        b.resolutionBar.max = Resolution.NUM_VALUES_WORKING() - 1
        //Log.e("SETTINGS", "resolution ordinal: ${sc.resolution.ordinal}")
        b.resolutionBar.progress = Resolution.foregrounds.indexOf(sc.resolution)
        updateResolutionText(sc.resolution, sc.aspectRatio)



        val listLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val onEditCustomBookmark = { adapter: ListAdapter<Fractal>, item: ListItem<Fractal> ->

            Fractal.tempBookmark1 = item.t
            act.showBookmarkDialog(item.t, edit = true)

        }
        val onDeleteCustomBookmark = { adapter: ListAdapter<Fractal>, item: ListItem<Fractal> ->

            val dialog = AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom)
                .setTitle("${resources.getString(R.string.delete)} ${item.t.name}?")
                .setIcon(R.drawable.warning)
                .setPositiveButton(android.R.string.ok) { dialog, whichButton ->

                    adapter.removeItemFromCustom(item)
                    val deleteId = item.t.customId

                    viewLifecycleOwner.lifecycleScope.launch {
                        db.fractalDao().apply {
                            delete(findById(deleteId))
                        }
                    }

                    requireContext().deleteFile(item.t.thumbnailPath)

                    Fractal.all.remove(item.t)
                    Fractal.bookmarks.remove(item.t)

                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .showImmersive(b.root)

        }
        val onDuplicateBookmark = { adapter: ListAdapter<Fractal>, item: ListItem<Fractal> ->

        }


        var firstBookmarkSelection = true

        val emptyFavorite = ListItem(Fractal.emptyFavorite, ListItemType.FAVORITE, R.layout.list_item_linear_empty_favorite)
        val emptyCustom = ListItem(Fractal.emptyCustom, ListItemType.CUSTOM, R.layout.list_item_linear_empty_custom)
        val listItems = arrayListOf<ListItem<Fractal>>()



        viewLifecycleOwner.lifecycleScope.launch {

            Log.e("POSITION", "loading bookmarks...")

            // load custom bookmarks
            db.fractalDao().apply {

                if (!act.previousFractalCreated || act.previousFractalId == -1) {
                    Log.d("MAIN", "previousFractal not created")
                    Fractal.previous.customId = insert(Fractal.previous.toDatabaseEntity()).toInt()
                    act.previousFractalId = Fractal.previous.customId
                    val edit = act.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE).edit()
                    edit.putInt(PREV_FRACTAL_ID, Fractal.previous.customId)
                    edit.putBoolean(PREV_FRACTAL_CREATED, true)
                    edit.apply()
                }

                getAll().forEach {

                    val shapeParams = if (listOfNotNull(it.julia, it.seed, it.p1, it.p2, it.p3, it.p4).isNotEmpty()) Shape.ParamSet(
                        ArrayList(listOfNotNull(it.p1, it.p2, it.p3, it.p4).map { p -> if (p.isComplex) ComplexParam(p) else RealParam(p) }),
                        julia = if (it.julia != null) ComplexParam(it.julia) else ComplexParam(),
                        seed = if (it.seed != null) ComplexParam(it.seed) else ComplexParam()
                    ) else null

                    val textureParams = null

                    val thumbnail = try {
                        val inputStream = requireContext().openFileInput(it.thumbnailPath)
                        BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }).let { bmp ->
                            inputStream?.close()
                            bmp
                        }
                    } catch (e: Exception) {
                        Log.e("MAIN", "thumnail path invalid")
                        BitmapFactory.decodeResource(resources, R.drawable.mandelbrot_icon)
                    }

                    val newBookmark = Fractal(

                        name = if (it.name == "") resources.getString(R.string.error) else it.name,
                        isFavorite = it.isFavorite,
                        thumbnailPath = it.thumbnailPath,
                        thumbnail = thumbnail,
                        customId = it.id,
                        goldFeature = false,

                        shapeId = it.shapeId,
                        juliaMode = it.juliaMode,
                        shapeParams = shapeParams,
                        position = if (it.position != null) Position(it.position) else null,

                        textureId = it.textureId,
                        textureRegion = TextureRegion.values()[it.textureMode],
                        textureMin = it.textureMin,
                        textureMax = it.textureMax,
                        textureParams = textureParams,
                        imagePath = it.imagePath,
                        imageId = Texture.defaultImages.getOrNull(it.imageId) ?: -1,

                        paletteId = it.paletteId,
                        color = ColorConfig(
                            frequency = it.frequency.toDouble(),
                            phase = it.phase.toDouble(),
                            density = it.density.toDouble(),
                            fillColor = it.solidFillColor,
                            outlineColor = it.accent2
                        )

                    )
                    if (it.id == act.previousFractalId) {
                        Fractal.previous = newBookmark
                    } else {
                        Fractal.bookmarks.add(0, newBookmark)
                        Log.d("MAIN", "Bookmark -- id: ${newBookmark.customId}, name: ${newBookmark.name}, imagePath: ${it.imagePath}, imageId: ${it.imageId} thumbPath: ${it.thumbnailPath}, frequency: ${it.frequency}, phase: ${it.phase}")
                    }
                    Log.e("POSITION", "bookmark '${it.name}' loaded")


                }

            }
            Fractal.all.addAll(0, Fractal.bookmarks)

            // populate list
            Fractal.all.forEach { listItems.add(
                ListItem(
                    it,
                    if (it.hasCustomId || it == Fractal.emptyCustom) ListItemType.CUSTOM else ListItemType.DEFAULT,
                    R.layout.other_list_item
                ))
            }
            if (Fractal.all.none { it.isFavorite }) listItems.add(emptyFavorite)
            if (Fractal.bookmarks.isEmpty()) listItems.add(emptyCustom)

            val bookmarkListAdapter = ListAdapter(
                listItems,
                onEditCustomBookmark,
                onDeleteCustomBookmark,
                onDuplicateBookmark,
                emptyFavorite,
                emptyCustom
            )
            b.bookmarkListLayout.defaultList.apply {
                adapter = bookmarkListAdapter
                setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                    val firstVisiblePos = listLayoutManager.findFirstCompletelyVisibleItemPosition()
                    highlightListItemType(
                        bookmarkListAdapter, when {
                        firstVisiblePos < bookmarkListAdapter.getGlobalPositionOf(bookmarkListAdapter.headerItems[1]) -> 0
                        firstVisiblePos < bookmarkListAdapter.getGlobalPositionOf(bookmarkListAdapter.headerItems[2]) -> 1
                        else -> 2
                    })
                }
                layoutManager = listLayoutManager
            }
            bookmarkListAdapter.showAllHeaders()
            bookmarkListAdapter.mItemClickListener = FlexibleAdapter.OnItemClickListener { view, position ->

                if (bookmarkListAdapter.getItemViewType(position) !in nonClickableViewTypes) {

                    val firstVisiblePos = listLayoutManager.findFirstCompletelyVisibleItemPosition()
                    val lastVisiblePos = listLayoutManager.findLastCompletelyVisibleItemPosition()

                    if (position != bookmarkListAdapter.activatedPos) bookmarkListAdapter.setActivatedPositionNoToggle(position)
                    val bookmark: Fractal = try {
                        bookmarkListAdapter.getActivatedItem()?.t ?: f
                    } catch (e: IndexOutOfBoundsException) {
                        Log.e("SHAPE", "array index out of bounds -- index: $position")
                        act.showMessage(resources.getString(R.string.msg_error))
                        f
                    }


                    if (bookmark.goldFeature && !sc.goldEnabled) act.showUpgradeScreen()
                    else {

                        if (fsv.r.isRendering) fsv.r.interruptRender = true

                        fsv.r.checkThresholdCross {

                            // restore state
                            if (firstBookmarkSelection) firstBookmarkSelection = false
                            else f.load(Fractal.tempBookmark2, fsv)

                            f.preload(bookmark)
                            Fractal.tempBookmark2 = f.bookmark(fsv)
                            f.load(bookmark, fsv)
                            act.updateCrashKeys()

                        }

                        fsv.r.renderShaderChanged = true
                        if (sc.autofitColorRange && f.color.density == 0.0 && !f.texture.hasRawOutput) {
                            fsv.r.autofitColorSelected = true
                            fsv.r.calcNewTextureSpan = true
                        }
                        fsv.r.renderToTex = true
                        fsv.requestRender()

                    }

                    true //Important!

                }
                else false

            }


            Log.e("POSITION", "bookmark load successful!")

        }


        var currentAspect : CardView? = null
        val aspectRatioButtonListener = { card: CardView, button: ImageButton, ratio: AspectRatio ->
            View.OnClickListener {

                if (ratio.goldFeature && !sc.goldEnabled) act.showUpgradeScreen()
                else {

                    sc.aspectRatio = ratio

                    currentAspect?.setCardBackgroundColor(resources.getColor(R.color.toggleButtonUnselected, null))
                    (currentAspect?.children?.first() as? ImageButton)?.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.aspectRatioIconUnselected, null))
                    // currentAspect?.cardElevation = 2f

                    card.setCardBackgroundColor(resources.getColor(R.color.divider, null))
                    button.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.selectedTint, null))
                    // card.cardElevation = 12f

                    currentAspect = card
                    updateResolutionText(sc.resolution, sc.aspectRatio)

                }

            }
        }

        b.aspect11Button.setOnClickListener(aspectRatioButtonListener(b.aspect11Card, b.aspect11Button, AspectRatio.RATIO_1_1))
        b.aspect45Button.setOnClickListener(aspectRatioButtonListener(b.aspect45Card, b.aspect45Button, AspectRatio.RATIO_4_5))
        b.aspect57Button.setOnClickListener(aspectRatioButtonListener(b.aspect57Card, b.aspect57Button, AspectRatio.RATIO_5_7))
        b.aspect23Button.setOnClickListener(aspectRatioButtonListener(b.aspect23Card, b.aspect23Button, AspectRatio.RATIO_2_3))
        b.aspect916Button.setOnClickListener(aspectRatioButtonListener(b.aspect916Card, b.aspect916Button, AspectRatio.RATIO_9_16))
        b.aspect12Button.setOnClickListener(aspectRatioButtonListener(b.aspect12Card, b.aspect12Button, AspectRatio.RATIO_1_2))
        b.aspectDefaultButton.setOnClickListener(aspectRatioButtonListener(b.aspectDefaultCard, b.aspectDefaultButton, AspectRatio.RATIO_SCREEN))

        val aspectCards = listOf(
            b.aspectDefaultCard,
            b.aspect11Card,
            b.aspect45Card,
            b.aspect57Card,
            b.aspect23Card,
            b.aspect916Card,
            b.aspect12Card
        )
        val aspectButtons = listOf(
            b.aspectDefaultButton,
            b.aspect11Button,
            b.aspect45Button,
            b.aspect57Button,
            b.aspect23Button,
            b.aspect916Button,
            b.aspect12Button
        )
        val goldAspects = listOf(
            b.aspect45Button,
            b.aspect57Button,
            b.aspect23Button,
            b.aspect916Button,
            b.aspect12Button
        )
        goldAspects.forEach {
            it.showGradient = true
            it.imageTintList = ColorStateList.valueOf(Color.WHITE)
        }


//        b.resolutionButton.setOnClickListener(subMenuButtonListener(b.resolutionLayout, b.resolutionButton))
//        b.aspectRatioButton.setOnClickListener(subMenuButtonListener(b.aspectRatioLayout, b.aspectRatioButton, UiLayoutHeight.MED))


        currentAspect = aspectCards[AspectRatio.all.indexOf(sc.aspectRatio)]
        aspectButtons[AspectRatio.all.indexOf(sc.aspectRatio)].performClick()


        b.positionDoneButton.setOnClickListener {

            firstBookmarkSelection = true

            act.apply {
                showCategoryButtons()
                showMenuToggleButton()
                showHeaderButtons()
            }
            b.imageNavButtons.hide()
            b.randomizeButton.hide()
            setCurrentLayout(b.zoomLayout)
            setCurrentButton(b.zoomButton)
//            b.positionMenuButtons.show()
            act.updateFragmentLayouts()

            sc.editMode = EditMode.POSITION

        }
        b.bookmarkListCancelButton.setOnClickListener {

            act.apply {
                showCategoryButtons()
                showMenuToggleButton()
                showHeaderButtons()
            }
            b.imageNavButtons.hide()
            setCurrentLayout(b.zoomLayout)
            setCurrentButton(b.zoomButton)
//            b.positionMenuButtons.show()


            handler.postDelayed({

                if (!firstBookmarkSelection) f.load(Fractal.tempBookmark2, fsv)
                fsv.r.checkThresholdCross { f.load(Fractal.tempBookmark3, fsv) }
                fsv.r.renderShaderChanged = true
                fsv.r.renderToTex = true
                fsv.requestRender()
                firstBookmarkSelection = true

            }, BUTTON_CLICK_DELAY_MED)

        }

        b.randomizeShapeSwitch.setOnClickListener {
            b.randomizePositionSwitch.apply {
                alpha = if (b.randomizeShapeSwitch.isChecked) 1f else 0.35f
                isClickable = b.randomizeShapeSwitch.isChecked
                if (!b.randomizeShapeSwitch.isChecked) isChecked = false
            }
        }
        b.randomizeButton.setOnClickListener {
            if (fsv.r.isRendering) fsv.r.interruptRender = true
            f.randomize(
                fsv,
                randomizeShape    = b.randomizeShapeSwitch    .isChecked,
                randomizeTexture  = b.randomizeTextureSwitch  .isChecked,
                randomizePosition = b.randomizePositionSwitch .isChecked,
                randomizeColor    = b.randomizeColorSwitch    .isChecked,
            )
            fsv.r.renderShaderChanged = true
            fsv.r.renderToTex = true
            fsv.r.calcNewTextureSpan = true
            fsv.requestRender()
        }





        b.resetPositionButton.setOnClickListener {

            fsv.r.checkThresholdCross { f.shape.position.reset() }
            fsv.r.calcNewTextureSpan = true
            fsv.r.renderToTex = true
            if (fsv.r.isRendering) fsv.r.interruptRender = true
            fsv.requestRender()

        }

        b.shiftHorizontalButton.setOnClickListener{
            subMenuButtonListener(b.xLayout, b.shiftHorizontalButton).onClick(it)
        }
        b.shiftVerticalButton.setOnClickListener {
            subMenuButtonListener(b.yLayout, b.shiftVerticalButton).onClick(it)
        }
        b.zoomButton.setOnClickListener(subMenuButtonListener(b.zoomLayout, b.zoomButton))
        b.rotateButton.setOnClickListener(subMenuButtonListener(b.rotationLayout, b.rotateButton))




        // SHIFT HORIZONTAL LAYOUT
        b.xValue.setOnEditorActionListener(editListener(b.yValue) { w: TextView ->
            val result = w.text.toString().formatToDouble()
            if (result != null) {
                f.shape.position.x = result
                if (fsv.r.isRendering) fsv.r.interruptRender = true
                fsv.r.renderToTex = true
            }
            w.text = "%.17f".format(f.shape.position.x)

        })
        b.shiftHorizontalSensitivity.sensitivityButton.apply {
            setOnClickListener {
            }
        }



        // SHIFT VERTICAL LAYOUT
        b.yValue.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()
            if (result != null) {
                f.shape.position.y = result
                if (fsv.r.isRendering) fsv.r.interruptRender = true
                fsv.r.renderToTex = true
            }
            w.text = "%.17f".format(f.shape.position.y)
        })
        b.shiftVerticalSensitivity.sensitivityButton.apply {}




        // ZOOM LAYOUT
        b.zoomSignificandValue.setOnEditorActionListener(
                editListener(b.zoomExponentValue) { w: TextView ->
                    val result1 = w.text.toString().formatToDouble(false)
                    val result2 = b.zoomExponentValue.text.toString().formatToDouble(false)
                    val result3 = "${w.text}e${b.zoomExponentValue.text}".formatToDouble(false)
                    if (result1 != null && result2 != null && result3 != null) {
                        if (result3.isInfinite() || result3.isNaN()) {
                            act.showMessage(resources.getString(R.string.msg_num_out_range))
                        }
                        else {
                            f.shape.position.zoom = result3
                            fsv.r.renderToTex = true
                            fsv.r.calcNewTextureSpan = true
                            if (fsv.r.isRendering) fsv.r.interruptRender = true
                        }
                    }
                    else {
                        act.showMessage(resources.getString(R.string.msg_invalid_format))
                    }
                    val scaleStrings = "%e".format(Locale.US, f.shape.position.zoom).split("e")
                    w.text = "%.2f".format(scaleStrings[0].toFloat())
                    b.zoomExponentValue.setText("%d".format(scaleStrings[1].toInt()))
                })
        b.zoomExponentValue.setOnEditorActionListener(
                editListener(null) { w: TextView ->

                    val result1 = b.zoomSignificandValue.text.toString().formatToDouble(false)
                    val result2 = w.text.toString().formatToDouble(false)
                    val result3 = "${b.zoomSignificandValue.text}e${w.text}".formatToDouble(false)
                    if (result1 != null && result2 != null && result3 != null) {
                        if (result3.isInfinite() || result3.isNaN()) {
                            act.showMessage(resources.getString(R.string.msg_num_out_range))
                        }
                        else {
                            fsv.r.checkThresholdCross { f.shape.position.zoom = result3 }
                            if (fsv.r.isRendering) fsv.r.interruptRender = true
                            fsv.r.renderToTex = true
                            fsv.r.calcNewTextureSpan = true
                        }
                    }
                    else {
                        act.showMessage(resources.getString(R.string.msg_invalid_format))
                    }
                    val scaleStrings = "%e".format(Locale.US, f.shape.position.zoom).split("e")
                    b.zoomSignificandValue.setText("%.2f".format(scaleStrings[0].toFloat()))
                    w.text = "%d".format(scaleStrings[1].toInt())

                })



        // ROTATION LAYOUT
        b.rotationValue.setOnEditorActionListener(
                editListener(null) { w: TextView ->
                    val result = w.text.toString().formatToDouble()?.inRadians()
                    if (result != null) {
                        f.shape.position.rotation = result
                        fsv.r.renderToTex = true
                        if (fsv.r.isRendering) fsv.r.interruptRender = true
                    }
                    w.text = "%.1f".format(f.shape.position.rotation.inDegrees())
                })
//        b.rotationLock.setOnClickListener {
//            f.shape.position.rotationLocked = b.rotationLock.isChecked
//        }




        b.resolutionLayout.hide()
        b.aspectRatioLayout.hide()
        b.imageNavButtons.hide()
        b.bookmarkListLayout.root.hide()
        b.randomizerLayout.hide()
        b.randomizeButton.hide()
        b.xLayout.hide()
        b.yLayout.hide()
        b.zoomLayout.hide()
        b.rotationLayout.hide()

        layout = b.zoomLayout
        button = b.zoomButton
        setCurrentButton(b.zoomButton)
        setCurrentLayout(b.zoomLayout)
        // zoomButton.performClick()

        updateLayout()
        crashlytics().setCustomKey(CRASH_KEY_FRAG_POS_CREATED, true)

    }


    override fun updateLayout() {
        updateValues()
    }

    override fun updateValues() {
        updateShiftValues()
        updateRotationValues()
        updateZoomValues()
    }

    override fun onGoldEnabled() {

        b.bookmarkListLayout.defaultList.adapter?.notifyDataSetChanged()
        b.resolutionBar.showGradient = false
        listOf(
            b.aspect45Button,
            b.aspect57Button,
            b.aspect23Button,
            b.aspect916Button,
            b.aspect12Button
        ).forEach {
            it.showGradient = false
            it.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.aspectRatioIconUnselected, null))
        }

    }

    fun getBookmarkListAdapter() : ListAdapter<Fractal>? {
        return b.bookmarkListLayout.defaultList.adapter as? ListAdapter<Fractal>
    }

    fun openBookmarksList() {

        crashlytics().updateLastAction(Action.BOOKMARK_LOAD)

        sc.editMode = EditMode.NONE

        setCurrentLayout(b.bookmarkListLayout.root)
        b.randomizeButton.hide()
//        b.positionMenuButtons.hide()
        b.imageNavButtons.show()
        b.bookmarkListCancelButton.show()

        Fractal.tempBookmark3 = f.bookmark(fsv)

        act.apply {
            hideMenuToggleButton()
            hideCategoryButtons()
            hideHeaderButtons()
        }

    }

    fun openRandomizer() {

        crashlytics().updateLastAction(Action.RANDOMIZE)

        sc.editMode = EditMode.COLOR

        setCurrentLayout(b.randomizerLayout)
        b.bookmarkListCancelButton.hide()
//        b.positionMenuButtons.hide()
        b.imageNavButtons.show()
        b.randomizeButton.show()

        act.apply {
            hideMenuToggleButton()
            hideCategoryButtons()
            hideHeaderButtons()
        }

    }

    fun showResolutionLayout() {

        crashlytics().updateLastAction(Action.RESOLUTION_CHANGE)

        sc.editMode = EditMode.NONE

        setCurrentLayout(b.resolutionLayout)
        b.bookmarkListCancelButton.hide()
//        b.positionMenuButtons.hide()
        b.imageNavButtons.show()

        act.apply {
            hideMenuToggleButton()
            hideCategoryButtons()
            hideHeaderButtons()
        }

    }

    fun showAspectRatioLayout() {

        crashlytics().updateLastAction(Action.ASPECT_CHANGE)

        sc.editMode = EditMode.NONE

        setCurrentLayout(b.aspectRatioLayout)
        b.bookmarkListCancelButton.hide()
//        b.positionMenuButtons.hide()
        b.imageNavButtons.show()

        act.apply {
            hideMenuToggleButton()
            hideCategoryButtons()
            hideHeaderButtons()
        }

    }

    fun updateShiftValues() {
        b.xValue.setText("%.17f".format(f.shape.position.x))
        b.yValue.setText("%.17f".format(f.shape.position.y))
    }

    fun updateRotationValues() {
        b.rotationValue.setText("%.1f".format(f.shape.position.rotation.inDegrees()))
    }

    fun updateZoomValues() {
        val zoomStrings = "%e".format(Locale.US, f.shape.position.zoom).split("e")
        b.zoomSignificandValue.setText("%.2f".format(zoomStrings.getOrNull(0)?.toFloat() ?: 1f))
        b.zoomExponentValue.setText("%d".format(zoomStrings.getOrNull(1)?.toInt() ?: 2))
    }

    fun updateResolutionText(res: Resolution, ratio: AspectRatio) {

        val dims = Point(res.w, res.h)
        if (ratio.r > AspectRatio.RATIO_SCREEN.r) dims.x = (dims.y / ratio.r).toInt()
        else if (ratio.r < AspectRatio.RATIO_SCREEN.r) dims.y = (dims.x * ratio.r).toInt()

        b.resolutionValue.text = "${dims.x} x ${dims.y}"

    }

    fun highlightListItemType(adapter: ListAdapter<Fractal>, index: Int) {
        adapter.apply {
            listOf(
                b.bookmarkListLayout.listFavoritesButton,
                b.bookmarkListLayout.listCustomButton,
                b.bookmarkListLayout.listDefaultButton
            ).forEachIndexed { i, b ->
                b.setTextColor(resources.getColor(if (index == i) R.color.colorDarkText else R.color.colorDarkTextMuted, null))
            }
        }
    }

}