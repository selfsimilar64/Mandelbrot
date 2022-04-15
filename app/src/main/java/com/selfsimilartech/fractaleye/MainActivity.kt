package com.selfsimilartech.fractaleye

import android.Manifest
import android.animation.LayoutTransition
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.database.Cursor
import android.graphics.*
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.database.getStringOrNull
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.window.layout.WindowMetricsCalculator
import com.android.billingclient.api.*
import com.google.firebase.crashlytics.ktx.setCustomKeys
import com.jaredrummler.android.device.DeviceName
import com.michaelflisar.changelog.ChangelogBuilder
import com.selfsimilartech.fractaleye.databinding.*
import com.woxthebox.draglistview.DragListView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.*

const val DEBUG_GOLD_ENABLED = true

const val MAX_SHAPE_PARAMS = 4
const val MAX_TEXTURE_PARAMS = 4
const val WRITE_STORAGE_REQUEST_CODE = 0
const val ITER_MAX_POW = 13.0
const val ITER_MIN_POW = 5.0
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


const val SHARED_PREFERENCES = "com.selfsimilartech.fractaleye.SETTINGS"

const val PALETTE_TABLE_NAME = "palette"
const val SHAPE_TABLE_NAME = "shape"
const val FRACTAL_TABLE_NAME = "fractal"
const val TEX_IM_PREFIX = "tex_im_"

const val SETTINGS_FRAGMENT_TAG = "SETTINGS"
const val VIDEO_FRAGMENT_TAG = "VIDEO"

const val AP_DIGITS = 64L



class MainActivity : AppCompatActivity(), SettingsFragment.OnSettingsChangedListener {

    lateinit var b : ActivityMainNewBinding
    lateinit var tutw : TutorialWelcomeBinding
    lateinit var db : AppDatabase
    lateinit var fsv : FractalSurfaceView

    private var listAdaptersInitialized = false
    val f = Fractal.default
    var sc = Settings

    private var uiState = UiState.HOME

    val handler = Handler(Looper.getMainLooper())

    private var activityInitialized = false
    private var screenWidth = 0
    private var screenHeight = 0
    private var goldEnabledDialogShown = false
    private var goldPendingDialogShown = false
    private var showEpilepsyDialog = true
    private var showTutorialOption = true
    private var dialog : AlertDialog? = null

    var previousFractalCreated = false
    var previousFractalId = -1

    var customScreenRes = false

    val editListener = { nextEditText: EditText?, setValueAndFormat: (w: EditText) -> Unit
        -> TextView.OnEditorActionListener { editText, actionId, _ ->

        when (actionId) {
            EditorInfo.IME_ACTION_NEXT -> {
                setValueAndFormat(editText as EditText)
                editText.clearFocus()
                editText.isSelected = false
                nextEditText?.requestFocus()
            }
            EditorInfo.IME_ACTION_DONE -> {
                setValueAndFormat(editText as EditText)
                val imm = baseContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(b.root.windowToken, 0)
                editText.clearFocus()
                editText.isSelected = false
            }
            else -> {}
        }

        editText.clearFocus()
        updateSystemBarsVisibility()
        true

    }}

    private val nonClickableViewTypes = listOf(
            R.layout.list_header,
            R.layout.list_item_linear_empty_favorite,
            R.layout.list_item_linear_empty_custom
    )

    private lateinit var bookmarkLists : ItemListManager<Fractal>
    private lateinit var paletteLists  : ItemListManager<Palette>
    private lateinit var shapeLists    : ItemListManager<Shape>
    private lateinit var textureLists  : ItemListManager<Texture>

    private lateinit var editModeSelector         : ViewSelector
    private lateinit var paletteListTypeSelector  : ViewSelector
    private lateinit var shapeListTypeSelector    : ViewSelector
    private lateinit var textureListTypeSelector  : ViewSelector
    private lateinit var bookmarkListTypeSelector : ViewSelector

    private lateinit var customColorDragAdapter  : CustomColorDragAdapter
    private lateinit var textureImageListAdapter : FlexibleAdapter<TextureImageListItem>


    private var currentList: ViewGroup? = null


    // position variables
    var activePositionParam = PositionParam.ZOOM


    // color variables
    private var activeColorParam : ColorParam = ColorRealParam.FREQUENCY
    private var updateFromEdit = false


    // shape variables
    private var prevShape = Shape.mandelbrot
    private var shapeThumbnailsRendered = false


    // texture variables
    private var prevTexture = Texture.escapeSmooth
    private var compatTexturesChanged = false
    private var textureChanged = false
    private val resultContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result?.data?.data?.let {

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val inputStream = contentResolver?.openInputStream(it)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            var sampleSize = 1
            val minWidth = 180
            if (options.outWidth >= minWidth*2) {  // give resolution options

                val v = layoutInflater.inflate(R.layout.alert_dialog_texture_image_resolution, null, false)
                val textureImageResBar = v.findViewById<SeekBar>(R.id.alertResolutionSeekBar)
                val textureImageResText = v.findViewById<TextView>(R.id.alertResolutionText)

                var newWidth  : Int
                var newHeight : Int
                val sampleSizes = listOf(10, 8, 6, 4, 2, 1).takeLast(min(6, floor(options.outWidth.toDouble()/minWidth).toInt()))
                textureImageResBar.max = sampleSizes.size - 1
                textureImageResBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        sampleSize = sampleSizes.getOrNull(progress) ?: sampleSizes[3]
                        newWidth = (options.outWidth.toDouble()/sampleSize).toInt()
                        newHeight = (options.outHeight.toDouble()/sampleSize).toInt()
                        textureImageResText.text = "%d x %d".format(newWidth, newHeight)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}


                })
                textureImageResBar.progress = textureImageResBar.max

                val dialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
                    .setIcon(R.drawable.resolution)
                    .setTitle(R.string.resolution)
                    .setMessage(R.string.texture_image_choose_res)
                    .setView(v)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        confirmTextureImageResolution(sampleSize, it)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()

                dialog.showImmersive(v)
                dialog.setCanceledOnTouchOutside(false)

            } else {
                confirmTextureImageResolution(1, it)
            }

        }
    }


    private lateinit var settingsFragment   : Fragment
    private lateinit var videoFragment      : VideoFragment


    private lateinit var billingClient : BillingClient
    private val billingClientStateListener = object : BillingClientStateListener {

        override fun onBillingSetupFinished(billingResult: BillingResult) {
            Log.v("MAIN", "billing setup finished")
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> lifecycleScope.launch { queryPurchases() }
                else -> {
                    if (BuildConfig.DEV_VERSION) lifecycleScope.launch { queryPurchases() }
                    Log.w("MAIN", "unknown billing response code")
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
                Log.v("MAIN", "purchases updated")
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


        // Weak reference to the Activity; only access this from the UI thread.
        private val mWeakActivity : WeakReference<MainActivity> = WeakReference(activity)

        fun sendMessage(type: MessageType, arg1: Int = 0, arg2: Int = 0, obj: Any? = null) {
            sendMessage(obtainMessage(type.ordinal, arg1, arg2, obj))
        }

        fun updateColorThumbnails(updateAll: Boolean) {
            sendMessage(obtainMessage(MessageType.MSG_UPDATE_COLOR_THUMBNAILS.ordinal, updateAll))
        }
        fun updateTextureThumbnail(n: Int) {
            sendMessage(obtainMessage(MessageType.MSG_UPDATE_TEXTURE_THUMBNAILS.ordinal, n, 0))
        }
        fun updateShapeThumbnail(shape: Shape, customIndex: Int?) {
            sendMessage(obtainMessage(MessageType.MSG_UPDATE_SHAPE_THUMBNAILS.ordinal, customIndex ?: -1, -1, shape))
        }
        fun showMessage(msgId: Int) {
            sendMessage(obtainMessage(MessageType.MSG_SHOW_MESSAGE_ID.ordinal, msgId))
        }
        fun showMessage(msg: String) {
            sendMessage(obtainMessage(MessageType.MSG_SHOW_MESSAGE_STRING.ordinal, msg))
        }
        fun showBookmarkDialog() {
            sendMessage(obtainMessage(MessageType.MSG_SHOW_BOOKMARK_DIALOG.ordinal))
        }
        fun bookmarkAsPreviousFractal() {
            sendMessage(obtainMessage(MessageType.MSG_BOOKMARK_AS_PREVIOUS_FRACTAL.ordinal))
        }
        fun addKeyframe() {
            sendMessage(obtainMessage(MessageType.MSG_ADD_KEYFRAME.ordinal))
        }
        fun updateRenderStats(estTime: Double, time: Double, fps: String) {
            sendMessage(obtainMessage(MessageType.MSG_UPDATE_RENDER_STATS.ordinal, Bundle().apply {
                putDouble("estTime",    estTime)
                putDouble("time",       time)
                putString("fps",        fps)
            }))
        }
        fun updatePositionParam() {
            sendMessage(obtainMessage(MessageType.MSG_UPDATE_POS_PARAM.ordinal))
        }
        fun updateColorParam() {
            sendMessage(obtainMessage(MessageType.MSG_UPDATE_COLOR_PARAM.ordinal))
        }
        fun updateShapeTextureParam() {
            sendMessage(obtainMessage(MessageType.MSG_UPDATE_SHAPE_TEX_PARAM.ordinal))
        }
        fun updateProgress(p: Double) {
            sendMessage(obtainMessage(MessageType.MSG_UPDATE_PROGRESS.ordinal, p))
        }
        fun queryTutorialTaskComplete() {
            sendMessage(obtainMessage(MessageType.MSG_QUERY_TUTORIAL_TASK_COMPLETE.ordinal))
        }
        fun shareImage() {
            sendMessage(obtainMessage(MessageType.MSG_SHARE_IMAGE.ordinal))
        }
        fun hideUi() {
            sendMessage(obtainMessage(MessageType.MSG_HIDE_UI.ordinal))
        }
        fun showUi() {
            sendMessage(obtainMessage(MessageType.MSG_SHOW_UI.ordinal))
        }


        // runs on UI thread
        override fun handleMessage(msg: Message) {

            val ordinal = msg.what
            val type = MessageType.values()[ordinal]

            val activity = mWeakActivity.get()
            if (activity == null) Log.w("MAIN ACTIVITY", "ActivityHandler.handleMessage: activity is null")

            type.performAction(activity, msg)

        }

    }

    class Tutorial(vararg tasks: Task) {

        val tasks = tasks.toList()

        var inProgress = false

        var activeTaskIndex : Int = -1
        var activeTask : Task? = null

        fun start() {
            inProgress = true
            activeTask = tasks[0]
            activeTaskIndex = 0
            activeTask?.onStart()
        }

        fun next(v: ViewGroup) {
            if (activeTaskIndex == tasks.size - 1) {
                finish(v)
            } else {
                activeTask?.onComplete(v)
                activeTaskIndex++
                activeTask = tasks[activeTaskIndex]
                activeTask?.onStart()
            }
        }

        fun finish(v: ViewGroup) {
            inProgress = false
            activeTask?.onComplete(v)
            activeTask = null
            activeTaskIndex = -1
        }



        abstract class Task(tag: String) {

            abstract fun onStart()

            abstract fun updateProgress()

            abstract fun isComplete() : Boolean

            @CallSuper
            open fun onComplete(v: ViewGroup) {
                v.children.forEach { child -> child.hide() }
            }

        }

    }




    private val basicsTutorial = Tutorial()

    fun queryTutorialTaskComplete() {
        basicsTutorial.run {
            if (activeTask?.isComplete() == true) next(b.tutorialWindow.root) else activeTask?.updateProgress()
        }
    }




    /* LIFECYCLE */

    override fun onCreate(savedInstanceState: Bundle?) {


        val onCreateStartTime = currentTimeMs()
        super.onCreate(savedInstanceState)
        b = ActivityMainNewBinding.inflate(layoutInflater)
        setContentView(b.root)
        updateSystemBarsVisibility()

        b.root.addView(object : View(this) {
            override fun onConfigurationChanged(newConfig: Configuration?) {
                super.onConfigurationChanged(newConfig)
                computeWindowSizeClasses()
            }
        })
        computeWindowSizeClasses()



        val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        val usResources = getLocalizedResources(applicationContext, Locale.US)

        //setTheme(R.style.AppTheme)
        crashlytics().setCustomKey(CrashKey.ACT_MAIN_CREATED, false)
        crashlytics().setCustomKeys {
            key( CrashKey.ACT_MAIN_CREATED,   false )
            key( CrashKey.GOLD_ENABLED,       false )
            key( CrashKey.MAX_ITER, f.shape.params.detail.scaledValue.toInt() )
        }
        crashlytics().updateLastAction(Action.INIT)

        db = AppDatabase.getInstance(applicationContext)

        paletteListTypeSelector  = ViewSelector(this, R.drawable.edit_mode_button_highlight, b.paletteList  .run { listOf(defaultListButton, customListButton, favoritesListButton) })
        shapeListTypeSelector    = ViewSelector(this, R.drawable.edit_mode_button_highlight, b.shapeList    .run { listOf(defaultListButton, customListButton, favoritesListButton) })
        textureListTypeSelector  = ViewSelector(this, R.drawable.edit_mode_button_highlight, b.textureList  .run { listOf(defaultListButton, customListButton, favoritesListButton) })
        bookmarkListTypeSelector = ViewSelector(this, R.drawable.edit_mode_button_highlight, b.bookmarkList .run { listOf(defaultListButton, customListButton, favoritesListButton) })

        loadCustomItems()



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
        Log.v("MAIN ACTIVITY", "screen resolution : ($screenWidth, $screenHeight), ratio : $screenRatio")

        UiLayoutHeight.values().forEach { it.initialize(resources) }

        // set screen resolution
        // create and insert new resolution if different from preloaded resolutions
        if (Resolution.foregrounds.none { it.w == screenWidth }) {
            Resolution.addResolution(screenWidth)
            customScreenRes = true
        }
        Resolution.SCREEN = Resolution.valueOf(screenWidth) ?: Resolution.R1080
        Resolution.initialize(screenRatio)
        AspectRatio.initialize()


        val handler = ActivityHandler(this)
        val r = FractalRenderer(baseContext, handler)


        // restore SettingsConfig from SharedPreferences
        goldEnabledDialogShown = sp.getBoolean(PreferenceKey.GOLD_ENABLED_DIALOG_SHOWN, false)
        goldPendingDialogShown = sp.getBoolean(PreferenceKey.GOLD_PENDING_DIALOG_SHOWN, false)
        showEpilepsyDialog = sp.getBoolean(PreferenceKey.SHOW_EPILEPSY_DIALOG, true)
        showTutorialOption = sp.getBoolean(PreferenceKey.SHOW_TUTORIAL_OPTION, true)
        previousFractalCreated = sp.getBoolean(PreferenceKey.PREV_FRACTAL_CREATED, false)
        previousFractalId = sp.getInt(PreferenceKey.PREV_FRACTAL_ID, -1)
        Log.v("MAIN", "previousFractalCreated: $previousFractalCreated, id: $previousFractalId")

        Texture.CUSTOM_IMAGE_COUNT = sp.getInt(PreferenceKey.TEX_IMAGE_COUNT, 0)

        // val maxStartupRes = if (sc.goldEnabled) Resolution.SCREEN else Resolution.R1080
        val savedResolution = sp.getInt(PreferenceKey.RESOLUTION, Resolution.foregrounds.indexOf(Resolution.R1080))
        sc.resolution = Resolution.foregrounds.getOrNull(savedResolution) ?: Resolution.R720
        crashlytics().setCustomKey(CrashKey.RESOLUTION, sc.resolution.toString())

        sc.targetFramerate          = sp.getInt( PreferenceKey.TARGET_FRAMERATE, sc.targetFramerate )
        sc.buttonAlignment          = ButtonAlignment.values()[sp.getInt( PreferenceKey.BUTTON_ALIGNMENT, sc.buttonAlignment.ordinal )]

        sc.continuousPosRender      = sp.getBoolean( PreferenceKey.CONTINUOUS_RENDER,     sc.continuousPosRender     )
        sc.renderBackground         =
            if (sc.continuousPosRender) false
            else                      sp.getBoolean( PreferenceKey.RENDER_BACKGROUND,     sc.renderBackground        )
        sc.hideSystemBars           = sp.getBoolean( PreferenceKey.HIDE_NAV_BAR,          sc.hideSystemBars          )
        sc.autofitColorRange        = sp.getBoolean( PreferenceKey.AUTOFIT_COLOR_RANGE,   sc.autofitColorRange       )
        sc.useAlternateSplit        = sp.getBoolean( PreferenceKey.USE_ALTERNATE_SPLIT,   sc.useAlternateSplit       )
        sc.advancedSettingsEnabled  = sp.getBoolean( PreferenceKey.ADVANCED_SETTINGS,     sc.advancedSettingsEnabled )
        sc.ultraHighResolutions     = sp.getBoolean( PreferenceKey.ULTRA_HIGH_RES,        sc.ultraHighResolutions    )
        sc.restrictParams           = sp.getBoolean( PreferenceKey.RESTRICT_PARAMS,       sc.restrictParams          )
        sc.allowSlowDualfloat       = sp.getBoolean( PreferenceKey.ALLOW_SLOW_DUALFLOAT,  sc.allowSlowDualfloat      )
        f.color.fillColor           = sp.getInt(     PreferenceKey.ACCENT_COLOR1,         Color.WHITE                )

//        sc.colorListViewType    = ListLayoutType.values()[sp.getInt(COLOR_LIST_VIEW_TYPE, ListLayoutType.GRID.ordinal)]
//        sc.shapeListViewType    = ListLayoutType.values()[sp.getInt(SHAPE_LIST_VIEW_TYPE, ListLayoutType.GRID.ordinal)]
//        sc.textureListViewType  = ListLayoutType.values()[sp.getInt(TEXTURE_LIST_VIEW_TYPE, ListLayoutType.GRID.ordinal)]
//        sc.bookmarkListViewType = ListLayoutType.values()[sp.getInt(BOOKMARK_LIST_VIEW_TYPE, ListLayoutType.GRID.ordinal)]


        b.gestureInterceptTop.setOnTouchListener { v, event -> true }
        b.gestureInterceptBottom.setOnTouchListener { v, event -> true }
        b.overlay.setOnTouchListener { v, event ->
            if (b.extrasMenu.root.isVisible) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_UP -> { if (!basicsTutorial.inProgress) hideExtrasMenu() }
                    else -> {}
                }
                true
            } else false
        }
        b.listHolder.setOnTouchListener { v, event -> true }
//        b.editModeButtonLayout.setOnTouchListener { v, event -> true }

        listOf(
            b.overlay,
            b.paramLayout,
            b.paramDisplayFrame,
//            b.editModeButtonLayout,
//            b.positionParamMenu.root,
            b.colorParamMenu.root,
            b.shapeParamMenu.root,
            b.textureParamMenu.root,
            b.paletteList.filterButtonLayout,
            b.shapeList.filterButtonLayout,
            b.textureList.filterButtonLayout,
            b.bookmarkList.filterButtonLayout,
            b.positionParamDisplay.root,
            b.textureShapeParamDisplay.root,
            b.textureShapeParamDisplay.item.realParamDisplay,
            b.customShapeCreator.root,
            b.customShapeCreator.equationLayout
        ).forEach { it.layoutTransition?.enableTransitionType(LayoutTransition.CHANGING) }


        fsv = b.fractalSurfaceView
        fsv.initialize(r, handler)
        fsv.layoutParams = FrameLayout.LayoutParams(screenWidth, screenHeight, Gravity.CENTER)
        b.fractalLayout.layoutParams = FrameLayout.LayoutParams(screenWidth, screenHeight, Gravity.CENTER)

        var firstBookmarkSelection = true




        b.debugButton.setOnClickListener {

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 480, 1040).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, 30*1_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0)
            }

            val compatCodecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { info ->

                info.isEncoder && info.supportedTypes.contains(Video.FileType.MPEG4.codecMime)
            }
            compatCodecs.forEach { info ->
                info.getCapabilitiesForType(Video.FileType.MPEG4.codecMime).videoCapabilities.let { cap ->
                    Log.d(
                        VideoEncoder.TAG, "%28s - w: %10s, h: %10s, br: %16s, fr: %8s".format(
                        info.name, cap.supportedWidths.toString(), cap.supportedHeights.toString(), cap.bitrateRange.toString(), cap.supportedFrameRates.toString()
                    ))
                    // Log.d(TAG, "${info.name} - w: ${cap.supportedWidths}, h: ${cap.supportedHeights}, br: ${cap.bitrateRange}, fr: ${cap.supportedFrameRates}")
                }
            }

//            val codec = MediaCodec.createByCodecName(MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(format))
//            codec.codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
//            Log.d("MAIN", "${codec.name}")

        }



//        val editModeButtons = listOf(
//            b.positionModeButton,
//            b.colorModeButton,
//            b.shapeModeButton,
//            b.textureModeButton
//        )

//        editModeSelector = ViewSelector(this, R.drawable.edit_mode_button_highlight, editModeButtons)
//        val editModeButtonListener = View.OnClickListener {
//            (it as? EditModeButton)?.run {
//                if (editModeSelector.isSelected(this)) {
//                    Log.d("MAIN", "$mode re-selected")
//                }
//                else {  // new selection
//
//                    Log.d("MAIN", "$mode clicked")
//
//                    if (mode.showParams) b.paramMenuLayout.show() else b.paramMenuLayout.hide()
//
//                    editModeSelector.select(this)
//
//                    sc.editMode = mode
//                    mode.run {
//                        updateDisplay()
//                        updateAdjust()
//                    }
//
//                    if (basicsTutorial.inProgress) queryTutorialTaskComplete()
//
//                }
//            }
//        }
//        editModeButtons.forEachIndexed { i, button ->
//            button.mode = EditMode.values()[i]
//            button.setOnClickListener(editModeButtonListener)
//        }

        EditMode.POSITION.init(
            updateDisplay       = { updatePositionDisplay() }
        )
        EditMode.COLOR.init(
            listNavButtons      = setOf(b.actionDone, b.actionNew),
            customNavButtons    = setOf(b.actionDone, b.actionCancel),
            updateDisplay       = { updateColorDisplay() },
            updateLayout        = { updateColorLayout() }
        )
        EditMode.SHAPE.init(
            listNavButtons      = setOf(b.actionDone, b.actionNew),
            customNavButtons    = setOf(b.actionDone, b.actionCancel),
            updateDisplay       = { bindShapeParameter(f.shape.params.active); updateShapeDisplayValues() },
            updateAdjust        = { updateRealParamAdjust() },
            updateLayout        = { updateShapeLayout() }
        )
        EditMode.TEXTURE.init(
            listNavButtons      = setOf(b.actionDone),
            updateDisplay       = { updateShapeDisplayValues(); bindTextureParameter(f.texture.params.active) },
            updateAdjust        = { updateRealParamAdjust() },
            updateLayout        = { updateTextureLayout() }
        )
        EditMode.NONE.init(
            listLayout      = b.bookmarkList.root,
            listNavButtons  = setOf(b.actionDone, b.actionCancel)
        )



        currentList = b.paletteList.root



        b.actionDone.setOnClickListener {
            when (uiState) {
                UiState.CUSTOM_PALETTE -> {
                    when {
                        Palette.all.any { palette -> palette.name == f.palette.name && palette.id != f.palette.id } -> {
                            showMessage(resources.getString(R.string.msg_custom_name_duplicate).format(resources.getString(R.string.palette)))
                        }
                        f.palette.name == "" -> { showMessage(resources.getString(R.string.msg_empty_name)) }
                        else -> {

                            if (f.palette.hasCustomId) {
                                updateFromEdit = true
                                f.palette.commit(lifecycleScope, db)
                            }
                            else {
                                updateFromEdit = false
                                f.palette.finalize(lifecycleScope, db)
                                paletteLists.addNewItem(f.palette)
                            }

                            // update ui
                            fsv.r.renderProfile = RenderProfile.COLOR_THUMB
                            fsv.r.renderSingleThumbnail = true
                            fsv.requestRender()
                            if (Palette.custom.size == Palette.MAX_CUSTOM_PALETTES_FREE && !sc.goldEnabled) {
                                b.actionNew.showGradient = true
                            }
                            setUiState(UiState.EDITMODE_LIST)

                        }
                    }
                }
                UiState.CUSTOM_SHAPE -> {

                    if (!sc.goldEnabled) showUpgradeScreen()
                    else {
                        when {
                            Shape.all.any { shape -> f.shape.name == shape.name && f.shape.id != shape.id } -> {
                                showMessage(resources.getString(R.string.msg_custom_name_duplicate).format(
                                        resources.getString(R.string.shape)
                                ))
                            }
                            b.customShapeCreator.eqnErrorIndicator.isVisible() -> {
                                showMessage(resources.getString(R.string.msg_eqn_error))
                            }
                            f.shape.name == "" -> { showMessage(resources.getString(R.string.msg_empty_name)) }
                            else -> {

                                if (f.texture != prevTexture) textureChanged = true

                                if (f.shape.hasCustomId) {

                                    updateFromEdit = true

                                    f.shape.commit(lifecycleScope, db)

                                    fsv.r.renderProfile = RenderProfile.SHAPE_THUMB
                                    fsv.requestRender()

                                }
                                else {

                                    updateFromEdit = false

                                    f.shape.finalize(lifecycleScope, db)

                                    shapeLists.addNewItem(f.shape)

                                    fsv.r.renderProfile = RenderProfile.SHAPE_THUMB
                                    fsv.requestRender()

                                }

                                sc.editMode = EditMode.SHAPE

                                // update ui
                                // b.customShapeCreator.shapeMathQuill.getLatex { customShape.latex = it }

                                setUiState(UiState.EDITMODE_LIST)
                                // b.shapeListLayout.list.adapter?.notifyDataSetChanged()

                            }
                        }
                    }

                }
                UiState.BOOKMARK_LIST -> {
                    setUiState(UiState.HOME)
                    updateLayouts()
                    if (compatTexturesChanged) onCompatTexturesChanged()
                }
                UiState.EDITMODE_LIST -> {
                    setUiState(UiState.HOME)

                    when (currentList) {
                        b.shapeList.root -> {
                            updateShapeLayout()
                            if (textureChanged) updateTextureLayout()
                            if (compatTexturesChanged) onCompatTexturesChanged()
                        }
                        b.textureList.root -> {
                            if (textureChanged) {
                                updateColorLayout()
                                updateTextureLayout()
                            }
                        }
                        b.paletteList.root -> {
                            fsv.r.renderProfile = RenderProfile.DISCRETE
                            updateColorLayout()
                        }
                        else -> {}
                    }
                }
                UiState.VIDEO_CONFIG -> {
                    setUiState(UiState.HOME)
                }
                UiState.RANDOMIZER -> {
                    if (textureChanged) updateTextureLayout()
                    if (compatTexturesChanged) onCompatTexturesChanged()
                    setUiState(UiState.HOME)
                    updateLayouts()
                }
                else -> {
                    setUiState(UiState.HOME)
                    updateGradient()
                }
            }
        }
        b.actionNew.setOnClickListener {
            when (sc.editMode) {
                EditMode.POSITION -> {}
                EditMode.COLOR -> {
                    if (Palette.custom.size == Palette.MAX_CUSTOM_PALETTES_FREE && !sc.goldEnabled) showUpgradeScreen()
                    else {

                        crashlytics().updateLastAction(Action.PALETTE_CREATE)
                        setUiState(UiState.CUSTOM_PALETTE)

                        fsv.r.renderProfile = RenderProfile.DISCRETE

                        f.palette = Palette(
                                name = "%s %s %d".format(
                                        resources.getString(R.string.header_custom),
                                        resources.getString(R.string.palette),
                                        Palette.nextCustomPaletteNum
                                ),
                                colors = Palette.generateColors(if (sc.goldEnabled) 6 else 3)
                        ).apply { initialize(resources) }

                        fsv.requestRender()
                        customColorDragAdapter.updateColors(f.palette.colors)

                    }
                }
                EditMode.SHAPE -> {

                    crashlytics().updateLastAction(Action.SHAPE_CREATE)
                    setUiState(UiState.CUSTOM_SHAPE)
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

                    prevTexture = f.texture

                    sc.editMode = EditMode.COLOR
                    fsv.r.renderProfile = RenderProfile.DISCRETE

                    f.shape = Shape.createNewCustom(resources)
                    f.texture = Texture.escapeSmooth

                    b.customShapeCreator.apply {
                        shapeMathQuill.setLatex(f.shape.latex)
                        shapeMathQuill.setCustomLoop(f.shape.latex, f.shape)
                    }

                    fsv.r.renderShaderChanged = true
                    fsv.r.renderToTex = true
                    fsv.requestRender()

                }
                EditMode.TEXTURE -> {}
                EditMode.NONE -> {}
            }
        }
        b.actionCancel.setOnClickListener {
            when (uiState) {
                UiState.CUSTOM_PALETTE -> {
                    if (f.palette.hasCustomId) f.palette.revert()
                    else {
                        f.palette.release()
                        f.palette = paletteLists.getSelectedItem() ?: Palette.eye
                    }
                    setUiState(UiState.EDITMODE_LIST)
                    updateGradient()
                    fsv.requestRender()
                }
                UiState.CUSTOM_SHAPE -> {

                    f.texture = prevTexture

                    if (f.shape.hasCustomId) {
                        // revert changes
                        f.shape.revert()
                    } else {
                        // select previous shape
                        // customShape.release()
                        f.shape = shapeLists.getSelectedItem() ?: Shape.mandelbrot
                    }
                    sc.editMode = EditMode.SHAPE
                    fsv.r.renderShaderChanged = true
                    fsv.r.renderToTex = true
                    fsv.requestRender()

                    setUiState(UiState.EDITMODE_LIST)

                }
                UiState.BOOKMARK_LIST -> {

                    setUiState(UiState.HOME)
                    fsv.interruptRender()

                    handler.postDelayed({

                        if (!firstBookmarkSelection) f.load(Fractal.tempBookmark2, fsv)
                        fsv.r.checkThresholdCross { f.load(Fractal.tempBookmark3, fsv) }
                        fsv.r.renderShaderChanged = true
                        fsv.r.renderToTex = true
                        fsv.renderContinuousDiscrete()
                        firstBookmarkSelection = true

                    }, BUTTON_CLICK_DELAY_MED)

                }
                UiState.VIDEO_PROGRESS -> {

                    fsv.interruptRender()

                }
                else -> {}
            }
        }

        b.actionRandomize.setOnClickListener {
            b.randomizer.run {
                fsv.interruptRender()
                prevShape = f.shape
                prevTexture = f.texture
                f.randomize(
                    fsv,
                    randomizeShapeSwitch    .isChecked,
                    randomizeTextureSwitch  .isChecked,
                    randomizePositionSwitch .isChecked,
                    randomizeColorSwitch    .isChecked
                )
                if (f.texture != prevTexture) textureChanged = true
                if (f.shape.compatTextures != prevShape.compatTextures) compatTexturesChanged = true
                fsv.r.renderShaderChanged = true
                fsv.r.calcNewTextureSpan = true
                fsv.renderContinuousDiscrete()
            }
        }

        b.actionRender.setOnClickListener {
            val render = DialogVideoOptionsBinding.inflate(layoutInflater, null, false)
            AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setView(render.root)
                .setPositiveButton(R.string.render_video) { _, _ ->
                    fsv.saveVideo()
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .create()
                .showImmersive(render.root)
        }



        val paramSelector = ViewSelector(
            this,
            R.drawable.aspect_item_background,
            listOf(b.positionButton, b.waveformButton)
                .plus(b.colorParamMenu.run   { listOf(densityButton, fillColorButton, outlineColorButton) })
                .plus(b.shapeParamMenu.run   { listOf(detailButton, seedButton, juliaButton, shapeParamButton1, shapeParamButton2, shapeParamButton3, shapeParamButton4) })
                .plus(b.textureParamMenu.run { listOf(escapeRadiusButton, textureImageButton, textureParamButton1, textureParamButton2, textureParamButton3, textureParamButton4) })
        )



        /* POSITION */

        b.positionButton.setOnClickListener {
            sc.editMode = EditMode.POSITION
            paramSelector.select(it)
        }

        PositionParam.ZOOM.run {
            onDecreaseClick = PositionChangeOnClickListener(fsv,
                transformFractal  = { fsv.r.checkThresholdCross { f.shape.position.zoom(1f/PositionParam.ZOOM.discreteDelta.toFloat()) } },
                transformQuad     = { fsv.r.zoom(1f/PositionParam.ZOOM.discreteDelta.toFloat()) },
                updateLayout      = { updatePositionDisplay() }
            )
            onDecreaseLongClick = PositionChangeOnLongClickListener(fsv,
                transformFractal  = { fsv.r.checkThresholdCross { f.shape.position.zoom(1f/PositionParam.ZOOM.continuousDelta.toFloat()) } },
                transformQuad     = { fsv.r.zoom(1f/PositionParam.ZOOM.continuousDelta.toFloat()) },
                updateLayout      = { EditMode.POSITION.updateDisplay() }
            )
            onIncreaseClick = PositionChangeOnClickListener(fsv,
                transformFractal  = { fsv.r.checkThresholdCross { f.shape.position.zoom(PositionParam.ZOOM.discreteDelta.toFloat()) } },
                transformQuad     = { fsv.r.zoom(PositionParam.ZOOM.discreteDelta.toFloat()) },
                updateLayout      = { EditMode.POSITION.updateDisplay() }
            )
            onIncreaseLongClick = PositionChangeOnLongClickListener(fsv,
                transformFractal  = { fsv.r.checkThresholdCross { f.shape.position.zoom(PositionParam.ZOOM.continuousDelta.toFloat()) } },
                transformQuad     = { fsv.r.zoom(PositionParam.ZOOM.continuousDelta.toFloat()) },
                updateLayout      = { EditMode.POSITION.updateDisplay() }
            )
        }
        PositionParam.ROTATION.run {
            onDecreaseClick = PositionChangeOnClickListener(fsv,
                transformFractal = { f.shape.position.rotate(PositionParam.ROTATION.discreteDelta.toFloat()) },
                transformQuad = { fsv.r.rotate(PositionParam.ROTATION.discreteDelta.toFloat()) },
                updateLayout = { EditMode.POSITION.updateDisplay() }
            )
            onDecreaseLongClick = PositionChangeOnLongClickListener(fsv,
                transformFractal = { f.shape.position.rotate(PositionParam.ROTATION.continuousDelta.toFloat()) },
                transformQuad = { fsv.r.rotate(PositionParam.ROTATION.continuousDelta.toFloat()) },
                updateLayout = { EditMode.POSITION.updateDisplay() }
            )
            onIncreaseClick = PositionChangeOnClickListener(fsv,
                transformFractal = { f.shape.position.rotate(-PositionParam.ROTATION.discreteDelta.toFloat()) },
                transformQuad = { fsv.r.rotate(-PositionParam.ROTATION.discreteDelta.toFloat()) },
                updateLayout = { EditMode.POSITION.updateDisplay() }
            )
            onIncreaseLongClick = PositionChangeOnLongClickListener(fsv,
                transformFractal = { f.shape.position.rotate(-PositionParam.ROTATION.continuousDelta.toFloat()) },
                transformQuad = { fsv.r.rotate(-PositionParam.ROTATION.continuousDelta.toFloat()) },
                updateLayout = { EditMode.POSITION.updateDisplay() }
            )
        }
        PositionParam.SHIFT_HORIZONTAL.run {
            onDecreaseClick = PositionChangeOnClickListener(fsv,
                transformFractal = { f.shape.position.translate(-PositionParam.SHIFT_HORIZONTAL.discreteDelta.toFloat(), 0f) },
                transformQuad = { fsv.r.translate(-2f*PositionParam.SHIFT_HORIZONTAL.discreteDelta.toFloat(), 0f) },
                updateLayout = { EditMode.POSITION.updateDisplay() }
            )
            onDecreaseLongClick = PositionChangeOnLongClickListener(fsv,
                transformFractal = { f.shape.position.translate(-PositionParam.SHIFT_HORIZONTAL.continuousDelta.toFloat(), 0f) },
                transformQuad = { fsv.r.translate(-2f*PositionParam.SHIFT_HORIZONTAL.continuousDelta.toFloat(), 0f) },
                updateLayout = { EditMode.POSITION.updateDisplay() }
            )
            onIncreaseClick = PositionChangeOnClickListener(fsv,
                transformFractal = { f.shape.position.translate(PositionParam.SHIFT_HORIZONTAL.discreteDelta.toFloat(), 0f) },
                transformQuad = { fsv.r.translate(2f*PositionParam.SHIFT_HORIZONTAL.discreteDelta.toFloat(), 0f) },
                updateLayout = { EditMode.POSITION.updateDisplay() }
            )
            onIncreaseLongClick = PositionChangeOnLongClickListener(fsv,
                transformFractal = { f.shape.position.translate(PositionParam.SHIFT_HORIZONTAL.continuousDelta.toFloat(), 0f) },
                transformQuad = { fsv.r.translate(2f*PositionParam.SHIFT_HORIZONTAL.continuousDelta.toFloat(), 0f) },
                updateLayout = { EditMode.POSITION.updateDisplay() }
            )
        }
        PositionParam.SHIFT_VERTICAL.run {
            onDecreaseClick = PositionChangeOnClickListener(fsv,
                transformFractal = { f.shape.position.translate(0f, PositionParam.SHIFT_VERTICAL.discreteDelta.toFloat()) },
                transformQuad = { fsv.r.translate(0f, -2f*PositionParam.SHIFT_VERTICAL.discreteDelta.toFloat()) },
                updateLayout = { EditMode.POSITION.updateDisplay() }
            )
            onDecreaseLongClick = PositionChangeOnLongClickListener(fsv,
                transformFractal = { f.shape.position.translate(0f, PositionParam.SHIFT_VERTICAL.continuousDelta.toFloat()) },
                transformQuad = { fsv.r.translate(0f, -2f*PositionParam.SHIFT_VERTICAL.continuousDelta.toFloat()) },
                updateLayout = { EditMode.POSITION.updateDisplay() }
            )
            onIncreaseClick = PositionChangeOnClickListener(fsv,
                transformFractal = { f.shape.position.translate(0f, -PositionParam.SHIFT_VERTICAL.discreteDelta.toFloat()) },
                transformQuad = { fsv.r.translate(0f, 2f*PositionParam.SHIFT_VERTICAL.discreteDelta.toFloat()) },
                updateLayout = { EditMode.POSITION.updateDisplay() }
            )
            onIncreaseLongClick = PositionChangeOnLongClickListener(fsv,
                transformFractal = { f.shape.position.translate(0f, -PositionParam.SHIFT_VERTICAL.continuousDelta.toFloat()) },
                transformQuad = { fsv.r.translate(0f, 2f*PositionParam.SHIFT_VERTICAL.continuousDelta.toFloat()) },
                updateLayout = { EditMode.POSITION.updateDisplay() }
            )
        }

//        val positionParamSelector = ParamSelector(this, R.drawable.edit_mode_button_highlight, b.positionParamMenu.run { listOf(zoomButton, rotateButton, shiftHorizontalButton, shiftVerticalButton) })
//        val positionParamListener = { button: ParamButton, p: PositionParam -> View.OnClickListener {
//            bindPositionParameter(p)
//            positionParamSelector.select(button)
//        }}
//        b.positionParamMenu.run {
//
//            zoomButton              .setOnClickListener(positionParamListener( zoomButton,             PositionParam.ZOOM              ))
//            rotateButton            .setOnClickListener(positionParamListener( rotateButton,           PositionParam.ROTATION          ))
//            shiftVerticalButton     .setOnClickListener(positionParamListener( shiftVerticalButton,    PositionParam.SHIFT_VERTICAL    ))
//            shiftHorizontalButton   .setOnClickListener(positionParamListener( shiftHorizontalButton,  PositionParam.SHIFT_HORIZONTAL  ))
//
//            b.positionParamDisplay.rotationLock.setOnClickListener {
//                f.shape.position.rotationLocked = b.positionParamDisplay.rotationLock.isChecked
//            }
//
//            zoomButton.performClick()
//
//        }

        val resetPositionListener = View.OnClickListener {

            fsv.r.checkThresholdCross { f.shape.position.reset() }

            if (sc.autofitColorRange) fsv.r.calcNewTextureSpan = true
            if (fsv.r.isRendering) fsv.r.interruptRender = true
            fsv.renderContinuousDiscrete()
            updatePositionDisplay()

        }
        b.positionResetButton.setOnClickListener(resetPositionListener)
        b.positionParamsToggle.isChecked = false





        /* COLOR */

        b.waveformButton.setOnClickListener {
            sc.editMode = EditMode.COLOR
            paramSelector.select(it)
        }

        ColorAccent.FILL.run {
            color = { f.color.fillColor }
            updateColor = { c: Int -> f.color.fillColor = c }
        }
        ColorAccent.OUTLINE.run {
            color = { f.color.outlineColor }
            updateColor = { c: Int -> f.color.outlineColor = c }
        }

//        val colorParamSelector = ViewSelector(this, R.drawable.edit_mode_button_highlight, b.colorParamMenu.run { listOf(frequencyButton, phaseButton, densityButton, fillColorButton, outlineColorButton) })
        val colorParamListener = { button: ParamButton, p: ColorParam -> View.OnClickListener {
            sc.editMode = EditMode.COLOR
            bindColorParameter(p)
            paramSelector.select(button)
        }}
        b.colorParamMenu.run {

            fillColorButton.setOnClickListener(colorParamListener(fillColorButton, ColorAccent.FILL))
            outlineColorButton.setOnClickListener(colorParamListener(outlineColorButton, ColorAccent.OUTLINE))
            frequencyButton.setOnClickListener(colorParamListener(frequencyButton, ColorRealParam.FREQUENCY))
            phaseButton.setOnClickListener(colorParamListener(phaseButton, ColorRealParam.PHASE))
            densityButton.setOnClickListener(colorParamListener(densityButton, ColorRealParam.DENSITY))

        }

        val autoColorListener = View.OnClickListener { button ->

            button as ImageToggleButton

            sc.autofitColorRange = button.isChecked
            fsv.r.autofitColorSelected = button.isChecked
            fsv.r.calcNewTextureSpan = true
            if (button.isChecked) {

                fsv.interruptRender()
                if (f.texture.usesDensity) b.colorParamMenu.densityButton.show()
                fsv.r.renderToTex = true

            }
            else {

                f.color.run {
                    density = 0.0
                    b.colorParamMenu.run {
                        densityButton.hide()
                        frequencyButton.performClick()
                    }

                    f.color.unfitFromSpan(fsv.r.textureSpan)
                    fsv.r.setTextureSpan(0f, 1f)

                }

            }

            updateColorDisplay()
            fsv.requestRender()

        }
        b.paletteListButton.setOnClickListener {

            crashlytics().updateLastAction(Action.PALETTE_CHANGE)
            setUiState(UiState.EDITMODE_LIST)
            showList(b.paletteList.root)

            // color thumbnail render
            handler.postDelayed({

                fsv.r.renderProfile = RenderProfile.COLOR_THUMB
                fsv.r.renderAllThumbnails = true
                fsv.r.renderToTex = true
                fsv.requestRender()

            }, BUTTON_CLICK_DELAY_MED)

        }
        b.colorAutofitButton.setOnClickListener(autoColorListener)
        b.colorParamsToggle.isChecked = false

        b.colorParamDisplay.fillColorSelector.run {
            setOnColorChangedListener(object : OnColorChangeListener {
                override fun onColorChanged(newColor: Int) {
                    f.color.fillColor = newColor
                    fsv.requestRender()
                }
            })
            setOnEditCustomAccentListener(object : ColorAccentSelector.OnCustomAccentActionListener {
                override fun onEditStart() {
                    setUiState(UiState.CUSTOM_ACCENT)
                }
            })
        }
        b.colorParamDisplay.outlineColorSelector.run {
            setOnColorChangedListener(object : OnColorChangeListener {
                override fun onColorChanged(newColor: Int) {
                    f.color.outlineColor = newColor
                    fsv.requestRender()
                }
            })
            setOnEditCustomAccentListener(object : ColorAccentSelector.OnCustomAccentActionListener {
                override fun onEditStart() {
                    setUiState(UiState.CUSTOM_ACCENT)
                }
            })
        }
        b.customAccentDoneButton.setOnClickListener {
            setUiState(UiState.HOME)
            when (activeColorParam) {
                ColorAccent.FILL -> b.colorParamDisplay.fillColorSelector.closeColorSelector()
                ColorAccent.OUTLINE -> b.colorParamDisplay.outlineColorSelector.closeColorSelector()
            }
        }

        updateColorLayout()
        bindColorParameter(ColorRealParam.FREQUENCY)

        b.customPaletteCreator.run {
            val customColorClickListener = { selectedItemIndex: Int, color: Int ->
                colorSelector.setColor(color)

//                val hsv = color.toHSV()
//                val h = satValueSelector.hue.let { if (it.isNaN()) 0 else it.roundToInt() }
//                val s = (100f * hsv[1]).let { if (it.isNaN()) 0 else it.roundToInt() }
//                val value = (100f * hsv[2]).let { if (it.isNaN()) 0 else it.roundToInt()}
//                hueEdit.setText("%d".format(h))
//                satEdit.setText("%d".format(s))
//                valEdit.setText("%d".format(value))

            }
            customColorDragAdapter = CustomColorDragAdapter(
                f.palette.colors,
                R.layout.color_drag_item,
                R.id.colorView,
                true,
                customColorClickListener
            )
            colorList.setAdapter(customColorDragAdapter, true)
            colorList.setDragListListener(object : DragListView.DragListListener {
                override fun onItemDragStarted(position: Int) {}
                override fun onItemDragging(itemPosition: Int, x: Float, y: Float) {}
                override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
                    if (fromPosition != toPosition) {

                        f.palette.colors.add(toPosition, f.palette.colors.removeAt(fromPosition))
                        f.palette.updateFlatPalette()
                        customColorDragAdapter.run {
                            selectedItemIndex = when (selectedItemIndex) {
                                fromPosition -> toPosition
                                in toPosition until fromPosition -> selectedItemIndex + 1
                                in fromPosition until toPosition -> selectedItemIndex - 1
                                else -> selectedItemIndex
                            }
                        }
                        fsv.requestRender()

                    }
                }
            })
            colorList.setLayoutManager(LinearLayoutManager(baseContext, LinearLayoutManager.HORIZONTAL, false))

//            f.paletteName.setOnEditorActionListener(editListener(null) { w: TextView ->
//                f.palette.name = w.text.toString()
//            })
            colorSelector.setOnColorChangeListener(object : OnColorChangeListener {
                override fun onColorChanged(newColor: Int) {

                    Log.d("MAIN", "hsv selector -- color changed")
                    f.palette.colors[customColorDragAdapter.selectedItemIndex] = newColor
                    f.palette.updateFlatPalette()

                    customColorDragAdapter.updateColor(customColorDragAdapter.selectedItemIndex, newColor)

//                    val hsv = newColor.toHSV()
//                    val h = hue.let { if (it.isNaN()) 0 else it.roundToInt() }
//                    val s = (100f * hsv[1]).let { if (it.isNaN()) 0 else it.roundToInt() }
//                    val value = (100f * hsv[2]).let { if (it.isNaN()) 0 else it.roundToInt()}
//
//                    hueEdit.setText("%d".format(h))
//                    satEdit.setText("%d".format(s))
//                    valEdit.setText("%d".format(value))

                    fsv.requestRender()

                }
            })
            randomizeButton.setOnClickListener {
                val newColors = Palette.generateColors(f.palette.colors.size)
                customColorDragAdapter.updateColors(newColors)
                f.palette.colors = newColors
                f.palette.updateFlatPalette()
                fsv.requestRender()
            }
            editNameButton.setOnClickListener {
                val dialogView = AlertDialogEditNameBinding.inflate(layoutInflater, null, false)
                dialogView.name.setText(f.palette.name)
                AlertDialog.Builder(this@MainActivity, R.style.AlertDialogCustom)
                    .setView(dialogView.root)
                    .setTitle(R.string.name)
                    .setIcon(R.drawable.edit_name)
                    .setPositiveButton(android.R.string.ok) { di, i ->
                        f.palette.name = dialogView.name.text.toString()
                    }
                    .create()
                    .showImmersive(dialogView.root)
            }
            addColorButton.setOnClickListener {

                if (f.palette.colors.size == Palette.MAX_CUSTOM_COLORS_FREE && !sc.goldEnabled) showUpgradeScreen()
                else {
                    customColorDragAdapter.run {
                        if (f.palette.colors.size < Palette.MAX_CUSTOM_COLORS_GOLD) {

                            val newColor = randomColor()
                            f.palette.colors.add(selectedItemIndex + 1, newColor)
                            addItem(selectedItemIndex + 1, Pair(nextUniqueId, newColor))
                            // itemList.add(Pair(getNextUniqueId(), newColor))
                            // notifyItemInserted(itemCount)

                            f.palette.updateFlatPalette()
                            fsv.requestRender()

                            when (f.palette.colors.size) {
                                Palette.MAX_CUSTOM_COLORS_FREE -> {
                                    // if (!sc.goldEnabled) addColorButton.showGradient = true
                                    removeColorButton.enable()
                                }
                                Palette.MAX_CUSTOM_COLORS_GOLD -> addColorButton.disable()
                            }

                        }
                    }
                }

            }
            removeColorButton.setOnClickListener {

                with (colorList.adapter as CustomColorDragAdapter) {

                    f.palette.colors.removeAt(selectedItemIndex)
                    removeItem(selectedItemIndex)
                    // itemList.removeAt(selectedItemIndex)
                    when (selectedItemIndex) {
                        itemList.size -> {
                            selectedItemIndex--
                        }
                        else -> {}
                    }
                    // notifyItemChanged(selectedItemIndex)
                    notifyDataSetChanged()

                    Log.v("MAIN", "selectedItemIndex: $selectedItemIndex")
                    f.palette.updateFlatPalette()
                    fsv.requestRender()

                    when (f.palette.colors.size) {
                        Palette.MAX_CUSTOM_COLORS_GOLD - 1 -> addColorButton.enable()
                        Palette.MAX_CUSTOM_COLORS_FREE - 1 -> {
                            // if (!sc.goldEnabled) addColorButton.showGradient = false
                            removeColorButton.disable()
                        }
                    }

                }

            }
        }





        b.textureShapeParamDisplay.item.run {

//            resetButton.setOnClickListener {
//                if (fsv.r.isRendering) fsv.r.interruptRender = true
//                activeParam().reset()
//                updateShapeDisplayValues()
//                fsv.renderContinuousDiscrete()
//            }
            realParamValue.setOnEditorActionListener(editListener(null) { w: TextView ->
                val result = "${w.text}".formatToDouble()
                val param = activeParam()
                if (result != null) {
                    param.run {
                        u = if (displayLinear) scale.convert(result) else result
                    }
                    fsv.renderContinuousDiscrete()
                }
                w.text = param.toString()
            })
            complexParamValue1.setOnEditorActionListener(editListener(complexParamValue2) { w: TextView ->
                val result = "${w.text}".formatToDouble()
                val param = activeParam()
                if (result != null) {
                    param.u = result
                    fsv.renderContinuousDiscrete()
                }
                w.text = param.u.format(COMPLEX_PARAM_DIGITS)
            })
            complexParamValue2.setOnEditorActionListener(editListener(null) { w: TextView ->
                val result = "${w.text}".formatToDouble()
                val param = activeParam()
                if (param is ComplexParam) {
                    if (result != null) {
                        param.v = result
                        fsv.renderContinuousDiscrete()
                    }
                    w.text = param.v.format(COMPLEX_PARAM_DIGITS)
                }
            })
            sensitivity.sensitivityValue.setOnEditorActionListener(editListener(null) { w: TextView ->
                val param = activeParam()
                val result = w.text.toString().formatToDouble()
                if (result != null) param.sensitivity = result
                w.text = "%d".format(param.sensitivity.toInt())
            })
            uLock.setOnClickListener {
                activeParam().uLocked = uLock.isChecked
            }
            vLock.setOnClickListener {
                activeParam().apply { if (this is ComplexParam) vLocked = vLock.isChecked }
            }

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, progress: Int, fromUser: Boolean) {

                    if (fromUser) {
                        val param = activeParam()
                        param.setValueFromProgress(progress.toDouble() / seekBar.max.toDouble())
                        updateShapeDisplayValues()
                    }

                    if (fromUser && fsv.r.renderProfile == RenderProfile.CONTINUOUS) {
                        fsv.r.renderToTex = true
                        fsv.requestRender()
                    }
                    if (basicsTutorial.inProgress) {
                        queryTutorialTaskComplete()
                    }

                }
                override fun onStartTrackingTouch(s: SeekBar) {

                    fsv.interruptRender()
                    fsv.r.renderProfile = RenderProfile.CONTINUOUS

                    val p = seekBar.progress.toDouble() / seekBar.max.toDouble()
                    activeParam().setValueFromProgress(p)

//                    f.shape.params.detail.u = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW) - 1.0
//                    b.maxIterValue.setText("%d".format(f.shape.params.detail.u.toInt()))

                    updateShapeDisplayValues()

                }
                override fun onStopTrackingTouch(s: SeekBar) {

                    Log.v("MAIN", "heyyy")

                    fsv.r.renderProfile = RenderProfile.DISCRETE
                    fsv.r.renderToTex = true
                    fsv.requestRender()

                    val p = seekBar.progress.toDouble() / seekBar.max.toDouble()
                    activeParam().setValueFromProgress(p)
                    updateShapeDisplayValues()

                    // val newIter = 2.0.pow(p*ITER_MAX_POW + (1.0 - p)*ITER_MIN_POW) - 1.0

                    // save state on iteration increase
                    // if (newIter > f.shape.params.detail.u.toInt()) bookmarkAsPreviousFractal()

//                    f.shape.params.detail.u = newIter
//                    crashlytics().setCustomKey(CrashKey.MAX_ITER, f.shape.params.detail.u.toInt())
//                    b.maxIterValue.setText("%d".format(f.shape.params.detail.u.toInt()))

                }
            })

        }


        /* SHAPE */

        var equationLayoutMinimized = true
//        val shapeParamSelector = ViewSelector(this, R.drawable.edit_mode_button_highlight, b.shapeParamMenu.run {
//            listOf(
//                detailButton,
//                juliaButton,
//                seedButton,
//                shapeParamButton1,
//                shapeParamButton2,
//                shapeParamButton3,
//                shapeParamButton4
//            )
//        })
        val setShapeParamListeners = { button: ParamButton, p: () -> RealParam ->
            button.setOnClickListener {
                sc.editMode = EditMode.SHAPE
                val param = p()
                if (param.goldFeature && !sc.goldEnabled) showUpgradeScreen()
                else {
                    f.shape.params.active = param
                    bindShapeParameter(param)
                    paramSelector.select(button)
                    updateShapeDisplayValues()
                    if (param !is ComplexParam) updateRealParamAdjust()
                }
            }
            button.setOnLongClickListener {
                val param = p()
                if (!param.goldFeature || sc.goldEnabled) {
                    param.reset()
                    fsv.r.renderToTex = true
                    fsv.requestRender()
                    button.performClick()
                    showMessage(resources.getString(R.string.param_reset).format(param.name.uppercase()), Toast.LENGTH_SHORT)
                }
                true
            }
        }

        b.shapeParamMenu.run {
            listOf(shapeParamButton1, shapeParamButton2, shapeParamButton3, shapeParamButton4).forEachIndexed { i, button ->
                setShapeParamListeners(button) { f.shape.params.at(i) }
            }
            setShapeParamListeners(detailButton)     { f.shape.params.detail }
            setShapeParamListeners(juliaButton) { f.shape.params.julia  }
            setShapeParamListeners(seedButton)  { f.shape.params.seed   }
        }
        b.shapeListButton.setOnClickListener {

            prevShape = f.shape

            crashlytics().updateLastAction(Action.SHAPE_CHANGE)
            setUiState(UiState.EDITMODE_LIST)
            showList(b.shapeList.root)


            if (!shapeThumbnailsRendered && Shape.custom.isNotEmpty()) {
                handler.postDelayed({

                    // showThumbnailRenderDialog()
                    b.thumbnailProgressBar.show()
                    b.thumbnailProgressBar.isIndeterminate = true

                    // render custom shape thumbnails
                    fsv.r.renderProfile = RenderProfile.SHAPE_THUMB
                    fsv.r.renderAllThumbnails = true
                    fsv.requestRender()

                }, BUTTON_CLICK_DELAY_MED)
            }

            // sc.editMode = EditMode.NONE

        }
        b.juliaModeButton.setOnClickListener { button ->
            button as GradientImageToggleButton
            if (f.shape != Shape.mandelbrot && !sc.goldEnabled) showUpgradeScreen()
            else {

                fsv.r.checkThresholdCross { f.shape.juliaMode = button.isChecked }

                b.shapeParamMenu.apply {
                    if (f.shape.juliaMode) {

                        seedButton.hide()
                        juliaButton.show()
                        juliaButton.performClick()

                    } else {

                        if (f.shape.params.active == f.shape.params.julia) {
                            detailButton.performClick()
                        }
                        if (f.shape.juliaSeed) seedButton.hide() else seedButton.show()
                        juliaButton.hide()

                    }
                }

                // loadActiveShapeParam()
                EditMode.POSITION.updateDisplay()

                fsv.interruptRender()
                fsv.r.renderShaderChanged = true
                fsv.renderContinuousDiscrete()

            }
            if (basicsTutorial.inProgress) queryTutorialTaskComplete()
        }
        b.juliaModeButton.setOnLongClickListener { button ->
            if (BuildConfig.DEV_VERSION) {
                if (f.shape.juliaMode) {
                    f.shape.positions.main.x = f.shape.params.julia.u
                    f.shape.positions.main.y = f.shape.params.julia.v
                } else {
                    f.shape.params.julia.setFrom(ComplexParam(f.shape.position.x, f.shape.position.y))
                }
                button.performClick()
                true
            } else { false }
        }
        b.customShapeCreator.run {

            equationLayoutSizeButton.setOnClickListener {
                if (equationLayoutMinimized) {
                    equationLayoutMinimized = false
                    equationLayout.updateLayoutParams {
                        height = resources.getDimension(R.dimen.equationLayoutHeightMax).toInt()
                    }
                    equationLayoutSizeButton.setImageResource(R.drawable.minimize)
                } else {
                    equationLayoutMinimized = true
                    equationLayout.updateLayoutParams {
                        height = resources.getDimension(R.dimen.equationLayoutHeightMin).toInt()
                    }
                    equationLayoutSizeButton.setImageResource(R.drawable.maximize)
                }
            }

            shapeMathQuill.apply {
                fsv = this@MainActivity.fsv
                eqnErrorIndicator = b.customShapeCreator.eqnErrorIndicator
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = false
                    builtInZoomControls = true
                    displayZoomControls = false
                    setSupportZoom(true)
                    defaultTextEncodingName = "utf-8"
                }
                loadDataWithBaseURL("file:///android_asset/", readHtml("mathquill.html"), "text/html", "UTF-8", null)
                setBackgroundColor(Color.TRANSPARENT)
            }
            val keyListener = { expr: Expr -> View.OnClickListener {
                shapeMathQuill.enterExpr(expr)
                shapeMathQuill.getLatex(f.shape)
            }}
            val shapeKeyListLayoutManager = GridLayoutManager(this@MainActivity, 4, GridLayoutManager.VERTICAL, false)
            val shapeKeyListItems = arrayListOf<ShapeKeyListItem>()

            Expr.numbers.forEach    { expr -> shapeKeyListItems.add(ShapeKeyListItem(expr, ShapeKeyListHeader.numbers))        }
            Expr.basic.forEach      { expr -> shapeKeyListItems.add(ShapeKeyListItem(expr, ShapeKeyListHeader.basic))        }
            Expr.trig.forEach       { expr -> shapeKeyListItems.add(ShapeKeyListItem(expr, ShapeKeyListHeader.trigonometry)) }

            val shapeKeyListAdapter = FlexibleAdapter(shapeKeyListItems)
            shapeKeyListLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) : Int {
                    return if (Expr.trig.contains(shapeKeyListAdapter.getItem(position)?.expr)) 2 else 1
                }
            }
            shapeKeyList.apply {
                adapter = shapeKeyListAdapter
                layoutManager = shapeKeyListLayoutManager
            }
            shapeKeyListAdapter.mItemClickListener = FlexibleAdapter.OnItemClickListener { _, position ->
                if (shapeKeyListAdapter.getItemViewType(position) !in nonClickableViewTypes) {
                    val expr = shapeKeyListAdapter.getItem(position)?.expr ?: Expr.z
                    shapeMathQuill.enterExpr(expr)
                    shapeMathQuill.getLatex(f.shape)
                    true
                } else false
            }

            zKey.setOnClickListener(keyListener(Expr.z))
            cKey.setOnClickListener(keyListener(Expr.c))
            prevKey.setOnClickListener { shapeMathQuill.enterKeystroke(Keystroke.LEFT) }
            nextKey.setOnClickListener { shapeMathQuill.enterKeystroke(Keystroke.RIGHT) }
            deleteKey.setOnClickListener {
                shapeMathQuill.enterKeystroke(Keystroke.BACKSPACE)
                shapeMathQuill.getLatex(f.shape)
            }
            leftParenKey.setOnClickListener(keyListener(Expr.leftParen))
            rightParenKey.setOnClickListener(keyListener(Expr.rightParen))

        }
        updateShapeLayout()
        bindShapeParameter(f.shape.params.detail)



        /* TEXTURE */

//        val textureParamSelector = ViewSelector(this, R.drawable.edit_mode_button_highlight, b.textureParamMenu.run { listOf(escapeRadiusButton, textureImageButton, textureParamButton1, textureParamButton2, textureParamButton3, textureParamButton4) })
        val setTextureParamListeners = { button: ParamButton, p: () -> RealParam ->
            button.setOnClickListener {
                sc.editMode = EditMode.TEXTURE
                val param = p()
                if (param.goldFeature && !sc.goldEnabled) showUpgradeScreen()
                else {
                    EditMode.TEXTURE.paramDisplayLayout = b.textureShapeParamDisplay.root
                    if (b.textureImageDisplay.root.isVisible) {
                        b.textureImageDisplay.root.hide()
                        b.textureShapeParamDisplay.root.show()
                    }
                    bindTextureParameter(param)
                    f.texture.params.active = param
                    paramSelector.select(button)
                    updateTextureDisplayValues()
                    if (param !is ComplexParam) updateRealParamAdjust()
                }
            }
            button.setOnLongClickListener {
                val param = p()
                if (!param.goldFeature || sc.goldEnabled) {
                    param.reset()
                    fsv.r.renderToTex = true
                    fsv.requestRender()
                    button.performClick()
                    showMessage(resources.getString(R.string.param_reset).format(param.name.uppercase()), Toast.LENGTH_SHORT)
                }
                true
            }
        }

        b.textureParamMenu.run {
            listOf(textureParamButton1, textureParamButton2, textureParamButton3, textureParamButton4).forEachIndexed { i, button ->
                setTextureParamListeners(button) { f.texture.params.at(i) }
            }
            setTextureParamListeners(escapeRadiusButton) { f.texture.params.radius }
            textureImageButton.setOnClickListener {
                EditMode.TEXTURE.paramDisplayLayout = b.textureImageDisplay.root
                paramSelector.select(textureImageButton)
                b.textureShapeParamDisplay.root.hide()
                b.textureImageDisplay.root.show()
            }
        }
        b.run {
            textureListButton.setOnClickListener {

                prevTexture = f.texture

                crashlytics().updateLastAction(Action.TEXTURE_CHANGE)
                setUiState(UiState.EDITMODE_LIST)
                showList(b.textureList.root)

                // save state on texture thumb render
                bookmarkAsPreviousFractal()

                // texture thumbnail render
                handler.postDelayed({

                    if (sc.textureListViewType == ListLayoutType.GRID && !fsv.r.validTextureThumbs) {

                        b.thumbnailProgressBar.show()
                        b.thumbnailProgressBar.isIndeterminate = true
                        // showThumbnailRenderDialog()

                        fsv.r.renderProfile = RenderProfile.TEXTURE_THUMB
                        fsv.r.renderAllThumbnails = true
                        fsv.requestRender()

                    }

                }, BUTTON_CLICK_DELAY_MED)

            }
            textureRegionButton.setOnClickListener {
                f.textureRegion = TextureRegion.values()[(TextureRegion.values().indexOf(f.textureRegion) + 1) % 3]
                textureRegionButton.setImageResource(f.textureRegion.iconId)
                fsv.requestRender()
            }
        }

        updateTextureLayout()
        bindTextureParameter(f.texture.params.radius)









        settingsFragment = SettingsFragment()
        videoFragment = VideoFragment()


        onAspectRatioChanged()
        updateButtonAlignment()

        b.uiToggleButton.setOnClickListener {
            // if (b.editModeButtonLayout.isVisible) hideAllUi(true, false) else showAllUi()
        }

        b.tutorialWindow.nextButton.setOnClickListener { basicsTutorial.next(b.tutorialWindow.root) }
        b.tutorialWindow.finishButton.setOnClickListener { basicsTutorial.finish(b.tutorialWindow.root) }

        b.videoConfigWindow.run {

            autocolorSwitch.setOnClickListener {
//                fsv.video.autocolor = autocolorSwitch.isChecked
            }
            initialZoomValue.setOnEditorActionListener(editListener(finalZoomValue) { w ->
                val result = w.text.toString().formatToDouble()
                if (result != null) {
                    PositionParam.ZOOM.fromDislayFormat(fsv.video.keyframes[0].f.position!!, result)
                    fsv.video.hasValidColorframes = false
                }
                w.setText(PositionParam.ZOOM.toDisplayFormat(fsv.video.keyframes[0].f.position!!))
            })
            finalZoomValue.setOnEditorActionListener(editListener(durationValue) { w ->
                val result = w.text.toString().formatToDouble()
                if (result != null) {
                    PositionParam.ZOOM.fromDislayFormat(fsv.video.keyframes[1].f.position!!, result)
                    fsv.video.hasValidColorframes = false
                }
                w.setText(PositionParam.ZOOM.toDisplayFormat(fsv.video.keyframes[1].f.position!!))
            })
            durationValue.setOnEditorActionListener(editListener(rotationsValue) { w ->
                val result = w.text.toString().formatToDouble()
                if (result != null) {
                    fsv.video.transitions[0].duration = floor(result.clamp(5.0, 60.0))
                    fsv.video.hasValidColorframes = false
                }
                w.setText(fsv.video.transitions[0].duration.toString())
                fileSizeText.text = resources.getString(R.string.estimated_file_size).format(fsv.video.fileSize)
            })
            rotationsValue.setOnEditorActionListener(editListener(null) { w ->
                val result = w.text.toString().formatToDouble()
                if (result != null) {
                    fsv.video.transitions[0].rotations = result.clamp(0.0, 5.0).toInt()
                    fsv.video.hasValidColorframes = false
                }
                w.setText("%d".format(fsv.video.transitions[0].rotations))
            })

            resolutionSelector.setOnSelectionChangeListener(object : EnumSelector.OnSelectionChangeListener {
                override fun onSelectionChange(newSelection: Int) {
                    fsv.video.outResolution = listOf(Resolution.R480, Resolution.R720, Resolution.R1080, Resolution.R1440, Resolution.R2160)[newSelection]
                    fileSizeText.text = fsv.video.getDisplayFileSize()
                    fsv.videoEncoder.updateFormat()
                }
            })
            framerateSelector.setOnSelectionChangeListener(object : EnumSelector.OnSelectionChangeListener {
                override fun onSelectionChange(newSelection: Int) {
                    fsv.video.framerate = Video.Framerate.values()[newSelection]
                    fileSizeText.text = fsv.video.getDisplayFileSize()
                    fsv.videoEncoder.updateFormat()
                }
            })
            qualitySelector.setOnSelectionChangeListener(object : EnumSelector.OnSelectionChangeListener {
                override fun onSelectionChange(newSelection: Int) {
                    fsv.video.quality = Video.Quality.values()[newSelection]
                    fileSizeText.text = fsv.video.getDisplayFileSize()
                }
            })

            previewVideoButton.setOnClickListener {

                if (fsv.video.useAutocolor && !fsv.video.hasValidColorframes) {
                    setUiState(UiState.VIDEO_PROGRESS)
                    b.videoProgressWindow.description.setText(R.string.preprocessing_video)
                    setVideoProgress(0.0)
                    fsv.renderColorframes {
                        fsv.previewVideo()
                    }
                } else {
                    fsv.previewVideo()
                }

            }
            cancelVideoButton.setOnClickListener {
                fsv.video.reset()
                setUiState(UiState.HOME)
            }
            renderVideoButton.setOnClickListener {

                if (fsv.video.keyframes[0].f.position!!.zoom <= fsv.video.keyframes[1].f.position!!.zoom) {
                    showMessage(R.string.msg_invalid_zoom_values)
                } else {

                    hideHomeUi(except = listOf(b.progressBar))
                    b.uiToggleButton.hide()
                    hide()
                    if (fsv.video.useAutocolor && !fsv.video.hasValidColorframes) {
                        setUiState(UiState.VIDEO_PROGRESS)
                        b.videoProgressWindow.description.setText(R.string.preprocessing_video)
                        setVideoProgress(0.0)
                        fsv.renderColorframes {
                            b.videoProgressWindow.description.setText(R.string.rendering_video)
                            setVideoProgress(0.0)
                            fsv.saveVideo()
                        }
                    } else {
                        hideHomeUi(except = listOf(b.progressBar))
                        b.uiToggleButton.hide()
                        hide()
                        setUiState(UiState.VIDEO_PROGRESS)
                        b.videoProgressWindow.description.setText(R.string.rendering_video)
                        setVideoProgress(0.0)
                        fsv.saveVideo()
                    }

                }

            }

        }

        b.extrasMenu.run {

            settingsButton.setOnClickListener {
                hideExtrasMenu()
                openSettingsMenu()
            }
            tutorialButton.setOnClickListener {
                hideExtrasMenu()
                basicsTutorial.start()
            }
            upgradeButton.setOnClickListener {
                hideExtrasMenu()
                showUpgradeScreen(true)
            }
            aboutButton.setOnClickListener {

                hideExtrasMenu()

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

                AlertDialog.Builder(this@MainActivity, R.style.AlertDialogCustom)
                    .setTitle(R.string.about)
                    .setIcon(R.drawable.info)
                    .setView(about.root)
                    .setPositiveButton(android.R.string.ok) { dialog, which -> }
                    .create()
                    .showImmersive(about.root)
            }
            shareButton.setOnClickListener {
                saveImageForShare()
            }
            shareButton.hide()

            bookmarkListButton.setOnClickListener {

                hideExtrasMenu()
                crashlytics().updateLastAction(Action.BOOKMARK_LOAD)

                setUiState(UiState.BOOKMARK_LIST)
                Fractal.tempBookmark3 = f.bookmark(fsv)

            }
            newBookmarkButton.setOnClickListener {

                hideExtrasMenu()

                crashlytics().updateLastAction(Action.NEW_BOOKMARK)
                Fractal.tempBookmark1 = f.bookmark(fsv)

                fsv.r.renderProfile = RenderProfile.SAVE_THUMBNAIL
                fsv.requestRender()

            }
            newVideoButton.setOnClickListener {

                hideExtrasMenu()

                setUiState(UiState.VIDEO_CONFIG)

                fsv.video.run {

                    useAutocolor = sc.autofitColorRange

                    val prevZoom = f.shape.position.zoom
                    f.shape.position.zoom = 10.0.pow(0.75)
                    addKeyframe(Video.Keyframe(f.bookmark(fsv)))

                    f.shape.position.zoom = prevZoom
                    addKeyframe(Video.Keyframe(f.bookmark(fsv)))

                    addTransition()

                }

                fsv.videoEncoder.updateFormat()

                b.videoConfigWindow.run {
                    initialZoomValue.setText((0.0).toString())
                    finalZoomValue.setText(PositionParam.ZOOM.toDisplayFormat(f.shape.position))
                    durationValue.setText(fsv.video.duration.toString())
                    fileSizeText.text = fsv.video.getDisplayFileSize()
                }

//                Fractal.tempBookmark1 = f.bookmark(fsv)
//                fsv.r.renderProfile = RenderProfile.KEYFRAME_THUMB
//                fsv.requestRender()

            }
            if (!BuildConfig.DEV_VERSION) newVideoButton.hide()

            resolutionButton.setOnClickListener {
                b.extrasMenu.optionsLayout.hide()
                b.extrasMenu.resolutionLayout.show()
            }
            val resolutionButtons = arrayListOf(r360, r480, r720, r1080, r1440, r2160, r2880, r3600, r4320, r5040, r5760).apply {
                if (customScreenRes) {
                    val i = Resolution.foregrounds.indexOf(Resolution.SCREEN)
                    add(i, rCustomScreen)
                    rCustomScreen.show()
                    resolutionLayout.removeViewAt(0)
                    resolutionLayout.addView(rCustomScreen, i)
                }
            }
            val resolutionParamList = ViewSelector(this@MainActivity, R.drawable.edit_mode_button_highlight, resolutionButtons).also { it.select(resolutionButtons[Resolution.foregrounds.indexOf(sc.resolution)]) }
            val resolutionButtonListener = { button: Button, res: Resolution -> View.OnClickListener {

                if (sc.resolution != res) {

                    crashlytics().updateLastAction(Action.RESOLUTION_CHANGE)

                    if (res.goldFeature && !sc.goldEnabled) showUpgradeScreen()
                    else {

                        resolutionParamList.select(button)

                        // save state on resolution increase
                        if (res.w > sc.resolution.w) bookmarkAsPreviousFractal()

                        sc.resolution = res
                        crashlytics().setCustomKey(CrashKey.RESOLUTION, sc.resolution.toString())
                        fsv.interruptRender()
                        fsv.r.fgResolutionChanged = true
                        fsv.r.renderToTex = true
                        fsv.requestRender()

                    }

                }
            }}
            resolutionButtons.forEachIndexed { i, button ->
                if (Resolution.foregrounds[i].goldFeature) button.showGradient = true
                button.setOnClickListener(resolutionButtonListener(button, Resolution.foregrounds[i]))
            }

            aspectRatioButton.setOnClickListener {
                b.extrasMenu.run {
                    optionsLayout.hide()
                    aspectRatioLayout.show()
                }
            }
            val aspectButtons = listOf(aspectDefault, aspect11, aspect45, aspect57, aspect23, aspect916, aspect12)
            val aspectParamList = ViewSelector(this@MainActivity, R.drawable.edit_mode_button_highlight, aspectButtons)
            val aspectRatioButtonListener = { button: Button, ratio: AspectRatio -> View.OnClickListener {
                if (ratio.goldFeature && !sc.goldEnabled) showUpgradeScreen()
                else {

                    aspectParamList.select(button)
                    sc.aspectRatio = ratio
                    updateResolutionText()
                    onAspectRatioChanged()

                }
            }}
            aspectButtons.forEachIndexed { i, button ->
                button.setOnClickListener(aspectRatioButtonListener(button, AspectRatio.all[i]))
            }
            listOf(aspect45, aspect57, aspect23, aspect916, aspect12).forEach { it.showGradient = true }

            randomizerButton.setOnClickListener {
                hideExtrasMenu()
                setUiState(UiState.RANDOMIZER)
            }
            b.randomizer.run {
                randomizeShapeSwitch.setOnClickListener {
                    randomizePositionSwitch.apply {
                        alpha = if (randomizeShapeSwitch.isChecked) 1f else 0.35f
                        isClickable = randomizeShapeSwitch.isChecked
                        if (!randomizeShapeSwitch.isChecked) isChecked = false
                    }
                }
            }

        }
        b.extrasMenuButton.setOnClickListener {
            b.extrasMenu.run {
                when {
                    root.isGone -> showExtrasMenu()
                    optionsLayout.isVisible -> hideExtrasMenu()
                    resolutionLayout.isVisible -> {
                        resolutionLayout.hide()
                        optionsLayout.show()
                    }
                    aspectRatioLayout.isVisible -> {
                        aspectRatioLayout.hide()
                        optionsLayout.show()
                    }
                }
            }
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
                        saveImage()
                    }
                } else saveImage()

            }

        }


        supportFragmentManager.beginTransaction()
            .add(R.id.settingsFragmentContainer, settingsFragment, SETTINGS_FRAGMENT_TAG)
            // .add(R.id.videoFragmentContainer, videoFragment, VIDEO_FRAGMENT_TAG)
            .commit()
        b.settingsFragmentContainer.hide()
        b.videoFragmentContainer.hide()



        b.overlay.bringToFront()
        b.settingsFragmentContainer.bringToFront()
        b.settingsFragmentContainer.hide()


        // need both?
        hideKeyboard(window, b.root)
        // window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        queryTutorialOption()
        crashlytics().setCustomKey(CrashKey.ACT_MAIN_CREATED, true)

        Log.d("MAIN", "onCreate took ${currentTimeMs() - onCreateStartTime} ms")

    }

    override fun onStart() {
        Log.v("MAIN", "onStart")
        super.onStart()
    }
    override fun onResume() {

        Log.v("MAIN", "onResume")
        super.onResume()
        fsv.onResume()
        if (listAdaptersInitialized) lifecycleScope.launch { queryPurchases() }
        // updateSystemBarsVisibility()

    }
    override fun onPause() {

        Log.v("MAIN", "activity paused ...")

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
        edit.run {
            putBoolean  ( PreferenceKey.GOLD_ENABLED_DIALOG_SHOWN,  goldEnabledDialogShown)
            putBoolean  ( PreferenceKey.GOLD_PENDING_DIALOG_SHOWN,  goldPendingDialogShown)
            putBoolean  ( PreferenceKey.SHOW_EPILEPSY_DIALOG,       showEpilepsyDialog)
            putBoolean  ( PreferenceKey.SHOW_TUTORIAL_OPTION,       showTutorialOption)
            putInt      ( PreferenceKey.RESOLUTION,                 min(Resolution.foregrounds.indexOf(sc.resolution), if (sc.goldEnabled) Resolution.foregrounds.indexOf(Resolution.SCREEN) else Resolution.foregrounds.indexOf(Resolution.R1080)))
            putInt      ( PreferenceKey.TARGET_FRAMERATE,           sc.targetFramerate)
            putInt      ( PreferenceKey.ASPECT_RATIO,               AspectRatio.all.indexOf(sc.aspectRatio))
            putBoolean  ( PreferenceKey.CONTINUOUS_RENDER,          sc.continuousPosRender)
            putBoolean  ( PreferenceKey.RENDER_BACKGROUND,          sc.renderBackground)
            putBoolean  ( PreferenceKey.RESTRICT_PARAMS,            sc.restrictParams)
            putBoolean  ( PreferenceKey.FIT_TO_VIEWPORT,            sc.fitToViewport)
            putBoolean  ( PreferenceKey.HIDE_NAV_BAR,               sc.hideSystemBars)
            putInt      ( PreferenceKey.BUTTON_ALIGNMENT,           sc.buttonAlignment.ordinal)
            putInt      ( PreferenceKey.COLOR_LIST_VIEW_TYPE,       sc.colorListViewType.ordinal)
            putInt      ( PreferenceKey.SHAPE_LIST_VIEW_TYPE,       sc.shapeListViewType.ordinal)
            putInt      ( PreferenceKey.TEXTURE_LIST_VIEW_TYPE,     sc.textureListViewType.ordinal)
            putInt      ( PreferenceKey.BOOKMARK_LIST_VIEW_TYPE,    sc.bookmarkListViewType.ordinal)
            putBoolean  ( PreferenceKey.AUTOFIT_COLOR_RANGE,        sc.autofitColorRange)
            putInt      ( PreferenceKey.VERSION_CODE_TAG,           BuildConfig.VERSION_CODE)
            putBoolean  ( PreferenceKey.USE_ALTERNATE_SPLIT,        sc.useAlternateSplit)
            putBoolean  ( PreferenceKey.ALLOW_SLOW_DUALFLOAT,       sc.allowSlowDualfloat)
            putInt      ( PreferenceKey.CHUNK_PROFILE,              sc.chunkProfile.ordinal)
            putBoolean  ( PreferenceKey.ADVANCED_SETTINGS,          sc.advancedSettingsEnabled)
            putBoolean  ( PreferenceKey.ULTRA_HIGH_RES,             sc.ultraHighResolutions)
            putInt      ( PreferenceKey.PALETTE,                    f.palette.id)
            putInt      ( PreferenceKey.ACCENT_COLOR1,              f.color.fillColor)
            putInt      ( PreferenceKey.ACCENT_COLOR2,              f.color.outlineColor)
        }

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
        edit.apply()

        super.onPause()
        fsv.onPause()

    }
    override fun onBackPressed() {
        // super.onBackPressed()
//        when (uiState) {
//            UiState.MINIMIZED -> {}
//            UiState.PARAM_MENU -> {
//                uiState = UiState.MINIMIZED
//            }
//            UiState.PARAM_EDIT -> {
//                uiState = UiState.PARAM_MENU
//            }
//        }
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        Log.d("MAIN", "window focus changed")
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
                    saveImage()
                } else {
                    showMessage(resources.getString(R.string.msg_save_failed))
                }
                return
            }
            else -> {}
        }
    }

    private fun computeWindowSizeClasses() {

        val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)

        val widthDp = metrics.bounds.width() / resources.displayMetrics.density
        val widthWindowSizeClass = when {
            widthDp < 600f -> WindowSizeClass.COMPACT
            widthDp < 840f -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }

        val heightDp = metrics.bounds.height() /
                resources.displayMetrics.density
        val heightWindowSizeClass = when {
            heightDp < 480f -> WindowSizeClass.COMPACT
            heightDp < 900f -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }

        Log.d("MAIN", "window size class:  {width: ${widthWindowSizeClass}}  {height: $heightWindowSizeClass}")

        // Use widthWindowSizeClass and heightWindowSizeClass
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
//        when (newConfig.orientation) {
//            Configuration.ORIENTATION_PORTRAIT -> {}
//            Configuration.ORIENTATION_LANDSCAPE -> {}
//        }
    }

    private fun queryTutorialOption() {

        if (showTutorialOption) {

            tutw = TutorialWelcomeBinding.inflate(layoutInflater)

            val dialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setView(tutw.root)
                .create()

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
//                b.highlightWindow.hide()
                dialog.dismiss()
                queryLoadPreviousFractalDialog()
            }
            tutw.tutorialStartButton.setOnClickListener {
                dialog.dismiss()
//                Handler(Looper.getMainLooper()).postDelayed({ startTutorial() }, BUTTON_CLICK_DELAY_MED)
            }

        } else queryLoadPreviousFractalDialog()

    }
    private fun queryLoadPreviousFractalDialog() {
        if (previousFractalCreated) {

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
//                    b.editModeButtons.getTabAt(EditMode.POSITION).select()
                }
                .create()
                .showImmersive(b.baseLayout)

        } else {
//            b.editModeButtons.getTabAt(EditMode.POSITION).select()
            activityInitialized = true
        }
    }
    private fun queryChangelog(fromSettings: Boolean = false) {

        val storedVersion = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE).getInt(PreferenceKey.VERSION_CODE_TAG, 0)
        val currentVersion = BuildConfig.VERSION_CODE
        if (BuildConfig.DEV_VERSION || fromSettings || storedVersion != currentVersion) {

            val builder: ChangelogBuilder = ChangelogBuilder()
                .withTitle("")
                .withUseBulletList(true)
                .withMinVersionToShow(42)

            builder.buildAndShowDialog(this, false)

        }

    }





    /* LISTS */

    private fun showList(list: ViewGroup) {
        currentList = list
        list.show()
    }

    private fun hideList() {
        currentList?.hide()
    }

    private fun loadCustomItems() {
        lifecycleScope.launch {

            val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
            val usResources = getLocalizedResources(applicationContext, Locale.US)
            val t = currentTimeMs()

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

            // load custom palettes
            db.colorPaletteDao().apply {
                getAll().forEach {
                    Palette.custom.add(0, Palette(
                        name = if (it.name == "") resources.getString(R.string.error) else it.name,
                        id = it.id,
                        hasCustomId = true,
                        colors = ArrayList(
                            arrayListOf(
                                it.c1, it.c2, it.c3,
                                it.c4, it.c5, it.c6,
                                it.c7, it.c8, it.c9,
                                it.c10, it.c11, it.c12
                            ).slice(0 until it.size)
                        ),
                        isFavorite = it.starred
                    ))
                    Palette.custom[0].initialize(resources)
                    Log.v("MAIN", "custom palette ${Palette.custom[0].name}, id: ${Palette.custom[0].id}")
                }
            }
            Palette.all.addAll(0, Palette.custom)
            f.palette = Palette.all.find { it.id == sp.getInt(PreferenceKey.PALETTE, Palette.eye.id) } ?: Palette.eye
            updateGradient()



            // load default shapes
            Shape.default.forEach {
                it.initialize(resources)
                it.isFavorite = sp.getBoolean(it.generateStarredKey(usResources), false)
            }

            // load custom shapes
            db.shapeDao().apply {
                getAll().forEach {
                    Shape.custom.add(0, Shape(
                        name = if (it.name == "") resources.getString(R.string.error) else it.name,
                        id = it.id,
                        hasCustomId = true,
                        latex = it.latex,
                        loop = "customshape_loop(z1, c)",
                        conditional = it.conditional,
                        positions = PositionList(
                            main = Position(
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
                        params = Shape.ParamSet(
                            seed = Complex(it.xSeed, it.ySeed),
                            julia = Complex(it.xJulia, it.yJulia),
                            detail = it.maxIter.toDouble()
                        ),
                        radius = it.bailoutRadius,
                        isConvergent = it.isConvergent,
                        hasDualFloat = it.hasDualFloat,
                        customLoopSingle = it.loopSF,
                        customLoopDual = it.loopDF,
                        isFavorite = it.isFavorite
                    ))
                    Shape.custom[0].initialize(resources)
                    Log.v("MAIN", "custom shape ${Shape.custom[0].name}, id: ${Shape.custom[0].id}")
                }
            }
            Shape.all.addAll(0, Shape.custom)



            // load default textures
            Texture.all.forEach { texture ->
                texture.initialize(resources)
                texture.isFavorite = sp.getBoolean(texture.generateStarredKey(usResources), false)
                if (texture == Texture.stripeAvg) {
                    Log.d("MAIN", "stripe average -- key: ${texture.generateStarredKey(usResources)}, favorite: ${texture.isFavorite}")
                }
            }



            // load default bookmarks
            Fractal.defaultList.forEach {
                it.initialize(resources)
                it.isFavorite = sp.getBoolean(it.generateStarredKey(usResources), false)
            }

            // load custom bookmarks
            db.fractalDao().apply {

                if (!previousFractalCreated || previousFractalId == -1) {
                    Log.v("MAIN", "previousFractal not created")
                    Fractal.previous.customId = insert(Fractal.previous.toDatabaseEntity()).toInt()
                    previousFractalId = Fractal.previous.customId
                    sp.edit()
                        .putInt(PreferenceKey.PREV_FRACTAL_ID, Fractal.previous.customId)
                        .putBoolean(PreferenceKey.PREV_FRACTAL_CREATED, true)
                        .apply()
                }

                getAll().forEach { e ->

                    val shape = Shape.all.find { it.id == e.shapeId } ?: Shape.mandelbrot
                    Log.v("MAIN", "shape: ${shape.name}")
                    val shapeParams = shape.params.clone().apply {
                        reset()
                        listOf(e.p1, e.p2, e.p3, e.p4).forEachIndexed { i, p ->
                            if (p != null) list.getOrNull(i)?.setFrom(if (p.isComplex) ComplexParam(p) else RealParam(p))
                        }
                        if (e.julia != null) julia.setFrom(ComplexParam(e.julia))
                        if (e.seed != null)  seed.setFrom(ComplexParam(e.seed))
                        detail.setFrom(RealParam(e.maxIter.toDouble()))
                    }

                    val texture = Texture.all.find { it.id == e.textureId } ?: Texture.escapeSmooth
                    val textureParams = texture.params.clone().apply {
                        reset()
                        listOf(e.q1, e.q2, e.q3, e.q4).forEachIndexed { i, p ->
                            if (p != null) list.getOrNull(i)?.setFrom(if (p.isComplex) ComplexParam(p) else RealParam(p))
                        }
                        radius.setFrom(RealParam(e.radius.toDouble()))
                    }

                    val thumbnail = try {
                        val inputStream = openFileInput(e.thumbnailPath)
                        BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }).let { bmp ->
                            inputStream?.close()
                            bmp
                        }
                    } catch (e: Exception) {
                        Log.e("MAIN", "thumnail path invalid")
                        BitmapFactory.decodeResource(resources, R.drawable.mandelbrot_icon)
                    }

                    val newBookmark = Fractal(

                        name = if (e.name == "") resources.getString(R.string.error) else e.name,
                        isFavorite = e.isFavorite,
                        thumbnailPath = e.thumbnailPath,
                        thumbnail = thumbnail,
                        customId = e.id,
                        goldFeature = false,

                        shapeId = e.shapeId,
                        juliaMode = e.juliaMode,
                        shapeParams = shapeParams,
                        position = if (e.position != null) Position(e.position) else null,

                        textureId = e.textureId,
                        textureRegion = TextureRegion.values()[e.textureMode],
                        textureMin = e.textureMin,
                        textureMax = e.textureMax,
                        textureParams = textureParams,
                        imagePath = e.imagePath,
                        imageId = Texture.defaultImages.getOrNull(e.imageId) ?: -1,

                        paletteId = e.paletteId,
                        color = ColorConfig(
                            frequency   = e.frequency.toDouble(),
                            phase       = e.phase.toDouble(),
                            density     = e.density.toDouble(),
                            fillColor     = e.solidFillColor,
                            outlineColor     = e.accent2
                        )

                    )
                    if (e.id == previousFractalId) {
                        Fractal.previous = newBookmark
                    } else {
                        Fractal.bookmarks.add(0, newBookmark)
                        Log.v("MAIN", "Bookmark -- id: ${newBookmark.customId}, name: ${newBookmark.name}, shape: ${shape.name}")
                    }


                }

            }
            Fractal.all.addAll(0, Fractal.bookmarks)



            fileList().filter { it.startsWith(TEX_IM_PREFIX) }.let { Texture.customImages.addAll(it.reversed()) }
            ShapeKeyListHeader.all.forEach { it.initialize(resources) }



            // populate lists
            populatePaletteList()
            populateShapeList()
            populateTextureList()
            populateBookmarkList()
            populateTextureImageList()
            listAdaptersInitialized = true
            queryPurchases()

            Log.d("MAIN", "database operations took ${currentTimeMs() - t} ms")

        }
    }

    private fun populateBookmarkList() {

        var firstBookmarkSelection = true

        bookmarkLists = ItemListManager(
            b.bookmarkList,
            bookmarkListTypeSelector,
            Fractal.defaultList,
            ArrayList(Fractal.bookmarks),
            this
        )
        bookmarkLists.setOnItemActionListener(object : NewListAdapter.OnItemActionListener<Fractal> {
            override fun onClick(t: Fractal): Boolean {

                if (t.goldFeature && !sc.goldEnabled) showUpgradeScreen()
                else {

                    if (fsv.r.isRendering) fsv.r.interruptRender = true
                    compatTexturesChanged = t.shape.compatTextures != Fractal.tempBookmark3.shape.compatTextures

                    fsv.r.checkThresholdCross {

                        // restore state
                        if (firstBookmarkSelection) firstBookmarkSelection = false
                        else f.load(Fractal.tempBookmark2, fsv)

                        f.preload(t)
                        Fractal.tempBookmark2 = f.bookmark(fsv)
                        f.load(t, fsv)
                        updateCrashKeys()

                    }

                    fsv.r.validAuxiliary = false
                    fsv.r.renderShaderChanged = true
                    if (sc.autofitColorRange && f.color.density == 0.0 && !f.texture.hasRawOutput) {
                        fsv.r.autofitColorSelected = true
                        fsv.r.calcNewTextureSpan = true
                    }
                    fsv.r.resetContinuousSize = true
                    fsv.renderContinuousDiscrete()

                }

                return true

            }
            override fun onEdit(t: Fractal) {
                Fractal.tempBookmark1 = t
                showBookmarkDialog(t, edit = true)
            }
            override fun onDelete(t: Fractal) {
                AlertDialog.Builder(this@MainActivity, R.style.AlertDialogCustom)
                    .setTitle("${resources.getString(R.string.delete)} ${t.name}?")
                    .setIcon(R.drawable.warning)
                    .setPositiveButton(android.R.string.ok) { dialog, whichButton ->

                        bookmarkLists.deleteItem(t)
                        val deleteId = t.customId

                        lifecycleScope.launch {
                            db.fractalDao().apply {
                                delete(findById(deleteId))
                            }
                        }

                        deleteFile(t.thumbnailPath)

                        Fractal.all.remove(t)
                        Fractal.bookmarks.remove(t)

                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .showImmersive(b.root)

            }
            override fun onDuplicate(t: Fractal) {}
            override fun onToggleFavorite(t: Fractal, isSelected: Boolean) {
                bookmarkLists.toggleFavorite(t, isSelected)
                if (t.isFavorite) showMessage(R.string.msg_new_favorite)
            }
        })

    }
    private fun populatePaletteList() {

        val onEditConfirm = { item: Palette ->

            setUiState(UiState.CUSTOM_PALETTE)
            fsv.r.renderProfile = RenderProfile.DISCRETE

            f.palette = item
            f.palette.edit()
            fsv.requestRender()

            customColorDragAdapter.updateColors(f.palette.colors)

        }

        paletteLists = ItemListManager(
            b.paletteList,
            paletteListTypeSelector,
            Palette.default,
            ArrayList(Palette.custom),
            this  // context
        )
        paletteLists.setOnItemActionListener(object : NewListAdapter.OnItemActionListener<Palette> {
            override fun onClick(t: Palette): Boolean {

                if (t != f.palette) {
                    f.palette = t
                    paletteLists.setSelection(t)
                    updateCrashKeys()
                    fsv.requestRender()
                }
                return true

            }
            override fun onEdit(t: Palette) {
                if (Fractal.bookmarks.any { bookmark -> bookmark.palette == t }) {
                    AlertDialog.Builder(this@MainActivity, R.style.AlertDialogCustom)
                        .setIcon(R.drawable.warning)
                        .setTitle("${resources.getString(R.string.edit)} ${t.name}?")
                        .setMessage(resources.getString(R.string.edit_palette_bookmark_warning).format(
                            Fractal.bookmarks.count { bookmark -> bookmark.palette == t }
                        ))
                        .setPositiveButton(R.string.edit) { _, _ -> onEditConfirm(t) }
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .showImmersive(b.root)
                } else onEditConfirm(t)
            }
            override fun onDelete(t: Palette) {
                Log.v("MAIN", "delete -- palette {${t.name}}")
                AlertDialog.Builder(this@MainActivity, R.style.AlertDialogCustom)
                    .setTitle("${resources.getString(R.string.delete)} ${t.name}?")
                    .setMessage(resources.getString(R.string.delete_palette_bookmark_warning).format(
                        Fractal.bookmarks.count { bookmark -> bookmark.palette == t }
                    ))
                    .setIcon(R.drawable.warning)
                    .setPositiveButton(android.R.string.ok) { _, _ ->

                        paletteLists.deleteItem(t)

                        lifecycleScope.launch {
                            Fractal.bookmarks.filter { it.palette == t }.forEach {
                                it.palette = Palette.eye
                                db.fractalDao().update(it.customId, it.palette.id, 0)
                            }

                            // item.t.release()
                            val deleteId = t.id
                            db.colorPaletteDao().apply { delete(findById(deleteId)) }

                        }

                        f.palette = paletteLists.getSelectedItem() ?: Palette.eye
                        Palette.all.remove(t)
                        Palette.custom.remove(t)
                        if (!sc.goldEnabled && Palette.custom.size < Palette.MAX_CUSTOM_PALETTES_FREE) {
                            b.actionNew.showGradient = false
                        }
                        fsv.requestRender()

                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .showImmersive(b.root)
            }
            override fun onDuplicate(t: Palette) {
                if (!sc.goldEnabled) showUpgradeScreen()
                else {

                    setUiState(UiState.CUSTOM_PALETTE)
                    fsv.r.renderProfile = RenderProfile.DISCRETE

                    f.palette = t.clone(resources)

                    f.palette = f.palette
                    fsv.requestRender()

                    customColorDragAdapter.updateColors(f.palette.colors)
//                customColorDragAdapter.apply { linkColor(0, itemList[0].second) }
//                b.f.paletteCreator.f.paletteName.setText(f.palette.name)

                }
            }
            override fun onToggleFavorite(t: Palette, isSelected: Boolean) {
                paletteLists.toggleFavorite(t, isSelected)
                if (t.isFavorite) showMessage(R.string.msg_new_favorite)
            }
        })
        paletteLists.setSelection(f.palette, fromUser = false)

    }
    private fun populateShapeList() {

        val onEditConfirm = { item: Shape ->

            setUiState(UiState.CUSTOM_SHAPE)
            sc.editMode = EditMode.POSITION
            fsv.r.renderProfile = RenderProfile.DISCRETE

            fsv.r.checkThresholdCross {
                f.shape = item
                f.shape.edit()
                // f.texture = if (f.shape.isConvergent) Texture.converge else Texture.escapeSmooth
                // f.shape.reset()
                f.shape.position.reset()
            }

            b.customShapeCreator.run {
                shapeMathQuill.setLatex(f.shape.latex)
                shapeMathQuill.setCustomLoop(f.shape.latex, f.shape)
            }

            fsv.r.renderShaderChanged = true
            fsv.r.renderToTex = true
            fsv.requestRender()

        }

        shapeLists = ItemListManager(
            b.shapeList,
            shapeListTypeSelector,
            ArrayList(Shape.default),
            ArrayList(Shape.custom),
            this
        )
        shapeLists.setOnItemActionListener(object : NewListAdapter.OnItemActionListener<Shape> {
            override fun onClick(t: Shape): Boolean {

                val newShape = t
                if (newShape != f.shape) {

                    if (newShape.goldFeature && !sc.goldEnabled) showUpgradeScreen()
                    else {

                        // reset texture if not compatible with new shape
                        if (!newShape.compatTextures.contains(f.texture)) {
                            f.texture = if (newShape == Shape.kleinian) Texture.escape else { if (newShape.isConvergent) Texture.convergeSmooth else Texture.escapeSmooth }
                            textureChanged = true
                        } else {
                            textureChanged = false
                        }
                        compatTexturesChanged = newShape.compatTextures != prevShape.compatTextures

                        fsv.r.checkThresholdCross { f.shape = newShape }
                        f.updateRadius()

                        fsv.r.validAuxiliary = false
                        fsv.r.resetContinuousSize = true
                        fsv.r.renderShaderChanged = true
                        fsv.renderContinuousDiscrete()

                        updateShapeDisplayValues()
                        updatePositionDisplay()
                        updateCrashKeys()

                    }

                }
                return true

            }
            override fun onEdit(t: Shape) {
                
                if (Fractal.bookmarks.any { bookmark -> bookmark.shape == t }) {
                    AlertDialog.Builder(this@MainActivity, R.style.AlertDialogCustom)
                        .setIcon(R.drawable.warning)
                        .setTitle("${resources.getString(R.string.edit)} ${t.name}?")
                        .setMessage(resources.getString(R.string.edit_shape_bookmark_warning).format(
                            Fractal.bookmarks.count { bookmark -> bookmark.shape == t }
                        ))
                        .setPositiveButton(R.string.edit) { _, _ -> onEditConfirm(t) }
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .showImmersive(b.root)
                }
                else onEditConfirm(t)
                
            }
            override fun onDelete(t: Shape) {

                AlertDialog.Builder(this@MainActivity, R.style.AlertDialogCustom)
                    .setTitle("${resources.getString(R.string.delete)} ${t.name}?")
                    .setMessage(resources.getString(R.string.delete_shape_bookmark_warning).format(
                        Fractal.bookmarks.count { bookmark -> bookmark.shape == t }
                    ))
                    .setIcon(R.drawable.warning)
                    .setPositiveButton(R.string.delete) { dialog, whichButton ->

                        shapeLists.deleteItem(t)
                        lifecycleScope.launch {

                            Fractal.bookmarks.filter { bookmark -> bookmark.shape == t }.forEach { bookmark ->
                                File(filesDir.path + bookmark.thumbnailPath).delete()
                                db.fractalDao().apply {
                                    delete(findById(bookmark.customId))
                                }
                            }

                            // item.t.release()
                            val deleteId = t.id
                            db.shapeDao().run { delete(findById(deleteId)) }

                        }

                        f.shape = shapeLists.getSelectedItem() ?: Shape.mandelbrot
                        Shape.all.remove(t)
                        Shape.custom.remove(t)

                        fsv.r.renderShaderChanged = true
                        fsv.r.renderToTex = true
                        fsv.requestRender()

                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .showImmersive(b.root)
                
            }
            override fun onDuplicate(t: Shape) {

                if (!sc.goldEnabled) showUpgradeScreen()
                else {

                    setUiState(UiState.CUSTOM_SHAPE)
                    sc.editMode = EditMode.POSITION
                    fsv.r.renderProfile = RenderProfile.DISCRETE

                    f.shape = t.clone(resources)
                    f.texture = Texture.escapeSmooth

                    b.customShapeCreator.apply {
                        shapeMathQuill.setLatex(f.shape.latex)
                        shapeMathQuill.setCustomLoop(f.shape.latex, f.shape)
                    }

                    fsv.r.renderShaderChanged = true
                    fsv.r.renderToTex = true
                    fsv.requestRender()

                }
                
            }
            override fun onToggleFavorite(t: Shape, isSelected: Boolean) {
                shapeLists.toggleFavorite(t, isSelected)
                if (t.isFavorite) showMessage(R.string.msg_new_favorite)
            }
        })

    }
    private fun populateTextureList() {

        textureLists = ItemListManager(
            b.textureList,
            textureListTypeSelector,
            ArrayList(f.shape.compatTextures),
            arrayListOf(),
            this
        )
        textureLists.setOnItemActionListener(object : NewListAdapter.OnItemActionListener<Texture> {
            override fun onClick(t: Texture): Boolean {

                if (t != f.texture) {

                    Log.v("MAIN", "${t.name} selected")
                    if (fsv.r.isRendering) fsv.r.interruptRender = true

                    if (t.goldFeature && !sc.goldEnabled) showUpgradeScreen()
                    else {

                        if (t.hasRawOutput != f.texture.hasRawOutput) fsv.r.loadTextureImage = true

                        if (t != prevTexture) textureChanged = true

                        if (sc.autofitColorRange) fsv.r.calcNewTextureSpan = true


                        if (fsv.r.isRendering) {

                            fsv.r.renderFinishedListener = object : RenderFinishedListener {
                                override fun onRenderFinished(buffer: ByteArray?) {
                                    f.texture = t
                                    f.updateRadius()
                                    updateCrashKeys()
                                    if (fsv.r.renderProfile == RenderProfile.TEXTURE_THUMB) {
                                        fsv.r.renderProfile = RenderProfile.DISCRETE
                                    }
                                    fsv.r.validAuxiliary = false
                                    fsv.r.resetContinuousSize = true
                                    fsv.r.renderShaderChanged = true
                                    fsv.renderContinuousDiscrete()
                                }
                            }

                        } else {

                            f.texture = t
                            f.updateRadius()
                            updateCrashKeys()
                            if (fsv.r.renderProfile == RenderProfile.TEXTURE_THUMB) {
                                fsv.r.renderProfile = RenderProfile.DISCRETE
                            }
                            fsv.r.validAuxiliary = false
                            fsv.r.resetContinuousSize = true
                            fsv.r.renderShaderChanged = true
                            fsv.renderContinuousDiscrete()

                        }

                    }
                }
                return true
                
            }
            override fun onEdit(t: Texture) {}
            override fun onDelete(t: Texture) {}
            override fun onDuplicate(t: Texture) {}
            override fun onToggleFavorite(t: Texture, isSelected: Boolean) {
                textureLists.toggleFavorite(t, isSelected)
                if (t.isFavorite) showMessage(R.string.msg_new_favorite)
            }
        })
        b.textureList.customListButton.hide()

    }

    private fun populateTextureImageList() {

        val textureImageItems = mutableListOf<TextureImageListItem>()
        Texture.defaultImages.forEach  { textureImageItems.add(TextureImageListItem(id = it))   }
        Texture.customImages.forEach { textureImageItems.add(TextureImageListItem(path = it)) }
        textureImageItems.add(TextureImageListItem(id = R.drawable.texture_image_add))
        textureImageListAdapter = FlexibleAdapter(textureImageItems)
        textureImageListAdapter.mode = SelectableAdapter.Mode.SINGLE
        b.textureImageDisplay.textureImageList.adapter = textureImageListAdapter
        textureImageListAdapter.mItemClickListener = FlexibleAdapter.OnItemClickListener { view, position ->

            val item = textureImageListAdapter.getItem(position)
            textureImageListAdapter.toggleSelection(position)

            if (item?.id == R.drawable.texture_image_add) {
                if (!sc.goldEnabled) showUpgradeScreen()
                else resultContract.launch(ActivityResultContracts.GetContent().createIntent(this, "image/*"))
                false
            } else {
                if (item != null) {
                    if (item.path != "") {
                        f.imagePath = item.path
                        f.imageId = -1
                    }
                    else if (item.id != -1) {
                        f.imagePath = ""
                        f.imageId = item.id
                    }
                }
                fsv.r.validAuxiliary = false
                fsv.r.loadTextureImage = true
                fsv.renderContinuousDiscrete()
                true
            }

        }
        textureImageListAdapter.mItemLongClickListener = FlexibleAdapter.OnItemLongClickListener { position ->

            val item = textureImageListAdapter.getItem(position)

            if (item?.id == -1) {
                AlertDialog.Builder(this, R.style.AlertDialogCustom)
                        .setTitle("${resources.getString(R.string.remove)} ${resources.getString(R.string.image)}?")
                        .setIcon(R.drawable.color_remove)
                        .setMessage(resources.getString(R.string.remove_texture_image_bookmark_warning).format(Fractal.bookmarks.count { it.imagePath == item.path }))
                        .setPositiveButton(android.R.string.ok) { dialog, which ->

                            deleteFile(item.path)
                            lifecycleScope.launch {
                                db.fractalDao().apply {
                                    Fractal.bookmarks.forEach {
                                        if (it.imagePath == item.path) {
                                            it.imagePath = ""
                                            it.imageId = R.drawable.flower
                                            updateImage(it.customId, it.imagePath, Texture.defaultImages.indexOf(it.imageId))
                                        }
                                    }
                                }
                            }

                            textureImageListAdapter.removeItem(position)

                            if (textureImageListAdapter.selectedPositions.contains(position)) {
                                textureImageListAdapter.apply {
                                    toggleSelection(0)
                                    notifyItemChanged(0)
                                }
                                f.imagePath = ""
                                f.imageId = Texture.defaultImages[0]
                                fsv.r.loadTextureImage = true
                                fsv.r.renderToTex = true
                                fsv.requestRender()
                            }

                        }
                        .setNegativeButton(android.R.string.cancel) { dialog, which ->

                        }
                        .show()
            }

        }

    }
    private fun confirmTextureImageResolution(sampleSize: Int, uri: Uri) {

        Log.v("TEXTURE", "sampleSize: $sampleSize")
        val inputStream = contentResolver?.openInputStream(uri)
        val bmp = BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = sampleSize
        })
        inputStream?.close()
        Log.v("TEXTURE", "width: ${bmp?.width}, height: ${bmp?.height}")

        // make local copy of image
        val path = "$TEX_IM_PREFIX${Texture.CUSTOM_IMAGE_COUNT}.png"
        val fos = openFileOutput(path, Context.MODE_PRIVATE)
        bmp?.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.close()

        Texture.CUSTOM_IMAGE_COUNT++
        val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        val edit = sp.edit()
        edit.putInt(PreferenceKey.TEX_IMAGE_COUNT, Texture.CUSTOM_IMAGE_COUNT)
        edit.apply()

        textureImageListAdapter.apply {
            val newPos = itemCount - 1
            if (addItem(newPos, TextureImageListItem(path = path))) {
                toggleSelection(newPos)
                notifyItemChanged(newPos)
            }
        }

        f.imagePath = path
        f.imageId = -1
        fsv.r.validAuxiliary = false
        fsv.r.loadTextureImage = true
        fsv.renderContinuousDiscrete()

    }





    /* USER ITERFACE */

    fun setUiState(newState: UiState, useless: Boolean = false) {
        if (newState != uiState) {

            val oldState = uiState
            uiState = newState

            // on exit
            when (oldState) {
                UiState.HOME -> {
                    b.utilityLayout.hide()
                    b.paramLayout.hide()
                }
                UiState.EDITMODE_LIST -> {
                    b.actionButtons.hide()
                    b.actionNew.hide()
                    hideList()
                    showMainUi()
                }
                UiState.BOOKMARK_LIST -> {
                    b.bookmarkList.root.hide()
                    b.actionButtons.hide()
                    b.actionCancel.hide()
                    showMainUi()
                }
                UiState.CUSTOM_ACCENT -> {
//                    b.paramSelectorLayout.show()
                    b.customAccentDoneButton.hide()
                }
                UiState.CUSTOM_PALETTE -> {
                    showMainUi()
                    b.customPaletteCreator.root.hide()
                    b.actionButtons.hide()
                    b.actionCancel.hide()
                }
                UiState.CUSTOM_SHAPE -> {
                    showMainUi()
                    b.customShapeCreator.root.hide()
                    b.actionButtons.hide()
                    b.actionCancel.hide()
                    b.actionDone.showGradient = false
                }
                UiState.VIDEO_CONFIG -> {
                    b.videoConfigWindow.hide()
                }
                UiState.VIDEO_PROGRESS -> {
                    b.videoProgressWindow.hide()
                }
                UiState.RANDOMIZER -> {
                    showMainUi()
                    b.randomizer.root.hide()
                    b.actionButtons.hide()
                    b.actionRandomize.hide()
                }
            }

            // on enter
            when (newState) {
                UiState.HOME -> {
                    showMainUi()
                    b.paramLayout.show()
                    b.utilityLayout.show()
                }
                UiState.EDITMODE_LIST -> {
                    hideMainUi()
                    b.actionButtons.show()
                    when (sc.editMode) {
                        EditMode.COLOR -> {
                            b.actionNew.show()
                            fsv.r.renderProfile = RenderProfile.COLOR_THUMB
                            if (!sc.goldEnabled && Palette.custom.size == Palette.MAX_CUSTOM_PALETTES_FREE)  {
                                b.actionNew.showGradient = true
                            }
                        }
                        EditMode.TEXTURE -> {

                        }
                        else -> {
                            b.actionNew.show()
                        }
                    }
                }
                UiState.BOOKMARK_LIST -> {
                    hideMainUi()
                    b.actionButtons.show()
                    b.actionCancel.show()
                    b.bookmarkList.root.show()
                }
                UiState.CUSTOM_ACCENT -> {
//                    hideHomeUi(except = listOf(b.paramMenuLayout))
//                    b.paramSelectorLayout.hide()
                    b.customAccentDoneButton.show()
                }
                UiState.CUSTOM_PALETTE -> {
                    hideMainUi()
                    b.customPaletteCreator.root.show()
                    b.actionButtons.show()
                    b.actionNew.hide()
                    b.actionCancel.show()
                }
                UiState.CUSTOM_SHAPE -> {
                    hideMainUi()
                    b.customShapeCreator.root.show()
                    b.actionButtons.show()
                    b.actionNew.hide()
                    b.actionCancel.show()
                    if (!sc.goldEnabled) b.actionDone.showGradient = true
                }
                UiState.VIDEO_CONFIG -> {
                    if (oldState == UiState.HOME) hideHomeUi()
                    b.videoConfigWindow.show()
                }
                UiState.VIDEO_PROGRESS -> {
                    b.videoProgressWindow.show()
                }
                UiState.RANDOMIZER -> {
                    hideMainUi()
                    b.randomizer.root.show()
                    b.actionButtons.show()
                    b.actionRandomize.show()
                }
            }

        }
    }
    private fun hideSystemBars() {
        hideSystemBars(window, b.root)
    }
    private fun showSystemBars() {
        showSystemBars(window, b.root)
    }
    override fun updateSystemBarsVisibility() {
        if (sc.hideSystemBars) hideSystemBars() else showSystemBars()
    }

    fun updateLayouts() {

        updatePositionDisplay()

        updateColorLayout()
        updateColorDisplay()

        updateShapeLayout()
        updateTextureLayout()
        updateShapeDisplayValues()

    }

    private fun updatePositionDisplay() {
        b.positionParamDisplay.run {
            value.setText(activePositionParam.toDisplayFormat(f.shape.position))
            if (activePositionParam == PositionParam.ROTATION) rotationLock.show() else rotationLock.hide()
        }
    }

    private fun updateGradient() {
        b.paletteListButtonGradient.background = f.palette.gradientDrawable
        b.colorParamDisplay.fillColorSelector.paletteSeekBar.setColors(f.palette.colors.toIntArray())
        b.colorParamDisplay.outlineColorSelector.paletteSeekBar.setColors(f.palette.colors.toIntArray())
    }
    private fun updateColorLayout() {
        b.colorParamMenu.outlineColorButton.run { if (f.texture.usesAccent) show() else hide() }
        b.run { colorAutofitButton.isChecked = sc.autofitColorRange }
        if (sc.autofitColorRange) {
            b.colorParamMenu.densityButton.run { if (f.texture.usesDensity) show() else hide() }
        }
        updateGradient()
    }
    fun updateColorDisplay() {
        b.colorParamDisplay.run {
            val p = activeColorParam
            if (p is ColorRealParam) {
                value.setText(p.toDisplayFormat(f.color))
                seekBar.progress = (p.toProgress(f.color)*seekBar.max).toInt()
            }
            else if (p is ColorAccent) {
                // colorSelector.satValueSelector.setColor(p.color())
            }
        }
    }

    private fun updateShapeLayout() {
        b.shapeParamMenu.run {
            val shapeParamButtonList = listOf(
                shapeParamButton1,
                shapeParamButton2,
                shapeParamButton3,
                shapeParamButton4
            )

            if (f.shape.hasCustomId) b.shapeListIcon.setImageBitmap(f.shape.thumbnail)
            else b.shapeListIcon.setImageResource(f.shape.thumbnailId)

            // update parameter display
            shapeParamButtonList.forEach { it.hide() }
            f.shape.params.list.forEachIndexed { index, param ->
                shapeParamButtonList[index].run {

                    if (!param.devFeature || BuildConfig.DEV_VERSION)
//                    showGradient = param.goldFeature && !sc.goldEnabled
                    show()
                    setImageResource(param.iconId)
                    setText(param.nameId)
                }
            }


            // update juliaModeButton

            b.run {
                if (!f.shape.isCustom() && f.shape.juliaModeInit) {
                    juliaModeButton.hide()
                    // juliaDivider.hide()
                } else {
                    juliaModeButton.show()
                    // juliaDivider.show()
                }

                juliaModeButton.showGradient = f.shape != Shape.mandelbrot && !sc.goldEnabled

                juliaModeButton.run {
                    isChecked = f.shape.juliaMode
                    if (isChecked) juliaButton.show() else juliaButton.hide()
                }
            }

            if (f.shape.juliaMode || f.shape.juliaSeed) seedButton.hide() else seedButton.show()

            detailButton.performClick()

        }
    }
    private fun updateTextureLayout() {
        textureChanged = false
        b.textureParamMenu.apply {

            val textureParamButtons = listOf(
                textureParamButton1,
                textureParamButton2,
                textureParamButton3,
                textureParamButton4
            )

            textureParamButtons.forEach { it.hide() }
            f.texture.params.list.forEachIndexed { index, param ->
                textureParamButtons[index].run {
                    show()
                    setImageResource(param.iconId)
                    setText(param.nameId)
                    // showGradient = param.goldFeature && !sc.goldEnabled
                }
            }
            if (f.texture.hasRawOutput) textureImageButton.show()
            else textureImageButton.hide()

            if (f.texture.hasRawOutput) {
                textureImageListAdapter.apply {
                    currentItems.forEachIndexed { i, item ->
                        if ((f.imagePath != "" && f.imagePath == item.path) || (f.imageId != -1 && f.imageId == item.id)) {
                            toggleSelection(i)
                            notifyItemChanged(i)
                        }
                    }
                }
            }

            escapeRadiusButton.performClick()

        }
        if (f.texture.params.list.isNotEmpty()) {
            b.textureParamMenu.run {
                listOf(textureParamButton1, textureParamButton2, textureParamButton3, textureParamButton4).getOrNull(
                    f.texture.params.list.indexOfFirst { param -> sc.goldEnabled || !param.goldFeature }
                )?.performClick()
            }
        }
    }
    private fun onCompatTexturesChanged() {
        compatTexturesChanged = false
        if (listAdaptersInitialized) textureLists.updateDataset(ArrayList(f.shape.compatTextures), arrayListOf())
    }

    private fun bindPositionParameter(p: PositionParam) {
        activePositionParam = p
        b.positionParamDisplay.run {
            label.setText(p.nameId)
            value.setOnEditorActionListener(editListener(null) { w: TextView ->
                val result = w.text.toString().formatToDouble()
                if (result != null) {
                    p.fromDislayFormat(f.shape.position, result)
                    fsv.renderContinuousDiscrete()
                }
                updatePositionDisplay()
            })
        }
        b.run {
            positionSensitivityButton.run { param = p }
            positionDecreaseButton.setImageResource(p.decreaseIconId)
            positionDecreaseButton.setOnClickListener(p.onDecreaseClick)
            positionDecreaseButton.setOnLongClickListener(p.onDecreaseLongClick)
            positionIncreaseButton.setImageResource(p.increaseIconId)
            positionIncreaseButton.setOnClickListener(p.onIncreaseClick)
            positionIncreaseButton.setOnLongClickListener(p.onIncreaseLongClick)
        }
        updatePositionDisplay()
    }
    private fun bindColorParameter(p: ColorParam) {
        activeColorParam = p
        b.colorParamDisplay.run {
            if (p is ColorRealParam) {

                colorValueDisplay.show()
                fillColorSelector.hide()
                outlineColorSelector.hide()
                icon.setImageResource(p.iconId)
                label.setText(p.nameId)
                value.setOnEditorActionListener(editListener(null) { w: TextView ->
                    val result = w.text.toString().formatToDouble()
                    if (result != null) {
                        p.fromDislayFormat(f.color, result)
                        seekBar.progress = (p.toProgress(f.color)*seekBar.max).toInt()
                        fsv.requestRender()
                    }
                    updateColorDisplay()
                })
                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            p.fromProgress(f.color, progress.toDouble() / seekBar.max)
                            value.setText(p.toDisplayFormat(f.color))
                            fsv.requestRender()
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })

            }
            else if (p is ColorAccent) {
                icon.setImageResource(p.iconId)
                colorValueDisplay.hide()
                when (p) {
                    ColorAccent.FILL -> {
                        outlineColorSelector.hide()
                        fillColorSelector.show()
                    }
                    ColorAccent.OUTLINE -> {
                        fillColorSelector.hide()
                        outlineColorSelector.show()
                    }
                }
            }
        }
        updateColorDisplay()
    }
    private fun bindParameter(p: RealParam){
        b.textureShapeParamDisplay.item?.run {
            if (p is ComplexParam) {
                realParamDisplay.hide()
                complexParamDisplay.show()
                complexParamIcon.setImageResource(p.iconId)
                complexParamName.text = p.name
                sensitivity.sensitivityValue.setText(p.sensitivity.toInt().toString())
            } else {
                complexParamDisplay.hide()
                realParamDisplay.show()
                realParamIconMax.setImageResource(p.iconId)
                realParamName.text = p.name
                sensitivity.sensitivityValue.setText(p.sensitivity.toInt().toString())
                seekBar.max = if (p.isDiscrete) p.interval.toInt() else 1000
            }
        }
    }
    private fun bindShapeParameter(p: RealParam) {
        bindParameter(p)
        f.shape.params.active = p
        updateShapeDisplayValues()
    }
    private fun bindTextureParameter(p: RealParam) {
        bindParameter(p)
        f.texture.params.active = p
        updateTextureDisplayValues()
    }

    fun updateShapeDisplayValues() {
        val active = activeParam()
        b.textureShapeParamDisplay.item?.run {
            if (active is ComplexParam) {
                complexParamValue1.setText(active.u.format(7))
                complexParamValue2.setText(active.v.format(7))
                sensitivity.sensitivityValue.setText(active.sensitivity.toInt().toString())
            } else {
                realParamValue.setText(active.toString())
                sensitivity.sensitivityValue.setText(active.sensitivity.toInt().toString())
                seekBar.progress = (active.getProgress()*seekBar.max).toInt()
            }
        }
    }
    private fun updateTextureDisplayValues() {
        updateShapeDisplayValues()
    }
    private fun updateRealParamAdjust() {
        val param = activeParam()
        if (param !is ComplexParam) {
            b.textureShapeParamDisplay.item?.run {
                if (param.isDiscrete) {
                    seekBar.max = param.interval.toInt()
                } else {
                    seekBar.max = 1000
                }
                Log.v("MAIN", "${param.name} -- ${param.getProgress()}, ${seekBar.max}")
                seekBar.progress = (param.getProgress() * seekBar.max.toDouble()).toInt()
            }
        }
    }



    private fun showMainUi() {
        listOf(
            b.extrasMenuButton,
            b.paramLayout,
            b.saveImageButton,
            b.uiToggleButton
        ).forEach { it.show() }

    }
    private fun hideMainUi() {
        b.run {
            // if (paramMenu.isVisible) positionParamsToggle.performClick()
            listOf(
                extrasMenuButton,
                paramLayout,
                uiToggleButton
            ).forEach { it.hide() }
        }
    }

    private fun hideHomeUi(except: List<View> = listOf()) {
        listOf(
            b.uiToggleButton,
            b.extrasMenuButton,
            b.saveImageButton,
            b.paramLayout,
            b.utilityLayout,
        ).minus(except).forEach { it.hide() }
    }

    private fun showExtrasMenu() {
//        if (b.paramDisplay.isVisible) b.paramMenuToggleButton.performClick()
        b.extrasMenu.root.show()
//        b.utilityButtons.hide()
//        b.paramMenuToggleButton.hide()
    }
    private fun hideExtrasMenu() {
        b.extrasMenu.run {
            root.hide()
            resolutionLayout.hide()
            aspectRatioLayout.hide()
            optionsLayout.show()
//            b.utilityButtons.show()
//            b.paramMenuToggleButton.show()
        }
    }

    private fun showActionButtons(vararg buttons: Button) {
        b.actionButtons.children.forEach { v -> v.hide() }
        buttons.forEach { button -> button.show() }
    }

    private fun onAspectRatioChanged() {

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

    }
    private fun updateResolutionText() {

        b.extrasMenu.run {
            val resolutionButtons = arrayListOf(r360, r480, r720, r1080, r1440, r2160, r2880, r3600, r4320, r5040, r5760).apply {
                if (customScreenRes) {
                    val i = Resolution.foregrounds.indexOf(Resolution.SCREEN)
                    add(i, rCustomScreen)
                    rCustomScreen.show()
                }
            }
            resolutionButtons.forEachIndexed { i, button ->

                val dims = Point(Resolution.foregrounds[i].w, Resolution.foregrounds[i].h)
                if (sc.aspectRatio.r > AspectRatio.RATIO_SCREEN.r) dims.x = (dims.y / sc.aspectRatio.r).toInt()
                else if (sc.aspectRatio.r < AspectRatio.RATIO_SCREEN.r) dims.y = (dims.x * sc.aspectRatio.r).toInt()

                button.text = "${dims.x} x ${dims.y}"

            }
        }

    }

    private fun calcNavBarHeight() : Int {

        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        val navBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0

        //Log.v("MAIN ACTIVITY", "navBarHeight: $navBarHeight")
        return navBarHeight

    }
    private fun getDefaultStatusBarHeight() : Int {

        return (STATUS_BAR_HEIGHT * resources.displayMetrics.scaledDensity).toInt()

    }
    private fun calcStatusBarHeight() : Int {

        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0

        //Log.v("MAIN ACTIVITY", "statusBarHeight: $statusBarHeight")
        return statusBarHeight

    }
    private fun calcDeviceHasNotch() : Boolean {

        val hasNotch = calcStatusBarHeight() > getDefaultStatusBarHeight()
        //Log.v("MAIN ACTIVITY", "device has notch: $hasNotch")
        return hasNotch

    }

    private fun openSettingsMenu() {
        crashlytics().updateLastAction(Action.SETTINGS)
        b.settingsFragmentContainer.show()
    }
    override fun closeSettingsMenu() {
        b.settingsFragmentContainer.hide()
    }

    fun showMessage(msg: String, length: Int = Toast.LENGTH_LONG) {
        runOnUiThread {
            val toast = Toast.makeText(baseContext, msg, length)
            // val toastHeight = b.ui.height + resources.getDimension(R.dimen.toastMargin).toInt()
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }
    }
    fun showMessage(msgId: Int, length: Int = Toast.LENGTH_LONG) {
        showMessage(resources.getString(msgId), length)
    }

    fun hideCategoryButtons() {
//        b.editModeButtons.hide()
    }
    fun showCategoryButtons() {
//        b.editModeButtons.show()
    }

    fun showMenuToggleButton() {
//        b.menuToggleButton.show()
    }
    fun hideMenuToggleButton() {
//        b.menuToggleButton.hide()
    }




    /* FRAGMENT COMMUNICATION */

    fun hideHeaderButtons() {
//        b.header.children.forEach { it.hide() }
    }
    fun showHeaderButtons() {
//        b.header.children.forEach { it.show() }
    }
    fun updateCrashKeys() {
        crashlytics().setCustomKeys {
            key(
                CrashKey.SHAPE_NAME,
                f.shape.getName(getLocalizedResources(this@MainActivity, Locale.US))
            )
            key(
                CrashKey.PALETTE_NAME,
                f.palette.getName(getLocalizedResources(this@MainActivity, Locale.US))
            )
            key(
                CrashKey.TEXTURE_NAME,
                f.texture.getName(getLocalizedResources(this@MainActivity, Locale.US))
            )
        }
    }
    fun updateColorValues() {
//        colorFragment.apply {
//            updateFrequencyLayout()
//            updatePhaseLayout()
//            updateDensityLayout()
//        }
    }
    fun updateFragmentLayouts() {
//        listOf(textureFragment, shapeFragment, colorFragment, positionFragment).forEach { it.updateLayout() }
    }
    fun updateRadius() {
//        textureFragment.loadRadius(updateProgress = true)
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
    fun showBookmarkDialog(item: Fractal, edit: Boolean = false) {

        val dialog = AlertDialogNewBookmarkBinding.inflate(layoutInflater)

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
                    } else {
                        lifecycleScope.launch {
                            Fractal.tempBookmark1.name = dialog.name.text.toString()
                            Log.v("MAIN", "new bookmark thumbnail path: ${Fractal.tempBookmark1.thumbnailPath}")
                            db.fractalDao().apply {
                                Fractal.tempBookmark1.customId = insert(Fractal.tempBookmark1.toDatabaseEntity()).toInt()
                            }
                            Fractal.tempBookmark1.goldFeature = false
                            Fractal.bookmarks.add(0, Fractal.tempBookmark1)
                            Fractal.all.add(0, Fractal.tempBookmark1)
                            Fractal.nextCustomFractalNum++
                        }
                        bookmarkLists.addNewItem(item)
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
                if (previousFractalId != -1) update(Fractal.previous.toDatabaseEntity())
                else {
                    showMessage(resources.getString(R.string.msg_error))
                }
            }
        }
    }
    fun updateColorThumbnails(updateAll: Boolean) {
        if (updateAll) paletteLists.updateCurrentItems()
        else {
            if (updateFromEdit) paletteLists.updateItemFromEdit(f.palette)
            else                paletteLists.updateItemFromAdd(f.palette)
        }
    }
    fun updateTextureThumbnail(n: Int) {

        if (n == -1) b.thumbnailProgressBar.hide()
        else {
            Log.v("MAIN", "texture thumb ${n + 1} rendered")
            f.shape.compatTextures.getOrNull(n)
                ?.let { texture -> textureLists.updateItemFromEdit(texture) }
            // updateTumbnailRenderDialog(n, f.shape.compatTextures.size)
            if (n == f.shape.compatTextures.size - 1) {
                // dismissThumbnailRenderDialog()
                b.thumbnailProgressBar.hide()
            }
        }

    }
    fun updateShapeThumbnail(shape: Shape, customIndex: Int) {

        if (updateFromEdit) shapeLists.updateItemFromEdit(shape) else shapeLists.updateItemFromAdd(shape)
        if (customIndex != -1) {
            Log.v("MAIN", "customIndex: $customIndex")
            val numShapes = Shape.custom.size
            // updateTumbnailRenderDialog(customIndex, numShapes)
            if (customIndex == numShapes - 1) {
                b.thumbnailProgressBar.hide()
                shapeThumbnailsRendered = true
            }
        }

    }




    /* VIDEO */

    fun addKeyframe() {
//        (videoFragment.b.keyframeRecycler.adapter as VideoAdapter).apply {
//
//            if (video.items.isNotEmpty()) {
//                video.addTransition(Video.Transition())
////                notifyItemInserted(video.items.size - 1)
//            }
//            video.addKeyframe(Video.Keyframe(Fractal.tempBookmark1))
//            notifyItemInserted(video.items.size - 1)
//
//        }
    }
    fun setVideoProgress(t: Double) {
        b.videoProgressWindow.run {
            progressBar.setProgress(t)
            estimatedTime.text = "${floor(100.0*t).toInt()}%"
        }
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
            Log.v("MAIN", "processing purchase...")
            Log.v("MAIN", originalJson)
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

        Log.v("MAIN", "querying purchases...")

        val purchaseQueryResult = billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP)
        purchaseQueryResult.purchasesList.getOrNull(0)?.apply {
            Log.v("MAIN", "processing purchase...")
            Log.v("MAIN", originalJson)
            when (purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (!BuildConfig.DEV_VERSION && listAdaptersInitialized) {
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
        if (DEBUG_GOLD_ENABLED && BuildConfig.DEV_VERSION && listAdaptersInitialized) {
            sc.goldEnabled = true
            onGoldEnabled()
        }

    }
    private fun onGoldEnabled() {

        Log.v("MAIN", "gold enabled!")
        crashlytics().setCustomKey(CrashKey.GOLD_ENABLED, true)
        val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        val edit = sp.edit()
        edit.putBoolean(PreferenceKey.GOLD_ENABLED_DIALOG_SHOWN, true)
        edit.apply()

        b.extrasMenu.logisticsOptions.removeView(b.extrasMenu.upgradeButton)
        b.extrasMenu.run {
            listOf(r1440, r2160, r2880, r3600, r4320, r5040, r5760, rCustomScreen).forEach { it.showGradient = false }
            listOf(aspect45, aspect57, aspect23, aspect916, aspect12).forEach { it.showGradient = false }
        }
        
        b.shapeParamMenu.run {
            listOf(shapeParamButton1, shapeParamButton2, shapeParamButton3, shapeParamButton4).forEach { v ->
                // v.showGradient = false
            }
        }
        b.textureParamMenu.run {
            listOf(textureParamButton1, textureParamButton2, textureParamButton3, textureParamButton4).forEach { v ->
                // v.showGradient = false
            }
        }
        
        shapeLists.updateAllItems()
        textureLists.updateAllItems()
        textureImageListAdapter.apply {
            notifyItemChanged(itemCount - 1)
        }

    }




    /* UTILITY */

    private fun getLocalizedResources(context: Context, desiredLocale: Locale?): Resources {
        var conf: Configuration = context.resources.configuration
        conf = Configuration(conf)
        conf.setLocale(desiredLocale)
        val localizedContext = context.createConfigurationContext(conf)
        return localizedContext.resources
    }
    private fun getAvailableHeapMemory() : Long {
        val runtime = Runtime.getRuntime()
        val usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
        val maxHeapSizeInMB = runtime.maxMemory() / 1048576L
        return maxHeapSizeInMB - usedMemInMB
    }
    private fun onRateButtonClicked(): Boolean {
        Toast.makeText(this, "Rate button was clicked", Toast.LENGTH_LONG).show()
        // button click handled
        return true
    }
    private fun readHtml(file: String) : String {

        var str = ""
        val br = resources.assets.open(file).bufferedReader()
        var line: String?

        while (br.readLine().also { line = it } != null) str += line + "\n"
        br.close()

        return str

    }
    private fun saveImage() {
        bookmarkAsPreviousFractal()
        fsv.r.renderProfile = RenderProfile.SAVE_IMAGE
        fsv.requestRender()
    }
    private fun saveImageForShare() {

        fsv.r.renderProfile = RenderProfile.SHARE_IMAGE
        fsv.requestRender()

    }
    fun shareImage() {

        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, Uri.parse(filesDir.path + "temp_image.jpg"))
            type = "image/jpeg"
        }
        startActivity(Intent.createChooser(shareIntent, null))


    }

    private fun activeParam() : RealParam {
        return if (sc.editMode == EditMode.SHAPE) f.shape.params.active else f.texture.params.active
    }

    override fun enableUltraHighRes() {
        b.extrasMenu.run { listOf(r3600, r4320, r5040, r5760).forEach { it.show() } }
    }
    override fun disableUltraHighRes() {
        b.extrasMenu.run { listOf(r3600, r4320, r5040, r5760).forEach { it.hide() } }
    }

    override fun updateButtonAlignment() {
//        Log.d("MAIN", "button alignment set to ${sc.buttonAlignment}")
//        sc.buttonAlignment.updateLayout(
//            b.overlay,
//            b.utilityLayout,
//            b.paramMenuLayout,
//            listOf(
//                b.utilityLayout,
//                b.positionUtilityButtons,
//                b.colorUtilityButtons,
//                b.shapeUtilityButtons,
//                b.textureUtilityButtons
//            )
//        )
    }



}
