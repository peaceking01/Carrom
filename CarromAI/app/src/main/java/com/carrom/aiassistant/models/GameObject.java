package com.carrom.aiassistant.models;

import android.graphics.PointF;

public class GameObject {
    private PointF position;
    private float radius;
    private PointF velocity;
    private GameObjectType type;
    private int color;
    
    public GameObject(PointF position, float radius, GameObjectType type) {
        this.position = position;
        this.radius = radius;
        this.velocity = new PointF(0f, 0f);
        this.type = type;
        this.color = 0;
    }
    
    public GameObject(PointF position, float radius, PointF velocity, 
                     GameObjectType type, int color) {
        this.position = position;
        this.radius = radius;
        this.velocity = velocity;
        this.type = type;
        this.color = color;
    }
    
    // Getters and Setters
    public PointF getPosition() { return position; }
    public void setPosition(PointF position) { this.position = position; }
    
    public float getRadius() { return radius; }
    public void setRadius(float radius) { this.radius = radius; }
    
    public PointF getVelocity() { return velocity; }
    public void setVelocity(PointF velocity) { this.velocity = velocity; }
    
    public GameObjectType getType() { return type; }
    public void setType(GameObjectType type) { this.type = type; }
    
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
}