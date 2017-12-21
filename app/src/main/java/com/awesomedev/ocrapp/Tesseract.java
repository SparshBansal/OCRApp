package com.awesomedev.ocrapp;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by sparsh on 12/21/17.
 */


public class Tesseract {

    private Context context;
    private TessBaseAPI mTess;
    private String tessdataPath = null;
    private static final String LOG_TAG = "OCRTextExtraction";

    public Tesseract(Context context){
        this.context = context;
    }

    public boolean setupTesseract() {
        Log.d(LOG_TAG, "Tesseract setup started");
        // check if tesseract data is already copied, otherwise copy it
        tessdataPath = context.getFilesDir() + "/tesseract/";
        checkTessdata(new File(tessdataPath + "tessdata/"));

        String language = "eng";
        mTess = new TessBaseAPI();
        return mTess.init(tessdataPath , language);
    }


    public String detectLines(Bitmap image){
        String OCRresult = null;
        mTess.setImage(image);
        mTess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
        OCRresult = mTess.getUTF8Text();
        return OCRresult;
    }

    private void checkTessdata(File dir) {
        //directory does not exist, but we can successfully create it
        if (!dir.exists() && dir.mkdirs()) {
            Log.d(LOG_TAG, "Copying tessdata");
            copyTessdata();
        }
        //The directory exists, but there is no data file in it
        if (dir.exists()) {
            String datafilepath = tessdataPath + "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                Log.d(LOG_TAG, "Copying tessdata");
                copyTessdata();
            }
        }
    }

    private void copyTessdata() {
        try {
            String filepath = tessdataPath + "/tessdata/eng.traineddata";

            AssetManager assetManager = context.getAssets();

            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            //copy the file to the location specified by filepath
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

