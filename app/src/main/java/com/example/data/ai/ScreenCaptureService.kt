package com.example.data.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream

/** Captures one frame only after Android's explicit screen-capture consent. */
class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var workerThread: HandlerThread? = null
    @Volatile private var captureFinished = false
    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            // stopCapture() performs the resource cleanup after the one-shot frame.
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (resultCode != android.app.Activity.RESULT_OK || resultData == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("AU Agent")
            .setContentText("Capturing one screen image…")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, resultData)
            projection?.registerCallback(projectionCallback, Handler(mainLooper))
            captureSingleFrame()
        } catch (_: Exception) {
            sendBroadcast(Intent(ACTION_CAPTURE_READY).setPackage(packageName))
            stopCapture()
        }
        return START_NOT_STICKY
    }

    private fun captureSingleFrame() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        val originalWidth = metrics.widthPixels.coerceAtLeast(1)
        val originalHeight = metrics.heightPixels.coerceAtLeast(1)
        // Bound memory usage on high-resolution phones.
        val scale = minOf(1f, 1280f / originalWidth, 1280f / originalHeight)
        val width = (originalWidth * scale).toInt().coerceAtLeast(1)
        val height = (originalHeight * scale).toInt().coerceAtLeast(1)
        workerThread = HandlerThread("AU-ScreenCapture").also { it.start() }
        val handler = Handler(workerThread!!.looper)
        imageReader = ImageReader.newInstance(originalWidth, originalHeight, PixelFormat.RGBA_8888, 2)
        imageReader!!.setOnImageAvailableListener({ reader ->
            var bitmap: Bitmap? = null
            try {
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                image.use { frame ->
                    val plane = frame.planes[0]
                    val buffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * originalWidth
                    val paddedWidth = originalWidth + rowPadding / pixelStride
                    val full = Bitmap.createBitmap(paddedWidth, originalHeight, Bitmap.Config.ARGB_8888)
                    full.copyPixelsFromBuffer(buffer)
                    bitmap = if (paddedWidth != originalWidth) Bitmap.createBitmap(full, 0, 0, originalWidth, originalHeight) else full
                    if (bitmap !== full) full.recycle()
                }
                val source = bitmap ?: return@setOnImageAvailableListener
                val scaled = if (width != originalWidth || height != originalHeight) Bitmap.createScaledBitmap(source, width, height, true) else source
                val outFile = File(cacheDir, "au-agent-screen-${System.currentTimeMillis()}.jpg")
                FileOutputStream(outFile).use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, 78, out) }
                if (scaled !== source) scaled.recycle()
                source.recycle()
                captureFinished = true
                sendBroadcast(Intent(ACTION_CAPTURE_READY).setPackage(packageName).putExtra(EXTRA_CAPTURE_PATH, outFile.absolutePath))
            } catch (_: Exception) {
                captureFinished = true
                sendBroadcast(Intent(ACTION_CAPTURE_READY).setPackage(packageName))
            } finally {
                bitmap?.let { if (!it.isRecycled) it.recycle() }
                stopCapture()
            }
        }, handler)
        virtualDisplay = projection?.createVirtualDisplay(
            "AU-Agent-Screen", originalWidth, originalHeight, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader!!.surface, null, handler
        )
        // Avoid hanging indefinitely if Android doesn't deliver a frame.
        handler.postDelayed({
            if (projection != null) {
                if (!captureFinished) {
                    captureFinished = true
                    sendBroadcast(Intent(ACTION_CAPTURE_READY).setPackage(packageName))
                }
                stopCapture()
            }
        }, 5000L)
    }

    private fun stopCapture() {
        try { virtualDisplay?.release() } catch (_: Exception) {}
        virtualDisplay = null
        try { imageReader?.close() } catch (_: Exception) {}
        imageReader = null
        try { projection?.stop() } catch (_: Exception) {}
        projection = null
        try { workerThread?.quitSafely() } catch (_: Exception) {}
        workerThread = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "AU Agent screen capture", NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onDestroy() {
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { imageReader?.close() } catch (_: Exception) {}
        try { projection?.stop() } catch (_: Exception) {}
        try { workerThread?.quitSafely() } catch (_: Exception) {}
        super.onDestroy()
    }

    companion object {
        const val ACTION_CAPTURE_READY = "com.example.auagent.SCREEN_CAPTURE_READY"
        const val EXTRA_RESULT_CODE = "screen_capture_result_code"
        const val EXTRA_RESULT_DATA = "screen_capture_result_data"
        const val EXTRA_CAPTURE_PATH = "screen_capture_file_path"
        private const val CHANNEL_ID = "au_agent_screen_capture"
        private const val NOTIFICATION_ID = 7204
    }
}
