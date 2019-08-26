package com.example.matt.gputest

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.view.animation.AlphaAnimation
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView


class Fractal(
        private val context     : Activity,
        val fractalConfig       : FractalConfig,
        val settingsConfig      : SettingsConfig,
        val screenRes           : IntArray
) {

    var glVersion : Float = 0f

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


    private val precisionThreshold = 6e-5
    private val aspectRatio = screenRes[1].toDouble()/screenRes[0]
    var renderShaderChanged = false
    var colorShaderChanged = false
    var resolutionChanged = false
    var renderProfileChanged = false
    var innerColor = "1.0"

    val autoPrecision = {
        if (fractalConfig.scale()[0] > precisionThreshold) Precision.SINGLE else Precision.DUAL
    }
    val precision = {
        if (settingsConfig.precision() == Precision.AUTO) autoPrecision() else settingsConfig.precision()
    }
    var texRes = {
        when (settingsConfig.resolution()) {
            Resolution.LOW -> intArrayOf(screenRes[0]/8, screenRes[1]/8)
            Resolution.MED -> intArrayOf(screenRes[0]/3, screenRes[1]/3)
            Resolution.HIGH -> screenRes
            // Resolution.ULTRA -> intArrayOf((7*screenRes[0])/4, (7*screenRes[1])/4)
        }
    }


    val renderShader = {

        loadRenderResources()

        """
        $header
        $arithmetic
        void main() {
            $init
            $mapInit
            $algInit
            for (int n = 0; n < maxIter; n++) {
                if (n == maxIter - 1) {
                    $algFinal
                    colorParams.w = -2.0;
                    break;
                }
                $loop
                $mapLoop
                $conditional {
                    $mapFinal
                    $algFinal
                    break;
                }
                $algLoop
            }
            fragmentColor = colorParams;
        }
        """

    }

    val colorShader = {

        loadColorResources()

        """
        $colorHeader
        void main() {

            vec3 color = vec3(1.0);
            vec4 s = texture(tex, texCoord);

            if (s.w != -1.0) {
                $colorIndex
                $colorPostIndex
            }

            fragmentColor = vec4(color, 1.0);

        }
        """

    }


    init {
        reset()
    }


    private fun loadRenderResources() {

        when(precision()) {
            Precision.SINGLE -> {
                header      = res.getString(R.string.header_sf)
                arithmetic  = res.getString(R.string.arithmetic_sf)
                init        = res.getString(R.string.general_init_sf)
                if (fractalConfig.juliaMode()) { init += res.getString(R.string.julia_sf) }
                else { init += res.getString(R.string.constant_sf) }
                loop        = res.getString(R.string.general_loop_sf)
                conditional = fractalConfig.map().conditionalSF    ?: ""
                mapInit     = fractalConfig.map().initSF           ?: ""
                algInit     = fractalConfig.texture().initSF
                mapLoop     = fractalConfig.map().loopSF           ?: ""
                if (fractalConfig.juliaMode()) {
                    mapLoop = mapLoop.replace("C", "P${fractalConfig.map().initParams.size + 1}", false)
                }
                algLoop     = fractalConfig.texture().loopSF
                mapFinal    = fractalConfig.map().finalSF          ?: ""
                algFinal    = fractalConfig.texture().finalSF
            }
            Precision.DUAL -> {

                header      = res.getString(R.string.header_df)
                arithmetic  = res.getString(R.string.arithmetic_util)
                arithmetic += res.getString(R.string.arithmetic_sf)
                arithmetic += res.getString(R.string.arithmetic_df)
                init        = res.getString(R.string.general_init_df)
                if (fractalConfig.juliaMode()) { init += res.getString(R.string.julia_df) }
                else { init += res.getString(R.string.constant_df) }
                loop        = res.getString(R.string.general_loop_df)
                conditional = fractalConfig.map().conditionalDF    ?: ""
                mapInit     = fractalConfig.map().initDF           ?: ""
                algInit     = fractalConfig.texture().initDF
                mapLoop     = fractalConfig.map().loopDF           ?: ""
                if (fractalConfig.juliaMode()) {
                    mapLoop = mapLoop.replace("A", "vec2(P${fractalConfig.map().initParams.size + 1}.x, 0.0)", false)
                    mapLoop = mapLoop.replace("B", "vec2(P${fractalConfig.map().initParams.size + 1}.y, 0.0)", false)
                }
                algLoop     = fractalConfig.texture().loopDF
                mapFinal    = fractalConfig.map().finalDF          ?: ""
                algFinal    = fractalConfig.texture().finalDF

            }
            else -> {}
        }

    }
    private fun loadColorResources() {

        if (fractalConfig.texture().name == "Escape Time Smooth with Lighting") {
            colorPostIndex = context.resources.getString(R.string.color_lighting)
            innerColor = "1.0"
        }
        else {
            colorPostIndex = ""
            innerColor = "0.0"
        }

    }

    private fun resetPosition() {
        fractalConfig.coords()[0] = fractalConfig.map().initCoords[0]
        fractalConfig.coords()[1] = fractalConfig.map().initCoords[1]
        fractalConfig.scale()[0] = fractalConfig.map().initScale
        fractalConfig.scale()[1] = fractalConfig.map().initScale * aspectRatio
        fractalConfig.params["bailoutRadius"] = fractalConfig.map().initBailout
        updatePositionEditTexts()
    }
    private fun resetMapParams() {
        for (i in 0 until fractalConfig.map().initParams.size) {
            (fractalConfig.params["p${i + 1}"] as DoubleArray)[0] = fractalConfig.map().initParams[i][0]
            (fractalConfig.params["p${i + 1}"] as DoubleArray)[1] = fractalConfig.map().initParams[i][1]
        }
        updateMapParamEditTexts()
    }
    fun resetTextureParams() {
        for (i in 0 until fractalConfig.texture().initParams.size) {
            fractalConfig.params["q${i + 1}"] = fractalConfig.texture().initParams[i].third
        }
        updateTextureParamEditTexts()
    }
    private fun resetColorParams() {
        fractalConfig.params["frequency"] = 3.5f
        fractalConfig.params["phase"] = 0f
    }
    fun reset() {
        resetPosition()
        resetMapParams()
        resetTextureParams()
        resetColorParams()
        fractalConfig.params["juliaMode"] = fractalConfig.map().initJuliaMode
    }
    @SuppressLint("SetTextI18n")
    fun updatePositionEditTexts() {

        val xCoordEdit = context.findViewById<EditText>(R.id.xCoordEdit)
        val yCoordEdit = context.findViewById<EditText>(R.id.yCoordEdit)
        val scaleSignificandEdit = context.findViewById<EditText>(R.id.scaleSignificandEdit)
        val scaleExponentEdit = context.findViewById<EditText>(R.id.scaleExponentEdit)
        val scaleStrings = "%e".format(fractalConfig.scale()[0]).split("e")
        val bailoutSignificandEdit = context.findViewById<EditText>(R.id.bailoutSignificandEdit)
        val bailoutExponentEdit = context.findViewById<EditText>(R.id.bailoutExponentEdit)
        val bailoutStrings = "%e".format(fractalConfig.bailoutRadius()).split("e")

        xCoordEdit?.setText("%.17f".format(fractalConfig.coords()[0]))
        yCoordEdit?.setText("%.17f".format(fractalConfig.coords()[1]))
        scaleSignificandEdit?.setText(scaleStrings[0])
        scaleExponentEdit?.setText(scaleStrings[1])
        bailoutSignificandEdit?.setText(bailoutStrings[0])
        bailoutExponentEdit?.setText(bailoutStrings[1])

    }
    @SuppressLint("SetTextI18n")
    fun updateMapParamEditText(i: Int) {
        // Log.d("FRACTAL", "updating map param EditText $i")

        val xEdit : EditText?
        val yEdit : EditText?

        when (i) {
            1 -> {
                xEdit = context.findViewById(R.id.p1xEdit)
                yEdit = context.findViewById(R.id.p1yEdit)
            }
            2 -> {
                xEdit = context.findViewById(R.id.p2xEdit)
                yEdit = context.findViewById(R.id.p2yEdit)
            }
            3 -> {
                xEdit = context.findViewById(R.id.p3xEdit)
                yEdit = context.findViewById(R.id.p3yEdit)
            }
            4 -> {
                xEdit = context.findViewById(R.id.p4xEdit)
                yEdit = context.findViewById(R.id.p4yEdit)
            }
            else -> {
                xEdit = null
                yEdit = null
            }
        }

        xEdit?.setText("%.8f".format((fractalConfig.params["p$i"] as DoubleArray)[0]))
        yEdit?.setText("%.8f".format((fractalConfig.params["p$i"] as DoubleArray)[1]))

    }
    fun updateMapParamEditTexts() {
        for (i in 1..NUM_MAP_PARAMS) {
            updateMapParamEditText(i)
        }
    }
    fun updateTextureParamEditText(i: Int) {

        val edit : EditText? = when (i) {
            1 -> context.findViewById(R.id.q1Edit)
            2 -> context.findViewById(R.id.q2Edit)
            else -> null
        }

        edit?.setText("%.3f".format((fractalConfig.params["q$i"] as Double)))

    }
    fun updateTextureParamEditTexts() {
        for (i in 1..NUM_TEXTURE_PARAMS) {
            updateMapParamEditText(i)
        }
    }
    fun updateColorParamEditTexts() {

        val frequencyEdit = context.findViewById<EditText>(R.id.frequencyEdit)
        val phaseEdit = context.findViewById<EditText>(R.id.phaseEdit)

        frequencyEdit?.setText("%.5f".format(fractalConfig.frequency()))
        phaseEdit?.setText("%.5f".format(fractalConfig.phase()))

    }
    fun updateDisplayParams(reaction: Reaction, reactionChanged: Boolean) {
        val displayParams = context.findViewById<LinearLayout>(R.id.displayParams)
        if (settingsConfig.displayParams()) {
            when (reaction) {
                Reaction.TRANSFORM -> {
                    (displayParams.getChildAt(1) as TextView).text = "x: %.17f".format(fractalConfig.coords()[0])
                    (displayParams.getChildAt(2) as TextView).text = "y: %.17f".format(fractalConfig.coords()[1])
                    (displayParams.getChildAt(3) as TextView).text = "scale: %e".format(fractalConfig.scale()[0])
                }
                Reaction.COLOR -> {
                    (displayParams.getChildAt(1) as TextView).text = "frequency: %.4f".format(fractalConfig.frequency())
                    (displayParams.getChildAt(2) as TextView).text = "phase: %.4f".format(fractalConfig.phase())
                }
                else -> {
                    val i = reaction.ordinal - 2
                    (displayParams.getChildAt(1) as TextView).text = "x: %.8f".format((fractalConfig.params["p${i + 1}"] as DoubleArray)[0])
                    (displayParams.getChildAt(2) as TextView).text = "y: %.8f".format((fractalConfig.params["p${i + 1}"] as DoubleArray)[1])
                    (displayParams.getChildAt(3) as TextView).text = "sensitivity: %.4f".format(fractalConfig.paramSensitivity())
                }
            }
        }
        if (settingsConfig.displayParams() || reactionChanged) {
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 1000L
            fadeOut.startOffset = 2500L
            fadeOut.fillAfter = true
            displayParams.animation = fadeOut
            displayParams.animation.start()
            displayParams.requestLayout()
        }
    }
    fun switchOrientation() {

//        val fractalConfig.coords() = doubleArrayOf((xCoords[0] + xCoords[1]) / 2.0, -(yCoords[0] + yCoords[1]) / 2.0)
//        translate(fractalConfig.coords())
//
//        // rotation by 90 degrees counter-clockwise
//        val xCoordsNew = doubleArrayOf(-yCoords[1], -yCoords[0])
//        val yCoordsNew = doubleArrayOf(xCoords[0], xCoords[1])
//        xCoords[0] = xCoordsNew[0]
//        xCoords[1] = xCoordsNew[1]
//        yCoords[0] = yCoordsNew[0]
//        yCoords[1] = yCoordsNew[1]
//
//        translate(fractalConfig.coords().negative())

//        Log.d("FRACTAL", "xCoordsNew:  (${xCoordsNew[0]}, ${xCoordsNew[1]})")
//        Log.d("FRACTAL", "yCoordsNew:  (${yCoordsNew[0]}, ${yCoordsNew[1]})")


    }
    fun setMapParam(i: Int, dPos: FloatArray) {
        // dx -- [0, screenWidth]
        val sensitivity =
                if (fractalConfig.paramSensitivity() == -1.0) fractalConfig.scale()[0]
                else fractalConfig.paramSensitivity()
        (fractalConfig.params["p$i"] as DoubleArray)[0] += sensitivity*dPos[0]/screenRes[0]
        (fractalConfig.params["p$i"] as DoubleArray)[1] -= sensitivity*dPos[1]/screenRes[1]

        // Log.d("FRACTAL", "setting map param ${p + 1} to (${fractalConfig.map().params[p - 1][0]}, ${fractalConfig.map().params[p - 1][1]})")

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
        //      JULIA :: (0.38168508, -0.20594095) + TRIANGLE INEQ !!!!!

        // MANDELBROT CPOW :: (1.31423213, 2.86942864)
        //      JULIA :: (-0.84765975, -0.02321229)

        // SINE4 :: (1.41421356, 0.0)
        //      JULIA :: (-2.16074089, 0.0)

        // HORSESHOE CRAB :: (-0.02955925, 0.49291033)
        //      JULIA :: (-0.75224080, -0.93949126)

        // HORSESHOE CRAB :: (-0.02926709, 0.48591950)
        //      JULIA :: (-0.74308518, -0.94826022)

        // PERSIAN RUG :: (2.26988707, 0.0)
        //      JULIA :: (-0.04956468, -0.00017392)

        // BURNING SHIP
        //      JULIA :: (0.56311972, -0.88230390)
        //      JULIA :: (0.82628826, -1.15622855)
        //      JULIA :: (0.78515850, -1.14163868)
        //      JULIA :: (-1.75579063, -0.00825099)

        updateDisplayParams(Reaction.valueOf("P$i"), false)
        updateMapParamEditText(i)

    }
    fun setMapParamSensitivity(i: Int, dScale: Float) {
        fractalConfig.params["paramSensitivity"] = fractalConfig.paramSensitivity() * dScale
        updateMapParamEditText(i)
        updateDisplayParams(Reaction.valueOf("P$i"), false)
    }
    fun setTextureParam(i: Int, dPos: FloatArray) {
        // dx -- [0, screenWidth]
        fractalConfig.params["q$i"] = fractalConfig.q1() + dPos[0]/screenRes[0]
        // updateDisplayParams(Reaction.valueOf("P$i"), false)
        updateTextureParamEditText(i)
    }
    fun translate(dScreenPos: FloatArray) {

        // update complex coordinates
        when (settingsConfig.precision()) {
            Precision.QUAD -> {
//                        val dPosDD = arrayOf(
//                                DualDouble((dScreenPos[0].toDouble() / screenRes[0]), 0.0) * (xCoordsDD[1] - xCoordsDD[0]),
//                                DualDouble((dScreenPos[1].toDouble() / screenRes[1]), 0.0) * (yCoordsDD[1] - yCoordsDD[0])
//                        )
//                        xCoordsDD[0] -= dPosDD[0]
//                        xCoordsDD[1] -= dPosDD[0]
//                        yCoordsDD[0] += dPosDD[1]
//                        yCoordsDD[1] += dPosDD[1]
            }
            else -> {
                fractalConfig.coords()[0] -= (dScreenPos[0] / screenRes[0])*fractalConfig.scale()[0]
                fractalConfig.coords()[1] += (dScreenPos[1] / screenRes[1])*fractalConfig.scale()[1]
            }
        }

        updatePositionEditTexts()
        updateDisplayParams(Reaction.TRANSFORM, false)
//        Log.d("FRACTAL", "translation (pixels) -- dx: ${dScreenPos[0]}, dy: ${dScreenPos[1]}")

    }
    private fun translate(dPos: DoubleArray) {

        // update complex coordinates
        when (settingsConfig.precision()) {
            Precision.QUAD -> {
//                        val dPosDD = arrayOf(
//                                DualDouble((dScreenPos[0].toDouble() / screenRes[0]), 0.0) * (xCoordsDD[1] - xCoordsDD[0]),
//                                DualDouble((dScreenPos[1].toDouble() / screenRes[1]), 0.0) * (yCoordsDD[1] - yCoordsDD[0])
//                        )
//                        xCoordsDD[0] -= dPosDD[0]
//                        xCoordsDD[1] -= dPosDD[0]
//                        yCoordsDD[0] += dPosDD[1]
//                        yCoordsDD[1] += dPosDD[1]
            }
            else -> {
                fractalConfig.coords()[0] += dPos[0]
                fractalConfig.coords()[1] += dPos[1]
            }
        }

//        Log.d("FRACTAL", "translation (coordinates) -- dx: ${dPos[0]}, dy: ${dPos[1]}")

    }
    fun scale(dScale: Float, screenFocus: FloatArray) {

        // update complex coordinates
        // convert focus coordinates from screen space to complex space
        val prop = doubleArrayOf(
                screenFocus[0].toDouble() / screenRes[0].toDouble(),
                screenFocus[1].toDouble() / screenRes[1].toDouble()
        )

        val precisionPreScale = precision()
        when (precisionPreScale) {
            Precision.QUAD -> {
//                        val focusDD = arrayOf(
//                                DualDouble(prop[0], 0.0) * (xCoordsDD[1] - xCoordsDD[0]) + xCoordsDD[0],
//                                DualDouble(prop[1], 0.0) * (yCoordsDD[0] - yCoordsDD[1]) + yCoordsDD[1]
//                        )
//                        val dScaleDD = DualDouble(1.0 / dScale.toDouble(), 0.0)
//
//                        // translate focus to origin in complex coordinates
//                        xCoordsDD[0] -= focusDD[0]
//                        xCoordsDD[1] -= focusDD[0]
//                        yCoordsDD[0] -= focusDD[1]
//                        yCoordsDD[1] -= focusDD[1]
//
//                        // scale complex coordinates
//                        xCoordsDD[0] *= dScaleDD
//                        xCoordsDD[1] *= dScaleDD
//                        yCoordsDD[0] *= dScaleDD
//                        yCoordsDD[1] *= dScaleDD
//
//                        // translate origin back to focusDD in complex coordinates
//                        xCoordsDD[0] += focusDD[0]
//                        xCoordsDD[1] += focusDD[0]
//                        yCoordsDD[0] += focusDD[1]
//                        yCoordsDD[1] += focusDD[1]
            }
            else -> {
                val focus = doubleArrayOf(
//                    xCoords[0] * (1.0 - prop[0]) + prop[0] * xCoords[1],
//                    yCoords[1] * (1.0 - prop[1]) + prop[1] * yCoords[0]
                        fractalConfig.coords()[0] + (prop[0] - 0.5)*fractalConfig.scale()[0],
                        fractalConfig.coords()[1] - (prop[1] - 0.5)*fractalConfig.scale()[1]
                )
                // Log.d("FRACTAL", "focus (coordinates) -- x: ${focus[0]}, y: ${focus[1]}")

                translate(focus.negative())
                fractalConfig.coords()[0] = fractalConfig.coords()[0] / dScale
                fractalConfig.coords()[1] = fractalConfig.coords()[1] / dScale
                translate(focus)
                fractalConfig.scale()[0] = fractalConfig.scale()[0] / dScale
                fractalConfig.scale()[1] = fractalConfig.scale()[1] / dScale
            }
        }

//        Log.d("FRACTAL", "length of x-interval: ${abs(xCoords[1] - xCoords[0])}")

        val precisionPostScale = precision()
        if (precisionPostScale != precisionPreScale) {
            renderShaderChanged = true
            Log.d("FRACTAL", "precision changed")
        }

        updatePositionEditTexts()
        updateDisplayParams(Reaction.TRANSFORM, false)
//        Log.d("FRACTAL", "scale -- dscale: $dScale")

    }
    fun setFrequency(dScale: Float) {
        fractalConfig.params["frequency"] = fractalConfig.frequency() * dScale
        updateDisplayParams(Reaction.COLOR, false)
        updateColorParamEditTexts()
    }
    fun setPhase(dx: Float) {
        fractalConfig.params["phase"] = (fractalConfig.phase() + dx/screenRes[0])
        updateDisplayParams(Reaction.COLOR, false)
        updateColorParamEditTexts()
    }

}