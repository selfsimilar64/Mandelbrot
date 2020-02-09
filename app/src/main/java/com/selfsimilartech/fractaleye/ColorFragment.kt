package com.selfsimilartech.fractaleye

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.os.Handler
import com.google.android.material.tabs.TabLayout
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlinx.android.synthetic.main.color_fragment.*
import java.text.NumberFormat
import java.text.ParseException


class ColorFragment : Fragment() {

    private val nf = NumberFormat.getInstance()

    private fun String.formatToDouble() : Double? {
        var d : Double? = null
        try { d = nf.parse(this)?.toDouble() }
        catch (e: ParseException) {
            val act =activity as MainActivity
            act.showMessage(resources.getString(R.string.msg_invalid_format))
        }
        return d
    }

    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.color_fragment, container, false)

    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {

        val act = activity as MainActivity
        val f = act.f
        val fsv = act.fsv

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
            fsv.updateSystemUI()
            true

        }}


        frequencyEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()?.toFloat()
            if (result != null) {
                f.frequency = result
            }
            w.text = "%.5f".format(f.frequency)
        })

        phaseEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            val result = w.text.toString().formatToDouble()?.toFloat()
            if (result != null) {
                f.phase = result
            }
            w.text = "%.5f".format(f.phase)
        })



        val handler = Handler()

        paletteCard.foreground = f.palette.getGradientDrawable(resources)
        colorPreviewName.text = resources.getString(f.palette.name)
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

            paletteCard.foreground = f.palette.getGradientDrawable(resources)
            colorPreviewName.text = resources.getString(f.palette.name)
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

                f.solidFillColor = when (tab.position) {
                    0 -> R.color.black
                    1 -> R.color.white
                    else -> R.color.cyan
                }

                fsv.requestRender()

            }
        })
        solidFillColorTabs.getTabAt(1)?.select()




        colorLayout.removeView(colorPreviewListLayout)
        colorPreviewListLayout.visibility = RecyclerView.VISIBLE



        val thumbRes = Resolution.THUMB.scaleRes(fsv.screenRes)
        ColorPalette.all.forEach {
            if (it.thumbnail == null) {
                it.thumbnail = Bitmap.createBitmap(thumbRes[0], thumbRes[0], Bitmap.Config.ARGB_8888)
            }
        }
        act.updateColorEditTexts()
        super.onViewCreated(v, savedInstanceState)

    }

}