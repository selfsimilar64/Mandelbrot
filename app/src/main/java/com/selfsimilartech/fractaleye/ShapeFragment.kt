package com.selfsimilartech.fractaleye

import android.content.Context
import android.content.res.ColorStateList
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.os.Handler
import androidx.constraintlayout.widget.ConstraintLayout
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
import kotlin.math.log
import kotlin.math.pow
import kotlinx.android.synthetic.main.shape_fragment.*
import java.text.NumberFormat
import java.text.ParseException


class ShapeFragment : Fragment() {

    private val nf = NumberFormat.getInstance()

    private fun String.formatToDouble() : Double? {
        var d : Double? = null
        try { d = nf.parse(this)?.toDouble() }
        catch (e: ParseException) {
            val act = activity as MainActivity
            act.showMessage(resources.getString(R.string.msg_invalid_format))
        }
        return d
    }


    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.shape_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        val act = activity as MainActivity
        val f = act.f
        val fsv = act.fsv
        val sc = act.sc
        val shapeList = if (BuildConfig.PAID_VERSION) Shape.all else Shape.all.filter { shape -> !shape.proFeature }

        val handler = Handler()
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
                    Log.d("SHAPE FRAGMENT", "some other action")
                }
            }

            fsv.requestRender()
            editText.clearFocus()
            act.updateSystemUI()
            true

        }}
        val lockListener = { j: Int -> View.OnClickListener {
            val lock = it as ToggleButton
            when (j) {
                0 -> f.shape.params.active.uLocked = lock.isChecked
                1 -> f.shape.params.active.vLocked = lock.isChecked
            }
        }}
        val linkListener = View.OnClickListener {
            val link = it as ToggleButton
            link.foregroundTintList = ColorStateList.valueOf(resources.getColor(
                    if (link.isChecked) R.color.white else R.color.colorDarkSelected, null
            ))
            f.shape.params.active.linked = link.isChecked
            if (link.isChecked) {
                act.updateShapeEditTexts()
                fsv.r.renderToTex = true
                fsv.requestRender()
            }
        }
        val juliaListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->

            val prevScale = f.position.scale
            f.shape.juliaMode = isChecked
            f.position = if (isChecked) f.shape.positions.julia else f.shape.positions.default
            fsv.r.checkThresholdCross(prevScale)

            if (f.shape.juliaMode) {

                shapeParamLayout.visibility = ConstraintLayout.VISIBLE
                // complexMapKatex.setDisplayText(resources.getString(f.shape.katex).format("P${f.shape.numParamsInUse + 1}"))
                shapeParamButtons.addTab(shapeParamButtons.newTab().setText(resources.getString(R.string.julia)))
                shapeParamButtons.getTabAt(shapeParamButtons.tabCount - 1)?.select()
                handler.postDelayed({
                    shapeLayoutScroll.smoothScrollTo(0, shapeParamLayout.y.toInt())
                }, BUTTON_CLICK_DELAY_LONG)
                if (f.shape.numParamsInUse == 1) {
                    fsv.r.reaction = Reaction.SHAPE
                    act.showTouchIcon()
                }
            }
            else {

                shapeParamButtons.removeTabAt(shapeParamButtons.tabCount - 1)
                if (f.shape.numParamsInUse == 0) shapeParamLayout.visibility = ConstraintLayout.GONE
                // complexMapKatex.setDisplayText(resources.getString(f.shape.katex).format("c"))
                if (f.shape.numParamsInUse == 0) fsv.r.reaction = Reaction.NONE

            }

            Log.d("SHAPE FRAGMENT", "numParamsInUse: ${f.shape.numParamsInUse}")


            act.updateShapeEditTexts()
            act.updatePositionEditTexts()
            act.updateDisplayParams(reactionChanged = true)

            fsv.r.renderShaderChanged = true
            fsv.r.renderToTex = true
            fsv.requestRender()

        }


        val shapeParamButtonValues = listOf(R.string.param1, R.string.param2, R.string.param3)

        shapeParamButtons.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {

                f.shape.params.active =
                        if (tab.text.toString() == resources.getString(R.string.julia)) f.shape.params.julia
                        else f.shape.params.at(tab.position)

                val param = f.shape.params.active
                uEdit.setText("%.8f".format(param.u))
                vEdit.setText("%.8f".format(param.v))
                uLock.isChecked = param.uLocked
                vLock.isChecked = param.vLocked
                linkParamButton.isChecked = param.linked
                linkParamButton.foregroundTintList = ColorStateList.valueOf(resources.getColor(
                        if (linkParamButton.isChecked) R.color.white else R.color.colorDarkSelected, null
                ))

                act.showTouchIcon()

            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) { onTabSelected(tab) }

        })
        shapeResetButton.setOnClickListener {

            f.shape.params.active.reset()
            act.updateShapeEditTexts()
            fsv.r.renderToTex = true
            fsv.requestRender()

        }




        uEdit.setText("%.8f".format(f.shape.params.active.u))
        vEdit.setText("%.8f".format(f.shape.params.active.v))
        uEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = "${w.text}".formatToDouble()
            if (result != null) {
                f.shape.params.active.u = result
                fsv.r.renderToTex = true
            }
            w.text = "%.8f".format((f.shape.params.active.u))
            if (f.shape.params.active.linked) {
                vEdit.setText("%.8f".format((f.shape.params.active.v)))
            }
        })
        vEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = "${w.text}".formatToDouble()
            if (result != null) {
                f.shape.params.active.v = result
                fsv.r.renderToTex = true
            }
            w.text = "%.8f".format((f.shape.params.active.v))
        })
        uLock.setOnClickListener(lockListener(0))
        vLock.setOnClickListener(lockListener(1))
        linkParamButton.setOnClickListener(linkListener)


        val maxIterBar = v.findViewById<SeekBar>(R.id.maxIterBar)
        val maxIterEdit = v.findViewById<EditText>(R.id.maxIterEdit)
        maxIterBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

                val p = seekBar.progress.toDouble() / 100.0
                val iter = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW).toInt()
                maxIterEdit.setText("%d".format(iter))

            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {

                val p = seekBar.progress.toDouble() / 100.0
                f.maxIter = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW).toInt()
                maxIterEdit.setText("%d".format(f.maxIter))

                Log.d("FRACTAL EDIT FRAGMENT", "maxIter: ${f.maxIter}")
                // f.maxIter = ((2.0.pow(5) - 1)*(1.0f - p) + (2.0.pow(12) - 1)*p).toInt()
                fsv.r.renderToTex = true
                fsv.requestRender()

            }

        })
        maxIterBar.progress = ((log(f.maxIter.toDouble(), 2.0) - ITER_MIN_POW)/(ITER_MAX_POW - ITER_MIN_POW)*100.0).toInt()
        maxIterEdit.setText("%d".format(f.maxIter))
        maxIterEdit.setOnEditorActionListener(editListener(null) {
            val result = "${it.text}".formatToDouble()?.toInt()
            if (result != null) {
                f.maxIter = result
                fsv.r.renderToTex = true
            }
            maxIterEdit.setText("%d".format(f.maxIter))
        })



        shapePreviewImage.setImageResource(f.shape.icon)
        shapePreviewText.text = resources.getString(f.shape.name)



        if (f.shape.juliaMode) juliaLayout.visibility = LinearLayout.GONE
        if (f.shape.juliaMode) juliaModeSwitch.isChecked = true
        juliaModeSwitch.setOnCheckedChangeListener(juliaListener)


        // create and set preview list adapter/manager
        val shapePreviewGridAdapter = ShapeAdapter(shapeList, R.layout.texture_shape_preview_item_grid)
        val shapePreviewGridManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
        val shapePreviewLinearAdapter = ShapeAdapter(shapeList, R.layout.shape_preview_item_linear)
        val shapePreviewLinearManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        when (sc.shapeListViewType) {

            ListLayoutType.GRID -> {
                shapePreviewList.adapter = shapePreviewGridAdapter
                shapePreviewList.layoutManager = shapePreviewGridManager
            }
            ListLayoutType.LINEAR -> {
                shapePreviewList.adapter = shapePreviewLinearAdapter
                shapePreviewList.layoutManager = shapePreviewLinearManager
            }

        }

        shapePreviewList.addOnItemTouchListener(RecyclerTouchListener(
                v.context,
                shapePreviewList,
                object : ClickListener {

                    override fun onClick(view: View, position: Int) {

                        if (shapeList[position] != f.shape) {


                            // reset texture if not compatible with new shape
                            if (!shapeList[position].compatTextures.contains(f.texture)) {
                                f.texture = Texture.exponentialSmoothing
                                act.updateTexturePreviewName()
                            }

                            val prevScale = f.position.scale
                            f.shape = shapeList[position]


                            // update juliaModeSwitch
                            juliaLayout.visibility =
                                    if (f.shape.juliaModeInit) LinearLayout.GONE
                                    else LinearLayout.VISIBLE
                            if (f.shape.juliaMode != juliaModeSwitch.isChecked) {
                                juliaModeSwitch.setOnCheckedChangeListener { _, _ -> }
                                juliaModeSwitch.isChecked = f.shape.juliaMode
                                juliaModeSwitch.setOnCheckedChangeListener(juliaListener)
                            }

                            if (f.shape.numParamsInUse == 0) fsv.r.reaction = Reaction.NONE
                            else {
                                fsv.r.reaction = Reaction.SHAPE
                                act.showTouchIcon()
                            }


                            // update parameter display
                            shapeParamLayout.visibility = if (f.shape.numParamsInUse == 0)
                                ConstraintLayout.GONE else ConstraintLayout.VISIBLE
                            shapeParamButtons.removeAllTabs()
                            for (i in 0 until f.shape.params.size) shapeParamButtons.addTab(
                                    shapeParamButtons.newTab().setText(shapeParamButtonValues[i])
                            )
                            if (f.shape.juliaMode) {
                                shapeParamButtons.addTab(
                                    shapeParamButtons.newTab().setText(resources.getString(R.string.julia))
                                )
                            }

                            fsv.r.checkThresholdCross(prevScale)

                            fsv.r.renderShaderChanged = true
                            fsv.r.renderToTex = true
                            fsv.requestRender()

                            act.updatePositionEditTexts()

                        }

                    }
                    override fun onLongClick(view: View, position: Int) {}

                }
        ))



        shapeParamButtons.removeAllTabs()
        for (i in 0 until f.shape.params.size) shapeParamButtons.addTab(
                shapeParamButtons.newTab().setText(shapeParamButtonValues[i])
        )
        if (f.shape.juliaMode) {
            shapeParamButtons.addTab(
                    shapeParamButtons.newTab().setText(resources.getString(R.string.julia))
            )
        }
        if (f.shape.numParamsInUse == 0) shapeParamLayout.visibility = ConstraintLayout.GONE







        // CLICK LISTENERS
        shapePreview.setOnClickListener {
            handler.postDelayed({

                shapeContent.hide()
                shapePreviewListLayout.show()

                act.hideCategoryButtons()
                shapeNavButtons.show()

                //shapePreviewList.adapter?.notifyDataSetChanged()


            }, BUTTON_CLICK_DELAY_LONG)
        }

        shapeListViewTypeButton.setOnClickListener {

            sc.shapeListViewType = ListLayoutType.values().run {
                get((sc.shapeListViewType.ordinal + 1) % size)
            }

            when (sc.shapeListViewType) {

                ListLayoutType.LINEAR -> {
                    shapePreviewList.adapter = shapePreviewLinearAdapter
                    shapePreviewList.layoutManager = shapePreviewLinearManager
                }
                ListLayoutType.GRID -> {
                    shapePreviewList.adapter = shapePreviewGridAdapter
                    shapePreviewList.layoutManager = shapePreviewGridManager
                }

            }

        }
        shapeDoneButton.setOnClickListener {
            handler.postDelayed({

                shapePreviewImage.setImageResource(f.shape.icon)
                shapePreviewText.text = resources.getString(f.shape.name)

                shapePreviewListLayout.hide()
                shapeContent.show()
                act.showCategoryButtons()
                shapeNavButtons.hide()

            }, BUTTON_CLICK_DELAY_SHORT)
        }


        shapePreviewListLayout.hide()
        shapeNavButtons.hide()




        act.updateShapeEditTexts()
        super.onViewCreated(v, savedInstanceState)

    }

}