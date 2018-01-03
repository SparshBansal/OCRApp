package com.awesomedev.ocrapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OCRTask.AsyncResponse,ImageProcessingTask.OnImageProcessedListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_TAKE_PHOTO = 98;
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
                launchCropActivity();
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
                        contourFile = FileUtils.createTextFile(MainActivity.this);
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

    // helper method to launch crop activity
    private void launchCropActivity() {
        CropImage.activity().setGuidelines(CropImageView.Guidelines.ON);
        CropImage.activity().start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {

            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE: {
                if (resultCode == RESULT_OK) {
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
                break;
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
        // Kick of the Async Task
        ImageProcessingTask ipTask = new ImageProcessingTask(MainActivity.this);
        ipTask.setOnImageProcessedListener(this);
        ipTask.execute(image);
    }

    @Override
    public void onLinesDetected(String text) {

        // start activity
        Intent intent = new Intent(MainActivity.this, TextProcessingActivity.class);
        intent.putExtra(MainActivity.EXTRA_DETECTED_TEXT, text);
        startActivity(intent);

    }

    @Override
    public void onImageProcessed(Mat cleanedImage) {

        try {
            contours = CVUtils.findContours(cleanedImage);
            logContourPoints(contours);

            Mat cvContourImage = new Mat();
            cleanedImage.copyTo(cvContourImage);

            Mat cvContourMap = CVUtils.drawContours(cvContourImage, contours);

            // convert mat to bitmap
            // cvErodedImage = CVUtils.erode(cvDilatedImage, 3);
            Bitmap result = CVUtils.getBitmapFromMat(cleanedImage);

            // save bitmap
            FileUtils.saveBitmap(result , MainActivity.this);

            Log.d(TAG, "processImage: bitmap config : " + result.getConfig().toString());

            Bitmap resultARGB = result.copy(Bitmap.Config.ARGB_8888, true);

            ivProcessed.setImageBitmap(resultARGB);
            this.processedImage = resultARGB;

            this.bProcessText.setVisibility(View.VISIBLE);
            this.bPlotContours.setVisibility(View.VISIBLE);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
