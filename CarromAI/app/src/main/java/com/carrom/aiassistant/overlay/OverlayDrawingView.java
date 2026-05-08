package com.carrom.aiassistant.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.carrom.aiassistant.models.BoardState;
import com.carrom.aiassistant.models.GameObject;
import com.carrom.aiassistant.models.ShotPrediction;
import com.carrom.aiassistant.models.TrajectoryPoint;
import com.carrom.aiassistant.physics.PhysicsEngine;

import java.util.List;

public class OverlayDrawingView extends View {

    // ── Static state updated by ScreenCaptureService ─────────────────────────
    public static volatile BoardState   currentState;
    public static volatile ShotPrediction bestShot;

    private final WindowManager windowManager;
    private boolean showAimLine = false;

    // ── Paints ────────────────────────────────────────────────────────────────
    private final Paint mainLinePaint;
    private final Paint bounceLinePaint;
    private final Paint coinTargetPaint;
    private final Paint pocketGlowPaint;
    private final Paint strikerPaint;
    private final Paint coinPaint;
    private final Paint textPaint;

    private final PhysicsEngine physicsEngine = new PhysicsEngine();

    // ────────────────────────────────────────────────────────────────────────
    //  Constructor
    // ────────────────────────────────────────────────────────────────────────

    public OverlayDrawingView(Context context) {
        super(context);
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        // Main shot trajectory — solid cyan line
        mainLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mainLinePaint.setColor(Color.CYAN);
        mainLinePaint.setStrokeWidth(4f);
        mainLinePaint.setStyle(Paint.Style.STROKE);
        mainLinePaint.setAlpha(220);

        // Bounce path — dashed yellow
        bounceLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bounceLinePaint.setColor(Color.YELLOW);
        bounceLinePaint.setStrokeWidth(3f);
        bounceLinePaint.setStyle(Paint.Style.STROKE);
        bounceLinePaint.setAlpha(180);
        bounceLinePaint.setPathEffect(new DashPathEffect(new float[]{18, 10}, 0));

        // Coin target path — orange
        coinTargetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        coinTargetPaint.setColor(Color.parseColor("#FF8C00"));
        coinTargetPaint.setStrokeWidth(3f);
        coinTargetPaint.setStyle(Paint.Style.STROKE);
        coinTargetPaint.setAlpha(200);
        coinTargetPaint.setPathEffect(new DashPathEffect(new float[]{12, 8}, 0));

        // Pocket glow — green circle
        pocketGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pocketGlowPaint.setColor(Color.GREEN);
        pocketGlowPaint.setStyle(Paint.Style.STROKE);
        pocketGlowPaint.setStrokeWidth(4f);
        pocketGlowPaint.setAlpha(160);

        // Striker outline
        strikerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strikerPaint.setColor(Color.WHITE);
        strikerPaint.setStyle(Paint.Style.STROKE);
        strikerPaint.setStrokeWidth(3f);

        // Coin detection outline
        coinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        coinPaint.setStyle(Paint.Style.STROKE);
        coinPaint.setStrokeWidth(2f);

        // Score / info text
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
        textPaint.setShadowLayer(4f, 2f, 2f, Color.BLACK);

        addToWindowManager();
        startRedrawLoop();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Window Manager
    // ────────────────────────────────────────────────────────────────────────

    private void addToWindowManager() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(this, params);
        setVisibility(GONE);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Draw
    // ────────────────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!showAimLine) return;

        BoardState state = currentState;
        if (state == null) return;

        // 1. Detected objects outlines
        drawDetectedObjects(canvas, state);

        // 2. Best shot prediction lines
        ShotPrediction shot = bestShot;
        if (shot == null && state.getStriker() != null) {
            shot = physicsEngine.findBestShot(state);
            bestShot = shot;
        }
        if (shot != null) {
            drawTrajectory(canvas, shot);
            drawScoreInfo(canvas, shot);
        }

        // 3. Pocket markers
        drawPockets(canvas, state);
    }

    // ── Detected Objects ─────────────────────────────────────────────────────

    private void drawDetectedObjects(Canvas canvas, BoardState state) {
        // Striker — white circle
        if (state.getStriker() != null) {
            GameObject s = state.getStriker();
            canvas.drawCircle(
                s.getPosition().x, s.getPosition().y,
                s.getRadius(), strikerPaint
            );
        }

        // Coins
        if (state.getCoins() != null) {
            for (GameObject coin : state.getCoins()) {
                switch (coin.getType()) {
                    case WHITE_COIN:
                        coinPaint.setColor(Color.parseColor("#EEEEEE")); break;
                    case BLACK_COIN:
                        coinPaint.setColor(Color.parseColor("#888888")); break;
                    case QUEEN:
                        coinPaint.setColor(Color.RED); break;
                    default:
                        coinPaint.setColor(Color.LTGRAY);
                }
                canvas.drawCircle(
                    coin.getPosition().x, coin.getPosition().y,
                    coin.getRadius(), coinPaint
                );
            }
        }

        // Queen special highlight
        if (state.getQueen() != null) {
            GameObject q = state.getQueen();
            coinPaint.setColor(Color.RED);
            coinPaint.setStrokeWidth(4f);
            canvas.drawCircle(q.getPosition().x, q.getPosition().y,
                q.getRadius() + 6f, coinPaint);
            canvas.drawText("Q",
                q.getPosition().x - 10, q.getPosition().y - q.getRadius() - 10,
                textPaint);
            coinPaint.setStrokeWidth(2f);
        }
    }

    // ── Trajectory Lines ──────────────────────────────────────────────────────

    private void drawTrajectory(Canvas canvas, ShotPrediction shot) {
        List<TrajectoryPoint> points = shot.getTrajectory();
        if (points == null || points.size() < 2) return;

        boolean firstBounceDrawn = false;
        boolean hitCoinDrawn     = false;
        int     bounceCount      = 0;

        for (int i = 0; i < points.size() - 1; i++) {
            PointF from = points.get(i).getPosition();
            PointF to   = points.get(i + 1).getPosition();

            TrajectoryPoint tp = points.get(i);

            if (tp.isBouncePoint() && !firstBounceDrawn) {
                // Switch from main to bounce paint
                firstBounceDrawn = true;
                bounceCount++;
            }

            if (tp.isCoinHitPoint() && !hitCoinDrawn) {
                hitCoinDrawn = true;
            }

            Paint p;
            if (!firstBounceDrawn && !hitCoinDrawn)  p = mainLinePaint;
            else if (hitCoinDrawn)                    p = coinTargetPaint;
            else                                      p = bounceLinePaint;

            canvas.drawLine(from.x, from.y, to.x, to.y, p);
        }

        // Arrow head at the end of main line
        if (points.size() >= 2) {
            drawArrowHead(canvas,
                points.get(0).getPosition(),
                points.get(1).getPosition(),
                mainLinePaint);
        }
    }

    private void drawArrowHead(Canvas canvas, PointF from, PointF to, Paint paint) {
        float angle  = (float) Math.atan2(to.y - from.y, to.x - from.x);
        float arrowLen = 30f;
        float spread   = (float) Math.toRadians(30);

        float x1 = to.x - arrowLen * (float) Math.cos(angle - spread);
        float y1 = to.y - arrowLen * (float) Math.sin(angle - spread);
        float x2 = to.x - arrowLen * (float) Math.cos(angle + spread);
        float y2 = to.y - arrowLen * (float) Math.sin(angle + spread);

        canvas.drawLine(to.x, to.y, x1, y1, paint);
        canvas.drawLine(to.x, to.y, x2, y2, paint);
    }

    // ── Pockets ───────────────────────────────────────────────────────────────

    private void drawPockets(Canvas canvas, BoardState state) {
        if (state.getPockets() == null) return;
        for (GameObject pocket : state.getPockets()) {
            canvas.drawCircle(
                pocket.getPosition().x,
                pocket.getPosition().y,
                pocket.getRadius() + 8f,
                pocketGlowPaint
            );
        }
    }

    // ── Score Info ────────────────────────────────────────────────────────────

    private void drawScoreInfo(Canvas canvas, ShotPrediction shot) {
        String info = String.format("Score: %.0f%%  Bounces: %d  %s",
            shot.getScore() * 100,
            shot.getBounces(),
            shot.isWillScore() ? "✓ SCORE" : ""
        );
        canvas.drawText(info, 40, 80, textPaint);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Redraw Loop (~30fps)
    // ────────────────────────────────────────────────────────────────────────

    private void startRedrawLoop() {
        post(new Runnable() {
            @Override public void run() {
                if (showAimLine) invalidate();
                postDelayed(this, 33);
            }
        });
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Public API
    // ────────────────────────────────────────────────────────────────────────

    /** Toggle aim line visibility. Returns new state (true = showing). */
    public boolean toggleAimLine() {
        showAimLine = !showAimLine;
        setVisibility(showAimLine ? VISIBLE : GONE);
        if (showAimLine) invalidate();
        return showAimLine;
    }

    public boolean isShowingAimLine() { return showAimLine; }

    public void destroy() {
        try { windowManager.removeView(this); } catch (Exception ignored) {}
    }
}
