package com.selfsimilartech.fractaleye

import kotlin.math.sqrt

class ComplexMap (
        val name            : String,
        val katex           : Int                = R.string.empty,
        val icon            : Int                = R.drawable.mandelbrot_icon,
        val conditionalSF   : Int                = R.string.empty,
        val initSF          : Int                = R.string.empty,
        val loopSF          : Int                = R.string.empty,
        val finalSF         : Int                = R.string.empty,
        val conditionalDF   : Int                = R.string.empty,
        val initDF          : Int                = R.string.empty,
        val loopDF          : Int                = R.string.empty,
        val finalDF         : Int                = R.string.empty,
        val textures        : List<String>       = Texture.arbitrary.keys.toList(),
        val positions       : PositionList       = PositionList(),
        params              : List<Param>        = listOf(),
        val juliaMode       : Boolean            = false,
        val z0              : Complex            = Complex.ZERO,   // seed?
        val bailoutRadius   : Float?             = null
) {


    class Param (
            u: Double = 0.0,
            v: Double = 0.0,
            uLocked: Boolean = false,
            vLocked: Boolean = false
    ) {

        private val uInit = u
        private val vInit = v
        private val uLockedInit = uLocked
        private val vLockedInit = vLocked

        var uLocked = uLockedInit
        var vLocked = vLockedInit
        var u = u
            set (value) {
                if (!uLocked) {
                    field = value
                    // f.updateMapParamEditText(key[1].toString().toInt())
                    // fsv.r.renderToTex = true
                }
            }
        var v = v
            set (value) {
                if (!vLocked) {
                    field = value
                    // f.updateMapParamEditText(key[1].toString().toInt())
                    // fsv.r.renderToTex = true
                }
            }

        fun reset() {
            uLocked = false
            vLocked = false
            u = uInit
            v = vInit
            uLocked = uLockedInit
            vLocked = vLockedInit
        }

    }


    companion object {


        val empty = ComplexMap("Empty")
        val mandelbrot = ComplexMap(
                "Mandelbrot",
                katex = R.string.mandelbrot_katex,
                icon = R.drawable.mandelbrot_icon,
                conditionalSF = R.string.escape_sf,
                loopSF = R.string.mandelbrot_loop_sf,
                conditionalDF = R.string.escape_df,
                loopDF = R.string.mandelbrot_loop_df,
                textures = Texture.all.keys.toList(),
                positions = PositionList(Position(x = -0.75, scale = 3.5))
        )
        val mandelbrotPower = ComplexMap(
                "Mandelbrot Power",
                katex = R.string.mandelbrotcpow_katex,
                icon = R.drawable.mandelbrotpower_icon,
                conditionalSF = R.string.escape_sf,
                loopSF = R.string.mandelbrotcpow_loop_sf,
                positions = PositionList(Position(scale = 3.5)),
                params = listOf(Param(4.0, vLocked = true))
        )
        val mandelbrotDualPower = ComplexMap(
                "Mandelbrot Dual Power",
                katex = R.string.dualpow_katex,
                icon = R.drawable.mandelbrotdualpower_icon,
                conditionalSF = R.string.escape_sf,
                initSF = R.string.dualpow_init_sf,
                loopSF = R.string.dualpow_loop_sf,
                positions = PositionList(Position(scale = 3.0)),
                z0 = Complex.ONE,
                params = listOf(Param(2.0, vLocked = true))
        )
        val mandelbox = ComplexMap(
                "Mandelbox",
                katex = R.string.mandelbox_katex,
                icon = R.drawable.mandelbox_icon,
                conditionalSF = R.string.escape_sf,
                loopSF = R.string.mandelbox_loop_sf,
                conditionalDF = R.string.escape_df,
                loopDF = R.string.mandelbox_loop_df,
                positions = PositionList(Position(scale = 5.0)),
                params = listOf(Param(-2.66421354, vLocked = true)),
                bailoutRadius = 5f
        )
        val kali = ComplexMap(
                "Kali",
                katex = R.string.kali_katex,
                icon = R.drawable.kali_icon,
                conditionalSF = R.string.escape_sf,
                loopSF = R.string.kali_loop_sf,
                conditionalDF = R.string.escape_df,
                loopDF = R.string.kali_loop_df,
                juliaMode = true,
                positions = PositionList(julia = Position(scale = 2.0)),
                params = listOf(Param(-0.33170626, -0.18423799)),
                bailoutRadius = 4e0f
        )
        val kaliSquare = ComplexMap(
                "Kali Square",
                conditionalSF = R.string.escape_sf,
                loopSF = R.string.kalisquare_loop_sf,
                juliaMode = true,
                positions = PositionList(julia = Position(scale = 4.0)),
                bailoutRadius = 4e0f
        )
        val mandelbar = ComplexMap(
                "Mandelbar",
                conditionalSF = R.string.escape_sf,
                loopSF = R.string.mandelbar_loop_sf,
                positions = PositionList(Position(scale = 3.0))
        )
        val logistic = ComplexMap(
                "Logistic",
                katex = R.string.logistic_katex,
                conditionalSF = R.string.escape_sf,
                loopSF = R.string.logistic_loop_sf,
                conditionalDF = R.string.escape_df,
                loopDF = R.string.logistic_loop_df,
                positions = PositionList(Position(scale = 3.5)),
                z0 = Complex(0.5, 0.0)
        )
        val burningShip = ComplexMap(
                "Burning Ship",
                katex = R.string.burningship_katex,
                icon = R.drawable.burningship_icon,
                conditionalSF = R.string.escape_sf,
                loopSF = R.string.burningship_loop_sf,
                conditionalDF = R.string.escape_df,
                loopDF = R.string.burningship_loop_df,
                positions = PositionList(Position(-0.45, -0.25, 3.5, Math.PI))
        )
        val magnet = ComplexMap(
                "Magnet",
                conditionalSF = R.string.escape_sf,
                loopSF = R.string.magnet_loop_sf,
                positions = PositionList(Position(scale = 3.5)),
                params = listOf(
                        Param(-1.0, vLocked = true),
                        Param(-2.0, vLocked = true)),
                bailoutRadius = 4e0f
        )
        val sine1 = ComplexMap(
                "Sine 1",
                katex = R.string.sine1_katex,
                icon = R.drawable.sine1_icon,
                conditionalSF = R.string.escape_sf,
                loopSF = R.string.sine1_loop_sf,
                positions = PositionList(Position(scale = 3.5)),
                bailoutRadius = 1e4f
        )
        val sine2 = ComplexMap(
                "Sine 2",
                katex = R.string.sine2_katex,
                icon = R.drawable.sine2_icon,
                conditionalSF = R.string.escape_sf,
                loopSF = R.string.sine2_loop_sf,
                positions = PositionList(Position(scale = 3.5)),
                params = listOf(Param(-0.26282884)),
                z0 = Complex.ONE
        )
        val sine3 = ComplexMap(
                "Sine 3",
                katex = R.string.sine3_katex,
                conditionalSF = R.string.escape_sf,
                loopSF = R.string.sine3_loop_sf,
                positions = PositionList(Position(scale = 3.5)),
                params = listOf(Param(0.31960705187983646, vLocked = true)),
                z0 = Complex.ONE,
                bailoutRadius = 1e1f
        )
        val horseshoeCrab = ComplexMap(
                "Horseshoe Crab",
                katex = R.string.horseshoecrab_katex,
                icon = R.drawable.horseshoecrab_icon,
                conditionalSF = R.string.escape_sf,
                loopSF = R.string.horseshoecrab_loop_sf,
                positions = PositionList(Position(scale = 5.0)),
                params = listOf(Param(sqrt(2.0))),
                z0 = Complex.ONE
        )
        val newton2 = ComplexMap(
                "Newton 2",
                conditionalSF = R.string.converge_sf,
                loopSF = R.string.newton2_loop_sf,
                positions = PositionList(julia = Position(scale = 3.5)),
                params = listOf(
                        Param(1.0, 1.0),
                        Param(-1.0, -1.0),
                        Param(2.0, -0.5)
                ),
                juliaMode = true
        )
        val newton3 = ComplexMap(
                "Newton 3",
                katex = R.string.newton3_katex,
                conditionalSF = R.string.converge_sf,
                loopSF = R.string.newton3_loop_sf,
                positions = PositionList(julia = Position(scale = 5.0)),
                juliaMode = true
        )
        val persianRug = ComplexMap(
                "Persian Rug",
                katex = R.string.persianrug_katex,
                initSF = R.string.persianrug_init_sf,
                conditionalSF = R.string.escape_sf,
                loopSF = R.string.persianrug_loop_sf,
                positions = PositionList(Position(scale = 1.5)),
                params = listOf(Param(0.642, 0.0)),
                bailoutRadius = 1e1f
        )
        val kleinian = ComplexMap(
                "Kleinian",
                icon = R.drawable.kleinian_icon,
                conditionalSF = R.string.escape_sf,
                initSF = R.string.kleinian_init_sf,
                loopSF = R.string.kleinian_loop_sf,
                positions = PositionList(julia = Position(y = -0.5, scale = 1.2)),
                params = listOf(
                        Param(2.0, vLocked = true),
                        Param(0.0, -1.0)
                ),
                juliaMode = true,
                bailoutRadius = 1e5f
        )
        val nova1 = ComplexMap(
                "Nova 1",
                katex = R.string.nova1_katex,
                icon = R.drawable.nova1_icon,
                conditionalSF = R.string.converge_sf,
                loopSF = R.string.nova1_loop_sf,
                positions = PositionList(Position(x = -0.3, scale = 1.5)),
                z0 = Complex.ONE,
                params = listOf(
                        Param(1.0, 0.0)
                )
        )
        val nova2 = ComplexMap(
                "Nova 2",
                katex = R.string.nova2_katex,
                icon = R.drawable.nova2_icon,
                conditionalSF = R.string.converge_sf,
                loopSF = R.string.nova2_loop_sf,
                juliaMode = true,
                positions = PositionList(julia = Position(x = -0.3, scale = 5.0))
        )
        val test = ComplexMap(
                "Test",
                conditionalSF = R.string.converge_sf,
                initSF = R.string.test_init_sf,
                loopSF = R.string.test_loop_sf,
                z0 = Complex.ONE,
                positions = PositionList(Position(scale = 3.5))
        )
        val all = mapOf(
                "Mandelbrot"             to  mandelbrot,
                "Mandelbrot Power"       to  mandelbrotPower,
                "Mandelbrot Dual Power"  to  mandelbrotDualPower,
                "Burning Ship"           to  burningShip,
                "Mandelbox"              to  mandelbox,
                "Kali"                   to  kali,
                "Sine 1"                 to  sine1,
                "Sine 2"                 to  sine2,
                "Horseshoe Crab"         to  horseshoeCrab,
                "Kleinian"               to  kleinian,
                "Nova 1"                 to  nova1,
                "Nova 2"                 to  nova2,
                "Test"                   to  test
        )

    }

    val numParams = params.size
    val hasDualFloat = loopDF != R.string.empty
    val params = List(NUM_MAP_PARAMS) { i: Int ->
        if (i < params.size) { params[i] }
        else { Param() }
    }
    var position = if (juliaMode) positions.julia else positions.default


    override fun toString() : String { return name }
    override fun equals(other: Any?): Boolean {
        return other is ComplexMap && name == other.name
    }
    override fun hashCode(): Int {
        return name.hashCode()
    }

}