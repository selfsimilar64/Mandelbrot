package com.selfsimilartech.fractaleye

import android.graphics.Bitmap
import kotlin.math.roundToInt

class Texture (

        val name            : Int,
        val init            : Int = R.string.empty,
        val loop            : String = "",
        var final           : String = "",
        val isAverage       : Boolean = false,
        val usesFirstDelta  : Boolean = false,
        val usesSecondDelta : Boolean = false,
        val params          : List<Param> = listOf(),
        val auto            : Boolean = false,
        val bailoutRadius   : Float = 1e2f,
        val frequency       : Float? = null,
        val proFeature      : Boolean = false,
        val displayName     : Int = name

) {

    class Param (
            val name: Int,
            val min: Double = 0.0,
            val max: Double = 1.0,
            q: Double = 0.0,
            val discrete: Boolean = false,
            val proFeature: Boolean = false
    ) {


        private val qInit = q
        var q = qInit

        val interval = max - min
        var progress = 0.0


        init {
            setProgressFromValue()
        }

        fun setValueFromProgress() {
            q = (1.0 - progress)*min + progress*max
        }
        fun setProgressFromValue() {
            progress = (q - min) / (max - min)
        }

        override fun toString(): String {
            return if (discrete) "%d".format(q.roundToInt()) else "%.3f".format(q)
        }

        fun toString(Q: Double) : String {
            return if (discrete) "%d".format(Q.roundToInt()) else "%.3f".format(Q)
        }

        fun reset() {
            q = qInit
        }

    }

    companion object {

        val empty = Texture(name = R.string.empty)
        val absoluteEscape = Texture(
                R.string.empty
        )
        val escape = Texture(
                R.string.escape,
                final = "iteration_final(n)"
        )
        val converge = Texture(
                R.string.converge,
                final = "iteration_final(n)"
        )
        val escapeSmooth = Texture(
                R.string.escape_smooth,
                final = "escape_smooth_final(n, z, z1, textureIn)",
                bailoutRadius = 1e2f
        )
        val distanceEstimationInt = Texture(
                R.string.distance_est,
                final = "dist_estim_final(modsqrz, alpha)",
                usesFirstDelta = true,
                auto = true,
                bailoutRadius = 1e2f
        )
        val normalMap1 = Texture(
                R.string.normal1,
                final = "normal_map1_final(z, alpha)",
                usesFirstDelta = true,
                bailoutRadius = 1e1f,
                frequency = 1f
        )
        val normalMap2 = Texture(
                R.string.normal2,
                final = "normal_map2_final(modsqrz, z, alpha, beta)",
                usesFirstDelta = true,
                usesSecondDelta = true,
                bailoutRadius = 1e2f,
                frequency = 1f
        )
        val triangleIneqAvgInt = Texture(
                R.string.triangle_ineq_avg_int,
                loop = "triangle_ineq_avg_int_loop(sum, sum1, n, modc, z1, z2);",
                isAverage = true,
                params = listOf(Param(R.string.alpha, 0.0, 5.0, 1.0)),
                bailoutRadius = 1e6f,
                displayName = R.string.triangle_ineq_avg
        )
        val triangleIneqAvgFloat = Texture(
                name = R.string.triangle_ineq_avg_float,
                loop = "triangle_ineq_avg_float_loop(sum, sum1, n, modc, z1, z2);",
                isAverage = true,
                bailoutRadius = 1e6f,
                displayName = R.string.triangle_ineq_avg
        )
        val curvatureAvg = Texture(
                R.string.curvature_avg,
                loop = "curvature_avg_loop(sum, sum1, n, z, z1, z2);",
                isAverage = true,
                params = listOf(
                        Param(R.string.thickness, 0.075, 2.0, 1.0, false, proFeature = true)
                ),
                bailoutRadius = 1e8f
        )
        val stripeAvg = Texture(
                R.string.stripe_avg,
                loop = "stripe_avg_loop(sum, sum1, z);",
                isAverage = true,
                params = listOf(
                        Param(R.string.density, 1.0, 8.0, 2.0, true),
                        Param(R.string.thickness, 0.075, 2.0, 1.0, false, proFeature = true)
                ),
                bailoutRadius = 1e6f
        )
        val orbitTrap = Texture(
                R.string.orbit_trap_miny,
                loop = "orbit_trap_minx_loop(minx, z);",
                final = "minx",
                bailoutRadius = 1e2f
        )
        val overlayAvg = Texture(
                R.string.overlay_avg,
                loop = "overlay_avg_loop(sum, sum1, z);",
                isAverage = true,
                params = listOf(Param(R.string.sharpness, 0.4, 0.5, 0.49)),
                bailoutRadius = 1e2f
        )
        val exponentialSmoothing = Texture(
                R.string.exponential_smooth,
                loop = "exp_smoothing_loop(sum, modsqrz);",
                final = "exp_smoothing_final(sum)",
                bailoutRadius = 1e2f
        )
        val orbitTrapMinXY = Texture(
                R.string.empty,
                init = R.string.orbittrapminxy_init,
                bailoutRadius = 1e2f
        )
        val testAvg = Texture(
                name = R.string.alpha,
                loop = "test_avg_loop(sum, sum1, n, z, z1, z2, z3, z4, c);",
                params = listOf(
                        Param(R.string.alpha, 0.0, 2.0*Math.PI, 0.0, proFeature = true)
                ),
                isAverage = true,
                bailoutRadius = 1e10f,
                proFeature = true
        )



        val all = mutableListOf(
                testAvg,
                escape,
                escapeSmooth,
                converge,
                exponentialSmoothing,
                distanceEstimationInt,
                triangleIneqAvgInt,
                triangleIneqAvgFloat,
                curvatureAvg,
                stripeAvg,
                orbitTrap,
                normalMap1,
                normalMap2,
                overlayAvg
        )

        val mandelbrot = mutableListOf(
                testAvg,
                escape,
                escapeSmooth,
                exponentialSmoothing,
                distanceEstimationInt,
                triangleIneqAvgInt,
                triangleIneqAvgFloat,
                curvatureAvg,
                stripeAvg,
                orbitTrap,
                normalMap1,
                normalMap2,
                overlayAvg
        )

        val divergent = mutableListOf(
                testAvg,
                escape,
                escapeSmooth,
                exponentialSmoothing,
                curvatureAvg,
                stripeAvg,
                overlayAvg,
                orbitTrap
        )

        val convergent = mutableListOf(
                converge,
                exponentialSmoothing,
                orbitTrap
        )

    }


    init {
        if (isAverage) this.final = "avg_final(sum, sum1, n, z, z1, textureIn)"
    }

    var activeParam = if (params.isNotEmpty()) params[0] else Param(R.string.empty)

    val numParamsInUse = if (BuildConfig.PAID_VERSION) params.size else {

        params.filter { param -> !param.proFeature } .size

    }

    var thumbnail : Bitmap? = null

    override fun equals(other: Any?): Boolean {
        return other is Texture && name == other.name
    }

    override fun hashCode(): Int {
        return loop.hashCode()
    }

}