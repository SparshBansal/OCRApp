package com.awesomedev.ocrapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements OCRTask.AsyncResponse {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_TAKE_PHOTO = 1;
    public static final String EXTRA_DETECTED_TEXT = "DETECTED TEXT";


    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "static initializer: failure");
        } else {
            Log.d(TAG, "static initializer: success");
        }
    }

    private ImageView ivOrg = null, ivProcessed = null;
    private Button bProcessText = null;

    private String mCurrentPhotoPath;
    private Tesseract tesseract;

    private Bitmap processedImage = null;

    private Uri photoUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivOrg = (ImageView) findViewById(R.id.iv_original);
        ivProcessed = (ImageView) findViewById(R.id.iv_processed);
        bProcessText = (Button) findViewById(R.id.b_process_text);

        tesseract = new Tesseract(MainActivity.this);
        tesseract.setupTesseract();

        bProcessText.setVisibility(View.INVISIBLE);

        ivOrg.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // launch the camera application
                    try {
                        File photoFile = createImageFile();
                        if (photoFile != null) {
                            photoUri = FileProvider.getUriForFile(MainActivity.this, "com.example.android.fileprovider", photoFile);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
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
                OCRTask ocrTask = new OCRTask(MainActivity.this , tesseract , MainActivity.this);
                ocrTask.execute(processedImage);
                return;
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

    // helper method to launch crop activity
    private void launchCropActivity(Uri imageUri) {
        CropImage.activity().setGuidelines(CropImageView.Guidelines.ON);
        CropImage.activity(imageUri).start(this);
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

    private void processImage(Bitmap image) throws IOException {
        Mat cvImage = new Mat();
        Utils.bitmapToMat(image, cvImage);

        saveBitmap(CVUtils.getBitmapFromMat(cvImage));
        // Create and apply edge mask
        Mat cvEdgeMask = CVUtils.createCannyEdgeMask(cvImage);
        Mat cvMaskedImage = CVUtils.applyMask(cvImage, cvEdgeMask);


        // Binarize Image
        Mat cvBinarizedImage = CVUtils.binarize(cvMaskedImage);

        // Erode Image for more clarity
        Mat cvDilatedImage = CVUtils.dilate(cvBinarizedImage);

        Mat cvSmoothenedImage = CVUtils.smoothen(cvDilatedImage);

        // convert mat to bitmap
        Bitmap result = CVUtils.getBitmapFromMat(cvSmoothenedImage);

        // save bitmap
        saveBitmap(result);

        Log.d(TAG, "processImage: bitmap config : " + result.getConfig().toString());

        Bitmap resultARGB = result.copy(Bitmap.Config.ARGB_8888, true);

        ivProcessed.setImageBitmap(resultARGB);
        this.processedImage = resultARGB;
        this.bProcessText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLinesDetected(String text) {

        // start activity
        Intent intent = new Intent(MainActivity.this, TextProcessingActivity.class);
        intent.putExtra(MainActivity.EXTRA_DETECTED_TEXT, text);
        startActivity(intent);

    }
}
