package com.example

import android.app.Application
import com.example.camera.PlacementCameraManager
import com.example.data.local.DataStoreManager
import com.example.data.local.FocusForestDatabase
import com.example.data.repository.FocusRepositoryImpl
import com.example.domain.repository.FocusRepository
import com.example.lock.FocusLockManager
import com.example.placement.MovementDetectionManager
import com.example.sensors.OrientationSensorManager
import com.example.timer.TimerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class FocusForestApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: FocusForestDatabase
        private set

    lateinit var repository: FocusRepository
        private set

    lateinit var dataStoreManager: DataStoreManager
        private set

    lateinit var sensorManager: OrientationSensorManager
        private set

    lateinit var cameraManager: PlacementCameraManager
        private set

    lateinit var movementManager: MovementDetectionManager
        private set

    lateinit var timerEngine: TimerEngine
        private set

    lateinit var lockManager: FocusLockManager
        private set

    lateinit var soundManager: com.example.audio.SoundFeedbackManager
        private set

    lateinit var authManager: com.example.auth.FirebaseAuthManager
        private set

    lateinit var adManager: com.example.ads.AdMobManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = FocusForestDatabase.getInstance(this)
        repository = FocusRepositoryImpl(database.focusDao())
        dataStoreManager = DataStoreManager(this)

        authManager = com.example.auth.FirebaseAuthManager(this)
        adManager = com.example.ads.AdMobManager(this, appScope)
        sensorManager = OrientationSensorManager(this)
        cameraManager = PlacementCameraManager(this)
        movementManager = MovementDetectionManager(this, sensorManager, cameraManager, appScope)
        timerEngine = TimerEngine(appScope)
        lockManager = FocusLockManager(this)
        soundManager = com.example.audio.SoundFeedbackManager(this, appScope)
    }
}
