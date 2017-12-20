package com.awesomedev.ocrapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_TAKE_PHOTO = 1;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "static initializer: failure");
        } else {
            Log.d(TAG, "static initializer: success");
        }
    }


    ImageView ivOrg = null, ivProcessed = null;
    private String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivOrg = (ImageView) findViewById(R.id.iv_original);
        ivProcessed = (ImageView) findViewById(R.id.iv_processed);


        ivOrg.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // launch the camera application
                    try {
                        File photoFile = createImageFile();
                        if (photoFile != null) {
                            Uri photoUri = FileProvider.getUriForFile(MainActivity.this, "com.example.android.fileprovider",photoFile);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK){
            Toast.makeText(this, "Picture Capture Successful", Toast.LENGTH_SHORT).show();
            Bitmap orgPic = setPic();

            // try image processing
            try {
                processImage(orgPic);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            Toast.makeText(this, "Picture Capture Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap setPic(){
        int targetW = ivOrg.getWidth();
        int targetH = ivOrg.getHeight();

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(mCurrentPhotoPath,bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW/targetW , photoH/targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Bitmap image = BitmapFactory.decodeFile(mCurrentPhotoPath,bmOptions);
        ivOrg.setImageBitmap(image);

        return image;
    }


    private Bitmap getBitmapFromMat(Mat cvImg){
        Bitmap result = Bitmap.createBitmap(cvImg.cols() , cvImg.rows() , Bitmap.Config.RGB_565);
        Utils.matToBitmap(cvImg , result);
        return result;
    }

    // helper method to apply mask to an image
    private Mat applyMask(Mat cvImage , Mat mask){

        // binarize mask
        Imgproc.threshold(mask,mask,254,255,Imgproc.THRESH_BINARY);

        Mat cvMaskedImage = new Mat();
        cvImage.copyTo(cvMaskedImage , mask);

        return cvMaskedImage;
    }

    // helper method to create an edge mask (Canny Edge Detector)
    private Mat createCannyEdgeMask(Mat cvImg){
        Mat cvEdgeMap= new Mat();
        Imgproc.Canny(cvImg,cvEdgeMap,80,100);
        Core.bitwise_not(cvEdgeMap,cvEdgeMap);
        return cvEdgeMap;
    }

    // helper method to binarize image
    private Mat binarize(Mat cvImg){
        Mat cvBinarized = new Mat();
        Mat cvGrayscaled = new Mat();

        Imgproc.cvtColor(cvImg,cvGrayscaled,Imgproc.COLOR_RGB2GRAY);
        Imgproc.adaptiveThreshold(cvGrayscaled,cvBinarized,255,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY,15,10);

        return cvBinarized;
    }

    private Mat smoothen(Mat cvImg){
        Mat cvSmoothenedImage = new Mat();
        Imgproc.GaussianBlur(cvImg,cvSmoothenedImage,new Size(5,5),0);
        return cvSmoothenedImage;
    }

    private void saveBitmap(Bitmap bitmap) throws IOException {
        File photoFile = createImageFile();
        FileOutputStream outputStream = new FileOutputStream(photoFile);

        bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        outputStream.flush();
        outputStream.close();

    }

    private void processImage(Bitmap image) throws IOException {
        Mat cvImage = new Mat();
        Utils.bitmapToMat(image,cvImage);

        // Create and apply edge mask
        Mat cvEdgeMask = createCannyEdgeMask(cvImage);
        Mat cvMaskedImage = applyMask(cvImage,cvEdgeMask);

        // Binarize Image
        Mat cvBinarizedImage = binarize(cvMaskedImage);
        Mat cvSmoothenedImage = smoothen(cvBinarizedImage);

        // convert mat to bitmap
        Bitmap result = getBitmapFromMat(cvSmoothenedImage);

        // save bitmap
        saveBitmap(result);
        ivProcessed.setImageBitmap(result);
    }
}
