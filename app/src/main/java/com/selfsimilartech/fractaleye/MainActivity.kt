package com.selfsimilartech.fractaleye

import android.Manifest
import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.database.Cursor
import android.graphics.*
import android.net.Uri
import android.os.*
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.database.getStringOrNull
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.viewpager.widget.ViewPager
import com.android.billingclient.api.*
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.crashlytics.ktx.setCustomKeys
import com.jaredrummler.android.device.DeviceName
import com.michaelflisar.changelog.ChangelogBuilder
import com.selfsimilartech.fractaleye.databinding.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.*


const val MAX_SHAPE_PARAMS = 4
const val MAX_TEXTURE_PARAMS = 4
const val WRITE_STORAGE_REQUEST_CODE = 0
const val ITER_MAX_POW = 16.0
const val ITER_MIN_POW = 0.0
const val MAX_FRAMERATE = 120
const val MIN_FRAMERATE = 24
const val BUTTON_CLICK_DELAY_SHORT = 100L
const val BUTTON_CLICK_DELAY_MED = 200L
const val BUTTON_CLICK_DELAY_LONG = 400L
const val PAGER_ANIM_DURATION = 200L
const val STATUS_BAR_HEIGHT = 24
const val NAV_BAR_HEIGHT = 48
const val COMPLEX_PARAM_DIGITS = 7
const val REAL_PARAM_DIGITS = 5
const val GOLD_ENABLED_DIALOG_SHOWN = "goldEnabledDialogShown"
const val GOLD_PENDING_DIALOG_SHOWN = "goldPendingDialogShown"
const val SHOW_EPILEPSY_DIALOG = "showEpilepsyDialog"
const val SHOW_TUTORIAL_OPTION = "showTutorialOption"
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
const val RESTRICT_PARAMS = "restrictParams"
const val ALLOW_SLOW_DUALFLOAT = "allowSlowDualfloat"
const val CHUNK_PROFILE = "chunkProfile"
const val TARGET_FRAMERATE = "targetFramerate"

const val VERSION_CODE_TAG = "versionCode"
const val SHARED_PREFERENCES = "com.selfsimilartech.fractaleye.SETTINGS"

const val PALETTE_TABLE_NAME = "palette"
const val SHAPE_TABLE_NAME = "shape"
const val FRACTAL_TABLE_NAME = "fractal"
const val TEX_IM_PREFIX = "tex_im_"

const val CRASH_KEY_ACT_MAIN_CREATED = "activity_main_created"
const val CRASH_KEY_FRAG_POS_CREATED = "fragment_position_created"
const val CRASH_KEY_FRAG_COLOR_CREATED = "fragment_color_created"
const val CRASH_KEY_FRAG_SHAPE_CREATED = "fragment_shape_created"
const val CRASH_KEY_FRAG_TEX_CREATED = "fragment_texture_created"
const val CRASH_KEY_MAX_ITER = "max_iterations"
const val CRASH_KEY_RESOLUTION = "resolution"
const val CRASH_KEY_GOLD_ENABLED = "gold_enabled"
const val CRASH_KEY_SHAPE_NAME = "shape_name"
const val CRASH_KEY_PALETTE_NAME = "palette_name"
const val CRASH_KEY_TEXTURE_NAME = "texture_name"
const val CRASH_KEY_LAST_ACTION = "last_action"

const val AP_DIGITS = 64L



class MainActivity : AppCompatActivity(), OnCompleteListener {


    lateinit var b : ActivityMainBinding
    lateinit var extrasMenu : ExtrasMenuBinding
    lateinit var tut : TutorialBinding
    lateinit var tutw : TutorialWelcomeBinding
    lateinit var db : AppDatabase
    lateinit var fsv : FractalSurfaceView

    var fragmentsCompleted = 0
    val f = Fractal.default
    var sc = SettingsConfig
    var uiLayoutHeight = UiLayoutHeight.CLOSED_SHORT

    private var activityInitialized = false
    private var screenWidth = 0
    private var screenHeight = 0
    private var navBarHeight = 0
    private var statusBarHeight = 0
    private var deviceHasNotch = false
    private var texturesDisabled = false
    private var goldEnabledDialogShown = false
    private var goldPendingDialogShown = false
    private var showEpilepsyDialog = true
    private var showTutorialOption = true
    private var tutorialFromSettings = false
    private var tutorialLayoutInflated = false
    private var dialog : AlertDialog? = null

    var previousFractalCreated = false
    var previousFractalId = -1


    private lateinit var settingsFragment   : Fragment
    private lateinit var textureFragment    : TextureFragment
    private lateinit var shapeFragment      : ShapeFragment
    private lateinit var colorFragment      : ColorFragment
    private lateinit var positionFragment   : PositionFragment


    private lateinit var billingClient : BillingClient
    private val billingClientStateListener = object : BillingClientStateListener {

        override fun onBillingSetupFinished(billingResult: BillingResult) {
            Log.e("MAIN", "billing setup finished")
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> lifecycleScope.launch { queryPurchases() }
                else -> {
                    if (BuildConfig.DEV_VERSION) lifecycleScope.launch { queryPurchases() }
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
                Log.e("MAIN", "purchases updated")
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

    enum class EditMode(val displayName: Int, val icon: Int) {

        TEXTURE(R.string.texture, R.drawable.texture) {
            override fun onDetermineMenuHeightOpen(act: MainActivity) {
                act.apply {
                    uiSetHeight(if (textureFragment.b.textureImageLayout.isVisible()) UiLayoutHeight.MED else UiLayoutHeight.SHORT)
                }
            }
            override fun onDetermineMenuHeightClosed(act: MainActivity) {
                act.apply {
                    uiSetHeight(if (textureFragment.b.textureImageLayout.isVisible()) UiLayoutHeight.CLOSED_MED else UiLayoutHeight.CLOSED_SHORT)
                }
            }
            override fun onMenuClosed(act: MainActivity) {
                // if (act.fsv.r.reaction == Reaction.NONE && !act.textureListLayout.isVisible()) act.categoryButtons.getTabAt(POSITION).select()
            }
            override fun onCategorySelected(act: MainActivity) {
                super.onCategorySelected(act)
                act.apply {
                    fsv.r.reaction = if (
                        textureFragment.b.realTextureParam.root.isVisible() ||
                        textureFragment.b.complexTextureParam.root.isVisible()
                    ) Reaction.TEXTURE else Reaction.NONE
                }
            }
            override fun onCategoryUnselected(act: MainActivity) {
                // onMenuClosed(act)
            }
        },
        SHAPE(R.string.shape, R.drawable.shape) {
            override fun onMenuClosed(act: MainActivity) {
//                act.apply {
//                    if (fsv.r.reaction == Reaction.NONE && !(shapeListLayout.isVisible() || customShapeLayout.isVisible())) categoryButtons.getTabAt(POSITION).select()
//                }
            }
            override fun onCategorySelected(act: MainActivity) {
                super.onCategorySelected(act)
                act.apply {
                    fsv.r.reaction = if (
                        shapeFragment.b.realShapeParam.root.isVisible() ||
                        shapeFragment.b.complexShapeParam.root.isVisible()
                    ) Reaction.SHAPE else Reaction.NONE
                }
            }
            override fun onCategoryUnselected(act: MainActivity) {

            }
        },
        COLOR(R.string.color, R.drawable.color) {
            override fun onDetermineMenuHeightOpen(act: MainActivity) {
                act.apply { uiSetHeight(if (colorFragment.b.miniColorPickerLayout.isVisible()) UiLayoutHeight.MED else UiLayoutHeight.SHORT) }
            }
            override fun onDetermineMenuHeightClosed(act: MainActivity) {
                act.apply { uiSetHeight(if (colorFragment.b.miniColorPickerLayout.isVisible()) UiLayoutHeight.CLOSED_MED else UiLayoutHeight.CLOSED_SHORT) }
            }
            override fun onMenuClosed(act: MainActivity) {}
            override fun onCategorySelected(act: MainActivity) {
                act.apply {
                    super.onCategorySelected(act)
                    colorFragment.b.densityButton. apply { if (f.texture.usesDensity && sc.autofitColorRange) show() else hide() }
                    fsv.r.reaction = Reaction.COLOR
                    // showTouchIcon()
                }
            }
            override fun onCategoryUnselected(act: MainActivity) {}
        },
        POSITION(R.string.position, R.drawable.position) {
            override fun onMenuClosed(act: MainActivity) {}
            override fun onCategorySelected(act: MainActivity) {
                super.onCategorySelected(act)
                act.fsv.r.reaction = Reaction.POSITION
                // showTouchIcon()
            }
            override fun onCategoryUnselected(act: MainActivity) {

            }
        };


        open fun onOpenMenu(act: MainActivity) {
            onDetermineMenuHeightOpen(act)
        }
        open fun onCloseMenu(act: MainActivity) {
            onDetermineMenuHeightClosed(act)
        }
        abstract fun onMenuClosed(act: MainActivity)
        open fun onCategorySelected(act: MainActivity) {
            act.apply { if (uiLayoutHeight.closed) onDetermineMenuHeightClosed(act) else onDetermineMenuHeightOpen(act) }
        }
        abstract fun onCategoryUnselected(act: MainActivity)
        open fun onDetermineMenuHeightOpen(act: MainActivity) {
            act.uiSetHeight(UiLayoutHeight.SHORT)
        }
        open fun onDetermineMenuHeightClosed(act: MainActivity) {
            act.uiSetHeight(UiLayoutHeight.CLOSED_SHORT)
        }

    }



    override fun onCreate(savedInstanceState: Bundle?) {

        val onCreateStartTime = now()
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        updateSystemBarsVisibility()

        val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        val usResources = getLocalizedResources(applicationContext, Locale.US)

        //setTheme(R.style.AppTheme)
        crashlytics().setCustomKey(CRASH_KEY_ACT_MAIN_CREATED, false)
        crashlytics().setCustomKeys {

            key( CRASH_KEY_ACT_MAIN_CREATED,   false )
            key( CRASH_KEY_FRAG_POS_CREATED,   false )
            key( CRASH_KEY_FRAG_COLOR_CREATED, false )
            key( CRASH_KEY_FRAG_SHAPE_CREATED, false )
            key( CRASH_KEY_FRAG_TEX_CREATED,   false )
            key( CRASH_KEY_GOLD_ENABLED,       false )

            key( CRASH_KEY_MAX_ITER, f.shape.maxIter )

        }
        crashlytics().updateLastAction(Action.INIT)

        db = AppDatabase.getInstance(applicationContext)

        // load custom palettes, shapes, and bookmarks
        lifecycleScope.launch {

            val t = now()

            // this is great!! thank you aha!!
            // get next autogenerate id values
            val query = "SELECT * FROM SQLITE_SEQUENCE"
            val cursor: Cursor = db.query(query, null)
            if (cursor.moveToFirst()) {
                do {
                    val tableName = cursor.getStringOrNull(cursor.getColumnIndex("name"))
                    val nextId = cursor.getStringOrNull(cursor.getColumnIndex("seq"))?.toInt() ?: -1
                    when (tableName) {
                        PALETTE_TABLE_NAME -> Palette.nextCustomPaletteNum  = when (nextId) {
                            -1 -> 1
                            else -> nextId + 1
                        }
                        SHAPE_TABLE_NAME   -> Shape.nextCustomShapeNum      = when (nextId) {
                            -1 -> 1
                            else -> nextId + 1
                        }
                        FRACTAL_TABLE_NAME -> Fractal.nextCustomFractalNum  = when (nextId) {
                            -1 -> 1
                            else -> nextId + 1
                        }
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()

            // load default palettes
            Palette.default.forEach {
                it.initialize(resources)
                it.isFavorite = sp.getBoolean(it.generateStarredKey(usResources), false)
            }

            // load default shapes
            Shape.default.forEach {
                it.initialize(resources)
                it.isFavorite = sp.getBoolean(it.generateStarredKey(usResources), false)
            }

            // load default textures
            Texture.all.forEach {
                it.initialize(resources)
                it.isFavorite = sp.getBoolean(it.generateStarredKey(usResources), false)
            }

            // load default bookmarks
            Fractal.defaultList.forEach {
                it.initialize(resources)
                it.isFavorite = sp.getBoolean(it.generateStarredKey(usResources), false)
            }

            fileList().filter { it.startsWith(TEX_IM_PREFIX) }.let { Texture.customImages.addAll(it.reversed()) }
            ListHeader.all.forEach { it.initialize(resources) }
            ShapeKeyListHeader.all.forEach { it.initialize(resources) }

            Log.e("MAIN", "database operations took ${now() - t} ms")

        }




        billingClient = BillingClient.newBuilder(this)
                .setListener(purchaseUpdateListener)
                .enablePendingPurchases()
                .build()
        billingClient.startConnection(billingClientStateListener)




        // establish screen dimensions
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                windowManager?.currentWindowMetrics?.bounds?.let { rect ->
                    screenWidth = rect.width()
                    screenHeight = rect.height()
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val displayMetrics = baseContext.resources.displayMetrics
                display?.getRealMetrics(displayMetrics)
                screenWidth = displayMetrics.widthPixels
                screenHeight = displayMetrics.heightPixels
            }
            else -> {
                val displayMetrics = baseContext.resources.displayMetrics
                windowManager.defaultDisplay.getRealMetrics(displayMetrics)
                screenWidth = displayMetrics.widthPixels
                screenHeight = displayMetrics.heightPixels
            }
        }

        val screenRatio = screenHeight.toDouble()/screenWidth
        Log.d("MAIN ACTIVITY", "screen resolution : ($screenWidth, $screenHeight), ratio : $screenRatio")

        UiLayoutHeight.values().forEach { it.initialize(resources) }

        // set screen resolution
        // create and insert new resolution if different from preloaded resolutions
        if (Resolution.working.none { it.w == screenWidth }) Resolution.addResolution(screenWidth)
        Resolution.SCREEN = Resolution.valueOf(screenWidth) ?: Resolution.R1080
        Resolution.initialize(screenRatio)
        AspectRatio.initialize()


        val r = FractalRenderer(this, baseContext, ActivityHandler(this))


        // restore SettingsConfig from SharedPreferences
        goldEnabledDialogShown = sp.getBoolean(GOLD_ENABLED_DIALOG_SHOWN, false)
        goldPendingDialogShown = sp.getBoolean(GOLD_PENDING_DIALOG_SHOWN, false)
        showEpilepsyDialog = sp.getBoolean(SHOW_EPILEPSY_DIALOG, true)
        showTutorialOption = sp.getBoolean(SHOW_TUTORIAL_OPTION, true)
        previousFractalCreated = sp.getBoolean(PREV_FRACTAL_CREATED, false)
        previousFractalId = sp.getInt(PREV_FRACTAL_ID, -1)
        Log.e("MAIN", "previousFractalCreated: $previousFractalCreated, id: $previousFractalId")

        Texture.CUSTOM_IMAGE_COUNT = sp.getInt(TEX_IMAGE_COUNT, 0)

        // val maxStartupRes = if (sc.goldEnabled) Resolution.SCREEN else Resolution.R1080
        val savedResolution = sp.getInt(RESOLUTION, Resolution.working.indexOf(Resolution.R1080))
        sc.resolution = Resolution.working.getOrNull(savedResolution) ?: Resolution.R720
        crashlytics().setCustomKey(CRASH_KEY_RESOLUTION, sc.resolution.toString())

        sc.targetFramerate      = sp.getInt(TARGET_FRAMERATE, 60)
        sc.continuousPosRender  = sp.getBoolean(CONTINUOUS_RENDER, false)
        sc.renderBackground     = sp.getBoolean(RENDER_BACKGROUND, true)
        sc.restrictParams       = sp.getBoolean(RESTRICT_PARAMS, true)
        sc.hideSystemBars       = sp.getBoolean(HIDE_NAV_BAR, true)
        sc.autofitColorRange    = sp.getBoolean(AUTOFIT_COLOR_RANGE, true)
        sc.useAlternateSplit    = sp.getBoolean(USE_ALTERNATE_SPLIT, false)
        sc.allowSlowDualfloat   = sp.getBoolean(ALLOW_SLOW_DUALFLOAT, false)
        // sc.chunkProfile         = ChunkProfile.values()[sp.getInt(CHUNK_PROFILE, 1)]
        f.accent1               = sp.getInt(ACCENT_COLOR1, Color.WHITE)

//        sc.colorListViewType    = ListLayoutType.values()[sp.getInt(COLOR_LIST_VIEW_TYPE, ListLayoutType.GRID.ordinal)]
//        sc.shapeListViewType    = ListLayoutType.values()[sp.getInt(SHAPE_LIST_VIEW_TYPE, ListLayoutType.GRID.ordinal)]
//        sc.textureListViewType  = ListLayoutType.values()[sp.getInt(TEXTURE_LIST_VIEW_TYPE, ListLayoutType.GRID.ordinal)]
//        sc.bookmarkListViewType = ListLayoutType.values()[sp.getInt(BOOKMARK_LIST_VIEW_TYPE, ListLayoutType.GRID.ordinal)]


        settingsFragment  = SettingsFragment()
        textureFragment   = TextureFragment()
        shapeFragment     = ShapeFragment()
        colorFragment     = ColorFragment()
        positionFragment  = PositionFragment()


        b.highlightWindow.hide()

        fsv = b.fractalSurfaceView
        fsv.initialize(r, this)
        fsv.layoutParams = FrameLayout.LayoutParams(screenWidth, screenHeight, Gravity.CENTER)
        b.fractalLayout.layoutParams = FrameLayout.LayoutParams(screenWidth, screenHeight, Gravity.CENTER)

        // baseLayout.setOnTouchListener { v, event -> fsv.onTouchEvent(event) }

        b.header.setOnTouchListener { v, event -> true }
        b.ui.setOnTouchListener { v, event -> true }

        onAspectRatioChanged()

        deviceHasNotch = calcDeviceHasNotch()
        navBarHeight = calcNavBarHeight()
        statusBarHeight = calcStatusBarHeight()
        // updateSurfaceViewLayout(resources.getDimension(R.dimen.uiLayoutHeightClosed))

        // uiSetHeight(UiLayoutHeight.CLOSED_SHORT)


        b.ui.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {

                b.ui.viewTreeObserver.removeOnGlobalLayoutListener(this)

                updateFractalLayout()

                val layoutList = listOf(
                    b.baseLayout,
                    b.fractalLayout,
                    b.overlay,
                    b.ui,
                    b.categoryPager,
                    b.header
                )
                layoutList.forEach {
                    it.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
                }

            }

        })

        /* HEADER BUTTONS */

        val extrasPopup = PopupWindow(this)
        extrasMenu = ExtrasMenuBinding.inflate(layoutInflater)
        extrasPopup.apply {
            contentView = extrasMenu.root
            isOutsideTouchable = true
        }

        extrasMenu.settingsButton.setOnClickListener {
            extrasPopup.dismiss()
            openSettingsMenu()
        }

        extrasMenu.bookmarksButton.setOnClickListener {
            extrasPopup.dismiss()
            if (b.editModeButtons.currentEditMode() != EditMode.POSITION) {
                b.editModeButtons.getTabAt(EditMode.POSITION).select()
            }
            positionFragment.openBookmarksList()
        }

        extrasMenu.newBookmarkButton.setOnClickListener {

            extrasPopup.dismiss()

            crashlytics().updateLastAction(Action.NEW_BOOKMARK)
            Fractal.tempBookmark1 = f.bookmark(fsv)

            fsv.r.renderProfile = RenderProfile.SAVE_THUMBNAIL
            fsv.requestRender()

        }

        extrasMenu.renderVideoButton.setOnClickListener {

            fsv.saveVideo(30.0, 12.0, 2.0*Math.PI)

        }
        if (!BuildConfig.DEV_VERSION) extrasMenu.imageLayout.removeView(extrasMenu.renderVideoButton)

        extrasMenu.resolutionButton.setOnClickListener {
            extrasPopup.dismiss()
            if (b.editModeButtons.currentEditMode() != EditMode.POSITION) {
                b.editModeButtons.getTabAt(EditMode.POSITION).select()
            }
            positionFragment.showResolutionLayout()
        }

        extrasMenu.aspectRatioButton.setOnClickListener {
            extrasPopup.dismiss()
            if (b.editModeButtons.currentEditMode() != EditMode.POSITION) {
                b.editModeButtons.getTabAt(EditMode.POSITION).select()
            }
            positionFragment.showAspectRatioLayout()
        }

        extrasMenu.upgradeButton.setOnClickListener {
            extrasPopup.dismiss()
            showUpgradeScreen(true)
        }

        extrasMenu.tutorialButton.setOnClickListener {
            extrasPopup.dismiss()
            startTutorial(fromSettings = true)
        }

//        extrasMenu.changelogButton.setOnClickListener {
//            extrasPopup.dismiss()
//            queryChangelog(fromSettings = true)
//        }

        extrasMenu.aboutButton.setOnClickListener {

            extrasPopup.dismiss()

            val about = AboutDialogBinding.inflate(layoutInflater)

            about.aboutText1.text = resources.getString(R.string.about_info_1).format(BuildConfig.VERSION_NAME)

            about.emailLayout.setOnClickListener {

                var contentString = ""
                contentString += "Android Version: ${Build.VERSION.RELEASE}\n"
                contentString += "Device: ${DeviceName.getDeviceName() ?: Build.BRAND} (${Build.MODEL})\n"
                contentString += "Fractal Eye Version: ${BuildConfig.VERSION_NAME}\n\n"
                contentString += "Please describe your problem here and attach images/video of the problem occurring if possible:\n\n"

                val emailIntent = Intent(Intent.ACTION_SENDTO)
                emailIntent.apply {
                    type = "message/rfc822"
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("selfaffinetech@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "Fractal Eye Help")
                    putExtra(Intent.EXTRA_TEXT, contentString)
                    if (emailIntent.resolveActivity(packageManager) != null) startActivity(emailIntent)
                }

            }

            about.instagramLayout.setOnClickListener {

                val uri = Uri.parse("http://instagram.com/_u/fractaleye.app")
                val likeIng = Intent(Intent.ACTION_VIEW, uri)

                likeIng.setPackage("com.instagram.android")

                try {
                    startActivity(likeIng)
                } catch (e: ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://instagram.com/fractaleye.app")))
                }

            }

            AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setTitle(R.string.about)
                .setIcon(R.drawable.info)
                .setView(about.root)
                .setPositiveButton(android.R.string.ok) { dialog, which -> }
                .create()
                .showImmersive(about.root)
        }

        extrasMenu.randomizerButton.setOnClickListener {
            extrasPopup.dismiss()
            if (b.editModeButtons.currentEditMode() != EditMode.POSITION) {
                b.editModeButtons.getTabAt(EditMode.POSITION).select()
            }
            positionFragment.openRandomizer()
        }


        b.extrasMenuButton.setOnClickListener {
            if (extrasPopup.isShowing) extrasPopup.dismiss() else extrasPopup.showAsDropDown(b.extrasMenuButton)
        }

        b.saveImageButton.setOnClickListener {

            crashlytics().updateLastAction(Action.SAVE_IMAGE)
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

        b.menuToggleButton.setOnClickListener {
            b.editModeButtons.currentEditMode().apply { if (uiIsClosed()) onOpenMenu(this@MainActivity) else onCloseMenu(this@MainActivity) }
        }

//        b.debugButton.setOnClickListener {
//            sc.gpuPrecision = GpuPrecision.DUAL
//            fsv.r.renderShaderChanged = true
//            fsv.r.renderToTex = true
//            fsv.requestRender()
//        }
//        if (!BuildConfig.DEV_VERSION) b.debugButton.hide()



        supportFragmentManager.beginTransaction().add(R.id.settingsFragmentContainer, settingsFragment, "SETTINGS").commit()
        b.settingsFragmentContainer.hide()


        // ViewPager2 stuff

//        b.categoryPager.adapter = EditModeAdapter(this)
//        b.categoryPager.offscreenPageLimit = 4
//        TabLayoutMediator(b.editModeButtons, b.categoryPager) { tab, position ->
//
//
//        }.attach()

        val categoryPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        categoryPagerAdapter.addFrag(textureFragment)
        categoryPagerAdapter.addFrag(shapeFragment)
        categoryPagerAdapter.addFrag(colorFragment)
        categoryPagerAdapter.addFrag(positionFragment)
        b.categoryPager.apply {
            adapter = categoryPagerAdapter
            offscreenPageLimit = 4
        }

        b.editModeButtons.setupWithViewPager(b.categoryPager)
        EditMode.values().forEach { createTabView(it) }

        b.editModeButtons.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {

                val category = EditMode.values()[tab.position]
                tab.customView?.alpha = 1f
//                (tab.customView as? TextView)?.setText(category.displayName)
                category.onCategorySelected(this@MainActivity)

            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.customView?.alpha = 0.4f
                val category = EditMode.values()[tab.position]
                category.onCategoryUnselected(this@MainActivity)
//                (tab.customView as? TextView)?.text = ""
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                b.menuToggleButton.performClick()
            }

        })

        b.overlay.bringToFront()
        b.settingsFragmentContainer.bringToFront()
        b.settingsFragmentContainer.hide()
        b.highlightWindow.hide()


        // need both?
        WindowCompat.getInsetsController(window, b.root)?.hide(WindowInsetsCompat.Type.ime())
        // window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        queryTutorialOption()
        crashlytics().setCustomKey(CRASH_KEY_ACT_MAIN_CREATED, true)

        Log.e("MAIN", "onCreate took ${now() - onCreateStartTime} ms")

    }

    override fun onStart() {
        Log.d("MAIN", "onStart")
        super.onStart()
        fsv.onResume()
    }

    override fun onResume() {

        Log.d("MAIN", "onResume")
        super.onResume()
        if (fragmentsCompleted == 4) lifecycleScope.launch { queryPurchases() }
        // updateSystemBarsVisibility()

    }

    override fun onPause() {

        Log.d("MAIN", "activity paused ...")

        // update favorite status of custom items
        lifecycleScope.launch {
            db.colorPaletteDao().apply {
                Palette.custom.forEach { updateIsFavorite(it.id, it.isFavorite) }
            }
            db.shapeDao().apply {
                Shape.custom.forEach { updateIsFavorite(it.id, it.isFavorite) }
            }
            db.fractalDao().apply {
                Fractal.bookmarks.forEach { updateIsFavorite(it.customId, it.isFavorite) }
            }
        }

        bookmarkAsPreviousFractal()

        val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        val edit = sp.edit()
        edit.putBoolean(GOLD_ENABLED_DIALOG_SHOWN, goldEnabledDialogShown)
        edit.putBoolean(GOLD_PENDING_DIALOG_SHOWN, goldPendingDialogShown)
        edit.putBoolean(SHOW_EPILEPSY_DIALOG, showEpilepsyDialog)
        edit.putBoolean(SHOW_TUTORIAL_OPTION, showTutorialOption)
        edit.putInt(RESOLUTION, min(Resolution.working.indexOf(sc.resolution), if (sc.goldEnabled) Resolution.working.indexOf(Resolution.SCREEN) else Resolution.working.indexOf(Resolution.R1080)))
        edit.putInt(TARGET_FRAMERATE, sc.targetFramerate)
        edit.putInt(ASPECT_RATIO, AspectRatio.all.indexOf(sc.aspectRatio))
        //edit.putInt(PRECISION, sc.precision.ordinal)
        //edit.putBoolean(AUTO_PRECISION, sc.autoPrecision)
        edit.putBoolean(CONTINUOUS_RENDER, sc.continuousPosRender)
        edit.putBoolean(RENDER_BACKGROUND, sc.renderBackground)
        edit.putBoolean(RESTRICT_PARAMS, sc.restrictParams)
        edit.putBoolean(FIT_TO_VIEWPORT, sc.fitToViewport)
        edit.putBoolean(HIDE_NAV_BAR, sc.hideSystemBars)
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
        Palette.default.forEach     { edit.putBoolean(it.generateStarredKey(usResources), it.isFavorite)}
        Shape.default.forEach       { edit.putBoolean(it.generateStarredKey(usResources), it.isFavorite)}
        Texture.all.forEach         { edit.putBoolean(it.generateStarredKey(usResources), it.isFavorite)}
        Fractal.defaultList.forEach { edit.putBoolean(it.generateStarredKey(usResources), it.isFavorite)}

        edit.putInt(VERSION_CODE_TAG, BuildConfig.VERSION_CODE)
        edit.putBoolean(USE_ALTERNATE_SPLIT, sc.useAlternateSplit)
        edit.putBoolean(ALLOW_SLOW_DUALFLOAT, sc.allowSlowDualfloat)
        edit.putInt(CHUNK_PROFILE, sc.chunkProfile.ordinal)
        edit.apply()

        super.onPause()
        fsv.onPause()

    }

    override fun onStop() {
        fsv.onPause()
        super.onStop()
    }

    override fun onBackPressed() {
        //super.onBackPressed()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        Log.e("MAIN", "window focus changed")
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || activityInitialized)) {
            updateSystemBarsVisibility()
        }
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

    private fun createTabView(c: EditMode) {
        val tab = LayoutInflater.from(this).inflate(R.layout.category_tab, null) as TextView
        tab.setText(c.displayName)
//        tab.text = ""
        tab.setCompoundDrawablesWithIntrinsicBounds(0, c.icon, 0, 0)
        tab.compoundDrawablePadding = resources.getDimension(R.dimen.recyclerViewDividerSize).toInt()
        tab.alpha = 0.4f
        b.editModeButtons.getTabAt(c).customView = tab
    }

    private fun queryTutorialOption() {

        if (BuildConfig.DEV_VERSION || showTutorialOption) {

            tutw = TutorialWelcomeBinding.inflate(layoutInflater)

            val dialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setView(tutw.root)
                .create()

            dialog.setOnShowListener { Log.e("MAIN", "dialog shown") }
            dialog.setCanceledOnTouchOutside(false)
            // dialog.show()
            dialog.showImmersive(tutw.root)

//            tutw.root.setOnTouchListener { view, motionEvent ->
//                Log.e("MAIN", "tutorial root was touched at (${motionEvent?.x}, ${motionEvent?.y})")
//                false
//            }

            tutw.tutorialWelcomeText.text = resources.getString(R.string.tutorial_welcome).format(resources.getString(R.string.app_name))
            tutw.tutorialSkipButton.setOnClickListener {
                showTutorialOption = false
                b.highlightWindow.hide()
                dialog.dismiss()
                queryEpilepsyDialog()
            }
            tutw.tutorialStartButton.setOnClickListener {
                dialog.dismiss()
                Handler(Looper.getMainLooper()).postDelayed({ startTutorial() }, BUTTON_CLICK_DELAY_MED)
            }

        } else queryEpilepsyDialog()

    }

    private fun queryEpilepsyDialog() {

        if (showEpilepsyDialog) {

            val dialogView = layoutInflater.inflate(R.layout.alert_dialog_custom, null)
            val checkBox = dialogView?.findViewById<CheckBox>(R.id.dontShowCheckBox)
            checkBox?.setOnCheckedChangeListener { buttonView, isChecked ->
                showEpilepsyDialog = !isChecked
            }

            val dialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setView(dialogView)
                .setIcon(R.drawable.warning)
                .setTitle(R.string.epilepsy_title)
                .setMessage(R.string.epilepsy_dscript)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener { queryLoadPreviousFractalDialog() }
                .create()

            dialog.showImmersive(dialogView)

        } else queryLoadPreviousFractalDialog()

    }

    private fun queryLoadPreviousFractalDialog() {
        if (!BuildConfig.DEV_VERSION && previousFractalCreated) {

            val dialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setTitle(R.string.load_previous_fractal)
                .setIcon(R.drawable.edit)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    fsv.r.checkThresholdCross { Fractal.default.load(Fractal.previous, fsv) }
                    updateFragmentLayouts()
                    fsv.r.renderToTex = true
                    fsv.r.renderShaderChanged = true
                    fsv.requestRender()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setOnDismissListener {
                    activityInitialized = true
                    b.editModeButtons.getTabAt(EditMode.POSITION).select()
                    queryChangelog()
                }
                .create()
                .showImmersive(b.baseLayout)

        } else {
            b.editModeButtons.getTabAt(EditMode.POSITION).select()
            activityInitialized = true
            queryChangelog()
        }
    }

    private fun queryChangelog(fromSettings: Boolean = false) {

        if (BuildConfig.DEV_VERSION || fromSettings || getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE).getInt(VERSION_CODE_TAG, 0) != BuildConfig.VERSION_CODE) {

            val builder: ChangelogBuilder = ChangelogBuilder()
                .withTitle(resources.getString(R.string.changelog))
                .withUseBulletList(true)
                .withMinVersionToShow(40)

            builder.buildAndShowDialog(this, false)

        }

    }




    /* USER ITERFACE */

    private fun hideSystemBars() {
        hideSystemBars(window, b.root)
    }

    private fun showSystemBars() {
        showSystemBars(window, b.root)
    }

    fun updateSystemBarsVisibility() {
        if (sc.hideSystemBars) hideSystemBars() else showSystemBars()
    }

    private fun uiIsOpen() : Boolean {
        return !uiIsClosed()
    }

    private fun uiIsClosed() : Boolean {
        return when (uiLayoutHeight) {
            UiLayoutHeight.CLOSED_SHORT, UiLayoutHeight.CLOSED_MED -> true
            else -> false
        }
    }

    fun uiSetHeight(newUiLayoutHeight: UiLayoutHeight? = null) {

        Log.d("MAIN", "setting ui height to : ${newUiLayoutHeight?.name}")

        val heightChanged = newUiLayoutHeight != uiLayoutHeight
        newUiLayoutHeight?.let { uiLayoutHeight = it }

        b.ui.layoutParams.height = uiLayoutHeight.dimen.toInt()
        b.ui.requestLayout()

        if (heightChanged) {

            if (fsv.r.isRendering) fsv.r.pauseRender = true
            b.menuToggleButton.rotation = if (uiIsClosed()) 0f else 180f

            val anim = ValueAnimator.ofFloat(0f, 1f)
            anim.duration = b.ui.layoutTransition.getDuration(LayoutTransition.CHANGING) + 75L
            anim.addUpdateListener {
                updateFractalLayout()
                if (anim.animatedFraction == 1f) {
                    if (uiLayoutHeight.closed) b.editModeButtons.currentEditMode().onMenuClosed(this)
                }
            }
            anim.start()

        }

    }

    fun updateFractalLayout(initial: Boolean = false) {

        if (sc.fitToViewport && b.fractalLayout.layoutParams.height >= b.ui.top - b.header.bottom) {
            val scaleFactor = (b.ui.top - b.header.bottom) / b.fractalLayout.layoutParams.height.toFloat()
            b.fractalLayout.apply {
                scaleX = scaleFactor
                scaleY = scaleFactor
            }
        }
        else {
            b.fractalLayout.apply {
                scaleX = 1f
                scaleY = 1f
            }
        }
        b.fractalLayout.y = (b.ui.top + b.header.bottom - b.fractalLayout.height)/2f
        b.fractalLayout.updateLayoutParams<FrameLayout.LayoutParams> { gravity = Gravity.CENTER }
        b.fractalLayout.requestLayout()

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
        b.fractalLayout.layoutParams = FrameLayout.LayoutParams(newWidth, newHeight, Gravity.CENTER)

        updateFractalLayout()

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

    private fun openSettingsMenu() {
        crashlytics().updateLastAction(Action.SETTINGS)
        b.settingsFragmentContainer.show()
    }

    fun closeSettingsMenu() {
        b.settingsFragmentContainer.hide()
    }

    fun showMessage(msg: String) {
        runOnUiThread {
            val toast = Toast.makeText(baseContext, msg, Toast.LENGTH_LONG)
            val toastHeight = b.ui.height + resources.getDimension(R.dimen.toastMargin).toInt()
            toast.setGravity(Gravity.BOTTOM, 0, toastHeight)
            toast.show()
        }
    }

    fun showMessage(msgId: Int) {
        showMessage(resources.getString(msgId))
    }

    fun hideCategoryButtons() { b.editModeButtons.hide() }

    fun showCategoryButtons() { b.editModeButtons.show() }

    fun toggleCategoryButtons() {
        if (b.editModeButtons.isHidden()) showCategoryButtons()
        else hideCategoryButtons()
    }

    fun showMenuToggleButton() {
        b.menuToggleButton.show()
    }

    fun hideMenuToggleButton() {
        b.menuToggleButton.hide()
    }




    /* FRAGMENT COMMUNICATION */

    override fun onComplete() {
        fragmentsCompleted++
        Log.d("MAIN", "!! fragment callback !! $fragmentsCompleted fragments completed")
        if (fragmentsCompleted == 4) {
            if (!billingClient.isReady) billingClient.startConnection(billingClientStateListener)
            else lifecycleScope.launch { queryPurchases() }
        }
    }

    fun hideHeaderButtons() {
        b.header.children.forEach { it.hide() }
    }

    fun showHeaderButtons() {
        b.header.children.forEach { it.show() }
    }

    fun updateCrashKeys() {
        crashlytics().setCustomKeys {
            key(
                CRASH_KEY_SHAPE_NAME,
                f.shape.getName(getLocalizedResources(this@MainActivity, Locale.US))
            )
            key(
                CRASH_KEY_PALETTE_NAME,
                f.palette.getName(getLocalizedResources(this@MainActivity, Locale.US))
            )
            key(
                CRASH_KEY_TEXTURE_NAME,
                f.texture.getName(getLocalizedResources(this@MainActivity, Locale.US))
            )
        }
    }

    fun updateShapeEditTexts() {
        shapeFragment.loadActiveParam()
    }

    fun updateTextureParam() {
        textureFragment.loadActiveParam()
    }

    fun updateColorValues() {
        colorFragment.apply {
            updateFrequencyLayout()
            updatePhaseLayout()
            updateDensityLayout()
        }
    }

    fun updatePositionLayout() {
        runOnUiThread { positionFragment.updateLayout() }
    }

    fun updateFragmentLayouts() {
        listOf(textureFragment, shapeFragment, colorFragment, positionFragment).forEach { it.updateLayout() }
    }

    fun updateRadius() {
        textureFragment.loadRadius(updateProgress = true)
    }

    fun onTextureChanged() {
        colorFragment.b.accentColor2Button.apply { if (f.texture.usesAccent) show() else hide() }
    }

    fun showThumbnailRenderDialog() {

        dialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
            .setTitle(R.string.rendering_icons)
            .setIcon(R.drawable.hourglass)
            .setView(R.layout.alert_dialog_progress)
            .setNegativeButton(android.R.string.cancel) { dialog, which ->
                fsv.r.interruptRender = true
            }
            .create()
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.showImmersive(b.root)

    }

    private fun updateTumbnailRenderDialog(index: Int, total: Int) {
        dialog?.findViewById<ProgressBar>(R.id.alertProgress)?.apply {
            max = total - 1
            progress = index
        }
        dialog?.findViewById<TextView>(R.id.alertProgressText)?.text = "${index + 1}/$total"
    }

    private fun dismissThumbnailRenderDialog() {
        dialog?.dismiss()
    }

    fun showBookmarkDialog(item: ListItem<Fractal>? = null, edit: Boolean = false) {

        val dialog = AlertDialogNewPresetBinding.inflate(layoutInflater)

        if (edit) dialog.name.setText(Fractal.tempBookmark1.name)
        else {
            dialog.name.setText("%s %s %d".format(
                    resources.getString(R.string.header_custom),
                    resources.getString(R.string.fractal),
                    Fractal.nextCustomFractalNum
            ))
        }

        dialog.thumbnail.setImageBitmap(Fractal.tempBookmark1.thumbnail)

        val d = AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setIcon(R.drawable.bookmark2)
                .setTitle("${resources.getString(if (edit) R.string.edit else R.string.save)} ${resources.getString(R.string.bookmark)}")
                .setView(dialog.root)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel) { dialog, which ->
                    if (!edit) File(filesDir.path + Fractal.tempBookmark1.thumbnailPath).delete()
                }
                .create()

        d.setCanceledOnTouchOutside(false)
        d.setOnShowListener {
            val b: Button = d.getButton(AlertDialog.BUTTON_POSITIVE)
            b.setOnClickListener {

                if (Fractal.all.find { it.name == dialog.name.text.toString() } != null) {
                    showMessage(resources.getString(R.string.msg_custom_name_duplicate).format(
                            resources.getString(R.string.bookmark)
                    ))
                }
                else if (dialog.name.text.toString() == "") {
                    showMessage(R.string.msg_empty_name)
                }
                else {
                    if (edit) {
                        lifecycleScope.launch {
                            Fractal.tempBookmark1.name = dialog.name.text.toString()
                            db.fractalDao().apply {
                                updateName(Fractal.tempBookmark1.customId, Fractal.tempBookmark1.name)
                            }
                        }
                        positionFragment.getBookmarkListAdapter()?.apply {
                            updateItem(item!!)
                            clearSelection()
                        }
                    } else {
                        lifecycleScope.launch {
                            Fractal.tempBookmark1.name = dialog.name.text.toString()
                            Log.d("MAIN", "new bookmark thumbnail path: ${Fractal.tempBookmark1.thumbnailPath}")
                            db.fractalDao().apply {
                                Fractal.tempBookmark1.customId = insert(Fractal.tempBookmark1.toEntity()).toInt()
                            }
                            Fractal.tempBookmark1.goldFeature = false
                            Fractal.bookmarks.add(0, Fractal.tempBookmark1)
                            Fractal.all.add(0, Fractal.tempBookmark1)
                            Fractal.nextCustomFractalNum++
                        }
                        positionFragment.getBookmarkListAdapter()?.addItemToCustom(
                            ListItem(Fractal.tempBookmark1, ListHeader.CUSTOM, R.layout.other_list_item), 0
                        )
                    }
                    d.dismiss()
                    if (!edit) showMessage(resources.getString(R.string.msg_bookmark_created))
                }

            }
            d.show()
        }
        d.showImmersive(dialog.root)

    }

    fun bookmarkAsPreviousFractal() {
        lifecycleScope.launch {
            db.fractalDao().apply {
                Fractal.previous = Fractal.default.bookmark(fsv)
                Fractal.previous.customId = previousFractalId
                if (previousFractalId != -1) update(Fractal.previous.toEntity())
                else {
                    showMessage(resources.getString(R.string.msg_error))
                }
            }
        }
    }

    fun updateColorThumbnails() {

        colorFragment.getPaletteListAdapter()?.notifyDataSetChanged()

    }

    fun updateTextureThumbnail(layoutIndex: Int, n: Int) {

        // Log.e("MAIN ACTIVITY", "updateTextureThumbnail was called !!!")
        textureFragment.b.textureListLayout.list.adapter?.notifyItemChanged(layoutIndex)
        Log.d("MAIN", "n: $n")
        updateTumbnailRenderDialog(n, f.shape.compatTextures.size)
        if (n + 1 == f.shape.compatTextures.size) dismissThumbnailRenderDialog()

    }

    fun updateShapeThumbnail(shape: Shape, customIndex: Int) {

        shapeFragment.getShapeListAdapter()?.updateItems(shape)
        if (customIndex != -1) {
            Log.d("MAIN", "customIndex: $customIndex")
            val numShapes = Shape.custom.size
            updateTumbnailRenderDialog(customIndex, numShapes)
            if (customIndex + 1 == numShapes) dismissThumbnailRenderDialog()
        }

    }







    /* TUTORIAL */

    private fun showNextButton() {
        tut.tutText1.hide()
        tut.tutText2.show()
        tut.tutText2.setText(R.string.tutorial_great)
        tut.tutProgress.hide()
        tut.tutNextButton.show()
    }

    fun highlightView(v: View) {
        b.highlightWindow.highlightView(v)
        val newConstraints = ConstraintSet()
        newConstraints.clone(tut.tutorialLayout)
        newConstraints.connect(tut.tutorialSubLayout.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        newConstraints.applyTo(tut.tutorialLayout)
        tut.tutorialSubLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = FrameLayout.LayoutParams.WRAP_CONTENT
        }
    }

    private fun highlightCategory(cat: EditMode) {
        b.highlightWindow.highlightView(b.editModeButtons.getTabAt(cat).view)
        val newConstraints = ConstraintSet()
        newConstraints.clone(tut.tutorialLayout)
        newConstraints.connect(tut.tutorialSubLayout.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        newConstraints.applyTo(tut.tutorialLayout)
        tut.tutorialSubLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = FrameLayout.LayoutParams.WRAP_CONTENT
        }
    }

    private fun highlightFractalWindow() {
        b.highlightWindow.highlightRect(Rect(fsv.left, b.header.bottom, fsv.right, b.ui.top))
        val newConstraints = ConstraintSet()
        newConstraints.clone(tut.tutorialLayout)
        newConstraints.clear(tut.tutorialSubLayout.id, ConstraintSet.TOP)
        newConstraints.applyTo(tut.tutorialLayout)
        tut.tutorialSubLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = resources.getDimension(R.dimen.uiLayoutHeightShort).toInt()
        }
    }

    private fun highlightUiComponent() {
        b.highlightWindow.highlightRect(Rect(fsv.left, b.ui.top, fsv.right, b.ui.top + resources.getDimension(R.dimen.uiComponentHeightShort).toInt()))
        val newConstraints = ConstraintSet()
        newConstraints.clone(tut.tutorialLayout)
        newConstraints.connect(tut.tutorialSubLayout.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        newConstraints.applyTo(tut.tutorialLayout)
        tut.tutorialSubLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = FrameLayout.LayoutParams.WRAP_CONTENT
        }
    }

    private fun highlightFractalWindowAndUiComponent() {
        b.highlightWindow.highlightRect(Rect(fsv.left, b.header.bottom, fsv.right, b.ui.top + resources.getDimension(R.dimen.uiComponentHeightShort).toInt()))
        val newConstraints = ConstraintSet()
        newConstraints.clone(tut.tutorialLayout)
        newConstraints.clear(tut.tutorialSubLayout.id, ConstraintSet.TOP)
        newConstraints.applyTo(tut.tutorialLayout)
        tut.tutorialSubLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = resources.getDimension(R.dimen.uiLayoutHeightShort).toInt() - resources.getDimension(R.dimen.uiComponentHeightShort).toInt()
        }
    }

    private fun startTutorial(fromSettings: Boolean = false) {

        tutorialFromSettings = fromSettings

        b.highlightWindow.show()
        b.highlightWindow.bringToFront()
        b.highlightWindow.consumeTouch = true

        if (sc.autofitColorRange) colorFragment.b.colorAutofitButton.performClick()
        Texture.stripeAvg.reset()
        fsv.r.checkThresholdCross { f.load(Fractal.tutorial1, fsv) }
        fsv.doingTutorial = true
        fsv.r.renderShaderChanged = true
        fsv.r.renderToTex = true
        fsv.requestRender()

        b.editModeButtons.getTabAt(EditMode.TEXTURE).select()
        uiSetHeight(UiLayoutHeight.SHORT)
        if (!tutorialLayoutInflated) {
            tut = TutorialBinding.inflate(layoutInflater, b.baseLayout, true)
            tutorialLayoutInflated = true
            // layoutInflater.inflate(R.layout.tutorial, baseLayout, true)
        } else {
            tut.tutorialLayout.show()
            tut.tutorialLayout.bringToFront()
        }
        tut.tutFinishButton.hide()
        tut.gestureAnimation.stopAnim()
        startCategoryDetails()

    }

    private fun startCategoryDetails() {
        b.menuToggleButton.hide()
        highlightView(b.editModeButtons)
        b.highlightWindow.startHighlightAnimation()
        tut.tutText1.showAndSetText(R.string.tutorial_1_1)
        tut.tutText2.hide()
        tut.tutProgress.hide()
        fsv.doingTutorial = true
        tut.tutNextButton.show()
        tut.tutNextButton.setOnClickListener {
            tut.tutNextButton.hide()
            startPositionCategoryClick()
        }
        tut.tutExitButton.show()
        tut.tutExitButton.setOnClickListener { endTutorial() }
    }

    private fun startPositionCategoryClick() {
        tut.tutText1.showAndSetText(R.string.tutorial_2_00)
        tut.tutText2.showAndSetText(R.string.tutorial_2_1)
        if (b.editModeButtons.currentEditMode() == EditMode.POSITION) b.editModeButtons.getTabAt(EditMode.COLOR).select()
        b.highlightWindow.apply {
            consumeTouch = false
            highlightCategory(EditMode.POSITION)
            isRequirementSatisfied = { true }
            onRequirementSatisfied = { startPositionPanInteract() }
        }
        f.shape.position.reset()
        // tutBackButton.show()
        // tutExitButton.setOnClickListener { startCategoryDetails() }
    }

    private fun startPositionPanInteract() {

        b.editModeButtons.getTabAt(EditMode.POSITION).select()
        tut.tutProgress.show()
        tut.tutText1.showAndSetText(R.string.tutorial_2_2)
        tut.tutText2.showAndSetText(R.string.tutorial_2_3)
        highlightFractalWindow()
        tut.gestureAnimation.startSwipeVerticalAnim()

        val highlightRatio = b.highlightWindow.highlight.run { height().toDouble()/width() }
        val zoomInit = 6.5
        val yInit = -0.25*zoomInit*highlightRatio
        val yReq = 0.25*zoomInit*highlightRatio
        f.shape.position.apply {
            reset()
            zoom = zoomInit
            zoomLocked = true
            rotationLocked = true
            y = yInit
        }
        fsv.r.renderToTex = true
        fsv.requestRender()
        fsv.isRequirementSatisfied = { f.shape.position.y > yReq }
        b.highlightWindow.isRequirementSatisfied = { false }
        fsv.onRequirementSatisfied = {
            tut.gestureAnimation.stopAnim()
            showNextButton()
        }
        fsv.updateTutorialProgress = {
            val d = abs(f.shape.position.y - yReq)
            tut.tutProgress.progress = (100.0*(1.0 - min(d, abs(yInit - yReq))/(abs(yInit - yReq)))).toInt()
        }
        tut.tutNextButton.setOnClickListener {
            tut.tutNextButton.hide()
            startPositionZoomInteract()
        }
//        tutExitButton?.setOnClickListener {
//            startPositionCategoryClick()
//        }
    }

    private fun startPositionZoomInteract() {
        tut.tutText1.showAndSetText(R.string.tutorial_pos_zoom_interact_1)
        tut.tutText2.showAndSetText(R.string.tutorial_pos_zoom_interact_2)
        tut.tutProgress.show()
        tut.tutProgress.progress = 0
        val zoomInit = 3.5
        val zoomReq = 6e-3
        f.shape.position.apply {
            reset()
            x = -0.77754949627
            y = -0.13556904821
            zoom = 3.5
            rotation = (-101.7).inRadians()
            xLocked = true
            yLocked = true
            rotationLocked = true
        }
        fsv.r.renderToTex = true
        fsv.requestRender()
        highlightFractalWindow()
        tut.gestureAnimation.startPinchAnim()
        fsv.isRequirementSatisfied = { f.shape.position.zoom < zoomReq }
        fsv.updateTutorialProgress = {
            val prevProgress = tut.tutProgress.progress
            tut.tutProgress.progress = (100.0*log(f.shape.position.zoom / zoomInit, zoomReq / zoomInit)).toInt()
            if (tut.tutProgress.progress > 50 && prevProgress <= 50) {
                tut.tutText1.hide()
                tut.tutText2.showAndSetText(R.string.tutorial_keep_going)
            }
            else if (tut.tutProgress.progress <= 50 && prevProgress > 50) {
                tut.tutText1.showAndSetText(R.string.tutorial_pos_zoom_interact_1)
                tut.tutText2.showAndSetText(R.string.tutorial_pos_zoom_interact_2)
            }
        }
        tut.tutNextButton.setOnClickListener {
            tut.tutNextButton.hide()
            f.shape.position.zoom = 6e-3
            fsv.r.renderToTex = true
            fsv.requestRender()
            startColorCategoryClick()
        }
        // tutExitButton?.setOnClickListener { startPositionPanInteract() }
    }

    private fun startColorCategoryClick() {
        tut.tutText1.showAndSetText(R.string.tutorial_3_00)
        tut.tutText2.showAndSetText(R.string.tutorial_3_1)
        tut.tutProgress.hide()
        highlightCategory(EditMode.COLOR)
        b.highlightWindow.isRequirementSatisfied = { true }
        b.highlightWindow.onRequirementSatisfied = { startPhaseInteract() }
        tut.gestureAnimation.stopAnim()
        // tutExitButton?.setOnClickListener { startPositionZoomInteract() }
    }

    private fun startPhaseInteract() {
        b.highlightWindow.isRequirementSatisfied = { false }
        b.editModeButtons.getTabAt(EditMode.COLOR).select()
        tut.tutText1.showAndSetText(R.string.tutorial_3_2)
        tut.tutText2.showAndSetText(R.string.tutorial_3_3)
        tut.tutProgress.hide()
        highlightFractalWindow()
        tut.gestureAnimation.startSwipeHorizontalAnim()
        val phaseInit = f.phase
        fsv.requestRender()
        fsv.isRequirementSatisfied = { abs(f.phase - phaseInit) > 0.5 }
        tut.tutNextButton.setOnClickListener {
            tut.tutNextButton.hide()
            startFreqInteract()
        }
        // tutExitButton.setOnClickListener { startColorCategoryClick() }
    }

    private fun startFreqInteract() {
        tut.tutText1.show()
        tut.tutText1.setText(R.string.tutorial_3_4)
        tut.tutText2.show()
        tut.tutText2.setText(R.string.tutorial_3_5)
        tut.tutProgress.show()
        tut.tutProgress.progress = 0
        // highlightFractalWindow()
        highlightFractalWindowAndUiComponent()
        tut.gestureAnimation.startPinchAnim()
        val freqInit = f.frequency
        val freqReq = 1.529f
        fsv.requestRender()
        fsv.isRequirementSatisfied = { f.frequency > freqReq }
        fsv.updateTutorialProgress = {
            tut.tutProgress.progress = (100.0*(f.frequency/freqInit - 1f)/(freqReq/freqInit - 1f)).toInt()
        }
        tut.tutNextButton.setOnClickListener {
            tut.tutNextButton.hide()
            startShapeCategoryClick()
        }
        // tutExitButton.setOnClickListener { startPhaseInteract() }
    }

    private fun startShapeCategoryClick() {
        tut.tutText1.show()
        tut.tutText1.setText(R.string.tutorial_4_00)
        tut.tutText2.show()
        tut.tutText2.setText(R.string.tutorial_4_1)
        tut.tutProgress.hide()
        highlightCategory(EditMode.SHAPE)
        b.highlightWindow.isRequirementSatisfied = { true }
        b.highlightWindow.onRequirementSatisfied = { startShapeParamClick() }
        tut.gestureAnimation.stopAnim()
        // tutExitButton.setOnClickListener { startFreqInteract() }
    }

    private fun startShapeParamClick() {
        b.editModeButtons.getTabAt(EditMode.SHAPE).select()
        tut.tutText1.showAndSetText(R.string.tutorial_4_2)
        tut.tutText2.hide()
        b.highlightWindow.isRequirementSatisfied = { false }
        f.phase = 0f
        f.frequency = 1f
        f.shape.positions.julia.apply {
            reset()
            zoom = 2.5
            rotation = 135.0.inRadians()
        }
        f.shape.params.julia.reset()
        f.shape.juliaMode = true
        shapeFragment.b.apply {
            juliaModeButton.isChecked = true
            juliaParamButton.show()
            maxIterButton.performClick()
        }
        fsv.r.renderToTex = true
        fsv.r.renderShaderChanged = true
        fsv.requestRender()
        shapeFragment.b.juliaParamButton.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                shapeFragment.b.juliaParamButton.viewTreeObserver.removeOnGlobalLayoutListener(this)
                highlightView(shapeFragment.b.juliaParamButton)
                b.highlightWindow.isRequirementSatisfied = { true }
                b.highlightWindow.onRequirementSatisfied = { startShapeParamDetails() }
            }
        })
        fsv.isRequirementSatisfied = { false }
        // tutExitButton?.setOnClickListener { startShapeCategoryClick() }
    }

    fun startShapeParamDetails() {
        shapeFragment.b.juliaParamButton.performClick()
        tut.tutText1.showAndSetText(R.string.tutorial_shape_param_info)
        tut.tutText2.showAndSetText(R.string.tutorial_shape_param_next)
        highlightUiComponent()
        b.highlightWindow.consumeTouch = true
        tut.tutNextButton.show()
        tut.tutNextButton.setOnClickListener {
            tut.tutNextButton.hide()
            startShapeParamInteract()
        }
        // tutExitButton.setOnClickListener { startShapeParamClick() }
    }

    private fun startShapeParamInteract() {
        highlightFractalWindowAndUiComponent()
        tut.gestureAnimation.startSwipeDiagonalAnim()
        tut.tutText1.showAndSetText(R.string.tutorial_shape_param_interact)
        tut.tutText2.hide()
        b.highlightWindow.consumeTouch = false
        b.highlightWindow.isRequirementSatisfied = { false }
        fsv.isRequirementSatisfied = { f.shape.params.julia.u.pow(2) + f.shape.params.julia.v.pow(2) > 0.5 }
        tut.tutNextButton.setOnClickListener {
            tut.tutNextButton.hide()
            startIterationClick()
        }
        // tutExitButton.setOnClickListener { startShapeParamDetails() }
    }

    private fun startIterationClick() {
        tut.tutText1.show()
        tut.tutText1.setText(R.string.tutorial_4_6)
        tut.tutText2.show()
        tut.tutText2.setText(R.string.tutorial_4_7)
        f.shape.params.julia.apply {
            u = -0.75
            v = 0.15
        }
        f.shape.maxIter = 30
        shapeFragment?.updateLayout()
        fsv.r.renderToTex = true
        fsv.requestRender()
        highlightView(shapeFragment.b.maxIterButton)
        b.highlightWindow.apply {
            isRequirementSatisfied = { true }
            onRequirementSatisfied = { startIterationInteract() }
        }
        // tutExitButton.setOnClickListener { startShapeParamInteract() }
    }

    private fun startIterationInteract() {
        shapeFragment.b.maxIterButton.performClick()
        tut.tutText1.show()
        tut.tutText1.setText(R.string.tutorial_4_8)
        tut.tutText2.hide()
        fsv.isRequirementSatisfied = { false }
        highlightFractalWindowAndUiComponent()
        shapeFragment.onTutorialReqMet = { showNextButton() }
        b.highlightWindow.isRequirementSatisfied = { false }
        tut.tutNextButton.setOnClickListener {
            tut.tutNextButton.hide()
            startTextureCategoryClick()
        }
        // tutExitButton.setOnClickListener { startIterationClick() }
    }

    private fun startTextureCategoryClick() {
        tut.tutText1.showAndSetText(R.string.tutorial_5_00)
        tut.tutText2.showAndSetText(R.string.tutorial_5_1)
        highlightCategory(EditMode.TEXTURE)
        b.highlightWindow.isRequirementSatisfied = { true }
        b.highlightWindow.onRequirementSatisfied = { startTextureParamClick() }
        f.texture = Texture.stripeAvg
        f.texture.params.apply {
            list[1].u = 90.0
            list[2].u = 30.0
        }
        f.shape.maxIter = 512
        f.frequency = 1.179f
        f.phase = 0.517f
        fsv.r.renderShaderChanged = true
        fsv.r.renderToTex = true
        fsv.requestRender()
        textureFragment.updateLayout()
        // tutExitButton.setOnClickListener { startIterationInteract() }
    }

    private fun startTextureParamClick() {
        b.editModeButtons.getTabAt(EditMode.TEXTURE).select()
        tut.tutText1.showAndSetText(R.string.tutorial_5_2)
        tut.tutText2.hide()
        highlightView(textureFragment.b.textureParamButton2)
        b.highlightWindow.onRequirementSatisfied = { startTextureParamDetails() }
        // tutExitButton.setOnClickListener { startTextureCategoryClick() }
    }

    private fun startTextureParamDetails() {
        textureFragment.b.textureParamButton2.performClick()
        tut.tutText1.showAndSetText(R.string.tutorial_texure_param_info)
        tut.tutText2.showAndSetText(R.string.tutorial_texture_param_next)
        highlightUiComponent()
        b.highlightWindow.isRequirementSatisfied = { false }
        tut.tutNextButton.show()
        tut.tutNextButton.setOnClickListener {
            tut.tutNextButton.hide()
            startTextureParamInteract()
        }
        // tutExitButton.setOnClickListener { startTextureParamClick() }
    }

    fun startTextureParamInteract() {
        tut.tutText1.showAndSetText(R.string.tutorial_texture_param_interact)
        tut.tutText2.hide()
        highlightFractalWindowAndUiComponent()
        b.highlightWindow.isRequirementSatisfied = { false }
        tut.gestureAnimation.startSwipeHorizontalAnim()
        val uTarget = 230.0
        fsv.isRequirementSatisfied = { abs(f.texture.activeParam.u - uTarget) < 1.0 }
        tut.tutNextButton.setOnClickListener {
            tut.tutNextButton.hide()
            startTutorialCongrats()
        }
        // tutExitButton.setOnClickListener { startTextureParamDetails() }
    }

    private fun startTutorialCongrats() {
        highlightCategory(EditMode.POSITION)  // just for layout change
        b.highlightWindow.clearHighlight()
        tut.tutText1.showAndSetText(R.string.tutorial_congrats)
        tut.tutText2.showAndSetText(R.string.tutorial_complete)
        tut.tutFinishButton.show()
        tut.tutFinishButton.setOnClickListener { endTutorial() }
    }

    private fun endTutorial() {
        showTutorialOption = false
        tut.tutorialLayout.hide()
        b.highlightWindow.stopHighlightAnimation()
        b.highlightWindow.hide()
        f.apply {
            shape.reset()
            texture = Texture.escapeSmooth
            shape.position.reset()
            frequency = 1f
            phase = 0.65f
        }
        Texture.stripeAvg.reset()
        b.editModeButtons.getTabAt(EditMode.POSITION).select()
        fsv.doingTutorial = false
        fsv.r.renderShaderChanged = true
        fsv.r.renderToTex = true
        fsv.requestRender()
        updateFragmentLayouts()
        if (!tutorialFromSettings) queryEpilepsyDialog()
    }






    /* GOOGLE PLAY BILLING / UPGRADE */

    fun showUpgradeScreen(fromButtonPress: Boolean = false) {

        crashlytics().updateLastAction(Action.UPGRADE)
        if (fromButtonPress) {
            val myIntent = Intent(this, UpgradeActivity::class.java)
            startActivity(myIntent)
        } else {
            // double check to avoid showing upgrade screen if not necessary
            if (!billingClient.isReady) billingClient.startConnection(billingClientStateListener)
            else lifecycleScope.launch { queryPurchases() }

            if (!sc.goldEnabled) {
                val myIntent = Intent(this, UpgradeActivity::class.java)
                startActivity(myIntent)
            }
        }

    }

    suspend fun consumePurchase() {
        val purchaseQueryResult = billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP)
        purchaseQueryResult.purchasesList.getOrNull(0)?.apply {
            Log.d("MAIN", "processing purchase...")
            Log.d("MAIN", originalJson)
            if (purchaseState == Purchase.PurchaseState.PURCHASED) {
                lifecycleScope.launch(Dispatchers.IO) {
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

    suspend fun queryPurchases() {

        val purchaseQueryResult = billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP)
        purchaseQueryResult.purchasesList.getOrNull(0)?.apply {
            Log.d("MAIN", "processing purchase...")
            Log.d("MAIN", originalJson)
            when (purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (fragmentsCompleted == 4) {
                        sc.goldEnabled = true
                        onGoldEnabled()
                    }
                    if (!isAcknowledged) {
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchaseToken)
                        lifecycleScope.launch(Dispatchers.IO) {
                            billingClient.acknowledgePurchase(acknowledgePurchaseParams.build())
                        }
                    }
                    if (!goldEnabledDialogShown) {
                        AlertDialog.Builder(this@MainActivity, R.style.AlertDialogCustom)
                            .setIcon(R.drawable.wow)
                            .setTitle(R.string.gold_enabled)
                            .setMessage(R.string.gold_enabled_dscript)
                            .setPositiveButton(android.R.string.ok, null)
                            .create()
                            .showImmersive(b.root)
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
                            .create()
                            .showImmersive(b.root)
                        goldPendingDialogShown = true
                    }
                }
            }
        }
        if (purchaseQueryResult.purchasesList.isEmpty()) {
            goldEnabledDialogShown = false
            goldPendingDialogShown = false
        }
        if (BuildConfig.DEV_VERSION && fragmentsCompleted == 4) {
            sc.goldEnabled = true
            onGoldEnabled()
        }

    }

    private fun onGoldEnabled() {
        crashlytics().setCustomKey(CRASH_KEY_GOLD_ENABLED, true)
        val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        val edit = sp.edit()
        edit.putBoolean(GOLD_ENABLED_DIALOG_SHOWN, true)
        edit.apply()
        extrasMenu.logisticsLayout.removeView(extrasMenu.upgradeButton)
        listOf(textureFragment, shapeFragment, colorFragment, positionFragment).forEach { runOnUiThread { it.onGoldEnabled() } }
    }






    /* UTILITY */

    private fun getLocalizedResources(context: Context, desiredLocale: Locale?): Resources {
        var conf: Configuration = context.resources.configuration
        conf = Configuration(conf)
        conf.setLocale(desiredLocale)
        val localizedContext = context.createConfigurationContext(conf)
        return localizedContext.resources
    }

    fun getAvailableHeapMemory() : Long {
        val runtime = Runtime.getRuntime()
        val usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
        val maxHeapSizeInMB = runtime.maxMemory() / 1048576L
        return maxHeapSizeInMB - usedMemInMB
    }

    fun onRateButtonClicked(): Boolean {
        Toast.makeText(this, "Rate button was clicked", Toast.LENGTH_LONG).show()
        // button click handled
        return true
    }


}
