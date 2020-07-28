package com.selfsimilartech.fractaleye

import android.content.res.Resources
import android.renderscript.Double2
import android.util.Range
import kotlin.math.exp
import kotlin.math.sqrt



class Shape (

        val nameId           : Int                   = -1,
        var name             : String                = "",
        val latexId          : Int                   = -1,
        var latex            : String                = "",
        val icon             : Int                   = R.drawable.mandelbrot_icon,
        val conditional      : String                = ESCAPE,
        val init             : String                = "",
        val loop             : String                = "",
        val final            : Int                   = R.string.empty,
        compatTextures       : MutableList<Texture>  = Texture.divergent,
        val positions        : PositionList          = PositionList(),
        val params           : ParamList             = ParamList(),
        juliaMode            : Boolean               = false,
        val juliaSeed        : Boolean               = false,
        val seed             : Complex               = Complex.ZERO,
        var maxIter          : Int                   = 256,
        val bailoutRadius    : Float                 = 1e6f,
        val power            : Float                 = 2f,
        val hasDynamicPower  : Boolean               = false,
        val isConvergent     : Boolean               = false,
        val isProFeature     : Boolean               = false,
        val isTranscendental : Boolean               = false,
        val hasDualFloat     : Boolean               = false,
        val hasPerturbation  : Boolean               = false,
        var customId         : Int                   = -1,
        var customLoopSF     : String                = "",
        var customLoopDF     : String                = "",
        var iterateNative    : ( d: ScriptField_IterateData) -> FloatArray = { _ -> floatArrayOf() }

) {


    class ParamList(
            val list: List<Param> = listOf(),
            val julia: ComplexParam = ComplexParam()
    ) {

        init {
            list.forEachIndexed { i, param ->
                if (param.name == "") param.name = "Param ${i + 1}"
            }
        }

        val size = list.size
        var active : Param = julia

        fun at(index: Int) = list[index]
        fun setFrom(newList: ParamList) {

            julia.setFrom(newList.julia)
            list.forEachIndexed { i, p -> p.setFrom(newList.list[i]) }

        }
        fun reset() {

            list.forEach { it.reset() }
            julia.reset()

        }

    }

    open class Param (

            u             : Double          = 0.0,
            var uRange    : Range<Double>   = Range(0.0, 1.0),
            uLocked        : Boolean         = false,
            sensitivity   : Double          = 1.0,
            var name      : String          = ""

    ) {

        val uInit = u
        val uLockedInit = uLocked
        val sensitivityInit = sensitivity

        var uLocked = uLockedInit
        open var u = u
            set (value) { if (!uLocked) field = value }
        var sensitivity = sensitivityInit

        open fun reset() {
            uLocked = false
            u = uInit
            uLocked = uLockedInit
            sensitivity = sensitivityInit
        }
        open fun clone() : Param {

            return Param(
                    uInit,
                    uRange,
                    uLockedInit,
                    sensitivityInit,
                    name
            )

        }
        open fun setFrom(newParam: Param) {

            uLocked = newParam.uLockedInit
            u = newParam.uInit
            sensitivity = newParam.sensitivityInit

        }
        open fun toFloatArray() : FloatArray = floatArrayOf(u.toFloat(), 0f)
        open fun toDouble2() : Double2 = Double2(u, 0.0)

    }

    class ComplexParam (

            u: Double = 0.0,
            v: Double = 0.0,
            uLocked: Boolean = false,
            vLocked: Boolean = false,
            uRange: Range<Double> = Range(0.0, 1.0),
            var vRange: Range<Double> = Range(0.0, 1.0),
            linked: Boolean = false,
            sensitivity: Double = 1.0,
            name: String = ""

    ) : Param(u, uRange, uLocked, sensitivity, name) {

        private val vInit = v
        private val vLockedInit = vLocked
        private val linkedInit = linked

        var vLocked = vLockedInit
        override var u = u
            set (value) {
                if (!uLocked) {
                    field = value
                    if (linked) v = u
                }
            }
        var v = v
            set (value) {
                if (!vLocked) {
                    field = if (linked) u else value
                }
            }
        var linked = linkedInit
            set (value) {
                field = value
                if (value) v = u
            }

        override fun reset() {
            uLocked = false
            vLocked = false
            u = uInit
            v = vInit
            uLocked = uLockedInit
            vLocked = vLockedInit
            linked = linkedInit
            sensitivity = sensitivityInit
        }
        override fun clone() : ComplexParam {

            return ComplexParam(
                    uInit,
                    vInit,
                    uLockedInit,
                    vLockedInit,
                    uRange,
                    vRange,
                    linkedInit,
                    sensitivityInit,
                    name
            )

        }
        override fun setFrom(newParam: Param) {

            with (newParam as ComplexParam) {
                u = newParam.uInit
                v = newParam.vInit
                uLocked = newParam.uLockedInit
                vLocked = newParam.vLockedInit
                uRange = newParam.uRange
                vRange = newParam.vRange
                sensitivity = newParam.sensitivityInit
            }

        }
        override fun toFloatArray() : FloatArray = floatArrayOf(u.toFloat(), v.toFloat())
        override fun toDouble2(): Double2 = Double2(u, v)

    }




    companion object {


        private const val ESCAPE = "escape(modsqrz, z)"
        private const val CONVERGE = "converge(eps, z, z1)"


        val testshape = Shape(
                name = "TEST",
                loop = "testshape(z1, c)",
                positions = PositionList(Position(zoom = 6.0)),
                seed = Complex.ONE,
                params = ParamList(listOf(ComplexParam(2.0, 0.0))),
                bailoutRadius = 3e2f,
                hasDualFloat = true,
                isProFeature = true
        )

        val mandelbrot = Shape(
                R.string.mandelbrot,
                latexId = R.string.mandelbrot_katex,
                icon = R.drawable.mandelbrot_icon,
                loop = "mandelbrot(z1, c)",
                compatTextures = Texture.mandelbrot without Texture.triangleIneqAvgFloat,
                positions = PositionList(
                    default = Position(x = -0.75, zoom = 3.5)
                ),
                maxIter = 512,
                hasDualFloat = true,
                hasPerturbation = true
        )
        val mandelbrotCubic = Shape(
                R.string.mandelbrot_cubic,
                icon = R.drawable.mandelbrotcubic_icon,
                latexId = R.string.mandelbrotcubic_katex,
                loop = "mandelbrot_cubic(z1, c)",
                compatTextures = Texture.mandelbrot without Texture.triangleIneqAvgFloat,
                positions = PositionList(Position(zoom = 3.5)),
                power = 3f,
                maxIter = 512,
                hasDualFloat = true,
                isProFeature = true
        )
        val mandelbrotQuartic = Shape(
                R.string.mandelbrot_quartic,
                icon = R.drawable.mandelbrotquartic_icon,
                latexId = R.string.mandelbrotquartic_katex,
                loop = "mandelbrot_quartic(z1, c)",
                compatTextures = Texture.mandelbrot without Texture.triangleIneqAvgFloat,
                positions = PositionList(Position(x = -0.175, zoom = 3.5)),
                power = 4f,
                maxIter = 512,
                hasDualFloat = true,
                isProFeature = true
        )
        val mandelbrotQuintic = Shape(
                R.string.mandelbrot_quintic,
                icon = R.drawable.mandelbrotquintic_icon,
                latexId = R.string.mandelbrotquintic_katex,
                loop = "mandelbrot_quintic(z1, c)",
                compatTextures = Texture.mandelbrot without Texture.triangleIneqAvgFloat,
                positions = PositionList(Position(zoom = 3.5)),
                power = 5f,
                maxIter = 512,
                hasDualFloat = true,
                isProFeature = true
        )
        val mandelbrotPow = Shape(
                R.string.mandelbrot_anypow,
                latexId = R.string.mandelbrotanypow_katex,
                icon = R.drawable.mandelbrotanypow_icon,
                loop = "mandelbrot_power(z1, c)",
                compatTextures = Texture.mandelbrot without Texture.triangleIneqAvgFloat,
                positions = PositionList(Position(x = -0.55, y = 0.5, zoom = 5.0)),
                params = ParamList(listOf(ComplexParam(16.0, 4.0))),
                hasDynamicPower = true,
                hasDualFloat = true
        )
        val clover = Shape(
                R.string.clover,
                latexId = R.string.dualpow_katex,
                icon = R.drawable.clover_icon,
                loop = "clover(z1, c)",
                positions = PositionList(Position(zoom = 2.0, rotation = 45.0.inRadians())),
                seed = Complex.ONE,
                params = ParamList(listOf(ComplexParam(2.0, vLocked = true))),
                hasDualFloat = true
        )
        val mandelbox = Shape(
                R.string.mandelbox,
                latexId = R.string.mandelbox_katex,
                icon = R.drawable.mandelbox_icon,
                loop = "mandelbox(z1, c)",
                positions = PositionList(Position(zoom = 6.5)),
                params = ParamList(listOf(
                        ComplexParam(-2.66421354, vLocked = true),
                        ComplexParam(1.0, 0.0))),
                bailoutRadius = 5f,
                hasDualFloat = true
        )
        val kali = Shape(
                R.string.kali,
                latexId = R.string.kali_katex,
                icon = R.drawable.kali_icon,
                loop = "kali(z1, c)",
                juliaMode = true,
                positions = PositionList(julia = Position(zoom = 3.0)),
                params = ParamList(julia = ComplexParam(-0.33170626, -0.18423799)),
                bailoutRadius = 4e0f,
                hasDualFloat = true
        )
        val kaliSquare = Shape(
                R.string.empty,
                loop = "kali_square(z1, c)",
                juliaMode = true,
                positions = PositionList(julia = Position(zoom = 4.0)),
                bailoutRadius = 4e0f
        )
        val mandelbar = Shape(
                R.string.empty,
                loop = "",
                positions = PositionList(Position(zoom = 3.0))
        )
        val logistic = Shape(
                R.string.empty,
                latexId = R.string.logistic_katex,
                loop = "",
                positions = PositionList(Position(zoom = 3.5)),
                seed = Complex(0.5, 0.0)
        )
        val burningShip = Shape(
                R.string.burning_ship,
                latexId = R.string.burningship_katex,
                icon = R.drawable.burningship_icon,
                loop = "burning_ship(z1, c)",
                positions = PositionList(Position(-0.4, -0.6, 4.0, Math.PI)),
                hasDualFloat = true
        )
        val magnet = Shape(
                R.string.empty,
                loop = "",
                positions = PositionList(Position(zoom = 3.5)),
                params = ParamList(listOf(
                        ComplexParam(-1.0, vLocked = true),
                        ComplexParam(-2.0, vLocked = true))),
                bailoutRadius = 4e0f
        )
        val sine = Shape(
                R.string.sine1,
                latexId = R.string.sine1_katex,
                icon = R.drawable.sine1_icon,
                loop = "sine1(z1, c)",
                positions = PositionList(Position(zoom = 6.0)),
                bailoutRadius = 3e2f,
                hasDualFloat = true
        )
        val sine2 = Shape(
                R.string.sine2,
                latexId = R.string.sine2_katex,
                icon = R.drawable.sine2_icon,
                loop = "sine2(z1, c)",
                positions = PositionList(Position(zoom = 3.5)),
                bailoutRadius = 1e1f,
                params = ParamList(listOf(ComplexParam(-0.26282884))),
                seed = Complex.ONE,
                hasDualFloat = true
        )
        val sine3 = Shape(
                R.string.empty,
                latexId = R.string.sine3_katex,
                loop = "",
                positions = PositionList(Position(zoom = 3.5)),
                params = ParamList(listOf(ComplexParam(0.31960705187983646, vLocked = true))),
                seed = Complex.ONE,
                bailoutRadius = 1e1f,
                isTranscendental = true
        )
        val horseshoeCrab = Shape(
                R.string.horseshoe_crab,
                latexId = R.string.horseshoecrab_katex,
                icon = R.drawable.horseshoecrab_icon,
                loop = "horseshoe_crab(z1, c)",
                positions = PositionList(Position(x = -0.25, zoom = 6.0, rotation = 90.0.inRadians())),
                bailoutRadius = 3e2f,
                params = ParamList(listOf(ComplexParam(sqrt(2.0)))),
                seed = Complex.ONE,
                hasDualFloat = true
        )
        val newton2 = Shape(
                R.string.empty,
                conditional = CONVERGE,
                loop = "",
                positions = PositionList(julia = Position(zoom = 3.5)),
                juliaMode = true,
                params = ParamList(listOf(
                        ComplexParam(1.0, 1.0),
                        ComplexParam(-1.0, -1.0),
                        ComplexParam(2.0, -0.5))
                )
        )
        val newton3 = Shape(
                R.string.empty,
                latexId = R.string.newton3_katex,
                conditional = CONVERGE,
                loop = "",
                positions = PositionList(julia = Position(zoom = 5.0)),
                juliaMode = true
        )
        val persianRug = Shape(
                R.string.empty,
                latexId = R.string.persianrug_katex,
                loop = "",
                juliaSeed = true,
                positions = PositionList(Position(zoom = 1.5)),
                params = ParamList(listOf(ComplexParam(0.642, 0.0))),
                bailoutRadius = 1e1f,
                isTranscendental = true
        )
        val kleinian = Shape(
                R.string.kleinian,
                icon = R.drawable.kleinian_icon,
                init = "kleinian_init(z, c);",
                loop = "kleinian(z1, c)",
                conditional = "kleinian_exit(modsqrz, z)",
                positions = PositionList(julia = Position(y = -0.5, zoom = 1.5)),
                juliaMode = true,
                params = ParamList(
                        listOf(ComplexParam(1.41421538, vLocked = true)),
                        ComplexParam(0.0, -1.0)
                ),
                maxIter = 32,
                bailoutRadius = 1e5f,
                isTranscendental = true
        )
        val nova1 = Shape(
                R.string.nova1,
                latexId = R.string.nova1_katex,
                icon = R.drawable.nova1_icon,
                conditional = CONVERGE,
                loop = "nova1(z1, c)",
                positions = PositionList(Position(x = -0.3, zoom = 1.75, rotation = 90.0.inRadians())),
                seed = Complex.ONE,
                params = ParamList(listOf(ComplexParam(1.0, 0.0))),
                isConvergent = true,
                hasDualFloat = true,
                compatTextures = Texture.convergent
        )
        val nova2 = Shape(
                R.string.nova2,
                latexId = R.string.nova2_katex,
                icon = R.drawable.nova2_icon,
                conditional = CONVERGE,
                loop = "nova2(z1, c)",
                juliaMode = true,
                positions = PositionList(julia = Position(x = -0.3, zoom = 5.0)),
                isConvergent = true,
                hasDualFloat = true,
                compatTextures = Texture.convergent
        )
        val collatz = Shape(
                nameId = R.string.collatz,
                icon = R.drawable.collatz_icon,
                latexId = R.string.collatz_katex,
                loop = "collatz(z1, c)",
                positions = PositionList(
                        Position(zoom = 1.5, rotation = (-90.0).inRadians()),
                        Position(rotation = (-90.0).inRadians())
                ),
                bailoutRadius = 5e1f,
                isProFeature = true,
                hasDualFloat = true
        )
        val mandelex = Shape(
                nameId = R.string.mandelex,
                icon = R.drawable.mandelex_icon,
                loop = "mandelex(z1, c)",
                juliaSeed = true,
                positions = PositionList(Position(zoom = 2e1)),
                params = ParamList(listOf(
                        Param(180.0, Range(0.0, 360.0), name = "angle"),
                        Param(0.5,   Range(0.0, 1.0),   name = "radius"),
                        Param(2.0,   Range(0.0, 5.0),   name = "scale"),
                        Param(2.0,   Range(0.0, 5.0),   name = "linear")
                )),
                hasDualFloat = true,
                isProFeature = true
        )
        val new1 = Shape(
                nameId = R.string.empty,
                loop = "",
                isProFeature = true
        )
        val new2 = Shape(
                nameId = R.string.empty,
                loop = "",
                positions = PositionList(Position(zoom = 6.0)),
                power = 150f,
                compatTextures = Texture.mandelbrot,
                isProFeature = true
        )
        val binet = Shape(
                name = "BINET",
                loop = "binet(z1, c)",
                positions = PositionList(Position(zoom = 5.0)),
                hasDualFloat = true,
                isProFeature = true
        )


        val all = arrayListOf(
                binet,
                testshape,
                mandelbrot,
                mandelbrotCubic,
                mandelbrotQuartic,
                mandelbrotQuintic,
                mandelbrotPow,
                mandelex,
                collatz,
                clover,
                burningShip,
                mandelbox,
                kali,
                sine,
                sine2,
                horseshoeCrab,
                kleinian,
                nova1,
                nova2
        )

    }

    val isCustom : Boolean
        get() = customId != -1

    val compatTextures = if (BuildConfig.PAID_VERSION) compatTextures else compatTextures.filter { texture -> !texture.proFeature }

    val juliaModeInit = juliaMode
    var juliaMode = juliaModeInit
        set(value) {
            field = value
            numParamsInUse = params.size + if (juliaMode) 1 else 0
        }

    var numParamsInUse = params.size + if (juliaMode) 1 else 0

    fun initialize(res: Resources) {

        when {
            nameId == -1 && name == "" -> {
                throw Error("neither nameId nor name was passed to the constructor")
            }
            name == "" -> {
                name = res.getString(nameId)
            }
        }

    }

    fun clone() : Shape {
        return Shape(
                nameId,
                name,
                latexId,
                latex,
                icon,
                conditional,
                init,
                loop,
                final,
                compatTextures.toMutableList(),
                positions.clone(),
                params,
                juliaMode,
                juliaSeed,
                seed,
                maxIter,
                bailoutRadius
        )
    }
    fun reset() {

        params.reset()
        positions.reset()
        juliaMode = juliaModeInit

    }

    override fun equals(other: Any?): Boolean {
        return other is Shape && name == other.name
    }
    override fun hashCode(): Int {
        return name.hashCode()
    }

}