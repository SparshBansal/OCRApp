package com.awesomedev.ocrapp;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ContourPlotActivity extends AppCompatActivity {


    private ScatterChart chart=null;
    private static final String TAG = ContourPlotActivity.class.getSimpleName();
    private ArrayList<ContourRect> mList= null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contour_plot);

        chart = findViewById(R.id.chart);
        mList = new ArrayList<>();

        String contourFileName = getIntent().getStringExtra(MainActivity.EXTRA_CONTOUR_FILE_PATH);
        readContourFileAndFillList(contourFileName);
        plotChart();
    }

    private void readContourFileAndFillList(String contourFilename){
        mList.clear();
        try {
            BufferedReader br = new BufferedReader(new FileReader(contourFilename));
            String line = br.readLine();

            while(line!=null){
                String[] dimens = line.split(" ");
                double length = Double.parseDouble(dimens[0]);
                double breadth = Double.parseDouble(dimens[1]);

                mList.add(new ContourRect(length,breadth,null));

                Log.d(TAG, "plotChart: " + "Length : " + String.valueOf(length) + " breadth : " + String.valueOf(breadth));

                line = br.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void plotChart(){

        if (mList.size() == 0)
            return;

        List<Entry> entries = new ArrayList<>();

        for (ContourRect rect : mList){
            entries.add(new Entry((float)rect.length , (float)rect.breadth));
        }

        ScatterDataSet scatterDataSet = new ScatterDataSet(entries, "Contours");
        scatterDataSet.setColor(Color.BLUE);

        ScatterData scatterData = new ScatterData(scatterDataSet);
        chart.setData(scatterData);
        chart.invalidate();

    }

}
