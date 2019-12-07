package com.selfsimilartech.fractaleye

import android.content.Context
import android.support.v4.app.Fragment
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.TabLayout
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlin.math.log
import kotlin.math.pow


class TextureFragment : Fragment() {

    // Store instance variables
    private lateinit var f: Fractal
    private lateinit var fsv: FractalSurfaceView

    // newInstance constructor for creating fragment with arguments
    fun passArguments(f: Fractal, fsv: FractalSurfaceView) {
        this.f = f
        this.fsv = fsv
    }


    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.texture_fragment, container, false)


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


        val layout = v.findViewById<LinearLayout>(R.id.textureLayout)
        val preview = v.findViewById<LinearLayout>(R.id.texturePreview)
        val previewImage = (preview.getChildAt(0) as CardView).getChildAt(0) as ImageView
        val previewText = preview.getChildAt(1) as TextView
        val previewList = v.findViewById<RecyclerView>(R.id.texturePreviewList)
        val content = v.findViewById<LinearLayout>(R.id.textureContent)

        layout.removeView(previewList)
        previewList.visibility = RecyclerView.VISIBLE

        previewImage.setImageResource(f.texture.icon)
        previewText.text = f.texture.name
        preview.setOnClickListener {
            content.visibility = LinearLayout.GONE
            layout.addView(previewList)
        }

        previewList.adapter = TextureAdapter(Texture.all)
        previewList.addOnItemTouchListener(
                RecyclerTouchListener(
                        v.context,
                        previewList,
                        object : ClickListener {

                            override fun onClick(view: View, position: Int) {

                                f.texture = Texture.all[position]
                                previewImage.setImageResource(f.texture.icon)
                                previewText.text = f.texture.name
                                layout.removeView(previewList)
                                content.visibility = LinearLayout.VISIBLE

                                fsv.r.renderShaderChanged = true
                                fsv.r.renderToTex = true
                                fsv.requestRender()

                                Log.e("MAIN ACTIVITY", "clicked texture: ${f.texture.name}")

                            }

                            override fun onLongClick(view: View, position: Int) {}

                        }
                )
        )


        return v
    }

}