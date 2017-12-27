package com.awesomedev.ocrapp;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

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

    public static Mat smoothen(Mat cvImg){
        Mat cvSmoothenedImage = new Mat();
        Imgproc.GaussianBlur(cvImg,cvSmoothenedImage,new Size(3,3),0);
        return cvSmoothenedImage;
    }

    public static Mat dilate(Mat cvImg){
        Mat cvDilatedImage = new Mat();
        Imgproc.erode(cvImg, cvDilatedImage,Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(4,4)));
        return cvDilatedImage;
    }

}
