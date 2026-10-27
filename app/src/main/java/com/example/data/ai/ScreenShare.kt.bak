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
import android.os.IBinder
import android.os.Looper
import android.util.Base64
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.io.ByteArrayOutputStream

/**
 * Live screen sharing for the voice assistant. A foreground service keeps a MediaProjection alive and
 * remembers the newest frame; [ScreenShare.captureBase64] hands that frame to the AI together with the
 * user's question, so "save this Instagram bio in a note" is answered from what is really on the screen.
 */
object ScreenShare {
    @Volatile var active: Boolean = false
        internal set
    @Volatile internal var latestJpeg: ByteArray? = null

    fun createIntent(context: Context): Intent =
        (context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent()

    fun start(context: Context, resultCode: Int, data: Intent) {
        val i = Intent(context, ScreenCaptureService::class.java)
            .putExtra("resultCode", resultCode)
            .putExtra("data", data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i) else context.startService(i)
    }

    fun stop(context: Context) {
        active = false
        latestJpeg = null
        context.stopService(Intent(context, ScreenCaptureService::class.java))
    }

    /** Newest frame as base64 JPEG, or null when sharing is off / nothing captured yet. */
    fun captureBase64(): String? = latestJpeg?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
}

class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastFrameAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) { stopSelf(); return START_NOT_STICKY }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel("au_screen_share", "AU Agent screen sharing", NotificationManager.IMPORTANCE_LOW))
        }
        val notification: Notification = NotificationCompat.Builder(this, "au_screen_share")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("AU Agent can see your screen")
            .setContentText("Screen sharing is on. Stop it from the voice assistant.")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(4711, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(4711, notification)
        }

        val resultCode = intent.getIntExtra("resultCode", 0)
        @Suppress("DEPRECATION")
        val data = intent.getParcelableExtra<Intent>("data")
        if (data == null) { stopSelf(); return START_NOT_STICKY }
        try {
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mp = mpm.getMediaProjection(resultCode, data)
            projection = mp
            mp!!.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { ScreenShare.active = false; stopSelf() }
            }, handler)

            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            // half-size frames are plenty for reading text and keep the upload small
            val w = (metrics.widthPixels / 2).coerceAtLeast(360)
            val h = (metrics.heightPixels / 2).coerceAtLeast(640)
            val ir = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
            reader = ir
            ir.setOnImageAvailableListener({ r ->
                val img = try { r.acquireLatestImage() } catch (e: Exception) { null } ?: return@setOnImageAvailableListener
                try {
                    val now = System.currentTimeMillis()
                    if (now - lastFrameAt < 600) return@setOnImageAvailableListener
                    lastFrameAt = now
                    val plane = img.planes[0]
                    val rowPadding = plane.rowStride - plane.pixelStride * w
                    val bmp = Bitmap.createBitmap(w + rowPadding / plane.pixelStride, h, Bitmap.Config.ARGB_8888)
                    bmp.copyPixelsFromBuffer(plane.buffer)
                    val cropped = Bitmap.createBitmap(bmp, 0, 0, w, h)
                    val out = ByteArrayOutputStream()
                    cropped.compress(Bitmap.CompressFormat.JPEG, 72, out)
                    ScreenShare.latestJpeg = out.toByteArray()
                } catch (e: Exception) {
                    // skip this frame
                } finally {
                    img.close()
                }
            }, handler)
            display = mp!!.createVirtualDisplay(
                "au_agent_screen", w, h, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, ir.surface, null, handler
            )
            ScreenShare.active = true
        } catch (e: Exception) {
            ScreenShare.active = false
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ScreenShare.active = false
        try { display?.release() } catch (_: Exception) {}
        try { reader?.close() } catch (_: Exception) {}
        try { projection?.stop() } catch (_: Exception) {}
        super.onDestroy()
    }
}
