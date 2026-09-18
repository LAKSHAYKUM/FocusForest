package com.example.camera

import android.content.Context
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * Lightweight local camera manager.
 * Captures 4x4 spatial luminance brightness grid to verify scene stability when phone is placed.
 * NO images or videos are stored or uploaded. All processing is transient and strictly local.
 */
class PlacementCameraManager(private val context: Context) {

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    private val _visualGridState = MutableStateFlow<FloatArray?>(null)
    val visualGridState: StateFlow<FloatArray?> = _visualGridState.asStateFlow()

    private var lastAnalysisTimestamp = 0L
    private val analysisThrottleMs = 150L // 6-7 fps lightweight sampling

    private var activePreview: Preview? = null

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?,
        onReady: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        if (cameraExecutor.isShutdown) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build()
                activePreview = preview
                previewView?.let {
                    preview.setSurfaceProvider(it.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageFrame(imageProxy)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                onReady()
            } catch (e: Exception) {
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageFrame(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastAnalysisTimestamp < analysisThrottleMs) {
            imageProxy.close()
            return
        }
        lastAnalysisTimestamp = now

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        try {
            val yBuffer = mediaImage.planes[0].buffer
            val yRowStride = mediaImage.planes[0].rowStride
            val yPixelStride = mediaImage.planes[0].pixelStride
            val width = imageProxy.width
            val height = imageProxy.height
            val bufferLimit = yBuffer.limit()

            // Sample 4x4 spatial zones across center region of placement frame
            val grid = FloatArray(16)
            var idx = 0
            for (gy in 1..4) {
                for (gx in 1..4) {
                    val sampleX = (width * gx) / 5
                    val sampleY = (height * gy) / 5
                    val bufferIndex = sampleY * yRowStride + sampleX * yPixelStride
                    if (bufferIndex >= 0 && bufferIndex < bufferLimit && idx < grid.size) {
                        val lum = (yBuffer.get(bufferIndex).toInt() and 0xFF) / 255f
                        grid[idx++] = lum
                    }
                }
            }
            _visualGridState.value = grid
        } catch (_: Exception) {
            // Silently recover
        } finally {
            imageProxy.close()
        }
    }

    fun stopCamera() {
        try {
            activePreview?.setSurfaceProvider(null)
            activePreview = null
            cameraProvider?.unbindAll()
        } catch (_: Exception) {}
        _visualGridState.value = null
    }

    fun release() {
        stopCamera()
        if (!cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
        }
    }

    companion object {
        fun calculateVisualDifference(gridA: FloatArray?, gridB: FloatArray?): Float {
            if (gridA == null || gridB == null || gridA.size != gridB.size) return 0f
            var diffSum = 0f
            for (i in gridA.indices) {
                diffSum += abs(gridA[i] - gridB[i])
            }
            return diffSum / gridA.size
        }
    }
}
