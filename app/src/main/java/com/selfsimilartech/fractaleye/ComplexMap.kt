package com.selfsimilartech.fractaleye

import android.content.res.Resources
import kotlin.math.sqrt

class ComplexMap (
        val name            : String,
        val katex           : String             = "$$",
        val icon            : Int                = R.drawable.mandelbrot_icon,
        val conditionalSF   : String?            = "",
        val initSF          : String?            = "",
        val loopSF          : String?            = "",
        val finalSF         : String?            = "",
        val conditionalDF   : String?            = "",
        val initDF          : String?            = "",
        val loopDF          : String?            = "",
        val finalDF         : String?            = "",
        val textures        : List<String>       = Texture.arbitrary.keys.toList(),
        val coords          : DoubleArray        = doubleArrayOf(0.0, 0.0),
        val scale           : Double             = 1.0,
        val params          : List<Param>        = listOf(),
        val z0              : DoubleArray        = doubleArrayOf(0.0, 0.0),  // seed?
        val juliaMode       : Boolean            = false,
        val bailout         : Float              = 1e5f
) {

    class Lockable (
            private var u: Double = 0.0,
            var locked: Boolean = false
    ) {

        fun get() : Double { return u }
        fun set(U: Double, add: Boolean = true) {
            if (!locked) {
                if (add) { u += U }
                else { u = U }
            }
        }

    }
    class Param (
        initU: Double = 0.0,
        initV: Double = 0.0,
        uLocked: Boolean = false,
        vLocked: Boolean = false
    ) {

        val u = Lockable(initU, uLocked)
        val v = Lockable(initV, vLocked)

    }

    companion object {

        val empty           = { ComplexMap("Empty", "$$") }
        val mandelbrot          = { res: Resources -> ComplexMap(
                "Mandelbrot",
                katex = res.getString(R.string.mandelbrot_katex),
                icon = R.drawable.mandelbrot_icon,
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.mandelbrot_loop_sf),
                conditionalDF = res.getString(R.string.escape_df),
                loopDF = res.getString(R.string.mandelbrot_loop_df),
                textures = Texture.all.keys.toList(),
                coords = doubleArrayOf(-0.75, 0.0),
                scale = 3.5
        )}
        val mandelbrotPower     = { res: Resources -> ComplexMap(
                "Mandelbrot Power",
                katex = res.getString(R.string.mandelbrotcpow_katex),
                icon = R.drawable.mandelbrotpower_icon,
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.mandelbrotcpow_loop_sf),
                scale = 3.5,
                params = listOf(Param(4.0, vLocked = true))
        )}
        val mandelbrotDualPower = { res: Resources -> ComplexMap(
                "Mandelbrot Dual Power",
                katex = res.getString(R.string.dualpow_katex),
                icon = R.drawable.mandelbrotdualpower_icon,
                conditionalSF = res.getString(R.string.escape_sf),
                initSF = res.getString(R.string.dualpow_init_sf),
                loopSF = res.getString(R.string.dualpow_loop_sf),
                scale = 3.0,
                z0 = doubleArrayOf(1.0, 0.0),
                params = listOf(Param(2.0, vLocked = true))
        )}
        val mandelbox           = { res: Resources -> ComplexMap(
                "Mandelbox",
                katex = res.getString(R.string.mandelbox_katex),
                icon = R.drawable.mandelbox_icon,
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.mandelbox_loop_sf),
                conditionalDF = res.getString(R.string.escape_df),
                loopDF = res.getString(R.string.mandelbox_loop_df),
                scale = 5.0,
                params = listOf(Param(-2.66421354, vLocked = true))
        ) }
        val kali                = { res: Resources -> ComplexMap(
                "Kali",
                katex = res.getString(R.string.kali_katex),
                icon = R.drawable.kali_icon,
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.kali_loop_sf),
                conditionalDF = res.getString(R.string.escape_df),
                loopDF = res.getString(R.string.kali_loop_df),
                juliaMode = true,
                bailout = 4e0f,
                scale = 4.0,
                params = listOf(Param(-0.33170626, -0.18423799))
        ) }
        val kaliSquare          = { res: Resources -> ComplexMap(
                "Kali Square",
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.kalisquare_loop_sf),
                juliaMode = true,
                bailout = 4e0f,
                scale = 4.0
        ) }
        val mandelbar           = { res: Resources -> ComplexMap(
                "Mandelbar",
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.mandelbar_loop_sf),
                scale = 3.5
        )}
        val logistic            = { res: Resources -> ComplexMap(
                "Logistic",
                katex = res.getString(R.string.logistic_katex),
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.logistic_loop_sf),
                conditionalDF = res.getString(R.string.escape_df),
                loopDF = res.getString(R.string.logistic_loop_df),
                scale = 3.5,
                z0 = doubleArrayOf(0.5, 0.0)
        ) }
        val burningShip         = { res: Resources -> ComplexMap(
                "Burning Ship",
                katex = res.getString(R.string.burningship_katex),
                icon = R.drawable.burningship_icon,
                conditionalSF = res.getString(R.string.escape_sf),
                initSF = res.getString(R.string.burningship_init_sf),
                loopSF = res.getString(R.string.burningship_loop_sf),
                conditionalDF = res.getString(R.string.escape_df),
                initDF = res.getString(R.string.burningship_init_df),
                loopDF = res.getString(R.string.burningship_loop_df),
                coords = doubleArrayOf(-0.45, 0.25),
                scale = 3.5
        ) }
        val magnet              = { res: Resources -> ComplexMap(
                "Magnet",
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.magnet_loop_sf),
                scale = 3.5,
                bailout = 4e0f,
                params = listOf(
                        Param(-1.0, vLocked = true),
                        Param(-2.0, vLocked = true))
        ) }
        val sine1               = { res: Resources -> ComplexMap(
                "Sine 1",
                katex = res.getString(R.string.sine1_katex),
                icon = R.drawable.sine1_icon,
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.sine1_loop_sf),
                conditionalDF = res.getString(R.string.escape_df),
                loopDF = res.getString(R.string.sine1_loop_df),
                scale = 3.5
        )}
        val sine2               = { res: Resources -> ComplexMap(
                "Sine 2",
                katex = res.getString(R.string.sine2_katex),
                icon = R.drawable.sine2_icon,
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.sine2_loop_sf),
                scale = 3.5,
                params = listOf(Param(-0.26282884)),
                z0 = doubleArrayOf(1.0, 0.0)
        ) }
        val sine3               = { res: Resources -> ComplexMap(
                "Sine 3",
                katex = res.getString(R.string.sine3_katex),
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.sine3_loop_sf),
                scale = 3.5,
                bailout = 1e1f,
                params = listOf(Param(0.31960705187983646, vLocked = true)),
                z0 = doubleArrayOf(1.0, 0.0)
        )}
        val horseshoeCrab       = { res: Resources -> ComplexMap(
                "Horseshoe Crab",
                katex = res.getString(R.string.horseshoecrab_katex),
                icon = R.drawable.horseshoecrab_icon,
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.horseshoecrab_loop_sf),
                conditionalDF = res.getString(R.string.escape_df),
                loopDF = res.getString(R.string.horseshoecrab_loop_df),
                scale = 5.0,
                params = listOf(Param(sqrt(2.0))),
                z0 = doubleArrayOf(1.0, 0.0)
        )}
        val newton2             = { res: Resources -> ComplexMap(
                "Newton 2",
                conditionalSF = res.getString(R.string.converge_sf),
                loopSF = res.getString(R.string.newton2_loop_sf),
                scale = 3.5,
                params = listOf(
                        Param(1.0, 1.0),
                        Param(-1.0, -1.0),
                        Param(2.0, -0.5)
                ),
                juliaMode = true
        ) }
        val newton3             = { res: Resources -> ComplexMap(
                "Newton 3",
                katex = res.getString(R.string.newton3_katex),
                conditionalSF = res.getString(R.string.converge_sf),
                loopSF = res.getString(R.string.newton3_loop_sf),
                scale = 5.0,
                juliaMode = true
        ) }
        val persianRug          = { res: Resources -> ComplexMap(
                "Persian Rug",
                katex = res.getString(R.string.persianrug_katex),
                initSF = res.getString(R.string.persianrug_init_sf),
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.persianrug_loop_sf),
                scale = 1.5,
                params = listOf(Param(0.642, 0.0)),
                bailout = 1e1f
        )}
        val kleinian            = { res: Resources -> ComplexMap(
                "Kleinian",
                icon = R.drawable.kleinian_icon,
                conditionalSF = res.getString(R.string.escape_sf),
                initSF = res.getString(R.string.kleinian_init_sf),
                loopSF = res.getString(R.string.kleinian_loop_sf),
                scale = 1.2,
                coords = doubleArrayOf(0.0, -0.5),
                params = listOf(
                        Param(2.0, vLocked = true),
                        Param(0.0, -1.0)
                ),
                juliaMode = true
        )}
        val nova1               = { res: Resources -> ComplexMap(
                "Nova 1",
                katex = res.getString(R.string.nova1_katex),
                icon = R.drawable.nova1_icon,
                conditionalSF = res.getString(R.string.converge_sf),
                loopSF = res.getString(R.string.nova1_loop_sf),
                coords = doubleArrayOf(-0.3, 0.0),
                scale = 1.5,
                z0 = doubleArrayOf(1.0, 0.0),
                params = listOf(
                        Param(1.0, 0.0)
                )
        ) }
        val nova2               = { res: Resources -> ComplexMap(
                "Nova 2",
                katex = res.getString(R.string.nova2_katex),
                icon = R.drawable.nova2_icon,
                conditionalSF = res.getString(R.string.converge_sf),
                loopSF = res.getString(R.string.nova2_loop_sf),
                juliaMode = true,
                scale = 5.0,
                coords = doubleArrayOf(-0.3, 0.0)
        ) }
        val test                = { res: Resources -> ComplexMap(
                "Test",
                conditionalSF = res.getString(R.string.converge_sf),
                initSF = res.getString(R.string.test_init_sf),
                loopSF = res.getString(R.string.test_loop_sf),
                z0 = doubleArrayOf(1.0, 0.0),
                scale = 3.5
        )}
        val all                 = mapOf(
                "Mandelbrot"            to  mandelbrot,
                "Mandelbrot Power"      to  mandelbrotPower,
                "Mandelbrot Dual Power" to  mandelbrotDualPower,
                "Burning Ship"          to  burningShip,
                "Mandelbox"             to  mandelbox,
                "Kali"                  to  kali,
                "Sine 1"                to  sine1,
                "Sine 2"                to  sine2,
                "Horseshoe Crab"        to  horseshoeCrab,
                "Kleinian"              to  kleinian,
                "Nova 1"                to  nova1,
                "Nova 2"                to  nova2,
                "Test"                  to  test
        )

    }

    val hasDualFloat = loopDF != ""
    override fun toString() : String { return name }
    override fun equals(other: Any?): Boolean {
        return other is ComplexMap && name == other.name
    }
    override fun hashCode(): Int {
        return name.hashCode()
    }

}