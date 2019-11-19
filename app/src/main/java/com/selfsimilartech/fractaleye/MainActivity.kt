package com.selfsimilartech.fractaleye

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import android.view.*
import android.view.animation.AlphaAnimation
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



operator fun Double.times(w: Complex) : Complex {
    return Complex(this*w.x, this*w.y)
}

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


enum class Precision(val threshold: Double) {
    SINGLE(5e-4), DUAL(1e-12), QUAD(1e-20)
}
enum class Reaction(val numDisplayParams: Int) {
    POSITION(4), COLOR(2), P1(3), P2(3), P3(3), P4(3)
}
enum class Resolution(val scale: Int) {
    LOW(8), MED(3), HIGH(1)
}



class SettingsConfig (
    var resolution          : Resolution    = Resolution.HIGH,
    var precision           : Precision     = Precision.SINGLE,
    var autoPrecision       : Boolean       = true,
    var continuousRender    : Boolean       = false,
    var displayParams       : Boolean       = true
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
        set(value) { if (!rotationLocked) { field = value } }


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
        val default : Position = Position(),
        val julia   : Position = Position(scale = 3.5)
)




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


class MainActivity : AppCompatActivity() {

    private lateinit var f : Fractal
    private lateinit var fsv : FractalSurfaceView
    private lateinit var sc : SettingsConfig
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



        // get initial values for fractalConfig

//        val map = ComplexMap.all[savedInstanceState?.getString("map")] ?: ComplexMap.mandelbrot
//        map.position.x = savedInstanceState?.getDouble("x") ?: map.position.x
//        map.position.y = savedInstanceState?.getDouble("y") ?: map.position.y
//        map.position.scale = savedInstanceState?.getDouble("scale") ?: map.position.scale
//        map.position.rotation = savedInstanceState?.getDouble("rotation") ?: map.position.rotation
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

        
        sc = SettingsConfig()

        // create fractal
        f = Fractal()

//        if (orientationChanged) {
//            f.switchOrientation()
//            Log.d("MAIN", "orientation changed")
//        }


        fsv = FractalSurfaceView(f, sc, this, intArrayOf(screenWidth, screenHeight))
        fsv.layoutParams = ViewGroup.LayoutParams(screenWidth, screenHeight)
        fsv.hideSystemUI()

        setContentView(R.layout.activity_main)

        val fractalLayout = findViewById<FrameLayout>(R.id.layout_main)
        fractalLayout.addView(fsv)


        val displayParams = findViewById<LinearLayout>(R.id.displayParams)
        displayParamRows = listOf(
            findViewById(R.id.displayParamRow1),
            findViewById(R.id.displayParamRow2),
            findViewById(R.id.displayParamRow3),
            findViewById(R.id.displayParamRow4)
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
            fsv.reaction = Reaction.valueOf(s)
            (displayParams.getChildAt(0) as TextView).text = when (fsv.reaction) {
                Reaction.POSITION, Reaction.COLOR -> s
                else -> "PARAMETER ${s[1]}"
            }

            if (sc.displayParams) {
                displayParams.removeViews(1, displayParams.childCount - 1)
                for (i in 0 until fsv.reaction.numDisplayParams) {
                    displayParams.addView(displayParamRows[i])
                }
            }

            for (b in uiQuickButtons) {
                val btd = b.background as TransitionDrawable
                if (b == it) { btd.startTransition(0) }
                else { btd.resetTransition() }
            }
            updateDisplayParams(Reaction.valueOf(s), true)
        }
        for (b in uiQuickButtons) {
            b.background = TransitionDrawable(buttonBackgrounds)
            b.setOnClickListener(uiQuickButtonListener)
        }
        val diff = f.map.params.size - uiQuick.childCount + 2
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
        val fractalEditFragment = FractalEditFragment()
        val settingsFragment = SettingsFragment()
        // val saveFragment = SaveFragment()

        fractalEditFragment.f = f
        fractalEditFragment.fsv = fsv

        settingsFragment.f = f
        settingsFragment.fsv = fsv
        settingsFragment.sc = sc






        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFrag( fractalEditFragment,  "Fractal" )
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
        // fsv.y = -uiFullHeightOpen/2f

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
                fsv.y = -uiFull.height/2.0f
            }
            anim.duration = 300
            anim.start()
        }

        val overlay = findViewById<ConstraintLayout>(R.id.overlay)
        overlay.bringToFront()

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)


    }


    fun addMapParams(n: Int) {
        val uiQuick = findViewById<LinearLayout>(R.id.uiQuick)
        for (i in 1..n) { uiQuick.addView(uiQuickButtons[uiQuick.childCount], 0) }
    }
    fun removeMapParams(n: Int) {
        val uiQuick = findViewById<LinearLayout>(R.id.uiQuick)
        for (i in 1..n) { uiQuick.removeView(uiQuickButtons[uiQuick.childCount - 1]) }
    }

    fun displayMessage(msg: String) {
        val buttons = findViewById<LinearLayout>(R.id.buttons)
        val toast = Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
        val toastHeight = fsv.screenRes[1] - buttons.y.toInt() + buttons.height
        toast.setGravity(Gravity.BOTTOM, 0, toastHeight)
        toast.show()
    }
    fun updateDisplayParams(reaction: Reaction, reactionChanged: Boolean = false, settingsChanged: Boolean = false) {

        val displayParams = findViewById<LinearLayout>(R.id.displayParams)

        if (settingsChanged) {
            if (sc.displayParams) {
                for (i in 0 until fsv.reaction.numDisplayParams) {
                    displayParams.addView(displayParamRows[i])
                }
            }
            else {
                displayParams.removeViews(1, displayParams.childCount - 1)
            }
        }

        val displayParam1 = findViewById<TextView>(R.id.displayParam1)
        val displayParam2 = findViewById<TextView>(R.id.displayParam2)
        val displayParam3 = findViewById<TextView>(R.id.displayParam3)
        val displayParam4 = findViewById<TextView>(R.id.displayParam4)
        val displayParamName1 = findViewById<TextView>(R.id.displayParamName1)
        val displayParamName2 = findViewById<TextView>(R.id.displayParamName2)
        val displayParamName3 = findViewById<TextView>(R.id.displayParamName3)
        val displayParamName4 = findViewById<TextView>(R.id.displayParamName4)
        val density = resources.displayMetrics.density
        val w : Int


        if (sc.displayParams) {
            when (reaction) {
                Reaction.POSITION -> {

                    displayParamName1.text = resources.getString(R.string.x)
                    displayParamName2.text = resources.getString(R.string.y)
                    displayParamName3.text = resources.getString(R.string.scale_lower)
                    displayParamName4.text = resources.getString(R.string.rotation_lower)
                    displayParam1.text = "%.17f".format(f.map.position.x)
                    displayParam2.text = "%.17f".format(f.map.position.y)
                    displayParam3.text = "%e".format(f.map.position.scale)
                    displayParam4.text = "%.0f".format(f.map.position.rotation * 180.0 / Math.PI)
                    w = (60f * density).toInt()

                }
                Reaction.COLOR -> {

                    displayParamName1.text = resources.getString(R.string.frequency)
                    displayParamName2.text = resources.getString(R.string.offset)
                    displayParam1.text = "%.4f".format(f.frequency)
                    displayParam2.text = "%.4f".format(f.phase)
                    w = (75f * density).toInt()

                }
                else -> {

                    val i = reaction.ordinal - 2
                    displayParamName1.text = "u"
                    displayParamName2.text = "v"
                    displayParamName3.text = resources.getString(R.string.sensitivity)
                    displayParam1.text = "%.8f".format((f.map.params[i].u))
                    displayParam2.text = "%.8f".format((f.map.params[i].v))
                    displayParam3.text = "%.4f".format(f.sensitivity)
                    w = (80f * density).toInt()

                }
            }

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

        }
        if (sc.displayParams || reactionChanged) {
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 1000L
            fadeOut.startOffset = 2500L
            fadeOut.fillAfter = true
            displayParams.animation = fadeOut
            displayParams.animation.start()
            displayParams.requestLayout()
        }

    }
    fun updateMapParamEditText(i: Int) {
        // Log.d("FRACTAL", "updating map param EditText $i")

        val xEdit : EditText?
        val yEdit : EditText?

        when (i) {
            1 -> {
                xEdit = findViewById(R.id.u1Edit)
                yEdit = findViewById(R.id.v1Edit)
            }
            2 -> {
                xEdit = findViewById(R.id.u2Edit)
                yEdit = findViewById(R.id.v2Edit)
            }
            3 -> {
                xEdit = findViewById(R.id.u3Edit)
                yEdit = findViewById(R.id.v3Edit)
            }
            4 -> {
                xEdit = findViewById(R.id.u4Edit)
                yEdit = findViewById(R.id.v4Edit)
            }
            else -> {
                xEdit = null
                yEdit = null
            }
        }

        xEdit?.setText("%.8f".format((f.map.params[i-1].u)))
        yEdit?.setText("%.8f".format((f.map.params[i-1].v)))

    }
    fun updateShapeEditTexts() {

        val bailoutSignificandEdit = findViewById<EditText>(R.id.bailoutSignificandEdit)
        val bailoutExponentEdit = findViewById<EditText>(R.id.bailoutExponentEdit)
        val bailoutStrings = "%e".format(f.bailoutRadius).split("e")
        bailoutSignificandEdit?.setText("%.5f".format(bailoutStrings[0].toFloat()))
        bailoutExponentEdit?.setText("%d".format(bailoutStrings[1].toInt()))

        for (i in 1..NUM_MAP_PARAMS) {
            updateMapParamEditText(i)
        }

        updatePositionEditTexts()

    }
    fun updateTextureEditTexts() {

        val q1Edit = findViewById<EditText>(R.id.q1Edit)
        val q2Edit = findViewById<EditText>(R.id.q2Edit)

        q1Edit?.setText("%.3f".format(f.texture.params[0].t))
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
        val scaleStrings = "%e".format(f.map.position.scale).split("e")
        val rotationEdit = findViewById<EditText>(R.id.rotationEdit)

//        Log.w("FRACTAL", "scaleExponent: %d".format(scaleStrings[1].toInt()))
//        Log.w("FRACTAL", "bailoutExponent: %d".format(bailoutStrings[1].toInt()))

        xCoordEdit?.setText("%.17f".format(f.map.position.x))
        yCoordEdit?.setText("%.17f".format(f.map.position.y))

        scaleSignificandEdit?.setText("%.5f".format(scaleStrings[0].toFloat()))
        scaleExponentEdit?.setText("%d".format(scaleStrings[1].toInt()))

        rotationEdit?.setText("%.0f".format(f.map.position.rotation * 180.0 / Math.PI))

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) { fsv.hideSystemUI() }
    }
    override fun onSaveInstanceState(outState: Bundle?) {

//        Log.d("MAIN ACTIVITY", "saving instance state !! outState: ${outState == null}")

//        outState?.putString("map", f.map.name)
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
