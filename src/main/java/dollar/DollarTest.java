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

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.geom.*;

@SuppressWarnings("serial")
class Canvas extends JPanel {
    private DollarTest test;

    Canvas(DollarTest test) {
        super();
        this.test = test;
    }
    public void paintComponent(Graphics g) {
        ArrayList<Point2D> points = test.getPoints();
        Graphics2D g2 = (Graphics2D) g;

        // get the last template result we matched, if there was one.
        Result result = test.getLastResult();

        if (points == null)
            return;

        for (int i=0 ; i<points.size()-1 ; i++) {
            Point2D p1 = points.get(i);
            Point2D p2 = points.get(i+1);
            g2.setStroke(new BasicStroke(4));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
        }
    }
}

@SuppressWarnings("serial")
public class DollarTest extends JFrame implements MouseListener, MouseMotionListener, ActionListener {
    private ArrayList<Point2D> points = null;
    private DollarRecognizer recog = new DollarRecognizer();
    private JLabel status;
    private JComboBox<String> combo;
    private Result lastResult = null;
    private Canvas panel;
    private static final String[] templateStrings = { "triangle", "x", "rectangle", "circle", "check", "caret", "zig-zag", "arrow", "left square bracket", "right square bracket", "v", "delete", "left curly brace", "right curly brace", "star", "pigtail", "flat", "half note", "quarter note", "eighth note", "sixteenth note", "half rest", "eighth rest", "sixteenth rest" };


    public DollarTest() {
        super("DollarTest");
        setLayout(new BorderLayout());
        JPanel topbox = new JPanel();
        combo = new JComboBox<String>(templateStrings);
        combo.setSelectedIndex(2);
        //combo.addActionListener(this);
        JButton showTemplate = new JButton ("Show Selected Template");
        showTemplate.addActionListener(this);
        topbox.setLayout(new BoxLayout(topbox, BoxLayout.LINE_AXIS));
        topbox.add(combo);
        topbox.add(showTemplate);
        super.add(topbox, "North");
        panel = new Canvas(this);
        panel.setMinimumSize(new Dimension(600, 400));
        panel.setPreferredSize(new Dimension(600, 400));
        super.add(panel, "Center");
        status = new JLabel("Status message");
        super.add(status, "South");
        addMouseListener(this);
        addMouseMotionListener(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setVisible(true);
    }

    public Result getLastResult() {
        return lastResult;
    }

    public ArrayList<Point2D> getPoints() {
        return points;
    }
    public void actionPerformed(ActionEvent ev) {
        String templateName = (String) combo.getSelectedItem();
        lastResult = null;
        Unistroke template = recog.getTemplate(templateName);
        if (template == null) {
            status.setText("Couldn't find template match");
        }
        points = template.getOriginalPoints();
        repaint();
    }
    public void mouseDragged(MouseEvent ev) {
        // translate to canvas coordinate system
        Point p = SwingUtilities.convertPoint(this, ev.getPoint(), panel);
        points.add(p);
        repaint();
    }
    public void mousePressed(MouseEvent ev) {
        points = new ArrayList<Point2D>();
        // translate to canvas coordinate system
        Point p = SwingUtilities.convertPoint(this, ev.getPoint(), panel);
        points.add(p);
        lastResult = null;
        repaint();
    }
    public void mouseReleased(MouseEvent ev) {
        // translate to canvas coordinate system
        Point p = SwingUtilities.convertPoint(this, ev.getPoint(), panel);
        points.add(p);
        Result result = recog.recognize(points);
        System.out.println("\nGot result: " + result.getName());
        System.out.println("\tScore=" + result.getScore());
        System.out.println("\tBounding Box =" + result.getBoundingBox());
        updateStatus(result);
        this.lastResult = result;
        repaint();
    }
    public void mouseMoved(MouseEvent ev) {
    }
    public void mouseExited(MouseEvent ev) {
    }
    public void mouseEntered(MouseEvent ev) {
    }
    public void mouseClicked(MouseEvent ev) {
    }

    private void updateStatus(Result r) {
        if (r == null) {
            status.setText("null result");
        } else if (r.getName().equals("No match")) {
            status.setText("No match");
        } else {
            Rectangle rect = r.getBoundingBox();
            status.setText(r.getName() + " (score=" + r.getScore() + ") " +
                    " bbox: x=" + rect.getX() + " y=" + rect.getY() + " w=" + rect.getWidth() + " h=" + rect.getHeight());
        }
    }

    public static void main(String[] args) {
        DollarTest dt = new DollarTest();
    }

}