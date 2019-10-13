package com.selfsimilartech.fractaleye

import android.content.res.Resources
import android.util.Range

class Texture (
        val name         : String,
        val initSF       : String = "",
        val loopSF       : String = "",
        val finalSF      : String = "",
        val initDF       : String = "",
        val loopDF       : String = "",
        val finalDF      : String = "",
        val initParams   : List<Triple<String, Range<Double>, Double>> = listOf()
) {

    companion object {

        val empty                 = {
            Texture(name = "Empty")
        }
        val absoluteEscape        = { res: Resources -> Texture(
                "Absolute Escape",
                finalSF = res.getString(R.string.absolute_escape_final),
                finalDF = res.getString(R.string.absolute_escape_final)
        )}
        val escape                = { res: Resources -> Texture(
                name = "Escape Time",
                finalSF = res.getString(R.string.escape_final),
                finalDF = res.getString(R.string.escape_final) )
        }
        val escapeSmooth          = { res: Resources -> Texture(
                name = "Escape Time Smooth",
                finalSF = res.getString(R.string.mandelbrot_smooth_final_sf),
                finalDF = res.getString(R.string.mandelbrot_smooth_final_df2) )
        }
        val distanceEstimation    = { res: Resources -> Texture(
                name = "Distance Estimation",
                initSF = res.getString(R.string.mandelbrot_dist_init_sf),
                loopSF = res.getString(R.string.mandelbrot_dist_loop_sf),
                finalSF = res.getString(R.string.mandelbrot_dist_final_sf),
                initDF = res.getString(R.string.mandelbrot_dist_init_df),
                loopDF = res.getString(R.string.mandelbrot_dist_loop_df),
                finalDF = res.getString(R.string.mandelbrot_dist_final_df) )
        }
        val normalMap1            = { res: Resources ->
            Texture(
                    name = "Normal Map 1",
                    initSF = res.getString(R.string.mandelbrot_normal1_init_sf),
                    loopSF = res.getString(R.string.mandelbrot_normal1_loop_sf),
                    finalSF = res.getString(R.string.mandelbrot_normal1_final_sf),
                    initDF = res.getString(R.string.mandelbrot_normal1_init_df),
                    loopDF = res.getString(R.string.mandelbrot_normal1_loop_df),
                    finalDF = res.getString(R.string.mandelbrot_normal1_final_df))
        }
        val normalMap2            = { res: Resources -> Texture(
                name = "Normal Map 2",
                initSF = res.getString(R.string.mandelbrot_normal2_init_sf),
                loopSF = res.getString(R.string.mandelbrot_normal2_loop_sf),
                finalSF = res.getString(R.string.mandelbrot_normal2_final_sf),
                initDF = res.getString(R.string.mandelbrot_normal2_init_df),
                loopDF = res.getString(R.string.mandelbrot_normal2_loop_df),
                finalDF = res.getString(R.string.mandelbrot_normal2_final_df) )
        }
        val distanceGradient      = { res: Resources -> Texture(
                name = "Distance Gradient",
                initSF = res.getString(R.string.mandelbrot_distgrad_init_sf),
                loopSF = res.getString(R.string.mandelbrot_distgrad_loop_sf),
                finalSF = res.getString(R.string.mandelbrot_distgrad_final_sf),
                initDF = res.getString(R.string.mandelbrot_distgrad_init_df),
                loopDF = res.getString(R.string.mandelbrot_distgrad_loop_df),
                finalDF = res.getString(R.string.mandelbrot_distgrad_final_df)
        ) }
        val lighting              = { res: Resources -> Texture(
                "Lighting",
                res.getString(R.string.mandelbrot_light_init_sf),
                res.getString(R.string.mandelbrot_light_loop_sf),
                res.getString(R.string.mandelbrot_light_final_sf),
                res.getString(R.string.mandelbrot_light_init_df),
                res.getString(R.string.mandelbrot_light_loop_df),
                res.getString(R.string.mandelbrot_light_final_df)
        )}
        val escapeSmoothLight     = {
            res: Resources -> escapeSmooth(res).add(lighting(res))
        }
        val triangleIneqAvg       = { res: Resources -> Texture(
                "Triangle Inequality Average",
                res.getString(R.string.triangle_init_sf),
                res.getString(R.string.triangle_loop_sf),
                res.getString(R.string.triangle_final_sf),
                res.getString(R.string.triangle_init_df),
                res.getString(R.string.triangle_loop_df),
                res.getString(R.string.triangle_final_df) )
        }
        val curvatureAvg          = { res: Resources -> Texture(
                "Curvature Average",
                res.getString(R.string.curvature_init),
                res.getString(R.string.curvature_loop_sf),
                res.getString(R.string.curvature_final_sf),
                res.getString(R.string.curvature_init),
                res.getString(R.string.curvature_loop_df),
                res.getString(R.string.curvature_final_df))
        }
        val stripeAvg             = { res: Resources -> Texture(
                "Stripe Average",
                res.getString(R.string.stripe_init),
                res.getString(R.string.stripe_loop_sf),
                res.getString(R.string.stripe_final_sf),
                res.getString(R.string.stripe_init),
                res.getString(R.string.stripe_loop_df),
                res.getString(R.string.stripe_final_df),
                listOf(Triple("Density", Range(0.0, 15.0), 5.0)) )
        }
        val orbitTrap             = { res: Resources -> Texture(
                "Orbit Trap",
                res.getString(R.string.orbittrap_init),
                res.getString(R.string.orbittrap_loop_sf),
                res.getString(R.string.orbittrap_final_radius),
                res.getString(R.string.orbittrap_init),
                res.getString(R.string.orbittrap_loop_df),
                res.getString(R.string.orbittrap_final_radius))
        }
        val overlayAvg            = { res: Resources -> Texture(
                "Overlay Average",
                res.getString(R.string.overlay_init),
                res.getString(R.string.overlay_loop_sf),
                res.getString(R.string.overlay_final_sf),
                res.getString(R.string.overlay_init),
                res.getString(R.string.overlay_loop_df),
                res.getString(R.string.overlay_final_df),
                listOf(Triple("Sharpness", Range(0.0, 0.5), 0.495)) )
        }
        val exponentialSmoothing  = { res: Resources -> Texture(
                "Exponential Smoothing",
                initSF = res.getString(R.string.exponential_smooth_init),
                loopSF = res.getString(R.string.exponential_smooth_loop_sf),
                finalSF = res.getString(R.string.exponential_smooth_final),
                initDF = res.getString(R.string.exponential_smooth_init),
                loopDF = res.getString(R.string.exponential_smooth_loop_df),
                finalDF = res.getString(R.string.exponential_smooth_final)
        ) }

        val arbitrary           = mapOf(
                "Escape Time"                  to  escape               ,
                "Curvature Average"            to  curvatureAvg         ,
                "Stripe Average"               to  stripeAvg            ,
                "Overlay Average"              to  overlayAvg           ,
                "Orbit Trap"                   to  orbitTrap            ,
                "Exponential Smoothing"        to  exponentialSmoothing
        )
        val all                 = mapOf(
                "Escape Time"                  to  escape               ,
                "Escape Time Smooth"           to  escapeSmooth         ,
                "Distance Estimation"          to  distanceEstimation   ,
                "Normal Map 1"                 to  normalMap1           ,
                "Normal Map 2"                 to  normalMap2           ,
                "Triangle Inequality Average"  to  triangleIneqAvg      ,
                "Curvature Average"            to  curvatureAvg         ,
                "Stripe Average"               to  stripeAvg            ,
                "Overlay Average"              to  overlayAvg           ,
                "Orbit Trap"                   to  orbitTrap            ,
                "Exponential Smoothing"        to  exponentialSmoothing
        )

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