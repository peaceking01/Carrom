package com.carrom.aiassistant.models;

import android.graphics.PointF;

public class TrajectoryPoint {

    private PointF  position;
    private PointF  velocity;
    private float   time;
    private boolean isBouncePoint;   // wall reflection ஆன இடம்
    private boolean isCoinHitPoint;  // coin-ஐ hit பண்ண இடம்

    public TrajectoryPoint(PointF position, PointF velocity, float time) {
        this.position      = position;
        this.velocity      = velocity;
        this.time          = time;
        this.isBouncePoint = false;
        this.isCoinHitPoint = false;
    }

    public TrajectoryPoint(PointF position, PointF velocity, float time,
                           boolean isBouncePoint, boolean isCoinHitPoint) {
        this.position       = position;
        this.velocity       = velocity;
        this.time           = time;
        this.isBouncePoint  = isBouncePoint;
        this.isCoinHitPoint = isCoinHitPoint;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public PointF getPosition()             { return position; }
    public void   setPosition(PointF p)     { this.position = p; }

    public PointF getVelocity()             { return velocity; }
    public void   setVelocity(PointF v)     { this.velocity = v; }

    public float  getTime()                 { return time; }
    public void   setTime(float t)          { this.time = t; }

    public boolean isBouncePoint()          { return isBouncePoint; }
    public void    setBouncePoint(boolean b){ this.isBouncePoint = b; }

    public boolean isCoinHitPoint()             { return isCoinHitPoint; }
    public void    setCoinHitPoint(boolean b)   { this.isCoinHitPoint = b; }
}
