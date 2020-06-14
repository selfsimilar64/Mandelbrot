package com.selfsimilartech.fractaleye

import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Point
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.michaelflisar.changelog.ChangelogBuilder
import com.michaelflisar.changelog.classes.ImportanceChangelogSorter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.color_fragment.*
import kotlinx.android.synthetic.main.position_fragment_old.*
import kotlinx.android.synthetic.main.settings_fragment.*
import kotlinx.android.synthetic.main.shape_fragment.*
import kotlinx.android.synthetic.main.texture_fragment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apfloat.Apcomplex
import org.apfloat.Apfloat
import org.apfloat.ApfloatMath
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.*


const val MAX_SHAPE_PARAMS = 4
const val MAX_TEXTURE_PARAMS = 2
const val WRITE_STORAGE_REQUEST_CODE = 0
const val ITER_MAX_POW = 16.0
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
const val AUTOFIT_COLOR_RANGE = "autofitColorRange"
const val HARDWARE_PROFILE = "hardwareProfile"
const val GPU_PRECISION = "gpuPrecision"
const val CPU_PRECISION = "cpuPrecision"
const val PALETTE = "palette"
const val VERSION_NAME_TAG = "versionName"
const val SHARED_PREFERENCES = "com.selfsimilartech.fractaleye.SETTINGS"


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
fun now() = System.currentTimeMillis()


fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.isVisible() : Boolean { return visibility == View.VISIBLE}
fun View.isHidden() : Boolean { return visibility == View.GONE}


fun Apfloat.sqr() : Apfloat = this.multiply(this)
fun Apcomplex.sqr() : Apcomplex = this.multiply(this)
fun Apcomplex.cube() : Apcomplex = this.multiply(this).multiply(this)
fun Apcomplex.mod() : Apfloat = ApfloatMath.sqrt(this.real().sqr().add(this.imag().sqr()))
fun Apcomplex.modSqr() : Apfloat = this.real().sqr().add(this.imag().sqr())
const val AP_DIGITS = 64L




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


enum class GpuPrecision(val bits: Int, val threshold: Double) {
    SINGLE(23, 5e-4), DUAL(46, 1e-12)
}
enum class CpuPrecision(val threshold: Double) {
    DOUBLE(1e-12), PERTURB(1e-200)
}
enum class Reaction(val numDisplayParams: Int) {
    NONE(0), SHAPE(3), COLOR(2), POSITION(4)
}
enum class Resolution(private val scale: Float, val square: Boolean = false) {
    SXTNTH(1/16f), EIGHTH(1/8f), FOURTH(1/4f), HALF(1/2f), R34(3/4f), FULL(1f),
    THREEHALVES(1.5f), DOUBLE(2f), FIVEHALVES(2.5f),
    // TRIPLE(3f), SEVENHALVES(3.5f), QUAD(4f),   // RG16F largeheap only
    THUMB(1/6f, true);
    fun scaleRes(screenRes: Point) : Point {
        return Point(
                (screenRes.x*scale).roundToInt(),
                (screenRes.y*scale).roundToInt())
    }
    companion object {
        private const val NUM_VALUES_GT_SCREEN_DIMS = 3
        val NUM_VALUES_PRO = values().size - 1
        val NUM_VALUES_FREE = values().size - NUM_VALUES_GT_SCREEN_DIMS - 1
        val HIGHEST = if (BuildConfig.PAID_VERSION) FIVEHALVES else FULL
    }
}
enum class TextureMode { OUT, IN, BOTH }
enum class ListLayoutType { GRID, LINEAR }


class Position(
        x: Double = 0.0,
        y: Double = 0.0,
        zoom: Double = 1.0,
        rotation: Double = 0.0,
        xap: Apfloat = Apfloat(x.toString(), 32L),
        yap: Apfloat = Apfloat(y.toString(), 32L),
        var ap: Long = 32L,
        xLocked: Boolean = false,
        yLocked: Boolean = false,
        scaleLocked: Boolean = false,
        rotationLocked: Boolean = false
) {

    private val xInit = x
    private val yInit = y
    private val zoomInit = zoom
    private val rotationInit = rotation
    private val xLockedInit = xLocked
    private val yLockedInit = yLocked
    private val scaleLockedInit = scaleLocked
    private val rotationLockedInit = rotationLocked


    var x = xInit
        set(value) { if (!xLocked) { field = value } }

    var y = yInit
        set(value) { if (!yLocked) { field = value } }

    var zoom = zoomInit
        set(value) { if (!scaleLocked) { field = value } }

    var rotation = rotationInit
        set(value) { if (!rotationLocked) {
            field = when {
                value <= Math.PI -> ((value - Math.PI).rem(2.0*Math.PI)) + Math.PI
                value > Math.PI -> ((value + Math.PI).rem(2.0*Math.PI)) - Math.PI
                else -> value
            }

        }}

    var xLocked = xLockedInit
    var yLocked = yLockedInit
    var scaleLocked = scaleLockedInit
    var rotationLocked = rotationLockedInit



    private val xapInit = xap
    private val yapInit = yap
    var xap = xapInit
    var yap = yapInit


    fun clone() : Position {
        return Position(x, y, zoom, rotation)
    }

    private fun translate(dx: Double, dy: Double) {

        x += dx
        y += dy

    }
    private fun translateAp(dx: Apfloat, dy: Apfloat) {

        xap = xap.add(dx)
        yap = yap.add(dy)

    }
    fun translate(dx: Float, dy: Float) {  // dx, dy --> [0, 1]

        val tx = dx*zoom
        val ty = dy*zoom
        val sinTheta = sin(-rotation)
        val cosTheta = cos(rotation)
        x -= tx*cosTheta - ty*sinTheta
        y += tx*sinTheta + ty*cosTheta

        xap = xap.subtract(Apfloat(tx*cosTheta - ty*sinTheta, ap))
        yap = yap.add(Apfloat(tx*sinTheta + ty*cosTheta, ap))

    }
    fun zoom(dZoom: Float, prop: DoubleArray) {

        if (!scaleLocked) {

            // unlock x and y to allow auxiliary transformations
            val xLockedTemp = xLocked
            val yLockedTemp = yLocked
            xLocked = false
            yLocked = false

            // calculate scaling variables
            val qx = prop[0] * zoom
            val qy = prop[1] * zoom
            val sinTheta = sin(rotation)
            val cosTheta = cos(rotation)
            val fx = x + qx * cosTheta - qy * sinTheta
            val fy = y + qx * sinTheta + qy * cosTheta

            // scale
            translate(-fx, -fy)
            x /= dZoom
            y /= dZoom
            translate(fx, fy)

            val fxap = xap.add(Apfloat(qx * cosTheta - qy * sinTheta, ap))
            val fyap = yap.add(Apfloat(qx * sinTheta + qy * cosTheta, ap))
            translateAp(fxap.negate(), fyap.negate())
            xap = xap.divide(Apfloat(dZoom, ap))
            yap = yap.divide(Apfloat(dZoom, ap))
            translateAp(fxap, fyap)


            zoom /= dZoom

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
            var qx = prop[0] * zoom
            var qy = prop[1] * zoom
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


            qx = prop[0] * zoom
            qy = prop[1] * zoom
            val fxap = xap.add(Apfloat(qx*cosTheta - qy*sinTheta, ap))
            val fyap = yap.add(Apfloat(qx*sinTheta + qy*cosTheta, ap))
            translateAp(fxap.negate(), fyap.negate())
            val qxap = xap
            val qyap = yap
            val sindThetaAp = Apfloat(sindTheta, ap)
            val cosdThetaAp = Apfloat(cosdTheta, ap)
            xap = qxap.multiply(cosdThetaAp).subtract(qyap.multiply(sindThetaAp))
            yap = qxap.multiply(sindThetaAp).add(qyap.multiply(cosdThetaAp))
            translateAp(fxap, fyap)


            rotation -= dTheta.toDouble()

            // set x and y locks to previous values
            xLocked = xLockedTemp
            yLocked = yLockedTemp

        }

    }

    fun reset() {
        x = xInit
        y = yInit
        zoom = zoomInit
        rotation = rotationInit
        xLocked = xLockedInit
        yLocked = yLockedInit
        scaleLocked = scaleLockedInit
        rotationLocked = rotationLockedInit
    }
    fun updatePrecision(newPrecision: Long) {

        //Log.e("MAIN ACTIVITY", "new position precision: $newPrecision")
        ap = newPrecision
        xap = Apfloat(xap.toString(), ap)
        yap = Apfloat(yap.toString(), ap)

    }

}

class PositionList(
        val default  : Position = Position(),
        val julia    : Position = Position(zoom = 3.5)
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


//    private val hexArray = "0123456789abcdef".toCharArray()
//    private lateinit var mTcpClient : TcpClient
//    private lateinit var ftpclient : MyFTPClient
//    private val handlerUi = Handler()


    lateinit var db : AppDatabase
    var f : Fractal = Fractal.mandelbrot
    var sc : SettingsConfig = SettingsConfig()
    lateinit var fsv : FractalSurfaceView
    private var screenWidth = 0
    private var screenHeight = 0
    private var navBarHeight = 0
    private var statusBarHeight = 0
    private var deviceHasNotch = false
    private var texturesDisabled = false

    private lateinit var settingsFragment : Fragment
    private lateinit var textureFragment : Fragment
    private lateinit var shapeFragment : Fragment
    private lateinit var colorFragment : Fragment
    private lateinit var positionFragment : Fragment

    // private var orientation = Configuration.ORIENTATION_UNDEFINED

    class ActivityHandler(activity: MainActivity) : Handler() {

        private val MSG_UPDATE_COLOR_THUMBNAILS = 0
        private val MSG_UPDATE_TEXTURE_THUMBNAILS = 1
        private val MSG_IMAGE_SAVED = 2
        private val MSG_ERROR = 3

        


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
        fun showErrorMessage() {
            sendMessage(obtainMessage(MSG_ERROR))
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
                MSG_ERROR -> activity?.showMessage(
                        activity.resources.getString(R.string.msg_error)
                )
                else -> throw RuntimeException("unknown msg $what")
            }
        }
    }


    enum class Category(val displayName: Int, val icon: Int) {

        SETTINGS(R.string.settings, R.drawable.settings) {
            override fun onOpenMenu(act: MainActivity) {
                onCategorySelected(act)
            }
            override fun onCloseMenu(act: MainActivity) {}
            override fun onMenuClosed(act: MainActivity) {
                act.categoryButtons.getTabAt(POSITION).select()
            }
            override fun onCategorySelected(act: MainActivity) {
                //val categoryNameButton = act.findViewById<Button>(R.id.categoryNameButton)
                //act.fsv.renderProfile = RenderProfile.MANUAL
                act.apply {
                    fsv.r.reaction = Reaction.NONE
                    hideTouchIcon()
                    if (renderOptionsLayout.isVisible() || displayOptionsLayout.isVisible()) uiSetOpenTall()
                    else if (uiIsClosed()) uiSetOpen()
                }
            }
            override fun onCategoryUnselected(act: MainActivity) {
                act.apply {
                    if (uiIsOpen() && (renderOptionsLayout?.isVisible() == true || displayOptionsLayout?.isVisible() == true)) {
                        uiSetHeight(resources.getDimension(R.dimen.uiLayoutHeight).toInt())
                    }
                }
            }
        },
        TEXTURE(R.string.texture, R.drawable.texture) {
            override fun onOpenMenu(act: MainActivity) {
                act.apply {
                    if (texturePreviewListLayout.isVisible()) uiSetOpenTall()
                    else uiSetOpen()
                }
            }
            override fun onCloseMenu(act: MainActivity) {}
            override fun onMenuClosed(act: MainActivity) {
                if (!act.texturePreviewListLayout.isVisible()) act.categoryButtons.getTabAt(POSITION).select()
            }
            override fun onCategorySelected(act: MainActivity) {
                if (!act.texturesDisabled) {
                    act.hideTouchIcon()
                    act.fsv.r.reaction = Reaction.NONE
                    if (act.uiIsClosed()) act.categoryNameButton.performClick()
                }
            }
            override fun onCategoryUnselected(act: MainActivity) {
                // onMenuClosed(act)
            }
        },
        SHAPE(R.string.shape, R.drawable.shape) {
            override fun onOpenMenu(act: MainActivity) {
                act.apply {
                    if (shapePreviewListLayout.isVisible()) uiSetOpenTall()
                    else uiSetOpen()
                }
            }
            override fun onCloseMenu(act: MainActivity) {}
            override fun onMenuClosed(act: MainActivity) {
                act.apply {
                    if (fsv.r.reaction == Reaction.NONE && !shapePreviewListLayout.isVisible()) categoryButtons.getTabAt(POSITION).select()
                }
            }
            override fun onCategorySelected(act: MainActivity) {
                //act.fsv.renderProfile = RenderProfile.MANUAL
                if (act.complexParamLayout.isVisible() or act.realParamLayout.isVisible()) {
                    act.fsv.r.reaction = Reaction.SHAPE
                    act.showTouchIcon()
                }
                else {
                    val categoryNameButton = act.findViewById<Button>(R.id.categoryNameButton)
                    act.fsv.r.reaction = Reaction.NONE
                    act.hideTouchIcon()
                    if (act.uiIsClosed()) categoryNameButton.performClick()
                }
            }
            override fun onCategoryUnselected(act: MainActivity) {

            }
        },
        COLOR(R.string.color, R.drawable.color) {
            override fun onOpenMenu(act: MainActivity) {
                act.apply {
                    if (colorPreviewListLayout.isVisible()) uiSetOpenTall()
                    else uiSetOpen()
                }
            }
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
                act.fsv.r.reaction = Reaction.COLOR
                act.showTouchIcon()
            }
            override fun onCategoryUnselected(act: MainActivity) {}
        },
        POSITION(R.string.position, R.drawable.position) {
            override fun onOpenMenu(act: MainActivity) {
                act.apply {
                    uiSetOpen()
                }
            }
            override fun onCloseMenu(act: MainActivity) {}
            override fun onMenuClosed(act: MainActivity) {}
            override fun onCategorySelected(act: MainActivity) {
                //act.fsv.renderProfile = RenderProfile.MANUAL
                act.fsv.r.reaction = Reaction.valueOf(name)
                act.showTouchIcon()
            }
            override fun onCategoryUnselected(act: MainActivity) {

            }
        };

        abstract fun onOpenMenu(act: MainActivity)
        abstract fun onCloseMenu(act: MainActivity)
        abstract fun onMenuClosed(act: MainActivity)
        abstract fun onCategorySelected(act: MainActivity)
        abstract fun onCategoryUnselected(act: MainActivity)

    }



    override fun onCreate(savedInstanceState: Bundle?) {

        db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "palettes"
        ).fallbackToDestructiveMigration().build()

        // restore SettingsConfig from SharedPreferences
        val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)

        val savedResolution = sp.getInt(RESOLUTION, Resolution.FULL.ordinal)
        sc.resolution = Resolution.values()[min(savedResolution, Resolution.FULL.ordinal)]
        //sc.resolution = Resolution.FOURTH
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
        sc.autofitColorRange = sp.getBoolean(AUTOFIT_COLOR_RANGE, true)
        //sc.showHints = sp.getBoolean(SHOW_HINTS, true)

        val displayMetrics = baseContext.resources.displayMetrics
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        //Log.d("MAIN ACTIVITY", "real metrics: ${displayMetrics.widthPixels}, ${displayMetrics.heightPixels}")
        //Log.d("MAIN ACTIVITY", "device has notch : ${calcDeviceHasNotch()}")
        //val statusBarHeight = (STATUS_BAR_HEIGHT * resources.displayMetrics.scaledDensity).toInt()
        //val navBarHeight = (NAV_BAR_HEIGHT * resources.displayMetrics.scaledDensity).toInt()

        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        val screenRes = Point(screenWidth, screenHeight)
        // Log.d("MAIN ACTIVITY", "status bar height : $statusBarHeight")
        //Log.d("MAIN ACTIVITY", "screen resolution : ($screenWidth, $screenHeight)")


        val categoryLayoutHeight = resources.getDimension(R.dimen.uiLayoutHeight).toInt()
        val categoryPagerHeight = resources.getDimension(R.dimen.categoryPagerHeight).toInt()
        val categoryNameButtonHeight = resources.getDimension(R.dimen.categoryNameButtonHeight).toInt()
        val categoryButtonsHeight = resources.getDimension(R.dimen.menuButtonsHeight).toInt()
        val uiHeightOpen = categoryLayoutHeight + categoryNameButtonHeight
        val uiHeightClosed = uiHeightOpen - categoryLayoutHeight


        ColorPalette.all.forEach {
            it.initialize(resources, Resolution.THUMB.scaleRes(screenRes))
        }


        val r = FractalRenderer(f, sc, this, baseContext, ActivityHandler(this), screenRes)
        fsv = FractalSurfaceView(baseContext, r)
        fsv.layoutParams = FrameLayout.LayoutParams(screenWidth, screenHeight)

        settingsFragment  = SettingsFragment()
        textureFragment   = TextureFragment()
        shapeFragment     = ShapeFragment()
        colorFragment     = ColorFragment()
        positionFragment  = PositionFragment()


        super.onCreate(savedInstanceState)


//        mTcpClient = TcpClient(this@MainActivity, TcpClient.OnMessageReceived { message ->
//            Log.d("message", "messageReceived: $message")
//            when {
//                message.contains("Hallo") -> {
//                    sendRenderscriptMessage("dev", "28712990mjk")
//                }
//                message.contains("Succesful") -> {
//                    /*
//                         * the next code is needed in the following situation
//                         * the remember user option is selected but the user has not yet logged in via the login button
//                         * or during a previous session.
//                         * If we receive the Succesful message from the server,we can deduce that the send username en password
//                         * were correct and therefore can be stored to the shared preferences file
//                         */
////                        if (settings.getBoolean("rememberUser", false)) {
////                            if (username !== "" && passwd !== "") {
////                                val editor: Editor = settings.edit()
////                                editor.putString("userName", username)
////                                editor.putString("passwd", passwd)
////                                editor.commit()
////                            }
////                        }
//                    Log.d("MAIN", "Build Succesful")
//                    mTcpClient.sendMessage("give bc\n")
//                    Log.d("MAIN", "give bc")
//                }
//                message.contains("UPLOADED") -> {
//
//                    //Connect to ftp server and fetch the file.
//                        GlobalScope.launch {
//
//                            var status = false
////                            Log.i("MainAct", "FtpThread")
////                            // Replace your UID & PW here
////                            Log.i("ftp", "ftp connect with $username $passwd")
////                            if (settings.getBoolean("UseDefault", true)) {
////                                MainActivity.IP_ADDR = MainActivity.DEFAULT_IP_ADDR
////                            } else {
////                                MainActivity.IP_ADDR = settings.getString("ServerIP", MainActivity.DEFAULT_IP_ADDR)
////                            }
//                            status = ftpclient.ftpConnect(resources.getString(R.string.defaultIP), "dev", "28712990mjk", 21)
//                            if (status) {
//                                Log.d("FTP", "Connection Success")
//                                status = ftpclient.ftpDownload("/bc64/template.bc", filesDir.path + "/template.bc")  // may need to change with Android 10 file access
//                                if (status) {
//                                    Log.e("MAIN", "download success!")
//                                }
//                                else {
//                                    Log.e("MAIN", "download failed :(")
//                                }
//                            } else {
//                                Log.e("FTP", "Connection failed")
//                                //createToast("Connection with TCP server failed!", false);
//                            }
//
//                        }
//
//                }
//                message.contains("login ok") -> {
//                    Log.d("MAIN", "login_ok")
//                }
//                message.contains("login error") -> {
//                    Log.d("MAIN", "login_nok")
//                }
//                message.contains("acount error") -> {
//                    Log.d("MAIN", "account_error")
//                }
//                message.contains("acount created") -> {
//                    Log.d("MAIN", "acount_created")
//                }
//                else -> {
////                        if (settings.getBoolean("rememberUser", false)) {
////                            if (username !== "" && passwd !== "") {
////                                val editor: Editor = settings.edit()
////                                editor.putString("userName", username)
////                                editor.putString("passwd", passwd)
////                                editor.commit()
////                            }
////                        }
//                    Log.d("MAIN", "Error message: $message")
//                }
//            }
//        })
//        ftpclient = MyFTPClient()



//        orientation = baseContext.resources.configuration.orientation
//        Log.d("MAIN ACTIVITY", "orientation: $orientation")
//        val orientationChanged = (savedInstanceState?.getInt("orientation") ?: orientation) != orientation

//        if (orientationChanged) {
//            f.switchOrientation()
//            Log.d("MAIN", "orientation changed")
//        }


        setContentView(R.layout.activity_main)
        fractalLayout.addView(fsv)


        deviceHasNotch = calcDeviceHasNotch()
        navBarHeight = calcNavBarHeight()
        statusBarHeight = calcStatusBarHeight()
        updateSurfaceViewLayout(resources.getDimension(R.dimen.uiLayoutHeightClosed))


        val layoutList = listOf(
                fractalLayout,
                overlay,
                ui,
                uiInnerLayout,
                uiInnerCard,
                categoryPager
                //textureLayout
        )
        layoutList.forEach {
            it.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        }


        val displayParamRows = listOf<LinearLayout>(
                displayParamRow1,
                displayParamRow2,
                displayParamRow3,
                displayParamRow4
        )
        displayParamRows.forEach { it.visibility = LinearLayout.GONE }
        updateDisplayParams(reactionChanged = true, settingsChanged = true)

        uiInnerLayout.layoutParams.height = categoryButtonsHeight
        updateSystemUI()


        categoryNameButton.setOnClickListener {

            if (!uiIsClosed()) uiSetClosed() else categoryButtons.getCurrentCategory().onOpenMenu(this)

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
            val transparentIcon = resources.getDrawable(Category.values()[i].icon, null)
            transparentIcon.alpha = 128
            categoryButtons.getTabAt(i)?.apply {
                contentDescription = resources.getString(Category.values()[i].displayName).toUpperCase(Locale.getDefault())
                //text = resources.getString(Category.values()[i].displayName)
                icon = transparentIcon
            }
        }

        categoryButtons.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {

                val category = Category.values()[tab.position]
                //Log.d("MAIN ACTIVITY", "category ${category.name}")

                if (!(category == Category.TEXTURE && texturesDisabled)) {
                    tab.icon?.alpha = 255


                    category.onCategorySelected(this@MainActivity)

                    categoryNameButton.text = resources.getString(category.displayName)
                    updateDisplayParams(reactionChanged = true)

                }

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


        // load custom color palettes
        GlobalScope.launch {
            if (BuildConfig.PAID_VERSION) {
                db.colorPaletteDao().apply {
                    //getAll().forEach { delete(it) }
                    getAll().forEach {
                        Log.d("COLOR", "custom palette size: ${it.size}")
                        ColorPalette.all.add(0, ColorPalette(
                                name = it.name,
                                colors = ArrayList(arrayListOf(it.c1, it.c2, it.c3, it.c4, it.c5, it.c6, it.c7, it.c8).slice(0 until it.size)),
                                customId = it.id
                        ))
                        ColorPalette.all[0].initialize(resources, Resolution.THUMB.scaleRes(screenRes))
                    }
                }
            }
            f.palette = ColorPalette.all[sp.getInt(PALETTE, ColorPalette.all.indexOf(ColorPalette.night))]
        }


        overlay.bringToFront()
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        if (sp.getString(VERSION_NAME_TAG, "") != BuildConfig.VERSION_NAME) showChangelog()

    }




    private fun uiSetClosed() {
        uiSetHeight(resources.getDimension(R.dimen.uiLayoutHeightClosed).toInt())
    }
    fun uiSetOpen() {
        uiSetHeight(resources.getDimension(R.dimen.uiLayoutHeight).toInt())
    }
    fun uiSetOpenTall() {
        uiSetHeight(resources.getDimension(R.dimen.uiLayoutHeightTall).toInt())
    }
    fun uiIsOpen() : Boolean {
        return !uiIsClosed()
    }
    fun uiIsClosed() : Boolean {
        return uiInnerLayout.layoutParams.height == resources.getDimension(R.dimen.uiLayoutHeightClosed).toInt()
    }
    fun uiSetHeight(newHeight: Int) {


        if (newHeight != uiInnerLayout.layoutParams.height) {

            uiInnerLayout.layoutParams.height = newHeight
            uiInnerLayout.requestLayout()

            categoryNameButton.setCompoundDrawablesWithIntrinsicBounds(
                    null, null,
                    if (newHeight == resources.getDimension(R.dimen.uiLayoutHeightClosed).toInt())
                        resources.getDrawable(R.drawable.expand, null)
                    else
                        resources.getDrawable(R.drawable.collapse, null),
                    null
            )

            val anim = ValueAnimator.ofFloat(0f, 1f)
            anim.duration = uiInnerLayout.layoutTransition.getDuration(LayoutTransition.CHANGING) + 75L
            anim.addUpdateListener {

                updateSurfaceViewLayout(uiInnerLayout.height.toFloat())

                if (anim.animatedFraction == 1f) {
                    if (newHeight == resources.getDimension(R.dimen.uiLayoutHeightClosed).toInt()) {
                        categoryButtons.getCurrentCategory().onMenuClosed(this)
                    }
                }

            }
            anim.start()

        }

    }
    fun updateSurfaceViewLayout(height: Float? = null) {

        var y = -(height ?: uiInnerLayout.height.toFloat())
        y -= if (!sc.fitToViewport) categoryNameButton.height else 0
        y -= if (!sc.hideNavBar) navBarHeight else 0
        y -= if (deviceHasNotch) statusBarHeight else 0
        y -= if (sc.fitToViewport) resources.getDimension(R.dimen.categoryNameButtonHeight).toInt() else 0
        val scaleRatio = screenHeight.toFloat()/(screenHeight + y)

        y /= 2f
        fsv.y = y

        fsv.scaleX = if (sc.fitToViewport) 1f/scaleRatio else 1f
        fsv.scaleY = if (sc.fitToViewport) 1f/scaleRatio else 1f

        fsv.requestLayout()

    }
    private fun calcNavBarHeight() : Int {

        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        val navBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0

        //Log.d("MAIN ACTIVITY", "navBarHeight: $navBarHeight")
        return navBarHeight

    }
    private fun getDefaultStatusBarHeight() : Int {

        return (STATUS_BAR_HEIGHT * resources.displayMetrics.scaledDensity).toInt()

    }
    private fun calcStatusBarHeight() : Int {

        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0

        //Log.d("MAIN ACTIVITY", "statusBarHeight: $statusBarHeight")
        return statusBarHeight

    }
    private fun calcDeviceHasNotch() : Boolean {

        val hasNotch = calcStatusBarHeight() > getDefaultStatusBarHeight()
        //Log.d("MAIN ACTIVITY", "device has notch: $hasNotch")
        return hasNotch

    }
    fun updateSystemUI() {

        updateSurfaceViewLayout(uiInnerLayout.height.toFloat())
        if (sc.hideNavBar) {
            fsv.systemUiVisibility = (
                    GLSurfaceView.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            // Set the content to appear under the system bars so that the
                            // content doesn't resize when the system bars hide and show.
                            or GLSurfaceView.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or GLSurfaceView.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or GLSurfaceView.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            // Hide the nav bar and status bar
                            or GLSurfaceView.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or GLSurfaceView.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
        else {
            window.navigationBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                resources.getColor(R.color.menu2, null) else resources.getColor(R.color.menu2)
            fsv.systemUiVisibility = (
                    GLSurfaceView.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or GLSurfaceView.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or GLSurfaceView.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or GLSurfaceView.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }

    }


    fun showMessage(msg: String) {
        val toast = Toast.makeText(baseContext, msg, Toast.LENGTH_LONG)
        val toastHeight = ui.height + resources.getDimension(R.dimen.toastMargin).toInt()
        toast.setGravity(Gravity.BOTTOM, 0, toastHeight)
        toast.show()
    }
    fun disableTextures() {
        texturePreviewName.alpha = 0.3f
        texturePreviewLayout.isClickable = false
        texturePreviewLayout.isFocusable = false
    }
    fun enableTextures() {
        texturePreviewName.alpha = 1f
        texturePreviewLayout.isClickable = true
        texturePreviewLayout.isFocusable = true
        texturePreviewButton.performClick()
    }
    fun updateDisplayParams(reactionChanged: Boolean = false, settingsChanged: Boolean = false) {

        val displayParamRows = listOf<LinearLayout>(
                displayParamRow1,
                displayParamRow2,
                displayParamRow3,
                displayParamRow4
        )

        if (sc.displayParams && fsv.r.reaction != Reaction.NONE) {

            displayParams.visibility = LinearLayout.VISIBLE
            if (reactionChanged) displayParamRows.forEach { it.visibility = LinearLayout.GONE }
            if (reactionChanged || settingsChanged)
                for (i in 0 until fsv.r.reaction.numDisplayParams)
                displayParamRows[i].visibility = LinearLayout.VISIBLE

            val density = resources.displayMetrics.density
            val w: Int

            // update text content
            when (fsv.r.reaction) {
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
                    displayParam3.text = "%e".format(f.position.zoom)
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

                    val param = f.shape.params.active
                    displayParamName1.text = "u"
                    displayParam1.text = "%.8f".format((param.u))
                    if (param is Shape.ComplexParam) {
                        displayParamName2.text = "v"
                        displayParam2.text = "%.8f".format((param.v))
                    }
                    displayParamName3.text = resources.getString(R.string.sensitivity)
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
    fun hideCategoryButtons() { categoryButtons.hide() }
    fun showCategoryButtons() { categoryButtons.show() }
    fun toggleCategoryButtons() {
        if (categoryButtons.isHidden()) showCategoryButtons()
        else hideCategoryButtons()
    }

    fun updateShapeEditTexts() {
        // Log.d("FRACTAL", "updating shape param EditText $i")

        (shapeFragment as ShapeFragment).loadActiveParam()


    }
    fun updateTextureEditTexts() {

        val bailoutStrings = "%e".format(Locale.US, f.bailoutRadius).split("e")
        bailoutSignificandEdit?.setText("%.5f".format(bailoutStrings[0].toFloat()))
        bailoutExponentEdit?.setText("%d".format(bailoutStrings[1].toInt()))

        //qEdit?.setText(f.texture.params[0].toString())

    }
    fun updateColorEditTexts() {

        frequencyEdit?.setText("%.5f".format(f.frequency))
        with (colorFragment as ColorFragment) {
            updateFrequencyLayout()
            updatePhaseLayout()
        }

        phaseEdit?.setText("%.5f".format(f.phase))

    }
    fun updatePositionEditTexts() {

        xEdit?.setText("%.17f".format(f.position.x))
        yEdit?.setText("%.17f".format(f.position.y))

        val scaleStrings = "%e".format(Locale.US, f.position.zoom).split("e")
        scaleSignificandEdit?.setText("%.5f".format(scaleStrings[0].toFloat()))
        scaleExponentEdit?.setText("%d".format(scaleStrings[1].toInt()))

        with (positionFragment as PositionFragment) {
            updateRotationLayout()
        }

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

    fun updateColorThumbnails() {

        colorPreviewList?.adapter?.notifyDataSetChanged()

    }
    fun updateTextureThumbnail(index: Int) {

        // Log.e("MAIN ACTIVITY", "updateTextureThumbnail was called !!!")
        texturePreviewList?.adapter?.notifyItemChanged(index)

    }


    override fun onBackPressed() {
        //super.onBackPressed()
    }
    override fun onPause() {

        //Log.d("MAIN ACTIVITY", "activity paused ...")

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
        edit.putBoolean(AUTOFIT_COLOR_RANGE, sc.autofitColorRange)
        edit.putInt(PALETTE, ColorPalette.all.indexOf(f.palette))
        edit.putString(VERSION_NAME_TAG, BuildConfig.VERSION_NAME)
        edit.apply()

        super.onPause()
        fsv.onPause()

    }
    override fun onResume() {
        super.onResume()
        fsv.onResume()
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        //Log.d("MAIN ACTIVITY", "window focus changed")
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) updateSystemUI()
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
    fun showChangelog() {

        val showAsDialog = true
        val bulletList = true
        val showVersion11OrHigherOnly = false
        val rowsShouldInheritFilterTextFromReleaseTag = false
        val managed = false
        val useCustomRenderer = false
        val useSorter = false
        val rateButton = false
        val showSummmary = false

        // Changelog
        val builder: ChangelogBuilder = ChangelogBuilder() // Everything is optional!
                .withUseBulletList(bulletList) // default: false
                .withManagedShowOnStart(managed) // default: false
                .withMinVersionToShow(if (showVersion11OrHigherOnly) 110 else -1) // default: -1, will show all version
                .withSorter(if (useSorter) ImportanceChangelogSorter() else null) // default: null, will show the logs in the same order as they are in the xml file
                .withRateButton(rateButton) // default: false
                .withSummary(showSummmary, true) // default: false

        // finally, show the dialog or the activity
        builder.buildAndShowDialog(this, false)

    }

    fun onRateButtonClicked(): Boolean {
        Toast.makeText(this, "Rate button was clicked", Toast.LENGTH_LONG).show()
        // button click handled
        return true
    }



    //    /*! \brief Message for the server used for the runtime compilation of RenderScript
//	 *
//	 * This function will be called when the user clicks on the submitbutton with the RenderScript Radiobutton selected.<br>
//	 * The message, beginning with the STARTPACKAGE tag, will contain the username and the hashed password. This is send
//	 * over TCP to the server, followed by the code from the code field in the app, which is send line per line. The end
//	 * of the message is indicated with the ENDPACKAGE tag.
//	 *
//	 * @param username name of the user
//	 * @param passwd the password of the user
//	 */
//    private fun sendRenderscriptMessage(username: String, passwd: String) {
//
//        if (mTcpClient.isConnected) {
//
//            Log.d("MAIN", "Processing. Please wait...")
//            val message = """
//#pragma version(1)
//#pragma rs java_package_name(com.selfsimilartech.fractaleye)
//#pragma rs_fp_full
//
//double width;
//double height;
//double aspectRatio;
//double bgScale;
//
//uint32_t maxIter;
//float escapeRadius;
//
//double scale;
//double xCoord;
//double yCoord;
//double sinRotation;
//double cosRotation;
//
//
//
//float2 RS_KERNEL iterate(uint32_t x, uint32_t y) {
//
//    // send update progress message at regular intervals
//    if (x == 0 && y % (int)(height/50) == 0) { rsSendToClient(0); }
//
//    float2 color;
//
//    // [-1.0, 1.0]
//    double u = 2.0*(x/width) - 1.0;
//    double v = 2.0*(y/height) - 1.0;
//
//    double aAux = u*scale;
//    double bAux = v*scale*aspectRatio;
//    double a = aAux*cosRotation - bAux*sinRotation + xCoord;
//    double b = aAux*sinRotation + bAux*cosRotation + yCoord;
//    complex c = a + b*I;
//
//
//
//
//    //complex z = 0.0 + 0.0*I;
//    //complex z1 = 0.0 + 0.0*I;
//    double zModSqr = 0.0;
//
//
//    for (int n = 0; n <= maxIter; n++) {
//
//
//        // ${'$'} SHAPE LOOP ${'$'}
//        //z = z*z + c;
//
//        //zModSqr = z.x*z.x + z.y*z.y;
//
//        if (zModSqr > escapeRadius*escapeRadius) {
//            color = (float2) { (float)n / (float)maxIter, 0.f };
//            break;
//        }
//        else if (n == maxIter) {
//            color = (float2) { 1.f, 1.f };
//        }
//
//
//        z1 = z;
//
//    }
//
//
//    return color;
//
//
//}
//
//                """
//
//            val lines = message.split("\\r?\\n").toTypedArray()
//            val strHash: String = createHash(passwd)
//            Log.i("send after conversion", strHash)
//            mTcpClient.sendMessage("STARTPACKAGE $username $passwd 24\n")
//            for (i in lines.indices) {
//                mTcpClient.sendMessage(lines[i])
//                Log.d("MAIN", lines[i])
//            }
//
//            //separator so that the code and the ENDPACKAGE message cannot be linked
//            mTcpClient.sendMessage("\n")
//
//            //wait some time
//            handlerUi.postDelayed({
//                GlobalScope.launch {
//
//                    mTcpClient.sendMessage("ENDPACKAGE\n")
//                    Log.i("ENDPACKAGE", "ENDPACKAGE")
//
//                }
//
//            }, 1000)
//
//        } else Log.e("MAIN", "Not connected")
//
//    }
//
//    /*! \brief Creates a MD5 hash of the password
//	 *
//	 * Converts the password to a MD5 hash, used for security reasons. Because the Hash consists of hex values
//	 * who need to be send over TCP, it's necessary to convert the byte array to a String. The resulting string
//	 * will be two times as long because each byte in hex is represented as two characters e.g. 01, 0A etc.
//	 * @param passwd The password used to create the hash value
//	 * @return strHash The resulting hash as a hex presented string
//	 */
//    private fun createHash(passwd: String): String { //create hash
//        var bytesOfMessage: ByteArray? = null
//        try {
//            bytesOfMessage = passwd.toByteArray(charset("UTF-8"))
//        } catch (e: UnsupportedEncodingException) {
//            e.printStackTrace()
//        }
//        var md: MessageDigest? = null
//        try {
//            md = MessageDigest.getInstance("MD5")
//        } catch (e: NoSuchAlgorithmException) {
//            e.printStackTrace()
//        }
//        val hash = md!!.digest(bytesOfMessage!!)
//        return byteArrayToHex(hash)
//    }
//
//    /* \brief Converts bytes to their hex value
//	 *
//	 * @param a is the array of bytes to be converted
//	 * @return Returns the String form of the bytes
//	 */
//    private fun byteArrayToHex(a: ByteArray): String {
//        val sb = StringBuilder()
//        for (b in a) sb.append(String.format("%02x", b and 0xff.toByte()))
//        return sb.toString()
//    }
//
//    /* \brief Converts byte to its hex value
//	 *
//	 * @param bytes are the bytes to be converted
//	 * @return Returns the String form of the byte
//	 */
//    private fun bytesToHex(bytes: ByteArray): String {
//        val hexChars = CharArray(bytes.size * 2)
//        for (j in bytes.indices) {
//            val v = (bytes[j] and 0xFF.toByte()).toUByte()
//            Log.e("MAIN", "v: $v")
//            hexChars[j * 2] = hexArray[v.toInt() ushr 4]
//            hexChars[j * 2 + 1] = hexArray[(v and 0x0F.toUByte()).toInt()]
//        }
//        return String(hexChars)
//    }
//
//    fun connectToServer() {
//
//        GlobalScope.launch {
//            Log.e("MAIN", "doing in background...")
//            mTcpClient.run()
//        }
//
//    }




}
