package com.selfsimilartech.fractaleye

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import com.google.android.material.tabs.TabLayout
import android.util.Log
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.texture_fragment.*
import java.util.*
import kotlin.math.floor
import kotlin.math.roundToInt


class TextureFragment : MenuFragment() {

    private lateinit var act : MainActivity
    private lateinit var f : Fractal
    private lateinit var fsv : FractalSurfaceView
    private lateinit var sc : SettingsConfig

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.texture_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        act = activity as MainActivity
        f = act.f
        fsv = act.fsv
        sc = act.sc

        textureLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        texturePreviewListLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)


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

        val handler = Handler()
        texturePreviewName.text = resources.getString(f.texture.displayName)


        val previewListWidth = fsv.r.screenRes.x - 2*resources.getDimension(R.dimen.categoryPagerMarginHorizontal) - resources.getDimension(R.dimen.navButtonSize)
        val previewGridWidth = resources.getDimension(R.dimen.textureShapePreviewSize) + 2*resources.getDimension(R.dimen.previewGridPaddingHorizontal)
        //Log.e("COLOR FRAGMENT", "texturePreviewListWidth: $previewListWidth")
        //Log.e("COLOR FRAGMENT", "texturePreviewGridWidth: $previewGridWidth")
        val spanCount = floor(previewListWidth.toDouble() / previewGridWidth).toInt()
        //Log.e("COLOR FRAGMENT", "spanCount: ${previewListWidth.toDouble() / previewGridWidth}")


        val textureParamButtons = listOf(
                textureParamButton1,
                textureParamButton2
        )


//        textureParamLayout.hide()
//        textureParamButtons.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
//            override fun onTabReselected(tab: TabLayout.Tab) {}
//            override fun onTabUnselected(tab: TabLayout.Tab) {}
//            override fun onTabSelected(tab: TabLayout.Tab) {
//
//                val param = f.texture.params[tab.position]
//                Log.d("TEXTURE FRAGMENT", "${resources.getString(param.name)} -- value: $param, progress: ${param.progress}, interval: ${param.interval.toInt()}")
//                qBar.max = if (param.discrete) param.interval.toInt() else 100
//                Log.d("TEXTURE FRAGMENT", "qBar max: ${qBar.max}")
//                qBar.progress = (param.progress*qBar.max.toDouble()).roundToInt()
//                Log.d("TEXTURE FRAGMENT", "${param.progress}")
//
//            }
//        })
        qEdit.setOnEditorActionListener(editListener(null) { w: TextView ->

            val param = f.texture.activeParam
            val result = w.text.toString().formatToDouble()
            if (result != null) {
                param.q = result
                fsv.r.renderToTex = true
            }
            param.setProgressFromValue()

            w.text = param.toString()
            qSeekBar.progress = (param.progress*qSeekBar.max.toDouble()).toInt()
            // Log.d("TEXTURE FRAGMENT", "param progress : ${param.progress}")
            // Log.d("TEXTURE FRAGMENT", "qBar max : ${qBar.max}")

        })
        qSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            fun updateEditTextOnly() {

                // update EditText but not param
                val param = f.texture.activeParam
                val progressNormal = qSeekBar.progress.toDouble()/qSeekBar.max
                qEdit.setText(param.toString((1.0 - progressNormal)*param.min + progressNormal*param.max))

            }
            fun updateEditTextAndParam() {

                // update EditText and param -- then render
                val param = f.texture.activeParam
                param.progress = qSeekBar.progress.toDouble()/qSeekBar.max
                param.setValueFromProgress()
                qEdit.setText(param.toString())

                fsv.r.renderToTex = true
                fsv.requestRender()

            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                if (sc.continuousRender) updateEditTextAndParam()
                else updateEditTextOnly()

            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                updateEditTextAndParam()

            }

        })
        if (f.texture.numParamsInUse != 0) {
            qSeekBar.max = if (f.texture.params[0].discrete)
                f.texture.params[0].interval.toInt() else 100
        }



        val previewListLinearManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val previewListLinearAdapter = TextureAdapter(f.shape.compatTextures, R.layout.texture_preview_item_linear)
        val previewListGridManager = GridLayoutManager(context, spanCount, GridLayoutManager.VERTICAL, false)
        val previewListGridAdapter = TextureAdapter(f.shape.compatTextures, R.layout.texture_shape_preview_item_grid)
        when (sc.textureListViewType) {
            ListLayoutType.LINEAR -> {
                texturePreviewList.adapter = previewListLinearAdapter
                texturePreviewList.layoutManager = previewListLinearManager
            }
            ListLayoutType.GRID -> {
                texturePreviewList.adapter = previewListGridAdapter
                texturePreviewList.layoutManager = previewListGridManager
            }
        }



        texturePreviewList.addOnItemTouchListener(
                RecyclerTouchListener(
                        v.context,
                        texturePreviewList,
                        object : ClickListener {

                            override fun onClick(view: View, position: Int) {

                                if (f.shape.compatTextures[position] != f.texture) {

                                    if (fsv.r.isRendering) fsv.r.interruptRender = true

                                    f.texture = f.shape.compatTextures[position]

                                    textureParamButtons.forEach { it.hide() }
                                    f.texture.params.forEachIndexed { index, param ->

                                        if (BuildConfig.PAID_VERSION || !param.proFeature) {
                                            textureParamButtons[index].apply {
                                                show()
                                                text = resources.getString(param.name)
                                            }
                                        }

                                    }

                                    //val bailoutStrings = "%e".format(Locale.US, f.bailoutRadius).split("e")
                                    //escapeRadiusSeekBar.progress = bailoutStrings[1].toInt()

                                    fsv.r.calcNewTextureSpan = true

                                    fsv.r.renderShaderChanged = true
                                    fsv.r.renderToTex = true
                                    fsv.requestRender()

                                    act.updateTextureEditTexts()

                                }

                            }

                            override fun onLongClick(view: View, position: Int) {}

                        }
                )
        )
//        previewList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                val rv = recyclerView as CustomRecyclerView
//                rv.updateRenderStates()
//                if (!rv.isRenderingFromQueue && rv.renderQueue.isNotEmpty()) {
//                    act?.renderNextTextureThumbnail()
//                    rv.isRenderingFromQueue = true
//                }
//            }
//
//        })




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


        val bailoutStrings = "%e".format(Locale.US, f.bailoutRadius).split("e")
        bailoutSignificandBar.progress = (bailoutSignificandBar.max*(bailoutStrings[0].toDouble() - 1.0)/8.99).toInt()
        bailoutExponentBar.progress = bailoutStrings[1].toInt()

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
                        if (fsv.r.renderProfile == RenderProfile.MANUAL && sc.continuousRender) {
                            //Log.e("TEXTURE", "escape radius seekbar subroutine")
                            fsv.r.renderToTex = true
                            fsv.requestRender()
                        }
                        //fsv.r.renderToTex = true
                    }
                }
                else {
                    act.showMessage(resources.getString(R.string.msg_invalid_format))
                }
                val bailoutStrings = "%e".format(Locale.US, f.bailoutRadius).split("e")
                //bailoutSignificandEdit.setText("%.5f".format(bailoutStrings[0].toFloat()))
                bailoutSignificandEdit.setText("%.2f".format(bailoutStrings[0].toFloat()))

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                fsv.r.renderToTex = true
                fsv.requestRender()

            }

        })
        bailoutExponentBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                val result1 = bailoutSignificandEdit.text.toString().formatToDouble(false)
                val result2 = "${bailoutSignificandEdit.text}e$progress".formatToDouble(false)?.toFloat()
                if (result1 != null && result2 != null) {
                    if (result2.isInfinite() || result2.isNaN()) {
                        act.showMessage(resources.getString(R.string.msg_num_out_range))
                    }
                    else {
                        f.bailoutRadius = result2
                        if (fsv.r.renderProfile == RenderProfile.MANUAL && sc.continuousRender) {
                            //Log.e("TEXTURE", "escape radius seekbar subroutine")
                            fsv.r.renderToTex = true
                            fsv.requestRender()
                        }
                        //fsv.r.renderToTex = true
                    }
                }
                else {
                    act.showMessage(resources.getString(R.string.msg_invalid_format))
                }
                val bailoutStrings = "%e".format(Locale.US, f.bailoutRadius).split("e")
                //bailoutSignificandEdit.setText("%.5f".format(bailoutStrings[0].toFloat()))
                bailoutExponentEdit.setText("%d".format(bailoutStrings[1].toInt()))

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

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
                text = resources.getString(param.name)
            }
        }



        // CLICK LISTENERS
        texturePreviewLayout.setOnClickListener {
            handler.postDelayed({

                with (texturePreviewList.adapter as TextureAdapter) {
                    if (textureList != f.shape.compatTextures) {
                        textureList = f.shape.compatTextures
                        notifyDataSetChanged()
                    }
                }

                textureSubMenuButtons.hide()
                texturePreviewLayout.hide()
                act.hideCategoryButtons()

                textureNavBar.show()
                texturePreviewListLayout.show()

                act.uiSetHeight(resources.getDimension(R.dimen.uiLayoutHeightTall).toInt())

                with (texturePreviewList.adapter as TextureAdapter) { if (isGridLayout) {
                    if (!fsv.r.textureThumbsRendered) {
                        fsv.r.renderProfile = RenderProfile.TEXTURE_THUMB
                        fsv.r.renderThumbnails = true
                        fsv.requestRender()
                    }
                }}

            }, BUTTON_CLICK_DELAY_LONG)
        }

        textureListViewTypeButton.setOnClickListener {

            sc.textureListViewType = ListLayoutType.values().run {
                get((sc.textureListViewType.ordinal + 1) % size)
            }

            when (sc.textureListViewType) {
                ListLayoutType.LINEAR -> {
                    texturePreviewList.adapter = previewListLinearAdapter
                    texturePreviewList.layoutManager = previewListLinearManager
                }
                ListLayoutType.GRID -> {
                    texturePreviewList.adapter = previewListGridAdapter
                    texturePreviewList.layoutManager = previewListGridManager
                }
            }

            with (texturePreviewList.adapter as TextureAdapter) {
                if (textureList != f.shape.compatTextures) {
                    textureList = f.shape.compatTextures
                    notifyDataSetChanged()
                }
            }


        }
        textureDoneButton.setOnClickListener {

            if (fsv.r.isRendering) fsv.r.pauseRender = true

            texturePreviewName.text = resources.getString(f.texture.displayName)

            act.showCategoryButtons()
            if (!act.uiIsClosed()) act.uiSetOpen()
            else MainActivity.Category.TEXTURE.onMenuClosed(act)

            texturePreviewListLayout.hide()
            texturePreviewLayout.show()
            textureSubMenuButtons.show()
            textureNavBar.hide()

            fsv.r.renderProfile = RenderProfile.MANUAL

        }



        val subMenuButtonListener = { layout: View, button: Button ->
            View.OnClickListener {
                showLayout(layout)
                alphaButton(button)
            }
        }
        val textureParamButtonListener = { button: Button, paramIndex: Int ->
            View.OnClickListener {
                fsv.r.reaction = Reaction.SHAPE
                f.texture.activeParam = f.texture.params[paramIndex]
                showLayout(textureParamLayout)
                //shapeResetButton.show()
                alphaButton(button)
                loadActiveParam()
            }
        }

        //texturePreviewButton.setOnClickListener(subMenuButtonListener(texturePreviewLayout, texturePreviewButton))
        texturePreviewButton.setOnClickListener {
            showLayout(texturePreviewLayout)
            alphaButton(texturePreviewButton)
            texturePreviewName.alpha = if (sc.hardwareProfile == HardwareProfile.CPU) 0.3f else 1f
        }
        textureModeButton.setOnClickListener(subMenuButtonListener(textureModeLayout, textureModeButton))
        escapeRadiusButton.setOnClickListener(subMenuButtonListener(escapeRadiusLayout, escapeRadiusButton))
        textureParamButtons.forEachIndexed { index, button ->
            button.setOnClickListener(textureParamButtonListener(button, index))
        }


        currentButton = texturePreviewButton
        currentLayout = texturePreviewLayout
        texturePreviewLayout.hide()
        textureModeLayout.hide()
        escapeRadiusLayout.hide()
        textureParamLayout.hide()

        texturePreviewListLayout.hide()
        textureNavBar.hide()


        texturePreviewButton.performClick()


        val thumbRes = Resolution.THUMB.scaleRes(fsv.r.screenRes)
        Texture.all.forEach {
            if (it.thumbnail == null) {
                it.thumbnail = Bitmap.createBitmap(thumbRes.x, thumbRes.x, Bitmap.Config.ARGB_8888)
            }
        }
        act.updateTextureEditTexts()
        super.onViewCreated(v, savedInstanceState)
    
    }
    
    fun loadActiveParam() {

        val param = f.texture.activeParam
        qEdit.setText(param.toString())
        qSeekBar.max = if (param.discrete) param.interval.toInt() else 1000
        //Log.d("TEXTURE FRAGMENT", "qSeekBar max: ${qSeekBar.max}")
        qSeekBar.progress = (param.progress*qSeekBar.max.toDouble()).roundToInt()
        //Log.d("TEXTURE FRAGMENT", "${param.progress}")
        
    }

}