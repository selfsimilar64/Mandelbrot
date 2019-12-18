package com.selfsimilartech.fractaleye

import android.content.Context
import android.support.v4.app.Fragment
import android.os.Bundle
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*


class TextureFragment : Fragment() {

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
        val previewListLayout = v.findViewById<LinearLayout>(R.id.texturePreviewListLayout)
        val content = v.findViewById<LinearLayout>(R.id.textureContent)
        val doneButton = v.findViewById<Button>(R.id.textureDoneButton)

        layout.removeView(previewListLayout)
        previewList.visibility = RecyclerView.VISIBLE

        previewImage.setImageBitmap(f.texture.thumbnail)
        previewText.text = f.texture.name
        preview.setOnClickListener {

            layout.addView(previewListLayout)
            content.visibility = LinearLayout.GONE

            fsv.renderProfile = RenderProfile.TEXTURE_THUMB
            fsv.r.renderThumbnails = true
            fsv.requestRender()


        }

        previewList.adapter = TextureAdapter(Texture.all)
        previewList.addOnItemTouchListener(
                RecyclerTouchListener(
                        v.context,
                        previewList,
                        object : ClickListener {

                            override fun onClick(view: View, position: Int) {

                                f.texture = Texture.all[position]

                                fsv.r.renderShaderChanged = true
                                fsv.r.renderToTex = true
                                fsv.requestRender()

                                Log.e("MAIN ACTIVITY", "clicked texture: ${f.texture.name}")

                            }

                            override fun onLongClick(view: View, position: Int) {}

                        }
                )
        )


        doneButton.setOnClickListener {

            previewImage.setImageBitmap(f.texture.thumbnail)
            previewImage.scaleType = ImageView.ScaleType.CENTER_CROP
            previewText.text = f.texture.name
            layout.removeView(previewListLayout)
            content.visibility = LinearLayout.VISIBLE
            fsv.renderProfile = RenderProfile.MANUAL

        }



        val bailoutSignificandEdit = v.findViewById<EditText>(R.id.bailoutSignificandEdit)
        val bailoutExponentEdit = v.findViewById<EditText>(R.id.bailoutExponentEdit)
        bailoutSignificandEdit.setOnEditorActionListener(
                editListener(bailoutExponentEdit) { w: TextView ->
                    f.bailoutRadius = "${w.text}e${bailoutExponentEdit.text}".formatToDouble()?.toFloat() ?: f.bailoutRadius
                    val bailoutStrings = "%e".format(f.bailoutRadius).split("e")
                    w.text = "%.5f".format(bailoutStrings[0].toFloat())
                    fsv.r.renderToTex = true
                })
        bailoutExponentEdit.setOnEditorActionListener(
                editListener(null) { w: TextView ->
                    f.bailoutRadius = "${bailoutSignificandEdit.text}e${w.text}".formatToDouble()?.toFloat() ?: f.bailoutRadius
                    val bailoutStrings = "%e".format(f.bailoutRadius).split("e")
                    w.text = "%d".format(bailoutStrings[1].toInt())
                    fsv.r.renderToTex = true
                })


        return v
    }

}