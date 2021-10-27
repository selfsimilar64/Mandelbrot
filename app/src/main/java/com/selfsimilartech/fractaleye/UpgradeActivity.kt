package com.selfsimilartech.fractaleye

import android.animation.LayoutTransition
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import com.selfsimilartech.fractaleye.databinding.ActivityUpgradeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList

class UpgradeActivity : AppCompatActivity() {

    lateinit var b : ActivityUpgradeBinding
    private lateinit var billingClient : BillingClient

    private val purchaseUpdateListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
//                        if (purchases != null) purchases[0]?.apply {
//                            Log.e("MAIN", "processing purchase...")
//                            Log.e("MAIN", originalJson)
//                            when (purchaseState) {
//                                Purchase.PurchaseState.PURCHASED -> {
//                                    finish()
//                                }
//                                Purchase.PurchaseState.PENDING -> {
//                                    finish()
//                                }
//                            }
//                        }
                        finish()
                    }
                    BillingClient.BillingResponseCode.USER_CANCELED -> {
                        // Handle an error caused by a user cancelling the purchase flow.
                    }
                    else -> {
                        // Handle any other error codes.
                    }
                }
            }


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        b = ActivityUpgradeBinding.inflate(layoutInflater)
        setContentView(b.root)


        billingClient = BillingClient.newBuilder(this)
                .setListener(purchaseUpdateListener)
                .enablePendingPurchases()
                .build()
        billingClient.startConnection(object : BillingClientStateListener {

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        Log.e("UPGRADE", "billing service setup finished")
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.e("UPGRADE", "!! BILLING SERVICE DISCONNECTED !!")
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }

        })


        b.imageLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

//        val timer = Timer()
//
//        val proShapes = Shape.default.filter { it.proFeature }.shuffled()
//        var shapeIndex = 0
//        val numProShapesRounded = proShapes.size - proShapes.size % 10
//        numProShapesText.text = numProShapesText.text.toString().format(numProShapesRounded)
//        timer.scheduleAtFixedRate(object : TimerTask() {
//            override fun run() {
//                runOnUiThread {
//                    proShapeIcon.animate().alpha(0f).withEndAction {
//                        proShapeIcon.setImageResource(proShapes[shapeIndex].thumbnailId)
//                        proShapeIcon.animate().alpha(1f).start()
//                    }.start()
//                    shapeIndex = (shapeIndex + 1) % proShapes.size
//                }
//            }
//        }, 2000L, 3500L)
//
//        val displayMetrics = baseContext.resources.displayMetrics
//        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
//        val screenWidth = displayMetrics.widthPixels
//        val screenHeight = displayMetrics.heightPixels
//        val screenRes = Point(screenWidth, screenHeight)
//        val highestResDims = Resolution.R2880.size
//        resolutionDiffText2.text = resolutionDiffText2.text.toString().format(highestResDims.x, highestResDims.y)

        b.upgradeNoThanksButton.setOnClickListener {
            finish()
        }
        b.upgradeConfirmButton.setOnClickListener {
            lifecycleScope.launch { launchUpgradeFlow() }
            //finish()
        }


        Glide.with(this).apply {
            setDefaultRequestOptions(
                    RequestOptions()
                            .format(DecodeFormat.PREFER_RGB_565)
                            .downsample(DownsampleStrategy.AT_MOST)
            )
            load( ResourcesCompat.getDrawable(resources, R.drawable.pro_header, null)).into(b.headerImage)
            load( R.drawable.creativity_collage     ).into(b.creativityImage)
            load( R.drawable.customization_collage  ).into(b.customizationImage)
            load( R.drawable.upgrade_resolution     ).into(b.resolutionImage)
        }


        b.upgradeCustomizationText1.text = resources.getString(R.string.upgrade_customization_content1).format(Shape.all.filter { it.goldFeature }.size)
        b.upgradeCustomizationText2.text = resources.getString(R.string.upgrade_customization_content2).format(Texture.all.filter { it.goldFeature }.size)
        b.upgradeCustomizationText3.text = resources.getString(R.string.upgrade_customization_content3).format(
                Shape.all.sumBy { shape ->
                    if (shape!!.goldFeature) shape.params.list.size + 1
                    else                    shape.params.list.filter { it.goldFeature }.size + 1
                } + Texture.all.sumBy { texture ->
                    if (texture.goldFeature) texture.params.size
                    else                     texture.params.list.filter { it.goldFeature }.size
                }
        )

        b.goldResolutionLabel.showGradient = true
        b.goldResolutionDims.showGradient = true
        b.freeResolutionDims.text = "%d x %d".format(Resolution.MAX_FREE.w, Resolution.MAX_FREE.h)
        b.goldResolutionDims.text = "%d x %d".format(Resolution.R2880.w, Resolution.R2880.h)

    }


    private suspend fun querySkuDetails() : SkuDetailsResult {
        val skuList = ArrayList<String>()
        skuList.add("gold_upgrade")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        return withContext(Dispatchers.IO) {
            billingClient.querySkuDetails(params.build())
        }
    }
    private suspend fun launchUpgradeFlow() {

        // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
        val skuDetailsList = querySkuDetails().skuDetailsList
        if (skuDetailsList != null && skuDetailsList.isNotEmpty()) {
            val flowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetailsList[0])
                    .build()
            val responseCode = billingClient.launchBillingFlow(this, flowParams).responseCode
            Log.e("UPGRADE", "response code: $responseCode")
        }
        else {
            // not connected to internet, debug version, etc.
            runOnUiThread {
                AlertDialog.Builder(this, R.style.AlertDialogCustom)
                        .setIcon(R.drawable.warning)
                        .setMessage(R.string.upgrade_error)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
            }
        }

    }

}