package com.gt.music.gestures;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GestureStroke {
    private final List<Point> points = new ArrayList<>();

    public void add(Point p) {
        points.add(p);
    }

    public List<Point> getPoints() {
        return points;
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public Rectangle getBoundingBox() {
        if (points.isEmpty()) return new Rectangle(0, 0, 0, 0);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Point p : points) {
            if (p.x < minX) minX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.x > maxX) maxX = p.x;
            if (p.y > maxY) maxY = p.y;
        }
        return new Rectangle(minX, minY, (maxX - minX), (maxY - minY));
    }
}
