package com.selfsimilartech.fractaleye

import android.Manifest
import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.database.Cursor
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.opengl.GLSurfaceView
import android.os.*
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
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
import com.michaelflisar.changelog.ChangelogSetup
import com.michaelflisar.changelog.classes.ImportanceChangelogSorter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.color_fragment.*
import kotlinx.android.synthetic.main.image_fragment.*
import kotlinx.android.synthetic.main.position_fragment_old.*
import kotlinx.android.synthetic.main.settings_fragment.*
import kotlinx.android.synthetic.main.shape_fragment.*
import kotlinx.android.synthetic.main.texture_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apfloat.Apcomplex
import org.apfloat.Apfloat
import org.apfloat.ApfloatMath
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt


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
const val ASPECT_RATIO = "aspectRatio"
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
const val BOOKMARK_LIST_VIEW_TYPE = "bookmarkListViewType"
const val AUTOFIT_COLOR_RANGE = "autofitColorRange"
const val HARDWARE_PROFILE = "hardwareProfile"
const val GPU_PRECISION = "gpuPrecision"
const val CPU_PRECISION = "cpuPrecision"
const val PREV_FRACTAL_CREATED = "previousFractalCreated"
const val PREV_FRACTAL_ID = "previousFractalId"
const val TEX_IMAGE_COUNT = "texImageCount"

const val PALETTE = "palette"
const val ACCENT_COLOR1 = "accentColor1"
const val ACCENT_COLOR2 = "accentColor2"

const val USE_ALTERNATE_SPLIT = "useAlternateSplit"
const val ALLOW_SLOW_DUALFLOAT = "allowSlowDualfloat"
const val CHUNK_PROFILE = "chunkProfile"
const val VERSION_CODE_TAG = "versionCode"
const val SHARED_PREFERENCES = "com.selfsimilartech.fractaleye.SETTINGS"

const val PALETTE_TABLE_NAME = "palette"
const val SHAPE_TABLE_NAME = "shape"
const val FRACTAL_TABLE_NAME = "fractal"

const val TEX_IM_PREFIX = "tex_im_"


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

fun View?.color() : Int {
    return (this?.background as? ColorDrawable)?.color ?: Color.BLACK
}


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




class PositionList(
        val default: Position = Position(),
        val julia: Position = Position(zoom = 3.5)
) {
    
    var active = default

    fun setFrom(newPositions: PositionList) {
        default.setFrom(newPositions.default)
        julia.setFrom(newPositions.julia)
    }
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
    else IntArray(ids.size) { i: Int -> res.getColor(ids[i], null) }
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

fun split(a: Float) : PointF {
    val t = a*8193f
    val q = t - a
    val hi = t - q
    val lo = a - hi
    Log.e("SPLIT", "t: $t, q: $q, hi: $hi, lo: $lo")
    return PointF(hi, lo)
}




class MainActivity : AppCompatActivity(), OnCompleteListener {


    var fragmentsCompleted = 0

    lateinit var db : AppDatabase
    val f = Fractal.default
    var sc = SettingsConfig
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
    private var previousFractalCreated = false
    private var dialog : AlertDialog? = null
    private var previousFractalId = -1


    private lateinit var settingsFragment   : Fragment
    private lateinit var imageFragment      : MenuFragment
    private lateinit var textureFragment    : MenuFragment
    private lateinit var shapeFragment      : MenuFragment
    private lateinit var colorFragment      : MenuFragment
    private lateinit var positionFragment   : MenuFragment


    private lateinit var billingClient : BillingClient
    private val billingClientStateListener = object : BillingClientStateListener {

        override fun onBillingSetupFinished(billingResult: BillingResult) {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> queryPurchases()
                else -> {
                    if (BuildConfig.DEV_VERSION) queryPurchases()
                    Log.e("MAIN", "unknown billing response code")
                }
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

    class ActivityHandler(activity: MainActivity) : Handler(Looper.getMainLooper()) {

        private val MSG_UPDATE_COLOR_THUMBNAILS = 0
        private val MSG_UPDATE_TEXTURE_THUMBNAILS = 1
        private val MSG_IMAGE_SAVED = 2
        private val MSG_ERROR = 3
        private val MSG_UPDATE_SHAPE_THUMBNAILS = 4
        private val MSG_SHOW_BOOKMARK_DIALOG = 5
        private val MSG_BOOKMARK_AS_PREVIOUS_FRACTAL = 6


        


        // Weak reference to the Activity; only access this from the UI thread.
        private val mWeakActivity : WeakReference<MainActivity> = WeakReference(activity)

        fun updateColorThumbnails() {
            sendMessage(obtainMessage(MSG_UPDATE_COLOR_THUMBNAILS))
        }
        fun updateTextureThumbnail(layoutIndex: Int, n: Int) {
            sendMessage(obtainMessage(MSG_UPDATE_TEXTURE_THUMBNAILS, layoutIndex, n))
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
        fun showBookmarkDialog() {
            sendMessage(obtainMessage(MSG_SHOW_BOOKMARK_DIALOG))
        }
        fun bookmarkAsPreviousFractal() {
            sendMessage(obtainMessage(MSG_BOOKMARK_AS_PREVIOUS_FRACTAL))
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
                MSG_UPDATE_TEXTURE_THUMBNAILS -> activity?.updateTextureThumbnail(msg.arg1, msg.arg2)
                MSG_UPDATE_SHAPE_THUMBNAILS -> activity?.updateShapeThumbnail(msg.obj as Shape, msg.arg1)
                MSG_IMAGE_SAVED -> activity?.showMessage(
                        "${activity.resources.getString(R.string.msg_save_successful)} ${msg.obj}"
                )
                MSG_ERROR -> activity?.showMessage(
                        activity.resources.getString(R.string.msg_error)
                )
                MSG_SHOW_BOOKMARK_DIALOG -> activity?.showBookmarkDialog()
                MSG_BOOKMARK_AS_PREVIOUS_FRACTAL -> activity?.bookmarkAsPreviousFractal()
                else -> throw RuntimeException("unknown msg $what")
            }
        }
    }


    enum class Category(val displayName: Int, val icon: Int) {

        IMAGE(R.string.image, R.drawable.image2) {
            override fun onOpenMenu(act: MainActivity) {
                act.apply {
                    if (presetPreviewListLayout.isVisible) uiSetOpenTall() else uiSetOpen()
                }
            }
            override fun onCloseMenu(act: MainActivity) {}
            override fun onMenuClosed(act: MainActivity) {
                if (!act.presetPreviewListLayout.isVisible) act.categoryButtons.getTabAt(POSITION).select()
            }
            override fun onCategorySelected(act: MainActivity) {
                //val categoryNameButton = act.findViewById<Button>(R.id.categoryNameButton)
                //act.fsv.renderProfile = RenderProfile.MANUAL
                act.apply {
                    fsv.r.reaction = Reaction.NONE
                    hideTouchIcon()
                    if (uiIsClosed()) uiSetOpen()
                }
            }
            override fun onCategoryUnselected(act: MainActivity) {}
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
                    if (act.f.texture.activeParam.nameId == R.string.density && !act.sc.autofitColorRange) act.textureModeButton.performClick()
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
                act.apply {
                    if (f.texture.usesDensity && sc.autofitColorRange) densityButton.show() else densityButton.hide()
                    fsv.r.reaction = Reaction.COLOR
                    showTouchIcon()
                }
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
        val migrate4to5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE fractal (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    isFavorite INTEGER NOT NULL,
                    thumbnailPath TEXT NOT NULL,
                    shapeId INTEGER NOT NULL,
                    customShape INTEGER NOT NULL,
                    juliaMode INTEGER NOT NULL,
                    maxIter INTEGER NOT NULL,
                    p1_id INTEGER,
                    p1_u REAL,
                    p1_v REAL,
                    p1_isComplex INTEGER,
                    p2_id INTEGER,
                    p2_u REAL,
                    p2_v REAL,
                    p2_isComplex INTEGER,
                    p3_id INTEGER,
                    p3_u REAL,
                    p3_v REAL,
                    p3_isComplex INTEGER,
                    p4_id INTEGER,
                    p4_u REAL,
                    p4_v REAL,
                    p4_isComplex INTEGER,
                    julia_id INTEGER,
                    julia_u REAL,
                    julia_v REAL,
                    julia_isComplex INTEGER,
                    seed_id INTEGER,
                    seed_u REAL,
                    seed_v REAL,
                    seed_isComplex INTEGER,
                    pos_x REAL,
                    pos_y REAL,
                    pos_zoom REAL,
                    pos_rotation REAL,
                    textureId INTEGER NOT NULL,
                    customTexture INTEGER NOT NULL,
                    textureMode INTEGER NOT NULL,
                    radius REAL NOT NULL,
                    q1_id INTEGER,
                    q1_u REAL,
                    q1_v REAL,
                    q1_isComplex INTEGER,
                    q2_id INTEGER,
                    q2_u REAL,
                    q2_v REAL,
                    q2_isComplex INTEGER,
                    q3_id INTEGER,
                    q3_u REAL,
                    q3_v REAL,
                    q3_isComplex INTEGER,
                    q4_id INTEGER,
                    q4_u REAL,
                    q4_v REAL,
                    q4_isComplex INTEGER,
                    paletteId INTEGER NOT NULL,
                    customPalette INTEGER NOT NULL,
                    frequency REAL NOT NULL,
                    phase REAL NOT NULL,
                    solidFillColor INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        val migrate5to6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {

                // create new table (remove parameter ids)
                database.execSQL("""
                    CREATE TABLE fractal_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    isFavorite INTEGER NOT NULL,
                    thumbnailPath TEXT NOT NULL,
                    shapeId INTEGER NOT NULL,
                    customShape INTEGER NOT NULL,
                    juliaMode INTEGER NOT NULL,
                    maxIter INTEGER NOT NULL,
                    p1_u REAL,
                    p1_v REAL,
                    p1_isComplex INTEGER,
                    p2_u REAL,
                    p2_v REAL,
                    p2_isComplex INTEGER,
                    p3_u REAL,
                    p3_v REAL,
                    p3_isComplex INTEGER,
                    p4_u REAL,
                    p4_v REAL,
                    p4_isComplex INTEGER,
                    julia_u REAL,
                    julia_v REAL,
                    julia_isComplex INTEGER,
                    seed_u REAL,
                    seed_v REAL,
                    seed_isComplex INTEGER,
                    pos_x REAL,
                    pos_y REAL,
                    pos_zoom REAL,
                    pos_rotation REAL,
                    textureId INTEGER NOT NULL,
                    customTexture INTEGER NOT NULL,
                    textureMode INTEGER NOT NULL,
                    radius REAL NOT NULL,
                    q1_u REAL,
                    q1_v REAL,
                    q1_isComplex INTEGER,
                    q2_u REAL,
                    q2_v REAL,
                    q2_isComplex INTEGER,
                    q3_u REAL,
                    q3_v REAL,
                    q3_isComplex INTEGER,
                    q4_u REAL,
                    q4_v REAL,
                    q4_isComplex INTEGER,
                    paletteId INTEGER NOT NULL,
                    customPalette INTEGER NOT NULL,
                    frequency REAL NOT NULL,
                    phase REAL NOT NULL,
                    solidFillColor INTEGER NOT NULL
                    )
                """.trimIndent())

                // copy data into new table
                database.execSQL("""
                    INSERT INTO fractal_new (
                    id,
                    name,
                    isFavorite,
                    thumbnailPath,
                    shapeId,
                    customShape,
                    juliaMode,
                    maxIter,
                    p1_u,
                    p1_v,
                    p1_isComplex,
                    p2_u,
                    p2_v,
                    p2_isComplex,
                    p3_u,
                    p3_v,
                    p3_isComplex,
                    p4_u,
                    p4_v,
                    p4_isComplex,
                    julia_u,
                    julia_v,
                    julia_isComplex,
                    seed_u,
                    seed_v,
                    seed_isComplex,
                    pos_x,
                    pos_y,
                    pos_zoom,
                    pos_rotation,
                    textureId,
                    customTexture,
                    textureMode,
                    radius,
                    q1_u,
                    q1_v,
                    q1_isComplex,
                    q2_u,
                    q2_v,
                    q2_isComplex,
                    q3_u,
                    q3_v,
                    q3_isComplex,
                    q4_u,
                    q4_v,
                    q4_isComplex,
                    paletteId,
                    customPalette,
                    frequency,
                    phase,
                    solidFillColor
                    ) SELECT id,
                    name,
                    isFavorite,
                    thumbnailPath,
                    shapeId,
                    customShape,
                    juliaMode,
                    maxIter,
                    p1_u,
                    p1_v,
                    p1_isComplex,
                    p2_u,
                    p2_v,
                    p2_isComplex,
                    p3_u,
                    p3_v,
                    p3_isComplex,
                    p4_u,
                    p4_v,
                    p4_isComplex,
                    julia_u,
                    julia_v,
                    julia_isComplex,
                    seed_u,
                    seed_v,
                    seed_isComplex,
                    pos_x,
                    pos_y,
                    pos_zoom,
                    pos_rotation,
                    textureId,
                    customTexture,
                    textureMode,
                    radius,
                    q1_u,
                    q1_v,
                    q1_isComplex,
                    q2_u,
                    q2_v,
                    q2_isComplex,
                    q3_u,
                    q3_v,
                    q3_isComplex,
                    q4_u,
                    q4_v,
                    q4_isComplex,
                    paletteId,
                    customPalette,
                    frequency,
                    phase,
                    solidFillColor FROM fractal
                """.trimIndent())

                // remove old table
                database.execSQL("DROP TABLE fractal")

                // Change table name
                database.execSQL("ALTER TABLE fractal_new RENAME TO fractal")

            }
        }
        val migrate6to7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE fractal ADD COLUMN accent2 INTEGER DEFAULT 0 NOT NULL")
            }
        }
        val migrate7to8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE fractal ADD COLUMN density REAL DEFAULT 0.0 NOT NULL")
            }
        }
        val migrate8to9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE fractal ADD COLUMN textureMin REAL DEFAULT 0.0 NOT NULL")
                database.execSQL("ALTER TABLE fractal ADD COLUMN textureMax REAL DEFAULT 1.0 NOT NULL")
            }
        }
        val migrate9to10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE fractal ADD COLUMN imagePath TEXT DEFAULT '' NOT NULL")
            }
        }
        val migrate10to11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE fractal ADD COLUMN imageId INTEGER DEFAULT -1 NOT NULL")
            }
        }

        db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "custom"
        ).fallbackToDestructiveMigrationFrom(1).addMigrations(
                migrate2to3,
                migrate3to4,
                migrate4to5,
                migrate5to6,
                migrate6to7,
                migrate7to8,
                migrate8to9,
                migrate9to10,
                migrate10to11
        ).build()

        billingClient = BillingClient.newBuilder(this)
                .setListener(purchaseUpdateListener)
                .enablePendingPurchases()
                .build()
        billingClient.startConnection(billingClientStateListener)


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
        if (Resolution.all.none { it.w == screenWidth }) Resolution.addResolution(screenWidth)
        Resolution.SCREEN = Resolution.valueOf(screenWidth) ?: Resolution.R1080
        Resolution.initialize(screenRatio)
        AspectRatio.initialize()


        // restore SettingsConfig from SharedPreferences
        val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)

        goldEnabledDialogShown = sp.getBoolean(GOLD_ENABLED_DIALOG_SHOWN, false)
        goldPendingDialogShown = sp.getBoolean(GOLD_PENDING_DIALOG_SHOWN, false)
        showEpilepsyDialog = sp.getBoolean(SHOW_EPILEPSY_DIALOG, true)
        previousFractalCreated = sp.getBoolean(PREV_FRACTAL_CREATED, false)
        previousFractalId = sp.getInt(PREV_FRACTAL_ID, -1)

        Texture.CUSTOM_IMAGE_COUNT = sp.getInt(TEX_IMAGE_COUNT, 0)

        // val maxStartupRes = if (sc.goldEnabled) Resolution.SCREEN else Resolution.R1080
        val savedResolution = sp.getInt(RESOLUTION, Resolution.all.indexOf(Resolution.R1080))
        sc.resolution = Resolution.all.getOrNull(savedResolution) ?: Resolution.R720
        // val savedAspectRatio = sp.getInt(ASPECT_RATIO, 0)
        // sc.aspectRatio = AspectRatio.all.getOrNull(savedAspectRatio) ?: AspectRatio.RATIO_SCREEN
        //sc.resolution = Resolution.FOURTH
        //sc.precision = Precision.values()[sp.getInt(PRECISION, Precision.SINGLE.ordinal)]
        //sc.autoPrecision = sp.getBoolean(AUTO_PRECISION, true)
        sc.continuousPosRender  = sp.getBoolean(CONTINUOUS_RENDER, false)
        sc.displayParams        = sp.getBoolean(DISPLAY_PARAMS, false)
        sc.renderBackground     = sp.getBoolean(RENDER_BACKGROUND, true)
        // sc.fitToViewport        = sp.getBoolean(FIT_TO_VIEWPORT, false)
        sc.hideNavBar           = sp.getBoolean(HIDE_NAV_BAR, true)
        sc.colorListViewType    = ListLayoutType.values()[sp.getInt(COLOR_LIST_VIEW_TYPE,    ListLayoutType.GRID.ordinal)]
        sc.shapeListViewType    = ListLayoutType.values()[sp.getInt(SHAPE_LIST_VIEW_TYPE,    ListLayoutType.GRID.ordinal)]
        sc.textureListViewType  = ListLayoutType.values()[sp.getInt(TEXTURE_LIST_VIEW_TYPE,  ListLayoutType.GRID.ordinal)]
        sc.bookmarkListViewType = ListLayoutType.values()[sp.getInt(BOOKMARK_LIST_VIEW_TYPE, ListLayoutType.GRID.ordinal)]
        sc.autofitColorRange    = sp.getBoolean(AUTOFIT_COLOR_RANGE, true)
        sc.useAlternateSplit    = sp.getBoolean(USE_ALTERNATE_SPLIT, false)
        sc.allowSlowDualfloat   = sp.getBoolean(ALLOW_SLOW_DUALFLOAT, false)
        sc.chunkProfile         = ChunkProfile.values()[sp.getInt(CHUNK_PROFILE, 1)]
        f.accent1               = sp.getInt(ACCENT_COLOR1, Color.WHITE)




        val categoryLayoutHeight = resources.getDimension(R.dimen.uiLayoutHeight).toInt()
        val categoryPagerHeight = resources.getDimension(R.dimen.categoryPagerHeight).toInt()
        val categoryNameButtonHeight = resources.getDimension(R.dimen.categoryNameButtonHeight).toInt()
        val categoryButtonsHeight = resources.getDimension(R.dimen.menuButtonHeight).toInt()
        val uiHeightOpen = categoryLayoutHeight + categoryNameButtonHeight
        val uiHeightClosed = uiHeightOpen - categoryLayoutHeight


        // initialize default palettes and shapes
        val usResources = getLocalizedResources(applicationContext, Locale.US)
        Palette.default.forEach {
            it.isFavorite = sp.getBoolean(
                    "Palette${usResources.getString(it.nameId).replace(" ", "")}Starred", false
            )
            it.initialize(resources)
        }
        Shape.default.forEach {
            it.initialize(resources)
            it.isFavorite = sp.getBoolean(
                    "Shape${usResources.getString(it.nameId).replace(" ", "")}Starred", false
            )
        }
        Texture.all.forEach {
            it.initialize(resources)
            it.isFavorite = sp.getBoolean(
                    "Texture${usResources.getString(it.nameId).replace(" ", "")}Starred", false
            )
        }
        Fractal.all.forEach { it.initialize(resources) }

        ListHeader.all.forEach { it.initialize(resources) }



        Log.d("MAIN", "available heap size in MB: ${getAvailableHeapMemory()}")



        val r = FractalRenderer(this, baseContext, ActivityHandler(this))
        //fsv = FractalSurfaceView(baseContext, f, sc, this, r)


        settingsFragment  = SettingsFragment()
        imageFragment     = ImageFragment()
        textureFragment   = TextureFragment()
        shapeFragment     = ShapeFragment()
        colorFragment     = ColorFragment()
        positionFragment  = PositionFragment()


        super.onCreate(savedInstanceState)

//        settingsFragment  = SettingsFragment()
//        supportFragmentManager.beginTransaction().add(settingsFragment, "SETTINGS").commit()


        setContentView(R.layout.activity_main)
        // fractalLayout.addView(fsv)
        fsv = fractalSurfaceView!!
        fsv.initialize(r, this)
        fsv.layoutParams = FrameLayout.LayoutParams(screenWidth, screenHeight, Gravity.CENTER)
        fractalLayout.layoutParams = FrameLayout.LayoutParams(screenWidth, screenHeight, Gravity.CENTER)
        baseLayout.setOnTouchListener { v, event -> fsv.onTouchEvent(event) }
        onAspectRatioChanged()

        deviceHasNotch = calcDeviceHasNotch()
        navBarHeight = calcNavBarHeight()
        statusBarHeight = calcStatusBarHeight()
        //updateSurfaceViewLayout(resources.getDimension(R.dimen.uiLayoutHeightClosed))


        val layoutList = listOf(
                baseLayout,
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



        newBookmarkButton.setOnClickListener {

            Fractal.tempBookmark1 = f.bookmark(fsv)

            fsv.r.renderProfile = RenderProfile.SAVE_THUMBNAIL
            fsv.requestRender()

        }

        settingsButton.setOnClickListener { openSettingsMenu() }

        saveImageButton.setOnClickListener {

            if (fsv.r.isRendering) showMessage(resources.getString(R.string.msg_save_wait))
            else {

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                WRITE_STORAGE_REQUEST_CODE)
                    } else {
                        fsv.r.renderProfile = RenderProfile.SAVE_IMAGE
                        fsv.requestRender()
                    }
                } else {
                    bookmarkAsPreviousFractal()
                    fsv.r.renderProfile = RenderProfile.SAVE_IMAGE
                    fsv.requestRender()
                }

            }

        }





        categoryNameButton.setOnClickListener {

            if (!uiIsClosed()) uiSetClosed() else categoryButtons.getCurrentCategory().onOpenMenu(this)

        }

        supportFragmentManager.beginTransaction().add(R.id.settingsFragmentContainer, settingsFragment, "SETTINGS").commit()
        settingsFragmentContainer.hide()

        val categoryPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        categoryPagerAdapter.addFrag(imageFragment)
        categoryPagerAdapter.addFrag(textureFragment)
        categoryPagerAdapter.addFrag(shapeFragment)
        categoryPagerAdapter.addFrag(colorFragment)
        categoryPagerAdapter.addFrag(positionFragment)
        categoryPager.adapter = categoryPagerAdapter
        categoryPager.offscreenPageLimit = 4


        categoryButtons.setupWithViewPager(categoryPager)
        for (i in 0..4) {
            val transparentIcon = ResourcesCompat.getDrawable(resources, Category.values()[i].icon, null)
            transparentIcon?.alpha = 128
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


        // load custom palettes, shapes, and bookmarks
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
                        PALETTE_TABLE_NAME -> Palette.nextCustomPaletteNum = nextId?.toInt() ?: 0 + 1
                        SHAPE_TABLE_NAME -> Shape.nextCustomShapeNum = nextId?.toInt() ?: 0 + 1
                        FRACTAL_TABLE_NAME -> Fractal.nextCustomFractalNum = nextId?.toInt() ?: 0 + 1
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()


            db.colorPaletteDao().apply {
                getAll().forEach {
                    Palette.custom.add(0, Palette(
                            name = it.name,
                            id = it.id,
                            hasCustomId = true,
                            colors = ArrayList(arrayListOf(
                                    it.c1, it.c2, it.c3,
                                    it.c4, it.c5, it.c6,
                                    it.c7, it.c8, it.c9,
                                    it.c10, it.c11, it.c12
                            ).slice(0 until it.size)),
                            isFavorite = it.starred
                    ))
                    Palette.custom[0].initialize(resources)
                    Log.d("MAIN", "custom palette ${Palette.custom[0].name}, id: ${Palette.custom[0].id}")
                }
            }

            Palette.all.addAll(0, Palette.custom)
            Palette.all.find { it.id == sp.getInt(PALETTE, Palette.night.id) }?.let { f.palette = it }


            db.shapeDao().apply {
                getAll().forEach {
                    Shape.custom.add(0, Shape(
                            name = it.name,
                            id = it.id,
                            hasCustomId = true,
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
                            maxIter = it.maxIter,
                            radius = it.bailoutRadius,
                            isConvergent = it.isConvergent,
                            hasDualFloat = it.hasDualFloat,
                            customLoopSF = it.loopSF,
                            customLoopDF = it.loopDF,
                            isFavorite = it.isFavorite
                    ))
                    Shape.custom[0].initialize(resources)
                    Log.d("MAIN", "custom shape ${Shape.custom[0].name}, id: ${Shape.custom[0].id}")
                }
            }
            Shape.all.addAll(0, Shape.custom)


            db.fractalDao().apply {

                if (!previousFractalCreated) {
                    Fractal.previous.customId = insert(Fractal.previous.toEntity()).toInt()
                    previousFractalId = Fractal.previous.customId
                    val edit = sp.edit()
                    edit.putInt(PREV_FRACTAL_ID, Fractal.previous.customId)
                    edit.putBoolean(PREV_FRACTAL_CREATED, true)
                    edit.apply()
                }

                getAll().forEach {

                    val shapeParams = if (listOfNotNull(it.julia, it.seed, it.p1, it.p2, it.p3, it.p4).isNotEmpty()) Shape.ParamListPreset(
                            listOfNotNull(it.p1, it.p2, it.p3, it.p4).map { p -> if (p.isComplex) ComplexParam(p) else RealParam(p) },
                            julia = if (it.julia != null) ComplexParam(it.julia) else null,
                            seed = if (it.seed != null) ComplexParam(it.seed) else null
                    ) else null

                    val textureParams = if (listOfNotNull(it.q1, it.q2, it.q3, it.q4).isNotEmpty()) Texture.ParamListPreset(
                            listOf(it.q1, it.q2, it.q3, it.q4).map { q ->
                                if (q != null) {
                                    if (q.isComplex) ComplexParam(q) else RealParam(q)
                                } else null
                            }
                    ) else null

                    val thumbnail = try {
                        val inputStream = openFileInput(it.thumbnailPath)
                        BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }).let { bmp ->
                            inputStream?.close()
                            bmp
                        }
                    } catch (e: Exception) {
                        Log.e("MAIN", "thumnail path invalid")
                        BitmapFactory.decodeResource(resources, R.drawable.mandelbrot_icon)
                    }

                    val newBookmark = Fractal(

                                name = it.name,
                                isFavorite = it.isFavorite,
                                thumbnailPath = it.thumbnailPath,
                                thumbnail = thumbnail,
                                customId = it.id,
                                goldFeature = false,

                                shapeId = it.shapeId,
                                juliaMode = it.juliaMode,
                                maxIter = it.maxIter,
                                shapeParams = shapeParams,
                                position = if (it.position != null) Position(it.position) else null,

                                textureId = it.textureId,
                                textureMode = TextureMode.values()[it.textureMode],
                                radius = it.radius,
                                textureMin = it.textureMin,
                                textureMax = it.textureMax,
                                textureParams = textureParams,
                                imagePath = it.imagePath,
                                imageId = Texture.defaultImages.getOrNull(it.imageId) ?: -1,

                                paletteId = it.paletteId,
                                frequency = it.frequency,
                                phase = it.phase,
                                density = it.density,
                                accent1 = it.solidFillColor,
                                accent2 = it.accent2

                    )
                    if (it.id == previousFractalId) Fractal.previous = newBookmark else Fractal.bookmarks.add(0, newBookmark)
                    Log.d("MAIN", "Bookmark -- id: ${newBookmark.customId}, name: ${newBookmark.name}, imagePath: ${it.imagePath}, imageId: ${it.imageId} thumbPath: ${it.thumbnailPath}, frequency: ${it.frequency}, phase: ${it.phase}")

                }

            }
            Fractal.all.addAll(0, Fractal.bookmarks)

        }

        // load custom texture images
        // fileList().forEachIndexed { i, f -> Log.d("MAIN", "file #$i: $f") }
        fileList().filter { it.startsWith(TEX_IM_PREFIX) }.let { Texture.customImages.addAll(it.reversed()) }


        overlay.bringToFront()
        settingsFragmentContainer.bringToFront()
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        if (!BuildConfig.DEV_VERSION) {
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
            } else {
                if (sp.getInt(VERSION_CODE_TAG, 0) != BuildConfig.VERSION_CODE) showChangelog()
            }
        }
        if (previousFractalCreated) showLoadPreviousFractalDialog()

    }



    fun uiSetClosed() {
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
                        ResourcesCompat.getDrawable(resources, R.drawable.expand, null)
                    else
                        ResourcesCompat.getDrawable(resources, R.drawable.collapse, null),
                    null
            )

            val anim = ValueAnimator.ofFloat(0f, 1f)
            anim.duration = uiInnerLayout.layoutTransition.getDuration(LayoutTransition.CHANGING) + 75L
            anim.addUpdateListener {

                updateSurfaceViewLayout()

                if (anim.animatedFraction == 1f) {
                    if (newHeight == resources.getDimension(R.dimen.uiLayoutHeightClosed).toInt()) {
                        categoryButtons.getCurrentCategory().onMenuClosed(this)
                    }
                }

            }
            anim.start()

        }

    }
    fun updateSurfaceViewLayout() {

        if (sc.fitToViewport && fractalLayout.layoutParams.height >= ui.top - header.bottom) {
            val scaleFactor = (ui.top - header.bottom) / fractalLayout.layoutParams.height.toFloat()
            fractalLayout.apply {
//                pivotX = (left + right)/2f
//                pivotY = (top + bottom)/2f
                scaleX = scaleFactor
                scaleY = scaleFactor
            }
        }
        else {
            fractalLayout.scaleX = 1f
            fractalLayout.scaleY = 1f
        }
        fractalLayout.y = (ui.top + header.bottom - fractalLayout.height)/2f
        fractalLayout.updateLayoutParams<FrameLayout.LayoutParams> { gravity = Gravity.CENTER }

        fractalLayout.requestLayout()

    }
    fun onAspectRatioChanged() {

        val newWidth : Int
        val newHeight : Int
        if (sc.aspectRatio.r <= AspectRatio.RATIO_SCREEN.r) {
            newWidth = FrameLayout.LayoutParams.MATCH_PARENT
            newHeight = (Resolution.SCREEN.w * sc.aspectRatio.r).toInt()
        }
        else {
            newWidth = (Resolution.SCREEN.h / sc.aspectRatio.r).toInt()
            newHeight = Resolution.SCREEN.h
        }
        fractalLayout.layoutParams = FrameLayout.LayoutParams(newWidth, newHeight, Gravity.CENTER)

        updateSurfaceViewLayout()

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

        updateSurfaceViewLayout()
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

    fun openSettingsMenu() {
        settingsFragmentContainer.apply {
            show()
        }
    }
    fun closeSettingsMenu() {
        settingsFragmentContainer.apply {
            hide()
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
                Reaction.NONE -> {

                    w = (60f * density).toInt()

                }
                Reaction.POSITION -> {

                    displayParamName1.text = resources.getString(R.string.x)
                    displayParamName2.text = resources.getString(R.string.y)
                    displayParamName3.text = resources.getString(R.string.zoom)
                    displayParamName4.text = resources.getString(R.string.rotation)
                    displayParam1.text = "%.17f".format(f.shape.position.x)
                    displayParam2.text = "%.17f".format(f.shape.position.y)
                    displayParam3.text = "%e".format(f.shape.position.zoom)
                    displayParam4.text = "%.0f".format(f.shape.position.rotation * 180.0 / Math.PI)
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
                            else f.texture.activeParam)
                                    .run { if (isRateParam) parent!! else this }
                    displayParamName1.text = "u"
                    displayParam1.text = "%.8f".format((param.u))
                    if (param is ComplexParam) {
                        displayParamName2.text = "v"
                        displayParam2.text = "%.8f".format((param.v))
                    } else {
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
        phaseEdit?.setText("%.5f".format(f.phase))
        densityEdit?.setText("%.5f".format(f.density))

        with(colorFragment as ColorFragment) {
            updateFrequencyLayout()
            updatePhaseLayout()
//            updateDensityLayout()
        }

    }
    fun updatePositionEditTexts() {

        xEdit?.setText("%.17f".format(f.shape.position.x))
        yEdit?.setText("%.17f".format(f.shape.position.y))

        val scaleStrings = "%e".format(Locale.US, f.shape.position.zoom).split("e")
        scaleSignificandEdit?.setText("%.5f".format(scaleStrings[0].toFloat()))
        scaleExponentEdit?.setText("%d".format(scaleStrings[1].toInt()))

        with(positionFragment as PositionFragment) {
            updateRotationLayout()
        }

    }
    fun updateHintVisibility() {

        val hints = listOf<TextView>(
                continuousRenderHint,
                renderBackgroundHint,
                displayParamsHint
//                bailoutHint
//                juliaModeHint,
//                maxIterHint,
//                phaseHint,
//                frequencyHint
        )
        hints.forEach { it.visibility = if (sc.showHints) TextView.VISIBLE else TextView.GONE }

    }
    fun updateFragmentLayouts() {

        listOf(textureFragment, shapeFragment, colorFragment, positionFragment).forEach { it.updateLayout() }

    }

    fun onTextureChanged() {
        if (f.texture.usesAccent) accentColor2Button.show() else accentColor2Button.hide()
    }

    fun showThumbnailDialog() {

        dialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setTitle(R.string.rendering_icons)
                .setIcon(R.drawable.hourglass)
                .setView(R.layout.alert_dialog_progress)
                .setNegativeButton(android.R.string.cancel) { dialog, which ->
                    fsv.r.interruptRender = true
                }
                .show()
        dialog?.setCanceledOnTouchOutside(false)

    }
    fun updateDialog(index: Int, total: Int) {
        dialog?.findViewById<ProgressBar>(R.id.alertProgress)?.apply {
            max = total - 1
            progress = index
        }
        dialog?.findViewById<TextView>(R.id.alertProgressText)?.text = "${index + 1}/$total"
    }
    fun dismissDialog() {
        dialog?.dismiss()
    }
    fun showBookmarkDialog(item: ListItem<Fractal>? = null, edit: Boolean = false) {

        val dialogView = layoutInflater.inflate(R.layout.alert_dialog_new_preset, null)
        val newBookmarkName = dialogView?.findViewById<EditText>(R.id.name)
        if (edit) newBookmarkName?.setText(Fractal.tempBookmark1.name)
        else {
            newBookmarkName?.setText("%s %s %d".format(
                    resources.getString(R.string.header_custom),
                    resources.getString(R.string.fractal),
                    Fractal.nextCustomFractalNum
            ))
        }
        val newBookmarkThumbnail = dialogView?.findViewById<ImageView>(R.id.thumbnail)
        newBookmarkThumbnail?.setImageBitmap(Fractal.tempBookmark1.thumbnail)

        val d = AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setIcon(R.drawable.bookmark2)
                .setTitle("${resources.getString(if (edit) R.string.edit else R.string.save)} ${resources.getString(R.string.bookmark)}")
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel) { dialog, which ->

                    if (!edit) File(filesDir.path + Fractal.tempBookmark1.thumbnailPath).delete()

                }
                .create()

        d.setOnShowListener {
            val b: Button = d.getButton(AlertDialog.BUTTON_POSITIVE)
            b.setOnClickListener {

                if (Fractal.all.find { it.name == newBookmarkName?.text?.toString() } != null) {
                    showMessage(resources.getString(R.string.msg_custom_name_duplicate).format(
                            resources.getString(R.string.bookmark)
                    ))
                } else {
                    if (edit) {
                        GlobalScope.launch {
                            db.fractalDao().apply {
                                newBookmarkName?.text?.toString()?.let { Fractal.tempBookmark1.name = it }
                                update(Fractal.tempBookmark1.customId, Fractal.tempBookmark1.name)
                            }
                        }
                        (imageFragment as ImageFragment).bookmarkListAdapter.apply {
                            updateItem(item!!)
                            clearSelection()
                        }
                    } else {

                        GlobalScope.launch {
                            db.fractalDao().apply {
                                newBookmarkName?.text?.toString()?.let { Fractal.tempBookmark1.name = it }
                                Fractal.bookmarks.add(Fractal.tempBookmark1)
                                Log.d("MAIN", "new bookmark thumbnail path: ${Fractal.tempBookmark1.thumbnailPath}")
                                Fractal.tempBookmark1.customId = insert(Fractal.tempBookmark1.toEntity()).toInt()
                            }
                            Fractal.tempBookmark1.goldFeature = false
                            Fractal.bookmarks.add(0, Fractal.tempBookmark1)
                            Fractal.all.add(0, Fractal.tempBookmark1)
                            Fractal.nextCustomFractalNum++
                        }

                        (imageFragment as ImageFragment).bookmarkListAdapter.addItemToCustom(
                                BookmarkListItem(Fractal.tempBookmark1, ListHeader.custom, sc.bookmarkListViewType), 0
                        )

                    }
                    d.dismiss()
                    if (!edit) showMessage(resources.getString(R.string.msg_bookmark_created))
                }

            }
            d.show()
        }
        d.show()

    }
    fun showLoadPreviousFractalDialog() {
        val dialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setTitle(R.string.load_previous_fractal)
                .setIcon(R.drawable.edit)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val prevZoom = f.shape.position.zoom
                    Fractal.default.load(Fractal.previous, fsv)
                    updateFragmentLayouts()
                    fsv.r.checkThresholdCross(prevZoom)
                    fsv.r.renderToTex = true
                    fsv.r.renderShaderChanged = true
                    fsv.requestRender()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()

    }
    fun bookmarkAsPreviousFractal() {
        GlobalScope.launch {
            db.fractalDao().apply {
                Fractal.previous = Fractal.default.bookmark(fsv)
                Fractal.previous.customId = previousFractalId
                update(Fractal.previous.toEntity())
            }
        }
    }

    fun updateColorThumbnails() {

        (colorPreviewList?.adapter as? ListAdapter<Palette>)?.notifyDataSetChanged()

    }
    fun updateTextureThumbnail(layoutIndex: Int, n: Int) {

        // Log.e("MAIN ACTIVITY", "updateTextureThumbnail was called !!!")
        texturePreviewList?.adapter?.notifyItemChanged(layoutIndex)
        Log.d("MAIN", "n: $n")
        updateDialog(n, f.shape.compatTextures.size)
        if (n + 1 == f.shape.compatTextures.size) dismissDialog()

    }
    fun updateShapeThumbnail(shape: Shape, customIndex: Int) {

        (shapePreviewList?.adapter as? ListAdapter<Shape>)?.updateItems(shape)
        if (customIndex != -1) {
            Log.d("MAIN", "customIndex: $customIndex")
            val numShapes = Shape.custom.size
            updateDialog(customIndex, numShapes)
            if (customIndex + 1 == numShapes) dismissDialog()
        }

    }


    override fun onBackPressed() {
        //super.onBackPressed()
    }
    override fun onPause() {

        Log.d("MAIN", "activity paused ...")

        // update any changes made to custom palettes
        GlobalScope.launch {
            db.colorPaletteDao().apply {
                Palette.custom.forEach {
                    Log.d("MAIN", "saving custom palette ${it.name}, starred= ${it.isFavorite}")
                    update(it.toDatabaseEntity())
                }
            }
        }

        bookmarkAsPreviousFractal()

        val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        val edit = sp.edit()
        edit.putBoolean(GOLD_ENABLED_DIALOG_SHOWN, goldEnabledDialogShown)
        edit.putBoolean(GOLD_PENDING_DIALOG_SHOWN, goldPendingDialogShown)
        edit.putBoolean(SHOW_EPILEPSY_DIALOG, showEpilepsyDialog)
        edit.putInt(RESOLUTION, min(Resolution.all.indexOf(sc.resolution), if (sc.goldEnabled) Resolution.all.indexOf(Resolution.SCREEN) else Resolution.all.indexOf(Resolution.R1080)))
        edit.putInt(ASPECT_RATIO, AspectRatio.all.indexOf(sc.aspectRatio))
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
        edit.putInt(BOOKMARK_LIST_VIEW_TYPE, sc.bookmarkListViewType.ordinal)
        edit.putBoolean(AUTOFIT_COLOR_RANGE, sc.autofitColorRange)

        edit.putInt(PALETTE, f.palette.id)
        edit.putInt(ACCENT_COLOR1, f.accent1)
        edit.putInt(ACCENT_COLOR2, f.accent2)

        /**
         *  Saved starred values of default ColorPalettes and Shapes
         *  key generated as palette/shape name in English without spaces + "Starred"
         *  e.g. "YinYangStarred"
         */
        val usResources = getLocalizedResources(applicationContext, Locale.US)
        Palette.default.forEach { edit.putBoolean(
                "Palette${usResources.getString(it.nameId).replace(" ", "")}Starred", it.isFavorite
        )}
        Shape.default.forEach { edit.putBoolean(
                "Shape${usResources.getString(it.nameId).replace(" ", "")}Starred", it.isFavorite
        )}
        Texture.all.forEach { edit.putBoolean(
                "Texture${usResources.getString(it.nameId).replace(" ", "")}Starred", it.isFavorite
        )}

        edit.putInt(VERSION_CODE_TAG, BuildConfig.VERSION_CODE)
        edit.putBoolean(USE_ALTERNATE_SPLIT, sc.useAlternateSplit)
        edit.putBoolean(ALLOW_SLOW_DUALFLOAT, sc.allowSlowDualfloat)
        edit.putInt(CHUNK_PROFILE, sc.chunkProfile.ordinal)
        edit.apply()

        super.onPause()
        fsv.onPause()

    }
    override fun onResume() {

        super.onResume()
        if (fragmentsCompleted == 5) queryPurchases()
        fsv.onResume()

    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        //Log.d("MAIN ACTIVITY", "window focus changed")
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) updateSystemUI()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
        ChangelogSetup.get().registerTag(GoldTag())
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
            Log.d("MAIN", "processing purchase...")
            Log.d("MAIN", originalJson)
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
        purchaseQueryResult.purchasesList?.getOrNull(0)?.apply {
            Log.d("MAIN", "processing purchase...")
            Log.d("MAIN", originalJson)
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
        if (purchaseQueryResult.purchasesList == null) Log.d("MAIN", "purchaseList is null")
        else Log.d("MAIN", "purchaseList size: ${purchaseQueryResult.purchasesList?.size}")
        if (purchaseQueryResult.purchasesList?.size == 0) {
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
        ( imageFragment    as ImageFragment    ).onGoldEnabled()
    }

    fun getAvailableHeapMemory() : Long {
        val runtime = Runtime.getRuntime()
        val usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
        val maxHeapSizeInMB = runtime.maxMemory() / 1048576L
        return maxHeapSizeInMB - usedMemInMB
    }
    fun showUpgradeScreen() {

        // double check to avoid showing upgrade screen if not necessary
        if (!billingClient.isReady) billingClient.startConnection(billingClientStateListener)
        else queryPurchases()

        if (!sc.goldEnabled) {
            val myIntent = Intent(this, UpgradeActivity::class.java)
            startActivity(myIntent)
        }

    }

    fun onRateButtonClicked(): Boolean {
        Toast.makeText(this, "Rate button was clicked", Toast.LENGTH_LONG).show()
        // button click handled
        return true
    }

    override fun onComplete() {
        fragmentsCompleted++
        Log.d("MAIN", "!! fragment callback !! $fragmentsCompleted fragments completed")
        if (fragmentsCompleted == 5) {
            if (!billingClient.isReady) billingClient.startConnection(billingClientStateListener)
            else queryPurchases()
        }
    }


}
