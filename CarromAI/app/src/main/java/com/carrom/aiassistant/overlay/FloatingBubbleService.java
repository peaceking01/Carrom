package com.carrom.aiassistant.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.core.app.NotificationCompat;

import com.carrom.aiassistant.R;
import com.carrom.aiassistant.capture.ScreenCaptureService;

public class FloatingBubbleService extends Service {

    private static final String CHANNEL_ID = "CarromAIChannel";
    private static final int    NOTIF_ID   = 1;

    private WindowManager      windowManager;
    private View               bubbleView;
    private ControlPanelView   controlPanel;
    private OverlayDrawingView drawingOverlay;

    // Drag tracking
    private int   initialX, initialY;
    private float touchX,   touchY;
    private long  touchDownTime;

    // ────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Order matters: drawing overlay first (bottom layer), then panel, then bubble
        drawingOverlay = new OverlayDrawingView(this);
        controlPanel   = new ControlPanelView(this, drawingOverlay);
        initBubble();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Pass projection data to ScreenCaptureService if sent via intent
        if (intent != null && intent.hasExtra("resultCode")) {
            ScreenCaptureService.projectionResultCode = intent.getIntExtra("resultCode", 0);
            ScreenCaptureService.projectionData       = intent.getParcelableExtra("data");

            Intent captureIntent = new Intent(this, ScreenCaptureService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(captureIntent);
            } else {
                startService(captureIntent);
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        removeSafe(bubbleView);
        if (controlPanel   != null) controlPanel.destroy();
        if (drawingOverlay != null) drawingOverlay.destroy();
        stopService(new Intent(this, ScreenCaptureService.class));
        super.onDestroy();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Floating Bubble
    // ────────────────────────────────────────────────────────────────────────

    private void initBubble() {
        bubbleView = LayoutInflater.from(this).inflate(R.layout.floating_bubble, null);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20;
        params.y = 200;

        windowManager.addView(bubbleView, params);

        ImageView icon = bubbleView.findViewById(R.id.bubbleIcon);
        icon.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    initialX      = params.x;
                    initialY      = params.y;
                    touchX        = event.getRawX();
                    touchY        = event.getRawY();
                    touchDownTime = System.currentTimeMillis();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    int newX = initialX + (int)(event.getRawX() - touchX);
                    int newY = initialY + (int)(event.getRawY() - touchY);
                    params.x = newX;
                    params.y = newY;
                    windowManager.updateViewLayout(bubbleView, params);
                    return true;

                case MotionEvent.ACTION_UP:
                    long elapsed = System.currentTimeMillis() - touchDownTime;
                    float dx = Math.abs(event.getRawX() - touchX);
                    float dy = Math.abs(event.getRawY() - touchY);
                    // Tap = short press with little movement
                    if (elapsed < 250 && dx < 12 && dy < 12) {
                        controlPanel.toggle();
                    }
                    return true;
            }
            return false;
        });
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Notification
    // ────────────────────────────────────────────────────────────────────────

    private void createNotificationChannel() {
        NotificationChannel ch = new NotificationChannel(
            CHANNEL_ID,
            "Carrom AI Overlay",
            NotificationManager.IMPORTANCE_LOW
        );
        ch.setDescription("Keeps the floating overlay active");
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
            .createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Carrom AI Assistant")
            .setContentText("Overlay engine is running")
            .setSmallIcon(R.drawable.bubble_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Helper
    // ────────────────────────────────────────────────────────────────────────

    private void removeSafe(View v) {
        try { if (v != null) windowManager.removeView(v); }
        catch (Exception ignored) {}
    }
}
