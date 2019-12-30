package com.selfsimilartech.fractaleye

import android.content.Context
import android.support.v4.app.Fragment
import android.os.Bundle
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.design.widget.TabLayout
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlinx.android.synthetic.main.texture_fragment.*
import java.util.*
import kotlin.math.roundToInt


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



    override fun onCreate(savedInstanceState: Bundle?) {
        val act = if (activity is MainActivity) activity as MainActivity else null
        if (!this::f.isInitialized) f = act!!.f
        if (!this::fsv.isInitialized) fsv = act!!.fsv
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.texture_fragment, container, false)

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


        // previewImage.setImageBitmap(f.texture.thumbnail)
        val handler = Handler()
        texturePreviewName.text = f.texture.name
        texturePreview.setOnClickListener {

            handler.postDelayed({

                if (f.shape.textures != (texturePreviewList.adapter as TextureAdapter).textureList) {
                    texturePreviewList.adapter = TextureAdapter(f.shape.textures)
                }

                textureLayout.addView(texturePreviewListLayout)
                textureContent.visibility = LinearLayout.GONE

                fsv.renderProfile = RenderProfile.TEXTURE_THUMB
                fsv.r.renderThumbnails = true
                fsv.requestRender()

            }, BUTTON_CLICK_DELAY)

        }



        qLayout.visibility = ConstraintLayout.GONE
        qEdit.setOnEditorActionListener(editListener(null) { w: TextView ->

            val param = f.texture.params[0]
            param.q = w.text.toString().formatToDouble() ?: param.q
            param.setProgressFromValue()

            w.text = param.toString()
            qBar.progress = (param.progress*qBar.max.toDouble()).toInt()
            Log.d("TEXTURE FRAGMENT", "param progress : ${param.progress}")
            Log.d("TEXTURE FRAGMENT", "qBar max : ${qBar.max}")

            fsv.r.renderToTex = true

        })
        qBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                // update EditText but not param
                val param = f.texture.params[0]
                val progressNormal = progress.toDouble()/qBar.max
                qEdit.setText(param.toString((1.0 - progressNormal)*param.min + progressNormal*param.max))

            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                // update EditText and param -- then render
                val param = f.texture.params[0]
                param.progress = qBar.progress.toDouble()/qBar.max
                param.setValueFromProgress()
                qEdit.setText(f.texture.params[0].toString())

                fsv.r.renderToTex = true
                fsv.requestRender()

            }

        })
        if (f.texture.numParamsInUse != 0) {
            qBar.max = if (f.texture.params[0].discrete)
                f.texture.params[0].interval.toInt() else 100
        }



        texturePreviewList.adapter = TextureAdapter(f.shape.textures)
        texturePreviewList.addOnItemTouchListener(
                RecyclerTouchListener(
                        v.context,
                        texturePreviewList,
                        object : ClickListener {

                            override fun onClick(view: View, position: Int) {

                                if (f.shape.textures[position] != f.texture) {

                                    f.texture = f.shape.textures[position]

                                    if (f.texture.numParamsInUse != 0) {

                                        val param = f.texture.params[0]
                                        qLayout.visibility = ConstraintLayout.VISIBLE
                                        qName.text = param.name
                                        Log.d("TEXTURE FRAGMENT", "q: $param")
                                        qBar.max = if (f.texture.params[0].discrete)
                                            f.texture.params[0].interval.toInt() else 100
                                        qBar.progress = (param.progress*qBar.max.toDouble()).roundToInt()
                                        Log.d("TEXTURE FRAGMENT", "${param.progress}")

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


        textureDoneButton.setOnClickListener {

            // previewImage.setImageBitmap(f.texture.thumbnail)
            // previewImage.scaleType = ImageView.ScaleType.CENTER_CROP
            texturePreviewName.text = f.texture.name
            textureLayout.removeView(texturePreviewListLayout)
            textureContent.visibility = LinearLayout.VISIBLE
            fsv.renderProfile = RenderProfile.MANUAL

        }



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



        textureModeTabs.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {}
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabSelected(tab: TabLayout.Tab) {

                f.textureMode = TextureMode.valueOf(tab.contentDescription.toString().toUpperCase(Locale.ROOT))
                fsv.requestRender()

            }
        })
        textureModeTabs.getTabAt(f.textureMode.ordinal)?.select()




        textureLayout.removeView(texturePreviewListLayout)
        texturePreviewListLayout.visibility = RecyclerView.VISIBLE


        act?.updateTextureEditTexts()
        super.onViewCreated(v, savedInstanceState)
    }

}