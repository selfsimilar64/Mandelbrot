package com.selfsimilartech.fractaleye

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.TransitionDrawable
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.design.widget.TabLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.util.Log
import android.util.Range
import android.view.*
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.math.*


const val SPLIT = 8193.0
const val NUM_MAP_PARAMS = 4
const val NUM_TEXTURE_PARAMS = 2
const val WRITE_STORAGE_REQUEST_CODE = 0
//const val PLUS_UNICODE = '\u002B'
//const val MINUS_UNICODE = '\u2212'



data class DualDouble (
        var hi : Double,
        var lo : Double
) {

    override fun toString() : String {
        return "{$hi + $lo}"
    }

    private fun quickTwoSum(a: Double, b: Double) : DualDouble {
        val s = a + b
        val e = b - (s - a)
        return DualDouble(s, e)
    }

    private fun twoSum(a: Double, b: Double) : DualDouble {
        val s = a + b
        val v = s - a
        val e = a - (s - v) + (b - v)
        return DualDouble(s, e)
    }

    private fun split(a: Double): DualDouble {
        val t = a * SPLIT
        val aHi = t - (t - a)
        val aLo = a - aHi
        return DualDouble(aHi, aLo)
    }

    private fun twoProd(a: Double, b: Double) : DualDouble {
        val p = a * b
        val aS = split(a)
        val bS = split(b)
        val err = aS.hi * bS.hi - p + aS.hi * bS.lo + aS.lo * bS.hi + aS.lo * bS.lo
        return DualDouble(p, err)
    }

    operator fun unaryMinus() : DualDouble {
        return DualDouble(-hi, -lo)
    }

    operator fun plus(b: DualDouble) : DualDouble {
        var s = twoSum(hi, b.hi)
        val t = twoSum(lo, b.lo)
        s.lo += t.hi
        s = quickTwoSum(s.hi, s.lo)
        s.lo += t.lo
        s = quickTwoSum(s.hi, s.lo)
        return s
    }

    operator fun minus(b: DualDouble) : DualDouble {
        return plus(b.unaryMinus())
    }

    operator fun times(b: DualDouble) : DualDouble {
        var p = twoProd(hi, b.hi)
        p.lo += hi * b.lo
        p.lo += lo * b.hi
        p = quickTwoSum(p.hi, p.lo)
        return p
    }

    operator fun div(b: DualDouble) : DualDouble {

        val xn = 1.0 / b.hi
        val yn = hi * xn
        val diff = minus(b*DualDouble(yn, 0.0))
        val prod = twoProd(xn, diff.hi)
        return DualDouble(yn, 0.0) + prod

    }

}


class ColorPalette (
        val name: String,
        private val colors: List<FloatArray>
) {

    companion object {

        private val RED         = floatArrayOf( 1.0f,  0.0f,  0.0f )
        private val GREEN       = floatArrayOf( 0.0f,  1.0f,  0.0f )
        private val BLUE        = floatArrayOf( 0.0f,  0.0f,  1.0f )
        private val YELLOW      = floatArrayOf( 1.0f,  1.0f,  0.0f )
        private val MAGENTA     = floatArrayOf( 1.0f,  0.0f,  1.0f )
        private val CYAN        = floatArrayOf( 0.0f,  1.0f,  1.0f )
        private val BLACK       = floatArrayOf( 0.0f,  0.0f,  0.0f )
        private val WHITE       = floatArrayOf( 1.0f,  1.0f,  1.0f )

        private val PURPLE      = floatArrayOf( 0.3f,   0.0f,   0.5f  )
        private val PINK        = floatArrayOf( 1.0f,   0.3f,   0.4f  )
        private val DARKBLUE1   = floatArrayOf( 0.0f,   0.15f,  0.25f )
        private val DARKBLUE2   = floatArrayOf( 0.11f,  0.188f, 0.35f )
        private val ORANGE1     = floatArrayOf( 1.0f,   0.6f,   0.0f  )
        private val ORAGNE2     = floatArrayOf( 0.9f,   0.4f,   0.2f  )
        private val TURQUOISE   = floatArrayOf( 0.25f,  0.87f,  0.82f )
        private val TUSK        = floatArrayOf( 0.93f,  0.8f,   0.73f )
        private val YELLOWISH   = floatArrayOf( 1.0f,   0.95f,  0.75f )
        private val GRASS       = floatArrayOf( 0.31f,  0.53f,  0.45f )
        private val SOFTGREEN   = floatArrayOf( 0.6f,   0.82f,  0.9f  )
        private val DEEPRED     = floatArrayOf( 0.8f,   0.0f,   0.3f  )
        private val MAROON      = floatArrayOf( 0.4f,   0.1f,   0.2f  )

        private val PURPLE2     = floatArrayOf( 115f/255f, 45f/255f, 90f/255f )
        private val MINT        = floatArrayOf( 189f/255f, 1f, 209f/255f )
        private val TANGERINE   = floatArrayOf( 246f/255f, 170f/255f, 26f/255f )


        val bw      = ColorPalette("Yin Yang", listOf(
                BLACK,
                WHITE
        ))
        val beach   = ColorPalette("Beach",  listOf(
                YELLOWISH,
                DARKBLUE1,
                BLACK,
                TURQUOISE,
                TUSK
        ))
        val p1      = ColorPalette("P1",     listOf(
                WHITE,
                PURPLE,
                BLACK,
                DEEPRED,
                WHITE
        ))
        val p3      = ColorPalette("P3",     listOf(
                floatArrayOf(0.0f, 0.1f, 0.2f),
                DARKBLUE1,
                WHITE,
                floatArrayOf(0.1f, 0.7f, 0.8f),
                floatArrayOf(0.85f, 0.5f, 0.2f)
        ))
        val p4      = ColorPalette("Vascular",     listOf(
                YELLOWISH,
                DARKBLUE2,
                BLACK,
                PURPLE.mult(0.7f),
                DEEPRED
        ))
        val p5      = ColorPalette("Flora",     listOf(
                YELLOWISH.mult(1.1f),
                MAGENTA.mult(0.4f),
                WHITE,
                DARKBLUE1,
                BLACK,
                DARKBLUE1
        ))
        val royal   = ColorPalette("Royal",  listOf(
                YELLOWISH,
                DARKBLUE1,
                SOFTGREEN.mult(0.5f),
                PURPLE.mult(0.35f),
                MAROON
        ))
        val p8      = ColorPalette("Groovy", listOf(
                floatArrayOf(75.0f, 115.0f, 115.0f).mult(1.0f/255.0f),
                floatArrayOf(255.0f, 170.0f, 28.0f).mult(1.0f/255.0f),
                floatArrayOf(255.0f, 118.0f, 111.0f).mult(1.0f/255.0f),
                BLACK
        ))
        val canyon  = ColorPalette("Canyon", listOf(
                DEEPRED.mult(0.4f),
                floatArrayOf(255.0f, 115.0f, 110.0f).mult(1.0f/255.0f),
                floatArrayOf(1.0f, 0.8f, 0.6f),
                floatArrayOf(75.0f, 130.0f, 115.0f).mult(1.0f/255.0f),
                PURPLE.mult(0.3f)
        ))
        val anubis = ColorPalette("Anubis",  listOf(
                BLACK,
                PURPLE2,
                MINT,
                YELLOWISH,
                floatArrayOf(1f, 1f, 0.6f),
                floatArrayOf(240f, 120f, 10f).mult(1f/255f)
        ))
        val all     = mapOf(
                bw.name      to  bw,
                beach.name   to  beach,
                p4.name      to  p4,
                p5.name      to  p5,
                royal.name   to  royal,
                p8.name      to  p8,
                canyon.name  to  canyon,
                anubis.name  to  anubis
        )

    }

    private val palette = colors.plus(colors[0])
    val size = palette.size
    val flatPalette = FloatArray(palette.size * 3) {i: Int ->
        val a = floor(i / 3.0f).toInt()
        val b = i % 3
        palette[a][b]
    }

    fun invert() : ColorPalette {
        return ColorPalette(name, List(size) { i: Int -> colors[i].invert() })
    }

    override fun toString() : String { return name }
    override fun equals(other: Any?): Boolean {
        return other is ColorPalette && other.name == name
    }
    override fun hashCode(): Int { return name.hashCode() }

}
class ComplexMap (
        val name            : String,
        val katex           : String             = "$$",
        val katexSize       : Float              = 4.5f,
        val conditionalSF   : String?            = "",
        val initSF          : String?            = "",
        val loopSF          : String?            = "",
        val finalSF         : String?            = "",
        val conditionalDF   : String?            = "",
        val initDF          : String?            = "",
        val loopDF          : String?            = "",
        val finalDF         : String?            = "",
        val initCoords      : DoubleArray        = doubleArrayOf(0.0, 0.0),
        val initScale       : Double             = 1.0,
        val initParams      : List<DoubleArray>  = listOf(),
        val initZ           : DoubleArray        = doubleArrayOf(0.0, 0.0),
        val initJuliaMode   : Boolean            = false,
        val initBailout     : Float              = 1e5f
) {
    
    companion object {
        
        val empty           = { ComplexMap("Empty", "$$") }
        val mandelbrot          = { res: Resources -> ComplexMap(
                "Mandelbrot",
                katex = res.getString(R.string.mandelbrot_katex),
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.mandelbrot_loop_sf),
                conditionalDF = res.getString(R.string.escape_df),
                loopDF = res.getString(R.string.mandelbrot_loop_df),
                initCoords = doubleArrayOf(-0.75, 0.0),
                initScale = 3.5
        )}
        val mandelbrotPower     = { res: Resources -> ComplexMap(
                "Mandelbrot Power",
                katex = res.getString(R.string.mandelbrotcpow_katex),
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.mandelbrotcpow_loop_sf),
                initScale = 3.5,
                initParams = listOf(doubleArrayOf(3.0, 0.0))
        )}
        val mandelbrotDualPower = { res: Resources -> ComplexMap(
                "Dual Power",
                katex = res.getString(R.string.dualpow_katex),
                conditionalSF = res.getString(R.string.escape_sf),
                initSF = res.getString(R.string.dualpow_init_sf),
                loopSF = res.getString(R.string.dualpow_loop_sf),
                initScale = 3.0,
                initZ = doubleArrayOf(1.0, 0.0),
                initParams = listOf(doubleArrayOf(2.0, -2.0))
        )}
        val mandelbar           = { res: Resources -> ComplexMap(
                "Mandelbar",
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.mandelbar_loop_sf),
                initScale = 3.5
        )}
        val logistic            = { res: Resources -> ComplexMap(
                "Logistic",
                katex = res.getString(R.string.logistic_katex),
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.logistic_loop_sf),
                conditionalDF = res.getString(R.string.escape_df),
                loopDF = res.getString(R.string.logistic_loop_df),
                initScale = 3.5,
                initZ = doubleArrayOf(0.5, 0.0)
        ) }
        val burningShip         = { res: Resources -> ComplexMap(
                "Burning Ship",
                katex = res.getString(R.string.burningship_katex),
                katexSize = 3.5f,
                conditionalSF = res.getString(R.string.escape_sf),
                initSF = res.getString(R.string.burningship_init_sf),
                loopSF = res.getString(R.string.burningship_loop_sf),
                conditionalDF = res.getString(R.string.escape_df),
                initDF = res.getString(R.string.burningship_init_df),
                loopDF = res.getString(R.string.burningship_loop_df),
                initCoords = doubleArrayOf(-0.35, 0.25),
                initScale = 3.5
        ) }
        val sine                = { res: Resources -> ComplexMap(
                "Sine",
                katex = res.getString(R.string.sine_katex),
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.sine_loop_sf),
                conditionalDF = res.getString(R.string.escape_df),
                loopDF = res.getString(R.string.sine_loop_df),
                initScale = 3.5
        )}
        val sine1               = { res: Resources -> ComplexMap(
                "Sine 1",
                katex = res.getString(R.string.sine1_katex),
                katexSize = 5f,
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.sine1_loop_sf),
                initScale = 3.5,
                initParams = listOf(doubleArrayOf(0.31960705187983646, 0.0)),
                initZ = doubleArrayOf(1.0, 0.0)
        ) }
        val sine2               = { res: Resources -> ComplexMap(
                "Sine 2",
                katex = res.getString(R.string.sine2_katex),
                katexSize = 5f,
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.sine2_loop_sf),
                conditionalDF = res.getString(R.string.escape_df),
                loopDF = res.getString(R.string.sine2_loop_df),
                initScale = 3.5,
                initBailout = 1e1f,
                initParams = listOf(doubleArrayOf(-0.26282883851642613, 2.042520182493586E-6)),
                initZ = doubleArrayOf(1.0, 0.0)
        )}
        val horseshoeCrab       = { res: Resources -> ComplexMap(
                "Horseshoe Crab",
                katex = res.getString(R.string.horseshoecrab_katex),
                katexSize = 5f,
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.horseshoecrab_loop_sf),
                conditionalDF = res.getString(R.string.escape_df),
                loopDF = res.getString(R.string.horseshoecrab_loop_df),
                initScale = 5.0,
                initParams = listOf(doubleArrayOf(sqrt(2.0), 0.0)),
                initZ = doubleArrayOf(1.0, 0.0)
        )}
        val newton2             = { res: Resources -> ComplexMap(
                "Newton 2",
                conditionalSF = res.getString(R.string.converge_sf),
                loopSF = res.getString(R.string.newton2_loop_sf),
                initScale = 3.5,
                initParams = listOf(
                        doubleArrayOf(1.0, 1.0),
                        doubleArrayOf(-1.0, -1.0),
                        doubleArrayOf(2.0, -0.5)
                ),
                initJuliaMode = true
        ) }
        val persianRug          = { res: Resources -> ComplexMap(
                "Persian Rug",
                katex = res.getString(R.string.persianrug_katex),
                katexSize = 4f,
                initSF = res.getString(R.string.persianrug_init_sf),
                conditionalSF = res.getString(R.string.escape_sf),
                loopSF = res.getString(R.string.persianrug_loop_sf),
                initScale = 1.5,
                initParams = listOf(doubleArrayOf(0.642, 0.0)),
                initBailout = 1e1f
        )}
        val kleinian            = { res: Resources -> ComplexMap(
                "Kleinian",
                conditionalSF = res.getString(R.string.escape_sf),
                initSF = res.getString(R.string.kleinian_init_sf),
                loopSF = res.getString(R.string.kleinian_loop_sf),
                initScale = 1.1,
                initCoords = doubleArrayOf(0.0, -0.5),
                initParams = listOf(
                    doubleArrayOf(2.0, 0.0),
                    doubleArrayOf(0.0, -1.0)
                ),
                initJuliaMode = true
        )}
        val test                = { res: Resources -> ComplexMap(
                "Test",
                conditionalSF = res.getString(R.string.escape_sf),
                initSF = res.getString(R.string.test_init_sf),
                loopSF = res.getString(R.string.test_loop_sf),
                initScale = 3.5,
                initParams = listOf(
                    doubleArrayOf(1.0, 1.0),
                    doubleArrayOf(1.0, 1.0)
                )
        )}
        val all         = mapOf(
            "Mandelbrot"            to  mandelbrot,
            "Mandelbrot Power"      to  mandelbrotPower,
            "Mandelbrot Dual Power" to  mandelbrotDualPower,
            "Logistic"              to  logistic,
            "Burning Ship"          to  burningShip,
            "Persian Rug"           to  persianRug,
            "Sine"                  to  sine,
            "Sine 2"                to  sine2,
            "Horseshoe Crab"        to  horseshoeCrab,
            "Kleinian"              to  kleinian
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
class TextureAlgorithm (
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

        val empty               = {
            TextureAlgorithm(name = "Empty")
        }
        val escape              = { res: Resources -> TextureAlgorithm(
                name = "Escape Time",
                finalSF = res.getString(R.string.escape_final),
                finalDF = res.getString(R.string.escape_final) )
        }
        val escapeSmooth        = { res: Resources -> TextureAlgorithm(
                name = "Escape Time Smooth",
                finalSF = res.getString(R.string.mandelbrot_smooth_final_sf),
                finalDF = res.getString(R.string.mandelbrot_smooth_final_df) )
        }
        val lighting            = { res: Resources -> TextureAlgorithm(
                "Lighting",
                res.getString(R.string.mandelbrot_light_init_sf),
                res.getString(R.string.mandelbrot_light_loop_sf),
                res.getString(R.string.mandelbrot_light_final),
                res.getString(R.string.mandelbrot_light_init_df),
                res.getString(R.string.mandelbrot_light_loop_df),
                res.getString(R.string.mandelbrot_light_final)
        )}
        val escapeSmoothLight   = {
            res: Resources -> escapeSmooth(res).add(lighting(res))
        }
        val triangleIneqAvg     = { res: Resources -> TextureAlgorithm(
                "Triangle Inequality Average",
                res.getString(R.string.triangle_init_sf),
                res.getString(R.string.triangle_loop_sf),
                res.getString(R.string.triangle_final_sf),
                res.getString(R.string.triangle_init_df),
                res.getString(R.string.triangle_loop_df),
                res.getString(R.string.triangle_final_df) )
        }
        val curvatureAvg        = { res: Resources -> TextureAlgorithm(
                "Curvature Average",
                res.getString(R.string.curvature_init),
                res.getString(R.string.curvature_loop_sf),
                res.getString(R.string.curvature_final_sf),
                res.getString(R.string.curvature_init),
                res.getString(R.string.curvature_loop_df),
                res.getString(R.string.curvature_final_df))
        }
        val stripeAvg           = { res: Resources -> TextureAlgorithm(
                "Stripe Average",
                res.getString(R.string.stripe_init),
                res.getString(R.string.stripe_loop_sf),
                res.getString(R.string.stripe_final_sf),
                res.getString(R.string.stripe_init),
                res.getString(R.string.stripe_loop_df),
                res.getString(R.string.stripe_final_df),
                listOf(Triple("Density", Range(0.0, 15.0), 5.0)) )
        }
        val orbitTrap           = { res: Resources -> TextureAlgorithm(
                "Orbit Trap",
                res.getString(R.string.orbittrap_init),
                res.getString(R.string.orbittrap_loop_sf),
                res.getString(R.string.orbittrap_final_radius),
                res.getString(R.string.orbittrap_init),
                res.getString(R.string.orbittrap_loop_df),
                res.getString(R.string.orbittrap_final_radius))
        }
        val overlayAvg          = { res: Resources -> TextureAlgorithm(
                "Overlay Average",
                res.getString(R.string.overlay_init),
                res.getString(R.string.overlay_loop_sf),
                res.getString(R.string.overlay_final_sf),
                res.getString(R.string.overlay_init),
                res.getString(R.string.overlay_loop_df),
                res.getString(R.string.overlay_final_df),
                listOf(Triple("Sharpness", Range(0.0, 0.5), 0.495)) )
        }
        val all                 = mapOf(
            "Escape Time"                  to  escape               ,
            "Escape Time Smooth"           to  escapeSmooth         ,
            "Triangle Inequality Average"  to  triangleIneqAvg      ,
            "Curvature Average"            to  curvatureAvg         ,
            "Stripe Average"               to  stripeAvg            ,
            "Orbit Trap"                   to  orbitTrap            ,
            "Overlay Average"              to  overlayAvg
        )

    }

    fun add(alg : TextureAlgorithm) : TextureAlgorithm {
        return TextureAlgorithm(
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
        return other is TextureAlgorithm && name == other.name
    }
    override fun hashCode(): Int {
        return name.hashCode()
    }

}


enum class Precision { SINGLE, DUAL, QUAD, AUTO }
enum class Reaction { POSITION, COLOR, P1, P2, P3, P4 }
enum class Resolution { LOW, MED, HIGH }


class FractalConfig (val params : MutableMap<String, Any>) {

    val map                 = { params["map"]              as  ComplexMap        }
    val p1                  = { params["p1"]               as  DoubleArray       }
    val p2                  = { params["p2"]               as  DoubleArray       }
    val p3                  = { params["p3"]               as  DoubleArray       }
    val p4                  = { params["p4"]               as  DoubleArray       }
    val texture             = { params["texture"]          as  TextureAlgorithm  }
    val q1                  = { params["q1"]               as  Double            }
    val q2                  = { params["q2"]               as  Double            }
    val juliaMode           = { params["juliaMode"]        as  Boolean           }
    val paramSensitivity    = { params["paramSensitivity"] as  Double            }
    val coords              = { params["coords"]           as  DoubleArray       }
    val savedCoords         = { params["savedCoords"]      as  DoubleArray       }
    val scale               = { params["scale"]            as  DoubleArray       }
    val savedScale          = { params["savedScale"]       as  DoubleArray       }
    val maxIter             = { params["maxIter"]          as  Int               }
    val bailoutRadius       = { params["bailoutRadius"]    as  Float             }
    val palette             = { params["palette"]          as  ColorPalette      }
    val frequency           = { params["frequency"]        as  Float             }
    val phase               = { params["phase"]            as  Float             }

    val numParamsInUse      = {
        val num1 = map().initParams.size
        val num2 = if (juliaMode() && !map().initJuliaMode) 1 else 0
        // val num3 = texture().initParams.size
        // Log.d("MAIN ACTIVITY", "$num1, $num2, $num3")
        Log.d("MAIN ACTIVITY", "$num1, $num2")
        num1 + num2
    }

}
class SettingsConfig (val params: MutableMap<String, Any>) {

    val resolution         = { params["resolution"]        as Resolution }
    val precision          = { params["precision"]         as Precision  }
    val continuousRender   = { params["continuousRender"]  as Boolean    }
    val displayParams      = { params["displayParams"]     as Boolean    }

}


fun MotionEvent.focalLength() : Float {
    val f = focus()
    val pos = floatArrayOf(x, y)
    val dist = floatArrayOf(pos[0] - f[0], pos[1] - f[1])
    return sqrt(dist[0].toDouble().pow(2.0) +
            dist[1].toDouble().pow(2.0)).toFloat()
}
fun MotionEvent.focus() : FloatArray {
    return if (pointerCount == 1) floatArrayOf(x, y)
    else { floatArrayOf((getX(0) + getX(1))/2.0f, (getY(0) + getY(1))/2.0f) }
}
fun FloatArray.mult(s: Float) : FloatArray {
    return FloatArray(this.size) {i: Int -> s*this[i]}
}
fun FloatArray.invert() : FloatArray {
    return FloatArray(this.size) {i: Int -> 1.0f - this[i]}
}
fun DoubleArray.mult(s: Double) : DoubleArray {
    return DoubleArray(this.size) {i: Int -> s*this[i]}
}
fun DoubleArray.negative() : DoubleArray {
    return doubleArrayOf(-this[0], -this[1])
}
fun splitSD(a: Double) : FloatArray {

    val b = FloatArray(2)
    b[0] = a.toFloat()
    b[1] = (a - b[0].toDouble()).toFloat()
    return b

}
fun splitDD(a: DualDouble) : FloatArray {

    val b = FloatArray(4)
    b[0] = a.hi.toFloat()
    b[1] = (a.hi - b[0].toDouble()).toFloat()
    b[2] = a.lo.toFloat()
    b[3] = (a.lo - b[2].toDouble()).toFloat()
    return b

}


class ViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {

    private val mFragmentList = ArrayList<Fragment>()
    private val mFragmentTitleList = ArrayList<String>()

    override fun getItem(position: Int) : Fragment {
        return mFragmentList[position]
    }

    override fun getCount() : Int {
        return mFragmentList.size
    }

    fun addFrag(fragment: Fragment, title: String) {
        mFragmentList.add(fragment)
        mFragmentTitleList.add(title)
    }

    override fun getPageTitle(position: Int) : CharSequence {
        return mFragmentTitleList[position]
    }

}


class MainActivity : AppCompatActivity(),
        EquationFragment.OnParamChangeListener,
        SettingsFragment.OnParamChangeListener {


    private lateinit var f : Fractal
    private lateinit var fractalView : FractalSurfaceView
    private lateinit var uiQuickButtons : List<View>
    private lateinit var displayParamRows : List<LinearLayout>

    private var orientation = Configuration.ORIENTATION_UNDEFINED



    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


//        orientation = baseContext.resources.configuration.orientation
//        Log.d("MAIN ACTIVITY", "orientation: $orientation")
//        val orientationChanged = (savedInstanceState?.getInt("orientation") ?: orientation) != orientation

        // get screen dimensions
        val displayMetrics = baseContext.resources.displayMetrics
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val aspectRatio = screenHeight.toDouble() / screenWidth

        // val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        // val statusBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0


        // get saved or default parameters

        val map = ComplexMap.all[savedInstanceState?.getString("map")]?.invoke(resources)
                ?: ComplexMap.mandelbrot(resources)
        val p1 = savedInstanceState?.getDoubleArray("p1") ?: doubleArrayOf(0.0, 0.0)
        val p2 = savedInstanceState?.getDoubleArray("p2") ?: doubleArrayOf(0.0, 0.0)
        val p3 = savedInstanceState?.getDoubleArray("p3") ?: doubleArrayOf(0.0, 0.0)
        val p4 = savedInstanceState?.getDoubleArray("p4") ?: doubleArrayOf(0.0, 0.0)
        val texture = TextureAlgorithm.all[savedInstanceState?.getString("texture")]?.invoke(resources)
                ?: TextureAlgorithm.escape(resources)
        val q1 = savedInstanceState?.getDouble("q1") ?: 0.0
        val q2 = savedInstanceState?.getDouble("q2") ?: 0.0
        val juliaMode = savedInstanceState?.getBoolean("juliaMode") ?: false
        val paramSensitivity = savedInstanceState?.getDouble("paramSensitivity") ?: 1.0
        val coords = savedInstanceState?.getDoubleArray("coords") ?: doubleArrayOf(0.0, 0.0)
        val savedCoords = savedInstanceState?.getDoubleArray("savedCoords") ?: doubleArrayOf(0.0, 0.0)
        val scale = savedInstanceState?.getDoubleArray("scale") ?: doubleArrayOf(1.0, aspectRatio)
        val savedScale = savedInstanceState?.getDoubleArray("savedScale") ?: doubleArrayOf(1.0, aspectRatio)
        val maxIter = savedInstanceState?.getInt("maxIter") ?: 255
        val bailoutRadius = savedInstanceState?.getFloat("bailoutRadius") ?: 1e5f
        val palette = ColorPalette.all[savedInstanceState?.getString("palette")] ?: ColorPalette.p5
        val frequency = savedInstanceState?.getFloat("frequency") ?: 2.0f
        val phase = savedInstanceState?.getFloat("phase") ?: 0.0f
        val resolution = Resolution.valueOf(savedInstanceState?.getString("resolution") ?: "HIGH")
        val precision = Precision.valueOf(savedInstanceState?.getString("precision") ?: "AUTO")
        val continuousRender = savedInstanceState?.getBoolean("continuousRender") ?: false
        val displayParamsBoolean = savedInstanceState?.getBoolean("displayParams") ?: true

        val fractalConfig = FractalConfig(mutableMapOf(
                "map"               to  map,
                "p1"                to  p1,
                "p2"                to  p2,
                "p3"                to  p3,
                "p4"                to  p4,
                "texture"           to  texture,
                "q1"                to  q1,
                "q2"                to  q2,
                "juliaMode"         to  juliaMode,
                "paramSensitivity"  to  paramSensitivity,
                "coords"            to  coords,
                "savedCoords"       to  savedCoords,
                "scale"             to  scale,
                "savedScale"        to  savedScale,
                "maxIter"           to  maxIter,
                "bailoutRadius"     to  bailoutRadius,
                "palette"           to  palette,
                "frequency"         to  frequency,
                "phase"             to  phase
        ))
        val settingsConfig = SettingsConfig(mutableMapOf(
                "resolution"        to resolution,
                "precision"         to precision,
                "continuousRender"  to continuousRender,
                "displayParams"     to displayParamsBoolean,
                "saveToFile"        to false
        ))


        // create fractal
        f = Fractal(
                this,
                fractalConfig,
                settingsConfig,
                intArrayOf(screenWidth, screenHeight)
        )

//        if (orientationChanged) {
//            f.switchOrientation()
//            Log.d("MAIN", "orientation changed")
//        }


        fractalView = FractalSurfaceView(f, this)
        fractalView.layoutParams = ViewGroup.LayoutParams(screenWidth, screenHeight)
        fractalView.hideSystemUI()

        setContentView(R.layout.activity_main)

        val fractalLayout = findViewById<FrameLayout>(R.id.layout_main)
        fractalLayout.addView(fractalView)


        val displayParams = findViewById<LinearLayout>(R.id.displayParams)
        displayParamRows = listOf(
            findViewById(R.id.displayParamRow1),
            findViewById(R.id.displayParamRow2),
            findViewById(R.id.displayParamRow3)
        )
        displayParams.removeViews(1, displayParams.childCount - 1)


        val uiQuick = findViewById<LinearLayout>(R.id.uiQuick)
        val buttonBackgrounds = arrayOf(
            resources.getDrawable(R.drawable.round_button_unselected, null),
            resources.getDrawable(R.drawable.round_button_selected, null)
        )
        uiQuickButtons = listOf(
            findViewById(R.id.transformButton),
            findViewById(R.id.colorButton),
            findViewById(R.id.paramButton1),
            findViewById(R.id.paramButton2),
            findViewById(R.id.paramButton3),
            findViewById(R.id.paramButton4)
        )
        val uiQuickButtonListener = View.OnClickListener {
            val s = when (it) {
                is Button -> it.text.toString()
                is ImageButton -> it.contentDescription.toString()
                else -> ""
            }
            fractalView.reaction = Reaction.valueOf(s)
            (displayParams.getChildAt(0) as TextView).text = when (fractalView.reaction) {
                Reaction.POSITION, Reaction.COLOR -> s
                else -> "PARAMETER ${s[1]}"
            }

            if (fractalView.f.settingsConfig.displayParams()) {
                displayParams.removeViews(1, displayParams.childCount - 1)
                for (i in 0 until fractalView.numDisplayParams()) {
                    displayParams.addView(displayParamRows[i])
                }
            }

            for (b in uiQuickButtons) {
                val btd = b.background as TransitionDrawable
                if (b == it) { btd.startTransition(0) }
                else { btd.resetTransition() }
            }
            fractalView.f.updateDisplayParams(Reaction.valueOf(s), true)
        }
        for (b in uiQuickButtons) {
            b.background = TransitionDrawable(buttonBackgrounds)
            b.setOnClickListener(uiQuickButtonListener)
        }
        val diff = f.fractalConfig.map().initParams.size - uiQuick.childCount + 2
        uiQuick.removeViews(0, abs(diff))
        uiQuick.bringToFront()
        uiQuickButtons[0].performClick()

        val buttonScroll = findViewById<HorizontalScrollView>(R.id.buttonScroll)
        val leftArrow = findViewById<ImageView>(R.id.leftArrow)
        val rightArrow = findViewById<ImageView>(R.id.rightArrow)
        leftArrow.alpha = 0f
        rightArrow.alpha = 0f

        buttonScroll.viewTreeObserver.addOnScrollChangedListener {
            if (uiQuick.width > buttonScroll.width) {
                val scrollX = buttonScroll.scrollX
                val scrollEnd = uiQuick.width - buttonScroll.width
                // Log.d("MAIN ACTIVITY", "scrollX: $scrollX")
                // Log.d("MAIN ACTIVITY", "scrollEnd: $scrollEnd")
                when {
                    scrollX > 5 -> leftArrow.alpha = 1f
                    scrollX < 5 -> leftArrow.alpha = 0f
                }
                when {
                    scrollX < scrollEnd - 5 -> rightArrow.alpha = 1f
                    scrollX > scrollEnd - 5 -> rightArrow.alpha = 0f
                }
            }
            else {
                leftArrow.alpha = 0f
                rightArrow.alpha = 0f
            }
        }


        val uiFullTabs = findViewById<TabLayout>(R.id.uiFullTabs)
        uiFullTabs.tabGravity = TabLayout.GRAVITY_FILL





        // initialize fragments and set UI params from fractal
        val equationFragment = EquationFragment()
        val settingsFragment = SettingsFragment()
        // val saveFragment = SaveFragment()

        equationFragment.config = f.fractalConfig
        settingsFragment.config = f.settingsConfig






        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFrag( equationFragment,  "Fractal" )
        adapter.addFrag( settingsFragment,  "Settings" )

        val viewPager = findViewById<ViewPager>(R.id.viewPager)
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(uiFullTabs))

        uiFullTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
                Log.d("MAIN ACTIVITY", "tab: ${tab.text}")
                when (tab.text) {
                    "fractal" -> {
                        Log.d("MAIN ACTIVITY", "Fractal tab selected")
                    }
                    else -> {}
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })





        // val phi = 0.5*(sqrt(5.0) + 1.0)
        // val uiFullHeightOpen = (screenHeight*(1.0 - 1.0/phi)).toInt()
        // val uiFullHeightFullscreen = screenHeight - statusBarHeight
        val uiFullHeightOpen = screenHeight/2
        // fractalView.y = -uiFullHeightOpen/2f

        val uiFull = findViewById<LinearLayout>(R.id.uiFull)
        uiFull.layoutParams.height = 1
        uiFull.bringToFront()

        val uiFullButton = findViewById<ImageButton>(R.id.uiFullButton)
        uiFullButton.setOnClickListener {
            val hStart : Int
            val hEnd : Int
            if (uiFull.height == 1) {
                hStart = 1
                hEnd = uiFullHeightOpen
            }
            else {
                hStart = uiFullHeightOpen
                hEnd = 1
            }

            val anim = ValueAnimator.ofInt(hStart, hEnd)
            anim.addUpdateListener { animation ->
                val intermediateHeight = animation?.animatedValue as Int
                val c = ConstraintSet()
                c.clone(overlay)
                c.constrainHeight(R.id.uiFull, intermediateHeight)
                c.applyTo(overlay)
                fractalView.y = -uiFull.height/2.0f
            }
            anim.duration = 300
            anim.start()
        }

        val overlay = findViewById<ConstraintLayout>(R.id.overlay)
        overlay.bringToFront()

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)


    }

    private fun addMapParams(n: Int) {
        val uiQuick = findViewById<LinearLayout>(R.id.uiQuick)
        for (i in 1..n) { uiQuick.addView(uiQuickButtons[uiQuick.childCount], 0) }
    }
    private fun removeMapParams(n: Int) {
        val uiQuick = findViewById<LinearLayout>(R.id.uiQuick)
        for (i in 1..n) { uiQuick.removeView(uiQuickButtons[uiQuick.childCount - 1]) }
    }

    override fun onSaveInstanceState(outState: Bundle?) {

        Log.d("MAIN ACTIVITY", "saving instance state !!")

        // save FractalConfig values
        outState?.putString(        "map",               f.fractalConfig.map().name          )
        outState?.putDoubleArray(   "p1",                f.fractalConfig.p1()                )
        outState?.putDoubleArray(   "p2",                f.fractalConfig.p2()                )
        outState?.putDoubleArray(   "p3",                f.fractalConfig.p3()                )
        outState?.putDoubleArray(   "p4",                f.fractalConfig.p4()                )
        outState?.putString(        "texture",           f.fractalConfig.texture().name      )
        outState?.putDouble(        "q1",                f.fractalConfig.q1()                )
        outState?.putDouble(        "q2",                f.fractalConfig.q2()                )
        outState?.putBoolean(       "juliaMode",         f.fractalConfig.juliaMode()         )
        outState?.putDouble(        "paramSensitivity",  f.fractalConfig.paramSensitivity()  )
        outState?.putDoubleArray(   "coords",            f.fractalConfig.coords()            )
        outState?.putDoubleArray(   "savedCoords",       f.fractalConfig.savedCoords()       )
        outState?.putDoubleArray(   "scale",             f.fractalConfig.scale()             )
        outState?.putDoubleArray(   "savedScale",        f.fractalConfig.savedScale()        )
        outState?.putInt(           "maxIter",           f.fractalConfig.maxIter()           )
        outState?.putFloat(         "bailoutRadius",     f.fractalConfig.bailoutRadius()     )
        outState?.putString(        "palette",           f.fractalConfig.palette().name      )
        outState?.putFloat(         "frequency",         f.fractalConfig.frequency()         )
        outState?.putFloat(         "phase",             f.fractalConfig.phase()             )

        // save SettingsConfig values
        outState?.putString(    "resolution",        f.settingsConfig.resolution().name   )
        outState?.putBoolean(   "continuousRender",  f.settingsConfig.continuousRender()  )
        outState?.putBoolean(   "displayParams",     f.settingsConfig.displayParams()     )
        outState?.putString(    "precision",         f.settingsConfig.precision().name    )

        super.onSaveInstanceState(outState)
    }
    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)
        when (fragment) {
            is SettingsFragment -> fragment.setOnParamChangeListener(this)
            is EquationFragment -> fragment.setOnParamChangeListener(this)
        }
    }
    override fun onEquationParamsChanged(key: String, value: Any) {
        if (f.fractalConfig.params[key] != value) {
            Log.d("MAIN ACTIVITY", "$key set from ${f.fractalConfig.params[key]} to $value")
            f.fractalConfig.params[key] = value
            when (key) {
                "map" -> {
                    f.reset()
                    f.renderShaderChanged = true
                    val uiQuick = findViewById<LinearLayout>(R.id.uiQuick)
                    removeMapParams(uiQuick.childCount - 2)
                    addMapParams(f.fractalConfig.map().initParams.size)
                    uiQuick.getChildAt(uiQuick.childCount - 1).performClick()
                    fractalView.r.renderToTex = true
                }
                "p1", "p2", "p3", "p4" -> {
                    f.updateMapParamEditText(key[1].toString().toInt())
                    fractalView.r.renderToTex = true
                }
                "q1", "q2" -> {
                    f.updateTextureParamEditText(key[1].toString().toInt())
                    fractalView.r.renderToTex = true
                }
                "texture" -> {
                    f.resetTextureParams()
                    if (f.fractalConfig.texture().name == "Escape Time Smooth with Lighting") {
                        f.colorShaderChanged = true
                    }
                    f.renderShaderChanged = true
                    fractalView.r.renderToTex = true
                }
                "juliaMode" -> {
                    if (value as Boolean) {
                        addMapParams(1)
                        f.fractalConfig.savedCoords()[0] = f.fractalConfig.coords()[0]
                        f.fractalConfig.savedCoords()[1] = f.fractalConfig.coords()[1]
                        f.fractalConfig.savedScale()[0] = f.fractalConfig.scale()[0]
                        f.fractalConfig.savedScale()[1] = f.fractalConfig.scale()[1]
                        f.fractalConfig.coords()[0] = 0.0
                        f.fractalConfig.coords()[1] = 0.0
                        f.fractalConfig.scale()[0] = 3.5
                        f.fractalConfig.scale()[1] = 3.5 * f.screenRes[1].toDouble() / f.screenRes[0]
                        f.fractalConfig.params["p${f.fractalConfig.numParamsInUse()}"] = doubleArrayOf(
                                f.fractalConfig.savedCoords()[0],
                                f.fractalConfig.savedCoords()[1]
                        )
                    }
                    else {
                        removeMapParams(1)
                        f.fractalConfig.coords()[0] = f.fractalConfig.savedCoords()[0]
                        f.fractalConfig.coords()[1] = f.fractalConfig.savedCoords()[1]
                        f.fractalConfig.scale()[0] = f.fractalConfig.savedScale()[0]
                        f.fractalConfig.scale()[1] = f.fractalConfig.savedScale()[1]
                    }
                    f.updateMapParamEditTexts()
                    f.renderShaderChanged = true
                    fractalView.r.renderToTex = true
                }
                "coords" -> {
                    f.updatePositionEditTexts()
                    fractalView.r.renderToTex = true
                }
                "scale" -> {
                    f.updatePositionEditTexts()
                    fractalView.r.renderToTex = true
                }
                "maxIter" -> {
                    fractalView.r.renderToTex = true
                }
                "bailoutRadius" -> {
                    f.updatePositionEditTexts()
                    fractalView.r.renderToTex = true
                }
                "palette" -> {}
                "frequency" -> {
                    f.updateColorParamEditTexts()
                }
                "phase" -> {
                    f.updateColorParamEditTexts()
                }
            }
            fractalView.requestRender()
        }
        else {
            Log.d("MAIN ACTIVITY", "$key already set to $value")
        }
    }
    override fun onSettingsParamsChanged(key: String, value: Any) {
        if (f.settingsConfig.params[key] != value) {
            Log.d("MAIN ACTIVITY", "$key set from ${f.settingsConfig.params[key]} to $value")
            f.settingsConfig.params[key] = value
            when (key) {
                "resolution" -> {
                    f.resolutionChanged = true
                    fractalView.r.renderToTex = true
                    fractalView.requestRender()
                }
                "precision" -> {
                    f.renderShaderChanged = true
                    fractalView.r.renderToTex = true
                    fractalView.requestRender()
                }
                "continuousRender" -> {
                    f.renderProfileChanged = true
                    fractalView.r.renderToTex = true
                    fractalView.requestRender()
                }
                "displayParams" -> {
                    val displayParams = findViewById<LinearLayout>(R.id.displayParams)
                    if (value as Boolean) {
                        for (i in 0 until fractalView.numDisplayParams()) {
                            displayParams.addView(displayParamRows[i])
                        }
                        fractalView.f.updateDisplayParams(fractalView.reaction, false)
                    }
                    else {
                        displayParams.removeViews(1, displayParams.childCount - 1)
                    }
                }
                "saveToFile" -> {
                    if (fractalView.r.isRendering) {
                        val toast = Toast.makeText(baseContext, "Please wait for the image to finish rendering", Toast.LENGTH_SHORT)
                        toast.show()
                    }
                    else {
                        if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this,
                                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                    WRITE_STORAGE_REQUEST_CODE)
                        }
                        else {
                            fractalView.r.saveImage = true
                            fractalView.requestRender()
                            val toast = Toast.makeText(baseContext, "Image saved to Gallery", Toast.LENGTH_SHORT)
                            toast.show()
                        }
                    }
                    f.settingsConfig.params[key] = false
                }
            }
        }
        else {
            Log.d("MAIN ACTIVITY", "$key already set to $value")
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            WRITE_STORAGE_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    fractalView.r.saveImage = true
                    fractalView.requestRender()
                    val toast = Toast.makeText(baseContext, "Image saved to Gallery", Toast.LENGTH_SHORT)
                    toast.show()
                } else {
                    val toast = Toast.makeText(baseContext, "Image not saved - storage permission required", Toast.LENGTH_LONG)
                    toast.show()
                }
                return
            }
            else -> {}
        }
    }


}
