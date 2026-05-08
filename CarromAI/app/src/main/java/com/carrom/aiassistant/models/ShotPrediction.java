package com.carrom.aiassistant.models;

import java.util.List;

public class ShotPrediction {
    private List<TrajectoryPoint> trajectory;
    private GameObject hitObject;
    private float score;
    private float angle;
    private float power;
    private int bounces;
    private boolean willScore;
    
    public ShotPrediction(List<TrajectoryPoint> trajectory, GameObject hitObject,
                         float score, float angle, float power, int bounces, boolean willScore) {
        this.trajectory = trajectory;
        this.hitObject = hitObject;
        this.score = score;
        this.angle = angle;
        this.power = power;
        this.bounces = bounces;
        this.willScore = willScore;
    }
    
    // Getters and Setters
    public List<TrajectoryPoint> getTrajectory() { return trajectory; }
    public void setTrajectory(List<TrajectoryPoint> trajectory) { this.trajectory = trajectory; }
    
    public GameObject getHitObject() { return hitObject; }
    public void setHitObject(GameObject hitObject) { this.hitObject = hitObject; }
    
    public float getScore() { return score; }
    public void setScore(float score) { this.score = score; }
    
    public float getAngle() { return angle; }
    public void setAngle(float angle) { this.angle = angle; }
    
    public float getPower() { return power; }
    public void setPower(float power) { this.power = power; }
    
    public int getBounces() { return bounces; }
    public void setBounces(int bounces) { this.bounces = bounces; }
    
    public boolean isWillScore() { return willScore; }
    public void setWillScore(boolean willScore) { this.willScore = willScore; }
}