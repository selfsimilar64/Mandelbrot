package com.selfsimilartech.fractaleye

import android.content.res.Resources
import android.graphics.Bitmap
import android.util.Range
import kotlin.math.sqrt


class Shape(

    var id: Int = -1, // DO NOT CHANGE THESE!!!
    var hasCustomId: Boolean = false,
    val nameId: Int = -1,
    override var name: String = "",
    val latexId: Int = -1,
    var latex: String = "",
    val thumbnailId: Int = R.drawable.mandelbrot_icon,
    override var thumbnail: Bitmap? = null,
    conditional: String = ESCAPE,
    val init: String = "",
    val loop: String = "",
    val final: Int = R.string.empty,
    var delta1: String = "",
    var deltaJulia1: String = "",
    var alphaSeed: Complex = Complex.ZERO,
    val hasAnalyticDelta: Boolean = false,
    compatTextures: List<Texture> = Texture.divergent,
    val positions: PositionList = PositionList(),
    val params: ParamList = ParamList(),
    juliaMode: Boolean = false,
    val juliaSeed: Boolean = false,
    // val seed             : Complex               = Complex.ZERO,
    var maxIter: Int = 256,
    radius: Float = 1e6f,
    val power: Float = 2f,
    val hasDynamicPower: Boolean = false,
    val isConvergent: Boolean = false,
    override val goldFeature: Boolean = false,
    val devFeature: Boolean = false,
    val isTranscendental: Boolean = false,
    val hasDualFloat: Boolean = true,
    val slowDualFloat: Boolean = false,
    val hasPerturbation: Boolean = false,
    var customLoopSingle: String = "",
    var customLoopDual: String = "",
    override var isFavorite: Boolean = false,
    var iterateNative: (d: ScriptField_IterateData) -> FloatArray = { _ -> floatArrayOf() }

) : Customizable {


    class ParamListPreset(
        val list: List<RealParam> = listOf(),
        val julia: ComplexParam? = null,
        val seed: ComplexParam? = null
    )

    class ParamList(
        val list: List<RealParam> = listOf(),
        val julia: ComplexParam = ComplexParam(),
        val seed: ComplexParam = ComplexParam()
    ) {

        val size = list.size
        var active: RealParam = julia

        fun at(index: Int) = list[index]
        fun setFrom(newList: ParamList) {

            julia.setFrom(newList.julia)
            seed.setFrom(newList.seed)
            list.forEachIndexed { i, p ->
                newList.list.find { it.nameId == p.nameId }?.let { p.setFrom(it) }
            }

        }

        fun setFrom(newParams: ParamListPreset) {

            newParams.julia?.let { julia.setFrom(it) }
            newParams.seed?.let { seed.setFrom(it) }
            list.forEachIndexed { i, p ->
                val newParam = newParams.list.getOrNull(i)
                if (newParam != null) p.setFrom(newParam)
            }

        }

        fun reset() {

            list.forEach { it.reset() }
            julia.reset()
            seed.reset()

        }

        fun clone(): ParamListPreset {
            return ParamListPreset(
                List(list.size) { i -> list[i].clone() },
                julia = julia.clone(),
                seed = seed.clone()
            )
        }

    }


    companion object {

        private var nextId = 0
            get() = field++

        private const val ESCAPE = "escape(modsqrz, z)"
        private const val CONVERGE = "converge(eps, z, z1)"

        const val CUSTOM_LOOP = "customshape_loop(z1, c)"


        val emptyFavorite = Shape(name = "Empty Favorite")
        val emptyCustom = Shape(name = "Empty Custom")
        val testshape = Shape(
            nameId = R.string.shape,
            loop = "testshape(z1, c)",
            conditional = CONVERGE,
            isConvergent = true,
            goldFeature = true
        )

        fun createNewCustom(res: Resources): Shape {
            return Shape(
                name = "%s %s %d".format(
                    res.getString(R.string.header_custom),
                    res.getString(R.string.shape),
                    nextCustomShapeNum
                ),
                latex = "z^2 + c",
                loop = CUSTOM_LOOP,
                customLoopSingle = "csqr(z) + c",
                customLoopDual = "cadd(csqr(z), c)",
                positions = PositionList(Position(zoom = 5e0)),
                hasDualFloat = true
            )
        }

        val mandelbrot = Shape(
            id = nextId,
            nameId = R.string.mandelbrot,
            latexId = R.string.mandelbrot_latex,
            thumbnailId = R.drawable.mandelbrot_icon,
            loop = "mandelbrot(z1, c)",
            delta1 = "mandelbrot_delta1(alpha, z1)",
            deltaJulia1 = "mandelbrot_julia_delta1(alpha, z1)",
            hasAnalyticDelta = true,
            compatTextures = Texture.mandelbrot.minus(Texture.triangleIneqAvgFloat),
            positions = PositionList(

                default = Position(x = -0.75, zoom = 3.5)

            ),
            maxIter = 512,
            hasPerturbation = true
        )
        val mandelbrotCubic = Shape(
            id = nextId,
            nameId = R.string.mandelbrot_cubic,
            latexId = R.string.mandelbrot_cubic_latex,
            thumbnailId = R.drawable.mandelbrotcubic_icon,
            loop = "mandelbrot_cubic(z1, c)",
            delta1 = mandelbrot.delta1,
            deltaJulia1 = mandelbrot.deltaJulia1,
            hasAnalyticDelta = true,
            compatTextures = Texture.mandelbrot.minus(Texture.triangleIneqAvgFloat),
            positions = PositionList(Position(zoom = 3.5)),
            power = 3f,
            maxIter = 512,
            goldFeature = true
        )
        val mandelbrotQuartic = Shape(
            id = nextId,
            nameId = R.string.mandelbrot_quartic,
            latexId = R.string.mandelbrot_quartic_latex,
            thumbnailId = R.drawable.mandelbrotquartic_icon,
            loop = "mandelbrot_quartic(z1, c)",
            delta1 = mandelbrot.delta1,
            deltaJulia1 = mandelbrot.deltaJulia1,
            hasAnalyticDelta = true,
            compatTextures = Texture.mandelbrot.minus(Texture.triangleIneqAvgFloat),
            positions = PositionList(Position(x = -0.175, zoom = 3.5)),
            power = 4f,
            maxIter = 512,
            goldFeature = true
        )
        val mandelbrotQuintic = Shape(
            id = nextId,
            nameId = R.string.mandelbrot_quintic,
            latexId = R.string.mandelbrot_quintic_latex,
            thumbnailId = R.drawable.mandelbrotquintic_icon,
            loop = "mandelbrot_quintic(z1, c)",
            delta1 = mandelbrot.delta1,
            deltaJulia1 = mandelbrot.deltaJulia1,
            hasAnalyticDelta = true,
            compatTextures = Texture.mandelbrot.minus(Texture.triangleIneqAvgFloat),
            positions = PositionList(Position(zoom = 3.5)),
            power = 5f,
            maxIter = 512,
            goldFeature = true
        )
        val mandelbrotSextic = Shape(
            id = nextId,
            nameId = R.string.mandelbrot_sextic,
            latexId = R.string.mandelbrot_sextic_latex,
            thumbnailId = R.drawable.mandelbrotsextic_icon,
            loop = "mandelbrot_sextic(z1, c)",
            delta1 = mandelbrot.delta1,
            deltaJulia1 = mandelbrot.deltaJulia1,
            hasAnalyticDelta = true,
            compatTextures = Texture.mandelbrot.minus(Texture.triangleIneqAvgFloat),
            positions = PositionList(Position(zoom = 3.5)),
            power = 6f,
            maxIter = 512,
            goldFeature = true
        )
        val mandelbrotPow = Shape(
            id = nextId,
            nameId = R.string.mandelbrot_anypow,
            thumbnailId = R.drawable.mandelbrotanypow_icon,
            loop = "mandelbrot_power(z1, c)",
            compatTextures = Texture.mandelbrot.minus(Texture.triangleIneqAvgFloat),
            positions = PositionList(Position(x = -0.55, y = 0.5, zoom = 5.0)),
            params = ParamList(listOf(ComplexParam(R.string.power, u = 16.0, v = 4.0))),
            hasDynamicPower = true,
            slowDualFloat = true,
            goldFeature = true
        )
        val clover = Shape(
            id = nextId,
            nameId = R.string.clover,
            latexId = R.string.clover_latex,
            thumbnailId = R.drawable.clover_icon,
            loop = "clover(z1, c)",
            positions = PositionList(Position(zoom = 2.0, rotation = 45.0.inRadians())),
            params = ParamList(

                listOf(RealParam(R.string.power, 2.0, Range(2.0, 6.0), discrete = true)),
                seed = ComplexParam(u = 1.0)

            )
        )
        val mandelbox = Shape(
            id = nextId,
            nameId = R.string.mandelbox,
            thumbnailId = R.drawable.mandelbox_icon,
            loop = "mandelbox(z1, c)",
            positions = PositionList(Position(zoom = 6.5)),
            params = ParamList(
                listOf(

                    ComplexParam(u = -2.66421354),
                    RealParam(R.string.scale, u = 1.0, uRange = Range(0.0, 5.0)),
                    RealParam(R.string.power, u = 1.0, uRange = Range(1.0, 6.0), discrete = true)

                )
            ),
            radius = 5f
        )
        val kali = Shape(
            id = nextId,
            nameId = R.string.kali,
            thumbnailId = R.drawable.kali_icon,
            loop = "kali(z1, c)",
            juliaMode = true,
            positions = PositionList(julia = Position(zoom = 3.0)),
            params = ParamList(julia = ComplexParam(u = -0.33170626, v = -0.18423799)),
            radius = 4e0f
        )
        val burningShip = Shape(
            id = nextId,
            nameId = R.string.burning_ship,
            latexId = R.string.burningship_latex,
            thumbnailId = R.drawable.burningship_icon,
            loop = "burning_ship(z1, c)",
            positions = PositionList(Position(-0.4, -0.6, 4.0, Math.PI))
        )
        val burningShipPow = Shape(
            id = nextId,
            nameId = R.string.burning_ship_anypow,
            thumbnailId = R.drawable.burningshipanypow_icon,
            loop = "burning_ship_power(z1, c)",
            params = ParamList(listOf(ComplexParam(R.string.power, 6.0, 1.0))),
            positions = PositionList(
                Position(
                    x = 0.15,
                    y = -0.15,
                    zoom = 4.0,
                    rotation = 0.5 * Math.PI
                )
            ),
            devFeature = true
        )
        val magnet1 = Shape(
            id = nextId,
            nameId = R.string.magnet1,
            // latexId = R.string.magnet1_latex,
            thumbnailId = R.drawable.magnet_icon,
            loop = "magnet1(z1, c)",
            positions = PositionList(
                Position(
                    x = 1.25,
                    zoom = 6.0,
                    rotation = (-90.0).inRadians()
                )
            ),
            params = ParamList(listOf(ComplexParam(u = 1.0, v = 2.0))),
            goldFeature = true
        )
        val magnet2 = Shape(
            id = nextId,
            nameId = R.string.magnet2,
            thumbnailId = R.drawable.magnet2_icon,
            loop = "magnet2(z1, c)",
            positions = PositionList(
                Position(
                    x = 1.25,
                    zoom = 4.0,
                    rotation = (-90.0).inRadians()
                )
            ),
            params = ParamList(listOf(ComplexParam(u = 1.0, v = 2.0))),
            goldFeature = true
        )
        val sine = Shape(
            id = nextId,
            nameId = R.string.sine,
            latexId = R.string.sine1_latex,
            thumbnailId = R.drawable.sine1_icon,
            loop = "sine(z1, c)",
            positions = PositionList(Position(zoom = 6.0)),
            radius = 3e2f,
            slowDualFloat = true
        )
        val cosine = Shape(
            id = nextId,
            nameId = R.string.cosine,
            latexId = R.string.cosine_latex,
            thumbnailId = R.drawable.cosine_icon,
            loop = "cosine(z1, c)",
            positions = PositionList(Position(zoom = 6.0)),
            radius = 3e2f,
            goldFeature = true,
            slowDualFloat = true
        )
        val hyperbolicSine = Shape(
            id = nextId,
            nameId = R.string.hyperbolic_sine,
            latexId = R.string.hyperbolic_sine_latex,
            thumbnailId = R.drawable.hyperbolicsine_icon,
            loop = "hyperbolic_sine(z1, c)",
            positions = PositionList(Position(zoom = 8.0)),
            radius = 3e2f,
            goldFeature = true,
            slowDualFloat = true
        )
        val hyperbolicCosine = Shape(
            id = nextId,
            nameId = R.string.hyperbolic_cosine,
            latexId = R.string.hyperbolic_cosine_latex,
            thumbnailId = R.drawable.hyperboliccosine_icon,
            loop = "hyperbolic_cosine(z1, c)",
            positions = PositionList(Position(x = -0.075, zoom = 8.0)),
            radius = 3e2f,
            goldFeature = true,
            slowDualFloat = true
        )
        val sine2 = Shape(
            id = nextId,
            nameId = R.string.sine2,
            thumbnailId = R.drawable.sine2_icon,
            loop = "sine2(z1, c)",
            positions = PositionList(Position(zoom = 3.5)),
            radius = 1e1f,
            params = ParamList(

                listOf(ComplexParam(u = -0.26282884)),
                seed = ComplexParam(u = 1.0)

            ),
            slowDualFloat = true
        )
        val horseshoeCrab = Shape(
            id = nextId,
            nameId = R.string.horseshoe_crab,
            thumbnailId = R.drawable.horseshoecrab_icon,
            loop = "horseshoe_crab(z1, c)",
            positions = PositionList(Position(x = -0.25, zoom = 6.0, rotation = 90.0.inRadians())),
            radius = 3e2f,
            params = ParamList(

                listOf(ComplexParam(u = sqrt(2.0))),
                seed = ComplexParam(u = 1.0)

            ),
            slowDualFloat = true
        )
        val necklace = Shape(
            id = nextId,
            nameId = R.string.necklace,
            thumbnailId = R.drawable.newton1_icon,
            loop = "necklace(z1, c)",
            params = ParamList(

                listOf(RealParam(R.string.power, 4.0, Range(3.0, 6.0), discrete = true)),
                julia = ComplexParam(u = 1.0)

            ),
            juliaMode = true,
            isConvergent = true,
            goldFeature = true
        )
        val kleinian = Shape(
            id = nextId,
            nameId = R.string.kleinian,
            thumbnailId = R.drawable.kleinian_icon,
            init = "float K = 0.0; float M = 0.0; vec2 t = vec2(0.0); float k = 0.0; kleinian_init(z, c, alpha, t, K, M, k);",
            loop = "kleinian(z1, c, t, K, M, k)",
            conditional = "kleinian_exit(z, t)",
            delta1 = "kleinian_delta(alpha, z1, t, K, M, k)",
            deltaJulia1 = "kleinian_delta(alpha, z1, t, K, M, k)",
            alphaSeed = Complex.ONE,
            hasAnalyticDelta = true,
            positions = PositionList(julia = Position(y = -0.5, zoom = 1.5)),
            juliaMode = true,
            params = ParamList(

                listOf(
                    ComplexParam(u = 2.0, goldFeature = true),
                    RealParam(
                        u = 0.0,
                        uRange = Range(0.0, 8.0),
                        discrete = true,
                        devFeature = true
                    ),
                    RealParam(u = 0.0, uRange = Range(0.0, 10.0), devFeature = true),
                    RealParam(u = 0.0, uRange = Range(0.0, 10.0), devFeature = true)
                ),
                julia = ComplexParam(u = 0.0, v = -1.0)

            ),
            maxIter = 128,
            radius = 1e5f,
            compatTextures = Texture.kleinianCompat
        )
        val nova1 = Shape(
            id = nextId,
            nameId = R.string.nova1,
            thumbnailId = R.drawable.nova1_icon,
            loop = "nova1(z1, c)",
            positions = PositionList(Position(x = -0.3, zoom = 1.75, rotation = 90.0.inRadians())),
            params = ParamList(

                listOf(ComplexParam(u = 1.0, v = 0.0)),
                seed = ComplexParam(u = 1.0)

            ),
            isConvergent = true
        )
        val nova2 = Shape(
            id = nextId,
            nameId = R.string.nova2,
            thumbnailId = R.drawable.nova2_icon,
            loop = "nova2(z1, c)",
            juliaMode = true,
            positions = PositionList(julia = Position(x = -0.3, zoom = 5.0)),
            isConvergent = true,
            slowDualFloat = true
        )
        val collatz = Shape(
            id = nextId,
            nameId = R.string.collatz,
            latexId = R.string.collatz_latex,
            thumbnailId = R.drawable.collatz_icon,
            loop = "collatz(z1, c)",
            positions = PositionList(

                Position(zoom = 1.5, rotation = (-90.0).inRadians()),
                Position(rotation = (-90.0).inRadians())

            ),
            radius = 5e1f,
            goldFeature = true,
            slowDualFloat = true
        )
        val mandelex = Shape(
            id = nextId,
            nameId = R.string.mandelex,
            thumbnailId = R.drawable.mandelex_icon,
            loop = "mandelex(z1, c)",
            juliaSeed = true,
            positions = PositionList(Position(zoom = 2e1)),
            params = ParamList(
                listOf(

                    RealParam(R.string.angle, 180.0, Range(0.0, 360.0), toRadians = true),
                    RealParam(R.string.radius, 0.5, Range(0.0, 1.0)),
                    RealParam(R.string.scale, 2.0, Range(0.0, 5.0)),
                    RealParam(R.string.linear, 2.0, Range(0.0, 5.0))

                )
            ),
            goldFeature = true
        )
        val cactus = Shape(
            id = nextId,
            nameId = R.string.cactus,
            thumbnailId = R.drawable.cactus_icon,
            loop = "cactus(z1, c)",
            positions = PositionList(Position(rotation = 90.0.inRadians(), zoom = 1.5)),
            juliaSeed = true,
            goldFeature = true
        )
        val sierpinksiTriangle = Shape(
            id = nextId,
            nameId = R.string.sierpinski_triangle,
            thumbnailId = R.drawable.sierpinskitri_icon,
            loop = "sierpinski_tri(z1, c)",
            params = ParamList(
                listOf(

                    RealParam(R.string.scale, 1.0, Range(2.0 / 3.0, 2.0)),
                    RealParam(R.string.rotate, 0.0, Range(0.0, 360.0), toRadians = true)

                )
            ),
            conditional = "escape_tri(z)",
            juliaMode = true,
            compatTextures = Texture.divergent.minus(Texture.escapeWithDistance),
            goldFeature = true
        )
        val sierpinksiSquare = Shape(
            id = nextId,
            nameId = R.string.sierpinski_square,
            thumbnailId = R.drawable.sierpinskisqr_icon,
            loop = "sierpinski_sqr(z1, c)",
            juliaMode = true,
            positions = PositionList(julia = Position(zoom = 1.75)),
            conditional = "escape_sqr(z)",
            compatTextures = Texture.divergent.minus(Texture.escapeWithDistance),
            goldFeature = true
        )
        val sierpinskiPentagon = Shape(
            id = nextId,
            nameId = R.string.sierpinski_pentagon,
            thumbnailId = R.drawable.sierpinskipent_icon,
            loop = "sierpinski_pent(z1, c)",
            juliaMode = true,
            positions = PositionList(julia = Position(rotation = Math.PI, zoom = 7.0)),
            params = ParamList(
                listOf(

                    RealParam(R.string.center, 0.0, Range(0.0, 1.0)),
                    RealParam(R.string.scale, 2.618033988, Range(0.0, 10.0)),
                    RealParam(R.string.rotate, 0.0, Range(0.0, 72.0), toRadians = true)

                )
            ),
            conditional = "escape_pent(z)",
            compatTextures = Texture.divergent.minus(Texture.escapeWithDistance),
            goldFeature = true
        )
        val tsquare = Shape(
            id = nextId,
            nameId = R.string.tsquare,
            thumbnailId = R.drawable.tsquare_icon,
            loop = "tsquare(z1, c)",
            juliaMode = true,
            params = ParamList(
                listOf(

                    RealParam(R.string.shift, 0.5, Range(0.0, 5.0)),
                    RealParam(R.string.scale, 2.0, Range(0.0, 5.0)),
                    RealParam(R.string.rotate, 0.0, Range(0.0, 90.0), toRadians = true)

                )
            ),
            conditional = "escape_sqr(z)",
            compatTextures = Texture.divergent.minus(Texture.escapeWithDistance),
            goldFeature = true
        )
        val lambertNewton = Shape(
            id = nextId,
            nameId = R.string.lambert_newton,
            thumbnailId = R.drawable.lambertnewton_icon,
            positions = PositionList(Position(zoom = 3.5, rotation = 90.0.inRadians())),
            loop = "lambert_newton(z1, c)",
            juliaSeed = true,
            isConvergent = true,
            goldFeature = true,
            slowDualFloat = true
        )
        val taurus = Shape(
            id = nextId,
            nameId = R.string.taurus,
            thumbnailId = R.drawable.taurus_icon,
            loop = "taurus(z1, c)",
            positions = PositionList(Position(x = 1.0, zoom = 7.5, rotation = (90.0).inRadians())),
            isConvergent = true,
            goldFeature = true
        )
        val ammonite = Shape(
            id = nextId,
            nameId = R.string.ammonite,
            thumbnailId = R.drawable.ammonite_icon,
            loop = "ammonite(z1, c)",
            positions = PositionList(
                julia = Position(
                    x = 0.56767345,
                    zoom = 6.0,
                    rotation = 90.0.inRadians()
                )
            ),
            params = ParamList(julia = ComplexParam(u = -0.56767345)),
            juliaMode = true,
            goldFeature = true
        )
        val angelbrot = Shape(
            id = nextId,
            nameId = R.string.angelbrot,
            thumbnailId = R.drawable.angelbrot_icon,
            loop = "ballfold1(z1, c)",
            positions = PositionList(Position(x = 1.0, zoom = 3.5, rotation = 0.5 * Math.PI)),
            params = ParamList(listOf(ComplexParam(u = -0.5 * Math.PI)))
        )
        val harriss = Shape(
            id = nextId,
            nameId = R.string.hardware,
            juliaMode = true,
            loop = "harriss(z1, c)",
            params = ParamList(
                listOf(
                    RealParam(u = 1.0, uRange = Range(0.0, 5.0)),
                    RealParam(u = 1.0, uRange = Range(0.0, 5.0))
                )
            ),
            conditional = "converge_sqr(z)",
            devFeature = true
        )
        val tree = Shape(
            id = nextId,
            nameId = R.string.about,
            juliaMode = true,
            loop = "tree(z1, c)",
            params = ParamList(
                listOf(
                    ComplexParam(v = 1.0),
                    ComplexParam(v = 0.75),
                    ComplexParam(1.0, 1.0),
                    ComplexParam(v = 0.5)
                )
            ),
            conditional = "converge_sqr(z)",
            devFeature = true
        )
        val tree2 = Shape(
            id = nextId,
            nameId = R.string.about,
            juliaMode = true,
            loop = "tree(z1, c)",
            params = ParamList(
                listOf(
                    ComplexParam(v = 1.0),
                    ComplexParam(v = 0.75),
                    ComplexParam(1.0, 1.0),
                    ComplexParam(v = 0.5)
                )
            ),
            conditional = "converge_sqr2(z)",
            devFeature = true
        )


        val kaliSquare = Shape(
            R.string.kali_square,
            loop = "kali_square(z1, c)",
            juliaMode = true,
            positions = PositionList(julia = Position(zoom = 4.0)),
            radius = 4e0f
        )
        val mandelbar = Shape(
            R.string.mandelbar,
            loop = "mandelbar(z1, c)",
            positions = PositionList(Position(zoom = 3.0))
        )
        val logistic = Shape(
            R.string.empty,
            latexId = R.string.logistic_katex,
            loop = "",
            positions = PositionList(Position(zoom = 3.5))
            // seed = Complex(0.5, 0.0)
        )
        val bessel = Shape(
            R.string.bessel,
            thumbnailId = R.drawable.bessel_icon,
            loop = "bessel(z1, c)",
            positions = PositionList(Position(zoom = 15.0)),
            radius = 3e2f,
            goldFeature = true,
            slowDualFloat = true
        )
        val newton2 = Shape(
            R.string.empty,
            loop = "",
            positions = PositionList(julia = Position(zoom = 3.5)),
            juliaMode = true,
            params = ParamList(
                listOf(
                    ComplexParam(u = 1.0, v = 1.0),
                    ComplexParam(u = -1.0, v = -1.0),
                    ComplexParam(u = 2.0, v = -0.5)
                )
            ),
            isConvergent = true
        )
        val newton3 = Shape(
            R.string.empty,
            latexId = R.string.newton3_katex,
            loop = "",
            positions = PositionList(julia = Position(zoom = 5.0)),
            juliaMode = true,
            isConvergent = true
        )
        val persianRug = Shape(
            R.string.empty,
            latexId = R.string.persianrug_katex,
            loop = "",
            juliaSeed = true,
            positions = PositionList(Position(zoom = 1.5)),
            params = ParamList(listOf(ComplexParam(u = 0.642, v = 0.0))),
            radius = 1e1f,
            isTranscendental = true
        )
        val sine3 = Shape(
            R.string.empty,
            latexId = R.string.sine3_katex,
            loop = "",
            positions = PositionList(Position(zoom = 3.5)),
            params = ParamList(
                listOf(ComplexParam(u = 0.31960705187983646, vLocked = true)),
                seed = ComplexParam(u = 1.0)
            ),
            radius = 1e1f,
            isTranscendental = true
        )
        val binet = Shape(
            nameId = R.string.binet,
            loop = "binet(z1, c)",
            positions = PositionList(Position(zoom = 5.0)),
            goldFeature = true,
            slowDualFloat = true
        )
        val dragon = Shape(
            nameId = R.string.heighway_dragon,
            loop = "dragon(z1, c)",
            params = ParamList(
                listOf(

                    ComplexParam(u = 0.5, v = sqrt(2.0))

                )
            ),
            conditional = "n == maxIter - 1u && z.y < z.x && z.y < 1.0 - z.x && z.y > 0.0",
            goldFeature = true
        )
        val cephalopod = Shape(
            nameId = R.string.cephalopod,
            loop = "cephalopod(z1, c)",
            isConvergent = true,
            goldFeature = true
        )
        val phoenix = Shape(
            nameId = R.string.shape_name,
            loop = "phoenix(z1, z2, c)",
            params = ParamList(
                listOf(
                    RealParam(u = 2.0, uRange = Range(-6.0, 6.0), discrete = true),
                    RealParam(u = 0.0, uRange = Range(-6.0, 6.0), discrete = true),
                    ComplexParam(u = 1.0)
                )
            ),
            goldFeature = true
        )


        var nextCustomShapeNum = 1
        val custom = arrayListOf<Shape>()
        val default = arrayListOf(
            tree,
            tree2,
            harriss,
            mandelbrot,
            mandelbrotCubic,
            mandelbrotQuartic,
            mandelbrotQuintic,
            mandelbrotSextic,
            mandelbrotPow,
            kleinian,
            mandelex,
            collatz,
            angelbrot,
            clover,
            burningShip,
            burningShipPow,
            mandelbox,
            cactus,
            tsquare,
            sierpinksiTriangle,
            sierpinksiSquare,
            sierpinskiPentagon,
            kali,
            sine,
            cosine,
            hyperbolicSine,
            hyperbolicCosine,
            lambertNewton,
            necklace,
            magnet1,
            magnet2,
            taurus,
            ammonite,
            sine2,
            horseshoeCrab,
            nova1,
            nova2
        ).filter { BuildConfig.DEV_VERSION || !it.devFeature }
        val all = ArrayList(default)

    }


    var position = if (juliaMode) positions.julia else positions.default

    val conditional = if (isConvergent) CONVERGE else conditional
    val radius = if (isConvergent) 1e-8f else radius
    val compatTextures = if (isConvergent) Texture.convergent else compatTextures

    val juliaModeInit = juliaMode
    var juliaMode = juliaModeInit
        set(value) {
            field = value
            numParamsInUse = params.size + if (juliaMode) 1 else 0
            if (hasAnalyticDelta) alphaSeed = if (juliaMode) Complex.ONE else Complex.ZERO
            position = if (value) positions.julia else positions.default
        }

    var numParamsInUse = params.size + if (juliaMode) 1 else 0

    init {
        if (!hasAnalyticDelta) {
            delta1 = "delta(_float(z), ${
                loop.replace(
                    "z1, c",
                    "_float(z1) + vec2(0.0001, 0.0), _float(c)"
                )
            }, alpha)"
            alphaSeed = Complex.ONE
        }
        if (!hasCustomId && id != -1) id = Integer.MAX_VALUE - id
    }

    fun initialize(res: Resources) {

        when {
            nameId == -1 && name == "" -> {
                throw Error("neither nameId nor name was passed to the constructor")
            }
            name == "" -> {
                name = res.getString(nameId)
            }
        }

        params.list.forEach { p ->
            if (p.nameId != -1) p.name = res.getString(p.nameId)
        }

        if (hasCustomId) {
            thumbnail = Bitmap.createBitmap(
                Resolution.THUMB.w,
                Resolution.THUMB.w,
                Bitmap.Config.RGB_565
            )
        }

        if (latexId != -1 && latex == "") latex = res.getString(latexId)

    }

    fun release() {
        thumbnail?.recycle()
        thumbnail = null
    }

    fun clone(res: Resources): Shape {
        return Shape(
            name = name + " " + res.getString(R.string.copy),
            latex = latex,
            conditional = conditional,
            init = init,
            loop = CUSTOM_LOOP,
            customLoopSingle = customLoopSingle,
            customLoopDual = customLoopDual,
            final = final,
            delta1 = delta1,
            deltaJulia1 = deltaJulia1,
            alphaSeed = alphaSeed,
            compatTextures = compatTextures.toMutableList(),
            positions = positions.clone(),
            params = params, // clone?
            juliaMode = juliaMode,
            juliaSeed = juliaSeed,
            maxIter = maxIter,
            radius = radius
        )
    }

    fun reset() {

        params.reset()
        positions.reset()
        juliaMode = juliaModeInit

    }

    fun toDatabaseEntity(): ShapeEntity {
        return ShapeEntity(
            if (hasCustomId) id else 0,
            name,
            latex,
            customLoopSingle,
            customLoopDual,
            conditional,
//                positions.default.toData(),
//                positions.julia.toData(),
            positions.default.x,
            positions.default.y,
            positions.default.zoom,
            positions.default.rotation,
            positions.julia.x,
            positions.julia.y,
            positions.julia.zoom,
            positions.julia.rotation,
            juliaMode,
            juliaSeed,
            params.seed.u,
            params.seed.v,
            maxIter,
            radius,
            isConvergent,
            hasDualFloat,
            isFavorite
        )
    }

    override fun equals(other: Any?): Boolean {
        return other is Shape && id == other.id
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun isCustom(): Boolean = hasCustomId

}