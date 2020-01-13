package com.selfsimilartech.fractaleye

import kotlin.math.sqrt

class Shape (
        val name            : Int,
        val katex           : Int                = R.string.empty,
        val icon            : Int                = R.drawable.mandelbrot_icon,
        val conditionalSF   : Int                = R.string.escape_sf,
        val initSF          : Int                = R.string.empty,
        val loopSF          : Int,
        val finalSF         : Int                = R.string.empty,
        val conditionalDF   : Int                = R.string.escape_df,
        val initDF          : Int                = R.string.empty,
        val loopDF          : Int                = R.string.empty,
        val finalDF         : Int                = R.string.empty,
        val textures        : ArrayList<Texture> = Texture.arbitrary,
        val positions       : PositionList       = PositionList(),
        params              : List<Param>        = listOf(),
        val juliaMode       : Boolean            = false,
        val z0              : Complex            = Complex.ZERO,   // seed?
        val bailoutRadius   : Float?             = null,
        val power           : Float              = 2f,
        val hasDynamicPower : Boolean            = false
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
                    // f.updateShapeParamEditTexts(key[1].toString().toInt())
                    // fsv.r.renderToTex = true
                }
            }
        var v = v
            set (value) {
                if (!vLocked) {
                    field = value
                    // f.updateShapeParamEditTexts(key[1].toString().toInt())
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


        val mandelbrot = Shape(
                R.string.mandelbrot,
                katex = R.string.mandelbrot_katex,
                icon = R.drawable.mandelbrot_icon,
                loopSF = R.string.mandelbrot_loop_sf,
                loopDF = R.string.mandelbrot_loop_df,
                textures = Texture.main,
                positions = PositionList(
                    default = Position(x = -0.75, scale = 3.5),
                    other = listOf(
                        Position(
                            x = -1.25735194436369140,
                            y = -0.07363029998042227,
                            scale = 1.87845e-3
                        ),
                        Position(
                            x = 0.39019590054025366,
                            y = -0.26701156160039610,
                            scale = 9.59743e-8,
                            rotation = 146.0.inRadians()
                        ),
                        Position(
                            x = -0.48414790254135703,
                            y = -0.59799104457234160,
                            scale = 6.15653e-4
                        )
                    )
                )
        )
        val mandelbrotCubic = Shape(
                R.string.mandelbrot_cubic,
                icon = R.drawable.mandelbrotcubic_icon,
                loopSF = R.string.mandelbrotcubic_loop_sf,
                loopDF = R.string.mandelbrotcubic_loop_df,
                textures = Texture.main,
                positions = PositionList(Position(scale = 3.5)),
                power = 3f
        )
        val mandelbrotQuartic = Shape(
                R.string.mandelbrot_quartic,
                icon = R.drawable.mandelbrotquartic_icon,
                loopSF = R.string.mandelbrotquartic_loop_sf,
                loopDF = R.string.mandelbrotquartic_loop_df,
                textures = Texture.main,
                positions = PositionList(Position(x = -0.175, scale = 3.5)),
                power = 4f
        )
        val mandelbrotQuintic = Shape(
                R.string.mandelbrot_quintic,
                icon = R.drawable.mandelbrotquintic_icon,
                loopSF = R.string.mandelbrotquintic_loop_sf,
                loopDF = R.string.mandelbrotquintic_loop_df,
                textures = Texture.main,
                positions = PositionList(Position(scale = 3.5)),
                power = 5f
        )
        val mandelbrotAnyPower = Shape(
                R.string.mandelbrot_anypow,
                katex = R.string.mandelbrotanypow_katex,
                icon = R.drawable.mandelbrotanypow_icon,
                loopSF = R.string.mandelbrotanypow_loop_sf,
                textures = Texture.main.replace(Texture.triangleIneqAvgInt, Texture.triangleIneqAvgFloat),
                positions = PositionList(Position(x = -0.55, y = 0.5, scale = 5.0)),
                params = listOf(Param(16.0, 4.0)),
                hasDynamicPower = true
        )
        val clover = Shape(
                R.string.clover,
                katex = R.string.dualpow_katex,
                icon = R.drawable.clover_icon,
                initSF = R.string.dualpow_init_sf,
                loopSF = R.string.dualpow_loop_sf,
                positions = PositionList(Position(scale = 2.0, rotation = 45.0.inRadians())),
                z0 = Complex.ONE,
                params = listOf(Param(2.0, vLocked = true)),
                hasDynamicPower = true
        )
        val mandelbox = Shape(
                R.string.mandelbox,
                katex = R.string.mandelbox_katex,
                icon = R.drawable.mandelbox_icon,
                loopSF = R.string.mandelbox_loop_sf,
                loopDF = R.string.mandelbox_loop_df,
                positions = PositionList(Position(scale = 6.5)),
                params = listOf(Param(-2.66421354, vLocked = true)),
                bailoutRadius = 5f
        )
        val kali = Shape(
                R.string.kali,
                katex = R.string.kali_katex,
                icon = R.drawable.kali_icon,
                loopSF = R.string.kali_loop_sf,
                conditionalDF = R.string.escape_df,
                juliaMode = true,
                positions = PositionList(julia = Position(scale = 3.0)),
                params = listOf(Param(-0.33170626, -0.18423799)),
                bailoutRadius = 4e0f
        )
        val kaliSquare = Shape(
                R.string.empty,
                loopSF = R.string.kalisquare_loop_sf,
                juliaMode = true,
                positions = PositionList(julia = Position(scale = 4.0)),
                bailoutRadius = 4e0f
        )
        val mandelbar = Shape(
                R.string.empty,
                loopSF = R.string.mandelbar_loop_sf,
                positions = PositionList(Position(scale = 3.0))
        )
        val logistic = Shape(
                R.string.empty,
                katex = R.string.logistic_katex,
                loopSF = R.string.logistic_loop_sf,
                loopDF = R.string.logistic_loop_df,
                positions = PositionList(Position(scale = 3.5)),
                z0 = Complex(0.5, 0.0)
        )
        val burningShip = Shape(
                R.string.burning_ship,
                katex = R.string.burningship_katex,
                icon = R.drawable.burningship_icon,
                loopSF = R.string.burningship_loop_sf,
                loopDF = R.string.burningship_loop_df,
                positions = PositionList(Position(-0.4, -0.6, 4.0, Math.PI))
        )
        val magnet = Shape(
                R.string.empty,
                loopSF = R.string.magnet_loop_sf,
                positions = PositionList(Position(scale = 3.5)),
                params = listOf(
                        Param(-1.0, vLocked = true),
                        Param(-2.0, vLocked = true)),
                bailoutRadius = 4e0f
        )
        val sine1 = Shape(
                R.string.sine1,
                katex = R.string.sine1_katex,
                icon = R.drawable.sine1_icon,
                loopSF = R.string.sine1_loop_sf,
                positions = PositionList(Position(scale = 6.0)),
                bailoutRadius = 1e4f
        )
        val sine2 = Shape(
                R.string.sine2,
                katex = R.string.sine2_katex,
                icon = R.drawable.sine2_icon,
                loopSF = R.string.sine2_loop_sf,
                positions = PositionList(Position(scale = 3.5)),
                params = listOf(Param(-0.26282884)),
                z0 = Complex.ONE
        )
        val sine3 = Shape(
                R.string.empty,
                katex = R.string.sine3_katex,
                loopSF = R.string.sine3_loop_sf,
                positions = PositionList(Position(scale = 3.5)),
                params = listOf(Param(0.31960705187983646, vLocked = true)),
                z0 = Complex.ONE,
                bailoutRadius = 1e1f
        )
        val horseshoeCrab = Shape(
                R.string.horseshoe_crab,
                katex = R.string.horseshoecrab_katex,
                icon = R.drawable.horseshoecrab_icon,
                loopSF = R.string.horseshoecrab_loop_sf,
                positions = PositionList(Position(x = -0.25, scale = 6.0, rotation = 90.0.inRadians())),
                params = listOf(Param(sqrt(2.0))),
                z0 = Complex.ONE
        )
        val newton2 = Shape(
                R.string.empty,
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
        val newton3 = Shape(
                R.string.empty,
                katex = R.string.newton3_katex,
                conditionalSF = R.string.converge_sf,
                loopSF = R.string.newton3_loop_sf,
                positions = PositionList(julia = Position(scale = 5.0)),
                juliaMode = true
        )
        val persianRug = Shape(
                R.string.empty,
                katex = R.string.persianrug_katex,
                initSF = R.string.persianrug_init_sf,
                loopSF = R.string.persianrug_loop_sf,
                positions = PositionList(Position(scale = 1.5)),
                params = listOf(Param(0.642, 0.0)),
                bailoutRadius = 1e1f
        )
        val kleinian = Shape(
                R.string.kleinian,
                icon = R.drawable.kleinian_icon,
                initSF = R.string.kleinian_init_sf,
                loopSF = R.string.kleinian_loop_sf,
                positions = PositionList(julia = Position(y = -0.5, scale = 1.5)),
                params = listOf(
                        Param(1.41421538, vLocked = true),
                        Param(0.0, -1.0)
                ),
                juliaMode = true,
                bailoutRadius = 1e5f
        )
        val nova1 = Shape(
                R.string.nova1,
                katex = R.string.nova1_katex,
                icon = R.drawable.nova1_icon,
                conditionalSF = R.string.converge_sf,
                loopSF = R.string.nova1_loop_sf,
                conditionalDF = R.string.converge_df,
                loopDF = R.string.nova1_loop_df,
                positions = PositionList(Position(x = -0.3, scale = 1.75, rotation = 90.0.inRadians())),
                z0 = Complex.ONE,
                params = listOf(
                        Param(1.0, 0.0)
                )
        )
        val nova2 = Shape(
                R.string.nova2,
                katex = R.string.nova2_katex,
                icon = R.drawable.nova2_icon,
                conditionalSF = R.string.converge_sf,
                loopSF = R.string.nova2_loop_sf,
                juliaMode = true,
                positions = PositionList(julia = Position(x = -0.3, scale = 5.0))
        )
        val fibonacciPowers = Shape(
                R.string.empty,
                initSF = R.string.fibonacci_init_sf,
                loopSF = R.string.fibonacci_loop_sf,
                juliaMode = true,
                bailoutRadius = 1e3f
        )
        val test = Shape(
                R.string.empty,
                conditionalSF = R.string.converge_sf,
                initSF = R.string.test_init_sf,
                loopSF = R.string.test_loop_sf,
                z0 = Complex.ONE,
                positions = PositionList(Position(scale = 3.5))
        )
        val pro = arrayListOf(
                mandelbrot,
                mandelbrotCubic,
                mandelbrotQuartic,
                mandelbrotQuintic,
                mandelbrotAnyPower,
                clover,
                burningShip,
                mandelbox,
                kali,
                sine1,
                sine2,
                horseshoeCrab,
                kleinian,
                nova1,
                nova2
        )
        val free = arrayListOf(
                mandelbrot,
                mandelbrotAnyPower,
                clover,
                burningShip,
                mandelbox,
                kali,
                sine1,
                sine2,
                horseshoeCrab,
                kleinian,
                nova1,
                nova2
        )

    }

    val numParams = params.size
    val hasDualFloat = loopDF != R.string.empty
    val params = List(NUM_MAP_PARAMS) { i: Int ->
        if (i < params.size) { params[i] }
        else { Param() }
    }
    var activeParamIndex = 0
    var activeParam = this.params[0]
        set(value) {
            field = value
            activeParamIndex = params.indexOf(field)
        }

    fun clone() : Shape {
        return Shape(
                name,
                katex,
                icon,
                conditionalSF,
                initSF,
                loopSF,
                finalSF,
                conditionalDF,
                initDF,
                loopDF,
                finalDF,
                textures,
                positions.clone(),
                params,
                juliaMode,
                z0,
                bailoutRadius)
    }

    override fun equals(other: Any?): Boolean {
        return other is Shape && name == other.name
    }
    override fun hashCode(): Int {
        return name.hashCode()
    }

}