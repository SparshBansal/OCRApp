package com.awesomedev.ocrapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import java.nio.channels.AsynchronousServerSocketChannel;

/**
 * Created by sparsh on 12/26/17.
 */

public class OCRTask extends AsyncTask<Bitmap , Void, String> {


    private ProgressDialog progressDialog;
    private Context context;
    private Tesseract tesseract;

    private static final String TAG = "OCRTASK";

    public interface AsyncResponse{
        void onLinesDetected(String text);
    }

    public AsyncResponse delegate=null;

    public OCRTask(Context context, Tesseract tesseract , AsyncResponse delegate){
        this.context = context;
        this.tesseract = tesseract;
        this.delegate = delegate;

        progressDialog = new ProgressDialog(context);
    }

    @Override
    protected void onPreExecute() {
        progressDialog.setMessage("Detecting Text");
        progressDialog.show();
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(Bitmap... bitmaps) {
        Bitmap bitmap = bitmaps[0];
        String result = tesseract.detectLines(bitmap);
        return result;
    }

    @Override
    protected void onPostExecute(String s) {
        this.progressDialog.hide();
        this.delegate.onLinesDetected(s);
        Log.d(TAG, "onPostExecute: " + s);
        super.onPostExecute(s);
    }
}
