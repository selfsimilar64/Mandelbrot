package com.selfsimilartech.fractaleye

import android.util.Range

class Texture (
        val name            : String,
        val icon            : Int = R.drawable.mandelbrot_icon,
        val initSF          : Int = R.string.empty,
        val loopSF          : Int = R.string.empty,
        val finalSF         : Int = R.string.empty,
        val initDF          : Int = R.string.empty,
        val loopDF          : Int = R.string.empty,
        val finalDF         : Int = R.string.empty,
        params              : List<Param> = listOf(),
        val auto            : Boolean = false,
        val bailoutRadius   : Float? = null
) {

    class Param (
        val name:   String = "",
        val range:  Range<Double> = Range(0.0, 1.0),
        t: Double = 0.0
    ) {

        private val tInit = t
        var t = tInit
            set (value) {
                if (field != value) {
                    field = value
                    // f.updateTextureParamEditText(key[1].toString().toInt())
                    // fsv.r.renderToTex = true
                }
            }

        fun reset() {
            t = tInit
        }

    }

    companion object {

        val empty = Texture(name = "Empty")
        val absoluteEscape = Texture(
                "Absolute Escape",
                finalSF = R.string.absolute_escape_final,
                finalDF = R.string.absolute_escape_final
        )
        val escape = Texture(
                name = "Escape Time",
                icon = R.drawable.escape,
                finalSF = R.string.escape_final,
                finalDF = R.string.escape_final
        )
        val escapeSmooth = Texture(
                name = "Escape Time Smooth",
                icon = R.drawable.escape_smooth,
                finalSF = R.string.mandelbrot_smooth_final_sf,
                finalDF = R.string.mandelbrot_smooth_final_df2,
                bailoutRadius = 1e2f
        )
        val distanceEstimation = Texture(
                name = "Distance Estimation",
                icon = R.drawable.distance_est,
                initSF = R.string.mandelbrot_dist_init_sf,
                loopSF = R.string.mandelbrot_dist_loop_sf,
                finalSF = R.string.mandelbrot_dist_final_sf,
                initDF = R.string.mandelbrot_dist_init_df,
                loopDF = R.string.mandelbrot_dist_loop_df,
                finalDF = R.string.mandelbrot_dist_final_df,
                auto = true,
                bailoutRadius = 1e2f
        )
        val normalMap1 = Texture(
                name = "Normal Map 1",
                icon = R.drawable.normal_map_1,
                initSF = R.string.mandelbrot_normal1_init_sf,
                loopSF = R.string.mandelbrot_normal1_loop_sf,
                finalSF = R.string.mandelbrot_normal1_final_sf,
                initDF = R.string.mandelbrot_normal1_init_df,
                loopDF = R.string.mandelbrot_normal1_loop_df,
                finalDF = R.string.mandelbrot_normal1_final_df,
                bailoutRadius = 1e2f
        )
        val normalMap2 = Texture(
                name = "Normal Map 2",
                icon = R.drawable.normal_map_2,
                initSF = R.string.mandelbrot_normal2_init_sf,
                loopSF = R.string.mandelbrot_normal2_loop_sf,
                finalSF = R.string.mandelbrot_normal2_final_sf,
                initDF = R.string.mandelbrot_normal2_init_df,
                loopDF = R.string.mandelbrot_normal2_loop_df,
                finalDF = R.string.mandelbrot_normal2_final_df,
                bailoutRadius = 1e2f
        )
        val distanceGradient = Texture(
                name = "Distance Gradient",
                initSF = R.string.mandelbrot_distgrad_init_sf,
                loopSF = R.string.mandelbrot_distgrad_loop_sf,
                finalSF = R.string.mandelbrot_distgrad_final_sf,
                initDF = R.string.mandelbrot_distgrad_init_df,
                loopDF = R.string.mandelbrot_distgrad_loop_df,
                finalDF = R.string.mandelbrot_distgrad_final_df
        )
        val lighting = Texture(
                "Lighting",
                R.string.mandelbrot_light_init_sf,
                R.string.mandelbrot_light_loop_sf,
                R.string.mandelbrot_light_final_sf,
                R.string.mandelbrot_light_init_df,
                R.string.mandelbrot_light_loop_df,
                R.string.mandelbrot_light_final_df
        )
        val triangleIneqAvg = Texture(
                "Triangle Inequality Average",
                R.drawable.triangle_ineq_avg,
                R.string.triangle_init_sf,
                R.string.triangle_loop_sf,
                R.string.triangle_final_sf,
                R.string.triangle_init_df,
                R.string.triangle_loop_df,
                R.string.triangle_final_df,
                bailoutRadius = 1e6f
        )
        val curvatureAvg = Texture(
                "Curvature Average",
                R.drawable.curvature_avg,
                R.string.curvature_init,
                R.string.curvature_loop_sf,
                R.string.curvature_final_sf,
                R.string.curvature_init,
                R.string.curvature_loop_df,
                R.string.curvature_final_df,
                bailoutRadius = 1e12f
        )
        val stripeAvg = Texture(
                "Stripe Average",
                R.drawable.stripe_avg,
                R.string.stripe_init,
                R.string.stripe_loop_sf,
                R.string.stripe_final_sf,
                R.string.stripe_init,
                R.string.stripe_loop_df,
                R.string.stripe_final_df,
                listOf(Param("Density", Range(0.0, 5.0), 1.0)),
                bailoutRadius = 1e6f
        )
        val orbitTrap = Texture(
                "Orbit Trap",
                R.drawable.orbit_trap_min_x,
                R.string.orbittrap_init,
                R.string.orbittrap_loop_sf,
                R.string.orbittrap_final_radius,
                R.string.orbittrap_init,
                R.string.orbittrap_loop_df,
                R.string.orbittrap_final_radius,
                bailoutRadius = 1e2f
        )
        val overlayAvg = Texture(
                "Overlay Average",
                R.drawable.overlay_avg,
                R.string.overlay_init,
                R.string.overlay_loop_sf,
                R.string.overlay_final_sf,
                R.string.overlay_init,
                R.string.overlay_loop_df,
                R.string.overlay_final_df,
                listOf(Param("Sharpness", Range(0.0, 0.5), 0.495)),
                bailoutRadius = 1e2f
        )
        val exponentialSmoothing = Texture(
                "Exponential Smoothing",
                R.drawable.exp_smooth,
                initSF = R.string.exponential_smooth_init,
                loopSF = R.string.exponential_smooth_loop_sf,
                finalSF = R.string.exponential_smooth_final,
                initDF = R.string.exponential_smooth_init,
                loopDF = R.string.exponential_smooth_loop_df,
                finalDF = R.string.exponential_smooth_final,
                bailoutRadius = 5f
        )
        val arbitrary = arrayListOf(
                escape,
                curvatureAvg,
                stripeAvg,
                overlayAvg,
                orbitTrap,
                exponentialSmoothing
        )
        val all = arrayListOf(
                escape,
                escapeSmooth,
                distanceEstimation,
                normalMap1,
                normalMap2,
                triangleIneqAvg,
                curvatureAvg,
                stripeAvg,
                overlayAvg,
                orbitTrap,
                exponentialSmoothing
        )

    }

    val numParamsInUse = params.size
    val params = List(NUM_TEXTURE_PARAMS) { i: Int ->
        if (i < params.size) params[i]
        else Param()
    }


    fun add(alg : Texture) : Texture {
        return Texture(
                "$name with ${alg.name}",
                initSF + alg.initSF,
                loopSF + alg.loopSF,
                finalSF + alg.finalSF,
                initDF + alg.initDF,
                loopDF + alg.loopDF,
                finalDF + alg.finalDF
        )
    }
    override fun toString() : String { return name }
    override fun equals(other: Any?): Boolean {
        return other is Texture && name == other.name
    }
    override fun hashCode(): Int {
        return name.hashCode()
    }

}