package com.selfsimilartech.fractaleye

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.*
import java.util.*
import kotlin.math.*
import android.view.MotionEvent
import android.content.Context
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector




const val SPLIT = 8193.0
const val NUM_MAP_PARAMS = 3
const val NUM_TEXTURE_PARAMS = 2
const val WRITE_STORAGE_REQUEST_CODE = 0
const val ITER_MAX_POW = 12.0
const val ITER_MIN_POW = 5.0
//const val PLUS_UNICODE = '\u002B'
//const val MINUS_UNICODE = '\u2212'



operator fun Double.times(w: Complex) : Complex {
    return Complex(this*w.x, this*w.y)
}
fun FrameLayout.getChildAtFront() : View? {
    return this.getChildAt(this.childCount - 1)
}
fun FrameLayout.hasAtFront(view: View) : Boolean = this.getChildAtFront()?.equals(view) ?: false
fun TabLayout.getCurrentTab() : TabLayout.Tab? = this.getTabAt(this.selectedTabPosition)

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


enum class Category(val icon: Int) {
    SETTINGS(R.drawable.settings),
    TEXTURE(R.drawable.texture),
    SHAPE(R.drawable.shape),
    COLOR(R.drawable.color),
    POSITION(R.drawable.position)
}
enum class Precision(val threshold: Double) {
    SINGLE(5e-4), DUAL(1e-12), QUAD(1e-20)
}
enum class Reaction(val numDisplayParams: Int) {
    POSITION(4), COLOR(2), SHAPE(3)
}
enum class Resolution(val scale: Int) {
    LOW(8), MED(3), HIGH(1)
}



class SettingsConfig (
    var resolution          : Resolution    = Resolution.HIGH,
    var precision           : Precision     = Precision.SINGLE,
    var autoPrecision       : Boolean       = true,
    var continuousRender    : Boolean       = false,
    var displayParams       : Boolean       = false
)


class Position(
        x: Double = 0.0,
        y: Double = 0.0,
        scale: Double = 1.0,
        rotation: Double = 0.0
) {

    private val xInit = x
    private val yInit = y
    private val scaleInit = scale
    private val rotationInit = rotation

    var x = x
        set(value) { if (!xLocked) { field = value } }

    var y = y
        set(value) { if (!yLocked) { field = value } }

    var scale = scale
        set(value) { if (!scaleLocked) { field = value } }

    var rotation = rotation
        set(value) { if (!rotationLocked) {
            val tau = 2.0*Math.PI
            field = if (value < 0) (tau + value) % tau else value % tau
        }}


    var xLocked: Boolean = false
    var yLocked: Boolean = false
    var scaleLocked: Boolean = false
    var rotationLocked: Boolean = false

    private fun translate(dx: Double, dy: Double) {

        x += dx
        y += dy

    }
    fun translate(dx: Float, dy: Float) {  // dx, dy --> [0, 1]

        val tx = dx*scale
        val ty = dy*scale
        val sinTheta = sin(-rotation)
        val cosTheta = cos(rotation)
        x -= tx*cosTheta - ty*sinTheta
        y += tx*sinTheta + ty*cosTheta

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
    }

}

class PositionList(
        val default  : Position = Position(),
        val julia    : Position = Position(scale = 3.5),
        val other    : List<Position> = listOf()
)


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





interface ClickListener {
    fun onClick(view: View, position: Int)

    fun onLongClick(view: View, position: Int)
}

class NoScrollViewPager : ViewPager {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

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
        }
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
}





class MainActivity : AppCompatActivity() {

    private var f : Fractal = Fractal.mandelbrot
    private var sc : SettingsConfig = SettingsConfig()
    private lateinit var fsv : FractalSurfaceView

    // private var orientation = Configuration.ORIENTATION_UNDEFINED


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



        // get initial values for fractalConfig

//        val shape = Shape.all[savedInstanceState?.getString("shape")] ?: Shape.mandelbrot
//        shape.position.x = savedInstanceState?.getDouble("x") ?: shape.position.x
//        shape.position.y = savedInstanceState?.getDouble("y") ?: shape.position.y
//        shape.position.scale = savedInstanceState?.getDouble("scale") ?: shape.position.scale
//        shape.position.rotation = savedInstanceState?.getDouble("rotation") ?: shape.position.rotation
//
//        val juliaMode = savedInstanceState?.getBoolean("juliaMode") ?: false
//        Log.e("MAIN ACTIVITY", "texture: ${savedInstanceState?.getString("texture")}")
//        val texture = Texture.all[savedInstanceState?.getString("texture")] ?: Texture.escape
//        Log.e("MAIN ACTIVITY", "texture: ${texture.name}")
//        val palette = ColorPalette.all[savedInstanceState?.getString("palette")] ?: ColorPalette.p9
//        val frequency = savedInstanceState?.getFloat("frequency") ?: 1f
//        val phase = savedInstanceState?.getFloat("phase") ?: 0f
//
//        val maxIter = savedInstanceState?.getInt("maxIter") ?: 255
//        val bailoutRadius = savedInstanceState?.getFloat("bailoutRadius") ?: 1e5f
//        val sensitivity = savedInstanceState?.getDouble("paramSensitivity") ?: 1.0




        // get initial values for settingsConfig

//        val resolution = Resolution.valueOf(savedInstanceState?.getString("resolution") ?: Resolution.HIGH.name)
//        val precision = Precision.valueOf(savedInstanceState?.getString("precision") ?: Precision.SINGLE.name)
//        val autoPrecision = savedInstanceState?.getBoolean("autoPrecision") ?: true
//        val continuousRender = savedInstanceState?.getBoolean("continuousRender") ?: false
//        val displayParamsBoolean = savedInstanceState?.getBoolean("displayParams") ?: true


//        if (orientationChanged) {
//            f.switchOrientation()
//            Log.d("MAIN", "orientation changed")
//        }


        fsv = FractalSurfaceView(f, sc, this, intArrayOf(screenWidth, screenHeight))
        fsv.layoutParams = ViewGroup.LayoutParams(screenWidth, screenHeight)
        fsv.hideSystemUI()

        setContentView(R.layout.activity_main2)

        val fractalLayout = findViewById<FrameLayout>(R.id.layout_main)
        fractalLayout.addView(fsv)


        val displayParams = findViewById<LinearLayout>(R.id.displayParams)
        val ui = findViewById<LinearLayout>(R.id.ui)
        val overlay = findViewById<ConstraintLayout>(R.id.overlay)
        val categoryPager = findViewById<NoScrollViewPager>(R.id.categoryPager)
        val categoryTabs = findViewById<TabLayout>(R.id.categoryButtons)
        val categoryNameButton = findViewById<Button>(R.id.categoryNameButton)

        val displayParamRows = listOf<LinearLayout>(
                findViewById(R.id.displayParamRow1),
                findViewById(R.id.displayParamRow2),
                findViewById(R.id.displayParamRow3),
                findViewById(R.id.displayParamRow4)
        )
        displayParamRows.forEach { it.visibility = LinearLayout.GONE }
        updateDisplayParams(reactionChanged = true, settingsChanged = true)

        val pagerHeightOpen = (175*resources.displayMetrics.density).toInt()
        val uiHeightOpen = (255*resources.displayMetrics.density).toInt()
        val uiHeightClosed = uiHeightOpen - pagerHeightOpen
        categoryPager.layoutParams.height = 1
        fsv.y = -uiHeightClosed/2f
        categoryNameButton.setOnClickListener {

            val hStart : Int
            val hEnd : Int

            if (categoryPager.height == 1) {
                hStart = 1
                hEnd = pagerHeightOpen
                (it as Button).setCompoundDrawablesWithIntrinsicBounds(null, null, resources.getDrawable(R.drawable.collapse, null), null)
            }
            else {
                hStart = pagerHeightOpen
                hEnd = 1
                (it as Button).setCompoundDrawablesWithIntrinsicBounds(null, null, resources.getDrawable(R.drawable.expand, null), null)
            }

            val anim = ValueAnimator.ofInt(hStart, hEnd)
            anim.addUpdateListener { animation ->
                val intermediateHeight = animation?.animatedValue as Int
                categoryPager.layoutParams.height = intermediateHeight
                categoryPager.requestLayout()
                fsv.y = -ui.height/2f
                if (hEnd == 1 && anim.animatedFraction == 1f && categoryTabs.getCurrentTab()
                        ?.contentDescription?.equals(Category.SETTINGS.name) == true) {
                    categoryTabs.getTabAt(4)?.select()
                } // ew
            }
            anim.duration = 200
            anim.start()

        }


        val categoryPagerAdapter = ViewPagerAdapter(supportFragmentManager)

        val settingsFragment  = SettingsFragment()
        val textureFragment   = TextureFragment()
        val shapeFragment     = ShapeFragment()
        val colorFragment     = ColorFragment()
        val positionFragment  = PositionFragment()

        settingsFragment.passArguments(f, fsv, sc)
        textureFragment.passArguments(f, fsv)
        shapeFragment.passArguments(f, fsv)
        colorFragment.passArguments(f, fsv)
        positionFragment.passArguments(f, fsv)


        categoryPagerAdapter.addFrag(settingsFragment)
        categoryPagerAdapter.addFrag(textureFragment)
        categoryPagerAdapter.addFrag(shapeFragment)
        categoryPagerAdapter.addFrag(colorFragment)
        categoryPagerAdapter.addFrag(positionFragment)
        categoryPager.adapter = categoryPagerAdapter
        categoryPager.offscreenPageLimit = 4


        categoryTabs.setupWithViewPager(categoryPager)
        for (i in 0..4) {
            categoryTabs.getTabAt(i)?.contentDescription = Category.values()[i].name
            val transparentIcon = resources.getDrawable(Category.values()[i].icon, null)
            transparentIcon.alpha = 128
            categoryTabs.getTabAt(i)?.icon = transparentIcon
        }

        categoryTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {

                tab.icon?.alpha = 255

                val s = tab.contentDescription.toString()
                Log.e("MAIN ACTIVITY", "tab selected: $s")

                fsv.reaction = when (s) {
                    Category.COLOR.name, Category.POSITION.name-> Reaction.valueOf(s)
                    Category.SHAPE.name -> if (f.numParamsInUse != 0) Reaction.SHAPE else Reaction.POSITION
                    Category.TEXTURE.name, Category.SETTINGS.name -> Reaction.POSITION
                    else -> Reaction.POSITION
                }

                if (s == Category.SETTINGS.name && categoryPager.height == 1) {
                    categoryNameButton.performClick()
                }

                categoryNameButton.text = s
                updateDisplayParams(reactionChanged = true)

            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.icon?.alpha = 128
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                categoryNameButton.performClick()
                onTabSelected(tab)
            }

        })
        categoryTabs.getTabAt(categoryTabs.tabCount - 1)?.select()



//        val buttonScroll = findViewById<HorizontalScrollView>(R.id.buttonScroll1)
//        val leftArrow = findViewById<ImageView>(R.id.leftArrow)
//        val rightArrow = findViewById<ImageView>(R.id.rightArrow)
//        leftArrow.alpha = 0f
//        rightArrow.alpha = 0f

//        buttonScroll.viewTreeObserver.addOnScrollChangedListener {
//            if (uiQuick.width > buttonScroll.width) {
//                val scrollX = buttonScroll.scrollX
//                val scrollEnd = uiQuick.width - buttonScroll.width
                // Log.d("MAIN ACTIVITY", "scrollX: $scrollX")
                // Log.d("MAIN ACTIVITY", "scrollEnd: $scrollEnd")
//                when {
//                    scrollX > 5 -> leftArrow.alpha = 1f
//                    scrollX < 5 -> leftArrow.alpha = 0f
//                }
//                when {
//                    scrollX < scrollEnd - 5 -> rightArrow.alpha = 1f
//                    scrollX > scrollEnd - 5 -> rightArrow.alpha = 0f
//                }
//            }
//            else {
//                leftArrow.alpha = 0f
//                rightArrow.alpha = 0f
//            }
//        }


        overlay.bringToFront()
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

    }


    fun displayMessage(msg: String) {
        val ui = findViewById<LinearLayout>(R.id.ui)
        val toast = Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
        val toastHeight = fsv.screenRes[1] - ui.height
        toast.setGravity(Gravity.BOTTOM, 0, toastHeight)
        toast.show()
    }
    fun updateDisplayParams(reactionChanged: Boolean = false, settingsChanged: Boolean = false) {

        val displayParams = findViewById<LinearLayout>(R.id.displayParams)
        val displayParamRows = listOf<LinearLayout>(
                findViewById(R.id.displayParamRow1),
                findViewById(R.id.displayParamRow2),
                findViewById(R.id.displayParamRow3),
                findViewById(R.id.displayParamRow4)
        )

        if (sc.displayParams) {

            if (settingsChanged) displayParams.visibility = LinearLayout.VISIBLE
            if (reactionChanged) displayParamRows.forEach { it.visibility = LinearLayout.GONE }
            if (reactionChanged || settingsChanged)
                for (i in 0 until fsv.reaction.numDisplayParams)
                displayParamRows[i].visibility = LinearLayout.VISIBLE

            val displayParam1 = findViewById<TextView>(R.id.displayParam1)
            val displayParam2 = findViewById<TextView>(R.id.displayParam2)
            val displayParam3 = findViewById<TextView>(R.id.displayParam3)
            val displayParam4 = findViewById<TextView>(R.id.displayParam4)
            val displayParamName1 = findViewById<TextView>(R.id.displayParamName1)
            val displayParamName2 = findViewById<TextView>(R.id.displayParamName2)
            val displayParamName3 = findViewById<TextView>(R.id.displayParamName3)
            val displayParamName4 = findViewById<TextView>(R.id.displayParamName4)
            val density = resources.displayMetrics.density
            val w: Int

            // update text content
            when (fsv.reaction) {
                Reaction.POSITION -> {

                    displayParamName1.text = resources.getString(R.string.x)
                    displayParamName2.text = resources.getString(R.string.y)
                    displayParamName3.text = resources.getString(R.string.zoom)
                    displayParamName4.text = resources.getString(R.string.rotation_lower)
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
                    displayParam1.text = "%.8f".format((f.shape.activeParam.u))
                    displayParam2.text = "%.8f".format((f.shape.activeParam.v))
                    displayParam3.text = "%.6f".format(f.sensitivity)
                    w = (80f * density).toInt()

                }
            }

            // update width
            displayParamName1.width = w
            displayParamName2.width = w
            displayParamName3?.width = w
            displayParamName4?.width = w
            displayParamName1.requestLayout()
            displayParamName2.requestLayout()
            displayParamName3?.requestLayout()
            displayParamName4?.requestLayout()
            displayParam1.requestLayout()
            displayParam2.requestLayout()
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
    fun updateShapeParamEditTexts() {
        // Log.d("FRACTAL", "updating shape param EditText $i")

        val xEdit = findViewById<EditText>(R.id.uEdit)
        val yEdit = findViewById<EditText>(R.id.vEdit)

        xEdit?.setText("%.8f".format((f.shape.activeParam.u)))
        yEdit?.setText("%.8f".format((f.shape.activeParam.v)))

    }
    fun updateShapeEditTexts() {

        val bailoutSignificandEdit = findViewById<EditText>(R.id.bailoutSignificandEdit)
        val bailoutExponentEdit = findViewById<EditText>(R.id.bailoutExponentEdit)
        val bailoutStrings = "%e".format(f.bailoutRadius).split("e")
        bailoutSignificandEdit?.setText("%.5f".format(bailoutStrings[0].toFloat()))
        bailoutExponentEdit?.setText("%d".format(bailoutStrings[1].toInt()))

        updateShapeParamEditTexts()
        updatePositionEditTexts()

    }
    fun updateTextureEditTexts() {

        val q1Edit = findViewById<EditText>(R.id.q1Edit)
        val q2Edit = findViewById<EditText>(R.id.q2Edit)

        q1Edit?.setText("%d".format(f.texture.params[0].t.toInt()))
        q2Edit?.setText("%.3f".format(f.texture.params[1].t))

    }
    fun updateColorEditTexts() {

        val frequencyEdit = findViewById<EditText>(R.id.frequencyEdit)
        val phaseEdit = findViewById<EditText>(R.id.phaseEdit)

        frequencyEdit?.setText("%.5f".format(f.frequency))
        phaseEdit?.setText("%.5f".format(f.phase))

    }
    fun updatePositionEditTexts() {

        val xCoordEdit = findViewById<EditText>(R.id.xCoordEdit)
        val yCoordEdit = findViewById<EditText>(R.id.yCoordEdit)
        val scaleSignificandEdit = findViewById<EditText>(R.id.scaleSignificandEdit)
        val scaleExponentEdit = findViewById<EditText>(R.id.scaleExponentEdit)
        val scaleStrings = "%e".format(f.position.scale).split("e")
        val rotationEdit = findViewById<EditText>(R.id.rotationEdit)

//        Log.w("FRACTAL", "scaleExponent: %d".format(scaleStrings[1].toInt()))
//        Log.w("FRACTAL", "bailoutExponent: %d".format(bailoutStrings[1].toInt()))

        xCoordEdit?.setText("%.17f".format(f.position.x))
        yCoordEdit?.setText("%.17f".format(f.position.y))

        scaleSignificandEdit?.setText("%.5f".format(scaleStrings[0].toFloat()))
        scaleExponentEdit?.setText("%d".format(scaleStrings[1].toInt()))

        rotationEdit?.setText("%.0f".format(f.position.rotation * 180.0 / Math.PI))

    }

    override fun onPause() {
        super.onPause()
        fsv.onPause()
    }
    override fun onResume() {
        super.onResume()
        fsv.onResume()
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) { fsv.hideSystemUI() }
    }
    override fun onSaveInstanceState(outState: Bundle?) {

//        Log.d("MAIN ACTIVITY", "saving instance state !! outState: ${outState == null}")

//        outState?.putString("shape", f.shape.name)
//        outState?.putString("texture", f.texture.name)
//        outState?.putBoolean("juliaMode", f.juliaMode)
//        outState?.putFloat("bailoutRadius", f.bailoutRadius)
//        outState?.putInt("maxIter", f.maxIter)
//        outState?.putDouble("sensitivity", f.sensitivity)
//        outState?.putString("palette", f.palette.name)
//        outState?.putFloat("frequency", f.frequency)
//        outState?.putFloat("phase", f.phase)

//        outState?.putString("resolution", sc.resolution.name)
//        outState?.putString("precision", sc.precision.name)
//        outState?.putBoolean("continuousRender", sc.continuousRender)
//        outState?.putBoolean("displayParams", sc.displayParams)

        super.onSaveInstanceState(outState)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            WRITE_STORAGE_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    fsv.r.saveImage = true
                    fsv.requestRender()
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
