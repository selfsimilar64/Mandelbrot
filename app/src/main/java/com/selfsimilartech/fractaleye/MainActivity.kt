package com.selfsimilartech.fractaleye

import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.*
import java.util.*
import kotlin.math.*
import android.view.MotionEvent
import android.content.Context
import android.content.res.Resources
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import java.lang.ref.WeakReference
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.settings_fragment.*
import kotlinx.android.synthetic.main.texture_fragment.*
import kotlinx.android.synthetic.main.shape_fragment.*
import kotlinx.android.synthetic.main.color_fragment.*
import kotlinx.android.synthetic.main.position_fragment.*


const val SPLIT = 8193.0
const val MAX_SHAPE_PARAMS = 3
const val MAX_TEXTURE_PARAMS = 2
const val WRITE_STORAGE_REQUEST_CODE = 0
const val ITER_MAX_POW = 12.0
const val ITER_MIN_POW = 5.0
const val BUTTON_CLICK_DELAY_LONG = 300L
const val BUTTON_CLICK_DELAY_SHORT = 100L
const val PAGER_ANIM_DURATION = 200L
const val STATUS_BAR_HEIGHT = 24
const val NAV_BAR_HEIGHT = 48

const val RESOLUTION = "resolution"
//const val PRECISION = "precision"
//const val AUTO_PRECISION = "autoPrecision"
const val DISPLAY_PARAMS = "displayParams"
const val CONTINUOUS_RENDER = "continuousRender"
const val RENDER_BACKGROUND = "renderBackground"
const val FIT_TO_VIEWPORT = "fitToViewport"
//const val SHOW_HINTS = "showHints"
const val HIDE_NAV_BAR = "hideNavBar"
const val COLOR_LIST_VIEW_TYPE = "colorListViewType"
const val SHAPE_LIST_VIEW_TYPE = "shapeListViewType"
const val TEXTURE_LIST_VIEW_TYPE = "textureListViewType"
const val SHARED_PREFERENCES = "com.selfsimilartech.fractaleye.SETTINGS"

const val USE_PERTURBATION = false

//const val PLUS_UNICODE = '\u002B'
//const val MINUS_UNICODE = '\u2212'



operator fun Double.times(w: Complex) : Complex {
    return Complex(this*w.x, this*w.y)
}
fun TabLayout.getCurrentTab() : TabLayout.Tab = getTabAt(selectedTabPosition) as TabLayout.Tab
fun TabLayout.getCategory(index: Int) : MainActivity.Category = MainActivity.Category.values()[index]
fun TabLayout.getCurrentCategory() : MainActivity.Category = MainActivity.Category.values()[selectedTabPosition]
fun TabLayout.getTabAt(category: MainActivity.Category) : TabLayout.Tab = getTabAt(category.ordinal) as TabLayout.Tab
fun List<Texture>.replace(old: Texture, new: Texture) : List<Texture> {

    val newList = this.toMutableList()
    val index = this.indexOf(old)
    newList.remove(old)
    newList.add(index, new)
    return newList

}
infix fun MutableList<Texture>.without(texture: Texture) : MutableList<Texture> {

    val newList = this.toMutableList()
    newList.remove(texture)
    return newList

}
fun View.setProFeature(value: Boolean) { tag = value }
fun View.isProFeature() : Boolean { return (tag ?: false) as Boolean }


fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.isVisible() : Boolean { return visibility == View.VISIBLE}
fun View.isHidden() : Boolean { return visibility == View.GONE}


//fun Apfloat.sqr() : Apfloat = this.multiply(this)
//fun Apcomplex.sqr() : Apcomplex = this.multiply(this)
//fun Apcomplex.cube() : Apcomplex = this.multiply(this).multiply(this)
//fun Apcomplex.mod() : Apfloat = ApfloatMath.sqrt(this.real().sqr().add(this.imag().sqr()))
//fun Apcomplex.modSqr() : Apfloat = this.real().sqr().add(this.imag().sqr())
// const val AP_DIGITS = 48L




data class Complex(
        val x : Double,
        val y : Double
) {

    companion object {

        val ZERO = Complex(0.0, 0.0)
        val ONE = Complex(1.0, 0.0)
        val i = Complex(0.0, 1.0)

    }


    override fun toString(): String {
        return "($x, $y)"
    }

    fun conj() : Complex{
        return Complex(x, -y)
    }

    fun mod() : Double {
        return sqrt(x*x + y*y)
    }

    operator fun unaryMinus() : Complex {
        return Complex(-x, -y)
    }

    operator fun plus(w: Complex) : Complex {
        return Complex(x + w.x, y + w.y)
    }

    operator fun minus(w: Complex) : Complex {
        return Complex(x - w.x, y - w.y)
    }

    operator fun times(s: Double) : Complex {
        return Complex(s*x, s*y)
    }

    operator fun times(w: Complex) : Complex {
        return Complex(x*w.x - y*w.y, x*w.y + y*w.x)
    }

    operator fun div(s: Double) : Complex {
        return Complex(x/s, y/s)
    }

    operator fun div(w: Complex) : Complex {
        return (this*w.conj())/mod()
    }

}

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


enum class Precision(val bits: Int, val threshold: Double) {
    SINGLE(23, 5e-4), DUAL(46, 1e-12), QUAD(0, 1e-20)
}
enum class Reaction(val numDisplayParams: Int) {
    NONE(0), SHAPE(3), COLOR(2), POSITION(4)
}
enum class Resolution(private val scale: Float, val square: Boolean = false) {
    EIGHTH(1/8f), SIXTH(1/6f), FOURTH(1/4f), HALF(1/2f), FULL(1f), DOUBLE(2f), TEST(2.5f), THUMB(1/6f, true);
    fun scaleRes(screenRes: IntArray) : IntArray {
        return intArrayOf((screenRes[0]*scale).roundToInt(), (screenRes[1]*scale).roundToInt())
    }
    companion object {
        private const val NUM_VALUES_GT_SCREEN_DIMS = 2
        val NUM_VALUES_PRO = values().size - 1
        val NUM_VALUES_FREE = values().size - NUM_VALUES_GT_SCREEN_DIMS - 1
        val HIGHEST = if (BuildConfig.PAID_VERSION) TEST else FULL
    }
}
enum class TextureMode { OUT, IN, BOTH }
enum class ListLayoutType { GRID, LINEAR }


class Position(
        x: Double = 0.0,
        y: Double = 0.0,
        scale: Double = 1.0,
        rotation: Double = 0.0,
        //xap: Apfloat = Apfloat(0.0),
        //yap: Apfloat = Apfloat(0.0),
        xLocked: Boolean = false,
        yLocked: Boolean = false,
        scaleLocked: Boolean = false,
        rotationLocked: Boolean = false
) {

    private val xInit = x
    private val yInit = y
    private val scaleInit = scale
    private val rotationInit = rotation
    private val xLockedInit = xLocked
    private val yLockedInit = yLocked
    private val scaleLockedInit = scaleLocked
    private val rotationLockedInit = rotationLocked


    var x = xInit
        set(value) { if (!xLocked) { field = value } }

    var y = yInit
        set(value) { if (!yLocked) { field = value } }

    var scale = scaleInit
        set(value) { if (!scaleLocked) { field = value } }

    var rotation = rotationInit
        set(value) { if (!rotationLocked) {
            val tau = 2.0*Math.PI
            field = if (value < 0) (tau + value) % tau else value % tau
        }}

    var xLocked = xLockedInit
    var yLocked = yLockedInit
    var scaleLocked = scaleLockedInit
    var rotationLocked = rotationLockedInit



//    val xapInit = xap
//    val yapInit = yap
//    var xap = xapInit
//    var yap = yapInit


    fun clone() : Position {
        return Position(x, y, scale, rotation)
    }

    private fun translate(dx: Double, dy: Double) {

        x += dx
        y += dy

    }
//    private fun translate_ap(dx: Apfloat, dy: Apfloat) {
//
//        xap = xap.add(dx)
//        yap = yap.add(dy)
//
//    }
    fun translate(dx: Float, dy: Float) {  // dx, dy --> [0, 1]

        val tx = dx*scale
        val ty = dy*scale
        val sinTheta = sin(-rotation)
        val cosTheta = cos(rotation)
        x -= tx*cosTheta - ty*sinTheta
        y += tx*sinTheta + ty*cosTheta

//        if (USE_PERTURBATION) {
//            xap = xap.subtract(Apfloat(tx*cosTheta - ty*sinTheta, AP_DIGITS))
//            yap = yap.add(Apfloat(tx*sinTheta + ty*cosTheta, AP_DIGITS))
//        }

    }
    fun scale(dScale: Float, prop: DoubleArray) {

        if (!scaleLocked) {

            // unlock x and y to allow auxiliary transformations
            val xLockedTemp = xLocked
            val yLockedTemp = yLocked
            xLocked = false
            yLocked = false

            // calculate scaling variables
            val qx = prop[0] * scale
            val qy = prop[1] * scale
            val sinTheta = sin(rotation)
            val cosTheta = cos(rotation)
            val fx = x + qx * cosTheta - qy * sinTheta
            val fy = y + qx * sinTheta + qy * cosTheta

            // scale
            translate(-fx, -fy)
            x /= dScale
            y /= dScale
            translate(fx, fy)
//            if (USE_PERTURBATION) {
//
//                val fxap = xap.add(Apfloat(qx * cosTheta - qy * sinTheta, AP_DIGITS))
//                val fyap = yap.add(Apfloat(qx * sinTheta + qy * cosTheta, AP_DIGITS))
//                translate_ap(fxap.negate(), fyap.negate())
//                xap = xap.divide(Apfloat(dScale, AP_DIGITS))
//                yap = yap.divide(Apfloat(dScale, AP_DIGITS))
//                translate_ap(fxap, fyap)
//
//            }


            scale /= dScale

            // set x and y locks to previous values
            xLocked = xLockedTemp
            yLocked = yLockedTemp

        }

    }
    fun rotate(dTheta: Float, prop: DoubleArray) {

        if (!rotationLocked) {

            // unlock x and y to allow auxiliary transformations
            val xLockedTemp = xLocked
            val yLockedTemp = yLocked
            xLocked = false
            yLocked = false

            // calculate rotation variables
            var qx = prop[0] * scale
            var qy = prop[1] * scale
            val sinTheta = sin(rotation)
            val cosTheta = cos(rotation)
            val fx = x + qx * cosTheta - qy * sinTheta
            val fy = y + qx * sinTheta + qy * cosTheta
            val sindTheta = sin(-dTheta)
            val cosdTheta = cos(dTheta)

            // rotate
            translate(-fx, -fy)
            qx = x
            qy = y
            x = qx * cosdTheta - qy * sindTheta
            y = qx * sindTheta + qy * cosdTheta
            translate(fx, fy)


            rotation -= dTheta.toDouble()

            // set x and y locks to previous values
            xLocked = xLockedTemp
            yLocked = yLockedTemp

        }

    }

    fun reset() {
        x = xInit
        y = yInit
        scale = scaleInit
        rotation = rotationInit
        xLocked = xLockedInit
        yLocked = yLockedInit
        scaleLocked = scaleLockedInit
        rotationLocked = rotationLockedInit
    }

}

class PositionList(
        val default  : Position = Position(),
        val julia    : Position = Position(scale = 3.5)
) {

    fun clone() : PositionList {
        return PositionList(
                default.clone(),
                julia.clone()
        )
    }
    fun reset() {

        default.reset()
        julia.reset()

    }

}


fun Double?.inRadians() : Double? = this?.times(Math.PI/180.0)
fun Double?.inDegrees() : Double? = this?.times(180.0/Math.PI)
fun Double.inRadians() : Double = this*Math.PI/180.0
fun Double.inDegrees() : Double = this*180.0/Math.PI


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


fun getColors(res: Resources, ids: List<Int>) : IntArray {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) IntArray(ids.size) { i: Int -> res.getColor(ids[i], null) }
    else IntArray(ids.size) { i: Int -> res.getColor(ids[i]) }
}





interface ClickListener {
    fun onClick(view: View, position: Int)
    fun onLongClick(view: View, position: Int)
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

class RecyclerTouchListener(
        context: Context,
        recyclerView: RecyclerView,
        private val clickListener: ClickListener?
) : RecyclerView.OnItemTouchListener {

    private val gestureDetector: GestureDetector


    init {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                val child = recyclerView.findChildViewUnder(e.x, e.y)
                if (child != null && clickListener != null) {
                    clickListener.onLongClick(child, recyclerView.getChildAdapterPosition(child))
                }
            }

        })
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        val child = rv.findChildViewUnder(e.x, e.y)
        if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
            clickListener.onClick(child, rv.getChildAdapterPosition(child))
            // rv.smoothScrollToPosition(rv.getChildLayoutPosition(child) + 1)
        }
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
}





class MainActivity : AppCompatActivity() {


    var f : Fractal = Fractal.mandelbrot
    var sc : SettingsConfig = SettingsConfig()
    lateinit var fsv : FractalSurfaceView

    // private var orientation = Configuration.ORIENTATION_UNDEFINED

    class ActivityHandler(activity: MainActivity) : Handler() {

        private val MSG_UPDATE_COLOR_THUMBNAILS = 0
        private val MSG_UPDATE_TEXTURE_THUMBNAILS = 1
        private val MSG_IMAGE_SAVED = 2
        private val MSG_UPDATE_PRECISION_BITS = 3

        


        // Weak reference to the Activity; only access this from the UI thread.
        private val mWeakActivity : WeakReference<MainActivity> = WeakReference(activity)

        fun updateColorThumbnails() {
            sendMessage(obtainMessage(MSG_UPDATE_COLOR_THUMBNAILS))
        }
        fun updateTextureThumbnail(index: Int) {
            sendMessage(obtainMessage(MSG_UPDATE_TEXTURE_THUMBNAILS, index))
        }
        fun showImageSavedMessage(dir: String) {
            sendMessage(obtainMessage(MSG_IMAGE_SAVED, dir))
        }
        fun updatePrecisionBits() {
            sendMessage(obtainMessage(MSG_UPDATE_PRECISION_BITS))
        }

        // runs on UI thread
        override fun handleMessage(msg: Message) {
            val what = msg.what
            //Log.d(TAG, "ActivityHandler [" + this + "]: what=" + what);

            val activity = mWeakActivity.get()
            if (activity == null) {
                Log.w("MAIN ACTIVITY", "ActivityHandler.handleMessage: activity is null")
            }

            when(what) {
                MSG_UPDATE_COLOR_THUMBNAILS -> activity?.updateColorThumbnails()
                MSG_UPDATE_TEXTURE_THUMBNAILS -> activity?.updateTextureThumbnail(msg.obj as Int)
                MSG_IMAGE_SAVED -> activity?.showMessage(
                        "${activity.resources.getString(R.string.msg_save_successful)} ${msg.obj}"
                )
                MSG_UPDATE_PRECISION_BITS -> activity?.updatePrecisionBits()
                else -> throw RuntimeException("unknown msg $what")
            }
        }
    }


    enum class Category(val displayName: Int, val icon: Int) {

        SETTINGS(R.string.settings, R.drawable.settings) {
            override fun onCloseMenu(act: MainActivity) {}
            override fun onMenuClosed(act: MainActivity) {
                act.categoryButtons.getTabAt(POSITION).select()
            }
            override fun onCategorySelected(act: MainActivity) {
                val categoryNameButton = act.findViewById<Button>(R.id.categoryNameButton)
                act.fsv.renderProfile = RenderProfile.MANUAL
                act.fsv.reaction = Reaction.NONE
                act.hideTouchIcon()
                if (act.categoryLayoutIsClosed()) categoryNameButton.performClick()
            }
            override fun onCategoryUnselected(act: MainActivity) {

            }
        },
        TEXTURE(R.string.texture, R.drawable.texture) {
            override fun onCloseMenu(act: MainActivity) {}
            override fun onMenuClosed(act: MainActivity) {
                if (act.textureContent.isVisible()) act.categoryButtons.getTabAt(POSITION).select()
            }
            override fun onCategorySelected(act: MainActivity) {
                act.hideTouchIcon()
                act.fsv.reaction = Reaction.NONE
                if (act.categoryLayoutIsClosed()) act.categoryNameButton.performClick()
            }
            override fun onCategoryUnselected(act: MainActivity) {
                // onMenuClosed(act)
            }
        },
        SHAPE(R.string.shape, R.drawable.shape) {
            override fun onCloseMenu(act: MainActivity) {}
            override fun onMenuClosed(act: MainActivity) {
                if (act.shapeContent.isVisible() && act.f.shape.numParamsInUse == 0) act.categoryButtons.getTabAt(POSITION).select()
            }
            override fun onCategorySelected(act: MainActivity) {
                act.fsv.renderProfile = RenderProfile.MANUAL
                if (act.f.shape.numParamsInUse != 0) {
                    act.fsv.reaction = Reaction.SHAPE
                    act.showTouchIcon()
                }
                else {
                    val categoryNameButton = act.findViewById<Button>(R.id.categoryNameButton)
                    act.fsv.reaction = Reaction.NONE
                    act.hideTouchIcon()
                    if (act.categoryLayoutIsClosed()) categoryNameButton.performClick()
                }
            }
            override fun onCategoryUnselected(act: MainActivity) {

            }
        },
        COLOR(R.string.color, R.drawable.color) {
            override fun onCloseMenu(act: MainActivity) {
                with (act) {
//                    when {
//                        colorPreviewListLayout.isVisible() -> colorPreviewListDoneButton.performClick()
//                        customPaletteLayout.isVisible() -> customPaletteDoneButton.performClick()
//                        else -> {}
//                    }
                }
            }
            override fun onMenuClosed(act: MainActivity) {}
            override fun onCategorySelected(act: MainActivity) {
//                if (act.findViewById<LinearLayout>(R.id.colorPreviewListLayout).isVisible()) {
//                    act.fsv.renderProfile = RenderProfile.MANUAL
//                }
//                else {
//                    act.fsv.renderProfile = RenderProfile.COLOR_THUMB
//                    act.fsv.r.renderToTex = true
//                    act.fsv.r.renderThumbnails = true
//                    act.fsv.requestRender()
//                }
                act.fsv.reaction = Reaction.COLOR
                act.showTouchIcon()
            }
            override fun onCategoryUnselected(act: MainActivity) {}
        },
        POSITION(R.string.position, R.drawable.position) {
            override fun onCloseMenu(act: MainActivity) {}
            override fun onMenuClosed(act: MainActivity) {}
            override fun onCategorySelected(act: MainActivity) {
                act.fsv.renderProfile = RenderProfile.MANUAL
                act.fsv.reaction = Reaction.valueOf(name)
                act.showTouchIcon()
            }
            override fun onCategoryUnselected(act: MainActivity) {

            }
        };

        abstract fun onCloseMenu(act: MainActivity)
        abstract fun onMenuClosed(act: MainActivity)
        abstract fun onCategorySelected(act: MainActivity)
        abstract fun onCategoryUnselected(act: MainActivity)

    }


    override fun onCreate(savedInstanceState: Bundle?) {

        // restore SettingsConfig from SharedPreferences
        val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        sc.resolution = Resolution.values()[sp.getInt(RESOLUTION, Resolution.FULL.ordinal)]
        //sc.precision = Precision.values()[sp.getInt(PRECISION, Precision.SINGLE.ordinal)]
        //sc.autoPrecision = sp.getBoolean(AUTO_PRECISION, true)
        sc.continuousRender = sp.getBoolean(CONTINUOUS_RENDER, false)
        sc.displayParams = sp.getBoolean(DISPLAY_PARAMS, false)
        sc.renderBackground = sp.getBoolean(RENDER_BACKGROUND, true)
        sc.fitToViewport = sp.getBoolean(FIT_TO_VIEWPORT, false)
        sc.hideNavBar = sp.getBoolean(HIDE_NAV_BAR, true)
        sc.colorListViewType = ListLayoutType.values()[sp.getInt(COLOR_LIST_VIEW_TYPE, ListLayoutType.GRID.ordinal)]
        sc.shapeListViewType = ListLayoutType.values()[sp.getInt(SHAPE_LIST_VIEW_TYPE, ListLayoutType.GRID.ordinal)]
        sc.textureListViewType = ListLayoutType.values()[sp.getInt(TEXTURE_LIST_VIEW_TYPE, ListLayoutType.GRID.ordinal)]
        //sc.showHints = sp.getBoolean(SHOW_HINTS, true)


        val displayMetrics = baseContext.resources.displayMetrics
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        Log.d("MAIN ACTIVITY", "real metrics: ${displayMetrics.widthPixels}, ${displayMetrics.heightPixels}")
        Log.d("MAIN ACTIVITY", "device has notch : ${deviceHasNotch()}")
        val statusBarHeight = (STATUS_BAR_HEIGHT * resources.displayMetrics.scaledDensity).toInt()
        //val navBarHeight = (NAV_BAR_HEIGHT * resources.displayMetrics.scaledDensity).toInt()
        val navBarHeight = getNavBarHeight()

        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val screenRes = intArrayOf(screenWidth, screenHeight)
        // Log.d("MAIN ACTIVITY", "status bar height : $statusBarHeight")
        Log.d("MAIN ACTIVITY", "screen resolution : ($screenWidth, $screenHeight)")


        val categoryLayoutHeight = resources.getDimension(R.dimen.categoryLayoutHeight).toInt()
        val categoryPagerHeight = resources.getDimension(R.dimen.categoryPagerHeight).toInt()
        val categoryNameButtonHeight = resources.getDimension(R.dimen.categoryNameButtonHeight).toInt()
        val categoryButtonsHeight = resources.getDimension(R.dimen.categoryButtonsHeight).toInt()
        val uiHeightOpen = categoryLayoutHeight + categoryNameButtonHeight
        val uiHeightClosed = uiHeightOpen - categoryLayoutHeight


        ColorPalette.all.forEach { it.initialize(resources, Resolution.THUMB.scaleRes(screenRes)) }


        fsv = FractalSurfaceView(f, sc, this, ActivityHandler(this), screenRes)
        fsv.layoutParams = FrameLayout.LayoutParams(screenWidth, screenHeight)

        val settingsFragment  = SettingsFragment()
        val textureFragment   = TextureFragment()
        val shapeFragment     = ShapeFragment()
        val colorFragment     = ColorFragment()
        val positionFragment  = PositionFragment()


        super.onCreate(savedInstanceState)

//        orientation = baseContext.resources.configuration.orientation
//        Log.d("MAIN ACTIVITY", "orientation: $orientation")
//        val orientationChanged = (savedInstanceState?.getInt("orientation") ?: orientation) != orientation

//        if (orientationChanged) {
//            f.switchOrientation()
//            Log.d("MAIN", "orientation changed")
//        }


        setContentView(R.layout.activity_main)
        fractalLayout.addView(fsv)


        val displayParamRows = listOf<LinearLayout>(
                displayParamRow1,
                displayParamRow2,
                displayParamRow3,
                displayParamRow4
        )
        displayParamRows.forEach { it.visibility = LinearLayout.GONE }
        updateDisplayParams(reactionChanged = true, settingsChanged = true)

        //categoryPager.layoutParams.height = 1
        categoryLayout.layoutParams.height = categoryButtonsHeight
        fsv.updateSystemUI()

        categoryNameButton.setOnClickListener {

            val hStart : Int
            val hEnd : Int

            if (categoryLayoutIsClosed()) {
                hStart = categoryButtonsHeight
                hEnd = categoryLayoutHeight
                (it as Button).setCompoundDrawablesWithIntrinsicBounds(null, null, resources.getDrawable(R.drawable.collapse, null), null)
            }
            else {
                categoryButtons.getCurrentCategory().onCloseMenu(this)
                hStart = categoryLayoutHeight
                hEnd = categoryButtonsHeight
                (it as Button).setCompoundDrawablesWithIntrinsicBounds(null, null, resources.getDrawable(R.drawable.expand, null), null)
            }


            var newHeight = screenHeight.toFloat()
            if (!sc.hideNavBar) newHeight -= navBarHeight
            if (deviceHasNotch()) newHeight -= getStatusBarHeight()


            val anim = ValueAnimator.ofInt(hStart, hEnd)
            anim.addUpdateListener { a ->

                // update categoryPager height
                val intermediateHeight = a?.animatedValue as Int
                //categoryPager.layoutParams.height = intermediateHeight
                //categoryPager.requestLayout()
                categoryLayout.layoutParams.height = intermediateHeight
                categoryLayout.requestLayout()

                // update fsv layout
                val animNewHeight = newHeight -
                        if (hStart == categoryButtonsHeight)
                            (1f - a.animatedFraction)*uiHeightClosed  +  a.animatedFraction*uiHeightOpen
                        else
                            (1f - a.animatedFraction)*uiHeightOpen    +  a.animatedFraction*uiHeightClosed


                val scaleRatio = fsv.screenRes[1].toFloat()/animNewHeight

                if (sc.fitToViewport) {

                    fsv.scaleX = 1f/scaleRatio
                    fsv.scaleY = 1f/scaleRatio
                    fsv.y = -0.5f*(fsv.screenRes[1] - animNewHeight)

                }
                else {
                    var newY = -ui.height
                    newY -= if (sc.hideNavBar) 0 else navBarHeight
                    fsv.y = newY/2f
                }

                if (hEnd == categoryButtonsHeight && anim.animatedFraction == 1f) {  // closing animation ended

                    categoryButtons.getCurrentCategory().onMenuClosed(this)

                }

            }
            anim.duration = PAGER_ANIM_DURATION
            anim.start()

        }


        val categoryPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        categoryPagerAdapter.addFrag(settingsFragment)
        categoryPagerAdapter.addFrag(textureFragment)
        categoryPagerAdapter.addFrag(shapeFragment)
        categoryPagerAdapter.addFrag(colorFragment)
        categoryPagerAdapter.addFrag(positionFragment)
        categoryPager.adapter = categoryPagerAdapter
        categoryPager.offscreenPageLimit = 4


        categoryButtons.setupWithViewPager(categoryPager)
        for (i in 0..4) {
            categoryButtons.getTabAt(i)?.contentDescription = resources.getString(Category.values()[i].displayName).toUpperCase(Locale.getDefault())
            val transparentIcon = resources.getDrawable(Category.values()[i].icon, null)
            transparentIcon.alpha = 128
            categoryButtons.getTabAt(i)?.icon = transparentIcon
        }

        categoryButtons.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {

                tab.icon?.alpha = 255

                val category = Category.values()[tab.position]

                category.onCategorySelected(this@MainActivity)

                categoryNameButton.text = resources.getString(category.displayName)
                updateDisplayParams(reactionChanged = true)

            }
            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.icon?.alpha = 128
                val category = Category.values()[tab.position]
                category.onCategoryUnselected(this@MainActivity)
            }
            override fun onTabReselected(tab: TabLayout.Tab) {
                // categoryNameButton.performClick()
                // onTabSelected(tab)
            }

        })
        categoryButtons.getTabAt(Category.POSITION).select()


        overlay.bringToFront()
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

    }

    private fun categoryLayoutIsOpen() : Boolean {
        return categoryLayout.height == resources.getDimension(R.dimen.categoryLayoutHeight).toInt()
    }
    private fun categoryLayoutIsClosed() : Boolean {
        return categoryLayout.height == resources.getDimension(R.dimen.categoryButtonsHeight).toInt()
    }
    private fun getNavBarHeight() : Int {

        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        val navBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0

        Log.d("MAIN ACTIVITY", "navBarHeight: $navBarHeight")
        return navBarHeight

    }
    private fun getDefaultStatusBarHeight() : Int {

        return (STATUS_BAR_HEIGHT * resources.displayMetrics.scaledDensity).toInt()

    }
    private fun getStatusBarHeight() : Int {

        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0

        Log.d("MAIN ACTIVITY", "statusBarHeight: $statusBarHeight")
        return statusBarHeight

    }
    private fun deviceHasNotch() : Boolean {

        val hasNotch = getStatusBarHeight() > getDefaultStatusBarHeight()
        Log.d("MAIN ACTIVITY", "device has notch: $hasNotch")
        return hasNotch

    }

    fun showMessage(msg: String) {
        val toast = Toast.makeText(baseContext, msg, Toast.LENGTH_LONG)
        val toastHeight = ui.height + resources.getDimension(R.dimen.toastMargin).toInt()
        toast.setGravity(Gravity.BOTTOM, 0, toastHeight)
        toast.show()
    }
    fun updateDisplayParams(reactionChanged: Boolean = false, settingsChanged: Boolean = false) {

        val displayParamRows = listOf<LinearLayout>(
                displayParamRow1,
                displayParamRow2,
                displayParamRow3,
                displayParamRow4
        )

        if (sc.displayParams && fsv.reaction != Reaction.NONE) {

            displayParams.visibility = LinearLayout.VISIBLE
            if (reactionChanged) displayParamRows.forEach { it.visibility = LinearLayout.GONE }
            if (reactionChanged || settingsChanged)
                for (i in 0 until fsv.reaction.numDisplayParams)
                displayParamRows[i].visibility = LinearLayout.VISIBLE

            val density = resources.displayMetrics.density
            val w: Int

            // update text content
            when (fsv.reaction) {
                Reaction.NONE -> {

                    w = (60f * density).toInt()

                }
                Reaction.POSITION -> {

                    displayParamName1.text = resources.getString(R.string.x)
                    displayParamName2.text = resources.getString(R.string.y)
                    displayParamName3.text = resources.getString(R.string.zoom)
                    displayParamName4.text = resources.getString(R.string.rotation)
                    displayParam1.text = "%.17f".format(f.position.x)
                    displayParam2.text = "%.17f".format(f.position.y)
                    displayParam3.text = "%e".format(f.position.scale)
                    displayParam4.text = "%.0f".format(f.position.rotation * 180.0 / Math.PI)
                    w = (60f * density).toInt()

                }
                Reaction.COLOR -> {

                    displayParamName1.text = resources.getString(R.string.frequency)
                    displayParamName2.text = resources.getString(R.string.offset)
                    displayParam1.text = "%.4f".format(f.frequency)
                    displayParam2.text = "%.4f".format(f.phase)
                    w = (75f * density).toInt()

                }
                Reaction.SHAPE -> {

                    displayParamName1.text = "u"
                    displayParamName2.text = "v"
                    displayParamName3.text = resources.getString(R.string.sensitivity)
                    displayParam1.text = "%.8f".format((f.shape.params.active.u))
                    displayParam2.text = "%.8f".format((f.shape.params.active.v))
                    displayParam3.text = "%.6f".format(f.sensitivity)
                    w = (80f * density).toInt()

                }
            }

            // update width
            displayParamName1?.width = w
            displayParamName2?.width = w
            displayParamName3?.width = w
            displayParamName4?.width = w
            displayParamName1.requestLayout()
            displayParamName2.requestLayout()
            displayParamName3?.requestLayout()
            displayParamName4?.requestLayout()
            displayParam1?.requestLayout()
            displayParam2?.requestLayout()
            displayParam3?.requestLayout()
            displayParam4?.requestLayout()

            // start fade animation
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 1000L
            fadeOut.startOffset = 2500L
            fadeOut.fillAfter = true
            displayParams.animation = fadeOut
            displayParams.animation.start()
            displayParams.requestLayout()

        }
        else {
            if (settingsChanged) { displayParams.visibility = LinearLayout.GONE }
        }

    }
    fun showTouchIcon() {

        // start fade animation
        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.duration = 1000L
        fadeOut.startOffset = 2000L
        fadeOut.fillAfter = true
        touchIcon.animation = fadeOut
        touchIcon.animation.start()
        touchIcon.requestLayout()

    }
    fun hideTouchIcon() {

        // start fade animation
        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.duration = 0L
        fadeOut.startOffset = 0L
        fadeOut.fillAfter = true
        touchIcon.animation = fadeOut
        touchIcon.animation.start()
        touchIcon.requestLayout()

    }
    fun recalculateSurfaceViewLayout() {

        // val navBarHeight = (NAV_BAR_HEIGHT * resources.displayMetrics.scaledDensity).toInt()
        val navBarHeight = getNavBarHeight()


        if (sc.fitToViewport) {

            val categoryLayoutHeight = resources.getDimension(R.dimen.categoryLayoutHeight).toInt()
            val categoryNameButtonHeight = resources.getDimension(R.dimen.categoryNameButtonHeight).toInt()
            val uiHeightOpen = categoryLayoutHeight + categoryNameButtonHeight
            val uiHeightClosed = uiHeightOpen - categoryLayoutHeight

            Log.d("MAIN ACTIVITY", "categoryPagerHeight: ${categoryPager.layoutParams.height}")

            var newHeight = fsv.screenRes[1]
            newHeight -= if (categoryLayoutIsClosed()) uiHeightClosed else uiHeightOpen
            if (!sc.hideNavBar) newHeight -= navBarHeight
            if (deviceHasNotch()) newHeight -= getStatusBarHeight()

            val scaleRatio = fsv.screenRes[1].toFloat()/newHeight
            fsv.scaleX = 1f/scaleRatio
            fsv.scaleY = 1f/scaleRatio
            fsv.y = -0.5f*(fsv.screenRes[1] - newHeight)

        }
        else {

            fsv.scaleX = 1f
            fsv.scaleY = 1f

            var newY = -ui.height
            if (!sc.hideNavBar) newY -= navBarHeight
            fsv.y = newY/2f

        }

    }
    fun hideCategoryButtons() { categoryButtons.hide() }
    fun showCategoryButtons() { categoryButtons.show() }

    fun updateShapeEditTexts() {
        // Log.d("FRACTAL", "updating shape param EditText $i")

        uEdit?.setText("%.8f".format((f.shape.params.active.u)))
        vEdit?.setText("%.8f".format((f.shape.params.active.v)))

    }
    fun updateTextureEditTexts() {

        val bailoutStrings = "%e".format(Locale.US, f.bailoutRadius).split("e")
        bailoutSignificandEdit?.setText("%.5f".format(bailoutStrings[0].toFloat()))
        bailoutExponentEdit?.setText("%d".format(bailoutStrings[1].toInt()))

        //qEdit?.setText(f.texture.params[0].toString())

    }
    fun updateColorEditTexts() {

        frequencyEdit?.setText("%.5f".format(f.frequency))
        phaseEdit?.setText("%.5f".format(f.phase))

    }
    fun updatePositionEditTexts() {

        xEdit?.setText("%.17f".format(f.position.x))
        yEdit?.setText("%.17f".format(f.position.y))

        val scaleStrings = "%e".format(Locale.US, f.position.scale).split("e")
        scaleSignificandEdit?.setText("%.5f".format(scaleStrings[0].toFloat()))
        scaleExponentEdit?.setText("%d".format(scaleStrings[1].toInt()))

        rotationEdit?.setText("%d".format(f.position.rotation.inDegrees().roundToInt()))

    }
    fun updateTexturePreviewName() {
        texturePreviewName.text = resources.getString(f.texture.name)
    }
    fun updateHintVisibility() {

        val hints = listOf<TextView>(
                continuousRenderHint,
                renderBackgroundHint,
                displayParamsHint,
                fitViewportHint
//                bailoutHint
//                juliaModeHint,
//                maxIterHint,
//                phaseHint,
//                frequencyHint
        )
        hints.forEach { it.visibility = if (sc.showHints) TextView.VISIBLE else TextView.GONE }

    }
    fun updatePrecisionBits() {

        precisionBitsText?.text = "${sc.precision.bits}-bit"

    }

    fun updateColorThumbnails() {

        colorPreviewList?.adapter?.notifyDataSetChanged()

    }
    fun updateTextureThumbnail(index: Int) {

        // Log.e("MAIN ACTIVITY", "updateTextureThumbnail was called !!!")
        texturePreviewList?.adapter?.notifyItemChanged(index)

    }


    override fun onPause() {

        Log.d("MAIN ACTIVITY", "activity paused ...")

        val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        val edit = sp.edit()
        edit.putInt(RESOLUTION, sc.resolution.ordinal)
        //edit.putInt(PRECISION, sc.precision.ordinal)
        //edit.putBoolean(AUTO_PRECISION, sc.autoPrecision)
        edit.putBoolean(CONTINUOUS_RENDER, sc.continuousRender)
        edit.putBoolean(DISPLAY_PARAMS, sc.displayParams)
        edit.putBoolean(RENDER_BACKGROUND, sc.renderBackground)
        edit.putBoolean(FIT_TO_VIEWPORT, sc.fitToViewport)
        edit.putBoolean(HIDE_NAV_BAR, sc.hideNavBar)
        //edit.putBoolean(SHOW_HINTS, sc.showHints)
        edit.putInt(COLOR_LIST_VIEW_TYPE, sc.colorListViewType.ordinal)
        edit.putInt(SHAPE_LIST_VIEW_TYPE, sc.shapeListViewType.ordinal)
        edit.putInt(TEXTURE_LIST_VIEW_TYPE, sc.textureListViewType.ordinal)
        edit.apply()

        super.onPause()
        fsv.onPause()

    }
    override fun onResume() {
        super.onResume()
        fsv.onResume()
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        Log.d("MAIN ACTIVITY", "window focus changed")
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) fsv.updateSystemUI()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            WRITE_STORAGE_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    showMessage(resources.getString(R.string.msg_save_enabled))
                } else {
                    showMessage(resources.getString(R.string.msg_save_failed))
                }
                return
            }
            else -> {}
        }
    }


}
