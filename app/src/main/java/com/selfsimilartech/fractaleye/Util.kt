package com.selfsimilartech.fractaleye

import android.animation.LayoutTransition
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.util.Range
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.apfloat.Apcomplex
import org.apfloat.Apfloat
import org.apfloat.ApfloatMath
import java.io.Serializable
import java.util.ArrayList
import kotlin.math.*


/* INTERFACES / CLASSES */

interface Goldable {
    val goldFeature : Boolean
}
interface Customizable : Goldable {

    var name : String
    var thumbnail : Bitmap?
    var isFavorite : Boolean

    fun isCustom() : Boolean

    fun getName(localizedResource: Resources) : String

}
interface ClickListener {
    fun onClick(view: View, position: Int)
    fun onLongClick(view: View, position: Int)
}
interface Toggleable {
    var isChecked : Boolean
}

enum class EditMode(val icon: Int, val alwaysDisplayParam: Boolean) {

    POSITION(R.drawable.position, false),
    COLOR(R.drawable.color, false),
    SHAPE(R.drawable.shape, true),
    TEXTURE(R.drawable.texture, true),
    NONE(R.drawable.cancel, false);

    var paramMenuLayout : ViewGroup? = null
    var paramDisplayLayout : ViewGroup? = null
    var listLayout      : ViewGroup? = null
    var utilityButtons  : ViewGroup? = null
    var listNavButtons     : ArrayList<Button> = arrayListOf()
    var customNavButtons   : ArrayList<Button> = arrayListOf()

    var updateDisplay : () -> Unit = {}
    var updateLayout: () -> Unit = {}
    var updateAdjust: () -> Unit = {}

}
enum class GpuPrecision(val bits: Int, val threshold: Double) {
    SINGLE(23, 5e-4), DUAL(46, 1e-12)
}
enum class CpuPrecision(val threshold: Double) {
    DOUBLE(1e-12), PERTURB(1e-200)
}
enum class Reaction {
    NONE, SHAPE, TEXTURE, COLOR, POSITION
}
enum class ChunkProfile(val fgSingle: Int, val bgSingle: Int, val fgDual: Int, val bgDual: Int, val fgContSingle: Int, val fgContDual: Int) {
    LOW(10, 3, 30, 10, 1, 1),
    MED(20, 5, 50, 15, 1, 1),
    HIGH(40, 10, 100, 20, 1, 1)
}
enum class TextureRegion(val iconId: Int) {
    OUT(R.drawable.texture_region_out),
    IN(R.drawable.texture_region_in),
    BOTH(R.drawable.texture_region_both)
}
enum class ListLayoutType { GRID, LINEAR }

enum class Sensitivity(val iconId: Int) {

    LOW(  R.drawable.sensitivity_low  ),
    MED(  R.drawable.sensitivity_med  ),
    HIGH( R.drawable.sensitivity_high );

    fun next() : Sensitivity = when (this) {
        LOW -> MED
        MED -> HIGH
        HIGH -> LOW
    }

}
enum class UiLayoutHeight(private val dimenId: Int, val closed: Boolean = false) {

    CLOSED_SHORT(R.dimen.uiLayoutHeightClosedShort, true),
    CLOSED_MED(R.dimen.uiLayoutHeightClosedMedium, true),
    SHORT(R.dimen.uiLayoutHeightShort),
    MED(R.dimen.uiLayoutHeightMedium),
    TALL(R.dimen.uiLayoutHeightTall),
    TALLER(R.dimen.uiLayoutHeightTaller);

    var dimen: Float = 0f

    fun initialize(res: Resources) {
        dimen = res.getDimension(dimenId)
    }

}
enum class UiState {

    HOME,
    EDITMODE_LIST,
    BOOKMARK_LIST,
    CUSTOM_PALETTE,
    CUSTOM_SHAPE,
    RANDOMIZER,
    VIDEO

}

interface Sensitive {
    var sensitivity : Sensitivity
}
class PositionParam(
    val nameId: Int,
    val decreaseIconId: Int,
    val increaseIconId: Int,
    private val discreteDeltas: List<Double>,
    private val continuousDeltas: List<Double>,
    val toDisplayFormat: (Position) -> String,
    val fromDislayFormat: (Position, Double) -> Unit
) : Sensitive {

    var onDecreaseClick        : View.OnClickListener?      = null
    var onDecreaseLongClick    : View.OnLongClickListener?  = null
    var onIncreaseClick        : View.OnClickListener?      = null
    var onIncreaseLongClick    : View.OnLongClickListener?  = null

    var discreteDelta = discreteDeltas[1]
    var continuousDelta = continuousDeltas[1]
    override var sensitivity: Sensitivity = Sensitivity.MED
        set (value) {
            field = value
            discreteDelta   = discreteDeltas[value.ordinal]
            continuousDelta = continuousDeltas[value.ordinal]
        }

    companion object {

        val ZOOM = PositionParam(
            R.string.zoom, R.drawable.zoom_out, R.drawable.zoom_in,
            listOf( 1.01,   1.1,   1.35  ),
            listOf( 1.0065, 1.025, 1.075 ),
            { pos -> (0.5 - log10(pos.zoom)).format(3) },
            { pos, result -> pos.zoom = 10.0.pow(0.5 - result) }
        )

        val ROTATION = PositionParam(
            R.string.rotation, R.drawable.rotate_left, R.drawable.rotate_right,
            listOf( Math.PI/512.0,  Math.PI/32.0,  Math.PI/4.0  ),
            listOf( Math.PI/1024.0, Math.PI/256.0, Math.PI/64.0 ),
            { pos -> "%.1f".format(pos.rotation.inDegrees()) },
            { pos, result -> pos.rotation = result.inRadians() }
        )

        val SHIFT_HORIZONTAL = PositionParam(
            R.string.x, R.drawable.shift_left, R.drawable.shift_right,
            listOf( 1.0/256.0, 1.0/32.0,  1.0/4.0  ),
            listOf( 1.0/512.0, 1.0/128.0, 1.0/32.0 ),
            { pos -> pos.x.format(15) },
            { pos, result -> pos.x = result }
        )

        val SHIFT_VERTICAL = PositionParam(
            R.string.y, R.drawable.shift_down, R.drawable.shift_up,
            SHIFT_HORIZONTAL.discreteDeltas,
            SHIFT_HORIZONTAL.continuousDeltas,
            { pos -> pos.y.format(15) },
            { pos, result -> pos.y = result }
        )

    }

}

open class ColorParam(val nameId: Int, val iconId: Int)
class ColorRealParam(

    nameId: Int,
    iconId: Int,
    private val discreteDeltas: List<Double>,
    private val continuousDeltas: List<Double>,
    val toDisplayFormat: (ColorConfig) -> String,
    val fromDislayFormat: (ColorConfig, Double) -> Unit,
    val toProgress: (ColorConfig) -> Double,
    val fromProgress: (ColorConfig, Double) -> Unit

) : ColorParam(nameId, iconId), Sensitive {

    var onDecreaseClick        : View.OnClickListener? = null
    var onDecreaseLongClick    : View.OnLongClickListener? = null
    var onIncreaseClick        : View.OnClickListener? = null
    var onIncreaseLongClick    : View.OnLongClickListener? = null

    var discreteDelta = discreteDeltas[1]
    var continuousDelta = continuousDeltas[1]
    override var sensitivity: Sensitivity = Sensitivity.MED
        set(value) {
            field = value
            discreteDelta = discreteDeltas[value.ordinal]
            continuousDelta = continuousDeltas[value.ordinal]
        }

    companion object {

        val FREQUENCY = ColorRealParam(
            R.string.frequency,
            R.drawable.frequency2,
            listOf(1.025, 1.05, 1.1),
            listOf(1.005, 1.025, 1.075),
            { config -> log2(config.frequency + 1.0).format(3) },
            { config, result -> config.frequency = 2.0.pow(result) - 1.0 },
            { config -> log2(config.frequency + 1.0)/5.0 },
            { config, progress -> config.frequency = 2.0.pow(progress*5.0) - 1.0 }
        )

        val PHASE = ColorRealParam(
            R.string.phase,
            R.drawable.phase,
            listOf(0.005, 0.025, 0.05),
            listOf(0.0025, 0.01, 0.035),
            { config -> config.phase.format(4) },
            { config, result -> config.phase = result },
            { config -> config.phase },
            { config, progress -> config.phase = progress }
        )

        val DENSITY = ColorRealParam(
            R.string.density,
            R.drawable.density,
            listOf(0.005, 0.025, 0.05),
            listOf(0.0025, 0.01, 0.035),
            { config -> config.density.format(3) },
            { config, result -> config.density = result.clamp(0.0, 5.0) },
            { config -> config.density/5.0 },
            { config, progress -> config.density = progress*5.0 }
        )

    }

}
class ColorAccent(nameId: Int, iconId: Int) : ColorParam(nameId, iconId) {

    companion object {
        val FILL = ColorAccent(R.string.solid_fill, R.drawable.fill_color)
        val OUTLINE = ColorAccent(R.string.outline, R.drawable.outline_color)
    }

    lateinit var color : () -> Int
    lateinit var updateColor : (c: Int) -> Unit

}

class PositionList(
    val main: Position = Position(),
    val julia: Position = Position(zoom = 3.5)
) {

    var active = main

    fun setFrom(newPositions: PositionList) {
        main.setFrom(newPositions.main)
        julia.setFrom(newPositions.julia)
    }
    fun clone() : PositionList {
        return PositionList(
            main.clone(),
            julia.clone()
        )
    }
    fun reset() {

        main.reset()
        julia.reset()

    }

}
class NoScrollViewPager : ViewPager {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        performClick()
        return false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return false
    }

    override fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        super.setCurrentItem(item, false)
    }

    override fun setCurrentItem(item: Int) {
        super.setCurrentItem(item, false)
    }

}
class ViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragmentList = ArrayList<Fragment>()

    override fun getItem(position: Int) : Fragment {
        return fragmentList[position]
    }

    override fun getCount() : Int {
        return fragmentList.size
    }

    fun addFrag(fragment: Fragment) {
        fragmentList.add(fragment)
    }

    override fun getPageTitle(position: Int) : CharSequence? {
        return null
    }

}



/* SYSTEM / UI */

enum class Action {
    INIT,
    SETTINGS,
    UPGRADE,
    NEW_BOOKMARK,
    SAVE_IMAGE,
    RESOLUTION_CHANGE,
    ASPECT_CHANGE,
    PALETTE_CHANGE,
    PALETTE_CREATE,
    SHAPE_CHANGE,
    SHAPE_CREATE,
    TEXTURE_CHANGE,
    BOOKMARK_LOAD,
    RANDOMIZE
}
fun crashlytics() : FirebaseCrashlytics = FirebaseCrashlytics.getInstance()
fun FirebaseCrashlytics.updateLastAction(action: Action) {
    setCustomKey(CRASH_KEY_LAST_ACTION, action.name)
}

fun currentTimeMs() = System.currentTimeMillis()
fun currentTimeNs() = System.nanoTime()
fun androidVersionAtLeast(version : Int) : Boolean {
    return Build.VERSION.SDK_INT >= version
}
fun hideKeyboard(window: Window, root: View) {
    WindowCompat.getInsetsController(window, root)?.hide(WindowInsetsCompat.Type.ime())
}
fun hideSystemBars(window: Window, root: View) {
    if (androidVersionAtLeast(Build.VERSION_CODES.R)) {
        window.setDecorFitsSystemWindows(false)
        window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
//        if (androidVersionAtLeast(Build.VERSION_CODES.P)) {
//            window.attributes?.run {
//                layoutInDisplayCutoutMode =
//                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
//            }
//        }
    }
}
fun showSystemBars(window: Window, root: View) {
    if (androidVersionAtLeast(Build.VERSION_CODES.R)) {
        window.setDecorFitsSystemWindows(true)
        window.insetsController?.show(WindowInsets.Type.systemBars())
    } else {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, root).show(WindowInsetsCompat.Type.systemBars())
    }
}
fun AlertDialog.showImmersive(root: View) {
    if (SettingsConfig.hideSystemBars) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window?.apply {
                setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                )
                show()
                root.doOnLayout {
                    hideSystemBars(this, root)
                }
                clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            }
        } else {
            window?.apply {
                setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                )
                hideSystemBars(this, root)
                show()
                clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            }
        }
    } else {
        show()
    }
}

fun ProgressBar.reset() { progress = 0 }
fun ProgressBar.setProgress(a: Int, b: Int) {
    progress = (a.toFloat()/b.toFloat() * max).toInt()
}

fun ViewGroup.setOnLayoutTransitionEndListener(targetType: Int = LayoutTransition.CHANGE_APPEARING, sticky: Boolean = false, action: () -> Unit) {
    layoutTransition?.addTransitionListener(object : LayoutTransition.TransitionListener {
        override fun startTransition(transition: LayoutTransition?, container: ViewGroup?, view: View?, transitionType: Int) {}
        override fun endTransition(transition: LayoutTransition?, container: ViewGroup?, view: View?, transitionType: Int) {
            Log.d("TRANSITION", "transition type: ${transitionString(transitionType)}")
            if (transitionType == targetType) {
                if (!sticky) layoutTransition?.removeTransitionListener(this)
                action()
            }
        }
        fun transitionString(type: Int) : String {
            return when (type) {
                LayoutTransition.CHANGING -> "CHANGING"
                LayoutTransition.APPEARING -> "APPEARING"
                LayoutTransition.DISAPPEARING -> "DISAPPEARING"
                LayoutTransition.CHANGE_APPEARING -> "CHANGE_APPEARING"
                LayoutTransition.CHANGE_DISAPPEARING -> "CHANGE_DISAPPEARING"
                else -> "other"
            }
        }
    })
}



/* VIEWS */

fun TextView?.showAndSetText(id: Int) {
    if (this != null) {
        show()
        setText(id)
    }
}

fun View?.color() : Int {
    return (this?.background as? ColorDrawable)?.color ?: Color.BLACK
}
fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.makeInvisible() { visibility = View.INVISIBLE }
fun View.isVisible() : Boolean { return visibility == View.VISIBLE}
fun View.isInvisible() : Boolean { return visibility == View.INVISIBLE }
fun View.isHidden() : Boolean { return visibility == View.GONE}

fun ImageButton.disable(tint: Boolean = false) {
    if (tint) foregroundTintList = ColorStateList.valueOf(Color.GRAY)
    isClickable = false
    isFocusable = false
}
fun ImageButton.enable() {
    foregroundTintList = null
    isClickable = true
    isFocusable = true
}

fun Button.disable() {
    foreground = ColorDrawable(resources.getColor(R.color.halfTransparentBlack, null))
    isClickable = false
    isFocusable = false
}
fun Button.enable() {
    foreground = null
    isClickable = true
    isFocusable = true
}

fun ViewGroup.disable(toggleAlpha: Boolean = false) {
    isClickable = false
    isFocusable = false
    if (toggleAlpha) alpha = 0.35f
}
fun ViewGroup.enable() {
    isClickable = true
    isFocusable = true
    alpha = 1f
}

fun ConstraintLayout.connectBottomToTop(a: Int, b: Int, margin: Int? = null) {
    ConstraintSet().let { set ->
        set.clone(this)
        set.clear(a, ConstraintSet.TOP)
        set.connect(a, ConstraintSet.BOTTOM, b, ConstraintSet.TOP, margin ?: 8.dp(context))
        set.connect(a, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        set.connect(a, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        set.applyTo(this)
    }
}
fun ConstraintLayout.connectTopToBottom(a: Int, b: Int, margin: Int? = null) {
    ConstraintSet().let { set ->
        set.clone(this)
        set.clear(a, ConstraintSet.BOTTOM)
        set.connect(a, ConstraintSet.TOP, b, ConstraintSet.BOTTOM, margin ?: 8.dp(context))
        set.connect(a, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        set.connect(a, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        set.applyTo(this)
    }
}
fun ConstraintLayout.connectEndToStart(a: Int, b: Int) {
    ConstraintSet().let { set ->
        set.clone(this)
        set.connect(a, ConstraintSet.END, b, ConstraintSet.START)
        set.applyTo(this)
    }
}
fun ConstraintLayout.link(a: Int, b: Int) {
    ConstraintSet().let { set ->
        set.clone(this)
        set.connect( a, ConstraintSet.END,    b, ConstraintSet.START  )
        set.connect( a, ConstraintSet.TOP,    b, ConstraintSet.TOP    )
        set.connect( a, ConstraintSet.BOTTOM, b, ConstraintSet.BOTTOM )
        set.applyTo(this)
    }
}

fun RecyclerView.smoothSnapToPosition(position: Int, max: Int) {


    val firstVisiblePos = (layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
    val lastVisiblePos = (layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

    val targetPos : Int
    val snapMode : Int

    if (position < max - 1 && position + 1 > lastVisiblePos) {
        targetPos = position + 1
        snapMode = LinearSmoothScroller.SNAP_TO_END
    }
    else if (position > 0 && position - 1 < firstVisiblePos) {
        targetPos = position - 1
        snapMode = LinearSmoothScroller.SNAP_TO_START
    } else {
        targetPos = position
        snapMode = LinearSmoothScroller.SNAP_TO_END
    }

    val smoothScroller = object : LinearSmoothScroller(this.context) {
        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
            return 1f/3f
        }
        override fun getVerticalSnapPreference() : Int = snapMode
        override fun getHorizontalSnapPreference() : Int = snapMode
    }

    smoothScroller.targetPosition = targetPos
    layoutManager?.startSmoothScroll(smoothScroller)

}



/* LISTS */

fun <T : Goldable> Collection<T>.filterGold() : Collection<T> {
    return if (SettingsConfig.goldEnabled) this else filter { !it.goldFeature }
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
    else { floatArrayOf((getX(0) + getX(1)) / 2.0f, (getY(0) + getY(1)) / 2.0f) }
}

fun List<Texture>.replace(old: Texture, new: Texture) : List<Texture> {

    val newList = this.toMutableList()
    val index = this.indexOf(old)
    newList.remove(old)
    newList.add(index, new)
    return newList

}
fun List<Texture>.remove(rem: List<Texture>) : List<Texture> {

    val newList = this.toMutableList()
    newList.removeAll(rem)
    return newList

}
infix fun MutableList<Texture>.without(texture: Texture) : MutableList<Texture> {

    val newList = this.toMutableList()
    newList.remove(texture)
    return newList

}

enum class ListItemType : Serializable { DEFAULT, CUSTOM, FAVORITE }



/* DATA */

fun Int.dp(ctx: Context) : Int {
    val density = ctx.resources.displayMetrics?.density ?: 7f
    return (this*density + 0.5f).toInt()
}
fun randomf() : Float = Math.random().toFloat()
fun randomi(n: Int) : Int = (n*Math.random()).toInt()

fun Int.clamp(low: Int, high: Int) : Int {
    return maxOf(minOf(this, high), low)
}
fun Float.clamp(low: Float, high: Float) : Float {
    return maxOf(minOf(this, high), low)
}
fun Double.clamp(low: Double, high: Double) : Double {
    return maxOf(minOf(this, high), low)
}

fun Double.round(s: Int) : Int {
    return s*(this/s.toDouble()).roundToInt()
}

fun Float.inRadians() : Float = this*Math.PI.toFloat()/180f
fun Float.inDegrees() : Float = this*180f/Math.PI.toFloat()
fun Float.split() : PointF {
    val t = this*8193f
    val q = t - this
    val hi = t - q
    val lo = this - hi
    Log.e("SPLIT", "t: $t, q: $q, hi: $hi, lo: $lo")
    return PointF(hi, lo)
}

operator fun Double.times(w: Complex) : Complex {
    return Complex(this * w.x, this * w.y)
}
fun Double?.inRadians() : Double? = this?.times(Math.PI / 180.0)
fun Double?.inDegrees() : Double? = this?.times(180.0 / Math.PI)
fun Double.inRadians() : Double = this*Math.PI/180.0
fun Double.inDegrees() : Double = this*180.0/Math.PI
fun Double.format(sigDigits: Int) : String {
    return when (this) {
        Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY -> "err"
        else -> {
            val trunc = abs(this).toInt().toString()
            var freeDigits = sigDigits - trunc.length
            if (this < 0.0) freeDigits--
            "%.${freeDigits}f".format(this)
        }
    }
}
fun Double.split() : FloatArray {

    val b = FloatArray(2)
    b[0] = this.toFloat()
    b[1] = (this - b[0].toDouble()).toFloat()
    return b

}
fun Double.splitToPointF() : PointF {
    val hi = this.toFloat()
    val lo = (this - hi.toDouble()).toFloat()
    return PointF(hi, lo)
}
fun scurve(t: Double, c: Double) : Double {
    return when {
        t < c         -> t*t/(2.0*c*(1.0 - c))
        t <= 1.0 - c  -> (t - 0.5*c)/(1.0 - c)
        t <= 1.0      -> (t*(1.0 - 0.5*t) - 0.5)/(c*(1.0 - c)) + 1.0
        else          -> 0.0
    }
}

fun DoubleArray.mult(s: Double) : DoubleArray {
    return DoubleArray(this.size) { i: Int -> s*this[i]}
}
fun DoubleArray.negative() : DoubleArray {
    return doubleArrayOf(-this[0], -this[1])
}

fun Range<Double>.size() : Double { return upper - lower }
infix fun ClosedRange<Double>.step(step: Double): Iterable<Double> {
    require(start.isFinite())
    require(endInclusive.isFinite())
    require(step > 0.0) { "Step must be positive, was: $step." }
    val sequence = generateSequence(start) { previous ->
        if (previous == Double.POSITIVE_INFINITY) return@generateSequence null
        val next = previous + step
        if (next > endInclusive) null else next
    }
    return sequence.asIterable()
}

fun Apfloat.sqr() : Apfloat = this.multiply(this)
fun Apcomplex.sqr() : Apcomplex = this.multiply(this)
fun Apcomplex.cube() : Apcomplex = this.multiply(this).multiply(this)
fun Apcomplex.mod() : Apfloat = ApfloatMath.sqrt(this.real().sqr().add(this.imag().sqr()))
fun Apcomplex.modSqr() : Apfloat = this.real().sqr().add(this.imag().sqr())
