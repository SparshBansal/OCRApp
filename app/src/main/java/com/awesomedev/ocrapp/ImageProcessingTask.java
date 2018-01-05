package com.awesomedev.ocrapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Pair;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.io.IOException;

/**
 * Created by sparsh on 1/3/18.
 */

public class ImageProcessingTask extends AsyncTask<Bitmap, Void, Mat> {

    Context context=null;
    private OnImageProcessedListener onImageProcessedListener = null;

    ProgressDialog progressDialog;

    public ImageProcessingTask(Context context){
        this.context = context;
        progressDialog = new ProgressDialog(context);
    }

    public interface OnImageProcessedListener {
        void onImageProcessed(Mat mat);
    }

    public void setOnImageProcessedListener(OnImageProcessedListener onImageProcessedListener){
        this.onImageProcessedListener = onImageProcessedListener;
    }

    private Mat processImage(Bitmap image) throws IOException {
        Mat cvImage = new Mat();
        Utils.bitmapToMat(image, cvImage);

        // grayscale the image
        FileUtils.saveBitmap(CVUtils.getBitmapFromMat(cvImage) , context);
        // Create and apply edge mask

        CVTextBox cvMserImage = CVUtils.mserTextDetection(cvImage);

        // Binarize Image
        Mat cvBinarizedImage = CVUtils.binarize(cvImage);


        // remove anything that lies outside the text boxes
        // TODO -- determine padding
        for (int y = 0; y < cvBinarizedImage.height(); y++) {
            for (int x = 0; x < cvBinarizedImage.width(); x++) {
                boolean liesInside = false;
                for (Pair<Point, Point> rect : cvMserImage.rects) {

                    int paddingW = (int) (Math.abs(rect.second.x - rect.first.x)/8);
                    int paddingH = (int) (Math.abs(rect.second.y - rect.first.y)/16);
                    if (x >= rect.first.x - paddingW && x <= rect.second.x + paddingW && y >= rect.first.y - paddingH && y <= rect.second.y + paddingH) {
                        liesInside = true;
                    }
                }

                if (!liesInside) {
                    double[] data = cvBinarizedImage.get(y, x);
                    data[0] = 255;
                    cvBinarizedImage.put(y, x, data);
                }
            }
        }

        Mat noiselessImage = CVUtils.reduceNoise(cvBinarizedImage);

        Mat cvDeskewedImage = CVUtils.deskewImage(noiselessImage);
        return cvDeskewedImage;

    }

    @Override
    protected Mat doInBackground(Bitmap... bitmaps) {

        Bitmap orgBitmap = bitmaps[0];
        try {
            Mat cleanedImage = processImage(orgBitmap);
            return cleanedImage;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Mat mat) {
        // disable loader
        progressDialog.hide();
        this.onImageProcessedListener.onImageProcessed(mat);
    }

    @Override
    protected void onPreExecute() {
        // show loader
        progressDialog.setMessage("Processing Image");
        progressDialog.show();
    }

}
