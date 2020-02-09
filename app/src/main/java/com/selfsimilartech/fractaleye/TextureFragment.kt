package com.selfsimilartech.fractaleye

import android.content.Context
import android.graphics.Bitmap
import java.text.NumberFormat
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.os.Handler
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.tabs.TabLayout
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlinx.android.synthetic.main.texture_fragment.*
import java.text.ParseException
import java.util.*
import kotlin.math.roundToInt


class TextureFragment : Fragment() {

    private val nf = NumberFormat.getInstance()

    private fun String.formatToDouble(showMsg: Boolean = true) : Double? {
        var d : Double? = null
        try { d = nf.parse(this)?.toDouble() }
        catch (e: ParseException) {
            if (showMsg) {
                val act = activity as MainActivity
                act.showMessage(resources.getString(R.string.msg_invalid_format))
            }
        }
        return d
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.texture_fragment, container, false)

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
                    fsv.requestRender()
                }
                else -> {
                    Log.d("EQUATION FRAGMENT", "some other action")
                }
            }

            editText.clearFocus()
            fsv.updateSystemUI()
            true

        }}


        // previewImage.setImageBitmap(f.texture.thumbnail)
        val handler = Handler()
        texturePreviewName.text = resources.getString(f.texture.displayName)
        texturePreview.setOnClickListener {

            handler.postDelayed({

                if (f.shape.compatTextures != (texturePreviewList.adapter as TextureAdapter).textureList) {
                    texturePreviewList.adapter = TextureAdapter(f.shape.compatTextures)
                }

                textureLayout.addView(texturePreviewListLayout)
                textureContent.visibility = LinearLayout.GONE

                fsv.renderProfile = RenderProfile.TEXTURE_THUMB
                fsv.r.renderThumbnails = true
                fsv.requestRender()

            }, BUTTON_CLICK_DELAY)

        }



        textureParamLayout.visibility = ConstraintLayout.GONE
        textureParamButtons.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {}
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabSelected(tab: TabLayout.Tab) {

                val param = f.texture.params[tab.position]
                Log.d("TEXTURE FRAGMENT", "${resources.getString(param.name)} -- value: $param, progress: ${param.progress}, interval: ${param.interval.toInt()}")
                qBar.max = if (param.discrete) param.interval.toInt() else 100
                Log.d("TEXTURE FRAGMENT", "qBar max: ${qBar.max}")
                qBar.progress = (param.progress*qBar.max.toDouble()).roundToInt()
                Log.d("TEXTURE FRAGMENT", "${param.progress}")

            }
        })
        qEdit.setOnEditorActionListener(editListener(null) { w: TextView ->

            val param = f.texture.params[textureParamButtons.selectedTabPosition]
            val result = w.text.toString().formatToDouble()
            if (result != null) {
                param.q = result
                fsv.r.renderToTex = true
            }
            param.setProgressFromValue()

            w.text = param.toString()
            qBar.progress = (param.progress*qBar.max.toDouble()).toInt()
            // Log.d("TEXTURE FRAGMENT", "param progress : ${param.progress}")
            // Log.d("TEXTURE FRAGMENT", "qBar max : ${qBar.max}")

        })
        qBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            fun updateEditTextOnly() {

                // update EditText but not param
                val param = f.texture.params[textureParamButtons.selectedTabPosition]
                val progressNormal = qBar.progress.toDouble()/qBar.max
                qEdit.setText(param.toString((1.0 - progressNormal)*param.min + progressNormal*param.max))

            }
            fun updateEditTextAndParam() {

                // update EditText and param -- then render
                val param = f.texture.params[textureParamButtons.selectedTabPosition]
                param.progress = qBar.progress.toDouble()/qBar.max
                param.setValueFromProgress()
                qEdit.setText(param.toString())

                fsv.r.renderToTex = true
                fsv.requestRender()

            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                if (fsv.sc.continuousRender) updateEditTextAndParam()
                else updateEditTextOnly()

            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                updateEditTextAndParam()

            }

        })
        if (f.texture.numParamsInUse != 0) {
            qBar.max = if (f.texture.params[0].discrete)
                f.texture.params[0].interval.toInt() else 100
        }



        texturePreviewList.adapter = TextureAdapter(f.shape.compatTextures)
        texturePreviewList.addOnItemTouchListener(
                RecyclerTouchListener(
                        v.context,
                        texturePreviewList,
                        object : ClickListener {

                            override fun onClick(view: View, position: Int) {

                                if (f.shape.compatTextures[position] != f.texture) {

                                    f.texture = f.shape.compatTextures[position]

                                    if (f.texture.numParamsInUse != 0) {

                                        textureParamLayout.visibility = ConstraintLayout.VISIBLE

                                        textureParamButtons.removeAllTabs()
                                        f.texture.params.forEach { param ->

                                            if (BuildConfig.PAID_VERSION || !param.proFeature) {
                                                textureParamButtons.addTab(textureParamButtons.newTab().setText(param.name))
                                            }

                                        }

                                        textureParamButtons.getTabAt(0)?.select()

                                    }
                                    else {

                                        textureParamLayout.visibility = ConstraintLayout.GONE

                                    }

                                    fsv.r.renderShaderChanged = true
                                    fsv.r.renderToTex = true
                                    fsv.requestRender()

                                    act.updateTextureEditTexts()

                                }

                                // Log.e("MAIN ACTIVITY", "clicked texture: ${f.texture.name}")

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
            texturePreviewName.text = resources.getString(f.texture.displayName)
            textureLayout.removeView(texturePreviewListLayout)
            textureContent.visibility = LinearLayout.VISIBLE
            fsv.renderProfile = RenderProfile.MANUAL

        }



        bailoutSignificandEdit.setOnEditorActionListener(
                editListener(bailoutExponentEdit) { w: TextView ->
                    val result1 = w.text.toString().formatToDouble()
                    val result2 = bailoutExponentEdit.text.toString().formatToDouble()
                    val result3 = "${w.text}e${bailoutExponentEdit.text}".formatToDouble()?.toFloat()
                    if (result1 != null && result2 != null && result3 != null) {
                        f.bailoutRadius = result3
                        fsv.r.renderToTex = true
                    }
                    else {
                        act.showMessage(resources.getString(R.string.msg_invalid_format))
                    }
                    val bailoutStrings = "%e".format(Locale.US, f.bailoutRadius).split("e")
                    w.text = "%.5f".format(bailoutStrings[0].toFloat())
                    bailoutExponentEdit.setText("%d".format(bailoutStrings[1].toInt()))
                })
        bailoutExponentEdit.setOnEditorActionListener(
                editListener(null) { w: TextView ->
                    val result1 = bailoutSignificandEdit.text.toString().formatToDouble()
                    val result2 = w.text.toString().formatToDouble()
                    val result3 = "${bailoutSignificandEdit.text}e${w.text}".formatToDouble()?.toFloat()
                    if (result1 != null && result2 != null && result3 != null) {
                        f.bailoutRadius = result3
                        fsv.r.renderToTex = true
                    }
                    else {
                        act.showMessage(resources.getString(R.string.msg_invalid_format))
                    }
                    val bailoutStrings = "%e".format(Locale.US, f.bailoutRadius).split("e")
                    bailoutSignificandEdit.setText("%.5f".format(bailoutStrings[0].toFloat()))
                    w.text = "%d".format(bailoutStrings[1].toInt())
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




        textureLayout.removeView(texturePreviewListLayout)
        texturePreviewListLayout.visibility = RecyclerView.VISIBLE


        val thumbRes = Resolution.THUMB.scaleRes(fsv.screenRes)
        Texture.all.forEach {
            if (it.thumbnail == null) {
                it.thumbnail = Bitmap.createBitmap(thumbRes[0], thumbRes[0], Bitmap.Config.ARGB_8888)
            }
        }
        act.updateTextureEditTexts()
        super.onViewCreated(v, savedInstanceState)
    }

}