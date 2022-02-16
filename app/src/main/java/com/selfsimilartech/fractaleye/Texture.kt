package com.selfsimilartech.fractaleye

import android.content.res.Resources
import android.graphics.Bitmap
import android.util.Log
import android.util.Range
import kotlin.math.pow

class Texture(

    var id                    : Int        = -1,
    var hasCustomId           : Boolean    = false,
    val nameId                : Int        = -1,
    override var name         : String     = "",
    val thumbnailId           : Int        = R.drawable.mandelbrot_icon,
    val init                  : String     = "",
    var loop                  : String     = "",
    var final                 : String     = "",
    val isAverage             : Boolean    = false,
    val isConvergentCompat    : Boolean    = false,
    val usesFirstDelta        : Boolean    = false,
    val usesSecondDelta       : Boolean    = false,
    params                    : ParamSet?  = null,
    val auto                  : Boolean    = false,
    val hasRawOutput          : Boolean    = false,
    override val goldFeature  : Boolean    = false,
    val devFeature            : Boolean    = false,
    val displayName           : Int        = nameId,
    val usesAccent            : Boolean    = false,
    val usesDensity           : Boolean    = false,
    override var isFavorite   : Boolean    = false

) : Customizable {


    class ParamSet(
        val list: ArrayList<RealParam> = arrayListOf(),
        escapeRadius: RealParam = RealParam(
            R.string.radius,
            R.drawable.radius,
            256.0,
            Range(1.0, 2.0.pow(30.0)),
            scale = RealParam.Scale.EXP,
            displayLinear = false
        ),
        convergeRadius: RealParam = RealParam(
            R.string.radius,
            R.drawable.radius,
            -256.0,
            Range(2.0.pow(-30.0), 1.0),
            scale = RealParam.Scale.EXP,
            displayLinear = false
        ),
        val convergent: Boolean = false
    ) {
        
        val radius = if (convergent) convergeRadius else escapeRadius

        var active : RealParam = radius

        fun initialize(res: Resources) {
            radius.name = res.getString(R.string.radius)
            list.forEach { p -> p.name = if (p.nameId != -1) res.getString(p.nameId) else "!!!" }
        }

        fun at(index: Int): RealParam = list[index]

        fun setFrom(newSet: ParamSet) {

            radius.setFrom(newSet.radius)
            list.forEachIndexed { i, p -> newSet.list.getOrNull(i)?.let { p.setFrom(it) } }

        }

        fun reset() {
            list.forEach { it.reset() }
            radius.reset()
        }

        fun clone() : ParamSet {
            return if (convergent) ParamSet(
                ArrayList(List(list.size) { i -> list[i].clone() }),
                convergeRadius = radius.clone()
            ) else ParamSet(
                ArrayList(List(list.size) { i -> list[i].clone() }),
                escapeRadius = radius.clone()
            )
        }

    }


    companion object {

        var CUSTOM_IMAGE_COUNT = 0
        val defaultImages = arrayListOf(
            R.drawable.flower,
            R.drawable.pocketwatch,
            R.drawable.snowflake
        )
        val customImages = mutableListOf<String>()

        var nextId = 0
            get() {
                return field++
            }

        val emptyFavorite = Texture(name = "Empty Favorite")
        val emptyCustom = Texture(name = "Empty Custom")
        val absoluteEscape = Texture(
            R.string.empty
        )
        val escape = Texture(
            id = nextId,
            nameId = R.string.escape,
            thumbnailId = R.drawable.escapetime_icon,
            final = "iteration_final(n)",
            usesDensity = true
        )
        val converge = Texture(
            id = nextId,
            nameId = R.string.converge,
            final = "iteration_final(n)",
            isConvergentCompat = true,
            params = ParamSet(convergent = true, convergeRadius = RealParam(1e-8))
        )
        val convergeSmooth = Texture(
            id = nextId,
            nameId = R.string.converge_smooth,
            loop = "converge_smooth_loop(sum, z, z1);",
            final = "converge_smooth_final(sum, n)",
            isConvergentCompat = true,
            params = ParamSet(convergent = true, convergeRadius = RealParam(1e-8))
        )
        val escapeSmooth = Texture(
            id = nextId,
            nameId = R.string.escape_smooth,
            thumbnailId = R.drawable.escapetime_smooth_icon,
            final = "escape_smooth_final(n, z, z1, textureType)",
            usesDensity = true
        )
        val distanceEstimation = Texture(
            id = nextId,
            nameId = R.string.distance_est,
            thumbnailId = R.drawable.distance_estimation_icon,
            final = "dist_estim_final(modsqrz, alpha)",
            usesFirstDelta = true,
            auto = true,
            usesDensity = true
        )
        val outline = Texture(
            id = nextId,
            nameId = R.string.distance_est_abs,
            thumbnailId = R.drawable.outline_icon,
            final = "outline_final(modsqrz, alpha)",
            usesFirstDelta = true,
            usesAccent = true,
            auto = true,
            params = ParamSet(
                arrayListOf(
                    RealParam(
                        R.string.width,
                        R.drawable.width,
                        0.25,
                        Range(0.000001, 3.0)
                    )
                ),
                escapeRadius = RealParam(32.0)
            ),
            devFeature = true
        )
        val normalMap1 = Texture(
            id = nextId,
            nameId = R.string.normal1,
            thumbnailId = R.drawable.normalmap1_icon,
            final = "normal_map1_final(z, alpha)",
            usesFirstDelta = true,
            params = ParamSet(escapeRadius = RealParam(16.0))
        )
        val normalMap2 = Texture(
            id = nextId,
            nameId = R.string.normal2,
            final = "normal_map2_final(modsqrz, z, alpha, beta)",
            usesFirstDelta = true,
            usesSecondDelta = true,
            params = ParamSet(escapeRadius = RealParam(16.0))
        )
        val triangleIneqAvgInt = Texture(
            id = nextId,
            nameId = R.string.triangle_ineq_avg_int,
            thumbnailId = R.drawable.triangle_ineq_icon,
            loop = "triangle_ineq_avg_int_loop(sum, sum1, n, modc, z1, z2);",
            isAverage = true,
            params = ParamSet(escapeRadius = RealParam(2.0.pow(20.0))),
            displayName = R.string.triangle_ineq_avg
        )
        val triangleIneqAvgFloat = Texture(
            id = nextId,
            nameId = R.string.triangle_ineq_avg_float,
            thumbnailId = R.drawable.triangle_ineq_icon,
            loop = "triangle_ineq_avg_float_loop(sum, sum1, n, modc, z1, z2);",
            isAverage = true,
            params = ParamSet(escapeRadius = RealParam(2.0.pow(20.0))),
            displayName = R.string.triangle_ineq_avg
        )
        val curvatureAvg = Texture(
            id = nextId,
            nameId = R.string.curvature_avg,
            thumbnailId = R.drawable.curvature_average_icon,
            loop = "curvature_avg_loop(sum, sum1, n, z, z1, z2);",
            isAverage = true,
            params = ParamSet(
                arrayListOf(
                    RealParam(
                        R.string.width,
                        R.drawable.width,
                        1.0,
                        Range(0.075, 10.0),
                        goldFeature = true
                    ),
                    RealParam(
                        R.string.bend,
                        R.drawable.curve,
                        0.0,
                        Range(-5.0, 5.0),
                        goldFeature = true
                    )
                ),
                escapeRadius = RealParam(2.0.pow(26.0))
            )
        )
        val stripeAvg = Texture(
            id = nextId,
            nameId = R.string.stripe_avg,
            thumbnailId = R.drawable.stripe_average_icon,
            loop = "stripe_avg_loop(sum, sum1, z);",
            isAverage = true,
            params = ParamSet(
                arrayListOf(
                    RealParam(
                        R.string.frequency,
                        R.drawable.frequency2,
                        1.0,
                        Range(1.0, 8.0),
                        goldFeature = true
                    ),
                    RealParam(
                        R.string.phase,
                        R.drawable.phase,
                        0.0,
                        Range(0.0, 360.0),
                        toRadians = true
                    ),
                    RealParam(
                        R.string.width,
                        R.drawable.width,
                        1.0,
                        Range(0.05, 30.0),
                        scale = RealParam.Scale.EXP,
                        goldFeature = true
                    )
                ),
                escapeRadius = RealParam(2.0.pow(20.0))
            )
        )
        val orbitTrapLine = Texture(
            id = nextId,
            nameId = R.string.orbit_trap_line,
            thumbnailId = R.drawable.orbittrap_line_icon,
            init = "float minDist = R;",
            loop = "orbit_trap_line_loop(z, minDist);",
            final = "minDist",
            params = ParamSet(
                arrayListOf(
                    RealParam(
                        R.string.spread,
                        R.drawable.spread,
                        -1.5,
                        Range(-10.0, 10.0),
                        goldFeature = true
                    ),
                    RealParam(
                        R.string.rotation,
                        R.drawable.rotate_left,
                        90.0,
                        Range(0.0, 180.0),
                        toRadians = true,
                        goldFeature = true
                    )
                )
            )
        )
        val orbitTrapCirc = Texture(
            id = nextId,
            nameId = R.string.orbit_trap_circ,
            thumbnailId = R.drawable.orbittrap_circle_icon,
            init = "float minDist = R;",
            loop = "orbit_trap_circ_loop(z, minDist);",
            final = "minDist",
            params = ParamSet(
                arrayListOf(
                    ComplexParam(R.string.position, R.drawable.param_position, u = -1.15, v = 1.0),
                    RealParam(R.string.size, R.drawable.size, 0.0, Range(0.0, 2.0))
                )
            ),
            goldFeature = true
        )
        val orbitTrapBox = Texture(
            id = nextId,
            nameId = R.string.orbit_trap_box,
            thumbnailId = R.drawable.orbittrap_square_icon,
            init = "float minDist = R;",
            loop = "orbit_trap_box_loop(z, minDist);",
            final = "minDist",
            params = ParamSet(
                arrayListOf(
                    ComplexParam(R.string.position, R.drawable.param_position, u = -1.5, v = 1.0),
                    ComplexParam(R.string.size, R.drawable.size, u = 0.25, v = 0.25)
                )
            ),
            goldFeature = true
        )
        val orbitTrapCircPuncture = Texture(
            R.string.mandelbrot,
            init = "float minDist = R; float angle = 0.0;",
            loop = "orbit_trap_circ_puncture_loop(z, minDist, angle);",
            final = "angle",
            params = ParamSet(
                arrayListOf(
                    ComplexParam(R.string.center),
                    RealParam(u = 1.0)
                )
            )
        )
        val overlayAvg = Texture(
            id = nextId,
            nameId = R.string.overlay_avg,
            thumbnailId = R.drawable.overlay_average_icon,
            loop = "overlay_avg_loop(sum, sum1, z);",
            isAverage = true,
            params = ParamSet(
                arrayListOf(
                    RealParam(R.string.sharpness, R.drawable.sharpness, 0.485, Range(0.48, 0.4999))
                )
            )
        )
        val exponentialSmoothing = Texture(
            id = nextId,
            nameId = R.string.exponential_smooth,
            thumbnailId = R.drawable.exponential_smoothing_icon,
            loop = "exp_smoothing_loop(sum, modsqrz);",
            final = "exp_smoothing_final(sum)",
            usesDensity = true
        )
        val angularMomentum = Texture(
            id = nextId,
            nameId = R.string.angular_momentum,
            thumbnailId = R.drawable.angular_momentum_icon,
            loop = "angular_momentum_loop(sum, sum1, z, z1, z2);",
            isAverage = true,
            params = ParamSet(escapeRadius = RealParam(2.0.pow(32.0))),
            goldFeature = true
        )
        val umbrella = Texture(
            id = nextId,
            nameId = R.string.umbrella,
            thumbnailId = R.drawable.umbrella_icon,
            loop = "umbrella_loop(sum, sum1, z, z1);",
            isAverage = true,
            params = ParamSet(
                arrayListOf(
                    RealParam(
                        R.string.frequency,
                        R.drawable.frequency2,
                        4.0,
                        Range(0.5, 8.0)
                    )
                )
            ),
            goldFeature = true
        )
        val umbrellaInverse = Texture(
            id = nextId,
            nameId = R.string.inverse_umbrella,
            thumbnailId = R.drawable.umbrella_inverse_icon,
            loop = "umbrella_inverse_loop(sum, sum1, z, z1);",
            isAverage = true,
            params = ParamSet(
                arrayListOf(
                    RealParam(
                        R.string.frequency,
                        R.drawable.frequency2,
                        1.618,
                        Range(0.5, 5.0)
                    )
                )
            ),
            goldFeature = true
        )
        val exitAngle = Texture(
            nameId = android.R.string.untitled,
            // loop = "exit_angle_loop(sum, sum1, z, z1);",
            final = "exit_angle_final(z, z1)",
            params = ParamSet(
                arrayListOf(
                    RealParam(R.string.exponent, R.drawable.exponent, u = 1.0, uRange = Range(0.0, 10.0))
                )
            ),
            // isAverage = true,
            goldFeature = true
        )
        val angle = Texture(
            nameId = R.string.angle,
            final = "angle_final(c)",
            params = ParamSet(arrayListOf(ComplexParam(R.string.center))),
            goldFeature = true
        )
        val orbitTrapImageOver = Texture(
            id = nextId,
            nameId = R.string.image_over,
            thumbnailId = R.drawable.image1_icon,
            init = "vec4 color = vec4(0.0); ivec2 imageSize = textureSize(image, 0); float imageRatio = float(imageSize.y)/float(imageSize.x);",
            loop = "orbit_trap_image_over_loop(z, color, imageRatio);",
            final = "orbit_trap_image_over_final(color)",
            params = ParamSet(
                arrayListOf(
                    RealParam(
                        R.string.size,
                        R.drawable.size,
                        1.0,
                        Range(0.0, 5.0),
                        goldFeature = true
                    ),
                    ComplexParam(
                        R.string.position,
                        R.drawable.param_position,
                        u = -1.2, v = 1.2
                    ),
                    RealParam(
                        R.string.rotation,
                        R.drawable.rotate_left,
                        0.0,
                        Range(0.0, 360.0),
                        toRadians = true,
                        goldFeature = true
                    )
                )
            ),
            hasRawOutput = true
        )
        val precisionTest = Texture(
            nameId = R.string.precision,
            final = escapeSmooth.final,
            params = ParamSet(
                arrayListOf(
                    RealParam(u = -45.0, uRange = Range(-45.0, -10.0)),  // psuedo-zero exponent
                    RealParam(u = 13.0, uRange = Range(1.0, 15.0))       // split exponent
                )
            )
        )
        val starLens = Texture(
            id = nextId,
            nameId = R.string.star_lens,
            thumbnailId = R.drawable.starlens_icon,
            isAverage = true,
            loop = "star_lens_loop(sum, sum1, z);",
            params = ParamSet(
                arrayListOf(
                    RealParam(
                        R.string.size,
                        R.drawable.size,
                        u = 0.15,
                        uRange = Range(0.1, 5.0)
                    ),
                    RealParam(
                        R.string.rotation,
                        R.drawable.rotate_left,
                        u = 0.0,
                        uRange = Range(0.0, 90.0),
                        toRadians = true
                    ),
                    ComplexParam(R.string.position, R.drawable.param_position, u = -1.15, v = 1.25),
                    RealParam(
                        R.string.sharpness,
                        R.drawable.sharpness,
                        u = 3.0,
                        uRange = Range(1.0, 8.0)
                    )
                )
            ),
            goldFeature = true
        )
        val discLens = Texture(
            id = nextId,
            nameId = R.string.disc_lens,
            thumbnailId = R.drawable.disclens_icon,
            isAverage = true,
            loop = "disc_lens_loop(sum, sum1, z);",
            params = ParamSet(
                arrayListOf(
                    RealParam(
                        R.string.size,
                        R.drawable.size,
                        u = 0.75,
                        uRange = Range(0.1, 5.0)
                    ),
                    ComplexParam(
                        R.string.position,
                        R.drawable.param_position,
                        u = -1.35,
                        v = 1.15,
                        goldFeature = true
                    ),
                    RealParam(
                        R.string.sharpness,
                        R.drawable.sharpness,
                        u = 4.0,
                        uRange = Range(1.0, 8.0),
                        goldFeature = true
                    )
                )
            ),
        )
        val sineLens = Texture(
            id = nextId,
            nameId = R.string.sin_lens,
            thumbnailId = R.drawable.sinelens_icon,
            isAverage = true,
            loop = "sine_lens_loop(sum, sum1, z);",
            params = ParamSet(
                arrayListOf(
                    RealParam(
                        R.string.size,
                        R.drawable.size,
                        u = 0.4,
                        uRange = Range(0.1, 5.0)
                    ),
                    RealParam(
                        R.string.rotation,
                        R.drawable.rotate_left,
                        u = -90.0,
                        uRange = Range(0.0, 180.0),
                        toRadians = true
                    ),
                    ComplexParam(
                        R.string.position,
                        R.drawable.param_position
                    ),
                    RealParam(
                        R.string.sharpness,
                        R.drawable.sharpness,
                        u = 3.0,
                        uRange = Range(1.0, 8.0)
                    )
                )
            ),
            goldFeature = true
        )
        val fieldLines = Texture(
            id = nextId,
            nameId = R.string.field_lines,
            thumbnailId = R.drawable.fieldlines_icon,
            usesFirstDelta = true,
            final = "field_lines_final(n, z, z1, alpha, textureType)",
            params = ParamSet(
                arrayListOf(
                    RealParam(
                        R.string.frequency,
                        R.drawable.frequency2,
                        u = 11.0,
                        uRange = Range(2.0, 15.0)
                    ),
                    RealParam(R.string.width, R.drawable.width, u = 0.5, uRange = Range(0.1, 0.75)),
                    RealParam(R.string.phase, R.drawable.phase, u = 0.0, uRange = Range(0.0, 1.0))
                )
            ),
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
            thumbnailId = R.drawable.escapetime_outline_icon,
            usesFirstDelta = true,
            final = "escape_smooth_dist_estim_final(n, z, z1, modsqrz, alpha, textureType)",
            params = ParamSet(
                arrayListOf(
                    RealParam(
                        R.string.width,
                        R.drawable.width,
                        0.25,
                        Range(0.01, 3.0),
                        scale = RealParam.Scale.EXP
                    )
                )
            ),
            usesDensity = true,
            usesAccent = true,
            auto = true,
            goldFeature = true
        )
        val orbitTrapImageUnder = Texture(
            id = nextId,
            nameId = R.string.image_under,
            thumbnailId = R.drawable.image2_icon,
            init = orbitTrapImageOver.init,
            loop = "orbit_trap_image_under_loop(z, color, imageRatio);",
            final = "orbit_trap_image_under_final(color)",
            params = ParamSet(
                arrayListOf(
                    RealParam(
                        R.string.size,
                        R.drawable.size,
                        1.0,
                        Range(0.0, 5.0),
                        goldFeature = true
                    ),
                    ComplexParam(
                        R.string.position,
                        R.drawable.param_position,
                        u = -1.2, v = 1.2
                    ),
                    RealParam(
                        R.string.rotation,
                        R.drawable.rotate_left,
                        0.0,
                        Range(0.0, 360.0),
                        toRadians = true,
                        goldFeature = true
                    )
                )
            ),
            hasRawOutput = true
        )
        val highlightIteration = Texture(
            id = nextId,
            nameId = R.string.texture_mode_out,
            final = "highlight_iter_final(n)",
            params = ParamSet(
                arrayListOf(
                    RealParam(
                        R.string.power,
                        u = 255.0,
                        uRange = Range(0.0, 500.0),
                        isDiscrete = true
                    )
                )
            )
        )
        val kleinianDistance = Texture(
            id = nextId,
            nameId = R.string.distance_est_abs,
            usesFirstDelta = true,
            final = "kleinian_dist_final(z, alpha, t)",
            params = ParamSet(
                arrayListOf(
                    RealParam(
                        R.string.width,
                        R.drawable.width,
                        0.75,
                        Range(0.00001, 3.0)
                    )
                )
            ),
            usesAccent = true,
            goldFeature = true
        )
        val kleinianBinary = Texture(
            id = nextId,
            nameId = R.string.binary,
            final = "kleinian_ab_final(z, t)"
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
            kleinianBinary

        ).filter { BuildConfig.DEV_VERSION || !it.devFeature }

        val mandelbrot = all.minus(
            arrayListOf(
                converge,
                convergeSmooth,
                kleinianDistance,
                kleinianBinary
            ).toSet()
        )

        val divergent = all.minus(
            arrayListOf(
                converge,
                convergeSmooth,
                triangleIneqAvgInt,
                triangleIneqAvgFloat,
                distanceEstimation,
                normalMap1,
                normalMap2,
                kleinianDistance,
                kleinianBinary
            ).toSet()
        )

        val convergent = mutableListOf(
            converge,
            convergeSmooth
        )

        val kleinianCompat = arrayListOf(
            kleinianDistance,
            kleinianBinary
        ).plus(divergent.minus(arrayListOf(escapeSmooth, escapeWithOutline).toSet()))

        val kaliCompat = arrayListOf(escape, escapeSmooth, exponentialSmoothing, orbitTrapCirc)

        val custom = listOf<Texture>()

    }


    val params = ParamSet(convergent = isConvergentCompat).also { set ->
        if (params != null) {
            set.list.addAll(params.list)
            set.radius.setFrom(params.radius)
        }
    }

    override var thumbnail: Bitmap? = null



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

        params.initialize(res)

        thumbnail = Bitmap.createBitmap(
                Resolution.THUMB.w,
                Resolution.THUMB.w,
                Bitmap.Config.RGB_565
        )

    }

    override fun getName(localizedResource: Resources): String {
        return if (isCustom()) name else localizedResource.getString(nameId)
    }

    fun reset() {
        params.reset()
    }

    fun generateStarredKey(usResources: Resources): String {
        return "Texture${usResources.getString(nameId).replace(" ", "")}Starred"
    }

    override fun equals(other: Any?): Boolean {
        return other is Texture && id == other.id
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun isCustom(): Boolean = hasCustomId

}