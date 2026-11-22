package com.realcity.ram

import android.app.*
import android.content.pm.ServiceInfo
import android.content.Context
import android.content.Intent
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

class ScreenCaptureService : Service() {
    companion object {
        const val EXTRA_RESULT_CODE = "capture_result_code"
        const val EXTRA_RESULT_DATA = "capture_result_data"
        const val ACTION_STOP = "com.realcity.ram.STOP_CAPTURE"
        private const val CHANNEL = "ram_screen_capture"
        private const val NOTIFICATION_ID = 742
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var worker: HandlerThread? = null
    private var frames = 0L

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Captura de diagnóstico", NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(NOTIFICATION_ID, notification())
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        @Suppress("DEPRECATION")
        val data = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        if (resultCode != Activity.RESULT_OK || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            startCapture()
        } catch (_: Exception) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val pending = PendingIntent.getActivity(this, 742, launch,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL) else Notification.Builder(this)
        return builder.setContentTitle("REAL CITY RAM • Captura ativa")
            .setContentText("Diagnóstico de quadros — toque para voltar ao app")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pending).setOngoing(true).build()
    }

    private fun startCapture() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        val density = metrics.densityDpi
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        worker = HandlerThread("ram-capture-diagnostics").also { it.start() }
        val handler = Handler(worker!!.looper)
        reader!!.setOnImageAvailableListener({ source ->
            val image = source.acquireLatestImage()
            if (image != null) {
                image.close()
                frames++
                getSharedPreferences("ram_settings_v2", Context.MODE_PRIVATE).edit()
                    .putLong("capture_frames", frames)
                    .putString("capture_size", "${width} × ${height}")
                    .putLong("capture_last_frame", System.currentTimeMillis())
                    .apply()
            }
        }, handler)
        virtualDisplay = projection?.createVirtualDisplay(
            "REAL_CITY_RAM_DIAGNOSTICS", width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader!!.surface, null, handler
        )
        getSharedPreferences("ram_settings_v2", Context.MODE_PRIVATE).edit()
            .putBoolean("capture_running", virtualDisplay != null).apply()
        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { stopSelf() }
        }, handler)
    }

    override fun onDestroy() {
        getSharedPreferences("ram_settings_v2", Context.MODE_PRIVATE).edit()
            .putBoolean("capture_running", false).apply()
        virtualDisplay?.release()
        reader?.close()
        projection?.stop()
        worker?.quitSafely()
        virtualDisplay = null
        reader = null
        projection = null
        worker = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
