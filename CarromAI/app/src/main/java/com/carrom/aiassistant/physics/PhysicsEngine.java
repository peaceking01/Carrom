package com.carrom.aiassistant.physics;

import android.graphics.PointF;

import com.carrom.aiassistant.models.BoardState;
import com.carrom.aiassistant.models.GameObject;
import com.carrom.aiassistant.models.GameObjectType;
import com.carrom.aiassistant.models.ShotPrediction;
import com.carrom.aiassistant.models.TrajectoryPoint;
import com.carrom.aiassistant.utils.MathUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PhysicsEngine {

    // ── Simulation constants ─────────────────────────────────────────────────
    private static final float FRICTION        = 0.985f;   // per frame decay
    private static final float COLLISION_DAMP  = 0.82f;   // energy lost on coin hit
    private static final float WALL_DAMP       = 0.90f;   // energy lost on wall bounce
    private static final float MIN_SPEED       = 0.5f;    // stop threshold
    private static final float TIME_STEP       = 16f;     // ms per step (~60Hz)
    private static final float MAX_SIM_TIME    = 4000f;   // 4 seconds max
    private static final float INITIAL_POWER   = 28f;    // px/step launch speed
    private static final int   ANGLE_STEPS     = 72;      // 360/72 = 5° resolution

    // ── Pocket capture radius multiplier ────────────────────────────────────
    private static final float POCKET_CAPTURE_MULT = 1.6f;

    // ────────────────────────────────────────────────────────────────────────
    //  Public API
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Scan all angles and return the highest-scoring shot.
     */
    public ShotPrediction findBestShot(BoardState state) {
        if (state.getStriker() == null) return null;

        List<ShotPrediction> candidates = new ArrayList<>();

        for (int i = 0; i < ANGLE_STEPS; i++) {
            float angle = i * (360f / ANGLE_STEPS);
            ShotPrediction shot = simulate(state, angle, INITIAL_POWER);
            if (shot != null) candidates.add(shot);
        }

        if (candidates.isEmpty()) return null;

        // Sort by score descending
        Collections.sort(candidates, (a, b) -> Float.compare(b.getScore(), a.getScore()));
        return candidates.get(0);
    }

    /**
     * Simulate a single shot at given angle (degrees) and power.
     */
    public ShotPrediction simulate(BoardState state, float angleDeg, float power) {
        if (state.getStriker() == null || state.getBoundaries() == null) return null;

        // Board limits
        float boardLeft   = state.getBoundaries().get(0).x;
        float boardTop    = state.getBoundaries().get(0).y;
        float boardRight  = state.getBoundaries().get(1).x;
        float boardBottom = state.getBoundaries().get(2).y;

        // Clone coins so simulation doesn't mutate state
        List<SimCoin> simCoins = cloneCoins(state);

        // Striker initial state
        PointF pos = copy(state.getStriker().getPosition());
        PointF vel = MathUtils.vectorFromAngle(angleDeg, power);
        float  r   = state.getStriker().getRadius();

        List<TrajectoryPoint> path = new ArrayList<>();
        path.add(new TrajectoryPoint(copy(pos), copy(vel), 0));

        float  elapsed      = 0;
        int    bounces      = 0;
        boolean hitCoin     = false;
        boolean willScore   = false;
        GameObject hitObj   = null;

        // ── Simulation loop ─────────────────────────────────────────────────
        while (elapsed < MAX_SIM_TIME) {
            float speed = MathUtils.magnitude(vel);
            if (speed < MIN_SPEED) break;

            // Apply friction
            vel = MathUtils.applyFriction(vel, TIME_STEP);

            // Next position
            PointF next = MathUtils.add(pos, vel);

            // ── Wall reflections ──────────────────────────────────────────
            boolean bounced = false;
            if (next.x - r < boardLeft) {
                next.x = boardLeft + r;
                vel.x  = -vel.x * WALL_DAMP;
                bounced = true;
            } else if (next.x + r > boardRight) {
                next.x = boardRight - r;
                vel.x  = -vel.x * WALL_DAMP;
                bounced = true;
            }
            if (next.y - r < boardTop) {
                next.y = boardTop + r;
                vel.y  = -vel.y * WALL_DAMP;
                bounced = true;
            } else if (next.y + r > boardBottom) {
                next.y = boardBottom - r;
                vel.y  = -vel.y * WALL_DAMP;
                bounced = true;
            }
            if (bounced) bounces++;

            // ── Coin collisions ───────────────────────────────────────────
            for (SimCoin coin : simCoins) {
                if (!coin.active) continue;
                float dist = MathUtils.distance(next, coin.pos);
                if (dist < r + coin.radius) {
                    // Elastic-ish collision
                    PointF normal = MathUtils.normalize(
                        MathUtils.subtract(next, coin.pos));

                    // Striker deflects
                    vel = MathUtils.reflect(vel, normal);
                    vel = MathUtils.multiply(vel, COLLISION_DAMP);

                    // Coin gets pushed
                    float coinSpeed = speed * (1f - COLLISION_DAMP);
                    coin.vel = MathUtils.multiply(normal, -coinSpeed);

                    if (!hitCoin) { hitCoin = true; hitObj = coin.obj; }

                    // Mark the hit point
                    TrajectoryPoint tp = new TrajectoryPoint(copy(next), copy(vel), elapsed, false, true);
                    path.add(tp);

                    // Simulate coin rolling to pocket
                    boolean scored = simulateCoinRoll(coin, state, boardLeft, boardTop, boardRight, boardBottom);
                    if (scored) willScore = true;
                }
            }

            pos = next;
            elapsed += TIME_STEP;

            boolean isBounce = bounced;
            path.add(new TrajectoryPoint(copy(pos), copy(vel), elapsed, isBounce, false));
        }

        float score = scoreShot(willScore, bounces, hitCoin, hitObj, state);
        return new ShotPrediction(path, hitObj, score, angleDeg, power, bounces, willScore);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Coin Roll Simulation
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Simulate a struck coin rolling until it stops or falls into a pocket.
     * Returns true if it reaches a pocket.
     */
    private boolean simulateCoinRoll(SimCoin coin, BoardState state,
                                     float l, float t, float r, float b) {
        float elapsed = 0;
        while (elapsed < MAX_SIM_TIME) {
            float speed = MathUtils.magnitude(coin.vel);
            if (speed < MIN_SPEED) break;

            coin.vel = MathUtils.applyFriction(coin.vel, TIME_STEP);
            PointF next = MathUtils.add(coin.pos, coin.vel);

            // Wall bounce for coin
            if (next.x - coin.radius < l) { next.x = l + coin.radius; coin.vel.x = -coin.vel.x * WALL_DAMP; }
            if (next.x + coin.radius > r)  { next.x = r - coin.radius; coin.vel.x = -coin.vel.x * WALL_DAMP; }
            if (next.y - coin.radius < t)  { next.y = t + coin.radius; coin.vel.y = -coin.vel.y * WALL_DAMP; }
            if (next.y + coin.radius > b)  { next.y = b - coin.radius; coin.vel.y = -coin.vel.y * WALL_DAMP; }

            coin.pos = next;

            // Pocket check
            if (state.getPockets() != null) {
                for (GameObject pocket : state.getPockets()) {
                    float pocketDist = MathUtils.distance(coin.pos, pocket.getPosition());
                    if (pocketDist < pocket.getRadius() * POCKET_CAPTURE_MULT) {
                        coin.active = false;
                        return true;
                    }
                }
            }
            elapsed += TIME_STEP;
        }
        return false;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Shot Scoring
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Score 0..1 — higher is better.
     * Rewards: scoring a coin, hitting queen, fewer bounces.
     */
    private float scoreShot(boolean willScore, int bounces, boolean hitCoin,
                            GameObject hitObj, BoardState state) {
        float score = 0f;

        if (willScore)  score += 0.60f;
        if (hitCoin)    score += 0.20f;

        // Bonus if we hit queen
        if (hitObj != null && hitObj.getType() == GameObjectType.QUEEN) score += 0.15f;

        // Penalise for many bounces (complex shot = risky)
        score -= bounces * 0.04f;

        return Math.max(0f, Math.min(1f, score));
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ────────────────────────────────────────────────────────────────────────

    private List<SimCoin> cloneCoins(BoardState state) {
        List<SimCoin> list = new ArrayList<>();
        if (state.getCoins() != null) {
            for (GameObject c : state.getCoins()) {
                list.add(new SimCoin(c, copy(c.getPosition()), c.getRadius()));
            }
        }
        if (state.getQueen() != null) {
            GameObject q = state.getQueen();
            list.add(new SimCoin(q, copy(q.getPosition()), q.getRadius()));
        }
        return list;
    }

    private PointF copy(PointF p) { return new PointF(p.x, p.y); }

    // ── Inner class for mutable simulation coin state ────────────────────────
    private static class SimCoin {
        final GameObject obj;
        PointF pos;
        PointF vel;
        final float radius;
        boolean active = true;

        SimCoin(GameObject obj, PointF pos, float radius) {
            this.obj    = obj;
            this.pos    = pos;
            this.vel    = new PointF(0, 0);
            this.radius = radius;
        }
    }
}
