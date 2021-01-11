package com.selfsimilartech.fractaleye

import android.annotation.SuppressLint
import android.graphics.Color
import kotlin.math.min

@SuppressLint("SetTextI18n")
class Fractal(

        // shape config
        shape:               Shape              = Shape.mandelbrot,
        shapeParams:         Shape.ParamList?   = null,
        // juliaMode:           Boolean            = shape.juliaMode,
        var position:        Position           = if (shape.juliaMode) shape.positions.julia else shape.positions.default,

        // texture config
        texture:             Texture            = if (shape.isConvergent) Texture.converge else Texture.escapeSmooth,
        var textureMode:     TextureMode        = TextureMode.OUT,

        // color config
        var palette:         ColorPalette       = ColorPalette.night,
        var frequency:       Float              = 1f,
        phase:               Float              = 0.65f,
        var density:         Float              = 0.999f,
        var solidFillColor:  Int                = Color.WHITE,


        //var maxIter:         Int                = 1024,
        var sensitivity:     Double             = 2.0,
        var bailoutRadius:   Float              = min(shape.radius, texture.radius)

) {


    companion object {

//        val test = Fractal(shape = Shape.testshape)
        val mandelbrot = Fractal()
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

        val mVid4 = Fractal(
                shape = Shape.mandelbrot,
                position = Position(
                        x = -1.3814035645127223,
                        y = 0.09698477331423325,
                        zoom = 4.91586e-12,
                        rotation = (4.0).inRadians()
                )
        )

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
//                frequency = 20f,
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
//                frequency = 34748.65234f,
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
//                frequency = 11.75f,
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
//                frequency = 26.82f,
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
//                frequency = 50f
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
//                frequency = 5.7963f,
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
//                frequency = 111.73709f
//        )
//        val m1 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = -0.48414790254135703,
//                        y = -0.59799104457234160,
//                        zoom = 6.15653e-4
//                ),
//                texture = Texture.orbitTrapLine,
//                palette = ColorPalette.p9,
//                frequency = 71.74303f,
//                phase = -0.26012f,
//                maxIter = 2.0.pow(14).toInt()
//        )
//        val m2 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = -1.25735194436369140,
//                        y = -0.07363029998042227,
//                        scale = 1.87845e-3
//                ),
//                texture = Texture.overlayAvg
//        )
//        val verbose = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = 0.39019590054025366,
//                        y = -0.26701156160039610,
//                        // xap = Apfloat(0.39019590054025366, AP_DIGITS),
//                        // yap = Apfloat(-0.26701156160039610, AP_DIGITS),
//                        zoom = 9.59743e-8,
//                        rotation = 146.0.inRadians()
//                ),
//                texture = Texture.stripeAvg,
//                palette = ColorPalette.night,
//                frequency = 1.40904f,
//                phase = 0.99570f
//                //maxIter = 2.0.pow(10.25).toInt()
//        )
//        val supernova = Fractal(
//                shape = Shape.mandelbrot,
//                //juliaMode = true,
//                shapeParams = Shape.ParamList(julia = ComplexParam(u = 0.16111111, v = -0.60128205)),
//                texture = Texture.stripeAvg,
//                position = Position(
//                        zoom = 1.18236,
//                        rotation = 347.0.inRadians()
//                ),
//                // maxIter = 298,
//                frequency = 6.93958f,
//                phase = 0.39675f
//        )
//        val flora = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = -1.74911968511756500,
//                        y = -0.00031377609027430,
//                        zoom = 6.48450e-8,
//                        rotation = 105.0.inRadians()
//                ),
//                // maxIter = 1478,
//                texture = Texture.curvatureAvg,
//                palette = ColorPalette.flora,
//                frequency = 27.22222f,
//                phase = 0.20167f
//        )
//        val m3 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = -0.5704813078722608,
//                        y = 0.46093225541667093,
//                        zoom = 3.66784e-4,
//                        rotation = 83.5.inRadians()
//                ),
//                texture = Texture.curvatureAvg,
//                //thickness = 30.0,
//                //maxIter = 4096,
//                palette = ColorPalette.torus,
//                frequency = 172.86172f,
//                phase = 0.976f,
//                density = 0.001f
//        )
//
//        val m4 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = -0.9962028022219888,
//                        y = -0.28981479465540777,
//                        zoom = 1.32459e-5,
//                        rotation = 139.8.inRadians()
//                ),
//                texture = Texture.triangleIneqAvgInt,
//                palette = ColorPalette.night,
//                frequency = 0.5547f,
//                phase = 0.936f
//        )
//
//        val m5 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = -1.2552680888938188,
//                        y = -0.06143179556901288,
//                        zoom = 2.44931e-3,
//                        rotation = (-39.9).inRadians()
//                ),
//                texture = Texture.escapeSmooth,
//                palette = ColorPalette.starling
//        )
//        val m6 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = -0.1638631206158162,
//                        y = -0.6498791635044113,
//                        zoom = 4.88796e-10,
//                        rotation = 58.6.inRadians()
//                ),
//                texture = Texture.escapeSmooth,
//                palette = ColorPalette.starling
//                // maxIter = MAX
//        )

//        val m7 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = -0.803498942145033,
//                        y = -0.15566004150590984,
//                        zoom = 1e-8,
//                        rotation = (-136.4).inRadians()
//                ),
//                texture = Texture.escapeSmooth,
//                palette = ColorPalette.neon
//        )
//
//        val m8 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = -1.3570644130424008,
//                        y = -0.04122036152539448,
//                        zoom = 1.5e-8,
//                        rotation = 1.7.inRadians()
//                ),
//                texture = Texture.curvatureAvg,
//                palette = ColorPalette.torus
//        )
//
//        val m9 = Fractal(
//                shape = Shape.mandelbrot,
//                // julia : -1.74898824
//                // julia : -1.74974992 + 0.00004225i
//                // julia : -1.74973765 + 0.0000467i
//                position = Position(zoom = 4.40624e-1),
//                texture = Texture.orbitTrapR,
//                // orbit center : 1.79592889 + 0i
//                // circular shape
//                palette = ColorPalette.torus
//        )
//
//        val m10 = Fractal(
//                shape = Shape.mandelbrot,
//                // julia : 0.34272851
//                position = Position(zoom = 2.39921),
//                texture = Texture.test,
//                palette = ColorPalette.cactus,
//                frequency = 1f,
//                phase = 0.76f
//        )
//
//        val m11 = Fractal(
//                shape = Shape.mandelbrot,
//                position = Position(
//                        x = -1.2553494205905504,
//                        y = -0.02931447838064166,
//                        zoom = 2.16703e-5,
//                        rotation = 93.2.inRadians()
//                ),
//                texture = Texture.umbrellaInverse,  // frequency = 1.536
//                palette = ColorPalette.polyphonic,
//                frequency = 8.0656f,
//                phase = 0.52f
//        )

        val m12 = Fractal(
                shape = Shape.mandelbrot,
                position = Position(
                        x = -0.03925566189175791,
                        y = -0.6825971567614861,
                        zoom = 4.23938e-9,
                        rotation = (1.4).inRadians()
                ),
                texture = Texture.stripeAvg  // default -- width: 30, phase: 180
        )

//        val mandelbrotCubic = Fractal(shape = Shape.mandelbrotCubic)
//        val mandelbrotQuartic = Fractal(shape = Shape.mandelbrotQuartic)

//        val mquad1 = Fractal(
//                shape = Shape.mandelbrotQuartic,
//                // julia : 0.59886693 + 1.03726528i
//                position = Position(
//                        x = -0.02953234124394922,
//                        y = -0.10532001011362754,
//                        zoom = 7.93786e-1,
//                        rotation = 27.4.inRadians()
//                ),
//                texture = Texture.escapeSmooth,
//                palette = ColorPalette.aquamarine,
//                frequency = 1.0201f,
//                phase = 0.354f
//        )
//        val mquad2 = Fractal(
//                shape = Shape.mandelbrotQuartic,
//                // julia : -1.17213209
//                position = Position(zoom = 3.5e-1),
//                texture = Texture.stripeAvg,
//                palette = ColorPalette.parachute
//        )
//        val mquad3 = Fractal(
//                shape = Shape.mandelbrotQuartic,
//                // julia : 0.64018818 + 0.10568383i
//                position = Position(zoom = 2.67959, rotation = 30.5.inRadians()),
//                texture = Texture.curvatureAvg,  // width : 6.441
//                palette = ColorPalette.parachute,
//                frequency = 1.51290f,
//                phase = 0.572f,
//                bailoutRadius = 1e8f
//        )
//
//        val mquint1 = Fractal(
//                shape = Shape.mandelbrotQuintic,
//                // julia : -0.6954005 + 0.23846631i
//                texture = Texture.escapeSmooth
//        )

//        val mandelbrotAnyPower = Fractal(shape = Shape.mandelbrotPow)
//        val nautilus = Fractal(
//                shape = Shape.mandelbrotAnyPower,
//                juliaMode = true,
//                shapeParams = Shape.ParamList(
//                        listOf(Shape.Param(1.24943476, -0.03900028)),
//                        Shape.Param(0.25946921, -0.37223729)
//                ),
//                position = Position(x = -0.75, y = 0.075, scale = 4.5),
//                texture = Texture.curvatureAvg,
//                frequency = 1.60128f,
//                phase = 0.14218f
//        )
//        val mp1 = Fractal(
//                shape = Shape.mandelbrotAnyPower,
//                juliaMode = true,
//                shapeParams = Shape.ParamList(
//                        listOf(Shape.Param(1.31423213, 2.86942864)),
//                        Shape.Param(-0.84765975, -0.02321229)
//                )
//        )

//        val clover = Fractal(shape = Shape.clover)
//        val c1 = Fractal(
//                shape = Shape.clover,
//                shapeParams = Shape.ParamList(
//                        listOf(Shape.Param(3.06925369, 0.0)),
//                        Shape.Param(-0.53906421, 0.00876671)
//                ),
//                position = Position(-1.0, 0.0, 3.41891e-1, 0.0),
//                texture = Texture.curvatureAvg,
//                // textureParams = Texture.ParamList(Texture.Param(30.0))
//                palette = ColorPalette.peacock,
//                frequency = 0.67508f,
//                phase = 0.97322f
//        )
//
//        val c2 = Fractal(
//                shape = Shape.clover,
//                position = Position(
//                        x = -0.0000159590504726,
//                        y = -0.00411803970982979,
//                        zoom = 2.9053e-6,
//                        rotation = (-119.6).inRadians()
//                ),
//                texture = Texture.escape
//        )

//        val mandelbox = Fractal(shape = Shape.mandelbox)
//        val mx1 = Fractal(
//                shape = Shape.mandelbox,
//                juliaMode = true,
//                shapeParams = Shape.ParamList(
//                        listOf(Shape.Param(-0.95421800, -0.76819397)),
//                        Shape.Param(-0.99635701, 2.48995369)
//                )
//        )
//        val mx2 = Fractal(
//                shape = Shape.mandelbox,
//                juliaMode = true,
//                shapeParams = Shape.ParamList(
//                        listOf(Shape.Param(-2.66421354, 0.0)),
//                        Shape.Param(3.26525985, 3.28239093)
//                ),
//                position = Position(
//                        x = -1.08072452128498540,
//                        y = -0.91860192005481600,
//                        scale = 2.66877e-2
//                ),
//                texture = Texture.orbitTrap,
//                frequency = 0.53343f,
//                phase = 0.45405f
//        )

//        val kali = Fractal(shape = Shape.kali)

//        val burningShip = Fractal(shape = Shape.burningShip)
//        val bs1 = Fractal(
//                shape = Shape.burningShip,
//                // juliaMode = true,
//                shapeParams = Shape.ParamList(
//                        julia = Shape.Param(0.70271582, -1.04922401)
//                ),
//                maxIter = 8092,
//                position = Position(
//                        x = 0.0,
//                        y = 0.3979338977,
//                        scale = 7.4e-2
//                )
//
//        )
//
//        val bsp1 = Fractal(
//                shape = Shape.burningshipAnyPow,
//                juliaMode = true,
//                shapeParams = Shape.ParamList(
//                        listOf(Shape.Param(0.24805465, -2.54550288)),
//                        Shape.Param(-14.29522458, -15.71238251)
//                ),
//                bailoutRadius = 1e2f,
//                texture = Texture.escape
//        )
//        val bsp2 = Fractal(
//                shape = Shape.burningshipAnyPow,
//                // juliaMode = true,
//                shapeParams = Shape.ParamList(
//                        listOf(Shape.Param(0.62440492, -1.67226752)),
//                        Shape.Param(-24.79982625, -39.39248709)
//                ),
//                position = Position(scale = 6.0),
//                texture = Texture.escape,
//                frequency = 0.29723f,
//                phase = 0.48f
//        )
//        val bsp3 = Fractal(
//                shape = Shape.burningshipAnyPow,
//                // juliaMode = true,
//                shapeParams = Shape.ParamList(
//                        listOf(Shape.Param(1.67084550, 1.05489061)),
//                        Shape.Param(0.22192777, -1.20128181)
//                ),
//                position = Position(scale = 1.2e1),
//                texture = Texture.exponentialSmoothing
//        )
//
//        val bsp4 = Fractal(shape = Shape.burningshipAnyPow,
//                //juliaMode = true,
//                shapeParams = Shape.ParamList(
//                        listOf(Shape.ComplexParam(2.0, 0.92766923)),
//                        Shape.ComplexParam(0.88195649, 0.11055675)
//                ),
//                position = Position(zoom = 8.0),
//                palette = ColorPalette.chroma,
//                frequency = 0.8112f,
//                phase = 0.07f,
//        )
//
//        val bsp5 = Fractal(
//                shape = Shape.burningshipAnyPow,
//                //juliaMode = true,
//                shapeParams = Shape.ParamList(
//                        listOf(Shape.ComplexParam(0.13137502, -2.96284583)),
//                        Shape.ComplexParam(5.22891719, -0.56637719)
//                ),
//                palette = ColorPalette.peacock,
//                frequency = 0.6346f,
//                phase = 0.984f
//        )

//        val sine = Fractal(shape = Shape.sine)
//        val sine1NativeTest = Fractal(
//                shape = Shape.sine,
//                position = Position(
//                        x = -1.4587963652223930,
//                        y = -1.3147301154163030,
//                        zoom = 2.5e-6
//                )
//        )
//        val sine1 = Fractal(
//                shape = Shape.sine1,
//                // juliaMode = true,
//                shapeParams = Shape.ParamList(julia = Shape.ComplexParam(-0.7460453, 0.49768078)),
//                position = Position(
//                        x = 1.413606639479727,
//                        y = 0.9228686743093834,
//                        zoom = 1.0885,
//                        rotation = 159.0.inRadians()
//                ),
//                palette = ColorPalette.chroma,
//                frequency = 0.4563f,
//                phase = 0.762f
//        )
//
//        val s1_2 = Fractal(
//                shape = Shape.sine,
//                // juliaMode = true,
//                // juliaParam = Shape.ComplexParam(-0.15792634, 0.12450936)
//                position = Position(1.132273833247264, 0.37712094215622094, 8.61147e-2, 113.7.inRadians()),
//                texture = Texture.stripeAvg,
//                palette = ColorPalette.atlas
//        )
//
//        val s1_3 = Fractal(
//                shape = Shape.sine.also { it.maxIter = 2150 },
//                position = Position(
//                        x = -2.6298003508693286,
//                        y = -0.52708000273624520,
//                        zoom = 8.9284e-11,
//                        rotation = 24.6.inRadians()
//                ),
//                texture = Texture.escapeSmooth,
//                palette = ColorPalette.starling
//        )
//
//        val sine2 = Fractal(shape = Shape.sine2)
//        val s2_1 = Fractal(
//                shape = Shape.sine2,
//                position = Position(x = -0.26282883851642613, y = 2.042520182493586E-6)
//        )
//
//        val horseshoeCrab = Fractal(shape = Shape.horseshoeCrab)
//        val hcNativeTest = Fractal(
//                shape = Shape.horseshoeCrab,
//                position = Position(
//                        x = 0.9001258454914515,
//                        y = 0.00000064000947079,
//                        zoom = 1e-5,
//                        rotation = (-96.1).inRadians()
//                )
//        )
//        val hc1 = Fractal(
//                shape = Shape.horseshoeCrab,
//                juliaMode = true,
//                shapeParams = Shape.ParamList(
//                        listOf(Shape.Param(-0.02955925, 0.49291033)),
//                        Shape.Param(-0.75224080, -0.93949126)
//                ),
//                textureMode = TextureMode.BOTH,
//                bailoutRadius = 1e5f,
//                frequency = 0.47227f,
//                phase = 0.19166f
//        )
//        val hc2 = Fractal(
//                shape = Shape.horseshoeCrab,
//                juliaMode = true,
//                shapeParams = Shape.ParamList(
//                        listOf(Shape.Param(-0.02926709, 0.48591950)),
//                        Shape.Param(-0.74308518, -0.94826022)
//                ),
//                textureMode = TextureMode.BOTH,
//                bailoutRadius = 1e5f,
//                frequency = 0.47227f,
//                phase = 0.19166f
//        )

//        val kleinian = Fractal(shape = Shape.kleinian)
//        val nova1 = Fractal(shape = Shape.nova1)
//        val nova2 = Fractal(shape = Shape.nova2)

//        val mandelex = Fractal(shape = Shape.mandelex)
//        val mandelex1 = Fractal(
//                shape = Shape.mandelex,
//                shapeParams = Shape.ParamList(listOf(
//                        Shape.mandelex.params.list[0],
//                        Shape.mandelex.params.list[1],
//                        Shape.Param(-2.22),
//                        Shape.ComplexParam(0.0, 0.26496899)
//                ))
//        )
//
//        val mandelex2 = Fractal(
//                shape = Shape.mandelex,
//                position = Position(
//                        x = -2.0048993919584417,
//                        y = -2.004682472266098,
//                        zoom = 1.09681e-1,
//                        rotation = 135.0.inRadians()
//                ),
//                texture = Texture.test  // with q1 = -14
//        )

//        val novaSpiral = Fractal(
//                shape = Shape.nova1,
//                shapeParams = Shape.ParamList(julia = Shape.ComplexParam(0.70277778, -1.12868152)),
//                position = Position(
//                        x = -0.54335401430352640,
//                        y = -0.23102467195610776,
//                        zoom = 4.30288e-3,
//                        rotation = 75.2.inRadians()
//                ),
//                frequency = 0.48f,
//                phase = 0.192f
//        )

        val mandelex = Fractal(shape = Shape.mandelex)

//        val magnet1 = Fractal(
//                shape = Shape.magnet,
//                // julia = 1.20209909 + 0.00114356i
//                position = Position(
//                        x = -0.18995022496666214,
//                        y = -1.9275025513422637,
//                        zoom = 9.1036e-1,
//                        rotation = (-74.5).inRadians()
//                ),
//                texture = Texture.escapeSmooth,
//                solidFillColor = Color.WHITE,
//                palette = ColorPalette.polyphonic,
//                frequency = 0.2809f,
//                phase = 0.818f
//        )

    }

    
    var shape = shape
        set (value) {
            if (field != value) {

                // Log.d("FRACTAL", "setting shape from $field to $value")
                field = value

                position = if (shape.juliaMode) shape.positions.julia else shape.positions.default
                bailoutRadius = min(shape.radius, texture.radius)

            }
        }

    var texture = texture
        set (value) {
            if (field != value) {

                // Log.d("FRACTAL", "setting texture from $field to $value")
                field = value

                bailoutRadius = min(shape.radius, texture.radius)

            }
        }

    var phase = phase
        set (value) {
            val mod = value % 1f
            field = if (value < 0f) 1f - mod else mod
        }



    init {

        // set shape params here
        if (shapeParams != null) { shape.params.setFrom(shapeParams) }

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

        // MAGNET
        //
        //      julia : -2.38768484 - 1.32304182i
        //      param 1 : 1.10988652 + 1.27607142i
        //      position
        //          x : -1.9146739905393257
        //          y : -0.3133017742571375
        //          zoom : 1.52665
        //          rotation : 180
        //      texture : Stripe Average
        //          frequency : 5
        //          phase : 216
        //          width : 30


    }

    fun clone() : Fractal {
        return Fractal(
                shape,
                shape.params, // clone?
                position.clone(),
                texture,
                textureMode,
                palette,
                frequency,
                phase,
                density,
                solidFillColor,
                //maxIter,
                sensitivity,
                bailoutRadius
        )
    }


}