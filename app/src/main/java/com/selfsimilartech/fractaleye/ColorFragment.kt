package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.support.v4.app.Fragment
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*


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
            val toast = Toast.makeText(context, "Invalid number format", Toast.LENGTH_LONG)
            toast.setGravity(Gravity.BOTTOM, 0, 20)
            toast.show()
        }
        return d
    }


    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.color_fragment, container, false)

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
            true

        }}

        val layout = v.findViewById<LinearLayout>(R.id.colorLayout)
        val preview = v.findViewById<LinearLayout>(R.id.colorPreview)
        val previewImage = v.findViewById<ImageView>(R.id.colorPreviewImage)
        val previewText = v.findViewById<TextView>(R.id.colorPreviewName)
        val previewList = v.findViewById<RecyclerView>(R.id.colorPreviewList)
        val previewListLayout = v.findViewById<LinearLayout>(R.id.colorPreviewListLayout)
        val doneButton = v.findViewById<Button>(R.id.colorDoneButton)
        val content = v.findViewById<LinearLayout>(R.id.colorContent)

        val frequencyEdit = v.findViewById<EditText>(R.id.frequencyEdit)
        frequencyEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            f.frequency = w.text.toString().formatToDouble()?.toFloat() ?: f.frequency
            w.text = "%.5f".format(f.frequency)
        })

        val phaseEdit = v.findViewById<EditText>(R.id.phaseEdit)
        phaseEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
            f.phase = w.text.toString().formatToDouble()?.toFloat() ?: f.phase
            w.text = "%.5f".format(f.phase)
        })



        previewImage.setImageDrawable(GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                f.palette.getColors(resources, f.palette.ids)
        ))
        previewText.text = f.palette.name
        preview.setOnClickListener {

            layout.addView(previewListLayout)
            content.visibility = LinearLayout.GONE

            fsv.renderProfile = RenderProfile.COLOR_THUMB
            fsv.r.renderThumbnails = true
            fsv.r.renderToTex = true
            fsv.requestRender()

        }


        previewList.adapter = ColorPaletteAdapter(ColorPalette.all)
        previewList.addOnItemTouchListener(
                RecyclerTouchListener(
                        v.context,
                        previewList,
                        object : ClickListener {

                            override fun onClick(view: View, position: Int) {

                                f.palette = ColorPalette.all[position]
                                fsv.requestRender()

                                Log.e("MAIN ACTIVITY", "clicked palette: ${f.palette.name}")

                            }

                            override fun onLongClick(view: View, position: Int) {}

                        }
                )
        )
        layout.removeView(previewListLayout)
        previewListLayout.visibility = RecyclerView.VISIBLE


        doneButton.setOnClickListener {

                previewImage.setImageDrawable(GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        f.palette.getColors(resources, f.palette.ids)
                ))
                previewText.text = f.palette.name
                layout.removeView(previewListLayout)
                content.visibility = LinearLayout.VISIBLE
                fsv.renderProfile = RenderProfile.MANUAL

        }


        return v
    }

}