package com.carrom.aiassistant.models;

import android.graphics.PointF;
import java.util.List;

public class BoardState {
    private GameObject striker;
    private List<GameObject> coins;
    private GameObject queen;
    private List<GameObject> pockets;
    private List<PointF> boundaries;
    private long timestamp;
    
    public BoardState(GameObject striker, List<GameObject> coins, GameObject queen,
                     List<GameObject> pockets, List<PointF> boundaries) {
        this.striker = striker;
        this.coins = coins;
        this.queen = queen;
        this.pockets = pockets;
        this.boundaries = boundaries;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public GameObject getStriker() { return striker; }
    public void setStriker(GameObject striker) { this.striker = striker; }
    
    public List<GameObject> getCoins() { return coins; }
    public void setCoins(List<GameObject> coins) { this.coins = coins; }
    
    public GameObject getQueen() { return queen; }
    public void setQueen(GameObject queen) { this.queen = queen; }
    
    public List<GameObject> getPockets() { return pockets; }
    public void setPockets(List<GameObject> pockets) { this.pockets = pockets; }
    
    public List<PointF> getBoundaries() { return boundaries; }
    public void setBoundaries(List<PointF> boundaries) { this.boundaries = boundaries; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}