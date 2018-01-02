package com.awesomedev.ocrapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import droidninja.filepicker.FilePickerActivity;
import droidninja.filepicker.FilePickerBuilder;
import droidninja.filepicker.FilePickerConst;

public class MainActivity extends AppCompatActivity implements OCRTask.AsyncResponse {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_TAKE_PHOTO = 1;
    public static final String EXTRA_DETECTED_TEXT = "DETECTED TEXT";
    private static final int READ_EXTERNAL_STORAGE_PERMISSION = 99;
    public static final String EXTRA_CONTOUR_FILE_PATH = "CONTOUR_FILE_PATH";


    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "static initializer: failure");
        } else {
            Log.d(TAG, "static initializer: success");
        }
    }

    private ImageView ivOrg = null, ivProcessed = null;
    private Button bProcessText = null, bPlotContours = null;

    private String mCurrentPhotoPath;
    private Tesseract tesseract;

    private Bitmap processedImage = null;

    private Uri photoUri = null;
    private String mCurrentLogFilePath;

    List<MatOfPoint> contours = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivOrg = findViewById(R.id.iv_original);
        ivProcessed = findViewById(R.id.iv_processed);
        bProcessText = findViewById(R.id.b_process_text);
        bPlotContours = findViewById(R.id.b_plot_graph);

        tesseract = new Tesseract(MainActivity.this);
        tesseract.setupTesseract();

        bProcessText.setVisibility(View.INVISIBLE);
        bPlotContours.setVisibility(View.INVISIBLE);

        ivOrg.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    launchFilePicker();
                    return;
                }
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION);
            }
        });


        bProcessText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start the text detection
                if (processedImage == null) {
                    Toast.makeText(MainActivity.this, "Please capture a pic", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(MainActivity.this, "Detecting text", Toast.LENGTH_SHORT).show();
                OCRTask ocrTask = new OCRTask(MainActivity.this, tesseract, MainActivity.this);
                ocrTask.execute(processedImage);
                return;
            }
        });

        bPlotContours.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (contours != null) {

                    File contourFile = null;
                    try {
                        contourFile = createTextFile();
                        FileWriter writer = new FileWriter(contourFile);

                        // find length and breadth
                        for (MatOfPoint contour : contours) {
                            RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));
                            Point[] vertices = new Point[4];

                            rect.points(vertices);

                            double length = euclideanDistance(vertices[0].x, vertices[0].y, vertices[1].x, vertices[1].y);
                            double breadth = euclideanDistance(vertices[1].x, vertices[1].y, vertices[2].x, vertices[2].y);
                            writer.append(String.valueOf(length) + " " + String.valueOf(breadth) + "\n");
                        }

                        writer.flush();
                        writer.close();

                        Intent intent = new Intent(MainActivity.this, ContourPlotActivity.class);
                        intent.putExtra(EXTRA_CONTOUR_FILE_PATH, contourFile.getAbsolutePath());
                        startActivity(intent);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private double euclideanDistance(double x1, double y1, double x2, double y2) {
        double dist = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        return dist;
    }

    private void launchFilePicker() {
        ArrayList<String> selectedFiles = new ArrayList<>();
        FilePickerBuilder.getInstance().setSelectedFiles(selectedFiles).setActivityTheme(R.style.AppTheme).pickPhoto(MainActivity.this);
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

    private File createTextFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String textFileName = "CONTOUR_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File textFile = File.createTempFile(
                textFileName,  /* prefix */
                ".txt",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        return textFile;
    }

    // helper method to launch crop activity
    private void launchCropActivity(Uri imageUri) {
        CropImage.activity().setGuidelines(CropImageView.Guidelines.ON);
        CropImage.activity(imageUri).start(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchFilePicker();
                } else {
                    Toast.makeText(this, "Need permission to read external storage", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            Toast.makeText(this, "Picture Capture Successful", Toast.LENGTH_SHORT).show();

            // try image processing
            // launch crop activity
            launchCropActivity(photoUri);
        } else {
            Toast.makeText(this, "Picture Capture Failed", Toast.LENGTH_SHORT).show();
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            photoUri = result.getUri();
            mCurrentPhotoPath = photoUri.getPath();

            Bitmap orgPic = setPic();
            try {
                processImage(orgPic);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (requestCode == FilePickerConst.REQUEST_CODE_PHOTO && resultCode == RESULT_OK) {
            ArrayList<String> photoPaths = new ArrayList<>();
            photoPaths.addAll(data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_MEDIA));


            if (photoPaths.size() > 0) {
                mCurrentPhotoPath = photoPaths.get(0);
                photoUri = Uri.parse(mCurrentPhotoPath);

                Bitmap orgPic = setPic();
                try {
                    processImage(orgPic);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Bitmap setPic() {
        int targetW = ivOrg.getWidth();
        int targetH = ivOrg.getHeight();

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Log.d(TAG, "setPic: width : " + String.valueOf(targetW) + " ; height : " + String.valueOf(targetH));
        Log.d(TAG, "setPic: ImageActualWidth: " + String.valueOf(photoW) + " ; ImageActualHeight : " + String.valueOf(photoH));

        Bitmap image = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        ivOrg.setBackgroundColor(getColor(R.color.transparent));
        ivOrg.setImageBitmap(image);

        return image;
    }

    private void saveBitmap(Bitmap bitmap) throws IOException {
        File photoFile = createImageFile();
        FileOutputStream outputStream = new FileOutputStream(photoFile);

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        outputStream.flush();
        outputStream.close();

    }

    private void logContourPoints(List<MatOfPoint> contours) throws IOException {
        // log contour points
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String logFileName = "DATA_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File logFile = File.createTempFile(
                logFileName,  /* prefix */
                ".txt",         /* suffix */
                storageDir      /* directory */
        );

        this.mCurrentLogFilePath = logFile.getAbsolutePath();
        FileWriter writer = new FileWriter(logFile);

        for (MatOfPoint contour : contours) {
            Point[] points = new Point[4];
            RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));

            rect.points(points);

            for (Point point : points) {
                writer.append(String.valueOf(point.x) + " " + String.valueOf(point.y) + " ");
            }
            writer.append('\n');
        }
    }

    private void processImage(Bitmap image) throws IOException {
        Mat cvImage = new Mat();
        Utils.bitmapToMat(image, cvImage);

        // grayscale the image
        saveBitmap(CVUtils.getBitmapFromMat(cvImage));
        // Create and apply edge mask

        CVTextBox cvMserImage = CVUtils.mserTextDetection(cvImage);

        // Binarize Image
        Mat cvBinarizedImage = CVUtils.binarize(cvImage);


        // remove anything that lies outside the text boxes
        for (int y=0 ; y < cvBinarizedImage.height() ; y++){
            for (int x=0 ; x < cvBinarizedImage.width(); x++){
                boolean liesInside = false;
                for (Pair<Point,Point> rect : cvMserImage.rects){
                    if (x >= rect.first.x && x <= rect.second.x && y >= rect.first.y && y <= rect.second.y){
                        liesInside = true;
                    }
                }

                if (!liesInside){
                    double[] data = cvBinarizedImage.get(y,x);
                    data[0] = 255;
                    cvBinarizedImage.put(y,x,data);
                }
            }
        }

        Mat noiselessImage = CVUtils.reduceNoise(cvBinarizedImage);

        // Find contours
        contours = CVUtils.findContours(noiselessImage);

        logContourPoints(contours);

        Mat cvContourImage = new Mat();
        noiselessImage.copyTo(cvContourImage);

        Mat cvContourMap = CVUtils.drawContours(cvContourImage, contours);

        // convert mat to bitmap
        // cvErodedImage = CVUtils.erode(cvDilatedImage, 3);
        Bitmap result = CVUtils.getBitmapFromMat(cvContourMap);

        // save bitmap
        saveBitmap(result);

        Log.d(TAG, "processImage: bitmap config : " + result.getConfig().toString());

        Bitmap resultARGB = result.copy(Bitmap.Config.ARGB_8888, true);

        ivProcessed.setImageBitmap(resultARGB);
        this.processedImage = resultARGB;

        this.bProcessText.setVisibility(View.VISIBLE);
        this.bPlotContours.setVisibility(View.VISIBLE);

    }

    @Override
    public void onLinesDetected(String text) {

        // start activity
        Intent intent = new Intent(MainActivity.this, TextProcessingActivity.class);
        intent.putExtra(MainActivity.EXTRA_DETECTED_TEXT, text);
        startActivity(intent);

    }
}
