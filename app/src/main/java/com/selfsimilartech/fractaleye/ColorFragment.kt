package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.support.v4.app.Fragment
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.TabLayout
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlinx.android.synthetic.main.color_fragment.*


class ColorFragment : Fragment() {

    // Store instance variables
    private lateinit var f: Fractal
    private lateinit var fsv: FractalSurfaceView

    // newInstance constructor for creating fragment with arguments
    fun passArguments(f: Fractal, fsv: FractalSurfaceView) {
        this.f = f
        this.fsv = fsv
    }

    fun String.formatToDouble() : Double? {
        var d : Double? = null
        try { d = this.toDouble() }
        catch (e: NumberFormatException) {
            val act = if (activity is MainActivity) activity as MainActivity else null
            act?.showMessage("Invalid number format")
        }
        return d
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val act = if (activity is MainActivity) activity as MainActivity else null
        if (!this::f.isInitialized) f = act!!.f
        if (!this::fsv.isInitialized) fsv = act!!.fsv
        super.onCreate(savedInstanceState)
    }


    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.color_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        val act = if (activity is MainActivity) activity as MainActivity else null

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
            // act?.onWindowFocusChanged(true)
            true

        }}


        frequencyEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            f.frequency = w.text.toString().formatToDouble()?.toFloat() ?: f.frequency
            w.text = "%.5f".format(f.frequency)
        })

        phaseEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            f.phase = w.text.toString().formatToDouble()?.toFloat() ?: f.phase
            w.text = "%.5f".format(f.phase)
        })



        val handler = Handler()
        colorPreviewImage.setImageDrawable(GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                ColorPalette.getColors(resources, f.palette.ids)
        ))
        colorPreviewName.text = f.palette.name
        colorPreview.setOnClickListener {

            handler.postDelayed({

                colorLayout.addView(colorPreviewListLayout)
                colorContent.visibility = LinearLayout.GONE

                fsv.renderProfile = RenderProfile.COLOR_THUMB
                fsv.r.renderThumbnails = true
                fsv.r.renderToTex = true
                fsv.requestRender()

            }, BUTTON_CLICK_DELAY)

        }

        colorDoneButton.setOnClickListener {

            colorPreviewImage.setImageDrawable(GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    ColorPalette.getColors(resources, f.palette.ids)
            ))
            colorPreviewName.text = f.palette.name
            colorLayout.removeView(colorPreviewListLayout)
            colorContent.visibility = LinearLayout.VISIBLE
            fsv.renderProfile = RenderProfile.MANUAL

        }


        // previewList.layoutManager = GridLayoutManager(context, 3)
        colorPreviewList.adapter = ColorPaletteAdapter(ColorPalette.all)
        colorPreviewList.addOnItemTouchListener(
                RecyclerTouchListener(
                        v.context,
                        colorPreviewList,
                        object : ClickListener {

                            override fun onClick(view: View, position: Int) {

                                if (ColorPalette.all[position] != f.palette) {
                                    f.palette = ColorPalette.all[position]
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

                f.solidFillColor = when (tab.text.toString()) {
                    "black" -> R.color.black
                    "white" -> R.color.white
                    else -> R.color.cyan
                }

                fsv.requestRender()

            }
        })
        solidFillColorTabs.getTabAt(1)?.select()




        colorLayout.removeView(colorPreviewListLayout)
        colorPreviewListLayout.visibility = RecyclerView.VISIBLE


        act?.updateColorEditTexts()
        super.onViewCreated(v, savedInstanceState)

    }

}