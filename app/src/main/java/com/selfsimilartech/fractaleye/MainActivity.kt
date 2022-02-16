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
import android.graphics.drawable.GradientDrawable
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
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.database.getStringOrNull
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
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
import java.text.NumberFormat
import java.text.ParseException
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
const val GOLD_ENABLED_DIALOG_SHOWN = "goldEnabledDialogShown"
const val GOLD_PENDING_DIALOG_SHOWN = "goldPendingDialogShown"
const val SHOW_EPILEPSY_DIALOG = "showEpilepsyDialog"
const val SHOW_TUTORIAL_OPTION = "showTutorialOption"
const val RESOLUTION = "resolution"
const val ASPECT_RATIO = "aspectRatio"
//const val PRECISION = "precision"
//const val AUTO_PRECISION = "autoPrecision"
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
const val PREV_FRACTAL_CREATED = "previousFractalCreated"
const val PREV_FRACTAL_ID = "previousFractalId"
const val TEX_IMAGE_COUNT = "texImageCount"
const val PALETTE = "palette"
const val ACCENT_COLOR1 = "accentColor1"
const val ACCENT_COLOR2 = "accentColor2"
const val USE_ALTERNATE_SPLIT = "useAlternateSplit"

const val ADVANCED_SETTINGS = "advancedSettings"
const val RESTRICT_PARAMS = "restrictParams"
const val ALLOW_SLOW_DUALFLOAT = "allowSlowDualfloat"
const val ULTRA_HIGH_RES = "ultraHighRes"
const val CHUNK_PROFILE = "chunkProfile"
const val TARGET_FRAMERATE = "targetFramerate"

const val VERSION_CODE_TAG = "versionCode"
const val SHARED_PREFERENCES = "com.selfsimilartech.fractaleye.SETTINGS"

const val PALETTE_TABLE_NAME = "palette"
const val SHAPE_TABLE_NAME = "shape"
const val FRACTAL_TABLE_NAME = "fractal"
const val TEX_IM_PREFIX = "tex_im_"

const val SETTINGS_FRAGMENT_TAG = "SETTINGS"
const val VIDEO_FRAGMENT_TAG = "VIDEO"

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



class MainActivity : AppCompatActivity(), SettingsFragment.OnSettingsChangedListener {

    lateinit var b : ActivityMainNewNewBinding
    lateinit var tutw : TutorialWelcomeBinding
    lateinit var db : AppDatabase
    lateinit var fsv : FractalSurfaceView

    private var listAdaptersInitialized = false
    val f = Fractal.default
    var sc = SettingsConfig

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

    private lateinit var editModeSelector         : ParamSelector
    private lateinit var paletteListTypeSelector  : ParamSelector
    private lateinit var shapeListTypeSelector    : ParamSelector
    private lateinit var textureListTypeSelector  : ParamSelector
    private lateinit var bookmarkListTypeSelector : ParamSelector

    private lateinit var customColorDragAdapter  : CustomColorDragAdapter
    private lateinit var textureImageListAdapter : FlexibleAdapter<TextureImageListItem>


    // position variables
    var activePositionParam = PositionParam.ZOOM


    // color variables
    private var activeColorParam : ColorParam = ColorRealParam.FREQUENCY
    private var customPalette = Palette(name="", colors = arrayListOf())
    private var savedCustomPaletteName = ""
    private var savedCustomColors = arrayListOf<Int>()
    private var updateFromEdit = false


    // shape variables
    private var prevShape = Shape.mandelbrot
    private var customShape = Shape(name = "q", latex = "$$")
    private var savedCustomShapeName = ""
    private var savedCustomLatex = ""
    private var savedCustomLoopSingle = ""
    private var savedCustomLoopDual = ""
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
    // private lateinit var videoFragment      : VideoFragment


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
        private val MSG_SHOW_MESSAGE_ID = 2
        private val MSG_SHOW_MESSAGE_STRING = 3
        private val MSG_UPDATE_SHAPE_THUMBNAILS = 4
        private val MSG_SHOW_BOOKMARK_DIALOG = 5
        private val MSG_BOOKMARK_AS_PREVIOUS_FRACTAL = 6
        private val MSG_ADD_KEYFRAME = 7
        private val MSG_UPDATE_RENDER_STATS = 8
        private val MSG_UPDATE_POS_PARAM = 9
        private val MSG_UPDATE_COLOR_PARAM = 10
        private val MSG_UPDATE_SHAPE_TEX_PARAM = 11
        private val MSG_UPDATE_PROGRESS = 12
        private val MSG_QUERY_TUTORIAL_TASK_COMPLETE = 13
        private val MSG_SHARE_IMAGE = 14
        private val MSG_PASS_TOUCH_TO_HIGHLIGHT_WINDOW = 15





        // Weak reference to the Activity; only access this from the UI thread.
        private val mWeakActivity : WeakReference<MainActivity> = WeakReference(activity)

        fun updateColorThumbnails(updateAll: Boolean) {
            sendMessage(obtainMessage(MSG_UPDATE_COLOR_THUMBNAILS, updateAll))
        }
        fun updateTextureThumbnail(n: Int) {
            sendMessage(obtainMessage(MSG_UPDATE_TEXTURE_THUMBNAILS, n, 0))
        }
        fun updateShapeThumbnail(shape: Shape, customIndex: Int?) {
            sendMessage(obtainMessage(MSG_UPDATE_SHAPE_THUMBNAILS, customIndex ?: -1, -1, shape))
        }
        fun showMessage(msgId: Int) {
            sendMessage(obtainMessage(MSG_SHOW_MESSAGE_ID, msgId))
        }
        fun showMessage(msg: String) {
            sendMessage(obtainMessage(MSG_SHOW_MESSAGE_STRING, msg))
        }
        fun showBookmarkDialog() {
            sendMessage(obtainMessage(MSG_SHOW_BOOKMARK_DIALOG))
        }
        fun bookmarkAsPreviousFractal() {
            sendMessage(obtainMessage(MSG_BOOKMARK_AS_PREVIOUS_FRACTAL))
        }
        fun addKeyframe() {
            sendMessage(obtainMessage(MSG_ADD_KEYFRAME))
        }
        fun updateRenderStats(estTime: Double, time: Double, fps: String) {
            sendMessage(obtainMessage(MSG_UPDATE_RENDER_STATS, Bundle().apply {
                putDouble("estTime",    estTime)
                putDouble("time",       time)
                putString("fps",        fps)
            }))
        }
        fun updatePositionParam() {
            sendMessage(obtainMessage(MSG_UPDATE_POS_PARAM))
        }
        fun updateColorParam() {
            sendMessage(obtainMessage(MSG_UPDATE_COLOR_PARAM))
        }
        fun updateShapeTextureParam() {
            sendMessage(obtainMessage(MSG_UPDATE_SHAPE_TEX_PARAM))
        }
        fun updateProgress(p: Double) {
            sendMessage(obtainMessage(MSG_UPDATE_PROGRESS, p))
        }
        fun queryTutorialTaskComplete() {
            sendMessage(obtainMessage(MSG_QUERY_TUTORIAL_TASK_COMPLETE))
        }
        fun shareImage() {
            sendMessage(obtainMessage(MSG_SHARE_IMAGE))
        }
        fun passTouchToHighlightWindow(e: MotionEvent?) {
            sendMessage(obtainMessage(MSG_PASS_TOUCH_TO_HIGHLIGHT_WINDOW, e))
        }



        // runs on UI thread
        override fun handleMessage(msg: Message) {

            val what = msg.what
            //Log.v(TAG, "ActivityHandler [" + this + "]: what=" + what);

            val activity = mWeakActivity.get()
            if (activity == null) {
                Log.w("MAIN ACTIVITY", "ActivityHandler.handleMessage: activity is null")
            }

            when (what) {
                MSG_UPDATE_COLOR_THUMBNAILS -> activity?.updateColorThumbnails(msg.obj as Boolean)
                MSG_UPDATE_TEXTURE_THUMBNAILS -> activity?.updateTextureThumbnail(msg.arg1)
                MSG_UPDATE_SHAPE_THUMBNAILS -> activity?.updateShapeThumbnail(msg.obj as Shape, msg.arg1)
                MSG_SHOW_MESSAGE_ID -> activity?.showMessage(msg.obj as Int)
                MSG_SHOW_MESSAGE_STRING -> activity?.showMessage(msg.obj as String)
                MSG_SHOW_BOOKMARK_DIALOG -> activity?.showBookmarkDialog(Fractal.tempBookmark1)
                MSG_BOOKMARK_AS_PREVIOUS_FRACTAL -> activity?.bookmarkAsPreviousFractal()
                MSG_ADD_KEYFRAME -> activity?.addKeyframe()
                MSG_UPDATE_RENDER_STATS -> {}
                MSG_UPDATE_POS_PARAM -> EditMode.POSITION.updateDisplay()
                MSG_UPDATE_COLOR_PARAM -> activity?.updateColorDisplay()
                MSG_UPDATE_SHAPE_TEX_PARAM -> activity?.updateShapeDisplayValues()
                MSG_UPDATE_PROGRESS -> activity?.b?.progressBar?.apply { progress = ((msg.obj as Double)*max.toDouble()).toInt() }
                MSG_QUERY_TUTORIAL_TASK_COMPLETE -> activity?.queryTutorialTaskComplete()
                MSG_SHARE_IMAGE -> activity?.shareImage()
                else -> throw RuntimeException("unknown msg $what")
            }

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




    private val basicsTutorial = Tutorial(
        object : Tutorial.Task( "start"                      ) {
            override fun onStart() {

                fsv.tutorialInProgress = true
                f.load(Fractal.tutorial1, fsv)
                fsv.r.renderToTex = true
                fsv.r.renderShaderChanged = true
                fsv.requestRender()

                updateLayouts()

                sc.editMode = EditMode.NONE
                editModeSelector.select(null)

                b.prevParamButton.disable()
                b.nextParamButton.disable()

                b.paramDisplay.hide()
                b.editModeButtonLayout.disable()
                b.progressBar.hide()
                b.utilityButtons.hide()
                b.saveImageButton.hide()
                b.uiToggleButton.hide()
                b.extrasMenuButton.hide()
                b.paramMenuToggleButton.hide()

                b.tutorialWindow.run {
                    root.show()
                    description4.setText(R.string.tutorial_basics_intro)
                }
                b.highlightWindow.run {
                    show()
                    bringToFront()
                    consumeTouch = false
                    startHighlightAnimation()
                    highlightViewGroup(b.editModeButtonLayout, 4.dp(context))
                }

            }
            override fun updateProgress() {}
            override fun isComplete() : Boolean {
                return sc.editMode == EditMode.POSITION
            }
        },
        object : Tutorial.Task( "position  :  mode"          ) {
            override fun onStart() {
                editModeSelector.select(null)
                b.editModeButtonLayout.enable()
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_pos)
                    nextButton.hide()
                }
                b.highlightWindow.run {
                    highlightView(b.positionModeButton)
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean { return sc.editMode == EditMode.POSITION }
            override fun onComplete(v: ViewGroup) {
                super.onComplete(v)
                b.editModeButtonLayout.disable()
            }
        },
        object : Tutorial.Task( "position  :  interact"      ) {
            override fun onStart() {
                b.editModeButtonLayout.hide()
                b.tutorialWindow.run {
                    progressBar.progress = 0
                    description1.show()
                    description1.setText(R.string.tutorial_basics_pos_zoom)
                    description2.show()
                    description2.setText(R.string.tutorial_basics_pos_pan)
                    description3.show()
                    description3.setText(R.string.tutorial_basics_pos_task)
                    image1.show()
                    image1.setImageResource(R.drawable.gesture_pinch)
                    image2.show()
                    image2.setImageResource(R.drawable.gesture_swipe_omni)
                    root.bringToFront()
                }
                b.baseLayout.setOnLayoutTransitionEndListener {
                    b.highlightWindow.highlightFromPosition(f.shape.position, -0.77754949627, -0.13556904821)
                }
                b.highlightWindow.run {
                    this.fsv = (this@MainActivity).fsv
                    consumeTouch = true
                    layoutParams = FrameLayout.LayoutParams(Resolution.SCREEN.w, Resolution.SCREEN.h, Gravity.CENTER)
                    clearHighlight()
                }
                b.gestureAnimation.run {
                    show()
                    startPinchAnim()
                    bringToFront()
                }
            }
            override fun updateProgress() {
                when (b.tutorialWindow.progressBar.progress) {
                    0 -> {
                        if (f.shape.position.run { zoom < 10.0.pow(-0.35) && sqrt((x + 0.77754949627).pow(2.0) + (y + 0.13556904821).pow(2.0)) < 5e-2 }) {
                            showMessage(R.string.tutorial_keep_going, Toast.LENGTH_SHORT)
                            b.highlightWindow.resetQuad()
                            b.highlightWindow.highlightFromPosition(f.shape.position, -0.77754949627, -0.13556904821)
                            b.tutorialWindow.progressBar.progress++
                            b.gestureAnimation.run {
                                stopAnim()
                                hide()
                            }
                        }
                    }
                    1 -> {
                        if (f.shape.position.run { zoom < 10.0.pow(-1.15) && sqrt((x + 0.77754949627).pow(2.0) + (y + 0.13556904821).pow(2.0)) < 5e-3 }) {
                            b.highlightWindow.resetQuad()
                            b.highlightWindow.highlightFromPosition(f.shape.position, -0.77754949627, -0.13556904821)
                            b.tutorialWindow.progressBar.progress++
                        }
                    }
                    2 -> {
                        if (f.shape.position.run { zoom < 10.0.pow(-2.0) && sqrt((x + 0.77754949627).pow(2.0) + (y + 0.13556904821).pow(2.0)) < 5e-4 }) {
                            b.tutorialWindow.progressBar.progress++
                        }
                    }
                    else -> {}
                }
            }
            override fun isComplete() : Boolean {
                return b.tutorialWindow.progressBar.progress == 3
            }
            override fun onComplete(v: ViewGroup) {
                super.onComplete(v)
                b.highlightWindow.run {
                    consumeTouch = false
                    resetQuad()
                    clearHighlight()
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        Gravity.NO_GRAVITY
                    )
                }
                f.shape.position.run {
                    zoomLocked = true
                    xLocked = true
                    yLocked = true
                }
            }
        },
        object : Tutorial.Task( "position  :  feedback"      ) {
            override fun onStart() {
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_great)
                    nextButton.show()
                }
                b.highlightWindow.run {
                    clearHighlight()
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return false
            }
        },
        object : Tutorial.Task( "position  :  reset"         ) {
            override fun onStart() {
                b.overlay.setOnLayoutTransitionEndListener {
                    b.highlightWindow.highlightView(b.positionResetButton)
                }
                b.positionResetButton.disable()
                b.utilityButtons.show()
                b.editModeButtonLayout.show()
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_pos_reset)
                    nextButton.show()
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return false
            }
            override fun onComplete(v: ViewGroup) {
                super.onComplete(v)
                b.positionResetButton.enable()
                b.utilityButtons.hide()
            }
        },
        object : Tutorial.Task( "color     :  mode"          ) {
            override fun onStart() {
                fsv.r.renderProfile = RenderProfile.DISCRETE
                fsv.r.renderToTex = true
                fsv.requestRender()
                b.editModeButtonLayout.show()
                b.editModeButtonLayout.enable()
                editModeSelector.select(null)
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_color)
                }
                b.highlightWindow.run {
                    highlightView(b.colorModeButton)
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean { return sc.editMode == EditMode.COLOR }
            override fun onComplete(v: ViewGroup) {
                super.onComplete(v)
                b.editModeButtonLayout.disable()
            }
        },
        object : Tutorial.Task( "color     :  interact"      ) {
            override fun onStart() {
                b.tutorialWindow.progressBar.progress = 0
                if (!sc.autofitColorRange) b.colorAutofitButton.performClick()
                b.editModeButtonLayout.hide()
                b.tutorialWindow.run {
                    description1.show()
                    description1.setText(R.string.tutorial_basics_color_phase)
                    description2.setText(R.string.tutorial_basics_color_freq)
                    description2.show()
                    description3.show()
                    description3.setText(R.string.tutorial_basics_color_task)
                    image1.show()
                    image1.setImageResource(R.drawable.gesture_swipe_horizontal)
                    image2.show()
                    image2.setImageResource(R.drawable.gesture_pinch)
                }
                b.highlightWindow.run {
                    clearHighlight()
                }
                b.gestureAnimation.run {
                    show()
                    startPinchAnim()
                }
            }
            override fun updateProgress() {
                if (b.tutorialWindow.progressBar.progress == 0 && f.color.frequency >= 0.35) {
                    b.tutorialWindow.progressBar.progress = 1
                    showMessage(R.string.tutorial_keep_going, Toast.LENGTH_SHORT)
                }
            }
            override fun isComplete(): Boolean {
                return f.color.frequency >= 1.0
            }
            override fun onComplete(v: ViewGroup) {
                super.onComplete(v)
                b.colorAutofitButton.performClick()
                b.gestureAnimation.run {
                    stopAnim()
                    hide()
                }
            }
        },
        object : Tutorial.Task( "color     :  feedback"      ) {
            override fun onStart() {
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_great)
                    nextButton.show()
                }
                b.highlightWindow.run {
                    clearHighlight()
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return false
            }
        },
        object : Tutorial.Task( "color     :  palette"       ) {
            override fun onStart() {
                b.overlay.setOnLayoutTransitionEndListener {
                    b.highlightWindow.highlightView(b.paletteListButton)
                }
                b.paletteListButton.disable()
                b.colorAutofitButton.disable()
                b.utilityButtons.show()
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_color_palette)
                    nextButton.show()
                }
                b.highlightWindow.run {
                    highlightView(b.paletteListButton)
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return false
            }
        },
        object : Tutorial.Task( "color     :  auto"          ) {
            override fun onStart() {
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_color_auto)
                    nextButton.show()
                }
                b.highlightWindow.run {
                    highlightView(b.colorAutofitButton)
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return false
            }

            override fun onComplete(v: ViewGroup) {
                super.onComplete(v)
                b.paletteListButton.enable()
                b.colorAutofitButton.enable()
                b.utilityButtons.hide()
            }
        },
        object : Tutorial.Task( "shape     :  mode"          ) {
            override fun onStart() {
                editModeSelector.select(null)
                b.editModeButtonLayout.show()
                b.editModeButtonLayout.enable()
                b.paramDisplay.hide()
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_shape)
                }
                b.highlightWindow.run {
                    highlightView(b.shapeModeButton)
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return sc.editMode == EditMode.SHAPE
            }

            override fun onComplete(v: ViewGroup) {
                super.onComplete(v)
                b.editModeButtonLayout.disable()
            }
        },
        object : Tutorial.Task( "shape     :  detail"        ) {
            override fun onStart() {
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_shape_detail)
                    nextButton.show()
                }
                b.highlightWindow.clearHighlight()
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return false
            }
        },
        object : Tutorial.Task( "shape     :  interact 1"    ) {
            override fun onStart() {
                b.overlay.setOnLayoutTransitionEndListener {
                    b.highlightWindow.highlightViewGroup(b.paramDisplay)
                }
                b.paramDisplay.show()
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_shape_task)
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return f.shape.params.detail.u >= log2(300.0).pow(2.0)
            }
            override fun onComplete(v: ViewGroup) {
                super.onComplete(v)
                b.paramDisplay.hide()
            }
        },
        object : Tutorial.Task( "shape     :  feedback 1"    ) {
            override fun onStart() {
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_shape_detail_tip)
                    nextButton.show()
                }
                b.highlightWindow.run {
                    clearHighlight()
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return false
            }
        },
        object : Tutorial.Task( "shape     :  list"          ) {
            override fun onStart() {
                b.overlay.setOnLayoutTransitionEndListener {
                    b.highlightWindow.highlightView(b.shapeListButton)
                }
                b.shapeListButton.disable()
                b.juliaModeButton.disable()
                b.utilityButtons.show()
                b.paramDisplay.hide()
                b.editModeButtonLayout.show()
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_shape_list)
                    nextButton.show()
                }
                b.highlightWindow.run {
                    highlightView(b.shapeListButton)
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return false
            }
        },
        object : Tutorial.Task( "shape     :  julia mode"    ) {
            override fun onStart() {
                b.juliaModeButton.enable()
                f.shape.params.julia.setFrom(ComplexParam(f.shape.position.x, f.shape.position.y))
                f.shape.positions.julia.run {
                    zoom = 10.0.pow(0.35)
                    rotation = Math.PI/2.0
                }
                f.color.run {
                    frequency = 2.0.pow(1.35) - 1.0
                    phase = 0.4
                }
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_shape_julia_mode_task)
                }
                b.highlightWindow.run {
                    highlightView(b.juliaModeButton)
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return f.shape.juliaMode
            }
            override fun onComplete(v: ViewGroup) {
                super.onComplete(v)
                b.shapeListButton.enable()
            }
        },
        object : Tutorial.Task( "shape     :  complex param" ) {
            override fun onStart() {
                b.overlay.setOnLayoutTransitionEndListener {
                    b.highlightWindow.highlightViewGroup(b.paramDisplay)
                }
                b.paramDisplay.show()
                b.utilityButtons.hide()
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_shape_complex_param)
                    nextButton.show()
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return false
            }
        },
        object : Tutorial.Task( "shape     :  interact 2"    ) {
            override fun onStart() {
                b.editModeButtonLayout.hide()
                b.tutorialWindow.run {
                    progressBar.progress = 0
                    description1.show()
                    description1.setText(R.string.tutorial_basics_shape_julia_param)
                    image1.show()
                    image1.setImageResource(R.drawable.gesture_swipe_omni)
                }
                b.highlightWindow.run {
                    clearHighlight()
                }
                b.gestureAnimation.run {
                    show()
                    startSwipeDiagonalAnim()
                }
            }
            override fun updateProgress() {
                b.tutorialWindow.progressBar.run {
                    if (progress == 0) {
                        progress = 1
                        handler.postDelayed({
                            b.gestureAnimation.run {
                                stopAnim()
                                hide()
                            }
                        }, 2000L)
                        handler.postDelayed({
                            progress = 2
                            queryTutorialTaskComplete()
                        }, 5000L)
                    }
                }

            }
            override fun isComplete(): Boolean {
                return b.tutorialWindow.progressBar.progress == 2
            }
            override fun onComplete(v: ViewGroup) {
                super.onComplete(v)
                b.paramDisplay.hide()
//                b.overlay.connectBottomToTop(b.paramDisplay.id, b.editModeButtonLayout.id)
//                b.overlay.connectBottomToTop(b.tutorialWindow.root.id, b.paramDisplay.id)
            }
        },
        object : Tutorial.Task( "shape     :  feedback 2"    ) {
            override fun onStart() {
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_great)
                    nextButton.show()
                }
                b.highlightWindow.run {
                    clearHighlight()
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return false
            }
        },
        object : Tutorial.Task( "texture   :  mode"          ) {
            override fun onStart() {
                editModeSelector.select(null)
                b.editModeButtonLayout.show()
                b.editModeButtonLayout.enable()
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_texture)
                }
                b.highlightWindow.highlightView(b.textureModeButton)
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return sc.editMode == EditMode.TEXTURE
            }
            override fun onComplete(v: ViewGroup) {
                super.onComplete(v)
                b.editModeButtonLayout.disable()
                f.texture = Texture.stripeAvg.apply {
                    params.list[2].u = 4.0
                    params.list[1].u = atan2(f.shape.params.julia.v, f.shape.params.julia.u).inDegrees() - 45.0
                }
                fsv.r.renderShaderChanged = true
                fsv.r.renderToTex = true
                fsv.requestRender()
                val param = f.texture.params.list[1]
                f.texture.params.active = param
                bindTextureParameter(param)
            }
        },
        object : Tutorial.Task( "texture   :  real param"    ) {
            override fun onStart() {
                b.overlay.setOnLayoutTransitionEndListener {
                    b.highlightWindow.highlightViewGroup(b.paramDisplay)
                }
                b.paramDisplay.show()
                b.editModeButtonLayout.hide()
                b.tutorialWindow.run {
                    progressBar.progress = 0
                    description4.show()
                    description4.setText(R.string.tutorial_basics_texture_task)
                }
                b.highlightWindow.run {
                    clearHighlight()
                }
            }
            override fun updateProgress() {
                b.tutorialWindow.progressBar.run {
                    if (progress == 0) {
                        progress = 1
                        handler.postDelayed({
                            b.tutorialWindow.progressBar.progress = 2
                            queryTutorialTaskComplete()
                        }, 5000L)
                    }
                }
                if (b.tutorialWindow.progressBar.progress == 0) {
                    b.tutorialWindow.progressBar.progress = 1

                }
            }
            override fun isComplete(): Boolean {
                return b.tutorialWindow.progressBar.progress == 2
            }
            override fun onComplete(v: ViewGroup) {

                super.onComplete(v)
                b.paramDisplay.hide()
                b.utilityButtons.show()
                b.overlay.connectBottomToTop(b.paramDisplay.id, b.editModeButtonLayout.id)
                b.overlay.connectBottomToTop(b.tutorialWindow.root.id, b.paramDisplay.id)

            }
        },
        object : Tutorial.Task( "texture   :  feedback"      ) {
            override fun onStart() {
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_great)
                    nextButton.show()
                }
                b.highlightWindow.run {
                    clearHighlight()
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return false
            }
        },
        object : Tutorial.Task( "texture   :  list"          ) {
            override fun onStart() {
                b.overlay.setOnLayoutTransitionEndListener {
                    b.highlightWindow.highlightView(b.textureListButton)
                }
                b.textureListButton.disable()
                b.textureRegionButton.disable()
                b.utilityButtons.show()
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_texture_list)
                    nextButton.show()
                }
                b.highlightWindow.run {
                    highlightView(b.textureListButton)
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return false
            }
        },
        object : Tutorial.Task( "texture   :  region"        ) {
            override fun onStart() {
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_texture_region)
                    nextButton.show()
                }
                b.highlightWindow.run {
                    highlightView(b.textureRegionButton)
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return false
            }
            override fun onComplete(v: ViewGroup) {
                super.onComplete(v)
                b.textureListButton.enable()
                b.textureRegionButton.enable()
                b.utilityButtons.hide()
            }
        },
        object : Tutorial.Task( "ui visibility"              ) {
            override fun onStart() {
                b.overlay.setOnLayoutTransitionEndListener(LayoutTransition.APPEARING) {
                    b.highlightWindow.highlightView(b.uiToggleButton)
                    b.overlay.connectTopToBottom(b.tutorialWindow.root.id, b.saveImageButton.id, 24.dp(b.root.context))
                }
                b.uiToggleButton.show()
                b.saveImageButton.show()
                b.extrasMenuButton.show()
                b.uiToggleButton.disable()
                b.saveImageButton.disable()
                b.extrasMenuButton.disable()
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_toggle_ui)
                    nextButton.show()
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return false
            }
        },
        object : Tutorial.Task( "save image"                 ) {
            override fun onStart() {
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_save_image)
                    nextButton.show()
                }
                b.highlightWindow.run {
                    highlightView(b.saveImageButton)
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return false
            }
        },
        object : Tutorial.Task( "extras menu"                ) {
            override fun onStart() {
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_options_menu)
                    nextButton.show()
                }
                b.highlightWindow.run {
                    highlightView(b.extrasMenuButton, scale = 5.dp(context))
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return true
            }
            override fun onComplete(v: ViewGroup) {
                super.onComplete(v)
                b.extrasMenuButton.enable()
            }
        },
        object : Tutorial.Task( "bookmarks"                  ) {
            override fun onStart() {
                b.overlay.setOnLayoutTransitionEndListener {
                    b.highlightWindow.highlightView(b.extrasMenu.bookmarkListButton)
                }
                showExtrasMenu()
                b.extrasMenu.root.disable()
                b.overlay.connectTopToBottom(b.tutorialWindow.root.id, b.extrasMenu.root.id)
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_bookmarks)
                    nextButton.show()
                }
                b.highlightWindow.run {
                    highlightView(b.extrasMenuButton, scale = 5.dp(context))
                }
            }
            override fun updateProgress() {}
            override fun isComplete(): Boolean {
                return false
            }
            override fun onComplete(v: ViewGroup) {
                super.onComplete(v)
                b.extrasMenu.root.enable()
                hideExtrasMenu()
            }
        },
        object : Tutorial.Task( "finish"                     ) {
            override fun onStart() {
                hideAllUi()
                b.uiToggleButton.hide()
                ConstraintSet().let { set ->
                    set.clone(b.overlay)
                    set.connect(b.tutorialWindow.root.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                    set.connect(b.tutorialWindow.root.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                    set.applyTo(b.overlay)
                }
                b.tutorialWindow.run {
                    description4.show()
                    description4.setText(R.string.tutorial_basics_finish)
                    nextButton.hide()
                    finishButton.show()
                }
                b.highlightWindow.run {
                    stopHighlightAnimation()
                    hide()
                }
            }
            override fun updateProgress() {}
            override fun onComplete(v: ViewGroup) {

                super.onComplete(v)

                b.prevParamButton.enable()
                b.nextParamButton.enable()

                b.editModeButtonLayout.enable()
                b.positionModeButton.performClick()
                b.saveImageButton.enable()
                b.uiToggleButton.enable()
                f.run {
                    color.reset()
                    shape.reset()
                    texture = Texture.escapeSmooth
                }
                fsv.r.renderToTex = true
                fsv.r.renderShaderChanged = true
                fsv.requestRender()

                updateLayouts()

                showAllUi()
                b.uiToggleButton.show()
                b.tutorialWindow.root.hide()

            }
            override fun isComplete(): Boolean { return true }
        }
    )

    fun queryTutorialTaskComplete() {
        basicsTutorial.run {
            if (activeTask?.isComplete() == true) next(b.tutorialWindow.root) else activeTask?.updateProgress()
        }
    }



    private fun String.formatToDouble(showMsg: Boolean = true) : Double? {
        val nf = NumberFormat.getInstance()
        var d : Double? = null
        try { d = nf.parse(this)?.toDouble() }
        catch (e: ParseException) {
            if (showMsg) {
                showMessage(resources.getString(R.string.msg_invalid_format))
            }
        }
        return d
    }




    /* LIFECYCLE */

    override fun onCreate(savedInstanceState: Bundle?) {

        val onCreateStartTime = currentTimeMs()
        super.onCreate(savedInstanceState)
        b = ActivityMainNewNewBinding.inflate(layoutInflater)
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

            key( CRASH_KEY_MAX_ITER, f.shape.params.detail.u.toInt() )

        }
        crashlytics().updateLastAction(Action.INIT)

        db = AppDatabase.getInstance(applicationContext)


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
        goldEnabledDialogShown = sp.getBoolean(GOLD_ENABLED_DIALOG_SHOWN, false)
        goldPendingDialogShown = sp.getBoolean(GOLD_PENDING_DIALOG_SHOWN, false)
        showEpilepsyDialog = sp.getBoolean(SHOW_EPILEPSY_DIALOG, true)
        showTutorialOption = sp.getBoolean(SHOW_TUTORIAL_OPTION, true)
        previousFractalCreated = sp.getBoolean(PREV_FRACTAL_CREATED, false)
        previousFractalId = sp.getInt(PREV_FRACTAL_ID, -1)
        Log.e("MAIN", "previousFractalCreated: $previousFractalCreated, id: $previousFractalId")

        Texture.CUSTOM_IMAGE_COUNT = sp.getInt(TEX_IMAGE_COUNT, 0)

        // val maxStartupRes = if (sc.goldEnabled) Resolution.SCREEN else Resolution.R1080
        val savedResolution = sp.getInt(RESOLUTION, Resolution.foregrounds.indexOf(Resolution.R1080))
        sc.resolution = Resolution.foregrounds.getOrNull(savedResolution) ?: Resolution.R720
        crashlytics().setCustomKey(CRASH_KEY_RESOLUTION, sc.resolution.toString())

        sc.targetFramerate          = sp.getInt(     TARGET_FRAMERATE,      sc.targetFramerate         )
        sc.continuousPosRender      = sp.getBoolean( CONTINUOUS_RENDER,     sc.continuousPosRender     )
        sc.renderBackground         =
            if (sc.continuousPosRender) false
            else                      sp.getBoolean( RENDER_BACKGROUND,     sc.renderBackground        )
        sc.hideSystemBars           = sp.getBoolean( HIDE_NAV_BAR,          sc.hideSystemBars          )
        sc.autofitColorRange        = sp.getBoolean( AUTOFIT_COLOR_RANGE,   sc.autofitColorRange       )
        sc.useAlternateSplit        = sp.getBoolean( USE_ALTERNATE_SPLIT,   sc.useAlternateSplit       )
        sc.advancedSettingsEnabled  = sp.getBoolean( ADVANCED_SETTINGS,     sc.advancedSettingsEnabled )
        sc.ultraHighResolutions     = sp.getBoolean( ULTRA_HIGH_RES,        sc.ultraHighResolutions    )
        sc.restrictParams           = sp.getBoolean( RESTRICT_PARAMS,       sc.restrictParams          )
        sc.allowSlowDualfloat       = sp.getBoolean( ALLOW_SLOW_DUALFLOAT,  sc.allowSlowDualfloat      )
        f.color.fillColor   = sp.getInt(     ACCENT_COLOR1,         Color.WHITE                )

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
        b.paramDisplay.setOnTouchListener { v, event -> true }
        b.editModeButtonLayout.setOnTouchListener { v, event -> true }
        listOf(
            b.overlay,
            b.paramDisplay,
            b.paramDisplayFrame,
            b.positionParamDisplay.root,
            b.textureShapeParamDisplay.root,
            b.textureShapeParamDisplay.realParamDisplay,
            b.customShapeCreator.root,
            b.customShapeCreator.equationLayout
        ).forEach { it.layoutTransition.enableTransitionType(LayoutTransition.CHANGING) }


        fsv = b.fractalSurfaceView
        fsv.initialize(r, handler)
        fsv.layoutParams = FrameLayout.LayoutParams(screenWidth, screenHeight, Gravity.CENTER)
        b.fractalLayout.layoutParams = FrameLayout.LayoutParams(screenWidth, screenHeight, Gravity.CENTER)

        var firstBookmarkSelection = true





        val editModeButtons = listOf(
            b.positionModeButton,
            b.colorModeButton,
            b.shapeModeButton,
            b.textureModeButton
        )
        editModeSelector = ParamSelector(this, R.drawable.edit_mode_button_highlight, editModeButtons)
        val editModeButtonListener = View.OnClickListener {
            (it as? EditModeButton)?.run {
                if (editModeSelector.isSelected(this)) b.paramMenuToggleButton.performClick()  // re-selection
                else {  // new selection

                    val oldMode = sc.editMode

                    // if (!mode.alwaysDisplayParam && b.paramMenu.isGone) b.paramDisplay.hide() else b.paramDisplay.show()

                    oldMode.run {
                        utilityButtons?.hide()
                        paramMenuLayout?.hide()
                        if (paramDisplayLayout != mode.paramDisplayLayout) paramDisplayLayout?.hide()
                    }
                    mode.run {
                        utilityButtons?.show()
                        paramMenuLayout?.show()
                        paramDisplayLayout?.show()
                    }
                    editModeSelector.select(this)
                    sc.editMode = mode
                    mode.run {
                        updateDisplay()
                        updateAdjust()
                    }

                    if (basicsTutorial.inProgress) queryTutorialTaskComplete()

                }
            }
        }
        editModeButtons.forEachIndexed { i, button ->
            button.mode = EditMode.values()[i]
            button.setOnClickListener(editModeButtonListener)
        }

        EditMode.POSITION.apply {
            
            paramMenuLayout = b.positionParamMenu.root
            paramDisplayLayout = b.positionParamDisplay.root
            utilityButtons  = b.positionUtilityButtons
            
            updateDisplay = { updatePositionDisplay() }

        }
        EditMode.COLOR.apply {

            paramMenuLayout = b.colorParamMenu.root
            paramDisplayLayout = b.colorParamDisplay.root
            listLayout      = b.paletteList.root
            utilityButtons  = b.colorUtilityButtons
            listNavButtons.addAll(setOf(b.actionDone, b.actionNew))
            customNavButtons.addAll(setOf(b.actionDone, b.actionCancel))

            updateDisplay = { updateColorDisplay() }
            updateAdjust = { updateColorAdjust() }
            updateLayout = { updateColorLayout() }

        }
        EditMode.SHAPE.apply {

            paramMenuLayout = b.shapeParamMenu.root
            paramDisplayLayout = b.textureShapeParamDisplay.root
            listLayout      = b.shapeList.root
            utilityButtons  = b.shapeUtilityButtons
            listNavButtons.addAll(setOf(b.actionDone, b.actionNew))
            customNavButtons.addAll(setOf(b.actionDone, b.actionCancel))

            updateDisplay = {
                bindShapeParameter(f.shape.params.active)
                updateShapeDisplayValues()
            }
            updateAdjust = { updateRealParamAdjust() }
            updateLayout = { updateShapeLayout() }

        }
        EditMode.TEXTURE.apply {

            paramMenuLayout = b.textureParamMenu.root
            paramDisplayLayout = b.textureShapeParamDisplay.root
            listLayout      = b.textureList.root
            utilityButtons  = b.textureUtilityButtons
            listNavButtons.addAll(setOf(b.actionDone))

            updateDisplay = {
                updateShapeDisplayValues()
                bindTextureParameter(f.texture.params.active)
            }
            updateAdjust = { updateRealParamAdjust() }
            updateLayout = { updateTextureLayout() }

        }
        EditMode.NONE.apply {
            listLayout = b.bookmarkList.root
            listNavButtons.addAll(setOf(b.actionDone, b.actionCancel))
        }



        paletteListTypeSelector  = ParamSelector(this, R.drawable.edit_mode_button_highlight, b.paletteList  .run { listOf(defaultListButton, customListButton, favoritesListButton) })
        shapeListTypeSelector    = ParamSelector(this, R.drawable.edit_mode_button_highlight, b.shapeList    .run { listOf(defaultListButton, customListButton, favoritesListButton) })
        textureListTypeSelector  = ParamSelector(this, R.drawable.edit_mode_button_highlight, b.textureList  .run { listOf(defaultListButton, customListButton, favoritesListButton) })
        bookmarkListTypeSelector = ParamSelector(this, R.drawable.edit_mode_button_highlight, b.bookmarkList .run { listOf(defaultListButton, customListButton, favoritesListButton) })



        b.actionDone.setOnClickListener {
            when (uiState) {
                UiState.CUSTOM_PALETTE -> {
                    when {
                        Palette.all.any {
                            if (customPalette.name == it.name) {
                                if (customPalette.hasCustomId) customPalette.id != it.id
                                else true
                            } else false
                        } -> {
                            showMessage(resources.getString(R.string.msg_custom_name_duplicate).format(
                                    resources.getString(R.string.palette)
                            ))
                        }
                        customPalette.name == "" -> {
                            showMessage(resources.getString(R.string.msg_empty_name))
                        }
                        else -> {

                            if (customPalette.hasCustomId) {

                                updateFromEdit = true

                                // update existing palette in database
                                lifecycleScope.launch {
                                    db.colorPaletteDao().apply {
                                        update(customPalette.toDatabaseEntity())
                                    }
                                    // paletteListTypeMenu.getSelection().performClick()
                                    paletteLists.updateItemFromEdit(customPalette)
                                    // paletteListAdapter.getActivatedItem()?.run { paletteListAdapter.updateItem(this) }
                                }
                            }
                            else {

                                updateFromEdit = false

                                // add new palette to database
                                lifecycleScope.launch {
                                    db.colorPaletteDao().apply {
                                        customPalette.id = insert(customPalette.toDatabaseEntity()).toInt()
                                        customPalette.hasCustomId = true
                                    }
                                }

                                paletteLists.addNewItem(customPalette)
                                Palette.nextCustomPaletteNum++
                                if (!paletteListTypeSelector.isSelected(b.paletteList.customListButton)) {
                                    // show new item indicator
                                }

                                Palette.all.add(0, customPalette)
                                Palette.custom.add(0, customPalette)

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
                            Shape.all.any {
                                if (customShape.name == it.name) {
                                    if (customShape.hasCustomId) customShape.id != it.id
                                    else true
                                } else false
                            } -> {
                                showMessage(resources.getString(R.string.msg_custom_name_duplicate).format(
                                        resources.getString(R.string.shape)
                                ))
                            }
                            b.customShapeCreator.eqnErrorIndicator.isVisible() -> {
                                showMessage(resources.getString(R.string.msg_eqn_error))
                            }
                            customShape.name == "" -> { showMessage(resources.getString(R.string.msg_empty_name)) }
                            else -> {

                                if (f.texture != prevTexture) textureChanged = true

                                lifecycleScope.launch {
                                    if (customShape.hasCustomId) {
                                        
                                        updateFromEdit = true

                                        // update existing shape in database
                                        db.shapeDao().update(customShape.toDatabaseEntity())

                                        fsv.r.renderProfile = RenderProfile.SHAPE_THUMB
                                        fsv.requestRender()

                                        // update list items if applicable (icon, latex)
                                        shapeLists.updateItemFromEdit(customShape)

                                    }
                                    else {

                                        updateFromEdit = false

                                        // add new shape to database
                                        db.shapeDao().apply {
                                            customShape.id = insert(customShape.toDatabaseEntity()).toInt()
                                            customShape.hasCustomId = true
                                            Log.e("SHAPE", "new custom id: ${customShape.id}")
                                            customShape.initialize(resources)
                                            fsv.r.renderProfile = RenderProfile.SHAPE_THUMB
                                            fsv.requestRender()
                                        }

                                        // add item to list adapter and select
                                        shapeLists.addNewItem(customShape)

                                        Shape.all.add(0, customShape)
                                        Shape.custom.add(0, customShape)
                                        Shape.nextCustomShapeNum++

                                    }
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
                    if (compatTexturesChanged) updateTextureListItems()
                }
                UiState.EDITMODE_LIST -> {
                    setUiState(UiState.HOME)
                    when (sc.editMode) {
                        EditMode.SHAPE -> {
                            updateShapeLayout()
                            if (textureChanged) updateTextureLayout()
                            if (compatTexturesChanged) updateTextureListItems()
                        }
                        EditMode.TEXTURE -> {
                            if (textureChanged) {
                                updateColorLayout()
                                updateTextureLayout()
                            }
                        }
                        EditMode.COLOR -> {
                            fsv.r.renderProfile = RenderProfile.DISCRETE
                            updateColorLayout()
                        }
                        else -> {}
                    }
                }
                UiState.VIDEO -> {
                    setUiState(UiState.HOME)
                }
                UiState.RANDOMIZER -> {
                    if (textureChanged) updateTextureLayout()
                    if (compatTexturesChanged) updateTextureListItems()
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

                        customPalette = Palette(
                                name = "%s %s %d".format(
                                        resources.getString(R.string.header_custom),
                                        resources.getString(R.string.palette),
                                        Palette.nextCustomPaletteNum
                                ),
                                colors = Palette.generateColors(if (sc.goldEnabled) 5 else 3)
                        ).apply { initialize(resources) }

                        // ColorPalette.all.add(0, customPalette)
                        f.palette = customPalette
                        fsv.requestRender()

                        customColorDragAdapter.updateColors(customPalette.colors)
//                        customColorDragAdapter.apply { linkColor(0, itemList[0].second) }
//                        b.customPaletteCreator.customPaletteName.setText(customPalette.name)

                    }
                }
                EditMode.SHAPE -> {

                    crashlytics().updateLastAction(Action.SHAPE_CREATE)
                    setUiState(UiState.CUSTOM_SHAPE)

                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

                    prevTexture = f.texture

                    sc.editMode = EditMode.COLOR
                    fsv.r.renderProfile = RenderProfile.DISCRETE

                    customShape = Shape.createNewCustom(resources)

                    b.customShapeCreator.apply {
//                        customShapeName.setText(customShape.name)
                        shapeMathQuill.setLatex(customShape.latex)
                    }

                    f.shape = customShape
                    f.texture = Texture.escapeSmooth

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
                    if (customPalette.hasCustomId) {
                        // revert changes
                        customPalette.apply {
                            name = savedCustomPaletteName
                            colors = ArrayList(savedCustomColors)
                            updateFlatPalette()
                        }
                        Log.v("COLOR", "cancel edit")
                    }
                    else {
                        // select previous palette
                        customPalette.release()
                        f.palette = paletteLists.getSelectedItem() ?: Palette.eye
                        Log.v("COLOR", "cancel new")
                    }
                    setUiState(UiState.EDITMODE_LIST)
                    updateGradient()
                    fsv.requestRender()
                }
                UiState.CUSTOM_SHAPE -> {

                    f.texture = prevTexture

                    if (customShape.hasCustomId) {
                        // revert changes
                        customShape.apply {
                            name = savedCustomShapeName
                            latex = savedCustomLatex
                            customLoopSingle = savedCustomLoopSingle
                            customLoopDual = savedCustomLoopDual
                        }
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





        /* POSITION */

        PositionParam.ZOOM.apply {
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
        PositionParam.ROTATION.apply {
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
        PositionParam.SHIFT_HORIZONTAL.apply {
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
        PositionParam.SHIFT_VERTICAL.apply {
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

        val positionParamSelector = ParamSelector(this, R.drawable.menu_button_highlight, b.positionParamMenu.run { listOf(zoomButton, rotateButton, shiftHorizontalButton, shiftVerticalButton) })
        val positionParamListener = { button: ImageButton, p: PositionParam -> View.OnClickListener {
            activePositionParam = p
            positionParamSelector.select(button)
            updatePositionDisplay()
            b.positionParamDisplay.run {

                if (p == PositionParam.ROTATION) rotationLock.show() else rotationLock.hide()
                value.setOnEditorActionListener(editListener(null) { w: TextView ->
                    val result = w.text.toString().formatToDouble()
                    if (result != null) {
                        p.fromDislayFormat(f.shape.position, result)
                        if (fsv.r.isRendering) fsv.r.interruptRender = true
                        fsv.renderContinuousDiscrete()
                    }
                    updatePositionDisplay()
                })
                sensitivityButton.run { param = p }

                decreaseButton.setImageResource(p.decreaseIconId)
                decreaseButton.setOnClickListener(p.onDecreaseClick)
                decreaseButton.setOnLongClickListener(p.onDecreaseLongClick)

                increaseButton.setImageResource(p.increaseIconId)
                increaseButton.setOnClickListener(p.onIncreaseClick)
                increaseButton.setOnLongClickListener(p.onIncreaseLongClick)
            }
        }}
        b.positionParamMenu.apply {

            zoomButton              .setOnClickListener(positionParamListener( zoomButton,             PositionParam.ZOOM              ))
            rotateButton            .setOnClickListener(positionParamListener( rotateButton,           PositionParam.ROTATION          ))
            shiftVerticalButton     .setOnClickListener(positionParamListener( shiftVerticalButton,    PositionParam.SHIFT_VERTICAL    ))
            shiftHorizontalButton   .setOnClickListener(positionParamListener( shiftHorizontalButton,  PositionParam.SHIFT_HORIZONTAL  ))

            b.positionParamDisplay.rotationLock.setOnClickListener {
                f.shape.position.rotationLocked = b.positionParamDisplay.rotationLock.isChecked
            }

            zoomButton.performClick()

        }
        b.positionResetButton.setOnClickListener {

            fsv.r.checkThresholdCross { f.shape.position.reset() }

            if (sc.autofitColorRange) fsv.r.calcNewTextureSpan = true
            if (fsv.r.isRendering) fsv.r.interruptRender = true
            fsv.renderContinuousDiscrete()
            updatePositionDisplay()

        }



        /* COLOR */

        ColorRealParam.FREQUENCY.run {
            onDecreaseClick = ColorChangeOnClickListener(fsv,
                transformFractal  = { f.color.frequency /= ColorRealParam.FREQUENCY.discreteDelta },
                updateLayout      = { updateColorDisplay() }
            )
            onDecreaseLongClick = ColorChangeOnLongClickListener(fsv,
                transformFractal  = { f.color.frequency /= ColorRealParam.FREQUENCY.continuousDelta },
                updateLayout      = { updateColorDisplay() }
            )
            onIncreaseClick = ColorChangeOnClickListener(fsv,
                transformFractal  = { f.color.frequency *= ColorRealParam.FREQUENCY.discreteDelta },
                updateLayout      = { updateColorDisplay() }
            )
            onIncreaseLongClick = ColorChangeOnLongClickListener(fsv,
                transformFractal  = { f.color.frequency *= ColorRealParam.FREQUENCY.continuousDelta },
                updateLayout      = { updateColorDisplay() }
            )
        }
        ColorRealParam.PHASE.run {
            onDecreaseClick = ColorChangeOnClickListener(fsv,
                transformFractal  = { f.color.phase -= ColorRealParam.PHASE.discreteDelta },
                updateLayout      = { updateColorDisplay() }
            )
            onDecreaseLongClick = ColorChangeOnLongClickListener(fsv,
                transformFractal  = { f.color.phase -= ColorRealParam.PHASE.continuousDelta },
                updateLayout      = { updateColorDisplay() }
            )
            onIncreaseClick = ColorChangeOnClickListener(fsv,
                transformFractal  = { f.color.phase += ColorRealParam.PHASE.discreteDelta },
                updateLayout      = { updateColorDisplay() }
            )
            onIncreaseLongClick = ColorChangeOnLongClickListener(fsv,
                transformFractal  = { f.color.phase += ColorRealParam.PHASE.continuousDelta },
                updateLayout      = { updateColorDisplay() }
            )
        }
        ColorRealParam.DENSITY.run {
            onDecreaseClick = ColorChangeOnClickListener(fsv,
                transformFractal  = { f.color.density = (f.color.density - ColorRealParam.DENSITY.discreteDelta).clamp(0.0, 5.0) },
                updateLayout      = { updateColorDisplay() }
            )
            onDecreaseLongClick = ColorChangeOnLongClickListener(fsv,
                transformFractal  = { f.color.density = (f.color.density - ColorRealParam.DENSITY.continuousDelta).clamp(0.0, 5.0) },
                updateLayout      = { updateColorDisplay() }
            )
            onIncreaseClick = ColorChangeOnClickListener(fsv,
                transformFractal  = { f.color.density = (f.color.density + ColorRealParam.DENSITY.discreteDelta).clamp(0.0, 5.0) },
                updateLayout      = { updateColorDisplay() }
            )
            onIncreaseLongClick = ColorChangeOnLongClickListener(fsv,
                transformFractal  = { f.color.density = (f.color.density + ColorRealParam.DENSITY.continuousDelta).clamp(0.0, 5.0) },
                updateLayout      = { updateColorDisplay() }
            )
        }

        ColorAccent.FILL.run {
            color = { f.color.fillColor }
            updateColor = { c: Int -> f.color.fillColor = c }
        }
        ColorAccent.OUTLINE.run {
            color = { f.color.outlineColor }
            updateColor = { c: Int -> f.color.outlineColor = c }
        }

        val colorParamSelector = ParamSelector(this, R.drawable.menu_button_highlight, b.colorParamMenu.run { listOf(frequencyButton, phaseButton, densityButton, fillColorButton, outlineColorButton) })
        val colorParamListener = { button: ImageButton, p: ColorParam -> View.OnClickListener {
            bindColorParameter(p)
            colorParamSelector.select(button)
        }}
        b.colorParamMenu.apply {

            fillColorButton.setOnClickListener(colorParamListener(fillColorButton, ColorAccent.FILL))
            outlineColorButton.setOnClickListener(colorParamListener(outlineColorButton, ColorAccent.OUTLINE))
            frequencyButton.setOnClickListener(colorParamListener(frequencyButton, ColorRealParam.FREQUENCY))
            phaseButton.setOnClickListener(colorParamListener(phaseButton, ColorRealParam.PHASE))
            densityButton.setOnClickListener(colorParamListener(densityButton, ColorRealParam.DENSITY))

        }
        b.paletteListButton.setOnClickListener {

            crashlytics().updateLastAction(Action.PALETTE_CHANGE)
            setUiState(UiState.EDITMODE_LIST)

//            paletteListAdapter.apply {
//                (if (selectedPositions.isEmpty()) getFirstPositionOf(f.palette) else activatedPos).let {
//                    setActivatedPosition(it)
//                    recyclerView?.scrollToPosition(it)
//                }
//            }

            // color thumbnail render
            handler.postDelayed({

                fsv.r.renderProfile = RenderProfile.COLOR_THUMB
                fsv.r.renderAllThumbnails = true
                fsv.r.renderToTex = true
                fsv.requestRender()

            }, BUTTON_CLICK_DELAY_MED)

        }
        b.colorAutofitButton.setOnClickListener {

            sc.autofitColorRange = b.colorAutofitButton.isChecked
            fsv.r.autofitColorSelected = b.colorAutofitButton.isChecked
            fsv.r.calcNewTextureSpan = true
            if (b.colorAutofitButton.isChecked) {

                fsv.interruptRender()
                if (f.texture.usesDensity) b.colorParamMenu.densityButton.show()
                fsv.r.renderToTex = true

            }
            else {

                f.color.apply {
                    density = 0.0
                    b.colorParamMenu.run {
                        densityButton.hide()
                        frequencyButton.performClick()
                    }

                    // adjust frequency and phase to match old fit

                    val M = fsv.r.textureSpan.max()
                    val m = fsv.r.textureSpan.min()
                    val L = M - m
                    val prevFreq = frequency
                    val prevPhase = phase

                    fsv.r.setTextureSpan(0f, 1f)

                    frequency = prevFreq / L
                    phase = prevPhase - prevFreq * m / L
                    Log.v("COLOR", "frequency set to $frequency")

                }

            }

            updateColorDisplay()
            fsv.requestRender()

        }
        b.colorParamDisplay.colorSelector.apply {
            satValueSelector.onUpdateLinkedColor = { c ->
                val p = activeColorParam
                if (p is ColorAccent) {
                    p.updateColor(c)
                    updateColorLayout()
                    when (activeColorParam) {
                        ColorAccent.FILL -> {
                            f.color.fillColor = c
                            (b.colorParamMenu.fillColorButton.drawable as? GradientDrawable)?.setColor(c)
                        }
                        ColorAccent.OUTLINE -> {
                            f.color.outlineColor = c
                            (b.colorParamMenu.outlineColorButton.drawable as? GradientDrawable)?.setColor(c)
                        }
                    }
                    satValueSelector.invalidate()
                    fsv.requestRender()
                }
            }
            hueSelector.setSatValueSelectorView(satValueSelector)
        }
        updateColorLayout()
        bindColorParameter(ColorRealParam.FREQUENCY)

        b.customPaletteCreator.run {
            val customColorClickListener = { selectedItemIndex: Int, color: Int ->
                colorSelector.run {

                    satValueSelector.linkedColorIndex = selectedItemIndex
                    satValueSelector.setColor(color, updateLinkedColor = false)

                    val hsv = color.toHSV()
                    val h = satValueSelector.hue.let { if (it.isNaN()) 0 else it.roundToInt() }
                    val s = (100f * hsv[1]).let { if (it.isNaN()) 0 else it.roundToInt() }
                    val value = (100f * hsv[2]).let { if (it.isNaN()) 0 else it.roundToInt()}

                    hueEdit.setText("%d".format(h))
                    satEdit.setText("%d".format(s))
                    valEdit.setText("%d".format(value))

                }
            }
            customColorDragAdapter = CustomColorDragAdapter(
                customPalette.colors,
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

                        customPalette.colors.add(toPosition, customPalette.colors.removeAt(fromPosition))
                        customPalette.updateFlatPalette()
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

//            customPaletteName.setOnEditorActionListener(editListener(null) { w: TextView ->
//                customPalette.name = w.text.toString()
//            })
            colorSelector.apply {

                satValueSelector.apply {
                    onUpdateLinkedColor = { newColor: Int ->  // UPDATE ACTIVE COLOR

                        customPalette.colors[linkedColorIndex] = newColor
                        customPalette.updateFlatPalette()
                        customColorDragAdapter.updateColor(linkedColorIndex, newColor)

                        val hsv = newColor.toHSV()
                        val h = hue.let { if (it.isNaN()) 0 else it.roundToInt() }
                        val s = (100f * hsv[1]).let { if (it.isNaN()) 0 else it.roundToInt() }
                        val value = (100f * hsv[2]).let { if (it.isNaN()) 0 else it.roundToInt()}

                        hueEdit.setText("%d".format(h))
                        satEdit.setText("%d".format(s))
                        valEdit.setText("%d".format(value))

                        invalidate()
                        fsv.requestRender()

                    }
                }
                hueSelector.setSatValueSelectorView(satValueSelector)

                hueEdit.setOnEditorActionListener(editListener(satEdit) { w: TextView ->
                    val result = w.text.toString().formatToDouble()?.toFloat()
                    if (result != null) hueSelector.setHue(result)
                    w.text = "%d".format(satValueSelector.hue.roundToInt())
                })
                satEdit.setOnEditorActionListener(editListener(valEdit) { w: TextView ->
                    val result = w.text.toString().formatToDouble()?.toFloat()
                    if (result != null) satValueSelector.setSat(result/100f)
                    val s = (100f * satValueSelector.sat).let { if (it.isNaN()) 0 else it.roundToInt() }
                    w.text = "%d".format(s)
                })
                valEdit.setOnEditorActionListener(editListener(null) { w: TextView ->
                    val result = w.text.toString().formatToDouble()?.toFloat()
                    if (result != null) satValueSelector.setVal(result/100f)
                    val value = (100f * satValueSelector.value).let { if (it.isNaN()) 0 else it.roundToInt() }
                    w.text = "%d".format(value)
                })

            }
            randomizeButton.setOnClickListener {
                val newColors = Palette.generateColors(customPalette.colors.size)
                customColorDragAdapter.updateColors(newColors)
                customPalette.colors = newColors
                customPalette.updateFlatPalette()
                fsv.requestRender()
            }
            editNameButton.setOnClickListener {
                val dialogView = AlertDialogEditNameBinding.inflate(layoutInflater, null, false)
                dialogView.name.setText(customPalette.name)
                AlertDialog.Builder(this@MainActivity, R.style.AlertDialogCustom)
                    .setView(dialogView.root)
                    .setTitle(R.string.name)
                    .setIcon(R.drawable.edit_name)
                    .setPositiveButton(android.R.string.ok) { di, i ->
                        customPalette.name = dialogView.name.text.toString()
                    }
                    .create()
                    .showImmersive(dialogView.root)
            }
            addColorButton.setOnClickListener {

                if (customPalette.colors.size == Palette.MAX_CUSTOM_COLORS_FREE && !sc.goldEnabled) showUpgradeScreen()
                else {
                    customColorDragAdapter.run {
                        if (customPalette.colors.size < Palette.MAX_CUSTOM_COLORS_GOLD) {

                            val newColor = randomColor()
                            customPalette.colors.add(selectedItemIndex + 1, newColor)
                            addItem(selectedItemIndex + 1, Pair(nextUniqueId, newColor))
                            // itemList.add(Pair(getNextUniqueId(), newColor))
                            // notifyItemInserted(itemCount)

                            customPalette.updateFlatPalette()
                            fsv.requestRender()

                            when (customPalette.colors.size) {
                                Palette.MAX_CUSTOM_COLORS_FREE -> {
                                    if (!sc.goldEnabled) addColorButton.showGradient = true
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

                    customPalette.colors.removeAt(selectedItemIndex)
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
                    customPalette.updateFlatPalette()
                    fsv.requestRender()

                    when (customPalette.colors.size) {
                        Palette.MAX_CUSTOM_COLORS_GOLD - 1 -> addColorButton.enable()
                        Palette.MAX_CUSTOM_COLORS_FREE - 1 -> {
                            if (!sc.goldEnabled) addColorButton.showGradient = false
                            removeColorButton.disable()
                        }
                    }

                }

            }
        }





        b.textureShapeParamDisplay.run {

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

                    val param = activeParam()
                    param.setValueFromProgress(progress.toDouble()/seekBar.max.toDouble())
                    updateShapeDisplayValues()

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
//                    crashlytics().setCustomKey(CRASH_KEY_MAX_ITER, f.shape.params.detail.u.toInt())
//                    b.maxIterValue.setText("%d".format(f.shape.params.detail.u.toInt()))

                }
            })

        }


        /* SHAPE */

        var equationLayoutMinimized = true
        val shapeParamSelector = ParamSelector(this, R.drawable.menu_button_highlight, b.shapeParamMenu.run { listOf(detailButton, juliaParamButton, seedParamButton, shapeParamButton1, shapeParamButton2, shapeParamButton3, shapeParamButton4) })
        val setShapeParamListeners = { button: AppCompatImageButton, p: () -> RealParam ->
            button.setOnClickListener {
                val param = p()
                if (param.goldFeature && !sc.goldEnabled) showUpgradeScreen()
                else {
                    f.shape.params.active = param
                    bindShapeParameter(param)
                    shapeParamSelector.select(button)
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

        b.shapeParamMenu.apply {
            listOf(shapeParamButton1, shapeParamButton2, shapeParamButton3, shapeParamButton4).forEachIndexed { i, button ->
                setShapeParamListeners(button) { f.shape.params.at(i) }
            }
            setShapeParamListeners(detailButton)     { f.shape.params.detail }
            setShapeParamListeners(juliaParamButton) { f.shape.params.julia  }
            setShapeParamListeners(seedParamButton)  { f.shape.params.seed   }
        }
        b.shapeListButton.setOnClickListener {

            prevShape = f.shape

            crashlytics().updateLastAction(Action.SHAPE_CHANGE)
            setUiState(UiState.EDITMODE_LIST)

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
        b.juliaModeButton.setOnClickListener {
            if (f.shape != Shape.mandelbrot && !sc.goldEnabled) showUpgradeScreen()
            else {

                fsv.r.checkThresholdCross { f.shape.juliaMode = b.juliaModeButton.isChecked }

                b.shapeParamMenu.apply {
                    if (f.shape.juliaMode) {

                        seedParamButton.hide()
                        juliaParamButton.show()
                        juliaParamButton.performClick()

//                        if (f.shape.numParamsInUse == 1) {
//                            sc.editMode = EditMode.SHAPE
//                        }

                    } else {

                        if (f.shape.params.active == f.shape.params.julia) {
                            detailButton.performClick()
                        }
                        if (f.shape.juliaSeed) seedParamButton.hide() else seedParamButton.show()
                        juliaParamButton.hide()

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
        b.juliaModeButton.setOnLongClickListener {
            if (BuildConfig.DEV_VERSION) {
                if (f.shape.juliaMode) {
                    f.shape.positions.main.x = f.shape.params.julia.u
                    f.shape.positions.main.y = f.shape.params.julia.v
                } else {
                    f.shape.params.julia.setFrom(ComplexParam(f.shape.position.x, f.shape.position.y))
                }
                b.juliaModeButton.performClick()
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
                shapeMathQuill.getLatex(customShape)
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
                    shapeMathQuill.getLatex(customShape)
                    true
                } else false
            }

            zKey.setOnClickListener(keyListener(Expr.z))
            cKey.setOnClickListener(keyListener(Expr.c))
            prevKey.setOnClickListener { shapeMathQuill.enterKeystroke(Keystroke.LEFT) }
            nextKey.setOnClickListener { shapeMathQuill.enterKeystroke(Keystroke.RIGHT) }
            deleteKey.setOnClickListener {
                shapeMathQuill.enterKeystroke(Keystroke.BACKSPACE)
                shapeMathQuill.getLatex(customShape)
            }
            leftParenKey.setOnClickListener(keyListener(Expr.leftParen))
            rightParenKey.setOnClickListener(keyListener(Expr.rightParen))

        }
        updateShapeLayout()
        bindShapeParameter(f.shape.params.detail)



        /* TEXTURE */

        val textureParamSelector = ParamSelector(this, R.drawable.menu_button_highlight, b.textureParamMenu.run { listOf(escapeRadiusButton, textureImageButton, textureParamButton1, textureParamButton2, textureParamButton3, textureParamButton4) })
        val setTextureParamListeners = { button: AppCompatImageButton, p: () -> RealParam ->
            button.setOnClickListener {
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
                    textureParamSelector.select(button)
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

        b.textureParamMenu.apply {
            listOf(textureParamButton1, textureParamButton2, textureParamButton3, textureParamButton4).forEachIndexed { i, button ->
                setTextureParamListeners(button) { f.texture.params.at(i) }
            }
            setTextureParamListeners(escapeRadiusButton) { f.texture.params.radius }
            textureImageButton.setOnClickListener {
                EditMode.TEXTURE.paramDisplayLayout = b.textureImageDisplay.root
                textureParamSelector.select(textureImageButton)
                b.textureShapeParamDisplay.root.hide()
                b.textureImageDisplay.root.show()
            }
        }
        b.textureListButton.setOnClickListener {

            prevTexture = f.texture

            crashlytics().updateLastAction(Action.TEXTURE_CHANGE)
            setUiState(UiState.EDITMODE_LIST)

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
        b.textureRegionButton.setOnClickListener {
            f.textureRegion = TextureRegion.values()[(TextureRegion.values().indexOf(f.textureRegion) + 1) % 3]
            b.textureRegionButton.setImageResource(f.textureRegion.iconId)
            fsv.requestRender()
        }
        updateTextureLayout()
        bindTextureParameter(f.texture.params.radius)




//        EditMode.POSITION.paramMenuLayout = b.positionParamMenu
//        EditMode.POSITION.paramMenuLayout = b.positionParamMenu
//        EditMode.POSITION.paramMenuLayout = b.positionParamMenu



        settingsFragment = SettingsFragment()
        // videoFragment = VideoFragment()

        // b.highlightWindow.hide()


        onAspectRatioChanged()

        b.prevParamButton.setOnClickListener {
            when (sc.editMode) {
                EditMode.POSITION -> positionParamSelector .prev()
                EditMode.COLOR    -> colorParamSelector    .prev()
                EditMode.SHAPE    -> shapeParamSelector    .prev()
                EditMode.TEXTURE  -> textureParamSelector  .prev()
                EditMode.NONE     -> {}
            }
        }
        b.nextParamButton.setOnClickListener {
            when (sc.editMode) {
                EditMode.POSITION -> positionParamSelector .next()
                EditMode.COLOR    -> colorParamSelector    .next()
                EditMode.SHAPE    -> shapeParamSelector    .next()
                EditMode.TEXTURE  -> textureParamSelector  .next()
                EditMode.NONE     -> {}
            }
        }

        b.uiToggleButton.setOnClickListener {
            if (b.editModeButtonLayout.isVisible) hideAllUi() else showAllUi()
        }
        b.paramMenuToggleButton.setOnClickListener {

            if (b.paramMenu.isVisible) { // open --> closed

                b.paramMenuDivider.hide()
                b.paramMenu.hide()

//                activePositionParam = PositionParam.ZOOM
//                positionParamSelector.select(b.positionParamMenu.zoomButton)
//                bindColorParameter(ColorRealParam.FREQUENCY)
//                colorParamSelector.select(b.colorParamMenu.frequencyButton)

//                if (!sc.editMode.alwaysDisplayParam) b.paramDisplay.hide()

            } else { // closed --> open

                b.paramMenuDivider.show()
                b.paramMenu.show()
//                if (!sc.editMode.alwaysDisplayParam) b.paramDisplay.show()

            }

            sc.editMode.run {
                updateAdjust()
                updateDisplay()
            }

        }

        b.tutorialWindow.nextButton.setOnClickListener { basicsTutorial.next(b.tutorialWindow.root) }
        b.tutorialWindow.finishButton.setOnClickListener { basicsTutorial.finish(b.tutorialWindow.root) }

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

                // setUiState(UiState.VIDEO)

                val v = DialogZoomVideoBinding.inflate(layoutInflater)
                v.run {
                    initialZoomValue.setOnEditorActionListener(editListener(finalZoomValue) { w ->
                        val result = w.text.toString().formatToDouble()
                        result?.let { fsv.video.keyframes[0].f.position?.zoom = 10.0.pow(-it) }
                        w.setText(PositionParam.ZOOM.toDisplayFormat(fsv.video.keyframes[0].f.position!!))
                    })
                    finalZoomValue.setOnEditorActionListener(editListener(durationValue) { w ->
                        val result = w.text.toString().formatToDouble()
                        result?.let { fsv.video.keyframes[1].f.position?.zoom = 10.0.pow(-it) }
                        w.setText(PositionParam.ZOOM.toDisplayFormat(fsv.video.keyframes[1].f.position!!))
                    })
                    durationValue.setOnEditorActionListener(editListener(null) { w ->
                        val result = w.text.toString().formatToDouble()
                        if (result != null) fsv.video.transitions[0].duration =
                            result.clamp(5.0, 60.0)
                        w.setText(fsv.video.transitions[0].duration.toString())
                    })
                }

                fsv.video.run {

                    val prevZoom = f.shape.position.zoom
                    f.shape.position.zoom = 10.0.pow(0.75)
                    addKeyframe(Video.Keyframe(f.bookmark(fsv)))
                    v.initialZoomValue.setText((-0.75).toString())

                    f.shape.position.zoom = prevZoom
                    addKeyframe(Video.Keyframe(f.bookmark(fsv)))
                    v.finalZoomValue.setText(PositionParam.ZOOM.toDisplayFormat(f.shape.position))

                    addTransition(Video.Transition())
                    v.durationValue.setText(transitions[0].duration.toString())

                }

                AlertDialog.Builder(this@MainActivity, R.style.AlertDialogCustom)
                    .setView(v.root)
                    .setTitle(R.string.new_video)
                    .setIcon(R.drawable.video)
                    .setPositiveButton(R.string.render) { _, _ ->
                        hideAllUi()
                        b.uiToggleButton.hide()
                        fsv.saveVideo()
                    }
                    .setNegativeButton(R.string.cancel) { _, _ ->

                    }
                    .create()
                    .showImmersive(v.root)


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
            val resolutionParamList = ParamSelector(this@MainActivity, R.drawable.edit_mode_button_highlight, resolutionButtons).also { it.select(resolutionButtons[Resolution.foregrounds.indexOf(sc.resolution)]) }
            val resolutionButtonListener = { button: Button, res: Resolution -> View.OnClickListener {

                if (sc.resolution != res) {

                    crashlytics().updateLastAction(Action.RESOLUTION_CHANGE)

                    if (res.goldFeature && !sc.goldEnabled) showUpgradeScreen()
                    else {

                        resolutionParamList.select(button)

                        // save state on resolution increase
                        if (res.w > sc.resolution.w) bookmarkAsPreviousFractal()

                        sc.resolution = res
                        crashlytics().setCustomKey(CRASH_KEY_RESOLUTION, sc.resolution.toString())
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
            val aspectParamList = ParamSelector(this@MainActivity, R.drawable.edit_mode_button_highlight, aspectButtons)
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
//        b.highlightWindow.hide()


        // need both?
        hideKeyboard(window, b.root)
        // window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        queryTutorialOption()
        crashlytics().setCustomKey(CRASH_KEY_ACT_MAIN_CREATED, true)

        Log.e("MAIN", "onCreate took ${currentTimeMs() - onCreateStartTime} ms")

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
        edit.putBoolean(GOLD_ENABLED_DIALOG_SHOWN, goldEnabledDialogShown)
        edit.putBoolean(GOLD_PENDING_DIALOG_SHOWN, goldPendingDialogShown)
        edit.putBoolean(SHOW_EPILEPSY_DIALOG, showEpilepsyDialog)
        edit.putBoolean(SHOW_TUTORIAL_OPTION, showTutorialOption)
        edit.putInt(RESOLUTION, min(Resolution.foregrounds.indexOf(sc.resolution), if (sc.goldEnabled) Resolution.foregrounds.indexOf(Resolution.SCREEN) else Resolution.foregrounds.indexOf(Resolution.R1080)))
        edit.putInt(TARGET_FRAMERATE, sc.targetFramerate)
        edit.putInt(ASPECT_RATIO, AspectRatio.all.indexOf(sc.aspectRatio))
        edit.putBoolean(CONTINUOUS_RENDER, sc.continuousPosRender)
        edit.putBoolean(RENDER_BACKGROUND, sc.renderBackground)
        edit.putBoolean(RESTRICT_PARAMS, sc.restrictParams)
        edit.putBoolean(FIT_TO_VIEWPORT, sc.fitToViewport)
        edit.putBoolean(HIDE_NAV_BAR, sc.hideSystemBars)
        edit.putInt(COLOR_LIST_VIEW_TYPE, sc.colorListViewType.ordinal)
        edit.putInt(SHAPE_LIST_VIEW_TYPE, sc.shapeListViewType.ordinal)
        edit.putInt(TEXTURE_LIST_VIEW_TYPE, sc.textureListViewType.ordinal)
        edit.putInt(BOOKMARK_LIST_VIEW_TYPE, sc.bookmarkListViewType.ordinal)
        edit.putBoolean(AUTOFIT_COLOR_RANGE, sc.autofitColorRange)
        edit.putInt(VERSION_CODE_TAG, BuildConfig.VERSION_CODE)
        edit.putBoolean(USE_ALTERNATE_SPLIT, sc.useAlternateSplit)
        edit.putBoolean(ALLOW_SLOW_DUALFLOAT, sc.allowSlowDualfloat)
        edit.putInt(CHUNK_PROFILE, sc.chunkProfile.ordinal)
        edit.putBoolean(ADVANCED_SETTINGS, sc.advancedSettingsEnabled)
        edit.putBoolean(ULTRA_HIGH_RES, sc.ultraHighResolutions)

        edit.putInt(PALETTE, f.palette.id)
        edit.putInt(ACCENT_COLOR1, f.color.fillColor)
        edit.putInt(ACCENT_COLOR2, f.color.outlineColor)

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
                    saveImage()
                } else {
                    showMessage(resources.getString(R.string.msg_save_failed))
                }
                return
            }
            else -> {}
        }
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

        val storedVersion = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE).getInt(VERSION_CODE_TAG, 0)
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
            f.palette = Palette.all.find { it.id == sp.getInt(PALETTE, Palette.eye.id) } ?: Palette.eye
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
                        .putInt(PREV_FRACTAL_ID, Fractal.previous.customId)
                        .putBoolean(PREV_FRACTAL_CREATED, true)
                        .apply()
                }

                getAll().forEach { e ->

                    val shape = Shape.all.find { it.id == e.shapeId } ?: Shape.mandelbrot
                    Log.e("MAIN", "shape: ${shape.name}")
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

            Log.e("MAIN", "database operations took ${currentTimeMs() - t} ms")

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
            fsv.requestRender()

            item.run {
                savedCustomPaletteName = name
                savedCustomColors = ArrayList(colors)
            }

            customPalette = item
            customColorDragAdapter.updateColors(customPalette.colors)
//            customColorDragAdapter.apply { linkColor(0, itemList[0].second) }
//            b.customPaletteCreator.customPaletteName.setText(customPalette.name)

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

                    customPalette = t.clone(resources)

                    f.palette = customPalette
                    fsv.requestRender()

                    customColorDragAdapter.updateColors(customPalette.colors)
//                customColorDragAdapter.apply { linkColor(0, itemList[0].second) }
//                b.customPaletteCreator.customPaletteName.setText(customPalette.name)

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

            // save values in case of cancel
            item.run {
                savedCustomPaletteName = name
                savedCustomLatex = latex
                savedCustomLoopSingle = customLoopSingle
                savedCustomLoopDual = customLoopDual
            }

            customShape = item
            fsv.r.checkThresholdCross {
                f.shape = customShape
                // f.texture = if (f.shape.isConvergent) Texture.converge else Texture.escapeSmooth
                // f.shape.reset()
                f.shape.position.reset()
            }

            b.customShapeCreator.apply {
//                customShapeName.setText(customShape.name)
                shapeMathQuill.setLatex(customShape.latex)
                shapeMathQuill.setCustomLoop(customShape.latex, customShape)
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

                    customShape = t.clone(resources)

                    b.customShapeCreator.apply {
                        shapeMathQuill.setLatex(customShape.latex)
                        shapeMathQuill.setCustomLoop(customShape.latex, customShape)
                    }

                    f.shape = customShape
                    f.texture = Texture.escapeSmooth

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

        Log.e("TEXTURE", "sampleSize: $sampleSize")
        val inputStream = contentResolver?.openInputStream(uri)
        val bmp = BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = sampleSize
        })
        inputStream?.close()
        Log.e("TEXTURE", "width: ${bmp?.width}, height: ${bmp?.height}")

        // make local copy of image
        val path = "$TEX_IM_PREFIX${Texture.CUSTOM_IMAGE_COUNT}.png"
        val fos = openFileOutput(path, Context.MODE_PRIVATE)
        bmp?.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.close()

        Texture.CUSTOM_IMAGE_COUNT++
        val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        val edit = sp.edit()
        edit.putInt(TEX_IMAGE_COUNT, Texture.CUSTOM_IMAGE_COUNT)
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

    private fun setUiState(newState: UiState, useless: Boolean = false) {
        if (newState != uiState) {

            val oldState = uiState
            uiState = newState

            // on exit
            when (oldState) {
                UiState.HOME -> {
                    sc.editMode.utilityButtons?.hide()
                    b.paramMenuToggleButton.hide()
                    b.editModeButtonLayout.hide()
                }
                UiState.EDITMODE_LIST -> {
                    sc.editMode.listLayout?.hide()
                    b.uiNavButtons.hide()
                    b.actionNew.hide()
                    showMainUi()
                }
                UiState.BOOKMARK_LIST -> {
                    b.bookmarkList.root.hide()
                    b.uiNavButtons.hide()
                    b.actionCancel.hide()
                    showMainUi()
                }
                UiState.CUSTOM_PALETTE -> {
                    showMainUi()
                    b.customPaletteCreator.root.hide()
                    b.uiNavButtons.hide()
                    b.actionCancel.hide()
                }
                UiState.CUSTOM_SHAPE -> {
                    showMainUi()
                    b.customShapeCreator.root.hide()
                    b.uiNavButtons.hide()
                    b.actionCancel.hide()
                    b.actionDone.showGradient = false
                }
                UiState.VIDEO -> {
                    showMainUi()
                    b.uiNavButtons.hide()
                    b.videoActions.hide()
                    b.videoFragmentContainer.hide()
                }
                UiState.RANDOMIZER -> {
                    showMainUi()
                    b.randomizer.root.hide()
                    b.uiNavButtons.hide()
                    b.actionRandomize.hide()
                }
            }

            // on enter
            when (newState) {
                UiState.HOME -> {
                    b.paramMenuToggleButton.show()
                    b.editModeButtonLayout.show()
                    sc.editMode.utilityButtons?.show()
                }
                UiState.EDITMODE_LIST -> {
                    hideMainUi()
                    b.uiNavButtons.show()
                    when (sc.editMode) {
                        EditMode.COLOR -> {
                            b.actionNew.show()
                            fsv.r.renderProfile = RenderProfile.COLOR_THUMB
                            if (!sc.goldEnabled && Palette.custom.size == Palette.MAX_CUSTOM_PALETTES_FREE) {
                                b.actionNew.showGradient = true
                            }
                        }
                        EditMode.TEXTURE -> {

                        }
                        else -> {
                            b.actionNew.show()
                        }
                    }
                    sc.editMode.listLayout?.show()
                }
                UiState.BOOKMARK_LIST -> {
                    hideMainUi()
                    b.uiNavButtons.show()
                    b.actionCancel.show()
                    b.bookmarkList.root.show()
                }
                UiState.CUSTOM_PALETTE -> {
                    hideMainUi()
                    b.customPaletteCreator.root.show()
                    b.uiNavButtons.show()
                    b.actionNew.hide()
                    b.actionCancel.show()
                }
                UiState.CUSTOM_SHAPE -> {
                    hideMainUi()
                    b.customShapeCreator.root.show()
                    b.uiNavButtons.show()
                    b.actionNew.hide()
                    b.actionCancel.show()
                    if (!sc.goldEnabled) b.actionDone.showGradient = true
                }
                UiState.VIDEO -> {
                    hideMainUi()
                    b.uiNavButtons.show()
                    b.videoActions.show()
                    b.videoFragmentContainer.show()
                }
                UiState.RANDOMIZER -> {
                    hideMainUi()
                    b.randomizer.root.show()
                    b.uiNavButtons.show()
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
            label.setText(activePositionParam.nameId)
            value.setText(activePositionParam.toDisplayFormat(f.shape.position))
            if (activePositionParam == PositionParam.ROTATION) rotationLock.show() else rotationLock.hide()
        }
    }

    private fun updateGradient() {
//        (b.paletteListButton.drawable as GradientDrawable).run {
//            colors = f.palette.colors.toIntArray()
//        }
        b.paletteListButtonGradient.background = f.palette.gradientDrawable
    }
    private fun updateColorLayout() {
        b.colorParamMenu.outlineColorButton.run { if (f.texture.usesAccent) show() else hide() }
        b.colorAutofitButton.isChecked = sc.autofitColorRange
        if (sc.autofitColorRange) {
            b.colorParamMenu.densityButton.run { if (f.texture.usesDensity) show() else hide() }
        }
        updateGradient()
    }
    private fun bindColorParameter(p: ColorParam) {
        activeColorParam = p
        b.colorParamDisplay.run {
            if (p is ColorRealParam) {

                colorValueDisplay.show()
                colorAccentDisplay.hide()
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
                sensitivityButton.run { param = p }
                decreaseButton.setOnClickListener(p.onDecreaseClick)
                decreaseButton.setOnLongClickListener(p.onDecreaseLongClick)
                increaseButton.setOnClickListener(p.onIncreaseClick)
                increaseButton.setOnLongClickListener(p.onIncreaseLongClick)

            }
            else if (p is ColorAccent) {
                icon.setImageResource(p.iconId)
                accentLabel.setText(p.nameId)
                colorValueDisplay.hide()
                colorAccentDisplay.show()
            }
        }
        updateColorDisplay()
    }
    private fun updateColorDisplay() {
        b.colorParamDisplay.run {
            val p = activeColorParam
            if (p is ColorRealParam) {
                value.setText(p.toDisplayFormat(f.color))
                seekBar.progress = (p.toProgress(f.color)*seekBar.max).toInt()
            }
            else if (p is ColorAccent) {
                colorSelector.satValueSelector.setColor(p.color())
            }
        }
    }
    private fun updateColorAdjust() {
        val p = activeColorParam
        if (p is ColorAccent) {

        } else if (p is ColorRealParam) {
            b.colorParamDisplay.run {

                root.show()

                decreaseButton.setImageResource(R.drawable.param_decrease)
                decreaseButton.setOnClickListener(p.onDecreaseClick)
                decreaseButton.setOnLongClickListener(p.onDecreaseLongClick)

                increaseButton.setImageResource(R.drawable.param_increase)
                increaseButton.setOnClickListener(p.onIncreaseClick)
                increaseButton.setOnLongClickListener(p.onIncreaseLongClick)

            }
        }
    }

    private fun updateShapeLayout() {
        b.shapeParamMenu.apply {
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
                shapeParamButtonList[index].apply {
                    if (!param.devFeature || BuildConfig.DEV_VERSION) show()
                    showGradient = param.goldFeature && !sc.goldEnabled
                    setImageResource(param.iconId)
                }
            }


            // update juliaModeButton

            if (!f.shape.isCustom() && f.shape.juliaModeInit) {
                b.juliaModeButton.hide()
                // juliaDivider.hide()
            } else {
                b.juliaModeButton.show()
                // juliaDivider.show()
            }

            if (f.shape != Shape.mandelbrot && !sc.goldEnabled) {
                b.juliaModeButton.showGradient = true
                b.juliaModeButton.drawable?.alpha = 255
            } else {
                b.juliaModeButton.showGradient = false
                b.juliaModeButton.drawable?.alpha = 127
            }

            b.juliaModeButton.apply {
                isChecked = f.shape.juliaMode
                if (isChecked) juliaParamButton.show() else juliaParamButton.hide()
            }
            if (f.shape.juliaMode || f.shape.juliaSeed) seedParamButton.hide() else seedParamButton.show()

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
                    showGradient = param.goldFeature && !sc.goldEnabled
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
    private fun updateTextureListItems() {
        compatTexturesChanged = false
        if (listAdaptersInitialized) textureLists.updateDataset(ArrayList(f.shape.compatTextures), arrayListOf())
    }
    private fun bindParameter(p: RealParam){
        b.textureShapeParamDisplay.run {
            if (p is ComplexParam) {
                realParamDisplay.hide()
                complexParamDisplay.show()
                complexParamIcon.setImageResource(p.iconId)
                complexParamName.text = p.name
                sensitivity.sensitivityValue.setText(p.sensitivity.toInt().toString())
            } else {
                complexParamDisplay.hide()
                realParamDisplay.show()
                realParamIcon.setImageResource(p.iconId)
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
    private fun updateShapeDisplayValues() {
        val active = activeParam()
        b.textureShapeParamDisplay.run {
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
            b.textureShapeParamDisplay.run {
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
        b.run {
            listOf(
                extrasMenuButton,
                editModeButtonLayout,
                utilityButtons,
                paramDisplay,
                uiToggleButton
            ).forEach { it.show() }
        }
    }
    private fun hideMainUi() {
        b.run {
            if (paramMenu.isVisible) paramMenuToggleButton.performClick()
            listOf(
                extrasMenuButton,
                editModeButtonLayout,
                utilityButtons,
                paramDisplay,
                uiToggleButton
            ).forEach { it.hide() }
        }
    }

    private fun hideAllUi() {
        b.run {
            listOf(
                progressBar,
                extrasMenuButton,
                editModeButtonLayout,
                utilityButtons,
                paramDisplay,
                saveImageButton,
                paramMenuToggleButton
            ).forEach { it.hide() }
            uiToggleButton.setImageResource(R.drawable.hidden)
        }
    }
    private fun showAllUi() {
        b.run {
            listOf(
                progressBar,
                extrasMenuButton,
                editModeButtonLayout,
                utilityButtons,
                paramDisplay,
                saveImageButton,
                paramMenuToggleButton
            ).forEach { it.show() }
            uiToggleButton.setImageResource(R.drawable.visible)
        }
    }

    private fun showExtrasMenu() {
        if (b.paramMenu.isVisible) b.paramMenuToggleButton.performClick()
        b.extrasMenu.root.show()
        b.utilityButtons.hide()
        b.paramMenuToggleButton.hide()
    }
    private fun hideExtrasMenu() {
        b.extrasMenu.run {
            root.hide()
            resolutionLayout.hide()
            aspectRatioLayout.hide()
            optionsLayout.show()
            b.utilityButtons.show()
            b.paramMenuToggleButton.show()
        }
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
        crashlytics().setCustomKey(CRASH_KEY_GOLD_ENABLED, true)
        val sp = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        val edit = sp.edit()
        edit.putBoolean(GOLD_ENABLED_DIALOG_SHOWN, true)
        edit.apply()

        b.extrasMenu.logisticsOptions.removeView(b.extrasMenu.upgradeButton)
        b.extrasMenu.run {
            listOf(r1440, r2160, r2880, r3600, r4320, r5040, r5760, rCustomScreen).forEach { it.showGradient = false }
            listOf(aspect45, aspect57, aspect23, aspect916, aspect12).forEach { it.showGradient = false }
        }
        
        b.shapeParamMenu.run {
            listOf(shapeParamButton1, shapeParamButton2, shapeParamButton3, shapeParamButton4).forEach { v -> v.showGradient = false }
        }
        b.textureParamMenu.run {
            listOf(textureParamButton1, textureParamButton2, textureParamButton3, textureParamButton4).forEach { v -> v.showGradient = false }
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
    private fun shareImage() {

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



}
