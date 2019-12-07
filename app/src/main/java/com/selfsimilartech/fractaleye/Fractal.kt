package com.selfsimilartech.fractaleye

import android.annotation.SuppressLint
import android.util.Log
import kotlin.math.pow

@SuppressLint("SetTextI18n")
class Fractal(

        // shape config
        shape:          Shape                   = Shape.mandelbrot,
        juliaMode:      Boolean                 = shape.juliaMode,
        var position:   Position                = if (juliaMode) shape.positions.julia else shape.positions.default,

        // texture config
        texture:        Texture                 = Texture.escape,

        // color config
        var palette:        ColorPalette        = ColorPalette.night,
        var frequency:      Float               = 1f,
        phase:              Float               = 0f,


        var maxIter:        Int             = 255,
        var sensitivity:    Double          = 1.0,
        var bailoutRadius:  Float           = shape.bailoutRadius ?: texture.bailoutRadius ?: 1e2f

) {


    companion object {

        val mandelbrot = Fractal()
        val m1 = Fractal(
                shape = Shape.mandelbrot,
                position = Shape.mandelbrot.positions.other[2],
                texture = Texture.orbitTrap,
                palette = ColorPalette.p9,
                frequency = 71.74303f,
                phase = -0.26012f,
                maxIter = 2.0.pow(12).toInt()
        )
        val m2 = Fractal(
                shape = Shape.mandelbrot,
                position = Shape.mandelbrot.positions.other[0],
                texture = Texture.overlayAvg
        )
        val verbose = Fractal(
                shape = Shape.mandelbrot,
                position = Shape.mandelbrot.positions.other[1],
                texture = Texture.stripeAvg,
                palette = ColorPalette.night,
                frequency = 25.51949f,
                phase = 0.11335f,
                maxIter = 2.0.pow(10.25).toInt()
        )

        val mandelbrotPower = Fractal(shape = Shape.mandelbrotPower)
        val mandelbrotDualPower = Fractal(shape = Shape.mandelbrotDualPower)
        val mandelbox = Fractal(shape = Shape.mandelbox)
        val kali = Fractal(shape = Shape.kali)
        val burningShip = Fractal(shape = Shape.burningShip)
        val sine1 = Fractal(shape = Shape.sine1)
        val sine2 = Fractal(shape = Shape.sine2)
        val horseshoeCrab = Fractal(shape = Shape.horseshoeCrab)
        val kleinian = Fractal(shape = Shape.kleinian)
        val nova1 = Fractal(shape = Shape.nova1)
        val nova2 = Fractal(shape = Shape.nova2)

    }


    var numParamsInUse = shape.numParams + if (juliaMode && !shape.juliaMode) 1 else 0

    
    var shape = shape
        set (value) {
            if (field != value) {

                Log.d("FRACTAL", "setting shape from $field to $value")
                field = value


                // reset texture if not compatible with new shape
                if (!shape.textures.contains(texture)) {
                    texture = Texture.escape
                }

                juliaMode = shape.juliaMode
                position = if (juliaMode) shape.positions.julia else shape.positions.default
                numParamsInUse = shape.numParams + if (juliaMode && !shape.juliaMode) 1 else 0
                bailoutRadius = shape.bailoutRadius ?: texture.bailoutRadius ?: bailoutRadius

            }
        }

    var juliaMode = juliaMode
        set (value) {
            if (field != value) {

                Log.d("FRACTAL", "setting juliaMode from $field to $value")
                field = value

                numParamsInUse += if (value) 1 else -1
                position = if (value) shape.positions.julia else shape.positions.default

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
//                    f.params["p${f.numParamsInUse()}"] = Shape.Param(
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

                bailoutRadius = shape.bailoutRadius ?: texture.bailoutRadius ?: bailoutRadius

            }
        }

    var phase = phase
        set (value) {
            val mod = value % 1f
            field = if (value < 0f) 1f - mod else mod
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

        // MANDELBOX :: (1.45345266, 0.0)
        //      JULIA :: (-0.01964831, 4.17113384)

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


}