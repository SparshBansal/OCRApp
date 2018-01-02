package com.awesomedev.ocrapp;

import android.graphics.Bitmap;
import android.util.Pair;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.List;

/**
 * Created by sparsh on 1/2/18.
 */

public class CVTextBox {
    Mat mBitmap;
    List<Pair<Point, Point>> rects;
}
