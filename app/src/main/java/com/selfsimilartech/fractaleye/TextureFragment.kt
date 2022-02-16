package com.selfsimilartech.fractaleye

import android.animation.LayoutTransition
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.selfsimilartech.fractaleye.databinding.FragmentTextureBinding
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.*


const val MAX_RADIUS_EXPONENT = 12.0

class TextureFragment : MenuFragment() {

    lateinit var b : FragmentTextureBinding

    val resultContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result?.data?.data?.let {

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val inputStream = context?.contentResolver?.openInputStream(it)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            var sampleSize = 1
            val minWidth = 180
            if (options.outWidth >= minWidth*2) {  // give resolution options

                val v = layoutInflater.inflate(R.layout.alert_dialog_texture_image_resolution, null, false)
                val textureImageResBar = v.findViewById<SeekBar>(R.id.alertResolutionSeekBar)
                val textureImageResText = v.findViewById<TextView>(R.id.alertResolutionText)

                var newWidth  : Int
                var newHeight : Int
                val sampleSizes = listOf(10, 8, 6, 4, 2, 1).takeLast(min(6, floor(options.outWidth.toDouble()/minWidth).toInt()))
                textureImageResBar.max = sampleSizes.size - 1
                textureImageResBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        sampleSize = sampleSizes.getOrNull(progress) ?: sampleSizes[3]
                        newWidth = (options.outWidth.toDouble()/sampleSize).toInt()
                        newHeight = (options.outHeight.toDouble()/sampleSize).toInt()
                        textureImageResText.text = "%d x %d".format(newWidth, newHeight)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}


                })
                textureImageResBar.progress = textureImageResBar.max

                val dialog = AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom)
                        .setIcon(R.drawable.resolution)
                        .setTitle(R.string.resolution)
                        .setMessage(R.string.texture_image_choose_res)
                        .setView(v)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            confirmTextureImageResolution(sampleSize, it)
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()

                dialog.setCanceledOnTouchOutside(false)

            } else {
                confirmTextureImageResolution(1, it)
            }

        }
    }

    private var realParamSeekBarListener : SeekBar.OnSeekBarChangeListener? = null
    private lateinit var textureListAdapter : ListAdapter<Texture>
    private lateinit var textureImageListAdapter : FlexibleAdapter<TextureImageListItem>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        b = FragmentTextureBinding.inflate(inflater, container, false)
        return b.root
        // return inflater.inflate(R.layout.texture_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        super.onViewCreated(v, savedInstanceState)


        Log.e("TEXTURE", "!! onViewCreated start !!")

        b.textureLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        val lockListener = { j: Int -> View.OnClickListener {
            val lock = it as android.widget.ToggleButton
            val param = f.texture.params.active
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
            val param = f.texture.params.active
            if (param is ComplexParam) param.linked = link.isChecked
            if (link.isChecked) {
                loadActiveParam()
                fsv.r.renderToTex = true
                fsv.requestRender()
            }
        }

        val resetListener = View.OnClickListener {
            f.texture.params.active.reset()
            loadActiveParam()
            fsv.r.renderToTex = true
            fsv.requestRender()
        }

        val handler = Handler(Looper.getMainLooper())
        val nonClickableViewTypes = listOf(
                R.layout.list_header,
                R.layout.list_item_linear_empty_favorite,
                R.layout.list_item_linear_empty_custom
        )


        val previewListWidth = Resolution.SCREEN.w - 2*resources.getDimension(R.dimen.categoryPagerMarginHorizontal) - resources.getDimension(R.dimen.navButtonHeight)
        val previewGridWidth = resources.getDimension(R.dimen.textureShapePreviewSize) + 2*resources.getDimension(R.dimen.previewGridPaddingHorizontal)
        //Log.e("COLOR FRAGMENT", "texturePreviewListWidth: $previewListWidth")
        //Log.e("COLOR FRAGMENT", "texturePreviewGridWidth: $previewGridWidth")
        val spanCount = floor(previewListWidth.toDouble() / previewGridWidth).toInt()
        //Log.e("COLOR FRAGMENT", "spanCount: ${previewListWidth.toDouble() / previewGridWidth}")


        val textureParamButtons = listOf(
                b.textureParamButton1,
                b.textureParamButton2,
                b.textureParamButton3,
                b.textureParamButton4
        )

        b.realTextureParam.apply {

            uValue2.setOnEditorActionListener(editListener(null) { w: TextView ->

                val param = f.texture.params.active
                val result = w.text.toString().formatToDouble()
                if (result != null) {
                    param.u = if (sc.restrictParams) param.clamp(result) else result
                    fsv.r.renderToTex = true
                }
                w.text = param.u.format(REAL_PARAM_DIGITS)

            })
            realParamResetButton.setOnClickListener(resetListener)
            realParamSensitivity.sensitivityValue.setOnEditorActionListener(editListener(null) { w: TextView ->
                
                val param = f.texture.params.active
                val result = w.text.toString().formatToDouble()
                if (result != null) param.sensitivity = result
                w.text = "%d".format(param.sensitivity.toInt())
            
            })
            paramAdjustLeftButton.setOnClickListener(ParamChangeOnClickListener(fsv,
                    transformFractal = { f.texture.params.active.apply { u -= sensitivityFactor/RealParam.ADJUST_DISCRETE } },
                    updateLayout = { updateParamText() }
            ))
            paramAdjustLeftButton.setOnLongClickListener(ParamChangeOnLongClickListener(fsv,
                    transformFractal = { f.texture.params.active.apply { u -= sensitivityFactor/RealParam.ADJUST_CONTINUOUS } },
                    updateLayout = { updateParamText() }
            ))
            paramAdjustRightButton.setOnClickListener(ParamChangeOnClickListener(fsv,
                    transformFractal = { f.texture.params.active.apply { u += sensitivityFactor/RealParam.ADJUST_DISCRETE } },
                    updateLayout = { updateParamText() }
            ))
            paramAdjustRightButton.setOnLongClickListener(ParamChangeOnLongClickListener(fsv,
                    transformFractal = { f.texture.params.active.apply { u += sensitivityFactor/RealParam.ADJUST_CONTINUOUS } },
                    updateLayout = { updateParamText() }
            ))

        }
        b.complexTextureParam.apply {

            uValue.setOnEditorActionListener(editListener(vValue) { w: TextView ->
                val result = "${w.text}".formatToDouble()
                val param = f.texture.params.active
                if (result != null) {
                    param.u = result
                    fsv.r.renderToTex = true
                }
                w.text = param.u.format(COMPLEX_PARAM_DIGITS)
            })
            vValue.setOnEditorActionListener(editListener(null) { w: TextView ->
                val result = "${w.text}".formatToDouble()
                val param = f.texture.params.active
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

                val param = f.texture.params.active
                val result = w.text.toString().formatToDouble()
                if (result != null) param.sensitivity = result
                w.text = "%d".format(param.sensitivity.toInt())

            })

        }


        val listLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val onEditCustomTexture = { adapter: ListAdapter<Texture>, item: ListItem<Texture> -> }
        val onDeleteCustomTexture = { adapter: ListAdapter<Texture>, item: ListItem<Texture> -> }
        val onDuplicateTexture = { adapter: ListAdapter<Texture>, item: ListItem<Texture> -> }

        val emptyFavorite = ListItem<Texture>(Texture.emptyFavorite, ListItemType.FAVORITE, R.layout.list_item_linear_empty_favorite)
        val emptyCustom = ListItem<Texture>(Texture.emptyCustom, ListItemType.CUSTOM, R.layout.list_item_linear_empty_custom)
        val listItems = getTextureListItems()



        textureListAdapter = ListAdapter(
                listItems,
                onEditCustomTexture,
                onDeleteCustomTexture,
                onDuplicateTexture,
                emptyFavorite,
                emptyCustom
        )
        b.textureListLayout.defaultList.apply {
            adapter = textureListAdapter
            setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                val firstVisiblePos = listLayoutManager.findFirstCompletelyVisibleItemPosition()
                highlightListItemType(textureListAdapter, when {
                    firstVisiblePos < textureListAdapter.getGlobalPositionOf(textureListAdapter.headerItems[1]) -> 0
                    else -> 1
                })
            }
            layoutManager = listLayoutManager
        }
        textureListAdapter.apply {
            //isLongPressDragEnabled = true
            mode = SelectableAdapter.Mode.SINGLE
            showAllHeaders()
            //setAnimationOnForwardScrolling(true)
            //setAnimationOnReverseScrolling(true)
        }
        textureListAdapter.mItemClickListener = FlexibleAdapter.OnItemClickListener { view, position ->

            if (textureListAdapter.getItemViewType(position) !in nonClickableViewTypes) {

                val firstVisiblePos = listLayoutManager.findFirstCompletelyVisibleItemPosition()
                val lastVisiblePos = listLayoutManager.findLastCompletelyVisibleItemPosition()

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

                        if (newTexture.hasRawOutput != f.texture.hasRawOutput) fsv.r.loadTextureImage = true
                        f.texture = newTexture
                        act.updateCrashKeys()

                        if (sc.autofitColorRange) fsv.r.calcNewTextureSpan = true

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



        val textureImageItems = mutableListOf<TextureImageListItem>()
        Texture.defaultImages.forEach  { textureImageItems.add(TextureImageListItem(id = it))   }
        Texture.customImages.forEach { textureImageItems.add(TextureImageListItem(path = it)) }
        textureImageItems.add(TextureImageListItem(id = R.drawable.texture_image_add))
        textureImageListAdapter = FlexibleAdapter(textureImageItems)
        textureImageListAdapter.mode = SelectableAdapter.Mode.SINGLE
        b.textureImageList.adapter = textureImageListAdapter
        textureImageListAdapter.mItemClickListener = FlexibleAdapter.OnItemClickListener { view, position ->

            val item = textureImageListAdapter.getItem(position)
            textureImageListAdapter.toggleSelection(position)

            if (item?.id == R.drawable.texture_image_add) {
                if (!sc.goldEnabled) act.showUpgradeScreen()
                else resultContract.launch(ActivityResultContracts.GetContent().createIntent(requireContext(), "image/*"))
                false
            } else {
                if (item != null) {
                    if (item.path != "") {
                        f.imagePath = item.path
                        f.imageId = -1
                    }
                    else if (item.id != -1) {
                        f.imagePath = ""
                        f.imageId = item.id
                    }
                }
                fsv.r.loadTextureImage = true
                fsv.r.renderToTex = true
                fsv.requestRender()
                true
            }

        }
        textureImageListAdapter.mItemLongClickListener = FlexibleAdapter.OnItemLongClickListener { position ->

            val item = textureImageListAdapter.getItem(position)

            if (item?.id == -1) {
                AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom)
                        .setTitle("${resources.getString(R.string.remove)} ${resources.getString(R.string.image)}?")
                        .setIcon(R.drawable.color_remove)
                        .setMessage(resources.getString(R.string.remove_texture_image_bookmark_warning).format(Fractal.bookmarks.count { it.imagePath == item.path }))
                        .setPositiveButton(android.R.string.ok) { dialog, which ->

                            requireContext().deleteFile(item.path)
                            viewLifecycleOwner.lifecycleScope.launch {
                                act.db.fractalDao().apply {
                                    Fractal.bookmarks.forEach {
                                        if (it.imagePath == item.path) {
                                            it.imagePath = ""
                                            it.imageId = R.drawable.flower
                                            updateImage(it.customId, it.imagePath, Texture.defaultImages.indexOf(it.imageId))
                                        }
                                    }
                                }
                            }

                            textureImageListAdapter.removeItem(position)

                            if (textureImageListAdapter.selectedPositions.contains(position)) {
                                textureImageListAdapter.apply {
                                    toggleSelection(0)
                                    notifyItemChanged(0)
                                }
                                f.imagePath = ""
                                f.imageId = Texture.defaultImages[0]
                                fsv.r.loadTextureImage = true
                                fsv.r.renderToTex = true
                                fsv.requestRender()
                            }

                        }
                        .setNegativeButton(android.R.string.cancel) { dialog, which ->

                        }
                        .show()
            }

        }





        b.radiusSignificandValue.setOnEditorActionListener(
                editListener(b.radiusExponentValue) { w: TextView ->
                    val result1 = w.text.toString().formatToDouble(false)
                    val result2 = b.radiusExponentValue.text.toString().formatToDouble(false)
                    val result3 = "${w.text}e${b.radiusExponentValue.text}".formatToDouble(false)?.toFloat()
                    if (result1 != null && result2 != null && result3 != null) {
                        if (result3.isInfinite() || result3.isNaN()) {
                            act.showMessage(resources.getString(R.string.msg_num_out_range))
                        } else {
                            fsv.r.renderToTex = true
                        }
                    } else {
                        act.showMessage(resources.getString(R.string.msg_invalid_format))
                    }
                    val radiusStrings = "%e".format(Locale.US, f.texture.params.radius.u).split("e")
                    w.text = "%.2f".format(radiusStrings[0].toFloat())
                    b.radiusExponentValue.setText("%d".format(radiusStrings[1].toInt()))
                })
        b.radiusExponentValue.setOnEditorActionListener(
                editListener(null) { w: TextView ->
                    val result1 = b.radiusSignificandValue.text.toString().formatToDouble(false)
                    val result2 = w.text.toString().formatToDouble(false)
                    val result3 = "${b.radiusSignificandValue.text}e${w.text}".formatToDouble(false)?.toFloat()
                    if (result1 != null && result2 != null && result3 != null) {
                        if (result3.isInfinite() || result3.isNaN()) {
                            act.showMessage(resources.getString(R.string.msg_num_out_range))
                        } else {
                            fsv.r.renderToTex = true
                        }
                    } else {
                        act.showMessage(resources.getString(R.string.msg_invalid_format))
                    }
                    val radiusStrings = "%e".format(Locale.US, f.texture.params.radius.u).split("e")
                    b.radiusSignificandValue.setText("%.2f".format(radiusStrings[0].toFloat()))
                    w.text = "%d".format(radiusStrings[1].toInt())
                })


//        val bailoutStrings = "%e".format(Locale.US, f.bailoutRadius).split("e")
//        bailoutSignificandBar.progress = (bailoutSignificandBar.max*(bailoutStrings[0].toDouble() - 1.0)/8.99).toInt()
//        bailoutExponentBar.progress = bailoutStrings[1].toInt()
        loadRadius()

        b.radiusBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

//                val sig = progress.toDouble() / radiusSignificandBar.max * 8.99 + 1.0
//                val result1 = radiusValue.text.toString().formatToDouble(false)
//                val result2 = "${sig}e${radiusExponentEdit.text}".formatToDouble(false)?.toFloat()
//                if (result1 != null && result2 != null) {
//                    if (result2.isInfinite() || result2.isNaN()) {
//                        act.showMessage(resources.getString(R.string.msg_num_out_range))
//                    } else {
//                        f.texture.params.radius.u = result2
//                        if (fsv.r.renderProfile == RenderProfile.CONTINUOUS) {
//                            fsv.r.renderToTex = true
//                            fsv.requestRender()
//                        }
//                    }
//                } else {
//                    act.showMessage(resources.getString(R.string.msg_invalid_format))
//                }
//                val bailoutStrings = "%e".format(Locale.US, f.texture.params.radius.u).split("e")
                //bailoutSignificandEdit.setText("%.5f".format(bailoutStrings[0].toFloat()))

                if (fromUser) {
                    val p = progress.toDouble() / b.radiusBar.max
                    var newExponent = (MAX_RADIUS_EXPONENT * p).toInt()
                    var newSignificand = ((MAX_RADIUS_EXPONENT * p) % 1.0) * 9.0 + 1.0
                    if (f.shape.isConvergent) {
                        newExponent *= -1
                        newSignificand = 10.99 - newSignificand
                    }
                    val newRadius = newSignificand * 10.0.pow(newExponent)
                    val radiusStrings = "%e".format(Locale.US, newRadius).split("e")
                    b.radiusSignificandValue.setText("%.2f".format(newSignificand))
                    b.radiusExponentValue.setText("%d".format(newExponent))
                    if (fsv.r.renderProfile == RenderProfile.CONTINUOUS) {
                        fsv.r.renderToTex = true
                        fsv.requestRender()
                    }
                }

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
//        radiusExponentBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//
//                val result1 = radiusSignificandEdit.text.toString().formatToDouble(false)
//                val result2 = if (f.shape.isConvergent) {
//                    "${radiusSignificandEdit.text}e-$progress".formatToDouble(false)?.toFloat()
//                } else {
//                    "${radiusSignificandEdit.text}e$progress".formatToDouble(false)?.toFloat()
//                }
//                if (result1 != null && result2 != null) {
//                    if (result2.isInfinite() || result2.isNaN()) {
//                        act.showMessage(resources.getString(R.string.msg_num_out_range))
//                    } else {
//                        f.texture.params.radius.u = result2
//                        if (fsv.r.renderProfile == RenderProfile.CONTINUOUS) {
//                            fsv.r.renderToTex = true
//                            fsv.requestRender()
//                        }
//                    }
//                } else {
//                    act.showMessage(resources.getString(R.string.msg_invalid_format))
//                }
//                val bailoutStrings = "%e".format(Locale.US, f.texture.params.radius.u).split("e")
//                //bailoutSignificandEdit.setText("%.5f".format(bailoutStrings[0].toFloat()))
//                radiusExponentEdit.setText("%d".format(bailoutStrings[1].toInt()))
//
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {
//                if (sc.continuousParamRender) fsv.r.renderProfile = RenderProfile.CONTINUOUS
//            }
//
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//
//                fsv.r.renderProfile = RenderProfile.DISCRETE
//                fsv.r.renderToTex = true
//                fsv.requestRender()
//
//            }
//
//        })


        b.textureModeTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {}
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabSelected(tab: TabLayout.Tab) {

                f.textureRegion = TextureRegion.values()[tab.position]
                fsv.requestRender()

            }
        })
        // b.textureModeTabs.getTabAt(f.textureMode.ordinal)?.select()



        textureParamButtons.forEach { it.hide() }
        f.texture.params.list.forEachIndexed { index, param ->
            textureParamButtons[index].apply {
                show()
                text = if (param.name == "") "Param ${index + 1}" else param.name
            }
        }



        // CLICK LISTENERS


        b.textureListButton.setOnClickListener {

            crashlytics().updateLastAction(Action.TEXTURE_CHANGE)

            // save state on texture thumb render
            act.bookmarkAsPreviousFractal()

            with(b.textureListLayout.defaultList.adapter as ListAdapter<Texture>) {
                removeRange(0, itemCount)
                addItems(0, getTextureListItems())
                if (selectedPositions.isEmpty()) {
                    getFirstPositionOf(f.texture).let {
                        setActivatedPosition(it)
                        recyclerView?.scrollToPosition(it)
                    }
                }
            }

            // ui changes
            b.textureSubMenuButtons.hide()
            act.apply {
                hideCategoryButtons()
                hideMenuToggleButton()
                hideHeaderButtons()
            }

            setCurrentLayout(b.textureListLayout.root)
            b.textureNavBar.show()

            // texture thumbnail render
            handler.postDelayed({

                if (sc.textureListViewType == ListLayoutType.GRID && !fsv.r.validTextureThumbs) {

                    act.showThumbnailRenderDialog()

                    fsv.r.renderProfile = RenderProfile.TEXTURE_THUMB
                    fsv.r.renderAllThumbnails = true
                    fsv.requestRender()

                }

            }, BUTTON_CLICK_DELAY_MED)

        }


//        textureListViewTypeButton.setOnClickListener {
//
//            sc.textureListViewType = ListLayoutType.values().run {
//                get((sc.textureListViewType.ordinal + 1) % size)
//            }
//
//            textureListAdapter.updateLayoutType(sc.textureListViewType)
//
//            when (sc.textureListViewType) {
//                ListLayoutType.LINEAR -> {
//                    texturePreviewList.layoutManager = previewListLinearManager
//                }
//                ListLayoutType.GRID -> {
//                    texturePreviewList.layoutManager = previewListGridManager
////                    if (!fsv.r.textureThumbsRendered) {
////                        fsv.r.renderProfile = RenderProfile.TEXTURE_THUMB
////                        fsv.r.renderThumbnails = true
////                        fsv.requestRender()
////                    }
//                }
//            }
//
//        }
        b.textureListDoneButton.setOnClickListener {

            if (fsv.r.isRendering) fsv.r.pauseRender = true

            act.apply {
                showCategoryButtons()
                showMenuToggleButton()
                showHeaderButtons()
            }

            b.textureSubMenuButtons.show()
            b.textureNavBar.hide()
            b.textureModeButton.performClick()

            updateLayout()

            fsv.r.renderProfile = RenderProfile.DISCRETE

        }


        val textureParamButtonListener = { button: GradientButton, paramIndex: Int ->
            View.OnClickListener {
                if (button.showGradient && !sc.goldEnabled) act.showUpgradeScreen()
                else {
                    // act.showTouchIcon()
                    sc.editMode = EditMode.TEXTURE
                    val param = f.texture.params.list.getOrNull(paramIndex)
                    if (param != null) {
                        f.texture.params.active = param
                        setCurrentLayout(if (f.texture.params.active is ComplexParam) b.complexTextureParam.root else b.realTextureParam.root)
                        if (!button.showGradient) setCurrentButton(button)
                        loadActiveParam()
                    }
                }
            }
        }

        b.textureModeButton.setOnClickListener(subMenuButtonListener(b.textureModeLayout, b.textureModeButton))
        b.escapeRadiusButton.setOnClickListener {
            subMenuButtonListener(b.radiusLayout, b.escapeRadiusButton).onClick(null)
            loadRadius()
        }
        textureParamButtons.forEachIndexed { index, button ->
            button.setOnClickListener(textureParamButtonListener(button, index))
        }
        b.textureImageButton.setOnClickListener(subMenuButtonListener(b.textureImageLayout, b.textureImageButton, UiLayoutHeight.MED))

//        binsButton.setOnClickListener {
//
//            sc.editMode = EditMode.TEXTURE
//            f.texture.params.active = f.texture.bins
//            showLayout(realTextureParam)
//            //shapeResetButton.show()
//            alphaButton(binsButton)
//            loadActiveParam()
//
//        }


        button = b.textureModeButton
        layout = b.textureModeLayout
        b.textureModeLayout.hide()
        b.radiusLayout.hide()
        b.textureImageLayout.hide()
        b.realTextureParam.root.hide()
        b.complexTextureParam.root.hide()
        b.textureImageButton.hide()
        b.textureListLayout.listCustomButton.hide()

        b.textureListLayout.root.hide()
        b.textureNavBar.hide()


        setCurrentLayout(b.textureModeLayout)
        setCurrentButton(b.textureModeButton)

        updateLayout()
        crashlytics().setCustomKey(CRASH_KEY_FRAG_TEX_CREATED, true)

    }


    override fun onGoldEnabled() {
        textureListAdapter.notifyDataSetChanged()
        textureImageListAdapter.apply {
            notifyItemChanged(itemCount - 1)
        }
        listOf(
                b.textureParamButton1,
                b.textureParamButton2,
                b.textureParamButton3,
                b.textureParamButton4
        ).forEach { it.showGradient = false }
    }

    private fun getTextureListItems() : ArrayList<ListItem<Texture>> {

        val listItems = arrayListOf<ListItem<Texture>>()
        val emptyFavorite = ListItem(Texture.emptyFavorite, ListItemType.FAVORITE, R.layout.list_item_linear_empty_favorite)
        val emptyCustom = ListItem(Texture.emptyCustom, ListItemType.CUSTOM, R.layout.list_item_linear_empty_custom)

        f.shape.compatTextures.forEach { listItems.add(

                ListItem(
                        it, ListItemType.DEFAULT,
                        R.layout.other_list_item
                        // disabled = !f.shape.compatTextures.contains(it)
                )

        )}
        if (f.shape.compatTextures.none { it.isFavorite }) listItems.add(emptyFavorite)
        // if (Shape.custom.isEmpty()) listItems.add(emptyCustom)

        return listItems

    }

    fun highlightListItemType(adapter: ListAdapter<Texture>, index: Int) {
        adapter.apply {
            listOf(
                b.textureListLayout.listFavoritesButton,
                // textureListLayout.listCustomButton,
                b.textureListLayout.listDefaultButton
            ).forEachIndexed { i, b ->
                b.setTextColor(resources.getColor(if (index == i) R.color.colorDarkText else R.color.colorDarkTextMuted, null))
            }
        }
    }

    private fun confirmTextureImageResolution(sampleSize: Int, uri: Uri) {

        Log.e("TEXTURE", "sampleSize: $sampleSize")
        val inputStream = context?.contentResolver?.openInputStream(uri)
        val bmp = BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = sampleSize
        })
        inputStream?.close()
        Log.e("TEXTURE", "width: ${bmp?.width}, height: ${bmp?.height}")

        // make local copy of image
        val path = "$TEX_IM_PREFIX${Texture.CUSTOM_IMAGE_COUNT}.png"
        val fos = requireContext().openFileOutput(path, Context.MODE_PRIVATE)
        bmp?.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.close()

        Texture.CUSTOM_IMAGE_COUNT++
        val sp = requireContext().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        val edit = sp.edit()
        edit.putInt(TEX_IMAGE_COUNT, Texture.CUSTOM_IMAGE_COUNT)
        edit.apply()

        textureImageListAdapter.apply {
            val newPos = itemCount - 1
            if (addItem(newPos, TextureImageListItem(path = path))) {
                toggleSelection(newPos)
                notifyItemChanged(newPos)
            }
        }

        f.imagePath = path
        f.imageId = -1
        fsv.r.loadTextureImage = true
        fsv.r.renderToTex = true
        fsv.requestRender()

    }

    fun loadRadius(updateProgress: Boolean = true) {

        val radiusStrings = "%e".format(Locale.US, f.texture.params.radius.u).split("e")
        val significand = radiusStrings.getOrNull(0)?.toFloat() ?: 1f
        val exponent = radiusStrings.getOrNull(1)?.toInt() ?: 2
        b.radiusSignificandValue.setText("%.2f".format(significand))
        b.radiusExponentValue.setText("%d".format(exponent))
        if (updateProgress) {
            b.radiusBar.progress =
                if (f.shape.isConvergent)   ((-exponent + (10.0 - significand)/9.0)/12.0*b.radiusBar.max).toInt()
                else                        ((exponent + (significand - 1.0)/9.0)/12.0*b.radiusBar.max).toInt()
        }

    }

    fun updateParamText() {
        val param = f.texture.params.active
        if (param is ComplexParam) {
            b.complexTextureParam.apply {
                uValue.setText(param.u.format(COMPLEX_PARAM_DIGITS))
                vValue.setText(param.v.format(COMPLEX_PARAM_DIGITS))
            }
        }
        else {
            b.realTextureParam.apply {
                uValue2.setText(param.u.format(REAL_PARAM_DIGITS))
            }
        }
    }

    fun loadActiveParam() {
        val param = f.texture.params.active
        if (param is ComplexParam) {
            b.complexTextureParam.apply {
                uValue.setText(param.u.format(COMPLEX_PARAM_DIGITS))
                uLock.isChecked = param.uLocked
                vValue.setText(param.v.format(COMPLEX_PARAM_DIGITS))
                vLock.isChecked = param.vLocked
                complexParamSensitivity.sensitivityValue.setText("%d".format(param.sensitivity.toInt()))
            }
        }
        else {
            if (param.nameId == R.string.density && !sc.autofitColorRange) param.u = param.uRange.lower
            b.realTextureParam.apply {
                uValue2.setText(param.u.format(REAL_PARAM_DIGITS))
                realParamSensitivity.sensitivityValue.setText("%d".format(param.sensitivity.toInt()))
            }
        }
    }

    fun selectMenuButton(button: Button?, layout: View?) {
        setCurrentLayout(layout)
        setCurrentButton(button)
    }

    override fun updateLayout() {

        val textureParamButtons = listOf(
                b.textureParamButton1,
                b.textureParamButton2,
                b.textureParamButton3,
                b.textureParamButton4
        )

        textureParamButtons.forEach { it.hide() }
        f.texture.params.list.forEachIndexed { index, param ->
            textureParamButtons[index].apply {
                text = if (param.name == "") "Param ${index + 1}" else param.name
                show()
                if (param.goldFeature && !sc.goldEnabled) {
                    showGradient = true
                    alpha = 1f
                } else {
                    showGradient = false
                    alpha = 0.4f
                }
            }
        }
        if (f.texture.hasRawOutput) b.textureImageButton.show()
        else b.textureImageButton.hide()

        if (f.texture.hasRawOutput) {
            textureImageListAdapter.apply {
                currentItems.forEachIndexed { i, item ->
                    if ((f.imagePath != "" && f.imagePath == item.path) || (f.imageId != -1 && f.imageId == item.id)) {
                        toggleSelection(i)
                        notifyItemChanged(i)
                    }
                }
            }
        }

        b.textureButtonsScroll.requestLayout()
        b.textureButtonsScrollLayout.requestLayout()
        b.textureButtonsScrollLayout.invalidate()

        // textureModeTabs[f.textureMode.ordinal - 1].performClick()
        loadRadius(updateProgress = true)
        if (f.texture.params.list.isEmpty()) selectMenuButton(b.escapeRadiusButton, b.radiusLayout) else loadActiveParam()

    }

    override fun updateValues() {
        loadActiveParam()
        loadRadius(updateProgress = false)
    }

}