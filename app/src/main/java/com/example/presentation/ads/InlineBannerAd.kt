package com.example.presentation.ads

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ads.AdMobManager
import com.example.ads.AdPlacement
import com.example.presentation.MainViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

sealed interface AdLoadStatus {
    object Idle : AdLoadStatus
    object Loading : AdLoadStatus
    object Loaded : AdLoadStatus
    data class Failed(val reason: String) : AdLoadStatus
}

@Composable
fun InlineBannerAd(
    viewModel: MainViewModel,
    placement: AdPlacement,
    modifier: Modifier = Modifier,
    adUnitId: String? = null
) {
    val adManager = viewModel.adManager
    val isInitialized by adManager.isInitialized.collectAsStateWithLifecycle()
    val isAdsEnabled by viewModel.adsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Scope check: Do not show ads during active focus, or if ads are disabled
    val isFocusing = uiState.sessionState == com.example.domain.model.SessionState.ACTIVE ||
            uiState.sessionState == com.example.domain.model.SessionState.CALIBRATING ||
            uiState.sessionState == com.example.domain.model.SessionState.MOVED_WARNING ||
            uiState.sessionState == com.example.domain.model.SessionState.RESTORED

    if (!adManager.isEligibleForAds(isInActiveFocusSession = isFocusing, isAdsEnabledByUser = isAdsEnabled)) {
        return
    }

    val context = LocalContext.current
    val effectiveAdUnitId = adUnitId ?: stringResource(R.string.admob_banner_test_unit_id)

    var adStatus by remember { mutableStateOf<AdLoadStatus>(AdLoadStatus.Loading) }
    var refreshTrigger by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("inline_banner_ad_${placement.placementId}"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Tag Bar: "SPONSORED" badge + discreet refresh button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "AD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = placement.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    if (adStatus is AdLoadStatus.Failed) {
                        IconButton(
                            onClick = {
                                adStatus = AdLoadStatus.Loading
                                refreshTrigger++
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry ad",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Ad Content Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Native AdView hosted inside AndroidView
                    if (isInitialized) {
                        key(refreshTrigger) {
                            AndroidView(
                                modifier = Modifier
                                    .wrapContentHeight()
                                    .testTag("admob_adview_${placement.placementId}"),
                                factory = { ctx ->
                                    AdView(ctx).apply {
                                        try {
                                            // Avoid GPU DRM rendernode errors on headless/software Mesa environments
                                            setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                                        } catch (_: Exception) {}
                                        setAdSize(AdSize.BANNER)
                                        setAdUnitId(effectiveAdUnitId)
                                        adListener = object : AdListener() {
                                            override fun onAdLoaded() {
                                                super.onAdLoaded()
                                                adStatus = AdLoadStatus.Loaded
                                                adManager.recordAdLoaded(placement)
                                            }

                                            override fun onAdFailedToLoad(error: LoadAdError) {
                                                super.onAdFailedToLoad(error)
                                                val detail = when (error.code) {
                                                    com.google.android.gms.ads.AdRequest.ERROR_CODE_NO_FILL -> "No Fill (Code 3) - Ad inventory pending"
                                                    com.google.android.gms.ads.AdRequest.ERROR_CODE_NETWORK_ERROR -> "Network error (Code 2)"
                                                    com.google.android.gms.ads.AdRequest.ERROR_CODE_INVALID_REQUEST -> "Invalid request (Code 1)"
                                                    com.google.android.gms.ads.AdRequest.ERROR_CODE_INTERNAL_ERROR -> "Internal error (Code 0)"
                                                    else -> error.message.take(45)
                                                }
                                                adStatus = AdLoadStatus.Failed(detail)
                                            }

                                            override fun onAdImpression() {
                                                super.onAdImpression()
                                                adManager.recordAdImpression(placement)
                                            }

                                            override fun onAdClicked() {
                                                super.onAdClicked()
                                                adManager.recordAdClick(placement)
                                            }
                                        }

                                        adManager.recordAdRequest()
                                        try {
                                            loadAd(AdRequest.Builder().build())
                                        } catch (e: Exception) {
                                            adStatus = AdLoadStatus.Failed("Init error")
                                        }
                                    }
                                },
                                update = { adView ->
                                    // Managed by factory and listener
                                },
                                onRelease = { adView ->
                                    try {
                                        adView.destroy()
                                    } catch (_: Exception) {}
                                }
                            )
                        }
                    }

                    // Loading indicator displayed only while waiting for ad load
                    if (!isInitialized || adStatus is AdLoadStatus.Loading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Loading sponsor message...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Error indicator displayed if ad load fails
                    if (adStatus is AdLoadStatus.Failed) {
                        val fail = adStatus as AdLoadStatus.Failed
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ad currently unavailable (${fail.reason})",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}
