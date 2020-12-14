package com.selfsimilartech.fractaleye

import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.viewpager.widget.ViewPager
import com.android.billingclient.api.*
import com.google.android.material.tabs.TabLayout
import com.michaelflisar.changelog.ChangelogBuilder
import com.michaelflisar.changelog.classes.ImportanceChangelogSorter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.color_fragment.*
import kotlinx.android.synthetic.main.position_fragment_old.*
import kotlinx.android.synthetic.main.settings_fragment.*
import kotlinx.android.synthetic.main.shape_fragment.*
import kotlinx.android.synthetic.main.texture_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apfloat.Apcomplex
import org.apfloat.Apfloat
import org.apfloat.ApfloatMath
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.*


const val MAX_SHAPE_PARAMS = 4
const val MAX_TEXTURE_PARAMS = 4
const val WRITE_STORAGE_REQUEST_CODE = 0
const val ITER_MAX_POW = 16.0
const val ITER_MIN_POW = 0.0
const val BUTTON_CLICK_DELAY_SHORT = 100L
const val BUTTON_CLICK_DELAY_MED = 300L
const val BUTTON_CLICK_DELAY_LONG = 400L
const val PAGER_ANIM_DURATION = 200L
const val STATUS_BAR_HEIGHT = 24
const val NAV_BAR_HEIGHT = 48

const val GOLD_ENABLED_DIALOG_SHOWN = "goldEnabledDialogShown"
const val GOLD_PENDING_DIALOG_SHOWN = "goldPendingDialogShown"
const val SHOW_EPILEPSY_DIALOG = "showEpilepsyDialog"

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
const val SOLID_FILL_COLOR = "solidFillColor"

const val CHUNK_PROFILE = "chunkProfile"
const val VERSION_CODE_TAG = "versionCode"
const val SHARED_PREFERENCES = "com.selfsimilartech.fractaleye.SETTINGS"

const val PALETTE_TABLE_NAME = "palette"
const val SHAPE_TABLE_NAME = "shape"


//const val PLUS_UNICODE = '\u002B'
//const val MINUS_UNICODE = '\u2212'



operator fun Double.times(w: Complex) : Complex {
    return Complex(this * w.x, this * w.y)
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
fun View.invisible() { visibility = View.INVISIBLE }
fun View.isVisible() : Boolean { return visibility == View.VISIBLE}
fun View.isInvisible() : Boolean { return visibility == View.INVISIBLE }
fun View.isHidden() : Boolean { return visibility == View.GONE}

fun Button.disable() {
    foregroundTintList = ColorStateList.valueOf(Color.GRAY)
    isClickable = false
    isFocusable = false
}
fun Button.enable() {
    foregroundTintList = null
    isClickable = true
    isFocusable = true
}


fun Apfloat.sqr() : Apfloat = this.multiply(this)
fun Apcomplex.sqr() : Apcomplex = this.multiply(this)
fun Apcomplex.cube() : Apcomplex = this.multiply(this).multiply(this)
fun Apcomplex.mod() : Apfloat = ApfloatMath.sqrt(this.real().sqr().add(this.imag().sqr()))
fun Apcomplex.modSqr() : Apfloat = this.real().sqr().add(this.imag().sqr())
const val AP_DIGITS = 64L


enum class GpuPrecision(val bits: Int, val threshold: Double) {
    SINGLE(23, 5e-4), DUAL(46, 1e-12)
}
enum class CpuPrecision(val threshold: Double) {
    DOUBLE(1e-12), PERTURB(1e-200)
}
enum class Reaction(val numDisplayParams: Int) {
    NONE(0), SHAPE(3), TEXTURE(3), COLOR(2), POSITION(4)
}
enum class ChunkProfile(val fgSingle: Int, val bgSingle: Int, val fgDual: Int, val bgDual: Int, val fgContSingle: Int, val fgContDual: Int) {
//    LOW  ( 1,   1,   1,    1,   1,  1 ),
    LOW(10, 3, 30, 10, 1, 1),
    MED(20, 5, 50, 15, 1, 1),
    HIGH(40, 10, 100, 20, 1, 1)
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
                value <= Math.PI -> ((value - Math.PI).rem(2.0 * Math.PI)) + Math.PI
                value > Math.PI -> ((value + Math.PI).rem(2.0 * Math.PI)) - Math.PI
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

        xap = xap.subtract(Apfloat(tx * cosTheta - ty * sinTheta, ap))
        yap = yap.add(Apfloat(tx * sinTheta + ty * cosTheta, ap))

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
            val fxap = xap.add(Apfloat(qx * cosTheta - qy * sinTheta, ap))
            val fyap = yap.add(Apfloat(qx * sinTheta + qy * cosTheta, ap))
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
        val default: Position = Position(),
        val julia: Position = Position(zoom = 3.5)
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


fun Double?.inRadians() : Double? = this?.times(Math.PI / 180.0)
fun Double?.inDegrees() : Double? = this?.times(180.0 / Math.PI)
fun Double.inRadians() : Double = this*Math.PI/180.0
fun Double.inDegrees() : Double = this*180.0/Math.PI
fun Float.inRadians() : Float = this*Math.PI.toFloat()/180f
fun Float.inDegrees() : Float = this*180f/Math.PI.toFloat()


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
fun DoubleArray.mult(s: Double) : DoubleArray {
    return DoubleArray(this.size) { i: Int -> s*this[i]}
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


fun split(a: Double) : PointF {
    val hi = a.toFloat()
    val lo = (a - hi.toDouble()).toFloat()
    return PointF(hi, lo)
}


class MainActivity : AppCompatActivity(), OnCompleteListener {


//    private val hexArray = "0123456789abcdef".toCharArray()
//    private lateinit var mTcpClient : TcpClient
//    private lateinit var ftpclient : MyFTPClient
//    private val handlerUi = Handler()

    var fragmentsCompleted = 0

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
    private var goldEnabledDialogShown = false
    private var goldPendingDialogShown = false
    private var showEpilepsyDialog = true

    private lateinit var settingsFragment : Fragment
    private lateinit var textureFragment : Fragment
    private lateinit var shapeFragment : Fragment
    private lateinit var colorFragment : Fragment
    private lateinit var positionFragment : Fragment


    private lateinit var billingClient : BillingClient
    private val billingClientStateListener = object : BillingClientStateListener {

        override fun onBillingSetupFinished(billingResult: BillingResult) {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> queryPurchases()
            }
        }

        override fun onBillingServiceDisconnected() {
            // Try to restart the connection on the next request to
            // Google Play by calling the startConnection() method.
        }

    }

    private val purchaseUpdateListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {

                    }
                    BillingClient.BillingResponseCode.USER_CANCELED -> {
                        // Handle an error caused by a user cancelling the purchase flow.
                    }
                    else -> {
                        // Handle any other error codes.
                    }
                }
            }



    // private var orientation = Configuration.ORIENTATION_UNDEFINED

    class ActivityHandler(activity: MainActivity) : Handler() {

        private val MSG_UPDATE_COLOR_THUMBNAILS = 0
        private val MSG_UPDATE_TEXTURE_THUMBNAILS = 1
        private val MSG_UPDATE_SHAPE_THUMBNAILS = 4
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
        fun updateShapeThumbnail(shape: Shape, customIndex: Int?) {
            sendMessage(obtainMessage(MSG_UPDATE_SHAPE_THUMBNAILS, customIndex ?: -1, -1, shape))
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
                MSG_UPDATE_SHAPE_THUMBNAILS -> activity?.updateShapeThumbnail(msg.obj as Shape, msg.arg1)
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
                if (act.fsv.r.reaction == Reaction.NONE && !act.texturePreviewListLayout.isVisible()) act.categoryButtons.getTabAt(POSITION).select()
            }
            override fun onCategorySelected(act: MainActivity) {

                if (!act.texturesDisabled) {
                    if (act.realTextureParam.isVisible() or act.complexTextureParam.isVisible()) {
                        act.fsv.r.reaction = Reaction.TEXTURE
                        act.showTouchIcon()
                    } else {
                        val categoryNameButton = act.findViewById<Button>(R.id.categoryNameButton)
                        act.fsv.r.reaction = Reaction.NONE
                        act.hideTouchIcon()
                        if (act.uiIsClosed()) categoryNameButton.performClick()
                    }
                }

            }
            override fun onCategoryUnselected(act: MainActivity) {
                // onMenuClosed(act)
            }
        },
        SHAPE(R.string.shape, R.drawable.shape) {
            override fun onOpenMenu(act: MainActivity) {
                act.apply {
                    if (shapePreviewListLayout.isVisible() || customShapeLayout.isVisible()) uiSetOpenTall()
                    else uiSetOpen()
                }
            }
            override fun onCloseMenu(act: MainActivity) {}
            override fun onMenuClosed(act: MainActivity) {
                act.apply {
                    if (fsv.r.reaction == Reaction.NONE && !(shapePreviewListLayout.isVisible() || customShapeLayout.isVisible())) categoryButtons.getTabAt(POSITION).select()
                }
            }
            override fun onCategorySelected(act: MainActivity) {
                //act.fsv.renderProfile = RenderProfile.MANUAL
                if (act.realShapeParam.isVisible() or act.complexShapeParam.isVisible()) {
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
                    if (colorPreviewListLayout.isVisible() || customPaletteLayout.isVisible()) uiSetOpenTall()
                    else uiSetOpen()
                }
            }
            override fun onCloseMenu(act: MainActivity) {
//                with(act) {
//                    when {
//                        colorPreviewListLayout.isVisible() -> colorPreviewListDoneButton.performClick()
//                        customPaletteLayout.isVisible() -> customPaletteDoneButton.performClick()
//                        else -> {}
//                    }
//                }
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
                // act.fsv.r.renderProfile = RenderProfile.MANUAL
                act.fsv.r.reaction = Reaction.POSITION
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

        //setTheme(R.style.AppTheme)

        val migrate2to3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE palette ADD COLUMN starred INTEGER DEFAULT 0 not null")
            }
        }
        val migrate3to4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE palette ADD COLUMN c9 INTEGER DEFAULT 0 not null")
                database.execSQL("ALTER TABLE palette ADD COLUMN c10 INTEGER DEFAULT 0 not null")
                database.execSQL("ALTER TABLE palette ADD COLUMN c11 INTEGER DEFAULT 0 not null")
                database.execSQL("ALTER TABLE palette ADD COLUMN c12 INTEGER DEFAULT 0 not null")
            }
        }
        db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "custom"
        ).fallbackToDestructiveMigrationFrom(1).addMigrations(migrate2to3, migrate3to4).build()


        billingClient = BillingClient.newBuilder(this)
                .setListener(purchaseUpdateListener)
                .enablePendingPurchases()
                .build()
        //billingClient.startConnection(billingClientStateListener)


        // establish screen dimensions
        val displayMetrics = baseContext.resources.displayMetrics
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        //Log.d("MAIN ACTIVITY", "real metrics: ${displayMetrics.widthPixels}, ${displayMetrics.heightPixels}")
        //Log.d("MAIN ACTIVITY", "device has notch : ${calcDeviceHasNotch()}")
        //val statusBarHeight = (STATUS_BAR_HEIGHT * resources.displayMetrics.scaledDensity).toInt()
        //val navBarHeight = (NAV_BAR_HEIGHT * resources.displayMetrics.scaledDensity).toInt()

        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        val screenRes = Point(screenWidth, screenHeight)
        val screenRatio = screenHeight.toDouble()/screenWidth
        // Log.d("MAIN ACTIVITY", "status bar height : $statusBarHeight")
        Log.d("MAIN ACTIVITY", "screen resolution : ($screenWidth, $screenHeight), ratio : $screenRatio")


        // set screen resolution
        // create and insert new resolution if different from preloaded resolutions
        if (Resolution.all.none { it.size.x == screenWidth }) Resolution.addResolution(screenWidth)
        Resolution.SCREEN = Resolution.valueOf(screenWidth) ?: Resolution.R1080
        Resolution.initialize(screenRatio)


        // restore SettingsConfig from SharedPreferences
        val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)

        goldEnabledDialogShown = sp.getBoolean(GOLD_ENABLED_DIALOG_SHOWN, false)
        goldPendingDialogShown = sp.getBoolean(GOLD_PENDING_DIALOG_SHOWN, false)

        showEpilepsyDialog = sp.getBoolean(SHOW_EPILEPSY_DIALOG, true)

        // val maxStartupRes = if (sc.goldEnabled) Resolution.SCREEN else Resolution.R1080
        val savedResolution = sp.getInt(RESOLUTION, Resolution.all.indexOf(Resolution.R1080))
        sc.resolution = Resolution.all[savedResolution]
        //sc.resolution = Resolution.FOURTH
        //sc.precision = Precision.values()[sp.getInt(PRECISION, Precision.SINGLE.ordinal)]
        //sc.autoPrecision = sp.getBoolean(AUTO_PRECISION, true)
        sc.continuousPosRender = sp.getBoolean(CONTINUOUS_RENDER, false)
        sc.displayParams = sp.getBoolean(DISPLAY_PARAMS, false)
        sc.renderBackground = sp.getBoolean(RENDER_BACKGROUND, true)
        sc.fitToViewport = sp.getBoolean(FIT_TO_VIEWPORT, false)
        sc.hideNavBar = sp.getBoolean(HIDE_NAV_BAR, true)
        sc.colorListViewType = ListLayoutType.values()[sp.getInt(COLOR_LIST_VIEW_TYPE, ListLayoutType.GRID.ordinal)]
        sc.shapeListViewType = ListLayoutType.values()[sp.getInt(SHAPE_LIST_VIEW_TYPE, ListLayoutType.GRID.ordinal)]
        sc.textureListViewType = ListLayoutType.values()[sp.getInt(TEXTURE_LIST_VIEW_TYPE, ListLayoutType.GRID.ordinal)]
        sc.autofitColorRange = sp.getBoolean(AUTOFIT_COLOR_RANGE, true)
        f.solidFillColor = sp.getInt(SOLID_FILL_COLOR, Color.WHITE)
        sc.chunkProfile = ChunkProfile.values()[sp.getInt(CHUNK_PROFILE, 1)]
        //sc.showHints = sp.getBoolean(SHOW_HINTS, true)



        val categoryLayoutHeight = resources.getDimension(R.dimen.uiLayoutHeight).toInt()
        val categoryPagerHeight = resources.getDimension(R.dimen.categoryPagerHeight).toInt()
        val categoryNameButtonHeight = resources.getDimension(R.dimen.categoryNameButtonHeight).toInt()
        val categoryButtonsHeight = resources.getDimension(R.dimen.menuButtonHeight).toInt()
        val uiHeightOpen = categoryLayoutHeight + categoryNameButtonHeight
        val uiHeightClosed = uiHeightOpen - categoryLayoutHeight


        // initialize default palettes and shapes
        val usResources = getLocalizedResources(applicationContext, Locale.US)
        ColorPalette.default.forEach {
            it.isFavorite = sp.getBoolean(
                    "Palette${usResources.getString(it.nameId).replace(" ", "")}Starred", false
            )
            it.initialize(resources, Resolution.THUMB.size)
        }
        Shape.default.forEach {
            it.initialize(resources)
            it.isFavorite = sp.getBoolean(
                    "Shape${usResources.getString(it.nameId).replace(" ", "")}Starred", false
            )
        }
        Texture.all.forEach {
            it.initialize(resources, Resolution.THUMB.size)
            it.isFavorite = sp.getBoolean(
                    "Texture${usResources.getString(it.nameId).replace(" ", "")}Starred", false
            )
        }

        ListHeader.all.forEach { it.initialize(resources) }



        Log.e("MAIN", "available heap size in MB: ${getAvailableHeapMemory()}")



        val r = FractalRenderer(f, sc, this, baseContext, ActivityHandler(this), screenRes)
        fsv = FractalSurfaceView(baseContext, r)
        fsv.layoutParams = FrameLayout.LayoutParams(screenWidth, screenHeight)

        settingsFragment  = SettingsFragment()
        textureFragment   = TextureFragment()
        shapeFragment     = ShapeFragment()
        colorFragment     = ColorFragment()
        positionFragment  = PositionFragment()


        super.onCreate(savedInstanceState)


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
                // uiInnerCard,
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

                tab.icon?.alpha = 255

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


        // load custom palettes and shapes
        GlobalScope.launch {

            // this is great!! thank you aha!!
            // get next autogenerate id values
            val query = "SELECT * FROM SQLITE_SEQUENCE"
            val cursor: Cursor = db.query(query, null)
            if (cursor.moveToFirst()) {
                do {
                    val tableName = cursor.getString(cursor.getColumnIndex("name"))
                    val nextId = cursor.getString(cursor.getColumnIndex("seq"))
                    when (tableName) {
                        PALETTE_TABLE_NAME -> ColorPalette.nextCustomPaletteNum = nextId.toInt() + 1
                        SHAPE_TABLE_NAME -> Shape.nextCustomShapeNum = nextId.toInt() + 1
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()


            db.colorPaletteDao().apply {
                getAll().forEach {
                    ColorPalette.custom.add(0, ColorPalette(
                            name = it.name,
                            colors = ArrayList(arrayListOf(it.c1, it.c2, it.c3, it.c4, it.c5, it.c6, it.c7, it.c8).slice(0 until it.size)),
                            customId = it.id,
                            isFavorite = it.starred
                    ))
                    ColorPalette.custom[0].initialize(resources, Resolution.THUMB.size)
                    Log.e("MAIN", "custom palette ${ColorPalette.custom[0].name}, id: ${ColorPalette.custom[0].customId}")
                }
            }

            ColorPalette.all.addAll(0, ColorPalette.custom)
            f.palette = ColorPalette.all[sp.getInt(PALETTE, ColorPalette.all.indexOf(ColorPalette.night))]


            db.shapeDao().apply {
                getAll().forEach {
                    Shape.custom.add(0, Shape(
                            name = it.name,
                            customId = it.id,
                            latex = it.latex,
                            loop = "customshape_loop(z1, c)",
                            conditional = it.conditional,
                            positions = PositionList(
                                    default = Position(
                                            x = it.xPosDefault,
                                            y = it.yPosDefault,
                                            zoom = it.zoomDefault,
                                            rotation = it.rotationDefault
                                    ),
                                    julia = Position(
                                            x = it.xPosJulia,
                                            y = it.yPosJulia,
                                            zoom = it.zoomJulia,
                                            rotation = it.rotationJulia
                                    )
                            ),
                            juliaMode = it.juliaMode,
                            juliaSeed = it.juliaSeed,
                            seed = Complex(it.xSeed, it.ySeed),
                            maxIter = it.maxIter,
                            radius = it.bailoutRadius,
                            isConvergent = it.isConvergent,
                            hasDualFloat = it.hasDualFloat,
                            customLoopSF = it.loopSF,
                            customLoopDF = it.loopDF,
                            isFavorite = it.isFavorite
                    ))
                    Shape.custom[0].initialize(resources, Resolution.THUMB.size)
                    Log.e("MAIN", "custom shape ${Shape.custom[0].name}, id: ${Shape.custom[0].customId}")
                }
            }
            Shape.all.addAll(0, Shape.custom)

        }


        overlay.bringToFront()
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        if (showEpilepsyDialog) {
            val dialogView = layoutInflater.inflate(R.layout.alert_dialog_custom, null)
            val checkBox = dialogView?.findViewById<CheckBox>(R.id.dontShowCheckBox)
            checkBox?.setOnCheckedChangeListener { buttonView, isChecked ->
                showEpilepsyDialog = !isChecked
            }
            AlertDialog.Builder(this, R.style.AlertDialogCustom)
                    .setView(dialogView)
                    .setIcon(R.drawable.warning)
                    .setTitle(R.string.epilepsy_title)
                    .setMessage(R.string.epilepsy_dscript)
                    .setPositiveButton(android.R.string.ok, null)
                    .setOnDismissListener {
                        if (sp.getInt(VERSION_CODE_TAG, 0) != BuildConfig.VERSION_CODE) showChangelog()
                    }
                    .show()
        }
        else {
            if (sp.getInt(VERSION_CODE_TAG, 0) != BuildConfig.VERSION_CODE) showChangelog()
        }

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

        if (fsv.r.isRendering) fsv.r.pauseRender = true
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
                resources.getColor(R.color.menuDark1, null) else resources.getColor(R.color.menuDark1)
            fsv.systemUiVisibility = (
                    GLSurfaceView.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or GLSurfaceView.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or GLSurfaceView.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or GLSurfaceView.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }

    }


    fun showMessage(msg: String) {
        runOnUiThread {
            val toast = Toast.makeText(baseContext, msg, Toast.LENGTH_LONG)
            val toastHeight = ui.height + resources.getDimension(R.dimen.toastMargin).toInt()
            toast.setGravity(Gravity.BOTTOM, 0, toastHeight)
            toast.show()
        }
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
        textureListButton.performClick()
    }
    fun updateDisplayParams(reactionChanged: Boolean = false, settingsChanged: Boolean = false) {

        val displayParamRows = listOf<LinearLayout>(
                displayParamRow1,
                displayParamRow2,
                displayParamRow3,
                displayParamRow4
        )

        if (sc.displayParams && fsv.r.reaction != Reaction.NONE) {

            displayParams.show()
            if (reactionChanged) displayParamRows.forEach { it.hide() }
            if (reactionChanged || settingsChanged)
                for (i in 0 until fsv.r.reaction.numDisplayParams)
                displayParamRows[i].show()

            val density = resources.displayMetrics.density
            val w: Int

            // update text content
            when (fsv.r.reaction) {
                Reaction.NONE-> {

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
                    displayParamName2.text = resources.getString(R.string.phase)
                    displayParam1.text = "%.4f".format(f.frequency)
                    displayParam2.text = "%.4f".format(f.phase)
                    w = (75f * density).toInt()

                }
                Reaction.SHAPE, Reaction.TEXTURE -> {

                    val param =
                            (if (fsv.r.reaction == Reaction.SHAPE) f.shape.params.active
                             else                                  f.texture.activeParam)
                            .run { if (isRateParam) parent!! else this }
                    displayParamName1.text = "u"
                    displayParam1.text = "%.8f".format((param.u))
                    if (param is ComplexParam) {
                        displayParamName2.text = "v"
                        displayParam2.text = "%.8f".format((param.v))
                    }
                    else {
                        displayParamRow2.hide()
                    }
                    displayParamName3.text = resources.getString(R.string.sensitivity)
                    displayParam3.text = "%.6f".format(param.sensitivity!!.u)
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

//        Log.e("MAIN", "bailout: ${"%e".format(Locale.US, f.bailoutRadius)}")

        (textureFragment as TextureFragment).loadActiveParam()

        //qEdit?.setText(f.texture.params[0].toString())

    }
    fun updateColorEditTexts() {

        frequencyEdit?.setText("%.5f".format(f.frequency))
        with(colorFragment as ColorFragment) {
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

        with(positionFragment as PositionFragment) {
            updateRotationLayout()
        }

    }
    fun updateTexturePreviewName() {
        texturePreviewName.text = resources.getString(f.texture.nameId)
    }
    fun updateBailoutRadiusLayout() {



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

        (colorPreviewList?.adapter as? ListAdapter<ColorPalette>)?.notifyDataSetChanged()

    }
    fun updateTextureThumbnail(index: Int) {

        // Log.e("MAIN ACTIVITY", "updateTextureThumbnail was called !!!")
        texturePreviewList?.adapter?.notifyItemChanged(index)

    }
    fun updateShapeThumbnail(shape: Shape, customIndex: Int) {

        (shapePreviewList?.adapter as? ListAdapter<Shape>)?.updateItems(shape)
        if (customIndex != -1) { with(shapeFragment as ShapeFragment) {
            Log.e("MAIN", "customIndex: $customIndex")
            val numShapes = Shape.custom.size
            updateDialog(customIndex, numShapes)
            if (customIndex + 1 == numShapes) dismissDialog()
        }}

    }


    override fun onBackPressed() {
        //super.onBackPressed()
    }
    override fun onPause() {

        Log.d("MAIN", "activity paused ...")

        // update any changes made to custom palettes
        GlobalScope.launch {
            db.colorPaletteDao().apply {
                ColorPalette.custom.forEach {
                    Log.e("MAIN", "saving custom palette ${it.name}, starred= ${it.isFavorite}")
                    update(it.toDatabaseEntity())
                }
            }
        }

        val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        val edit = sp.edit()
        edit.putBoolean(GOLD_ENABLED_DIALOG_SHOWN, goldEnabledDialogShown)
        edit.putBoolean(GOLD_PENDING_DIALOG_SHOWN, goldPendingDialogShown)
        edit.putBoolean(SHOW_EPILEPSY_DIALOG, showEpilepsyDialog)
        edit.putInt(RESOLUTION, min(Resolution.all.indexOf(sc.resolution), if (sc.goldEnabled) Resolution.all.indexOf(Resolution.SCREEN) else Resolution.all.indexOf(Resolution.R1080)))
        //edit.putInt(PRECISION, sc.precision.ordinal)
        //edit.putBoolean(AUTO_PRECISION, sc.autoPrecision)
        edit.putBoolean(CONTINUOUS_RENDER, sc.continuousPosRender)
        edit.putBoolean(DISPLAY_PARAMS, sc.displayParams)
        edit.putBoolean(RENDER_BACKGROUND, sc.renderBackground)
        edit.putBoolean(FIT_TO_VIEWPORT, sc.fitToViewport)
        edit.putBoolean(HIDE_NAV_BAR, sc.hideNavBar)
        //edit.putBoolean(SHOW_HINTS, sc.showHints)
        edit.putInt(COLOR_LIST_VIEW_TYPE, sc.colorListViewType.ordinal)
        edit.putInt(SHAPE_LIST_VIEW_TYPE, sc.shapeListViewType.ordinal)
        edit.putInt(TEXTURE_LIST_VIEW_TYPE, sc.textureListViewType.ordinal)
        edit.putBoolean(AUTOFIT_COLOR_RANGE, sc.autofitColorRange)

        edit.putInt(PALETTE, max(ColorPalette.all.indexOf(f.palette), 0))
        edit.putInt(SOLID_FILL_COLOR, f.solidFillColor)

        /**
         *  Saved starred values of default ColorPalettes and Shapes
         *  key generated as palette/shape name in English without spaces + "Starred"
         *  e.g. "YinYangStarred"
         */
        val usResources = getLocalizedResources(applicationContext, Locale.US)
        ColorPalette.default.forEach { edit.putBoolean(
                "Palette${usResources.getString(it.nameId).replace(" ", "")}Starred", it.isFavorite
        )}
        Shape.default.forEach { edit.putBoolean(
                "Shape${usResources.getString(it.nameId).replace(" ", "")}Starred", it.isFavorite
        )}
        Texture.all.forEach { edit.putBoolean(
                "Texture${usResources.getString(it.nameId).replace(" ", "")}Starred", it.isFavorite
        )}

        edit.putInt(VERSION_CODE_TAG, BuildConfig.VERSION_CODE)
        edit.putInt(CHUNK_PROFILE, sc.chunkProfile.ordinal)
        edit.apply()

        super.onPause()
        fsv.onPause()

    }
    override fun onResume() {

        super.onResume()
        Log.e("MAIN", "!! onResume !!")
        if (fragmentsCompleted == 5) queryPurchases()
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
    fun showSlowDualfloatDialog() {

    }
    fun getLocalizedResources(context: Context, desiredLocale: Locale?): Resources {
        var conf: Configuration = context.resources.configuration
        conf = Configuration(conf)
        conf.setLocale(desiredLocale)
        val localizedContext = context.createConfigurationContext(conf)
        return localizedContext.resources
    }

    fun consumePurchase() {

        val purchaseQueryResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
        purchaseQueryResult?.purchasesList?.getOrNull(0)?.apply {
            Log.e("MAIN", "processing purchase...")
            Log.e("MAIN", originalJson)
            if (purchaseState == Purchase.PurchaseState.PURCHASED) {
                GlobalScope.launch(Dispatchers.IO) {
                    billingClient.consumePurchase(
                            ConsumeParams.newBuilder()
                                    .setPurchaseToken(purchaseToken)
                                    .build()
                    )
                }
                sc.goldEnabled = false
            }
        }
    }
    fun queryPurchases() {

        val purchaseQueryResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
        purchaseQueryResult?.purchasesList?.getOrNull(0)?.apply {
            Log.e("MAIN", "processing purchase...")
            Log.e("MAIN", originalJson)
            when (purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    sc.goldEnabled = true
                    onGoldEnabled()
                    if (!isAcknowledged) {
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchaseToken)
                        GlobalScope.launch(Dispatchers.IO) {
                            billingClient.acknowledgePurchase(acknowledgePurchaseParams.build())
                        }
                    }
                    if (!goldEnabledDialogShown) {
                        AlertDialog.Builder(this@MainActivity, R.style.AlertDialogCustom)
                                .setIcon(R.drawable.wow)
                                .setTitle(R.string.gold_enabled)
                                .setMessage(R.string.gold_enabled_dscript)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        goldEnabledDialogShown = true
                    }
                }
                Purchase.PurchaseState.PENDING -> {
                    if (!goldPendingDialogShown) {
                        AlertDialog.Builder(this@MainActivity, R.style.AlertDialogCustom)
                                .setIcon(R.drawable.pending)
                                .setTitle(R.string.gold_pending)
                                .setMessage(R.string.gold_pending_dscript)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        goldPendingDialogShown = true
                    }
                }
            }
        }
        if (purchaseQueryResult?.purchasesList == null) Log.e("MAIN", "purchaseList is null")
        else Log.e("MAIN", "purchaseList size: ${purchaseQueryResult.purchasesList?.size}")
        if (purchaseQueryResult?.purchasesList?.size == 0) {
            goldEnabledDialogShown = false
            goldPendingDialogShown = false
        }
        if (BuildConfig.DEV_VERSION) {
            sc.goldEnabled = true
            onGoldEnabled()
        }

    }
    fun onGoldEnabled() {
        val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        val edit = sp.edit()
        edit.putBoolean(GOLD_ENABLED_DIALOG_SHOWN, true)
        edit.apply()
        ( textureFragment  as TextureFragment  ).onGoldEnabled()
        ( shapeFragment    as ShapeFragment    ).onGoldEnabled()
        ( colorFragment    as ColorFragment    ).onGoldEnabled()
        ( settingsFragment as SettingsFragment ).onGoldEnabled()
    }

    fun getAvailableHeapMemory() : Long {
        val runtime = Runtime.getRuntime()
        val usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
        val maxHeapSizeInMB = runtime.maxMemory() / 1048576L
        return maxHeapSizeInMB - usedMemInMB
    }
    fun showUpgradeScreen() {

        val myIntent = Intent(this, UpgradeActivity::class.java)
        startActivity(myIntent)

    }

    fun onRateButtonClicked(): Boolean {
        Toast.makeText(this, "Rate button was clicked", Toast.LENGTH_LONG).show()
        // button click handled
        return true
    }

    override fun onComplete() {
        fragmentsCompleted++
        Log.e("MAIN", "!! fragment callback !! $fragmentsCompleted fragments completed")
        if (fragmentsCompleted == 5) {
            if (!billingClient.isReady) billingClient.startConnection(billingClientStateListener)
            else queryPurchases()
        }
    }


}
