package com.awesomedev.ocrapp;

import android.graphics.Bitmap;
import android.util.Pair;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
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

    public static CVTextBox mserTextDetection(Mat mat){
        Mat imageMat2 = new Mat();
        Imgproc.cvtColor(mat , imageMat2, Imgproc.COLOR_RGB2GRAY);
        Mat mRgba = mat;
        Mat mGray = imageMat2;

        CVTextBox textBox = new CVTextBox();

        Scalar CONTOUR_COLOR = new Scalar(1, 255, 128, 0);
        MatOfKeyPoint keyPoint = new MatOfKeyPoint();
        List<KeyPoint> listPoint = new ArrayList<>();
        KeyPoint kPoint = new KeyPoint();
        Mat mask = Mat.zeros(mGray.size(), CvType.CV_8UC1);
        int rectanx1;
        int rectany1;
        int rectanx2;
        int rectany2;

        Scalar zeros = new Scalar(0,0,0);
        List<MatOfPoint> contour2 = new ArrayList<>();
        Mat kernel = new Mat(1, 50, CvType.CV_8UC1, Scalar.all(255));
        Mat morByte = new Mat();
        Mat hierarchy = new Mat();

        Rect rectan3 = new Rect();
        int imgSize = mRgba.height() * mRgba.width();

        if(true){
            FeatureDetector detector = FeatureDetector.create(FeatureDetector.MSER);
            detector.detect(mGray, keyPoint);
            listPoint = keyPoint.toList();
            for(int ind = 0; ind < listPoint.size(); ++ind){
                kPoint = listPoint.get(ind);
                rectanx1 = (int ) (kPoint.pt.x - 0.5 * kPoint.size);
                rectany1 = (int ) (kPoint.pt.y - 0.5 * kPoint.size);

                rectanx2 = (int) (kPoint.size);
                rectany2 = (int) (kPoint.size);
                if(rectanx1 <= 0){
                    rectanx1 = 1;
                }
                if(rectany1 <= 0){
                    rectany1 = 1;
                }
                if((rectanx1 + rectanx2) > mGray.width()){
                    rectanx2 = mGray.width() - rectanx1;
                }
                if((rectany1 + rectany2) > mGray.height()){
                    rectany2 = mGray.height() - rectany1;
                }
                Rect rectant = new Rect(rectanx1, rectany1, rectanx2, rectany2);
                Mat roi = new Mat(mask, rectant);
                roi.setTo(CONTOUR_COLOR);
            }
            Imgproc.morphologyEx(mask, morByte, Imgproc.MORPH_DILATE, kernel);
            Imgproc.findContours(morByte, contour2, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
            List<Pair<Point,Point>> rects = new ArrayList<>();
            for(int i = 0; i<contour2.size(); ++i){
                rectan3 = Imgproc.boundingRect(contour2.get(i));
                if(rectan3.area() > 0.5 * imgSize || rectan3.area()<100 || rectan3.width / rectan3.height < 2){
                    Mat roi = new Mat(morByte, rectan3);
                    roi.setTo(zeros);
                }else{
                    Imgproc.rectangle(mRgba, rectan3.br(), rectan3.tl(), CONTOUR_COLOR);
                    rects.add(new Pair<Point, Point>(rectan3.tl() , rectan3.br()));
                }
            }
            textBox.mBitmap = mRgba;
            textBox.rects = rects;
        }
        return textBox;
    }
}
