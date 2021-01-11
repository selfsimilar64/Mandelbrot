package com.selfsimilartech.fractaleye

import android.content.res.Resources
import android.graphics.Bitmap
import android.util.Range

class Texture (

        val nameId          : Int = -1,
        var name            : String = "",
        val init            : String = "",
        var loop            : String = "",
        var final           : String = "",
        val isAverage       : Boolean = false,
        val usesFirstDelta  : Boolean = false,
        val usesSecondDelta : Boolean = false,
        val params          : List<RealParam> = listOf(),
        val bins            : RealParam = RealParam(nameId = R.string.bins, u = 20.0, uRange = Range(0.0, 20.0), goldFeature = true),
        val auto            : Boolean = false,
        val radius          : Float = 1e2f,
        val frequency       : Float? = null,
        val goldFeature      : Boolean = false,
        val displayName     : Int = nameId,
        var isFavorite      : Boolean = false

) {


    companion object {

        val emptyFavorite = Texture(name = "Empty Favorite")
        val emptyCustom = Texture(name = "Empty Custom")
        val absoluteEscape = Texture(
                R.string.empty
        )
        val escape = Texture(
                R.string.escape,
                final = "iteration_final(n)"
        )
        val converge = Texture(
                R.string.converge,
                final = "iteration_final(n)",
                radius = 1e-8f
        )
        val escapeSmooth = Texture(
                R.string.escape_smooth,
                final = "escape_smooth_final(n, z, z1, textureIn)",
                radius = 1e2f
        )
        val distanceEstimation = Texture(
                R.string.distance_est,
                final = "dist_estim_final(modsqrz, alpha)",
                usesFirstDelta = true,
                auto = true,
                radius = 1e2f
        )
        val absoluteDistance = Texture(
                R.string.distance_est_abs,
                final = "dist_estim_abs_final(modsqrz, alpha)",
                usesFirstDelta = true,
                auto = true,
                params = listOf(RealParam(R.string.size, 1.0, Range(0.000001, 3.0))),
                radius = 3e1f,
                goldFeature = true
        )
        val normalMap1 = Texture(
                R.string.normal1,
                final = "normal_map1_final(z, alpha)",
                usesFirstDelta = true,
                radius = 1e1f,
                frequency = 1f
        )
        val normalMap2 = Texture(
                R.string.normal2,
                final = "normal_map2_final(modsqrz, z, alpha, beta)",
                usesFirstDelta = true,
                usesSecondDelta = true,
                radius = 1e2f,
                frequency = 1f
        )
        val triangleIneqAvgInt = Texture(
                R.string.triangle_ineq_avg_int,
                loop = "triangle_ineq_avg_int_loop(sum, sum1, n, modc, z1, z2);",
                isAverage = true,
                radius = 1e6f,
                displayName = R.string.triangle_ineq_avg
        )
        val triangleIneqAvgFloat = Texture(
                nameId = R.string.triangle_ineq_avg_float,
                loop = "triangle_ineq_avg_float_loop(sum, sum1, n, modc, z1, z2);",
                isAverage = true,
                radius = 1e6f,
                displayName = R.string.triangle_ineq_avg
        )
        val curvatureAvg = Texture(
                R.string.curvature_avg,
                loop = "curvature_avg_loop(sum, sum1, n, z, z1, z2);",
                isAverage = true,
                params = listOf(
                        RealParam(R.string.width, 1.0, Range(0.075, 10.0), goldFeature = true),
                        RealParam(R.string.bend, 0.0, Range(-5.0, 5.0), goldFeature = true)
                ),
                radius = 1e8f
        )
        val stripeAvg = Texture(
                R.string.stripe_avg,
                loop = "stripe_avg_loop(sum, sum1, z);",
                isAverage = true,
                params = listOf(
                        RealParam(R.string.frequency,  1.0,  Range(1.0, 8.0),                     goldFeature = true),
                        RealParam(R.string.phase,      0.0,  Range(0.0, 360.0), toRadians = true, goldFeature = true),
                        RealParam(R.string.width,      1.0,  Range(0.075, 30.0),                  goldFeature = true)
                ),
                radius = 1e6f
        )
        val orbitTrapLine = Texture(
                R.string.orbit_trap_line,
                init = "float minDist = R;",
                loop = "orbit_trap_line_loop(z, minDist);",
                final = "minDist",
                params = listOf(
                        RealParam(R.string.shift, 0.0, Range(-10.0, 10.0), goldFeature = true),
                        RealParam(R.string.rotate, 0.0, Range(0.0, 180.0), toRadians = true, goldFeature = true)
                )
        )
        val orbitTrapCirc = Texture(
                R.string.orbit_trap_circ,
                init = "float minDist = R;",
                loop = "orbit_trap_circ_loop(z, minDist);",
                final = "minDist",
                params = listOf(
                        ComplexParam(R.string.center),
                        RealParam(R.string.size, 0.0, Range(0.0, 2.0))
                ),
                radius = 1e2f,
                goldFeature = true
        )
        val orbitTrapBox = Texture(
                R.string.orbit_trap_box,
                init = "float minDist = R;",
                loop = "orbit_trap_box_loop(z, minDist);",
                final = "minDist",
                params = listOf(
                        ComplexParam(R.string.center),
                        ComplexParam(R.string.size, 1.0, 1.0)
                ),
                goldFeature = true
        )
        val orbitTrapCircPuncture = Texture(
                R.string.mandelbrot,
                init = "float minDist = R; float angle = 0.0;",
                loop = "orbit_trap_circ_puncture_loop(z, minDist, angle);",
                final = "angle",
                params = listOf(
                        ComplexParam(R.string.center),
                        RealParam(u = 1.0)
                )
        )
        val overlayAvg = Texture(
                R.string.overlay_avg,
                loop = "overlay_avg_loop(sum, sum1, z);",
                isAverage = true,
                params = listOf(
                        RealParam(R.string.sharpness, 0.45, Range(0.4, 0.5))
                ),
                radius = 1e2f
        )
        val exponentialSmoothing = Texture(
                R.string.exponential_smooth,
                loop = "exp_smoothing_loop(sum, modsqrz);",
                final = "exp_smoothing_final(sum)",
                radius = 1e2f
        )
        val angularMomentum = Texture(
                nameId = R.string.angular_momentum,
                loop = "angular_momentum_loop(sum, sum1, z, z1, z2);",
                isAverage = true,
                params = listOf(RealParam(R.string.alpha, 0.0, Range(0.0, 2.0*Math.PI), goldFeature = true)),
                radius = 1e10f,
                goldFeature = true
        )
        val umbrella = Texture(
                nameId = R.string.umbrella,
                loop = "umbrella_loop(sum, sum1, z, z1);",
                isAverage = true,
                params = listOf(RealParam(R.string.frequency, 4.0, Range(1.0, 8.0))),
                goldFeature = true
        )
        val umbrellaInverse = Texture(
                nameId = R.string.inverse_umbrella,
                loop = "umbrella_inverse_loop(sum, sum1, z, z1);",
                isAverage = true,
                params = listOf(RealParam(R.string.frequency, 4.0, Range(1.0, 5.0))),
                goldFeature = true
        )
        val exitAngle = Texture(
                nameId = R.string.texture_name,
                loop = "exit_angle_loop(sum, sum1, z, z1);",
                isAverage = true,
                goldFeature = true
        )
        val angle = Texture(
                nameId = R.string.angle,
                final = "angle_final(c)",
                params = listOf(ComplexParam(R.string.center)),
                goldFeature = true
        )
        val precisionTest = Texture(
                nameId = R.string.precision,
                final = escapeSmooth.final,
                params = listOf(
                        RealParam(u = -45.0, uRange = Range(-45.0, -10.0)),  // psuedo-zero exponent
                        RealParam(u = 13.0, uRange = Range(1.0, 15.0))       // split exponent
                )
        )



        val all = mutableListOf(
                umbrella,
                umbrellaInverse,
                angularMomentum,
                escape,
                escapeSmooth,
                converge,
                exponentialSmoothing,
                distanceEstimation,
                absoluteDistance,
                triangleIneqAvgInt,
                triangleIneqAvgFloat,
                curvatureAvg,
                stripeAvg,
                orbitTrapLine,
                orbitTrapCirc,
                orbitTrapBox,
                normalMap1,
                normalMap2,
                overlayAvg
        )

        val mandelbrot = all.minus(converge)

        val divergent = all.minus(listOf(
                converge,
                triangleIneqAvgInt,
                triangleIneqAvgFloat,
                distanceEstimation,
                normalMap1,
                normalMap2
        ))

        val convergent = mutableListOf(
                converge,
                exponentialSmoothing,
                orbitTrapLine,
                orbitTrapCirc,
                orbitTrapBox
        )

    }


    init {
        if (isAverage) this.final = "avg_final(sum, sum1, n, z, z1, textureIn)"
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

        params.forEach { p ->
            if (p.nameId != -1) p.name = res.getString(p.nameId)
        }

        thumbnail = Bitmap.createBitmap(
                Resolution.THUMB.w,
                Resolution.THUMB.w,
                Bitmap.Config.RGB_565
        )

    }

    var activeParam = if (params.isNotEmpty()) params[0] else RealParam(R.string.empty)

    var thumbnail : Bitmap? = null

    override fun equals(other: Any?): Boolean {
        return other is Texture && nameId == other.nameId
    }

    override fun hashCode(): Int {
        return loop.hashCode()
    }

}