package com.carrom.aiassistant.models;

import android.graphics.PointF;

public class Circle {
    private PointF center;
    private float radius;
    private int color;
    
    public Circle(PointF center, float radius) {
        this.center = center;
        this.radius = radius;
        this.color = 0;
    }
    
    public Circle(PointF center, float radius, int color) {
        this.center = center;
        this.radius = radius;
        this.color = color;
    }
    
    public PointF getCenter() {
        return center;
    }
    
    public void setCenter(PointF center) {
        this.center = center;
    }
    
    public float getRadius() {
        return radius;
    }
    
    public void setRadius(float radius) {
        this.radius = radius;
    }
    
    public int getColor() {
        return color;
    }
    
    public void setColor(int color) {
        this.color = color;
    }
}