/**
 * The $1 Unistroke Recognizer
 *
 *  Jacob O. Wobbrock, Ph.D.
 *  The Information School
 *  University of Washington
 *  Seattle, WA 98195-2840
 *  wobbrock@uw.edu
 *
 *  Andrew D. Wilson, Ph.D.
 *  Microsoft Research
 *  One Microsoft Way
 *  Redmond, WA 98052
 *  awilson@microsoft.com
 *
 *  Yang Li, Ph.D.
 *  Department of Computer Science and Engineering
 *  University of Washington
 *  Seattle, WA 98195-2840
 *  yangli@cs.washington.edu
 *
 * The academic publication for the $1 recognizer, and what should be
 * used to cite it, is:
 *
 *     Wobbrock, J.O., Wilson, A.D. and Li, Y. (2007). Gestures without
 *     libraries, toolkits or training: A $1 recognizer for user interface
 *     prototypes. Proceedings of the ACM Symposium on User Interface
 *     Software and Technology (UIST '07). Newport, Rhode Island (October
 *     7-10, 2007). New York: ACM Press, pp. 159-168.
 *     https://dl.acm.org/citation.cfm?id=1294238
 *
 * The Protractor enhancement was separately published by Yang Li and programmed
 * here by Jacob O. Wobbrock:
 *
 *     Li, Y. (2010). Protractor: A fast and accurate gesture
 *     recognizer. Proceedings of the ACM Conference on Human
 *     Factors in Computing Systems (CHI '10). Atlanta, Georgia
 *     (April 10-15, 2010). New York: ACM Press, pp. 2169-2172.
 *     https://dl.acm.org/citation.cfm?id=1753654
 *
 * This software is distributed under the "New BSD License" agreement:
 *
 * Copyright (C) 2007-2012, Jacob O. Wobbrock, Andrew D. Wilson and Yang Li.
 * All rights reserved. Last updated July 14, 2018.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the names of the University of Washington nor Microsoft,
 *      nor the names of its contributors may be used to endorse or promote
 *      products derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Jacob O. Wobbrock OR Andrew D. Wilson
 * OR Yang Li BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Port to Java and other modifications by Keith Edwards, 2019.
 **/
package dollar;

import java.util.ArrayList;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.*;

public class Unistroke {
    static final double SQUARE_SIZE = 250.0;
    static final Point ORIGIN = new Point(0,0);
    static final int NUM_POINTS = 64;
    static final double DIAGONAL = Math.sqrt(SQUARE_SIZE * SQUARE_SIZE + SQUARE_SIZE * SQUARE_SIZE);
    static final double HALF_DIAGONAL = 0.5 * DIAGONAL;
    static final double ANGLE_RANGE = deg2Rad(45.0);
    static final double ANGLE_PRECISION = deg2Rad(2.0);
    static final double PHI = 0.5 * (-1.0 + Math.sqrt(5.0));	// Golden Ratio

    private String name;
    private ArrayList<Point2D> points;
    private ArrayList<Point2D> originalPoints; // keep this around for template demo mode
    private double radians;
    private Rectangle origBBox;

    Unistroke(String n, ArrayList<Point2D> p) {
        this.originalPoints = p;
        this.name = n;
        this.points = resample(p, NUM_POINTS);
        //if (this.points.size() != NUM_POINTS) {
        //	System.out.println("+++ Unistroke " + name + " has " + this.points.size() + " points.");
        //}

        this.radians = indicativeAngle(this.points);
        this.origBBox = boundingBox(this.points);
        this.points = rotateBy(this.points, -this.radians);
        this.points = scaleTo(this.points, SQUARE_SIZE);
        this.points = translateTo(this.points, ORIGIN);
    }

    Unistroke(String n, Point2D[] p) {
        this.name = n;
        this.originalPoints = new ArrayList<Point2D>();
        for (int i=0 ; i<p.length ; i++) {
            this.originalPoints.add(p[i]);
        }
        this.points = resample(this.originalPoints, NUM_POINTS);

        this.radians = indicativeAngle(this.points);
        this.points = rotateBy(this.points, -this.radians);
        this.points = scaleTo(this.points, SQUARE_SIZE);
        this.points = translateTo(this.points, ORIGIN);
    }

    public String getName() {
        return name;
    }
    // This is the method you should use to get the resampled/rotated/etc stroke points
    public ArrayList<Point2D> getPoints() {
        return points;
    }
    // This method returns the original (unprocessed) points. Only used for template demo mode.
    public ArrayList<Point2D> getOriginalPoints() {
        return originalPoints;
    }
    public double getRadians() {
        return radians;
    }
    public Rectangle getOriginalBBox() {
        return origBBox;
    }

    static ArrayList<Point2D> resample(ArrayList<Point2D> origPoints, int n) {
        int origSize = origPoints.size();
        double intervalLength = pathLength(origPoints) / (n-1); // interval length
        double D = 0.0;
        ArrayList<Point2D> points = new ArrayList<Point2D>();
        for (int i=0 ; i<origPoints.size() ; i++) {
            points.add(origPoints.get(i));
        }
        ArrayList<Point2D> newPoints = new ArrayList<Point2D>();

        newPoints.add(points.get(0));

        for (int i=1 ; i<points.size() ; i++) {
            Point2D pt1 = points.get(i-1);
            Point2D pt2 = points.get(i);

            double d = distance(pt1, pt2);
            if ((D + d) >= intervalLength) {
                double qx = pt1.getX() + ((intervalLength - D) / d) * (pt2.getX() - pt1.getX());
                double qy = pt1.getY() + ((intervalLength - D) / d) * (pt2.getY() - pt1.getY());
                Point2D.Double q = new Point2D.Double(qx, qy);
                newPoints.add(q); // add new point 'q'
                points.add(i, q); // insert q at position i in points s.t. q will be the next i
                D = 0.0;
            } else {
                D += d;
            }
        }
        // Sometimes we fall a rounding-error short of adding the last point, so add it if so.
        if (newPoints.size() == n-1) {
            newPoints.add(new Point2D.Double(points.get(points.size() - 1).getX(),
                    points.get(points.size() - 1).getY()));
        }
        return newPoints;
    }

    static double indicativeAngle(ArrayList<Point2D> points) {
        Point2D c = centroid(points);
        return Math.atan2(c.getY() - points.get(0).getY(), c.getX() - points.get(0).getX());
    }

    // rotates points around centroid
    static ArrayList<Point2D> rotateBy(ArrayList<Point2D> points, double radians) {
        Point2D c = centroid(points);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        ArrayList<Point2D> newPoints = new ArrayList<Point2D>();

        for (int i=0 ; i<points.size() ; i++) {
            double qx = (points.get(i).getX() - c.getX()) * cos - (points.get(i).getY() - c.getY()) * sin + c.getX();
            double qy = (points.get(i).getX() - c.getX()) * sin + (points.get(i).getY() - c.getY()) * cos + c.getY();
            newPoints.add(new Point2D.Double(qx, qy));

        }

        return newPoints;
    }

    // non-uniform scale; assumes 2D gestures (i.e., no lines)
    static ArrayList<Point2D> scaleTo(ArrayList<Point2D> points, double size) {
        Rectangle bbox = boundingBox(points);
        ArrayList<Point2D> newPoints = new ArrayList<Point2D>();

        for (int i = 0; i < points.size(); i++) {
            double qx = points.get(i).getX() * (size / bbox.getWidth());
            double qy = points.get(i).getY() * (size / bbox.getHeight());
            newPoints.add(new Point2D.Double(qx, qy));

        }
        return newPoints;
    }

    // translates points' centroid
    static ArrayList<Point2D> translateTo(ArrayList<Point2D> points, Point2D pt) {
        Point2D c = centroid(points);
        ArrayList<Point2D> newPoints = new ArrayList<Point2D>();
        for (int i = 0; i < points.size(); i++) {
            double qx = points.get(i).getX() + pt.getX() - c.getX();
            double qy = points.get(i).getY() + pt.getY() - c.getY();
            newPoints.add(new Point2D.Double(qx, qy));

        }
        return newPoints;
    }

    static double distanceAtBestAngle(ArrayList<Point2D> points, Unistroke template,
                                      double a, double b, double threshold) {
        double x1 = PHI * a + (1.0 - PHI) * b;
        double f1 = distanceAtAngle(points, template, x1);
        double x2 = (1.0 - PHI) * a + PHI * b;
        double f2 = distanceAtAngle(points, template, x2);

        while (Math.abs(b - a) > threshold) {
            if (f1 < f2) {
                b = x2;
                x2 = x1;
                f2 = f1;
                x1 = PHI * a + (1.0 - PHI) * b;
                f1 = distanceAtAngle(points, template, x1);
            } else {
                a = x1;
                x1 = x2;
                f1 = f2;
                x2 = (1.0 - PHI) * a + PHI * b;
                f2 = distanceAtAngle(points, template, x2);
            }
        }
        return Math.min(f1, f2);
    }

    static double distanceAtAngle(ArrayList<Point2D> points, Unistroke template, double radians) {
        ArrayList<Point2D> newPoints = rotateBy(points, radians);
        return pathDistance(newPoints, template.points);
    }

    static Point2D centroid(ArrayList<Point2D> points) {
        double x = 0.0, y = 0.0;
        for (int i = 0; i < points.size(); i++) {
            x += points.get(i).getX();
            y += points.get(i).getY();
        }
        x /= points.size();
        y /= points.size();

        return new Point2D.Double(x, y);
    }

    static Rectangle boundingBox(ArrayList<Point2D> points) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (int i = 0; i < points.size(); i++) {
            minX = Math.min(minX, (int) points.get(i).getX());
            minY = Math.min(minY, (int) points.get(i).getY());
            maxX = Math.max(maxX, (int) points.get(i).getX());
            maxY = Math.max(maxY, (int) points.get(i).getY());
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    static double pathDistance(ArrayList<Point2D> pts1, ArrayList<Point2D> pts2) {
        double d = 0.0;
        for (int i = 0; i < pts1.size(); i++) // assumes pts1.length == pts2.length
            d += distance(pts1.get(i), pts2.get(i));
        return d / pts1.size();
    }

    static double pathLength(ArrayList<Point2D> points) {
        double d = 0.0;
        for (int i = 1; i < points.size(); i++)
            d += distance(points.get(i - 1), points.get(i));
        return d;
    }

    static double distance(Point2D p1, Point2D p2) {
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    static double deg2Rad(double d) {
        return (d * Math.PI / 180.0);
    }
}