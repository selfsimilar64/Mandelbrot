package com.selfsimilartech.fractaleye

import android.animation.LayoutTransition
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import eu.davidea.flexibleadapter.FlexibleAdapter
import kotlinx.android.synthetic.main.image_fragment.*
import kotlinx.android.synthetic.main.list_layout.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IndexOutOfBoundsException


class ImageFragment : MenuFragment() {

//    private fun createNotificationChannel() {
//        // Create the NotificationChannel, but only on API 26+ because
//        // the NotificationChannel class is new and not in the support library
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = "fractal notify"
//            val descriptionText = "remind about fractals"
//            val importance = NotificationManager.IMPORTANCE_HIGH
//            val channel = NotificationChannel("Fractal Eye", name, importance).apply {
//                description = descriptionText
//            }
//            // Register the channel with the system
//            val notificationManager: NotificationManager =
//                    (activity as MainActivity).getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
//    }

    lateinit var bookmarkListAdapter : ListAdapter<Fractal>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {

        return inflater.inflate(R.layout.image_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        super.onViewCreated(v, savedInstanceState)



        val nonClickableViewTypes = listOf(
                R.layout.list_header,
                R.layout.list_item_linear_empty_favorite,
                R.layout.list_item_linear_empty_custom
        )

        val handler = Handler(Looper.getMainLooper())

        imageLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)


        fitToViewportButton.setOnClickListener {

            // if (!fitToViewportButton.isChecked && sc.aspectRatio != AspectRatio.RATIO_SCREEN) aspectDefaultButton.performClick()

            sc.fitToViewport = fitToViewportButton.isChecked
            act.updateSurfaceViewLayout()

        }
        fitToViewportButton.isChecked = sc.fitToViewport


        resolutionBar.showGradient = true
        resolutionBar.gradientStartProgress = Resolution.NUM_VALUES_FREE - 1
        resolutionBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            var prevProgress = 0

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Log.e("SETTINGS", "progress: $progress")
                updateResolutionText(Resolution.working[progress], sc.aspectRatio)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                prevProgress = resolutionBar.progress
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                val newRes = Resolution.working[resolutionBar.progress]

                if (sc.resolution != newRes) {

                    if (resolutionBar.progress > resolutionBar.gradientStartProgress && !sc.goldEnabled) {
                        resolutionBar.progress = prevProgress
                        act.showUpgradeScreen()
                    }
                    else {

                        // save state on resolution increase
                        if (newRes.w > sc.resolution.w) act.bookmarkAsPreviousFractal()

                        sc.resolution = newRes
                        if (fsv.r.isRendering) fsv.r.interruptRender = true
                        fsv.r.fgResolutionChanged = true
                        fsv.r.renderToTex = true
                        fsv.requestRender()

                    }

                }

            }

        })
        resolutionBar.max = Resolution.NUM_VALUES_WORKING() - 1
        //Log.e("SETTINGS", "resolution ordinal: ${sc.resolution.ordinal}")
        resolutionBar.progress = Resolution.working.indexOf(sc.resolution)
        updateResolutionText(sc.resolution, sc.aspectRatio)


//        renderBackgroundSwitch.isChecked = sc.renderBackground
//        renderBackgroundSwitch.setOnCheckedChangeListener { _, isChecked ->
//
//            sc.renderBackground = isChecked
//
//            fsv.r.renderBackgroundChanged = true
//            fsv.r.renderToTex = isChecked
//            fsv.requestRender()
//
//        }


//        fitToViewportSwitch.isChecked = sc.fitToViewport
//        fitToViewportSwitch.setOnCheckedChangeListener { _, isChecked ->
//
//            sc.fitToViewport = isChecked
//            act.updateSurfaceViewLayout()
//
//        }



        val listLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val onEditCustomBookmark = { adapter: ListAdapter<Fractal>, item: ListItem<Fractal> ->

            Fractal.tempBookmark1 = item.t
            act.showBookmarkDialog(item, edit = true)

        }
        val onDeleteCustomBookmark = { adapter: ListAdapter<Fractal>, item: ListItem<Fractal> ->

            val dialog = AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom)
                    .setTitle("${resources.getString(R.string.delete)} ${item.t.name}?")
                    .setIcon(R.drawable.warning)
                    .setPositiveButton(android.R.string.ok) { dialog, whichButton ->

                        adapter.removeItemFromCustom(item)
                        val deleteId = item.t.customId

                        GlobalScope.launch {
                            act.db.fractalDao().apply {
                                delete(findById(deleteId))
                            }
                        }

                        requireContext().deleteFile(item.t.thumbnailPath)

                        Fractal.all.remove(item.t)
                        Fractal.bookmarks.remove(item.t)

                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()

        }
        val onDuplicateBookmark = { adapter: ListAdapter<Fractal>, item: ListItem<Fractal> ->

        }


        val emptyFavorite = ListItem(Fractal.emptyFavorite, ListHeader.FAVORITE, R.layout.list_item_linear_empty_favorite)
        val emptyCustom = ListItem(Fractal.emptyCustom, ListHeader.CUSTOM, R.layout.list_item_linear_empty_custom)
        val listItems = arrayListOf<ListItem<Fractal>>()



        Fractal.all.forEach { listItems.add(
                ListItem(
                        it,
                        if (it.hasCustomId || it == Fractal.emptyCustom) ListHeader.CUSTOM else ListHeader.DEFAULT,
                        R.layout.other_list_item
                ).apply {
                    if (it.isFavorite) {
                        val favorite = ListItem(it, ListHeader.FAVORITE, R.layout.other_list_item, compliment = this)
                        compliment = favorite
                        listItems.add(favorite)
                    }
                })
        }
        if (Fractal.all.none { it.isFavorite }) listItems.add(emptyFavorite)
        if (Fractal.bookmarks.isEmpty()) listItems.add(emptyCustom)
        listItems.sortBy { it.header.type }


        var firstBookmarkSelection = true

        bookmarkListAdapter = ListAdapter(
                listItems,
                onEditCustomBookmark,
                onDeleteCustomBookmark,
                onDuplicateBookmark,
                emptyFavorite,
                emptyCustom
        )
        bookmarkListLayout.list.apply {
            adapter = bookmarkListAdapter
            setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                val firstVisiblePos = listLayoutManager.findFirstCompletelyVisibleItemPosition()
                highlightListHeader(bookmarkListAdapter, when {
                    firstVisiblePos < bookmarkListAdapter.getGlobalPositionOf(bookmarkListAdapter.headerItems[1]) -> 0
                    firstVisiblePos < bookmarkListAdapter.getGlobalPositionOf(bookmarkListAdapter.headerItems[2]) -> 1
                    else -> 2
                })
            }
            layoutManager = listLayoutManager
        }
        bookmarkListAdapter.apply {
            //isLongPressDragEnabled = true
            showAllHeaders()
            //setAnimationOnForwardScrolling(true)
            //setAnimationOnReverseScrolling(true)
        }
        bookmarkListAdapter.currentItems.forEach {
            Log.e("IMAGE", "f: ${it.t.name}")

        }

        bookmarkListAdapter.mItemClickListener = FlexibleAdapter.OnItemClickListener { view, position ->

            if (bookmarkListAdapter.getItemViewType(position) !in nonClickableViewTypes) {

                val firstVisiblePos = listLayoutManager.findFirstCompletelyVisibleItemPosition()
                val lastVisiblePos = listLayoutManager.findLastCompletelyVisibleItemPosition()
                if (position + 1 > lastVisiblePos) bookmarkListLayout.list.smoothSnapToPosition(position + 1, LinearSmoothScroller.SNAP_TO_END)
                else if (position - 1 < firstVisiblePos) bookmarkListLayout.list.smoothSnapToPosition(position - 1)

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

                    fsv.r.checkThresholdCross(showMsg = false) {

                        // restore state
                        if (firstBookmarkSelection) firstBookmarkSelection = false
                        else f.load(Fractal.tempBookmark2, fsv)

                        f.preload(bookmark)
                        Fractal.tempBookmark2 = f.bookmark(fsv)
                        f.load(bookmark, fsv)

                    }

                    fsv.r.renderShaderChanged = true
                    if (sc.autofitColorRange && f.density == 0f && !f.texture.hasRawOutput) {
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


        bookmarkListButton.setOnClickListener {
            Handler(Looper.getMainLooper()).postDelayed({

                fsv.r.reaction = Reaction.NONE

                showLayout(bookmarkListLayout)
                imageSubMenuButtons.hide()
                imageNavButtons.show()

                Fractal.tempBookmark3 = f.bookmark(fsv)

                act.hideMenuToggleButton()
                act.hideCategoryButtons()
                act.uiSetHeight(UiLayoutHeight.TALL)

            }, BUTTON_CLICK_DELAY_SHORT)
        }

        var currentAspect : CardView? = null
        val aspectRatioButtonListener = { card: CardView, button: ImageButton, ratio: AspectRatio ->
            View.OnClickListener {

                if (ratio.goldFeature && !sc.goldEnabled) act.showUpgradeScreen()
                else {

                    sc.aspectRatio = ratio

                    currentAspect?.setCardBackgroundColor(resources.getColor(R.color.toggleButtonUnselected, null))
                    (currentAspect?.children?.first() as? ImageButton)?.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.colorDarkTextMuted, null))
                    // currentAspect?.cardElevation = 2f

                    card.setCardBackgroundColor(resources.getColor(R.color.divider, null))
                    button.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.selectedTint, null))
                    // card.cardElevation = 12f

                    currentAspect = card
                    updateResolutionText(sc.resolution, sc.aspectRatio)
                    act.onAspectRatioChanged()

                }

            }
        }

        aspect11Button.setOnClickListener(aspectRatioButtonListener(aspect11Card, aspect11Button, AspectRatio.RATIO_1_1))
        aspect45Button.setOnClickListener(aspectRatioButtonListener(aspect45Card, aspect45Button, AspectRatio.RATIO_4_5))
        aspect57Button.setOnClickListener(aspectRatioButtonListener(aspect57Card, aspect57Button, AspectRatio.RATIO_5_7))
        aspect23Button.setOnClickListener(aspectRatioButtonListener(aspect23Card, aspect23Button, AspectRatio.RATIO_2_3))
        aspect916Button.setOnClickListener(aspectRatioButtonListener(aspect916Card, aspect916Button, AspectRatio.RATIO_9_16))
        aspect12Button.setOnClickListener(aspectRatioButtonListener(aspect12Card, aspect12Button, AspectRatio.RATIO_1_2))
        aspectDefaultButton.setOnClickListener(aspectRatioButtonListener(aspectDefaultCard, aspectDefaultButton, AspectRatio.RATIO_SCREEN))

        val aspectCards = listOf(
                aspectDefaultCard,
                aspect11Card,
                aspect45Card,
                aspect57Card,
                aspect23Card,
                aspect916Card,
                aspect12Card
        )
        val aspectButtons = listOf(
                aspectDefaultButton,
                aspect11Button,
                aspect45Button,
                aspect57Button,
                aspect23Button,
                aspect916Button,
                aspect12Button
        )
        val goldAspects = listOf(
                aspect45Button,
                aspect57Button,
                aspect23Button,
                aspect916Button,
                aspect12Button
        )
        goldAspects.forEach {
            it.showGradient = true
            it.imageTintList = ColorStateList.valueOf(Color.WHITE)
        }



        resolutionButton.setOnClickListener(subMenuButtonListener(resolutionLayout, resolutionButton))
        aspectRatioButton.setOnClickListener(subMenuButtonListener(aspectRatioLayout, aspectRatioButton, UiLayoutHeight.MED))


        currentAspect = aspectCards[AspectRatio.all.indexOf(sc.aspectRatio)]
        aspectButtons[AspectRatio.all.indexOf(sc.aspectRatio)]?.performClick()


        bookmarkListLayout.apply {
            listFavoritesButton.setOnClickListener {
                list.smoothSnapToPosition(bookmarkListAdapter.getGlobalPositionOf(bookmarkListAdapter.headerItems[0]))
            }
            listCustomButton.setOnClickListener {
                list.smoothSnapToPosition(bookmarkListAdapter.getGlobalPositionOf(bookmarkListAdapter.headerItems[1]))
            }
            listDefaultButton.setOnClickListener {
                list.smoothSnapToPosition(bookmarkListAdapter.getGlobalPositionOf(bookmarkListAdapter.headerItems[2]))
            }
        }
        

        newBookmarkButton2.setOnClickListener {

            Fractal.tempBookmark1 = f.bookmark(fsv)

            fsv.r.renderProfile = RenderProfile.SAVE_THUMBNAIL
            fsv.requestRender()

        }
        bookmarkListDoneButton.setOnClickListener {
            handler.postDelayed({

                firstBookmarkSelection = true

                act.showCategoryButtons()
                act.showMenuToggleButton()
                imageNavButtons.hide()
                imageSubMenuButtons.show()
                resolutionButton.performClick()
                act.uiSetHeight(UiLayoutHeight.SHORT)

                act.updateFragmentLayouts()

            }, BUTTON_CLICK_DELAY_SHORT)
        }
        bookmarkListCancelButton.setOnClickListener {
            handler.postDelayed({

                act.showCategoryButtons()
                act.showMenuToggleButton()
                imageNavButtons.hide()
                imageSubMenuButtons.show()
                resolutionButton.performClick()
                act.uiSetHeight(UiLayoutHeight.SHORT)

            }, BUTTON_CLICK_DELAY_SHORT)
            handler.postDelayed({

                if (!firstBookmarkSelection) f.load(Fractal.tempBookmark2, fsv)
                fsv.r.checkThresholdCross { f.load(Fractal.tempBookmark3, fsv) }
                fsv.r.renderShaderChanged = true
                fsv.r.renderToTex = true
                fsv.requestRender()
                firstBookmarkSelection = true

            }, BUTTON_CLICK_DELAY_MED)
        }


        currentLayout = resolutionLayout
        currentButton = resolutionButton
        resolutionLayout.hide()
        aspectRatioLayout.hide()
        imageNavButtons.hide()
        bookmarkListLayout.hide()


        resolutionButton.performClick()


        // if (sc.goldEnabled) onGoldEnabled()

    }


    fun updateResolutionText(res: Resolution, ratio: AspectRatio) {

        val dims = Point(res.w, res.h)
        if (ratio.r > AspectRatio.RATIO_SCREEN.r) dims.x = (dims.y / ratio.r).toInt()
        else if (ratio.r < AspectRatio.RATIO_SCREEN.r) dims.y = (dims.x * ratio.r).toInt()

        resolutionValue.text = "${dims.x} x ${dims.y}"
    }

    fun highlightListHeader(adapter: ListAdapter<Fractal>, index: Int) {
        adapter.apply {
            listOf(
                    bookmarkListLayout.listFavoritesButton,
                    bookmarkListLayout.listCustomButton,
                    bookmarkListLayout.listDefaultButton
            ).forEachIndexed { i, b ->
                b.setTextColor(resources.getColor(if (index == i) R.color.colorDarkText else R.color.colorDarkTextMuted, null))
            }
        }
    }

    fun onGoldEnabled() {

        bookmarkListAdapter.notifyDataSetChanged()
        resolutionBar.showGradient = false
        val goldAspects = listOf(
                aspect45Button,
                aspect57Button,
                aspect23Button,
                aspect916Button,
                aspect12Button
        )
        goldAspects.forEach {
            it.showGradient = false
            it.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.unselectedTint, null))
        }

    }

}