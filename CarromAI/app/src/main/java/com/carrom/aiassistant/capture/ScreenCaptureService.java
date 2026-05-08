package com.carrom.aiassistant.capture;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import com.carrom.aiassistant.R;
import com.carrom.aiassistant.detection.CarromDetector;
import com.carrom.aiassistant.models.BoardState;
import com.carrom.aiassistant.overlay.OverlayDrawingView;

import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {

    private static final String CHANNEL_ID      = "ScreenCaptureChannel";
    private static final int    NOTIF_ID        = 2;
    private static final int    FRAME_DELAY_MS  = 33; // ~30 FPS

    // Static access for other components
    public static ScreenCaptureService instance;
    public static int    projectionResultCode;
    public static Intent projectionData;

    private MediaProjection  mediaProjection;
    private VirtualDisplay   virtualDisplay;
    private ImageReader      imageReader;
    private CarromDetector   detector;
    private Handler          handler;
    private Runnable         captureRunnable;

    private int screenWidth;
    private int screenHeight;
    private int screenDpi;
    private boolean isScanning = false;

    // ────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        detector = new CarromDetector();
        handler  = new Handler(Looper.getMainLooper());

        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());
        resolveScreenMetrics();
        initMediaProjection();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopScanning();
        tearDown();
        instance = null;
        super.onDestroy();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Init
    // ────────────────────────────────────────────────────────────────────────

    private void resolveScreenMetrics() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        screenWidth  = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDpi    = metrics.densityDpi;
    }

    private void initMediaProjection() {
        MediaProjectionManager mpm =
            (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpm.getMediaProjection(projectionResultCode, projectionData);
        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override public void onStop() { stopSelf(); }
        }, null);

        imageReader = ImageReader.newInstance(
            screenWidth, screenHeight, PixelFormat.RGBA_8888, 2
        );

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "CarromCapture",
            screenWidth, screenHeight, screenDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.getSurface(), null, null
        );
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Scanning Loop
    // ────────────────────────────────────────────────────────────────────────

    /** Call from ControlPanelView "Start Scan" button */
    public void startScanning() {
        if (isScanning) return;
        isScanning = true;

        captureRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isScanning) return;
                Bitmap frame = captureFrame();
                if (frame != null) {
                    // Run heavy detection on background thread
                    final Bitmap bmp = frame;
                    new Thread(() -> {
                        BoardState state = detector.analyze(bmp);
                        bmp.recycle();
                        publishState(state);
                    }).start();
                }
                handler.postDelayed(this, FRAME_DELAY_MS);
            }
        };
        handler.post(captureRunnable);
    }

    public void stopScanning() {
        isScanning = false;
        if (captureRunnable != null) {
            handler.removeCallbacks(captureRunnable);
            captureRunnable = null;
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Frame Capture
    // ────────────────────────────────────────────────────────────────────────

    private Bitmap captureFrame() {
        Image image = imageReader.acquireLatestImage();
        if (image == null) return null;

        try {
            Image.Plane plane       = image.getPlanes()[0];
            ByteBuffer  buffer      = plane.getBuffer();
            int         rowStride   = plane.getRowStride();
            int         pixelStride = plane.getPixelStride();
            int         rowPadding  = rowStride - pixelStride * screenWidth;

            Bitmap bmp = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            );
            bmp.copyPixelsFromBuffer(buffer);

            // Crop away any row padding
            if (rowPadding != 0) {
                Bitmap cropped = Bitmap.createBitmap(bmp, 0, 0, screenWidth, screenHeight);
                bmp.recycle();
                return cropped;
            }
            return bmp;

        } catch (Exception e) {
            return null;
        } finally {
            image.close();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  State Publishing
    // ────────────────────────────────────────────────────────────────────────

    private void publishState(BoardState state) {
        // Push to overlay drawing view
        OverlayDrawingView.currentState = state;

        // Push to auto-play engine
        if (com.carrom.aiassistant.automation.AutoPlayService.instance != null) {
            com.carrom.aiassistant.automation.AutoPlayService.instance.onNewState(state);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Cleanup
    // ────────────────────────────────────────────────────────────────────────

    private void tearDown() {
        if (virtualDisplay   != null) virtualDisplay.release();
        if (imageReader      != null) imageReader.close();
        if (mediaProjection  != null) mediaProjection.stop();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Notification
    // ────────────────────────────────────────────────────────────────────────

    private void createNotificationChannel() {
        NotificationChannel ch = new NotificationChannel(
            CHANNEL_ID, "Screen Capture", NotificationManager.IMPORTANCE_LOW
        );
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Carrom AI")
            .setContentText("Screen capture active")
            .setSmallIcon(R.drawable.bubble_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }
}
