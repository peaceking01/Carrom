package com.carrom.aiassistant.automation;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

import com.carrom.aiassistant.models.BoardState;
import com.carrom.aiassistant.models.ShotPrediction;
import com.carrom.aiassistant.overlay.OverlayDrawingView;
import com.carrom.aiassistant.physics.PhysicsEngine;
import com.carrom.aiassistant.utils.MathUtils;

public class AutoPlayService extends AccessibilityService {

    // Static singleton — accessed by ControlPanelView
    public static AutoPlayService instance;

    private final PhysicsEngine physicsEngine = new PhysicsEngine();
    private final Handler       handler       = new Handler(Looper.getMainLooper());

    private boolean autoPlayEnabled = false;
    private boolean shotInProgress  = false;

    // How long to wait between automated shots (ms)
    private static final long SHOT_COOLDOWN_MS = 1800;

    // Swipe gesture duration (ms) — longer = slower/smoother pull
    private static final long SWIPE_DURATION_MS = 220;

    // Striker drag distance in pixels — maps power to screen distance
    private static final float POWER_TO_PX = 2.8f;

    // ────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // We don't need to handle accessibility events for gesture injection
    }

    @Override
    public void onInterrupt() {
        stopAutoPlay();
    }

    @Override
    public void onDestroy() {
        instance = null;
        stopAutoPlay();
        super.onDestroy();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Public API — called by ControlPanelView buttons
    // ────────────────────────────────────────────────────────────────────────

    public void startAutoPlay() {
        autoPlayEnabled = true;
    }

    public void stopAutoPlay() {
        autoPlayEnabled = false;
        handler.removeCallbacksAndMessages(null);
    }

    /**
     * Called by ScreenCaptureService every time a new frame is analysed.
     * If AutoPlay is ON and no shot is currently in progress,
     * calculate the best shot and inject the swipe gesture.
     */
    public void onNewState(BoardState state) {
        if (!autoPlayEnabled || shotInProgress) return;
        if (state == null || state.getStriker() == null) return;

        // Find best shot on the calling thread (already background)
        ShotPrediction best = physicsEngine.findBestShot(state);
        if (best == null || best.getScore() < 0.1f) return;

        // Share with overlay so aim line updates immediately
        OverlayDrawingView.bestShot = best;

        // Execute gesture on main thread after a short delay
        shotInProgress = true;
        handler.postDelayed(() -> {
            executeShot(state.getStriker().getPosition(), best);
        }, 300); // 300 ms look-ahead delay
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Gesture Injection
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Inject a swipe that simulates pulling the striker and releasing.
     *
     * In most carrom games the gesture is:
     *   Press on striker  →  drag backward (opposite to shot direction)  →  release
     * The flick speed / distance represents power.
     */
    private void executeShot(PointF strikerPos, ShotPrediction shot) {
        // Pull-back vector: opposite to shot direction
        float pullAngle = shot.getAngle() + 180f;
        float pullDist  = shot.getPower() * POWER_TO_PX;

        PointF pullVec  = MathUtils.vectorFromAngle(pullAngle, pullDist);
        PointF startPt  = new PointF(strikerPos.x + pullVec.x,
                                     strikerPos.y + pullVec.y);
        PointF endPt    = strikerPos; // release at striker centre

        Path swipePath = new Path();
        swipePath.moveTo(startPt.x, startPt.y);
        swipePath.lineTo(endPt.x,   endPt.y);

        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(swipePath, 0, SWIPE_DURATION_MS);

        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(stroke)
            .build();

        dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription g) {
                // Wait for cooldown before allowing next shot
                handler.postDelayed(() -> {
                    shotInProgress  = false;
                    OverlayDrawingView.bestShot = null; // force recalculation
                }, SHOT_COOLDOWN_MS);
            }

            @Override
            public void onCancelled(GestureDescription g) {
                shotInProgress = false;
            }
        }, handler);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Status
    // ────────────────────────────────────────────────────────────────────────

    public boolean isAutoPlayEnabled() { return autoPlayEnabled; }
    public boolean isShotInProgress()  { return shotInProgress;  }
}
