package com.selfsimilartech.fractaleye

import android.content.res.Resources
import android.graphics.Bitmap
import android.util.Range

class Texture (

        var id              : Int = -1,
        var hasCustomId     : Boolean = false,
        val nameId          : Int = -1,
        override var name            : String = "",
        val init            : String = "",
        var loop            : String = "",
        var final           : String = "",
        val isAverage       : Boolean = false,
        val usesFirstDelta  : Boolean = false,
        val usesSecondDelta : Boolean = false,
        val params          : ParamList = ParamList(),
        val auto            : Boolean = false,
        val radius          : Float = 1e2f,
        val frequency       : Float? = null,
        val hasRawOutput    : Boolean = false,
        override val goldFeature     : Boolean = false,
        val devFeature      : Boolean = false,
        val displayName     : Int = nameId,
        val usesAccent      : Boolean = false,
        val usesDensity     : Boolean = false,
        override var isFavorite      : Boolean = false

) : Customizable, Goldable {



    class ParamListPreset(val list: List<RealParam?> = listOf()) {

        constructor(vararg p: RealParam?) : this(p.toList())

        override fun toString(): String {
            return list.joinToString(separator = ", ") { "(u: ${it?.u})" }
        }

    }

    class ParamList(val list: List<RealParam> = listOf()) {

        constructor(vararg p: RealParam) : this(p.toList())

        val size = list.size

        fun setFrom(newList: ParamList) {
            list.forEachIndexed { i, p ->
                newList.list.find { it.nameId == p.nameId } ?.let { p.setFrom(it) }
            }
        }
        fun setFrom(newParams: ParamListPreset) {

            list.forEachIndexed { i, p ->
                val newParam = newParams.list.getOrNull(i)
                if (newParam != null) p.setFrom(newParam)
            }

        }

        fun reset() {
            list.forEach { it.reset() }
        }

        fun clone() : ParamListPreset {
            return ParamListPreset(
                    List(list.size) { i -> list[i].clone() }
            )
        }

    }


    companion object {

        var CUSTOM_IMAGE_COUNT = 0
        val defaultImages = listOf(
                R.drawable.flower,
                R.drawable.pocketwatch,
                R.drawable.snowflake
        )
        val customImages = mutableListOf<String>()

        var nextId = 0
            get() { return field++ }

        val emptyFavorite = Texture(name = "Empty Favorite")
        val emptyCustom = Texture(name = "Empty Custom")
        val absoluteEscape = Texture(
                R.string.empty
        )
        val escape = Texture(
                id = nextId,
                nameId = R.string.escape,
                final = "iteration_final(n)",
                usesDensity = true
        )
        val converge = Texture(
                id = nextId,
                nameId = R.string.converge,
                final = "iteration_final(n)",
                radius = 1e-8f
        )
        val convergeSmooth = Texture(
                id = nextId,
                nameId = R.string.converge_smooth,
                loop = "converge_smooth_loop(sum, z, z1);",
                final = "converge_smooth_final(sum, n)"
        )
        val escapeSmooth = Texture(
                id = nextId,
                nameId = R.string.escape_smooth,
                final = "escape_smooth_final(n, z, z1, textureType)",
                radius = 1e2f,
                usesDensity = true
        )
        val distanceEstimation = Texture(
                id = nextId,
                nameId = R.string.distance_est,
                final = "dist_estim_final(modsqrz, alpha)",
                usesFirstDelta = true,
                auto = true,
                radius = 1e2f,
                usesDensity = true
        )
        val outline = Texture(
                id = nextId,
                nameId = R.string.distance_est_abs,
                final = "outline_final(modsqrz, alpha)",
                usesFirstDelta = true,
                usesAccent = true,
                auto = true,
                params = ParamList(listOf(RealParam(R.string.size, 0.25, Range(0.000001, 3.0)))),
                radius = 3e1f,
                devFeature = true
        )
        val normalMap1 = Texture(
                id = nextId,
                nameId = R.string.normal1,
                final = "normal_map1_final(z, alpha)",
                usesFirstDelta = true,
                radius = 1e1f,
                frequency = 1f
        )
        val normalMap2 = Texture(
                id = nextId,
                nameId = R.string.normal2,
                final = "normal_map2_final(modsqrz, z, alpha, beta)",
                usesFirstDelta = true,
                usesSecondDelta = true,
                radius = 4e0f,
                frequency = 1f
        )
        val triangleIneqAvgInt = Texture(
                id = nextId,
                nameId = R.string.triangle_ineq_avg_int,
                loop = "triangle_ineq_avg_int_loop(sum, sum1, n, modc, z1, z2);",
                isAverage = true,
                radius = 1e6f,
                displayName = R.string.triangle_ineq_avg
        )
        val triangleIneqAvgFloat = Texture(
                id = nextId,
                nameId = R.string.triangle_ineq_avg_float,
                loop = "triangle_ineq_avg_float_loop(sum, sum1, n, modc, z1, z2);",
                isAverage = true,
                radius = 1e6f,
                displayName = R.string.triangle_ineq_avg
        )
        val curvatureAvg = Texture(
                id = nextId,
                nameId = R.string.curvature_avg,
                loop = "curvature_avg_loop(sum, sum1, n, z, z1, z2);",
                isAverage = true,
                params = ParamList(listOf(
                        RealParam(R.string.width, 1.0, Range(0.075, 10.0), goldFeature = true),
                        RealParam(R.string.bend, 0.0, Range(-5.0, 5.0), goldFeature = true)
                )),
                radius = 1e8f
        )
        val stripeAvg = Texture(
                id = nextId,
                nameId = R.string.stripe_avg,
                loop = "stripe_avg_loop(sum, sum1, z);",
                isAverage = true,
                params = ParamList(listOf(
                        RealParam(R.string.frequency,  1.0,  Range(1.0, 8.0), goldFeature = true),
                        RealParam(R.string.phase,      0.0,  Range(0.0, 360.0), toRadians = true),
                        RealParam(R.string.width,      1.0,  Range(0.075, 30.0),                  goldFeature = true)
                )),
                radius = 1e6f
        )
        val orbitTrapLine = Texture(
                id = nextId,
                nameId = R.string.orbit_trap_line,
                init = "float minDist = R;",
                loop = "orbit_trap_line_loop(z, minDist);",
                final = "minDist",
                params = ParamList(listOf(
                        RealParam(R.string.shift, 0.0, Range(-10.0, 10.0), goldFeature = true),
                        RealParam(R.string.rotate, 0.0, Range(0.0, 180.0), toRadians = true, goldFeature = true)
                ))
        )
        val orbitTrapCirc = Texture(
                id = nextId,
                nameId = R.string.orbit_trap_circ,
                init = "float minDist = R;",
                loop = "orbit_trap_circ_loop(z, minDist);",
                final = "minDist",
                params = ParamList(listOf(
                        ComplexParam(R.string.center),
                        RealParam(R.string.size, 0.0, Range(0.0, 2.0))
                )),
                radius = 1e2f,
                goldFeature = true
        )
        val orbitTrapBox = Texture(
                id = nextId,
                nameId = R.string.orbit_trap_box,
                init = "float minDist = R;",
                loop = "orbit_trap_box_loop(z, minDist);",
                final = "minDist",
                params = ParamList(listOf(
                        ComplexParam(R.string.center),
                        ComplexParam(R.string.size, 1.0, 1.0)
                )),
                goldFeature = true
        )
        val orbitTrapCircPuncture = Texture(
                R.string.mandelbrot,
                init = "float minDist = R; float angle = 0.0;",
                loop = "orbit_trap_circ_puncture_loop(z, minDist, angle);",
                final = "angle",
                params = ParamList(listOf(
                        ComplexParam(R.string.center),
                        RealParam(u = 1.0)
                ))
        )
        val overlayAvg = Texture(
                id = nextId,
                nameId = R.string.overlay_avg,
                loop = "overlay_avg_loop(sum, sum1, z);",
                isAverage = true,
                params = ParamList(listOf(
                        RealParam(R.string.sharpness, 0.45, Range(0.4, 0.5))
                )),
                radius = 1e2f
        )
        val exponentialSmoothing = Texture(
                id = nextId,
                nameId = R.string.exponential_smooth,
                loop = "exp_smoothing_loop(sum, modsqrz);",
                final = "exp_smoothing_final(sum)",
                radius = 1e2f,
                usesDensity = true
        )
        val angularMomentum = Texture(
                id = nextId,
                nameId = R.string.angular_momentum,
                loop = "angular_momentum_loop(sum, sum1, z, z1, z2);",
                isAverage = true,
                radius = 1e10f,
                goldFeature = true
        )
        val umbrella = Texture(
                id = nextId,
                nameId = R.string.umbrella,
                loop = "umbrella_loop(sum, sum1, z, z1);",
                isAverage = true,
                params = ParamList(listOf(RealParam(R.string.frequency, 4.0, Range(0.5, 8.0)))),
                goldFeature = true
        )
        val umbrellaInverse = Texture(
                id = nextId,
                nameId = R.string.inverse_umbrella,
                loop = "umbrella_inverse_loop(sum, sum1, z, z1);",
                isAverage = true,
                params = ParamList(listOf(RealParam(R.string.frequency, 1.618, Range(0.5, 5.0)))),
                goldFeature = true
        )
        val exitAngle = Texture(
                nameId = android.R.string.untitled,
                // loop = "exit_angle_loop(sum, sum1, z, z1);",
                final = "exit_angle_final(z, z1)",
                params = ParamList(listOf(
                        RealParam(R.string.power, u = 1.0, uRange = Range(0.0, 10.0))
                )),
                // isAverage = true,
                goldFeature = true
        )
        val angle = Texture(
                nameId = R.string.angle,
                final = "angle_final(c)",
                params = ParamList(listOf(ComplexParam(R.string.center))),
                goldFeature = true
        )
        val orbitTrapImageOver = Texture(
                id = nextId,
                nameId = R.string.image_over,
                init = "vec4 color = vec4(0.0); ivec2 imageSize = textureSize(image, 0); float imageRatio = float(imageSize.y)/float(imageSize.x);",
                loop = "orbit_trap_image_over_loop(z, color, imageRatio);",
                final = "orbit_trap_image_over_final(color)",
                params = ParamList(listOf(
                        RealParam(R.string.size, 1.0, Range(0.0, 5.0), goldFeature = true),
                        ComplexParam(R.string.center, u = -1.2, v = 1.2),
                        RealParam(R.string.rotate, 0.0, Range(0.0, 360.0), toRadians = true, goldFeature = true)
                )),
                hasRawOutput = true
        )
        val precisionTest = Texture(
                nameId = R.string.precision,
                final = escapeSmooth.final,
                params = ParamList(listOf(
                        RealParam(u = -45.0, uRange = Range(-45.0, -10.0)),  // psuedo-zero exponent
                        RealParam(u = 13.0, uRange = Range(1.0, 15.0))       // split exponent
                ))
        )
        val starLens = Texture(
                id = nextId,
                nameId = R.string.star_lens,
                isAverage = true,
                loop = "star_lens_loop(sum, sum1, z);",
                params = ParamList(listOf(
                        RealParam(R.string.scale, u = 0.5, uRange = Range(0.1, 5.0)),
                        RealParam(R.string.rotate, u = 0.0, uRange = Range(0.0, 90.0), toRadians = true),
                        ComplexParam(R.string.shift),
                        RealParam(R.string.smooth, u = 4.0, uRange = Range(1.0, 8.0))
                )),
                goldFeature = true
        )
        val discLens = Texture(
                id = nextId,
                nameId = R.string.disc_lens,
                isAverage = true,
                loop = "disc_lens_loop(sum, sum1, z);",
                params = ParamList(listOf(
                        RealParam(R.string.scale, u = 1.0, uRange = Range(0.1, 5.0)),
                        ComplexParam(R.string.shift, goldFeature = true),
                        RealParam(R.string.smooth, u = 4.0, uRange = Range(1.0, 8.0), goldFeature = true)
                ))
        )
        val sineLens = Texture(
                id = nextId,
                nameId = R.string.sin_lens,
                isAverage = true,
                loop = "sine_lens_loop(sum, sum1, z);",
                params = ParamList(listOf(
                        RealParam(R.string.scale, u = 1.0, uRange = Range(0.1, 5.0)),
                        RealParam(R.string.rotate, u = 0.0, uRange = Range(0.0, 180.0), toRadians = true),
                        ComplexParam(R.string.shift),
                        RealParam(R.string.smooth, u = 4.0, uRange = Range(1.0, 8.0))
                )),
                goldFeature = true
        )
        val fieldLines = Texture(
                id = nextId,
                nameId = R.string.field_lines,
                usesFirstDelta = true,
                final = "field_lines_final(n, z, z1, alpha, textureType)",
                params = ParamList(listOf(
                        RealParam(R.string.count, u = 11.0, uRange = Range(2.0, 15.0)),
                        RealParam(R.string.size, u = 0.5, uRange = Range(0.1, 0.75)),
                        RealParam(R.string.phase, u = 0.0, uRange = Range(0.0, 1.0))
                )),
                goldFeature = true
        )
        val fieldLines2 = Texture(
                nameId = R.string.field_lines2,
                final = "field_lines2_final(z, z1, textureType)",
                goldFeature = true
        )
        val escapeWithOutline = Texture(
                id = nextId,
                nameId = R.string.escape_with_distance,
                usesFirstDelta = true,
                final = "escape_smooth_dist_estim_final(n, z, z1, modsqrz, alpha, textureType)",
                params = ParamList(listOf(RealParam(R.string.size, 0.25, Range(0.00001, 3.0)))),
                usesDensity = true,
                usesAccent = true,
                auto = true,
                goldFeature = true
        )
        val orbitTrapImageUnder = Texture(
                id = nextId,
                nameId = R.string.image_under,
                init = orbitTrapImageOver.init,
                loop = "orbit_trap_image_under_loop(z, color, imageRatio);",
                final = "orbit_trap_image_under_final(color)",
                params = ParamList(listOf(
                        RealParam(R.string.size, 1.0, Range(0.0, 5.0), goldFeature = true),
                        ComplexParam(R.string.center, u = -1.2, v = 1.2),
                        RealParam(R.string.rotate, 0.0, Range(0.0, 360.0), toRadians = true, goldFeature = true)
                )),
                hasRawOutput = true
        )
        val highlightIteration = Texture(
                id = nextId,
                nameId = R.string.texture_mode_out,
                final = "highlight_iter_final(n)",
                params = ParamList(listOf(
                        RealParam(R.string.power, 255.0, Range(0.0, 500.0), discrete = true)
                ))
        )
        val kleinianDistance = Texture(
                id = nextId,
                nameId = R.string.distance_est_abs,
                usesFirstDelta = true,
                final = "kleinian_dist_final(z, alpha, t)",
                params = ParamList(listOf(RealParam(R.string.size, 0.75, Range(0.00001, 3.0)))),
                usesAccent = true,
                goldFeature = true
        )
        val kleinianBinary = Texture(
                id = nextId,
                nameId = R.string.binary,
                final = "kleinian_ab_final(z, t)"
        )
        val harrissTest = Texture(
                id = nextId,
                nameId = R.string.harriss_test,
                final = "harriss_test_final(z, n)",
                params = ParamList(listOf(RealParam(u = 0.5, uRange = Range(0.0, 5.0)))),
                devFeature = true
        )
        val importanceGradientNumeric = Texture(
            id = nextId,
            nameId = R.string.importance_gradient_numeric,
            init = "vec2 z_dx = vec2(0.0); vec2 z_dy = vec2(0.0); float h = 0.0; float h_dx = 0.0; float h_dy = 0.0;",
            loop = "importance_grad_numeric_loop(n, z, z_dx, z_dy, c, h, h_dx, h_dy);",
            final = "importance_grad_numeric_final(h, h_dx, h_dy)",
            params = ParamList(listOf(
                ComplexParam(),
                RealParam(u = 1.0, uRange = Range(0.0, 10.0)),
                RealParam(u = 1.0, uRange = Range(0.0, 100.0))
            )),
            devFeature = true
        )
        val importance = Texture(
            id = nextId,
            nameId = R.string.importance,
            init = "float h = 0.0;",
            loop = "importance_loop(z, h);",
            final = "importance_final(textureType, h, z)",
            params = ParamList(listOf(
                ComplexParam(),
                RealParam(u = 1.0, uRange = Range(0.0, 10.0)),
                RealParam(u = 1.0, uRange = Range(0.0, 100.0))
            )),
            devFeature = true
        )
        val importanceGradientAnalytic = Texture(
            id = nextId,
            nameId = R.string.importance_gradient_analytic,
            init = "vec2 grad = vec2(0.0);",
            loop = "importance_grad_analytic_loop(n, z, z1, alpha, grad);",
            final = "importance_grad_analytic_final(z, alpha, grad)",
            params = ParamList(listOf(
                ComplexParam(),
                RealParam(u = 1.0, uRange = Range(0.0, 10.0)),
                RealParam(u = 1.0, uRange = Range(0.0, 100.0))
            )),
            devFeature = true
        )



        val all = mutableListOf(

            escape,
            escapeSmooth,
            converge,
            convergeSmooth,
            exponentialSmoothing,
            umbrella,
            umbrellaInverse,
            angularMomentum,
            outline,
            distanceEstimation,
            escapeWithOutline,
            triangleIneqAvgInt,
            triangleIneqAvgFloat,
            curvatureAvg,
            stripeAvg,
            overlayAvg,
            fieldLines,
            discLens,
            starLens,
            sineLens,
            orbitTrapLine,
            orbitTrapCirc,
            orbitTrapBox,
            orbitTrapImageOver,
            orbitTrapImageUnder,
            normalMap1,
            normalMap2,
            kleinianDistance,
            kleinianBinary,
            importance,
            importanceGradientNumeric,
            importanceGradientAnalytic,
            harrissTest,

        ).filter { BuildConfig.DEV_VERSION || !it.devFeature }

        val mandelbrot = all.minus(listOf(
                converge,
                convergeSmooth,
                kleinianDistance,
                kleinianBinary
        ))

        val divergent = all.minus(listOf(
                converge,
                convergeSmooth,
                triangleIneqAvgInt,
                triangleIneqAvgFloat,
                distanceEstimation,
                normalMap1,
                normalMap2,
                kleinianDistance,
                kleinianBinary
        ))

        val convergent = mutableListOf(
                converge,
                convergeSmooth,
                exponentialSmoothing
        )

        val kleinianCompat = listOf(
                kleinianDistance,
                kleinianBinary
        ).plus(divergent.minus(listOf(escapeSmooth, escapeWithOutline)))

        val custom = listOf<Texture>()

    }


    init {
        if (isAverage) this.final = "avg_final(sum, sum1, n, z, z1, textureType)"
        if (!hasCustomId && id != -1) id = Integer.MAX_VALUE - id
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

        params.list.forEach { p ->
            if (p.nameId != -1) p.name = res.getString(p.nameId)
        }

        thumbnail = Bitmap.createBitmap(
                Resolution.THUMB.w,
                Resolution.THUMB.w,
                Bitmap.Config.RGB_565
        )

    }

    override fun getName(localizedResource: Resources): String {
        return if (isCustom()) name else localizedResource.getString(nameId)
    }

    var activeParam = if (params.size != 0) params.list[0] else RealParam(R.string.empty)

    override var thumbnail : Bitmap? = null

    fun reset() {
        params.reset()
    }

    fun generateStarredKey(usResources: Resources) : String {
        return "Texture${usResources.getString(nameId).replace(" ", "")}Starred"
    }

    override fun equals(other: Any?): Boolean {
        return other is Texture && id == other.id
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun isCustom() : Boolean = hasCustomId

}