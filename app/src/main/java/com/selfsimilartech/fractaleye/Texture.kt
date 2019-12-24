package com.selfsimilartech.fractaleye

import android.graphics.Bitmap
import android.util.Log
import kotlin.math.roundToInt
import kotlin.reflect.typeOf

class Texture (
        val name            : String,
        val initSF          : Int = R.string.empty,
        val loopSF          : Int = R.string.empty,
        val finalSF         : Int = R.string.empty,
        val initDF          : Int = R.string.empty,
        val loopDF          : Int = R.string.empty,
        val finalDF         : Int = R.string.empty,
        params              : List<Param> = listOf(),
        val auto            : Boolean = false,
        val bailoutRadius   : Float? = null,
        val frequency       : Float? = null
) {

    class Param (
            val name: String = "",
            val min: Double = 0.0,
            val max: Double = 1.0,
            q: Double = 0.0,
            val discrete: Boolean = false
    ) {


        private val qInit = q
        var q = qInit

        val interval = max - min
        var progress = 0.0
            set(value) {
                field = value
                q = (1.0 - progress)*min + progress*max
                Log.d("TEXTURE", "q progress set to $value")
                Log.d("TEXTURE", "q set to $q")
            }


        override fun toString(): String {
            return if (discrete) "%d".format(q.roundToInt()) else "%.3f".format(q)
        }

        fun reset() {
            q = qInit
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
                finalSF = R.string.escape_final,
                finalDF = R.string.escape_final
        )
        val escapeSmooth = Texture(
                name = "Escape Time Smooth",
                finalSF = R.string.mandelbrot_smooth_final_sf,
                finalDF = R.string.mandelbrot_smooth_final_df,
                bailoutRadius = 1e2f
        )
        val distanceEstimation = Texture(
                name = "Distance Estimation",
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
                name = "Normal Map 2",
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
                R.string.stripe_init,
                R.string.stripe_loop_sf,
                R.string.stripe_final_sf,
                R.string.stripe_init,
                R.string.stripe_loop_df,
                R.string.stripe_final_df,
                listOf(Param("Stripe Density", 1.0, 10.0, 2.0, true)),
                bailoutRadius = 1e6f
        )
        val orbitTrap = Texture(
                "Orbit Trap",
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
                R.string.overlay_init,
                R.string.overlay_loop_sf,
                R.string.overlay_final_sf,
                R.string.overlay_init,
                R.string.overlay_loop_df,
                R.string.overlay_final_df,
                listOf(Param("Sharpness", 0.4, 0.5, 0.49)),
                bailoutRadius = 1e2f
        )
        val exponentialSmoothing = Texture(
                "Exponential Smoothing",
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
                exponentialSmoothing,
                distanceEstimation,
                triangleIneqAvg,
                curvatureAvg,
                stripeAvg,
                orbitTrap,
                normalMap1,
                normalMap2,
                overlayAvg
        )

    }

    val numParamsInUse = params.size
    val params = List(NUM_TEXTURE_PARAMS) { i: Int ->
        if (i < params.size) params[i]
        else Param()
    }

    var thumbnail : Bitmap? = null

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