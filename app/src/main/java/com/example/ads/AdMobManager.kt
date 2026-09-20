package com.example.ads

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.example.data.local.dataStore
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class AdMetrics(
    val impressions: Int = 0,
    val clicks: Int = 0,
    val adRequests: Int = 0,
    val fills: Int = 0,
    val estimatedRevenueMicros: Long = 0L // e.g. from paid event or simulated eCPM
) {
    val ctr: Float
        get() = if (impressions > 0) (clicks.toFloat() / impressions.toFloat()) * 100f else 0f

    val fillRate: Float
        get() = if (adRequests > 0) (fills.toFloat() / adRequests.toFloat()) * 100f else 0f

    val estimatedRevenueUsd: Double
        get() = estimatedRevenueMicros / 1_000_000.0
}

enum class AdPlacement(val placementId: String, val displayName: String) {
    FOREST_BOTTOM("forest_bottom", "Forest Screen Banner"),
    STATS_INLINE("stats_inline", "Analytics & Insights"),
    HISTORY_FOOTER("history_footer", "Session Archive Footer"),
    SETTINGS_FOOTER("settings_footer", "Preferences Banner")
}

class AdMobManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val BANNER_AD_UNIT_ID = "ca-app-pub-9576035225797145/3396629539"
        const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-9576035225797145/3396629539"

        private val KEY_ADS_ENABLED = booleanPreferencesKey("ads_enabled")
        private val KEY_IMPRESSIONS = intPreferencesKey("ad_impressions")
        private val KEY_CLICKS = intPreferencesKey("ad_clicks")
        private val KEY_REQUESTS = intPreferencesKey("ad_requests")
        private val KEY_FILLS = intPreferencesKey("ad_fills")
        private val KEY_REVENUE_MICROS = longPreferencesKey("ad_revenue_micros")
        private val KEY_PERSONALIZED_ADS = booleanPreferencesKey("ad_personalized_consent")
    }

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _metrics = MutableStateFlow(AdMetrics())
    val metrics: StateFlow<AdMetrics> = _metrics.asStateFlow()

    val adsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_ADS_ENABLED] ?: true }
    val personalizedConsentFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_PERSONALIZED_ADS] ?: true }

    // Rate limiting: placement to last impression timestamp
    private val lastImpressionTimes = mutableMapOf<AdPlacement, Long>()
    private val MIN_REFRESH_INTERVAL_MS = 30_000L // 30s ad frequency protection

    init {
        // Load initial metrics from local store
        scope.launch(Dispatchers.IO) {
            val prefs = context.dataStore.data.first()
            _metrics.value = AdMetrics(
                impressions = prefs[KEY_IMPRESSIONS] ?: 0,
                clicks = prefs[KEY_CLICKS] ?: 0,
                adRequests = prefs[KEY_REQUESTS] ?: 0,
                fills = prefs[KEY_FILLS] ?: 0,
                estimatedRevenueMicros = prefs[KEY_REVENUE_MICROS] ?: 0L
            )
        }

        // Initialize Google Mobile Ads SDK asynchronously on the main looper thread
        scope.launch(Dispatchers.Main) {
            try {
                MobileAds.initialize(context.applicationContext) { status ->
                    _isInitialized.value = true
                }
            } catch (e: Exception) {
                // MobileAds init fail safe
                _isInitialized.value = true
            }
        }
    }

    /**
     * Determine if a user and session state is currently eligible for banner ads.
     * Note: Ads are NEVER shown during active focus sessions to prevent distraction.
     */
    fun isEligibleForAds(isInActiveFocusSession: Boolean, isAdsEnabledByUser: Boolean): Boolean {
        if (isInActiveFocusSession) return false
        return isAdsEnabledByUser
    }

    /**
     * Check rate limiting per placement
     */
    fun canRequestAd(placement: AdPlacement): Boolean {
        val now = System.currentTimeMillis()
        val lastTime = lastImpressionTimes[placement] ?: 0L
        return (now - lastTime) >= MIN_REFRESH_INTERVAL_MS
    }

    fun recordAdRequest() {
        scope.launch(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                val curr = prefs[KEY_REQUESTS] ?: 0
                prefs[KEY_REQUESTS] = curr + 1
            }
            _metrics.value = _metrics.value.copy(adRequests = _metrics.value.adRequests + 1)
        }
    }

    fun recordAdLoaded(placement: AdPlacement) {
        lastImpressionTimes[placement] = System.currentTimeMillis()
        scope.launch(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                val curr = prefs[KEY_FILLS] ?: 0
                prefs[KEY_FILLS] = curr + 1
            }
            _metrics.value = _metrics.value.copy(fills = _metrics.value.fills + 1)
        }
    }

    fun recordAdImpression(placement: AdPlacement) {
        lastImpressionTimes[placement] = System.currentTimeMillis()
        scope.launch(Dispatchers.IO) {
            // Assume an average conservative eCPM benchmark for estimation ($1.50 eCPM = 1500 micros per impression)
            val simulatedImpressionRevenue = 1500L
            context.dataStore.edit { prefs ->
                val currImp = prefs[KEY_IMPRESSIONS] ?: 0
                val currRev = prefs[KEY_REVENUE_MICROS] ?: 0L
                prefs[KEY_IMPRESSIONS] = currImp + 1
                prefs[KEY_REVENUE_MICROS] = currRev + simulatedImpressionRevenue
            }
            _metrics.value = _metrics.value.copy(
                impressions = _metrics.value.impressions + 1,
                estimatedRevenueMicros = _metrics.value.estimatedRevenueMicros + simulatedImpressionRevenue
            )
        }
    }

    fun recordAdClick(placement: AdPlacement) {
        scope.launch(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                val curr = prefs[KEY_CLICKS] ?: 0
                prefs[KEY_CLICKS] = curr + 1
            }
            _metrics.value = _metrics.value.copy(clicks = _metrics.value.clicks + 1)
        }
    }

    suspend fun setAdsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ADS_ENABLED] = enabled }
    }

    suspend fun setPersonalizedConsent(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PERSONALIZED_ADS] = enabled }
    }

    suspend fun resetMetrics() {
        context.dataStore.edit { prefs ->
            prefs[KEY_IMPRESSIONS] = 0
            prefs[KEY_CLICKS] = 0
            prefs[KEY_REQUESTS] = 0
            prefs[KEY_FILLS] = 0
            prefs[KEY_REVENUE_MICROS] = 0L
        }
        _metrics.value = AdMetrics()
    }
}
