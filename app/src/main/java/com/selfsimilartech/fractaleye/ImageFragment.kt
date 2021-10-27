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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import com.selfsimilartech.fractaleye.databinding.ImageFragmentBinding
import eu.davidea.flexibleadapter.FlexibleAdapter
import kotlinx.coroutines.launch
import java.lang.IndexOutOfBoundsException


class ImageFragment : MenuFragment() {

    lateinit var b : ImageFragmentBinding

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

        b = ImageFragmentBinding.inflate(inflater, container, false)
        return b.root
        // return inflater.inflate(R.layout.image_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        super.onViewCreated(v, savedInstanceState)

        val nonClickableViewTypes = listOf(
                R.layout.list_header,
                R.layout.list_item_linear_empty_favorite,
                R.layout.list_item_linear_empty_custom
        )

        val handler = Handler(Looper.getMainLooper())

        b.imageLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)


        b.fitToViewportButton.setOnClickListener {

            // if (!fitToViewportButton.isChecked && sc.aspectRatio != AspectRatio.RATIO_SCREEN) aspectDefaultButton.performClick()

            sc.fitToViewport = b.fitToViewportButton.isChecked
            act.updateFractalLayout()

        }
        b.fitToViewportButton.isChecked = sc.fitToViewport


        b.resolutionBar.showGradient = true
        b.resolutionBar.gradientStartProgress = Resolution.NUM_VALUES_FREE - 1
        b.resolutionBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            var prevProgress = 0

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Log.e("SETTINGS", "progress: $progress")
                updateResolutionText(Resolution.working[progress], sc.aspectRatio)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                prevProgress = b.resolutionBar.progress
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                val newRes = Resolution.working[b.resolutionBar.progress]

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
        b.resolutionBar.progress = Resolution.working.indexOf(sc.resolution)
        updateResolutionText(sc.resolution, sc.aspectRatio)


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

                        viewLifecycleOwner.lifecycleScope.launch {
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
        b.bookmarkListLayout.list.apply {
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
                if (position + 1 > lastVisiblePos) b.bookmarkListLayout.list.smoothSnapToPosition(position + 1, LinearSmoothScroller.SNAP_TO_END)
                else if (position - 1 < firstVisiblePos) b.bookmarkListLayout.list.smoothSnapToPosition(position - 1)

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
                        act.updateCrashKeys()

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


//        b.bookmarkListButton.setOnClickListener {
//            crashlytics().updateLastAction(Action.BOOKMARK_LOAD)
//            Handler(Looper.getMainLooper()).postDelayed({
//
//                fsv.r.reaction = Reaction.NONE
//
//                showLayout(b.bookmarkListLayout.root)
//                b.imageSubMenuButtons.hide()
//                b.imageNavButtons.show()
//
//                Fractal.tempBookmark3 = f.bookmark(fsv)
//
//                act.hideMenuToggleButton()
//                act.hideCategoryButtons()
//                act.uiSetHeight(UiLayoutHeight.TALL)
//
//            }, BUTTON_CLICK_DELAY_SHORT)
//        }

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


        b.bookmarkListLayout.apply {
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


//        b.newBookmarkButton2.setOnClickListener {
//
//            crashlytics().updateLastAction(Action.NEW_BOOKMARK)
//            Fractal.tempBookmark1 = f.bookmark(fsv)
//
//            fsv.r.renderProfile = RenderProfile.SAVE_THUMBNAIL
//            fsv.requestRender()
//
//        }

        b.bookmarkListCancelButton.setOnClickListener {
            handler.postDelayed({

                act.showCategoryButtons()
                act.showMenuToggleButton()
                b.imageNavButtons.hide()
//                b.imageSubMenuButtons.show()
//                b.resolutionButton.performClick()
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


        layout = b.resolutionLayout
        button = b.fitToViewportButton
//        currentButton = b.resolutionButton
        b.resolutionLayout.hide()
        b.aspectRatioLayout.hide()
        b.imageNavButtons.hide()
        b.bookmarkListLayout.root.hide()


        setCurrentLayout(b.resolutionLayout)
//        alphaButton(b.resolutionButton)

        updateLayout()

    }

    override fun updateLayout() {}

    override fun updateValues() {}

    fun updateResolutionText(res: Resolution, ratio: AspectRatio) {

        val dims = Point(res.w, res.h)
        if (ratio.r > AspectRatio.RATIO_SCREEN.r) dims.x = (dims.y / ratio.r).toInt()
        else if (ratio.r < AspectRatio.RATIO_SCREEN.r) dims.y = (dims.x * ratio.r).toInt()

        b.resolutionValue.text = "${dims.x} x ${dims.y}"
    }

    fun highlightListHeader(adapter: ListAdapter<Fractal>, index: Int) {
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

    override fun onGoldEnabled() {

        bookmarkListAdapter.notifyDataSetChanged()
        b.resolutionBar.showGradient = false
        val goldAspects = listOf(
                b.aspect45Button,
                b.aspect57Button,
                b.aspect23Button,
                b.aspect916Button,
                b.aspect12Button
        )
        goldAspects.forEach {
            it.showGradient = false
            it.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.unselectedTint, null))
        }

    }

}