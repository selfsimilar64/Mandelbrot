package com.selfsimilartech.fractaleye

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import kotlin.math.pow

inline fun <T> Iterable<T>.random(predicate: (T) -> Boolean): T {
    return filter(predicate).random()
}


class Fractal(

    val nameId: Int = -1,
    override var name: String = "",
    val thumbnailId: Int = -1,
    var thumbnailPath: String = "",
    override var thumbnail: Bitmap? = null,

    var shape: Shape = Shape.mandelbrot,
    val shapeId: Int? = null,
    val shapeParams: Shape.ParamSet? = null,
    val juliaMode: Boolean? = null,
    val position: Position? = null,

    var texture: Texture = if (shape.isConvergent) Texture.converge else Texture.escapeSmooth,
    val textureId: Int? = null,
    val textureParams: Texture.ParamSet? = null,
    var textureRegion: TextureRegion = TextureRegion.OUT,
    var textureMin: Float = 0f,
    var textureMax: Float = 1f,
    var imagePath: String = "",
    var imageId: Int = R.drawable.flower,

    var palette: Palette = Palette.eye,
    var paletteId: Int? = null,
    val color : ColorConfig = ColorConfig(),

    var customId: Int = -1,
    override var isFavorite: Boolean = false,
    goldFeature: Boolean? = null

) : Customizable, Goldable {


    companion object {

        val emptyFavorite = Fractal(name = "Empty Favorite")
        val emptyCustom = Fractal(name = "Empty Custom")

        val default = Fractal()
        var previous = Fractal(name = "Load Previous")
        var tempBookmark1 = default
        var tempBookmark2 = default
        var tempBookmark3 = default

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


            // SINE4 :: (1.41421356, 0.0)
            //      JULIA :: (-2.16074089, 0.0)


            // HORSESHOE CRAB ::
            //      JULIA ::

            // HORSESHOE CRAB :: (1.0, 0.0) @
            //      x:          -1.58648660503412890
            //      y:           0.09697857522320943
            //      scale:       7.28425e-3
            //      rotation:   -183

            // PERSIAN RUG :: (2.26988707, 0.0)
            //      JULIA :: (-0.04956468, -0.00017392)

            // ABS(SIN(Z^2 - i*Z)) + C
            //      JULIA :: (0.19371092, 0.53478799)

            // csc(z^2 - c^2) + z + c
            //
            //      position: x = -1.0, y = 0.0, zoom = 1.00035, rotation = 90.0
            //      julia: (1.05275438, 0.0)
            //
            //      julia: (0.18360151, -0.15116809)

            // z^6 - 0.5*z^2*c^-1 + c
            //
            //      julia : 0.34033034 - 0.18748498i
            //      position
            //          x : 0.5631456883270495
            //          y : 0.15625812421446417
            //          zoom : 1.37821e-1
            //          rotation : 33.5
            //      texture : Inverse Umbrella
            //          frequency : 0.836

            // z^3 + sin(z) + c
            //
            //      julia : 0.81992837i
            //      texture : Stripe Average (default params)

            // z^2 + z/c
            //
            //      julia : -0.00394863 + 0.00115633i
            //      texture : Circular Orbit Trap (default)
            //          mode : in

            // AMMONITE
            //
            //      julia : -1.03501498 - 0.3581269i
            //      texture : Absolute Distance

            // iabs(z^3) + c
            //
            //      julia : -0.44554545 + 0.01983273i
            //      rotation : -90
            //      texture : Absolute Distance


        }

        val tutorial1 = Fractal(
            name = "tut1",
            shape = Shape.mandelbrot,
            shapeParams = Shape.ParamSet(detail = 64.0),
            position = Position(
                x = -0.77754949627,
                y = -0.13556904821,
                zoom = 3.5,
                rotation = Math.PI/2.0,
                rotationLocked = true
            ),
//            texture = Texture.stripeAvg,
//            textureParams = Texture.ParamSet(
//                arrayListOf(
//                    RealParam(1.0), RealParam(235.0), RealParam(30.0)
//                ),
//                escapeRadius = RealParam(2.0.pow(20.0))
//            ),
            textureRegion = TextureRegion.OUT,
            palette = Palette.eye,
            color = ColorConfig(
                frequency = 0.2,
                fillColor = Color.BLACK,
                outlineColor = Color.WHITE
            ),
            juliaMode = false
        )


//        val mDensityTest1 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = -1.479732701443228,
//                        y = 0.0008044657178647,
//                        zoom = 1.01168e-6,
//                        rotation = (-119.5).inRadians()
//                )
//        )
//        val mGlitch1 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = -0.7683483885236218,
//                        y = -0.1165795261828601,
//                        zoom = 6.24e-12
//                )
//        )
//        val mVid1 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = 0.3375567540479731,
//                        y = -0.05267960943947526,
//                        zoom = 5.72778e-11
//                ),
//                texture = Texture.curvatureAvg,
//                palette = ColorPalette.torus
//        )
//        val mVid2 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = -0.7377039902090139,
//                        y = 0.17448877577323915,
//                        zoom = 5.79295e-11,
//                        rotation = (91.3).inRadians()
//                ),
//                texture = Texture.umbrellaInverse  // freq = 2.059
//
//
//        )
//        val mVid3 = Fractal(
//                shape = Shape.mandelbrotCubic,
//                position = Position(
//                        x = -0.00728939027348551,
//                        y = 1.1169566784910356,
//                        zoom = 7.6085e-11,
//                        rotation = (33.8).inRadians()
//                ),
//                // texture = Texture.umbrellaInverse  // freq = 0.838
//                // texture = Texture.stripeAvg  // freq = 3.032, width = 30
//                texture = Texture.stripeAvg  // freq = 2, phase = -73.736, width = 30
//        )

//        val mVid4 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = -1.3814035645127223,
//                        y = 0.09698477331423325,
//                        zoom = 4.91586e-12,
//                        rotation = (4.0).inRadians()
//                )
//        )

        //        val mSeriesApproxTest1 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        zoom = 1e-36,
//                        rotation = 180.0.inRadians(),
//                        xap = Apfloat("-1.7698932575350291605327642596051289784358915304", 48),
//                        yap = Apfloat("3.6665870040147116377855723420874234222248717255e-3", 48),
//                        ap = 42L
//                ),
//                //maxIter = 4096
//        )
//        val mDeepest1 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        xap = Apfloat("-5.62202521195439460338751400513072567871607195375149113774507282802937241082050340212101321846290569778067679e-1", 112),
//                        yap = Apfloat("-6.42818086354737358752229229456718819199748888047401664097605400917577814750525594280686862736489757054111863e-1", 112),
//                        zoom = 1e-104
//                ),
//                //maxIter = 8092,
//                color = ColorConfig( frequency = 20f,
//                texture = Texture.escape
//        )
//        val mDeepest2 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        xap = Apfloat("-1.0544771725700995178830906306879317879925615621654752792718847805671499760658366475783039836200368628724748964957373100327700143679042450535196539629095952759955254240252230880768112984860588793012200658468926898293330018116840158882393564e-1", 240),
//                        yap = Apfloat("-8.83424379137861398078416491581561703052184463526436214637018584728462323825289990699107469061080852352652917020142164276221872618100201216981273277230846180759168858489800958053877272210486915715162847442759312636496008265026519407741760465e-1", 240),
//                        zoom = 1.05e-200,
//                        rotation = 280.0.inRadians()
//                ),
//                //maxIter = 70000,
//                palette = ColorPalette.p9,
//                color = ColorConfig( frequency = 34748.65234f,
//                phase = 0.19252f
//        )
//        val mError1 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        xap = Apfloat("-1.05447717257009951788309063068793178799256156215620218400544e-1", 60),
//                        yap = Apfloat("-8.83424379137861398078416491581561703052184463526966470742829e-1", 60),
//                        zoom = 5e-47,
//                        rotation = 260.0.inRadians()
//                ),
//                //maxIter = 20000,
//                color = ColorConfig( frequency = 11.75f,
//                phase = 0.7f
//        )
//        val mLongDoubleTest = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        xap = Apfloat("-1.03082297810407865651659914435189621", 32),
//                        yap = Apfloat("-3.60982081830837541008216113996844167e-1", 32),
//                        zoom = 2.4e-21,
//                        rotation = (-152.5).inRadians()
//                ),
//                //maxIter = 4096,
//                color = ColorConfig( frequency = 26.82f,
//                phase = 0.512f
//        )
//        val flake = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        xap = Apfloat("-1.99996619445037030418434688506350579675531241540724851511761922944801584242342684381376129778868913812287046406560949864353810575744772166485672496092803920095332", 175),
//                        yap = Apfloat("0.00000000000000000000000000000000030013824367909383240724973039775924987346831190773335270174257280120474975614823581185647299288414075519224186504978181625478529", 175),
//                        zoom = 1.7e-157
//                ),
//                //maxIter = 35000,
//                color = ColorConfig( frequency = 50f
//        )
//        val m1 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        xap = Apfloat("-1.05825011874730510319979120756264191e-1", 28),
//                        yap = Apfloat("-8.83576342602260643014054651400671031e-1", 28),
//                        zoom = 2.05854e-21,
//                        rotation = 146.3.inRadians()
//                ),
//                palette = ColorPalette.chroma,
//                color = ColorConfig( frequency = 5.7963f,
//                phase = 0.938f,
//                maxIter = 50000
//        )
//        val mDeepest = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = -1.24375615535091620,
//                        y = 0.11621137117223326,
//                        scale = 6.39541e-24
//                ),
//                maxIter = 8192,
//                phase = 0.284f,
//                color = ColorConfig( frequency = 111.73709f
//        )
        val m1 = Fractal(
            nameId = R.string.transformation,
            thumbnailId = R.drawable.transformation_icon,
            shape = Shape.mandelbrot,
            shapeParams = Shape.ParamSet(detail = 2048.0),
            position = Position(
                x = -0.48414790254135703,
                y = -0.59799104457234160,
                zoom = 6.15653e-4
            ),
            texture = Texture.orbitTrapLine,
            textureParams = Texture.ParamSet(
                arrayListOf(
                    RealParam(),
                    RealParam(R.string.rotate, u = 89.531)
                )
            ),
            textureRegion = TextureRegion.BOTH,
            palette = Palette.p9,
            color = ColorConfig(
                frequency = 62.72,
                phase = 0.5
            )
        )
        val m2 = Fractal(
            nameId = R.string.metropolis,
            thumbnailId = R.drawable.metropolis_icon,
            shape = Shape.mandelbrot,
            position = Position(
                x = -1.25735194436369140,
                y = -0.07363029998042227,
                zoom = 1.87845e-3
            ),
            texture = Texture.overlayAvg,
            textureParams = Texture.ParamSet(
                arrayListOf(
                    RealParam(R.string.sharpness, u = 0.495)
                )
            ),
            palette = Palette.cosmic,
            color = ColorConfig(
                frequency = 4.5796,
                phase = 0.568
            )
        )
        val verbose = Fractal(
            nameId = R.string.verbose,
            thumbnailId = R.drawable.verbose_icon,
            shape = Shape.mandelbrot,
            shapeParams = Shape.ParamSet(detail = 1024.0),
            position = Position(
                x = 0.39019590054025366,
                y = -0.26701156160039610,
                zoom = 9.59743e-8,
                rotation = 146.0.inRadians()
            ),
            texture = Texture.stripeAvg,
            palette = Palette.elephant,
            color = ColorConfig(
                frequency = 25.17241,
                phase = 0.68273
            )
        )
        val supernova = Fractal(
            nameId = R.string.supernova,
            thumbnailId = R.drawable.supernova_icon,
            shape = Shape.mandelbrot,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                julia = Complex(0.16111111, -0.60128205),
                detail = 256.0
            ),
            texture = Texture.stripeAvg,
            position = Position(
                zoom = 2.38,
                rotation = 347.0.inRadians()
            ),
            palette = Palette.dragon,
            color = ColorConfig(
                frequency = 0.9801,
                phase = 0.45
            )
        )
        val flora = Fractal(
            nameId = R.string.bloom,
            thumbnailId = R.drawable.bloom_icon,
            shape = Shape.mandelbrot,
            shapeParams = Shape.ParamSet(detail = 1478.0),
            position = Position(
                x = -1.74911968511756500,
                y = -0.00031377609027430,
                zoom = 6.48450e-8,
                rotation = 105.0.inRadians()
            ),
            texture = Texture.curvatureAvg,
            textureParams = Texture.ParamSet(escapeRadius = RealParam(1e12)),
            palette = Palette.flora,
            color = ColorConfig(
                frequency = 38.33518,
                phase = 0.91075
            )
        )
        val m3 = Fractal(
            nameId = R.string.hierarchy,
            thumbnailId = R.drawable.hierarchy_icon,
            shape = Shape.mandelbrot,
            shapeParams = Shape.ParamSet(detail = 1400.0),
            position = Position(
                x = -0.5704813078722608,
                y = 0.46093225541667093,
                zoom = 3.66784e-4,
                rotation = 83.5.inRadians()
            ),
            texture = Texture.curvatureAvg,
            textureParams = Texture.ParamSet(
                arrayListOf(
                    RealParam(R.string.width, u = 30.0)
                )
            ),
            palette = Palette.torus,
            color = ColorConfig(
                frequency = 12.32,
                phase = 0.398
            )
        )

        val m4 = Fractal(
            nameId = R.string.plumage,
            thumbnailId = R.drawable.plumage_icon,
            shape = Shape.mandelbrot,
            shapeParams = Shape.ParamSet(detail = 1024.0),
            position = Position(
                x = -0.9962028022219888,
                y = -0.28981479465540777,
                zoom = 1.32459e-5,
                rotation = 139.8.inRadians()
            ),
            texture = Texture.triangleIneqAvgInt,
            textureParams = Texture.ParamSet(
                escapeRadius = RealParam(2.0.pow(30.0))
            ),
            palette = Palette.night,
            color = ColorConfig(
                frequency = 30.25,
                phase = 0.488
            )
        )

        val m5 = Fractal(
            nameId = R.string.numerical_culture,
            thumbnailId = R.drawable.numerical_culture_icon,
            shape = Shape.mandelbrot,
            position = Position(
                x = -1.2552680888938188,
                y = -0.06143179556901288,
                zoom = 2.44931e-3,
                rotation = (-39.9).inRadians()
            ),
            texture = Texture.stripeAvg,
            textureParams = Texture.ParamSet(
                arrayListOf(
                    RealParam(R.string.frequency, u = 2.0),
                    RealParam(R.string.phase, u = 0.0),
                    RealParam(R.string.width, u = 0.075)
                )
            ),
            palette = Palette.anaglyph,
            color = ColorConfig(
        frequency = 10.75,
                phase = 0.758
            )
        )
        val m6 = Fractal(
            nameId = R.string.pinwheel,
            thumbnailId = R.drawable.pinwheel_icon,
            shape = Shape.mandelbrot,
            shapeParams = Shape.ParamSet(detail = 4096.0),
//                position = Position(
//                        x = -0.1638631206158162,
//                        y = -0.6498791635044113,
//                        zoom = 4.88796e-10,
//                        rotation = 58.6.inRadians()
//                ),
            position = Position(
                x = -0.1638631206158162,
                y = -0.6498791635044113,
                zoom = 1.11e-8,
                rotation = 34.0.inRadians()
            ),
            texture = Texture.escapeSmooth,
            palette = Palette.fusion,
            color = ColorConfig(
                frequency = 0.0256,
                phase = 0.674
            ),
        )

        val m7 = Fractal(
            name = "PRESET 7",
            shape = Shape.mandelbrot,
            position = Position(
                x = -0.803498942145033,
                y = -0.15566004150590984,
                zoom = 1e-8,
                rotation = (-136.4).inRadians()
            ),
            texture = Texture.escapeWithOutline,
            textureParams = Texture.ParamSet(
                arrayListOf(
                    RealParam(R.string.size, u = 0.055)
                )
                // RealParam(R.string.density, u = 1.0)
            ),
            palette = Palette.starling,
            color = ColorConfig(
        frequency = 0.4489,
                phase = 0.49
            )
            // autoColor = true
        )

        val m8 = Fractal(
            nameId = R.string.capillaries,
            thumbnailId = R.drawable.capillaries_icon,
            shape = Shape.mandelbrot,
            position = Position(
                x = -1.3570644130424008,
                y = -0.04122036152539448,
                zoom = 1.5e-8,
                rotation = 1.7.inRadians()
            ),
            shapeParams = Shape.ParamSet(detail = 512.0),
            texture = Texture.curvatureAvg,
            textureParams = Texture.ParamSet(
                escapeRadius = RealParam(2.0.pow(30.0))
            ),
            palette = Palette.anaglyph,
            color = ColorConfig(
                frequency = 17.47,
                phase = 0.66,
                fillColor = Color.BLACK
            )
        )

        val m9 = Fractal(
            nameId = R.string.forgotten,
            thumbnailId = R.drawable.forgotten_icon,
            shape = Shape.mandelbrot,
            shapeParams = Shape.ParamSet(
                julia = Complex(-1.74973765, 0.0000467),
                detail = 2048.0
                // julia = ComplexParam(u = -1.74974992, v = 0.00004225)
                // julia = ComplexParam(u = -1.74898824)
            ),
            juliaMode = true,
            position = Position(zoom = 4.40624e-1),
            texture = Texture.orbitTrapCirc,
            textureParams = Texture.ParamSet(
                arrayListOf(
                    ComplexParam(R.string.center, u = 1.79592889)
                )
            ),
            palette = Palette.torus,
            color = ColorConfig(
                frequency = 0.81,
                phase = 0.48
            )
        )

        val m10 = Fractal(
            nameId = R.string.watermelon,
            thumbnailId = R.drawable.watermelon_icon,
            shape = Shape.mandelbrot,
            juliaMode = true,
            shapeParams = Shape.ParamSet(julia = Complex(0.32472851, 0.0)),
            position = Position(zoom = 2.5),
            texture = Texture.umbrella,
            palette = Palette.cactus,
            color = ColorConfig(
                frequency = 1.0,
                phase = 0.76
            )
        )

        val m11 = Fractal(
            nameId = R.string.perpetual_light,
            thumbnailId = R.drawable.perpetual_light_icon,
            shape = Shape.mandelbrot,
            position = Position(
                x = -1.2553494205905504,
                y = -0.02931447838064166,
                zoom = 2.16703e-5,
                rotation = 93.2.inRadians()
            ),
            texture = Texture.umbrellaInverse,
            textureParams = Texture.ParamSet(
                arrayListOf(
                    RealParam(R.string.frequency, u = 1.536)
                )
            ),
            palette = Palette.polyphonic,
            color = ColorConfig(
        frequency = 8.0656,
                phase = 0.162
            )
        )

        val m12 = Fractal(
            name = "PRESET 12",
            shape = Shape.mandelbrot,
            shapeParams = Shape.ParamSet(detail = 2151.0),
            position = Position(
                x = -0.03925566189175791,
                y = -0.6825971567614861,
                zoom = 4.23938e-9,
                rotation = (1.4).inRadians()
            ),
            texture = Texture.stripeAvg,
            textureParams = Texture.ParamSet(
                arrayListOf(
                    RealParam(R.string.phase, u = 180.0),
                    RealParam(R.string.width, u = 30.0)
                )
            )
        )

        val m13 = Fractal(
            nameId = R.string.alignment,
            thumbnailId = R.drawable.alignment_icon,
            shape = Shape.mandelbrot,
            shapeParams = Shape.ParamSet(detail = 670.0),
            position = Position(
                x = 0.2812207685692142,
                y = -0.00932027230849095,
                zoom = 1.6e-6,
                rotation = (20.2).inRadians()
            ),
            texture = Texture.escapeSmooth,
            palette = Palette.refraction,
            color = ColorConfig(
                frequency = 4.2849,
                phase = 0.42
            )
        )

        val m14 = Fractal(
            name = "PRESET 14",
            shape = Shape.mandelbrot,
            shapeParams = Shape.ParamSet(julia = Complex(0.26207244, 0.00216744)),
            juliaMode = true,
            texture = Texture.escapeWithOutline,
            textureParams = Texture.ParamSet(
                arrayListOf(RealParam(R.string.size, u = 0.274))
            )
        )


        val mquad1 = Fractal(
            nameId = R.string.homeostasis,
            thumbnailId = R.drawable.homeostasis_icon,
            shape = Shape.mandelbrotQuartic,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                julia = Complex(0.59886693, 1.03726528)
            ),
            position = Position(
                x = -0.02953234124394922,
                y = -0.10532001011362754,
                zoom = 7.93786e-1,
                rotation = 27.4.inRadians()
            ),
            texture = Texture.escapeSmooth,
            palette = Palette.aquamarine,
            color = ColorConfig(
                frequency = 0.739,
                phase = 0.972
            )
        )
        val mquad2 = Fractal(
            name = "QUAD 2",
            shape = Shape.mandelbrotQuartic,
            juliaMode = true,
            shapeParams = Shape.ParamSet(julia = Complex(-1.17213209, 0.0)),
            position = Position(zoom = 3.5e-1),
            texture = Texture.stripeAvg,
            palette = Palette.parachute
        )
        val mquad3 = Fractal(
            name = "QUAD 3",
            shape = Shape.mandelbrotQuartic,
            juliaMode = true,
            shapeParams = Shape.ParamSet(julia = Complex(0.64018818, 0.10568383)),
            position = Position(zoom = 2.67959, rotation = 30.5.inRadians()),
            texture = Texture.curvatureAvg,
            textureParams = Texture.ParamSet(
                arrayListOf(RealParam(R.string.width, u = 6.441)),
                escapeRadius = RealParam(1e8)
            ),
            palette = Palette.parachute,
            color = ColorConfig(
                frequency = 1.51290,
                phase = 0.504
            )
        )

        val mquint1 = Fractal(
            name = "QUINT 1",
            shape = Shape.mandelbrotQuintic,
            juliaMode = true,
            shapeParams = Shape.ParamSet(julia = Complex(-0.6954005, 0.23846631)),
            texture = Texture.escapeSmooth
        )

        val nautilus = Fractal(
            nameId = R.string.nautilus,
            thumbnailId = R.drawable.nautilus_icon,
            shape = Shape.mandelbrotPow,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(R.string.power, u = 1.24943476, v = -0.03900028)),
                julia = Complex(0.25946921, -0.37223729)
            ),
            position = Position(x = -0.75, y = 0.075, zoom = 4.5),
            texture = Texture.curvatureAvg,
            palette = Palette.fossil,
            color = ColorConfig(
                frequency = 0.9216,
                phase = 0.444
            )
        )
        val mp1 = Fractal(
            name = "MP 1",
            shape = Shape.mandelbrotPow,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(R.string.power, u = 1.31423213, v = 2.86942864)),
                julia = Complex(-0.84765975, -0.02321229)
            )
        )

        val c1 = Fractal(
            name = "CLOVER 1",
            shape = Shape.clover,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(R.string.power, 3.06925369, 0.0)),
                julia = Complex(-0.53906421, 0.00876671)
            ),
            position = Position(-1.0, 0.0, 3.41891e-1, 0.0),
            texture = Texture.curvatureAvg,
            textureParams = Texture.ParamSet(
                arrayListOf(RealParam(R.string.width, u = 30.0))
            ),
            palette = Palette.peacock,
            color = ColorConfig(
                frequency = 0.67508,
                phase = 0.97322
            )
        )

        val c2 = Fractal(
            name = "CLOVER 2",
            shape = Shape.clover,
            position = Position(
                x = -0.0000159590504726,
                y = -0.00411803970982979,
                zoom = 2.9053e-6,
                rotation = (-119.6).inRadians()
            ),
            texture = Texture.escape
        )


        val bs1 = Fractal(
            nameId = R.string.eukaryote,
            thumbnailId = R.drawable.eukaryote_icon,
            shape = Shape.burningShip,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                julia = Complex(0.70271582, -1.04922401),
                detail = 2048.0
            ),
            position = Position(
                x = 0.0,
                y = 0.3979338977,
                zoom = 7.4e-2
            ),
            palette = Palette.p9,
            color = ColorConfig(
                frequency = 0.38947,
                phase = 0.4
            )
        )
        val bs2 = Fractal(
            name = "BS 2",
            shape = Shape.burningShip,
            juliaMode = true,
            shapeParams = Shape.ParamSet(julia = Complex(0.56311972, -0.88230390))
        )
        val bs3 = Fractal(
            name = "BS 3",
            shape = Shape.burningShip,
            juliaMode = true,
            shapeParams = Shape.ParamSet(julia = Complex(0.82628826, -1.15622855))
        )
        val bs4 = Fractal(
            name = "BS 4",
            shape = Shape.burningShip,
            juliaMode = true,
            shapeParams = Shape.ParamSet(julia = Complex(0.78515850, -1.14163868))
        )
        val bs5 = Fractal(
            nameId = R.string.radio_tower,
            thumbnailId = R.drawable.radio_tower_icon,
            shape = Shape.burningShip,
            position = Position(
                zoom = 1.122,
                rotation = (-45.0).inRadians()
            ),
            juliaMode = true,
            shapeParams = Shape.ParamSet(julia = Complex(-1.75579063, -0.00825099)),
            texture = Texture.sineLens,
            textureParams = Texture.ParamSet(
                arrayListOf(
                    RealParam(0.4),                 // size
                    RealParam(0.0),                 // rotation
                    ComplexParam(-4.1246, 0.23526), // position
                    RealParam(3.0)                  // sharpness
                )
            ),
            palette = Palette.aquamarine,
            color = ColorConfig(
                frequency = 0.4624,
                phase = 0.776,
                fillColor = Color.BLACK
            ),
        )
        val bs6 = Fractal(
            name = "BS 6",
            shape = Shape.burningShip,
            position = Position(
                x = -0.618,
                zoom = 7e-2,
                rotation = 90.0.inRadians()
            ),
            juliaMode = true,
            shapeParams = Shape.ParamSet(julia = Complex(-1.21314957, 0.00826136)),
            texture = Texture.stripeAvg,
            palette = Palette.bioluminescent,
            color = ColorConfig(
                frequency = 222.0,
                phase = 0.85
            )
        )
        val bs7 = Fractal(
            nameId = R.string.black_hole,
            thumbnailId = R.drawable.black_hole_icon,
            shape = Shape.burningShip,
            juliaMode = true,
            shapeParams = Shape.ParamSet(julia = Complex(-1.62542315, 0.0)),
            position = Position(
                zoom = 2.5e-1,
                rotation = 90.0.inRadians()
            ),
            texture = Texture.orbitTrapCirc,
            textureParams = Texture.ParamSet(
                arrayListOf(ComplexParam(R.string.center, 1.015, 0.122))
            ),
            palette = Palette.amethyst,
            color = ColorConfig(
                frequency = 2.31,
                phase = 0.516,
                fillColor = Color.BLACK
            )
        )


        val bsp1 = Fractal(
            name = "BSP 1",
            shape = Shape.burningShipPow,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(R.string.power, 0.24805465, -2.54550288)),
                julia = Complex(-14.29522458, -15.71238251)
            ),
            texture = Texture.escape,
            color = ColorConfig(
                frequency = 0.3844,
                phase = 0.5
            )
        )
        val bsp2 = Fractal(
            name = "BSP 2",
            thumbnailId = R.drawable.crown_icon,
            shape = Shape.burningShipPow,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(R.string.power, 0.62440492, -1.67226752)),
                julia = Complex(-24.85942321, -39.94407828)
            ),
            position = Position(
                zoom = 1.75e2,
                rotation = (45.0).inRadians()
            ),
            texture = Texture.escape,
            palette = Palette.gold,
            color = ColorConfig(
                frequency = 0.756,
                phase = 0.016
            )
        )
        val bsp3 = Fractal(
            name = "TREE PLANET",
            thumbnailId = R.drawable.tree_planet_icon,
            shape = Shape.burningShipPow,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(R.string.power, 1.67084550, 1.05489061)),
                julia = Complex(0.22192777, -1.20128181)
            ),
            position = Position(zoom = 1.2e1),
            texture = Texture.escapeSmooth,
            palette = Palette.cactus,
            color = ColorConfig(
                frequency = 1.0201,
                phase = 0.588
            )
        )

        val bsp4 = Fractal(
            name = "BSP 4",
            shape = Shape.burningShipPow,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(R.string.power, 2.0, 0.92766923)),
                julia = Complex(0.88195649, 0.11055675)
            ),
            position = Position(zoom = 8.0),
            palette = Palette.chroma,
            color = ColorConfig(
                frequency = 0.8112,
                phase = 0.07
            )
        )

        val bsp5 = Fractal(
            nameId = R.string.thought_antannae,
            thumbnailId = R.drawable.thought_antannae_icon,
            shape = Shape.burningShipPow,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(R.string.power, 0.13137502, -2.96284583)),
                julia = Complex(6.37948395, 0.90960284)
            ),
            position = Position(zoom = 14.5),
            textureRegion = TextureRegion.BOTH,
            palette = Palette.peacock,
            color = ColorConfig(
                frequency = 0.5041,
                phase = 0.762
            )
        )

//        val sine1NativeTest = Fractal(
//                shape = Shape.sine,
//                position = Position(
//                        x = -1.4587963652223930,
//                        y = -1.3147301154163030,
//                        zoom = 2.5e-6
//                )
//        )

        val sine1 = Fractal(
            name = "SINE_1 1",
            shape = Shape.sine,
            juliaMode = true,
            shapeParams = Shape.ParamSet(julia = Complex(-0.7460453, 0.49768078)),
            position = Position(
                x = 1.413606639479727,
                y = 0.9228686743093834,
                zoom = 1.0885,
                rotation = 159.0.inRadians()
            ),
            palette = Palette.chroma,
            color = ColorConfig(
                frequency = 0.4563,
                phase = 0.762
            )
        )

        val s1_2 = Fractal(
            nameId = R.string.gravity_of_mind,
            thumbnailId = R.drawable.gravity_of_mind_icon,
            shape = Shape.sine,
            juliaMode = true,
            shapeParams = Shape.ParamSet(julia = Complex(-0.15792634, 0.12450936)),
            // position = Position(1.132273833247264, 0.37712094215622094, 8.61147e-2, 113.7.inRadians()),
            position = Position(
                1.1608286747165415,
                0.39370485791410836,
                8.61147e-2,
                113.7.inRadians()
            ),
            texture = Texture.stripeAvg,
            palette = Palette.atlas,
            color = ColorConfig(
                frequency = 2.89,
                phase = 0.87,
                fillColor = Color.BLACK
            )
        )

        val s1_3 = Fractal(
            name = "SINE_1 3",
            shape = Shape.sine,
            shapeParams = Shape.ParamSet(detail = 2150.0),
            position = Position(
                x = -2.6298003508693286,
                y = -0.52708000273624520,
                zoom = 8.9284e-11,
                rotation = 24.6.inRadians()
            ),
            texture = Texture.escapeSmooth,
            palette = Palette.starling
        )

        val s2_1 = Fractal(
            name = "SINE_2 1",
            shape = Shape.sine2,
            position = Position(x = -0.26282883851642613, zoom = 2.042520182493586e-6)
        )
//
//        val hcNativeTest = Fractal(
//                shape = Shape.horseshoeCrab,
//                position = Position(
//                        x = 0.9001258454914515,
//                        y = 0.00000064000947079,
//                        zoom = 1e-5,
//                        rotation = (-96.1).inRadians()
//                )
//        )

        val hc1 = Fractal(
            name = "HC 1",
            shape = Shape.horseshoeCrab,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(0, -0.02955925, 0.49291033)),
                julia = Complex(-0.75224080, -0.93949126)
            ),
            textureRegion = TextureRegion.BOTH,
            color = ColorConfig(
                frequency = 0.47227,
                phase = 0.19166
            )
        )
        val hc2 = Fractal(
            name = "HC 2",
            shape = Shape.horseshoeCrab,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(0, -0.02926709, 0.48591950)),
                julia = Complex(-0.74308518, -0.94826022)
            ),
            textureRegion = TextureRegion.BOTH,
            color = ColorConfig(
                frequency = 0.47227,
                phase = 0.19166
            )
        )


        val mx1 = Fractal(
            name = "MX 1",
            shape = Shape.mandelex,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(RealParam(R.string.scale, -2.22)),
                julia = Complex(0.0, 0.26496899)
            )
        )

        val mx2 = Fractal(
            name = "MX 2",
            shape = Shape.mandelex,
            position = Position(
                x = -2.0048993919584417,
                y = -2.004682472266098,
                zoom = 1.09681e-1,
                rotation = 135.0.inRadians()
            )
        )

        val novaSpiral = Fractal(
            name = "NOVA SPIRAL",
            shape = Shape.nova1,
            juliaMode = true,
            shapeParams = Shape.ParamSet(julia = Complex(0.70277778, -1.12868152)),
            position = Position(
                x = -0.54335401430352640,
                y = -0.23102467195610776,
                zoom = 4.30288e-3,
                rotation = 75.2.inRadians()
            ),
            color = ColorConfig(
                frequency = 0.48,
                phase = 0.192
            )
        )


        val mag1_1 = Fractal(
            nameId = R.string.peppermint,
            thumbnailId = R.drawable.peppermint_icon,
            shape = Shape.magnet1,
            juliaMode = true,
            shapeParams = Shape.ParamSet(julia = Complex(1.20209909, 0.00114356)),
            position = Position(
                x = -0.18995022496666214,
                y = -1.9275025513422637,
                zoom = 1.3,
                rotation = (-74.5).inRadians()
            ),
            texture = Texture.escapeSmooth,
            palette = Palette.polyphonic,
            color = ColorConfig(
                frequency = 0.2809,
                fillColor = Color.WHITE
            )
        )
        val mag1_2 = Fractal(
            name = "TWO'S COMPLIMENT",
            shape = Shape.magnet1,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(0, 1.10988652, 1.27607142)),
                julia = Complex(-2.38768484, 1.32304182)
            ),
            position = Position(
                x = -1.9146739905393257,
                y = -0.3133017742571375,
                zoom = 1.52665,
                rotation = 180.0.inRadians()
            ),
            texture = Texture.stripeAvg,
            textureParams = Texture.ParamSet(
                arrayListOf(
                    RealParam(R.string.frequency, 5.0),
                    RealParam(R.string.phase, 216.0),
                    RealParam(R.string.width, 30.0)
                )
            )
        )


        val mbx1 = Fractal(
            name = "MBX 1",
            shape = Shape.mandelbox,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(0, 0.60267262, 0.94995500)),
                julia = Complex(-15.00327866, 43.11857865)
            )
        )
        val armada = Fractal(
            nameId = R.string.armada,
            thumbnailId = R.drawable.armada_icon,
            shape = Shape.mandelbox,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(
                    // ComplexParam(0, 0.60267, 0.949955),
                    // ComplexParam(0, 0.53537926, -0.84461183),
                    ComplexParam(0, 1.125, -0.000436),
                    RealParam(R.string.scale, u = 0.3981641)
                    // ComplexParam(1, 0.2131688, -0.33629411)
                ),
                julia = Complex(-1.84318884, 2.30011367)
            ),
            textureParams = Texture.ParamSet(
                escapeRadius = RealParam(2.0.pow(2.3))
            ),
            palette = Palette.polyphonic,
            color = ColorConfig(
                frequency = 0.36,
                phase = 0.458
            )
        )
        val mbx3 = Fractal(
            name = "MBX 3",
            shape = Shape.mandelbox,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(0, 1.58564605, 0.06087502)),
                julia = Complex(5.75040877, 5.75041244)
            )
        )
        val mbx4 = Fractal(
            name = "MBX 4",
            shape = Shape.mandelbox,
            juliaMode = true,
            shapeParams = Shape.ParamSet(listOf(ComplexParam(0, 1.69056660, -1.66451872)))
        )
        val mbx5 = Fractal(
            name = "MBX 5",
            shape = Shape.mandelbox,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(0, 1.45345266, 0.0)),
                julia = Complex(-0.01964831, 4.17113384)
            )
        )
        val mbx6 = Fractal(
            nameId = R.string.canopy,
            thumbnailId = R.drawable.canopy_icon,
            shape = Shape.mandelbox,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(0, u = -0.95421800, v = -0.76819397)),
                julia = Complex(-0.99635701, 2.48995369)
            ),
            textureParams = Texture.ParamSet(
                escapeRadius = RealParam(2.0.pow(2.34))
            ),
            position = Position(
                x = -1.0,
                y = 1.0,
                zoom = 2.15,
                rotation = (-180.0).inRadians()
            ),
            palette = Palette.island,
            color = ColorConfig(
                frequency = 0.3364,
                phase = 0.574
            )
        )
        val mbx7 = Fractal(
            nameId = R.string.reactor,
            thumbnailId = R.drawable.reactor_icon,
            shape = Shape.mandelbox,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(0, u = -2.66421354, v = 0.0)),
                julia = Complex(3.26525985, 3.28239093)
            ),
            position = Position(
                x = -1.08072452128498540,
                y = -0.91860192005481600,
                zoom = 2.66877e-2
            ),
            texture = Texture.orbitTrapLine,
            textureParams = Texture.ParamSet(
                arrayListOf(
                    RealParam(),
                    RealParam(R.string.rotate, u = 89.3)
                )
            ),
            palette = Palette.refraction,
            color = ColorConfig(
                frequency = 0.864,
                phase = 0.656
            )
        )


        val masquerade = Fractal(
            nameId = R.string.masquerade,
            thumbnailId = R.drawable.masquerade_icon,
            shape = Shape.kali,
            shapeParams = Shape.ParamSet(julia = Complex(-0.96665798, -0.02109066)),
            texture = Texture.escape,
            textureParams = Texture.ParamSet(
                escapeRadius = RealParam(2.0.pow(2.35))
            ),
            palette = Palette.honor,
            color = ColorConfig(
                frequency = 0.1764,
                phase = 0.95
            )
        )

        val bf1 = Fractal(
            nameId = R.string.porcelain,
            thumbnailId = R.drawable.porcelain_icon,
            shape = Shape.angelbrot,
            juliaMode = true,
            position = Position(zoom = 2.5),
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(0, 1.23172118, 0.0)),
                julia = Complex(-0.96727896, 0.0)
            ),
            palette = Palette.elephant,
            color = ColorConfig(
                frequency = 1.8496,
                phase = 0.826
            )
        )
        val bf2 = Fractal(
            nameId = R.string.synchronized_swimming,
            thumbnailId = R.drawable.synchronized_swimming_icon,
            shape = Shape.angelbrot,
            juliaMode = true,
            position = Position(zoom = 2.67),
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(0, 0.50611401, 0.0)),
                julia = Complex(-0.28616842, -0.20683480)
            ),
            palette = Palette.bioluminescent,
            color = ColorConfig(
                frequency = 2.8561,
                phase = 0.896
            )
        )
        val bf3 = Fractal(
            nameId = R.string.ephemeral,
            thumbnailId = R.drawable.ephemeral_icon,
            shape = Shape.angelbrot,
            juliaMode = true,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(0, 1.23172118, 0.0)),
                julia = Complex(-1.97994954, -0.06283444),
                detail = 2048.0
            ),
            position = Position(
                zoom = 2.5,
                rotation = (-45.0).inRadians()
            ),
            texture = Texture.escapeWithOutline,
            textureParams = Texture.ParamSet(
                arrayListOf(RealParam(R.string.size, u = 0.217))
            ),
            textureRegion = TextureRegion.BOTH,
            palette = Palette.p9,
            color = ColorConfig(
                frequency = 1.0,
                phase = 0.65
            )
        )

        val epicenter = Fractal(
            nameId = R.string.epicenter,
            thumbnailId = R.drawable.epicenter_icon,
            shape = Shape.mandelbrotQuartic,
            position = Position(
                zoom = 3.92e-12,
                rotation = (-92.0).inRadians(),
                x = -1.1202783562484184,
                y = -0.01068235251833857
            ),
            palette = Palette.dawn,
            color = ColorConfig(
                frequency = 0.418,
                phase = 0.984,
                fillColor = Color.WHITE
            ),
        )

        val invertedForest = Fractal(
            nameId = R.string.invertedforest,
            thumbnailId = R.drawable.invertedforest_icon,
            position = Position(zoom = 4.0),
            shape = Shape.tsquare,
            shapeParams = Shape.ParamSet(
                listOf(
                    RealParam(R.string.shift, u = 0.5754),
                    RealParam(R.string.scale, u = 1.7048),
                    RealParam(R.string.rotate, u = 45.0)
                )
            ),
            palette = Palette.who,
            color = ColorConfig(
        frequency = 1.207,
                phase = 0.94
            )
        )

        val sundial = Fractal(
            nameId = R.string.sundial,
            thumbnailId = R.drawable.sundial_icon,
            shape = Shape.mandelbrot,
            shapeParams = Shape.ParamSet(detail = 2048.0),
            position = Position(
                x = -1.4742797671188836,
                y = 0.0,
                zoom = 1.57e-9,
                rotation = 90.0.inRadians()
            ),
            palette = Palette.time,
            color = ColorConfig(
                frequency = 3.709,
                phase = 0.294,
                density = 3.0,
                fillColor = Color.BLACK
            ),
            texture = Texture.distanceEstimation,
            textureRegion = TextureRegion.BOTH
        )

        val mantra = Fractal(
            nameId = R.string.mantra,
            thumbnailId = R.drawable.mantra_icon,
            shape = Shape.mandelbrot,
            juliaMode = true,
            shapeParams = Shape.ParamSet(julia = Complex(-0.2287, -0.69842)),
            position = Position(
                x = -0.31407533880148586,
                y = -0.42896510395574367,
                zoom = 2.21e-1,
                rotation = 38.9.inRadians()
            ),
            palette = Palette.eye,
            color = ColorConfig(
                frequency = 2.2,
                phase = 0.07
            ),
            texture = Texture.starLens,
            textureParams = Texture.ParamSet(
                arrayListOf(
                    RealParam(R.string.scale, u = 0.5),
                    RealParam(R.string.rotate, u = 64.512),
                    ComplexParam(R.string.shift, u = 0.20592, v = -0.279),
                    RealParam(R.string.smooth, u = 1.0)
                )
            )
        )

        val mitosis = Fractal(
            nameId = R.string.mitosis,
            thumbnailId = R.drawable.mitosis_icon,
            shape = Shape.kleinian,
            shapeParams = Shape.ParamSet(
                listOf(ComplexParam(u = 1.9459, v = 0.01707)),
                julia = Complex.ZERO
            ),
            position = Position(
                x = 0.0,
                y = 0.973291857,
                zoom = 1.96,
                rotation = 90.0.inRadians()
            ),
            texture = Texture.kleinianDistance,
            textureParams = Texture.ParamSet(
                arrayListOf(
                    RealParam(R.string.size, u = 0.0759)
                )
            ),
            textureRegion = TextureRegion.BOTH,
            palette = Palette.yinyang,
            color = ColorConfig(
                frequency = 0.151,
                phase = 0.036,
                outlineColor = Color.rgb(255, 228, 142)
            ),
        )

        val defaultList = arrayListOf(
            mitosis,
            mantra,
            sundial,
            epicenter,
            invertedForest,
            bf1, bf2, bf3,
            masquerade,
            armada, mbx6, mbx7,
            mag1_1,
            bs1, bs5, bs7,
            s1_2,
            m1, m2, m3, m4, m5, m6, m8, m9, m10, m11, m13, verbose, flora, supernova,
            mquad1,
            nautilus
        )

        val all = ArrayList<Fractal>(defaultList)

        val bookmarks = arrayListOf<Fractal>()

        var nextCustomFractalNum = 1

    }


    //    var phase = phase
//        set(value) {
//            var rem = value.rem(1f)
//            if (rem < 0.0) rem += 1f
//            field = rem
//        }

    var loadError = false
    override var goldFeature =
        goldFeature ?: (
            shape.goldFeature ||
            shape.params.list.let {
                var hasGoldParam = false
                it.forEachIndexed { i, p ->
                    if (p.goldFeature && shapeParams != null && shapeParams.list.getOrNull(i) != null) hasGoldParam =
                        true
                }
                hasGoldParam
            } ||
            (shape.juliaMode && shape != Shape.mandelbrot) ||
            texture.goldFeature ||
            texture.params.list.let {
                var hasGoldParam = false
                it.forEachIndexed { i, p ->
                    if (p.goldFeature && textureParams != null && textureParams.list.getOrNull(i) != null) hasGoldParam =
                        true
                }
                hasGoldParam
            }
        )

    val hasCustomId: Boolean
        get() = customId != -1



    init {

//        val prevRadius = texture.params.radius.u
        if (shapeId != null) {
            val loadShape = Shape.all.find { it.id == shapeId }
            if (loadShape != null) this.shape = loadShape else loadError = true
        }
        if (textureId != null) {
            val loadTexture = Texture.all.find { it.id == textureId }
            if (loadTexture != null) this.texture = loadTexture else loadError = true
        }
        if (paletteId != null) {
            val loadPalette = Palette.all.find { it.id == paletteId }
            if (loadPalette != null) this.palette = loadPalette else loadError = true
        }
//        texture.params.radius.u = prevRadius

    }


    fun initialize(resources: Resources) {

        if (nameId != -1) name = resources.getString(nameId)
        if (thumbnailId != -1) thumbnail = BitmapFactory.decodeResource(
            resources,
            thumbnailId,
            BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })

    }

    override fun getName(localizedResource: Resources): String {
        return if (isCustom()) name else localizedResource.getString(nameId)
    }

    fun updateRadius() {
        texture.params.radius.run {
            Log.d("FRACTAL", "scaled value: %e, min radius: %e, new value: ${scale.convert(shape.minRadius.toDouble())}".format(scaledValue, shape.minRadius))
            if (scaledValue > shape.minRadius) u = scale.convert(shape.minRadius.toDouble())
        }
    }

    fun interpolate(f1: Fractal, f2: Fractal, transition: Video.Transition, r: Double) {

        if (f1.position != null && f2.position != null) {
            val s = scurve(r, 0.5*(1.0 - transition.linearity))
            val t = scurve(r, 0.45)
            shape.position.apply {
                f1.position.let { p1 ->
                    f2.position.let { p2 ->

                        val alpha = (p1.x*p2.zoom - p2.x*p1.zoom)/(p2.zoom - p1.zoom)
                        val beta  = (p1.y*p2.zoom - p2.y*p1.zoom)/(p2.zoom - p1.zoom)
                        x = (p1.x - alpha) * (p2.zoom/p1.zoom).pow(s) + alpha
                        y = (p1.y - beta)  * (p2.zoom/p1.zoom).pow(s) + beta

                        zoom = p1.zoom*(p2.zoom/p1.zoom).pow(s)
                        rotation = s*p2.rotation + (1.0 - s)*(p1.rotation + 2.0*Math.PI*transition.rotations)

                    }
                }
            }
            shape.params.detail.u = s*f2.shapeParams?.detail?.u!! + (1.0 - s)*f1.shapeParams?.detail?.u!!
        }

//        color.run {
//            phase = 45.0*(1.0 - r)
//        }

    }

    fun preload(bookmark: Fractal) {
        shape = bookmark.shape
        texture = bookmark.texture
    }

    fun load(bookmark: Fractal, fsv: FractalSurfaceView) {

        shape = bookmark.shape
        shape.reset()
        bookmark.juliaMode?.let { shape.juliaMode = it }
        if (bookmark.shapeParams != null) shape.params.setFrom(bookmark.shapeParams)
        if (bookmark.position != null) shape.position.setFrom(bookmark.position)

        texture = bookmark.texture
        texture.reset()
        if (SettingsConfig.autofitColorRange && texture in listOf(
                Texture.outline,
                Texture.escapeWithOutline,
                Texture.kleinianDistance
            )
        ) {
            SettingsConfig.autofitColorRange = false
            fsv.r.calcNewTextureSpan = true
        }
        if (bookmark.textureParams != null) texture.params.setFrom(bookmark.textureParams)
        textureRegion = bookmark.textureRegion
        imagePath = bookmark.imagePath
        imageId = bookmark.imageId
        if (texture.hasRawOutput) fsv.r.loadTextureImage = true

        palette = bookmark.palette
        bookmark.color.let { config ->
            color.setFrom(config)
            if (config.density == 0.0) {
                fsv.r.setTextureSpan(0f, 1f)
                SettingsConfig.autofitColorRange = false
            } else {
                fsv.r.setTextureSpan(bookmark.textureMin, bookmark.textureMax)
                SettingsConfig.autofitColorRange = true
            }
        }

    }

    fun toDatabaseEntity(): FractalEntity {
        Log.d("FRACTAL", "image path: $imagePath, image id: $imageId")
        return FractalEntity(

            if (hasCustomId) customId else 0,
            name,
            isFavorite,
            thumbnailPath,

            shape.id,
            shape.hasCustomId,
            shape.juliaMode,
            shape.params.detail.scaledValue.toInt(),
            shape.params.list.getOrNull(0)?.toData(0),
            shape.params.list.getOrNull(1)?.toData(1),
            shape.params.list.getOrNull(2)?.toData(2),
            shape.params.list.getOrNull(3)?.toData(3),
            if (shape.juliaMode) shape.params.julia.toData(-1) else null,
            shape.params.seed.toData(-1),
            shape.position.toData(),

            texture.id,
            texture.hasCustomId,
            textureRegion.ordinal,
            texture.params.radius.scaledValue.toFloat(),
            textureMin,
            textureMax,
            imagePath,
            Texture.defaultImages.indexOf(imageId),
            texture.params.list.getOrNull(0)?.toData(0),
            texture.params.list.getOrNull(1)?.toData(1),
            texture.params.list.getOrNull(2)?.toData(2),
            texture.params.list.getOrNull(3)?.toData(3),

            palette.id,
            palette.hasCustomId,
            color.frequency.toFloat(),
            color.phase.toFloat(),
            color.density.toFloat(),
            color.fillColor,
            color.outlineColor

        )
    }

    fun bookmark(fsv: FractalSurfaceView): Fractal {

        val rawFreq: Double
        val rawPhase: Double
        val M = fsv.r.textureSpan.max()
        val m = fsv.r.textureSpan.min()
        val L = M - m

        color.run {

            if (SettingsConfig.autofitColorRange && density == 0.0) {

                rawFreq = if (texture.hasRawOutput) 1.0 else frequency / L
                rawPhase = phase - frequency * m / L

            } else {

                rawFreq = frequency
                rawPhase = phase

            }

            Log.d("FRACTAL", "raw phase: $rawPhase, raw frequency: $rawFreq")


            return Fractal(

                name = name,
                goldFeature = false,

                shape = shape,
                shapeId = shapeId,
                shapeParams = shape.params.clone(),
                juliaMode = shape.juliaMode,
                position = shape.position.clone(),

                texture = texture,
                textureId = textureId,
                textureParams = texture.params.clone(),
                textureRegion = textureRegion,
                textureMin = (if (density != 0.0) m else 0.0).toFloat(),
                textureMax = (if (density != 0.0) M else 1.0).toFloat(),
                imagePath = imagePath,
                imageId = imageId,

                palette = palette,
                paletteId = paletteId,
                color = ColorConfig(
                    frequency = rawFreq,
                    phase = rawPhase,
                    density = density,
                    fillColor = fillColor,
                    outlineColor = outlineColor
                )

            )

        }

    }

    fun randomize(
        fsv: FractalSurfaceView,
        randomizeShape: Boolean,
        randomizeTexture: Boolean,
        randomizePosition: Boolean,
        randomizeColor: Boolean
    ) {

        if (randomizeShape) {

            if (randomizePosition) {
                fsv.r.checkThresholdCross {

                    shape = Shape.default.filter { s ->
                        s.randomConfigs.any { config ->
                            SettingsConfig.goldEnabled || !config.isGoldFeature(s)
                        }
                    }.filterGold().random()

                    shape.reset()
                    val config = shape.randomConfigs.filter { SettingsConfig.goldEnabled || !it.isGoldFeature(shape) }.random()
                    shape.setFrom(config)
                    val t = Math.random()
                    shape.position.apply {
                        if (zoom < 1e-2 && config.juliaMode != true) zoom = t*zoom + (1.0 - t)*1e-2
                        rotation = Math.random() * 2.0 * Math.PI
                    }

                }
            } else {
                fsv.r.checkThresholdCross {
                    shape = Shape.all.filterGold().random()
                    shape.reset()
                    shape.params.list.filterGold().forEach { if (!it.devFeature) it.randomize(1.0) }
                }
            }

//                shape.params.list.forEach { it.randomizePerturb(0.15) }
//                if (shape.juliaMode) { shape.params.julia.randomizePerturb(1.0 - t) }

            if ((SettingsConfig.goldEnabled || shape == Shape.mandelbrot) && !shape.juliaMode && Math.random() < 0.2) {
                shape.params.julia.setFrom(
                    ComplexParam(
                        shape.position.x,
                        shape.position.y
                    )
                )
                if (Math.random() < 0.5) shape.params.julia.randomizePerturb(0.25)
                shape.juliaMode = true
                shape.position.reset()
            }

            shape.params.detail.setFrom(RealParam(512.0))
        }

        if (randomizeTexture) {
            texture =
                shape.compatTextures.random { (SettingsConfig.goldEnabled || !it.goldFeature) && !it.devFeature && it != Texture.orbitTrapImageOver }
            texture.params.list.filterGold().forEach {
                if (Math.random() < 0.5) it.randomize(1.0)
            }
            if (texture == Texture.orbitTrapImageUnder) {
                imageId = Texture.defaultImages.random()
                fsv.r.loadTextureImage = true
            }
        }

        if (randomizeColor) {
            palette = Palette.all.random()
            color.apply {
                frequency = Math.random() + 1.0
                phase = Math.random()
                if (Math.random() < 0.5) {
                    textureRegion = TextureRegion.BOTH
                } else {
                    textureRegion = TextureRegion.OUT
                    fillColor =
                        if (Math.random() < 0.5) palette.getColor(
                            Math.random().toFloat()
                        )
                        else listOf(Color.WHITE, Color.BLACK).random()
                }
                if (texture.usesAccent) {
                    outlineColor =
                        if (Math.random() < 0.5) palette.getColor(
                            Math.random().toFloat()
                        )
                        else listOf(Color.WHITE, Color.BLACK).random()
                }
            }
        }

        SettingsConfig.autofitColorRange = texture !in listOf(Texture.outline, Texture.escapeWithOutline, Texture.orbitTrapImageUnder)
        fsv.r.calcNewTextureSpan = true

        updateRadius()

    }

    fun generateStarredKey(usResources: Resources): String {
        return "Bookmark${usResources.getString(nameId).replace(" ", "")}Starred"
    }

    override fun equals(other: Any?): Boolean {
        return other is Fractal && other.name == name
    }

    override fun isCustom(): Boolean = hasCustomId


}