package com.carrom.aiassistant.utils;

import android.graphics.PointF;
import com.carrom.aiassistant.models.Circle;

public class MathUtils {
    
    public static final float FRICTION_COEFFICIENT = 0.98f;
    public static final float COLLISION_DAMPING = 0.85f;
    public static final float MIN_VELOCITY = 0.1f;
    public static final float MAX_SIMULATION_TIME = 5000f;
    public static final float TIME_STEP = 16f;
    
    public static float distance(PointF p1, PointF p2) {
        float dx = p2.x - p1.x;
        float dy = p2.y - p1.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    public static float angle(PointF from, PointF to) {
        return (float) Math.atan2(to.y - from.y, to.x - from.x);
    }
    
    public static float angleDegrees(PointF from, PointF to) {
        return (float) Math.toDegrees(angle(from, to));
    }
    
    public static PointF normalize(PointF vector) {
        float magnitude = (float) Math.sqrt(vector.x * vector.x + vector.y * vector.y);
        if (magnitude > 0) {
            return new PointF(vector.x / magnitude, vector.y / magnitude);
        }
        return new PointF(0f, 0f);
    }
    
    public static float dotProduct(PointF v1, PointF v2) {
        return v1.x * v2.x + v1.y * v2.y;
    }
    
    public static PointF reflect(PointF velocity, PointF normal) {
        float dot = 2 * dotProduct(velocity, normal);
        return new PointF(
            velocity.x - dot * normal.x,
            velocity.y - dot * normal.y
        );
    }
    
    public static PointF applyFriction(PointF velocity, float deltaTime) {
        float frictionFactor = (float) Math.pow(FRICTION_COEFFICIENT, deltaTime / TIME_STEP);
        return new PointF(velocity.x * frictionFactor, velocity.y * frictionFactor);
    }
    
    public static boolean isCircleIntersecting(Circle c1, Circle c2) {
        return distance(c1.getCenter(), c2.getCenter()) <= (c1.getRadius() + c2.getRadius());
    }
    
    public static boolean lineIntersectsCircle(PointF lineStart, PointF lineEnd, Circle circle) {
        float dx = lineEnd.x - lineStart.x;
        float dy = lineEnd.y - lineStart.y;
        float fx = lineStart.x - circle.getCenter().x;
        float fy = lineStart.y - circle.getCenter().y;
        
        float a = dx * dx + dy * dy;
        float b = 2 * (fx * dx + fy * dy);
        float c = (fx * fx + fy * fy) - circle.getRadius() * circle.getRadius();
        
        float discriminant = b * b - 4 * a * c;
        return discriminant >= 0;
    }
    
    public static PointF vectorFromAngle(float angleDegrees, float magnitude) {
        float angleRad = (float) Math.toRadians(angleDegrees);
        return new PointF(
            (float) Math.cos(angleRad) * magnitude,
            (float) Math.sin(angleRad) * magnitude
        );
    }
    
    public static float magnitude(PointF vector) {
        return (float) Math.sqrt(vector.x * vector.x + vector.y * vector.y);
    }
    
    public static PointF add(PointF v1, PointF v2) {
        return new PointF(v1.x + v2.x, v1.y + v2.y);
    }
    
    public static PointF subtract(PointF v1, PointF v2) {
        return new PointF(v1.x - v2.x, v1.y - v2.y);
    }
    
    public static PointF multiply(PointF v, float scalar) {
        return new PointF(v.x * scalar, v.y * scalar);
    }
}