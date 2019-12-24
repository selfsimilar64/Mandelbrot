package com.selfsimilartech.fractaleye

import android.content.Context
import android.support.v4.app.Fragment
import android.os.Bundle
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.util.Log
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
            val act = if (activity is MainActivity) activity as MainActivity else null
            act?.showMessage("Invalid number format")
        }
        return d
    }

    // Inflate the view for the fragment based on layout XML
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.texture_fragment, container, false)
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


        val layout = v.findViewById<LinearLayout>(R.id.textureLayout)
        val preview = v.findViewById<LinearLayout>(R.id.texturePreview)
        val previewName = v.findViewById<TextView>(R.id.texturePreviewName)
        // val previewImage = (previewName.getChildAt(0) as CardView).getChildAt(0) as ImageView
        // val previewText = previewName.getChildAt(1) as TextView
        val previewList = v.findViewById<RecyclerView>(R.id.texturePreviewList)
        val previewListLayout = v.findViewById<LinearLayout>(R.id.texturePreviewListLayout)
        val content = v.findViewById<LinearLayout>(R.id.textureContent)
        val doneButton = v.findViewById<Button>(R.id.textureDoneButton)

        layout.removeView(previewListLayout)
        previewListLayout.visibility = RecyclerView.VISIBLE

        // previewImage.setImageBitmap(f.texture.thumbnail)
        val handler = Handler()
        previewName.text = f.texture.name
        preview.setOnClickListener {

            handler.postDelayed({

                if (f.shape.textures != (previewList.adapter as TextureAdapter).textureList) {
                    previewList.adapter = TextureAdapter(f.shape.textures)
                }

                layout.addView(previewListLayout)
                content.visibility = LinearLayout.GONE

                fsv.renderProfile = RenderProfile.TEXTURE_THUMB
                fsv.r.renderThumbnails = true
                fsv.requestRender()

            }, BUTTON_CLICK_DELAY)

        }




        val qLayout = v.findViewById<ConstraintLayout>(R.id.qLayout)
        val qBar = v.findViewById<SeekBar>(R.id.qBar)
        val qName = v.findViewById<TextView>(R.id.qName)
        val qEdit = v.findViewById<EditText>(R.id.qEdit)

        qLayout.visibility = ConstraintLayout.GONE
        qEdit.setOnEditorActionListener(editListener(null) { w: TextView ->

            val param = f.texture.params[0]
            param.q = w.text.toString().formatToDouble() ?: param.q
            param.progress = (param.q - param.min) / (param.max - param.min)
            w.text = param.toString()
            qBar.progress = (param.progress*qBar.max.toDouble()).toInt()
            Log.d("TEXTURE FRAGMENT", "param progress : ${param.progress}")
            Log.d("TEXTURE FRAGMENT", "qBar max : ${qBar.max}")
            fsv.r.renderToTex = true

        })
        qBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                f.texture.params[0].progress = qBar.progress.toDouble()/qBar.max
                qEdit.setText(f.texture.params[0].toString())
                act?.updateTextureEditTexts()

            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                f.texture.params[0].progress = qBar.progress.toDouble()/qBar.max
                qEdit.setText(f.texture.params[0].toString())
                act?.updateTextureEditTexts()

                fsv.r.renderToTex = true
                fsv.requestRender()

            }

        })
        if (f.texture.numParamsInUse != 0) {
            qBar.max = if (f.texture.params[0].discrete)
                f.texture.params[0].interval.toInt() else 100
        }



        previewList.adapter = TextureAdapter(f.shape.textures)
        previewList.addOnItemTouchListener(
                RecyclerTouchListener(
                        v.context,
                        previewList,
                        object : ClickListener {

                            override fun onClick(view: View, position: Int) {

                                if (f.shape.textures[position] != f.texture) {

                                    f.texture = f.shape.textures[position]

                                    if (f.texture.numParamsInUse != 0) {

                                        val param = f.texture.params[0]
                                        qLayout.visibility = ConstraintLayout.VISIBLE
                                        qName.text = param.name
                                        qEdit.setText(param.toString())
                                        qBar.max = if (f.texture.params[0].discrete)
                                            f.texture.params[0].interval.toInt() else 100

                                    }
                                    else {

                                        qLayout.visibility = ConstraintLayout.GONE

                                    }

                                    fsv.r.renderShaderChanged = true
                                    fsv.r.renderToTex = true
                                    fsv.requestRender()

                                    act?.updateTextureEditTexts()

                                }

                                Log.e("MAIN ACTIVITY", "clicked texture: ${f.texture.name}")

                            }

                            override fun onLongClick(view: View, position: Int) {}

                        }
                )
        )


        doneButton.setOnClickListener {

            // previewImage.setImageBitmap(f.texture.thumbnail)
            // previewImage.scaleType = ImageView.ScaleType.CENTER_CROP
            previewName.text = f.texture.name
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val act = if (activity is MainActivity) activity as MainActivity else null
        act?.updateTextureEditTexts()
        super.onViewCreated(view, savedInstanceState)
    }

}