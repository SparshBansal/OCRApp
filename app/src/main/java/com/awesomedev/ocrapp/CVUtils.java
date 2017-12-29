package com.awesomedev.ocrapp;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by sparsh on 12/21/17.
 */

public class CVUtils {


    public static Bitmap getBitmapFromMat(Mat cvImg){
        Bitmap result = Bitmap.createBitmap(cvImg.cols() , cvImg.rows() , Bitmap.Config.RGB_565);
        Utils.matToBitmap(cvImg , result);
        return result;
    }

    // helper method to apply mask to an image
    public static Mat applyMask(Mat cvImage , Mat mask){

        // binarize mask
        Imgproc.threshold(mask,mask,254,255,Imgproc.THRESH_BINARY);

        Mat cvMaskedImage = new Mat();
        cvImage.copyTo(cvMaskedImage , mask);

        return cvMaskedImage;
    }

    // helper method to create an edge mask (Canny Edge Detector)
    public static Mat createCannyEdgeMask(Mat cvImg){
        Mat cvEdgeMap= new Mat();
        Imgproc.Canny(cvImg,cvEdgeMap,50,100);
        Core.bitwise_not(cvEdgeMap,cvEdgeMap);
        return cvEdgeMap;
    }


    // helper method to binarize image
    public static Mat binarize(Mat cvImg){
        Mat cvBinarized = new Mat();
        Mat cvGrayscaled = new Mat();

        Imgproc.cvtColor(cvImg,cvGrayscaled,Imgproc.COLOR_RGB2GRAY);
        Imgproc.adaptiveThreshold(cvGrayscaled,cvBinarized,255,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY,41,12);

        return cvBinarized;
    }


    public static Mat reduceNoise(Mat cvImg){
        Mat cvResult = new Mat();
        Imgproc.medianBlur(cvImg , cvResult ,3);
        return cvResult;
    }

    public static Mat smoothen(Mat cvImg){
        Mat cvSmoothenedImage = new Mat();
        Imgproc.GaussianBlur(cvImg,cvSmoothenedImage,new Size(3,3),0);
        return cvSmoothenedImage;
    }

    public static List<MatOfPoint> findContours(Mat cvImg){
        Mat cvContourMap = new Mat();
        Mat cvInvertedImage = new Mat();
        Core.bitwise_not(cvImg , cvInvertedImage);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(cvInvertedImage, contours, new Mat() , Imgproc.RETR_EXTERNAL , Imgproc.CHAIN_APPROX_SIMPLE);

        return contours;
    }

    public static Mat drawContours(Mat cvImage , List<MatOfPoint> contours){

        // find best fit rectangle for each contour
        Scalar green = new Scalar(81,190,0);
        for (MatOfPoint contour : contours){
            RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));
            drawRotatedRect(cvImage, rect , green , 4);
        }
        return cvImage;
    }

    private static void drawRotatedRect(Mat image , RotatedRect rotatedRect , Scalar color, int thickness){
        Point[] vertices = new Point[4];
        rotatedRect.points(vertices);
        MatOfPoint points = new MatOfPoint(vertices);
        Imgproc.drawContours(image, Arrays.asList(points),-1, color,thickness);
    }


    public static Mat erode(Mat cvImg , int kernelSize){
        Mat cvErodedImage= new Mat();
        Imgproc.erode(cvImg,cvErodedImage,Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(kernelSize,kernelSize)));
        return cvErodedImage;
    }

    public static Mat dilate(Mat cvImg , int kernelSize){
        Mat cvDilatedImage = new Mat();
        Imgproc.dilate(cvImg, cvDilatedImage, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize,kernelSize)));
        return cvDilatedImage;
    }

}
