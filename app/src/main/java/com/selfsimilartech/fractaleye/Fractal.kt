package com.selfsimilartech.fractaleye

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


@SuppressLint("SetTextI18n")
class Fractal(
        private val context     : Activity,
        val sc                  : SettingsConfig,
        val screenRes           : IntArray,
        
        // shape config
        map:            ComplexMap              = ComplexMap.mandelbrot,
        juliaMode:      Boolean                 = map.juliaMode,

        // texture config
        texture:        Texture                 = Texture.exponentialSmoothing,
        
        // color config
        palette:        ColorPalette            = ColorPalette.p9,
        frequency:      Float                   = 1f,
        phase:          Float                   = 0f,
        
        
        maxIter:        Int             = 255,
        sensitivity:    Double          = 1.0
) {


    var numParamsInUse = map.numParams + if (juliaMode && !map.juliaMode) 1 else 0

    
    var map = map
        set (value) {
            if (field != value) {

                Log.d("FRACTAL", "setting map from $field to $value")
                field = value


                // reset texture if not compatible with new map
                if (!map.textures.contains(texture.name)) {
                    texture = Texture.escape
                }

                juliaMode = map.juliaMode
                map.position = if (juliaMode) map.positions.julia else map.positions.default
                numParamsInUse = map.numParams + if (juliaMode && !map.juliaMode) 1 else 0
                bailoutRadius = map.bailoutRadius ?: texture.bailoutRadius ?: bailoutRadius
                autoPrecision = if (!map.hasDualFloat || map.position.scale > precisionThreshold) Precision.SINGLE else Precision.DUAL

                updateShapeEditTexts()
                updatePositionEditTexts()

                // update render profile
                renderShaderChanged = true

            }
        }

    var juliaMode = juliaMode
        set (value) {
            if (field != value) {

                Log.d("FRACTAL", "setting juliaMode from $field to $value")
                field = value

                numParamsInUse += if (value) 1 else -1
                map.position = if (value) map.positions.julia else map.positions.default

                updateMapParamEditText(numParamsInUse)

//                if (value as Boolean) {
//                    addMapParams(1)
//                    f.savedCoords()[0] = f.coords()[0]
//                    f.savedCoords()[1] = f.coords()[1]
//                    f.savedScale()[0] = f.scale()[0]
//                    f.savedScale()[1] = f.scale()[1]
//                    f.coords()[0] = 0.0
//                    f.coords()[1] = 0.0
//                    f.scale()[0] = 3.5
//                    f.scale()[1] = 3.5 * f.screenRes[1].toDouble() / f.screenRes[0]
//                    f.params["p${f.numParamsInUse()}"] = ComplexMap.Param(
//                            f.savedCoords()[0],
//                            f.savedCoords()[1]
//                    )
//                }
//                else {
//                    removeMapParams(1)
//                    f.coords()[0] = f.savedCoords()[0]
//                    f.coords()[1] = f.savedCoords()[1]
//                    f.scale()[0] = f.savedScale()[0]
//                    f.scale()[1] = f.savedScale()[1]
//                }
//                f.updateMapParamEditTexts()
//                f.renderShaderChanged = true
//                fsv.r.renderToTex = true
            }
        }

    var texture = texture
        set (value) {
            if (field != value) {

                Log.d("FRACTAL", "setting texture from $field to $value")
                field = value

                bailoutRadius = map.bailoutRadius ?: texture.bailoutRadius ?: bailoutRadius

                updateShapeEditTexts()

                renderShaderChanged = true
                // fsv.r.renderToTex = true

            }
        }

    var palette = palette
        set (value) {
            if (field != value) {

                field = value

            }
        }

    var frequency = frequency
        set (value) {
            if (field != value) {
                field = value
            }
        }

    var phase = phase
        set (value) {
            if (field != value) {
                field = value
            }
        }

    var maxIter = maxIter
        set (value) {
            if (field != value) {

                Log.d("FRACTAL", "setting maxIter from $field to $value")
                field = value
                // fsv.r.renderToTex = true

            }
        }

    var bailoutRadius = texture.bailoutRadius ?: 2f
        set (value) {
            if (field != value) {

                Log.d("FRACTAL", "setting bailoutRadius from $field to $value")
                field = value

                // update EditText here

                // fsv.r.renderToTex = true

            }
        }

    var sensitivity = sensitivity
        set (value) {
            if (field != value) {
                field = value
            }
        }



    private var header       : String = ""
    private var arithmetic   : String = ""
    private var init         : String = ""
    private var loop         : String = ""
    private var conditional  : String = ""
    private var algInit      : String = ""
    private var algLoop      : String = ""
    private var algFinal     : String = ""
    private var mapInit      : String = ""
    private var mapLoop      : String = ""
    private var mapFinal     : String = ""

    private val res = context.resources
    private val colorHeader = res.getString(R.string.color_header)
    private val colorIndex = res.getString(R.string.color_index)
    private var colorPostIndex  : String = ""


    private val precisionThreshold = 5e-4
    val aspectRatio = screenRes[1].toDouble() / screenRes[0]
    var renderShaderChanged = false
    var colorShaderChanged = false
    var resolutionChanged = false
    var renderProfileChanged = false
    private var innerColor = "1.0"

    var autoPrecision = if (!map.hasDualFloat || map.position.scale > precisionThreshold) Precision.SINGLE else Precision.DUAL
    val precision = {
        if (sc.precision() == Precision.AUTO) autoPrecision else sc.precision()
    }
    var texRes = {
        when (sc.resolution()) {
            Resolution.LOW -> intArrayOf(screenRes[0]/8, screenRes[1]/8)
            Resolution.MED -> intArrayOf(screenRes[0]/3, screenRes[1]/3)
            Resolution.HIGH -> screenRes
            // Resolution.ULTRA -> intArrayOf((7*screenRes[0])/4, (7*screenRes[1])/4)
        }
    }


    val renderShader = {

        loadRenderResources()

        """$header
        $arithmetic
        void main() {
            $init
            $mapInit
            $algInit
            for (int n = 0; n < maxIter; n++) {
                if (n == maxIter - 1) {
                    $algFinal
                    colorParams.y = -1.0;
                    break;
                }
                $loop
                $mapLoop
                $algLoop
                $conditional {
                    $mapFinal
                    $algFinal
                    break;
                }
            }
            fragmentColor = colorParams;
        }
        """

    }

    val colorShader = {

        loadColorResources()

        """$colorHeader
        void main() {

            vec3 color = vec3(1.0);
            vec4 s = texture(tex, texCoord);

            if (s.y != -1.0) {
                $colorIndex
                $colorPostIndex
            }

            fragmentColor = vec4(color, 1.0);

        }
        """

    }





    val saved = {

        // SINE2 :: (-0.26282883851642613, 2.042520182493586E-6)
        // SINE2 :: (-0.999996934286532, 9.232660318047263E-5)
        // SINE2 :: (-0.2287186333845716, 0.1340647963904784)

        // SINE1 :: -0.578539160583084
        // SINE1 :: -0.8717463705274795
        // SINE1 :: 0.2948570315666499
        // SINE1 :: 0.31960705187983646
        // SINE1 :: -0.76977662
        //      JULIA :: (-0.85828304, -0.020673078)
        //      JULIA :: (-0.86083659, 0.0)
        // SINE1 :: -1.0
        //      JULIA :: (0.53298706, 0.00747937)

        // MANDELBROT
        //      JULIA :: (0.38168508, -0.20594095) + TRIANGLE INEQ

        // MANDELBROT @
        //      x:         -1.25735194436369140
        //      y:         -0.07363029998042227
        //      scale:      1.87845e-3
        // MANDELBROT @
        //      x:          0.39019590054025366
        //      y:         -0.26701156160039610
        //      scale:      9.59743e-8
        //      rotation:   146
        // MANDELBROT @
        //      x:         -0.48414790254135703
        //      y:         -0.59799104457234160
        //      scale:      6.15653e-4
        //      rotation:   2
        //      texture:    orbit trap
        //      palette:    p9
        //      frequency:  71.74303
        //      offset:     -0.26012
        //      maxIter:    1047

        // MANDELBROT CPOW :: (1.31423213, 2.86942864)
        //      JULIA :: (-0.84765975, -0.02321229)

        // SINE4 :: (1.41421356, 0.0)
        //      JULIA :: (-2.16074089, 0.0)

        // HORSESHOE CRAB :: (-0.02955925, 0.49291033)
        //      JULIA :: (-0.75224080, -0.93949126)

        // HORSESHOE CRAB :: (-0.02926709, 0.48591950)
        //      JULIA :: (-0.74308518, -0.94826022)

        // HORSESHOE CRAB :: (1.0, 0.0) @
        //      x:          -1.58648660503412890
        //      y:           0.09697857522320943
        //      scale:       7.28425e-3
        //      rotation:   -183

        // PERSIAN RUG :: (2.26988707, 0.0)
        //      JULIA :: (-0.04956468, -0.00017392)

        // BURNING SHIP
        //      JULIA :: (0.56311972, -0.88230390)
        //      JULIA :: (0.82628826, -1.15622855)
        //      JULIA :: (0.78515850, -1.14163868)
        //      JULIA :: (-1.75579063, -0.00825099)
        //      JULIA :: (-1.21314957, 0.00826136) + TRIANGLE INEQ / STRIPE
        //      JULIA :: (-1.62542315, 0.0)

        // MANDELBOX :: (1.58564605, 0.06087502)
        //      JULIA :: (5.75040877, 5.75041244)

        // MANDELBOX :: (0.60267262, 0.94995500)
        //      JULIA :: (-15.00327866, 43.11857865)

        // MANDELBOX :: (1.69056660, -1.66451872)
        //      JULIA :: MODIFY

        // BALLFOLD(Z^2 + P1) + C :: (1.23172118, 0.0)
        //      JULIA :: (-0.96727896, 0.0)

        // BALLFOLD(Z^2 + P1) + C :: (0.50611401, 0.0)
        //      JULIA :: (-0.28616842, -0.20683480)

        // KALI
        //      JULIA :: (-0.96665798, -0.02109066)
        //      JULIA :: (-0.04638263, -1.85958555)

        // ABS(SIN(Z^2 - i*Z)) + C
        //      JULIA :: (0.19371092, 0.53478799)


    }




    private fun loadRenderResources() {

        when(precision()) {
            Precision.SINGLE -> {
                header      = res.getString(R.string.header_sf)
                arithmetic  = res.getString(R.string.arithmetic_sf)
                init        = res.getString(R.string.general_init_sf)
                init += if (juliaMode) {
                    res.getString(R.string.julia_sf)
                } else {
                    res.getString(R.string.constant_sf)
                }
                loop        = res.getString(R.string.general_loop_sf)
                conditional = res.getString(map.conditionalSF)
                mapInit     = res.getString(map.initSF)
                algInit     = res.getString(texture.initSF)
                mapLoop     = res.getString(map.loopSF)
                if (juliaMode && !map.juliaMode) {
                    mapLoop = mapLoop.replace("C", "P${numParamsInUse}", false)
                }
                algLoop     = res.getString(texture.loopSF)
                mapFinal    = res.getString(map.finalSF)
                algFinal    = res.getString(texture.finalSF)
            }
            Precision.DUAL -> {

                header      = res.getString(R.string.header_df)
                arithmetic  = res.getString(R.string.arithmetic_util)
                arithmetic += res.getString(R.string.arithmetic_sf)
                arithmetic += res.getString(R.string.arithmetic_df)
                init        = res.getString(R.string.general_init_df)
                init += if (juliaMode) { res.getString(R.string.julia_df) }
                else { res.getString(R.string.constant_df) }
                loop        = res.getString(R.string.general_loop_df)
                conditional = res.getString(map.conditionalDF)
                mapInit     = res.getString(map.initDF)
                algInit     = res.getString(texture.initDF)
                mapLoop     = res.getString(map.loopDF)
                if (juliaMode && !map.juliaMode) {
                    mapLoop = mapLoop.replace("A", "vec2(P${map.numParams + 1}.x, 0.0)", false)
                    mapLoop = mapLoop.replace("B", "vec2(P${map.numParams + 1}.y, 0.0)", false)
                }
                algLoop     = res.getString(texture.loopDF)
                mapFinal    = res.getString(map.finalDF)
                algFinal    = res.getString(texture.finalDF)

            }
            else -> {}
        }

    }
    private fun loadColorResources() {

        colorPostIndex = ""
        innerColor = "0.0"

    }

    fun checkThresholdCross(prevScale: Double, prevPrecision: Precision) {

        autoPrecision = if (!map.hasDualFloat || map.position.scale > precisionThreshold) Precision.SINGLE else Precision.DUAL
        if (precision() != prevPrecision) { renderShaderChanged = true }

        // display message
        val singleThresholdCrossed = map.position.scale < precisionThreshold && prevScale > precisionThreshold
        val dualThresholdCrossed = map.position.scale < 1e-12 && prevScale > 1e-12
        if ((!map.hasDualFloat && singleThresholdCrossed) || (map.hasDualFloat && dualThresholdCrossed)) {
            val toast = Toast.makeText(context.baseContext, "Zoom limit reached", 2*Toast.LENGTH_LONG)
            val toastHeight = context.findViewById<LinearLayout>(R.id.buttons).height +
                    context.findViewById<LinearLayout>(R.id.uiFull).height +
                    context.findViewById<ProgressBar>(R.id.progressBar).height + 30
            toast.setGravity(Gravity.BOTTOM, 0, toastHeight)
            toast.show()
        }
        if (map.hasDualFloat && singleThresholdCrossed) {
            val toast = Toast.makeText(context.baseContext, "Switching to dual-precision\nImage generation will be slower", Toast.LENGTH_LONG)
            val toastHeight = context.findViewById<LinearLayout>(R.id.buttons).height +
                    context.findViewById<LinearLayout>(R.id.uiFull).height +
                    context.findViewById<ProgressBar>(R.id.progressBar).height + 30
            toast.setGravity(Gravity.BOTTOM, 0, toastHeight)
            toast.show()
        }

    }
    fun updateMapParamEditText(i: Int) {
        // Log.d("FRACTAL", "updating map param EditText $i")

        val xEdit : EditText?
        val yEdit : EditText?

        when (i) {
            1 -> {
                xEdit = context.findViewById(R.id.u1Edit)
                yEdit = context.findViewById(R.id.v1Edit)
            }
            2 -> {
                xEdit = context.findViewById(R.id.u2Edit)
                yEdit = context.findViewById(R.id.v2Edit)
            }
            3 -> {
                xEdit = context.findViewById(R.id.u3Edit)
                yEdit = context.findViewById(R.id.v3Edit)
            }
            4 -> {
                xEdit = context.findViewById(R.id.u4Edit)
                yEdit = context.findViewById(R.id.v4Edit)
            }
            else -> {
                xEdit = null
                yEdit = null
            }
        }

        xEdit?.setText("%.8f".format((map.params[i-1].u)))
        yEdit?.setText("%.8f".format((map.params[i-1].v)))

    }
    fun updateShapeEditTexts() {

        val bailoutSignificandEdit = context.findViewById<EditText>(R.id.bailoutSignificandEdit)
        val bailoutExponentEdit = context.findViewById<EditText>(R.id.bailoutExponentEdit)
        val bailoutStrings = "%e".format(bailoutRadius).split("e")
        bailoutSignificandEdit?.setText("%.5f".format(bailoutStrings[0].toFloat()))
        bailoutExponentEdit?.setText("%d".format(bailoutStrings[1].toInt()))

        for (i in 1..NUM_MAP_PARAMS) {
            updateMapParamEditText(i)
        }

        updatePositionEditTexts()

    }
    fun updateTextureEditTexts() {

        val q1Edit = context.findViewById<EditText>(R.id.q1Edit)
        val q2Edit = context.findViewById<EditText>(R.id.q2Edit)

        q1Edit?.setText("%.3f".format(texture.params[0].t))
        q2Edit?.setText("%.3f".format(texture.params[1].t))

    }
    fun updateColorEditTexts() {

        val frequencyEdit = context.findViewById<EditText>(R.id.frequencyEdit)
        val phaseEdit = context.findViewById<EditText>(R.id.phaseEdit)

        frequencyEdit?.setText("%.5f".format(frequency))
        phaseEdit?.setText("%.5f".format(phase))

    }
    fun updatePositionEditTexts() {

        val xCoordEdit = context.findViewById<EditText>(R.id.xCoordEdit)
        val yCoordEdit = context.findViewById<EditText>(R.id.yCoordEdit)
        val scaleSignificandEdit = context.findViewById<EditText>(R.id.scaleSignificandEdit)
        val scaleExponentEdit = context.findViewById<EditText>(R.id.scaleExponentEdit)
        val scaleStrings = "%e".format(map.position.scale).split("e")
        val rotationEdit = context.findViewById<EditText>(R.id.rotationEdit)

//        Log.w("FRACTAL", "scaleExponent: %d".format(scaleStrings[1].toInt()))
//        Log.w("FRACTAL", "bailoutExponent: %d".format(bailoutStrings[1].toInt()))

        xCoordEdit?.setText("%.17f".format(map.position.x))
        yCoordEdit?.setText("%.17f".format(map.position.y))

        scaleSignificandEdit?.setText("%.5f".format(scaleStrings[0].toFloat()))
        scaleExponentEdit?.setText("%d".format(scaleStrings[1].toInt()))

        rotationEdit?.setText("%.0f".format(map.position.rotation * 180.0 / Math.PI))

    }
    fun updateDisplayParams(reaction: Reaction, reactionChanged: Boolean = false) {

        val displayParams = context.findViewById<LinearLayout>(R.id.displayParams)
        val displayParam1 = context.findViewById<TextView>(R.id.displayParam1)
        val displayParam2 = context.findViewById<TextView>(R.id.displayParam2)
        val displayParam3 = context.findViewById<TextView>(R.id.displayParam3)
        val displayParam4 = context.findViewById<TextView>(R.id.displayParam4)
        val displayParamName1 = context.findViewById<TextView>(R.id.displayParamName1)
        val displayParamName2 = context.findViewById<TextView>(R.id.displayParamName2)
        val displayParamName3 = context.findViewById<TextView>(R.id.displayParamName3)
        val displayParamName4 = context.findViewById<TextView>(R.id.displayParamName4)
        val density = context.resources.displayMetrics.density
        val w : Int

        if (sc.displayParams()) {
            when (reaction) {
                Reaction.POSITION -> {

                    displayParamName1.text = res.getString(R.string.x)
                    displayParamName2.text = res.getString(R.string.y)
                    displayParamName3.text = res.getString(R.string.scale_lower)
                    displayParamName4.text = res.getString(R.string.rotation_lower)
                    displayParam1.text = "%.17f".format(map.position.x)
                    displayParam2.text = "%.17f".format(map.position.y)
                    displayParam3.text = "%e".format(map.position.scale)
                    displayParam4.text = "%.0f".format(map.position.rotation * 180.0 / Math.PI)
                    w = (60f * density).toInt()

                }
                Reaction.COLOR -> {

                    displayParamName1.text = res.getString(R.string.frequency)
                    displayParamName2.text = res.getString(R.string.offset)
                    displayParam1.text = "%.4f".format(frequency)
                    displayParam2.text = "%.4f".format(phase)
                    w = (75f * density).toInt()

                }
                else -> {

                    val i = reaction.ordinal - 2
                    displayParamName1.text = "u"
                    displayParamName2.text = "v"
                    displayParamName3.text = res.getString(R.string.sensitivity)
                    displayParam1.text = "%.8f".format((map.params[i].u))
                    displayParam2.text = "%.8f".format((map.params[i].v))
                    displayParam3.text = "%.4f".format(sensitivity)
                    w = (80f * density).toInt()

                }
            }

            displayParamName1.width = w
            displayParamName2.width = w
            displayParamName3?.width = w
            displayParamName4?.width = w
            displayParamName1.requestLayout()
            displayParamName2.requestLayout()
            displayParamName3?.requestLayout()
            displayParamName4?.requestLayout()
            displayParam1.requestLayout()
            displayParam2.requestLayout()
            displayParam3?.requestLayout()
            displayParam4?.requestLayout()

        }
        if (sc.displayParams() || reactionChanged) {
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 1000L
            fadeOut.startOffset = 2500L
            fadeOut.fillAfter = true
            displayParams.animation = fadeOut
            displayParams.animation.start()
            displayParams.requestLayout()
        }
    }

}