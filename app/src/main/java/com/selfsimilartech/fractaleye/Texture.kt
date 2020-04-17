package com.selfsimilartech.fractaleye

import android.graphics.Bitmap
import kotlin.math.roundToInt

class Texture (
        val name            : Int,
        val initSF          : Int = R.string.empty,
        val loopSF          : Int = R.string.empty,
        val finalSF         : Int = R.string.empty,
        val initDF          : Int = R.string.empty,
        val loopDF          : Int = R.string.empty,
        val finalDF         : Int = R.string.empty,
        val params          : List<Param> = listOf(),
        val auto            : Boolean = false,
        val bailoutRadius   : Float? = null,
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

    var activeParam = if (params.isNotEmpty()) params[0] else Param(R.string.empty)

    companion object {

        val empty = Texture(name = R.string.empty)
        val absoluteEscape = Texture(
                R.string.empty,
                finalSF = R.string.absolute_escape_final,
                finalDF = R.string.absolute_escape_final
        )
        val escape = Texture(
                R.string.escape,
                finalSF = R.string.escape_final,
                finalDF = R.string.escape_final
        )
        val escapeSmooth = Texture(
                R.string.escape_smooth,
                finalSF = R.string.mandelbrot_smooth_final_sf,
                finalDF = R.string.mandelbrot_smooth_final_df,
                bailoutRadius = 1e2f
        )
        val distanceEstimationInt = Texture(
                R.string.distance_est,
                initSF = R.string.mandelbrot_dist_int_init_sf,
                loopSF = R.string.mandelbrot_dist_int_loop_sf,
                finalSF = R.string.mandelbrot_dist_int_final_sf,
                initDF = R.string.mandelbrot_dist_int_init_df,
                loopDF = R.string.mandelbrot_dist_int_loop_df,
                finalDF = R.string.mandelbrot_dist_int_final_df,
                auto = true,
                bailoutRadius = 1e2f
        )
        val normalMap1 = Texture(
                R.string.normal1,
                initSF = R.string.mandelbrot_normal1_init_sf,
                loopSF = R.string.mandelbrot_normal1_loop_sf,
                finalSF = R.string.mandelbrot_normal1_final_sf,
                initDF = R.string.mandelbrot_normal1_init_df,
                loopDF = R.string.mandelbrot_normal1_loop_df,
                finalDF = R.string.mandelbrot_normal1_final_df,
                bailoutRadius = 1e2f,
                frequency = 1f
        )
        val normalMap2 = Texture(
                R.string.normal2,
                initSF = R.string.mandelbrot_normal2_init_sf,
                loopSF = R.string.mandelbrot_normal2_loop_sf,
                finalSF = R.string.mandelbrot_normal2_final_sf,
                initDF = R.string.mandelbrot_normal2_init_df,
                loopDF = R.string.mandelbrot_normal2_loop_df,
                finalDF = R.string.mandelbrot_normal2_final_df,
                bailoutRadius = 1e2f,
                frequency = 1f
        )
        val distanceGradient = Texture(
                name = R.string.empty,
                initSF = R.string.mandelbrot_distgrad_init_sf,
                loopSF = R.string.mandelbrot_distgrad_loop_sf,
                finalSF = R.string.mandelbrot_distgrad_final_sf,
                initDF = R.string.mandelbrot_distgrad_init_df,
                loopDF = R.string.mandelbrot_distgrad_loop_df,
                finalDF = R.string.mandelbrot_distgrad_final_df
        )
        val lighting = Texture(
                R.string.empty,
                R.string.mandelbrot_light_init_sf,
                R.string.mandelbrot_light_loop_sf,
                R.string.mandelbrot_light_final_sf,
                R.string.mandelbrot_light_init_df,
                R.string.mandelbrot_light_loop_df,
                R.string.mandelbrot_light_final_df
        )
        val triangleIneqAvgInt = Texture(
                R.string.triangle_ineq_avg_int,
                R.string.triangle_init_sf,
                R.string.triangle_int_loop_sf,
                R.string.triangle_final_sf,
                R.string.triangle_init_df,
                R.string.triangle_int_loop_df,
                R.string.triangle_final_df,
                bailoutRadius = 1e6f,
                displayName = R.string.triangle_ineq_avg
        )
        val triangleIneqAvgFloat = Texture(
                R.string.triangle_ineq_avg_float,
                R.string.triangle_init_sf,
                R.string.triangle_float_loop_sf,
                R.string.triangle_final_sf,
                R.string.triangle_init_df,
                R.string.triangle_float_loop_df,
                R.string.triangle_final_df,
                bailoutRadius = 1e6f,
                displayName = R.string.triangle_ineq_avg
        )
        val curvatureAvg = Texture(
                R.string.curvature_avg,
                R.string.curvature_init,
                R.string.curvature_loop_sf,
                R.string.curvature_final_sf,
                R.string.curvature_init,
                R.string.curvature_loop_df,
                R.string.curvature_final_df,
                listOf(
                        Param(R.string.thickness, 0.075, 2.0, 1.0, false, proFeature = true)
                ),
                bailoutRadius = 1e8f
        )
        val stripeAvg = Texture(
                R.string.stripe_avg,
                R.string.stripe_init,
                R.string.stripe_loop_sf,
                R.string.stripe_final_sf,
                R.string.stripe_init,
                R.string.stripe_loop_df,
                R.string.stripe_final_df,
                listOf(
                        Param(R.string.density, 1.0, 8.0, 2.0, true),
                        Param(R.string.thickness, 0.075, 2.0, 1.0, false, proFeature = true)
                ),
                bailoutRadius = 1e6f
        )
        val stripeMedianBins = Texture(
                R.string.stripe_median_bins,
                R.string.stripe_bins_init,
                R.string.stripe_bins_loop_sf,
                R.string.stripe_bins_median_final_sf,
                params = listOf(
                        Param(R.string.density, 1.0, 8.0, 2.0, true)
                ),
                bailoutRadius = 1e9f,
                proFeature = true
        )
        val orbitTrap = Texture(
                R.string.orbit_trap_miny,
                R.string.orbittrap_init,
                R.string.orbittrap_loop_sf,
                R.string.orbittrap_final_radius,
                R.string.orbittrap_init,
                R.string.orbittrap_loop_df,
                R.string.orbittrap_final_radius,
                bailoutRadius = 1e2f
        )
        val overlayAvg = Texture(
                R.string.overlay_avg,
                R.string.overlay_init,
                R.string.overlay_loop_sf,
                R.string.overlay_final_sf,
                R.string.overlay_init,
                R.string.overlay_loop_df,
                R.string.overlay_final_df,
                listOf(Param(R.string.sharpness, 0.4, 0.5, 0.49)),
                bailoutRadius = 1e2f
        )
        val exponentialSmoothing = Texture(
                R.string.exponential_smooth,
                initSF = R.string.exponential_smooth_init,
                loopSF = R.string.exponential_smooth_loop_sf,
                finalSF = R.string.exponential_smooth_final,
                initDF = R.string.exponential_smooth_init,
                loopDF = R.string.exponential_smooth_loop_df,
                finalDF = R.string.exponential_smooth_final,
                bailoutRadius = 1e2f
        )
        val orbitTrapMinXY = Texture(
                R.string.empty,
                initSF = R.string.orbittrapminxy_init,
                loopSF = R.string.orbittrapminxy_loop_sf,
                finalSF = R.string.orbittrapminxy_final_radius,
                bailoutRadius = 1e2f
        )
        val arbitrary = mutableListOf(
                escape,
                exponentialSmoothing,
                curvatureAvg,
                stripeAvg,
                overlayAvg,
                orbitTrap
        )

        val all = mutableListOf(
                stripeMedianBins,
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

    }

    val numParamsInUse = if (BuildConfig.PAID_VERSION) params.size else {

        params.filter { param -> !param.proFeature } .size

    }

    var thumbnail : Bitmap? = null

    override fun equals(other: Any?): Boolean {
        return other is Texture && name == other.name
    }

    override fun hashCode(): Int {
        return loopSF.hashCode()
    }

}