package com.awesomedev.ocrapp;

import org.opencv.core.Point;

/**
 * Created by sparsh on 12/29/17.
 */

public class ContourRect {
    public double length,breadth;
    public Point[] vertices;

    public ContourRect(double length, double breadth , Point[] points) {
        this.length = length;
        this.breadth = breadth;

        this.vertices= new Point[4];
/*        for (int i=0 ; i < vertices.length; i++){
            vertices[i] = new Point();

            vertices[i].x = points[i].x;
            vertices[i].y = points[i].y;
        }*/
    }
}
