package com.carrom.aiassistant.detection;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;

import com.carrom.aiassistant.models.BoardState;
import com.carrom.aiassistant.models.GameObject;
import com.carrom.aiassistant.models.GameObjectType;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class CarromDetector {

    // ── HSV colour ranges ────────────────────────────────────────────────────
    // Tune these for your specific game's colour scheme
    private static final Scalar WHITE_LOW   = new Scalar(0,   0,   180);
    private static final Scalar WHITE_HIGH  = new Scalar(180, 50,  255);

    private static final Scalar BLACK_LOW   = new Scalar(0,   0,   0);
    private static final Scalar BLACK_HIGH  = new Scalar(180, 255, 80);

    private static final Scalar QUEEN_LOW   = new Scalar(0,   120, 80);
    private static final Scalar QUEEN_HIGH  = new Scalar(15,  255, 255);

    private static final Scalar STRIKER_LOW  = new Scalar(20,  100, 100);
    private static final Scalar STRIKER_HIGH = new Scalar(35,  255, 255);

    // ── Coin size constraints (pixels) ───────────────────────────────────────
    private static final int COIN_MIN_R    = 10;
    private static final int COIN_MAX_R    = 25;
    private static final int STRIKER_MIN_R = 20;
    private static final int STRIKER_MAX_R = 42;

    // ────────────────────────────────────────────────────────────────────────
    //  Public API
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Analyse one captured Bitmap frame and return the full board state.
     * Called from a background thread by ScreenCaptureService.
     */
    public BoardState analyze(Bitmap bitmap) {
        Mat rgba = new Mat();
        Utils.bitmapToMat(bitmap, rgba);

        Mat hsv = new Mat();
        Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(hsv,  hsv, Imgproc.COLOR_RGB2HSV);

        RectF         boardRect  = detectBoardBounds(rgba);
        GameObject    striker    = detectStriker(hsv, boardRect);
        List<GameObject> coins   = detectCoins(hsv, boardRect, striker);
        GameObject    queen      = detectQueen(hsv, boardRect);
        List<GameObject> pockets = buildPockets(boardRect);

        rgba.release();
        hsv.release();

        return new BoardState(striker, coins, queen, pockets, boundaryPoints(boardRect));
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Board Detection
    // ────────────────────────────────────────────────────────────────────────

    private RectF detectBoardBounds(Mat rgba) {
        Mat gray  = new Mat();
        Mat edges = new Mat();
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);
        Imgproc.Canny(gray, edges, 50, 150);

        List<MatOfPoint> contours  = new ArrayList<>();
        Mat              hierarchy = new Mat();
        Imgproc.findContours(edges, contours, hierarchy,
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double screenArea = rgba.width() * rgba.height();
        Rect   best       = null;
        double bestArea   = 0;

        for (MatOfPoint c : contours) {
            double area = Imgproc.contourArea(c);
            if (area > screenArea * 0.25 && area > bestArea) {
                bestArea = area;
                best     = Imgproc.boundingRect(c);
            }
        }

        gray.release(); edges.release(); hierarchy.release();

        if (best == null) {
            // Fallback: treat entire screen as board
            return new RectF(0, 0, rgba.width(), rgba.height());
        }
        return new RectF(best.x, best.y, best.x + best.width, best.y + best.height);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Striker Detection
    // ────────────────────────────────────────────────────────────────────────

    private GameObject detectStriker(Mat hsv, RectF board) {
        Mat mask = colorMask(hsv, STRIKER_LOW, STRIKER_HIGH);
        float[][] circles = houghCircles(mask, STRIKER_MIN_R, STRIKER_MAX_R, 50, 25);
        mask.release();

        for (float[] c : circles) {
            if (isInsideBoard(c[0], c[1], board)) {
                return new GameObject(
                    new PointF(c[0], c[1]), c[2],
                    new PointF(0, 0),
                    GameObjectType.STRIKER, 0xFFFFFFFF
                );
            }
        }
        return null;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Coin Detection
    // ────────────────────────────────────────────────────────────────────────

    private List<GameObject> detectCoins(Mat hsv, RectF board, GameObject striker) {
        List<GameObject> result = new ArrayList<>();

        // White coins
        Mat wMask = colorMask(hsv, WHITE_LOW, WHITE_HIGH);
        for (float[] c : houghCircles(wMask, COIN_MIN_R, COIN_MAX_R, 50, 18)) {
            if (isInsideBoard(c[0], c[1], board) && !samePos(c[0], c[1], striker)) {
                result.add(makeCoin(c, GameObjectType.WHITE_COIN, 0xFFEEEEEE));
            }
        }
        wMask.release();

        // Black coins
        Mat bMask = colorMask(hsv, BLACK_LOW, BLACK_HIGH);
        for (float[] c : houghCircles(bMask, COIN_MIN_R, COIN_MAX_R, 50, 18)) {
            if (isInsideBoard(c[0], c[1], board) && !samePos(c[0], c[1], striker)) {
                result.add(makeCoin(c, GameObjectType.BLACK_COIN, 0xFF444444));
            }
        }
        bMask.release();

        return result;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Queen Detection
    // ────────────────────────────────────────────────────────────────────────

    private GameObject detectQueen(Mat hsv, RectF board) {
        Mat mask = colorMask(hsv, QUEEN_LOW, QUEEN_HIGH);
        for (float[] c : houghCircles(mask, COIN_MIN_R, COIN_MAX_R, 50, 18)) {
            if (isInsideBoard(c[0], c[1], board)) {
                mask.release();
                return makeCoin(c, GameObjectType.QUEEN, 0xFFFF0000);
            }
        }
        mask.release();
        return null;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Pocket helpers
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Carrom board has 4 corner pockets.
     * We derive them from the board bounding rect with a fixed margin.
     */
    private List<GameObject> buildPockets(RectF b) {
        float margin = (b.right - b.left) * 0.06f;
        float r      = margin * 0.9f;
        List<GameObject> pockets = new ArrayList<>();

        pockets.add(pocketAt(b.left  + margin, b.top    + margin, r));
        pockets.add(pocketAt(b.right - margin, b.top    + margin, r));
        pockets.add(pocketAt(b.left  + margin, b.bottom - margin, r));
        pockets.add(pocketAt(b.right - margin, b.bottom - margin, r));

        return pockets;
    }

    private GameObject pocketAt(float x, float y, float r) {
        return new GameObject(new PointF(x, y), r, new PointF(0, 0),
            GameObjectType.POCKET, 0xFF000000);
    }

    private List<PointF> boundaryPoints(RectF b) {
        List<PointF> pts = new ArrayList<>();
        pts.add(new PointF(b.left,  b.top));
        pts.add(new PointF(b.right, b.top));
        pts.add(new PointF(b.right, b.bottom));
        pts.add(new PointF(b.left,  b.bottom));
        return pts;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  OpenCV helpers
    // ────────────────────────────────────────────────────────────────────────

    /** Create a binary mask for an HSV range with morphological cleanup. */
    private Mat colorMask(Mat hsv, Scalar low, Scalar high) {
        Mat mask   = new Mat();
        Mat kernel = Imgproc.getStructuringElement(
            Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        Core.inRange(hsv, low, high, mask);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN,  kernel);
        kernel.release();
        return mask;
    }

    /**
     * Run HoughCircles on a binary mask.
     * Returns array of [cx, cy, radius] triples.
     */
    private float[][] houghCircles(Mat mask, int minR, int maxR,
                                   double param1, double param2) {
        Mat circles = new Mat();
        Imgproc.HoughCircles(mask, circles,
            Imgproc.HOUGH_GRADIENT,
            1.0,   // dp
            15.0,  // minDist between circle centres
            param1, param2,
            minR, maxR);

        float[][] result = new float[circles.cols()][3];
        for (int i = 0; i < circles.cols(); i++) {
            double[] c = circles.get(0, i);
            result[i][0] = (float) c[0]; // cx
            result[i][1] = (float) c[1]; // cy
            result[i][2] = (float) c[2]; // radius
        }
        circles.release();
        return result;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Geometry helpers
    // ────────────────────────────────────────────────────────────────────────

    private boolean isInsideBoard(float cx, float cy, RectF board) {
        if (board == null) return true;
        float margin = 10f;
        return cx > board.left + margin && cx < board.right  - margin
            && cy > board.top  + margin && cy < board.bottom - margin;
    }

    private boolean samePos(float cx, float cy, GameObject obj) {
        if (obj == null) return false;
        float dx = cx - obj.getPosition().x;
        float dy = cy - obj.getPosition().y;
        return Math.sqrt(dx * dx + dy * dy) < obj.getRadius() * 2.0f;
    }

    private GameObject makeCoin(float[] c, GameObjectType type, int color) {
        return new GameObject(
            new PointF(c[0], c[1]), c[2],
            new PointF(0, 0), type, color
        );
    }
}
