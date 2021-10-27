package com.selfsimilartech.fractaleye

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
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.firebase.crashlytics.FirebaseCrashlytics
import eu.davidea.flexibleadapter.FlexibleAdapter
import org.apfloat.Apcomplex
import org.apfloat.Apfloat
import org.apfloat.ApfloatMath
import java.util.ArrayList
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt



/* INTERFACES / CLASSES */

interface Goldable {
    val goldFeature : Boolean
}
interface Customizable {

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
enum class TextureMode { OUT, IN, BOTH }
enum class ListLayoutType { GRID, LINEAR }
enum class Sensitivity(

    val iconId: Int,
    val zoomDiscrete: Float,
    val zoomContinuous: Float,
    val rotationDiscrete: Float,
    val rotationContinuous: Float,
    val shiftDiscrete: Float,
    val shiftContinuous: Float,
    val param: Float

) {

    LOW(R.drawable.sensitivity_low, 1.01f, 1.0065f, Math.PI.toFloat() / 256f, Math.PI.toFloat() / 512f, 1 / 256f, 1 / 512f, 0.01f),
    MED(R.drawable.sensitivity_med, 1.1f, 1.035f, Math.PI.toFloat() / 16f, Math.PI.toFloat() / 128f, 1 / 32f, 1 / 128f, 2f),
    HIGH(R.drawable.sensitivity_high, 1.35f, 1.125f, Math.PI.toFloat() / 2f, Math.PI.toFloat() / 64f, 1 / 4f, 1 / 32f, 10f);

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
class ViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {

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

fun now() = System.currentTimeMillis()
fun androidVersionAtLeast(version : Int) : Boolean {
    return Build.VERSION.SDK_INT >= version
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



/* VIEWS */

fun TabLayout.currentEditMode() : MainActivity.EditMode = MainActivity.EditMode.values()[selectedTabPosition]
fun TabLayout.getTabAt(editMode: MainActivity.EditMode) : TabLayout.Tab = getTabAt(editMode.ordinal) as TabLayout.Tab

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

fun ImageButton.disable() {
    foregroundTintList = ColorStateList.valueOf(Color.GRAY)
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

fun RecyclerView.smoothSnapToPosition(targetPos: Int, snapMode: Int = LinearSmoothScroller.SNAP_TO_START) {

    val smoothScroller = object : LinearSmoothScroller(this.context) {
        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
            return super.calculateSpeedPerPixel(displayMetrics)
        }
        override fun getVerticalSnapPreference() : Int = snapMode
        override fun getHorizontalSnapPreference() : Int = snapMode
    }

    val firstVisiblePos = (layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
    (adapter as FlexibleAdapter<*>).apply {
        val distance = targetPos - firstVisiblePos
        if (abs(distance) > 12) {
            scrollToPosition(targetPos - sign(distance.toDouble()).toInt() * 12)
        }
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



/* DATA */

fun Int.clamp(low: Int, high: Int) : Int {
    return maxOf(minOf(this, high), low)
}
fun Float.clamp(low: Float, high: Float) : Float {
    return maxOf(minOf(this, high), low)
}
fun Double.clamp(low: Double, high: Double) : Double {
    return maxOf(minOf(this, high), low)
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
