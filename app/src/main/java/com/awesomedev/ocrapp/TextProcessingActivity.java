package com.awesomedev.ocrapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;

public class TextProcessingActivity extends AppCompatActivity {


    private EditText etDisplayText = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_processing);

        etDisplayText = (EditText) findViewById(R.id.et_display_text);


        Intent intent= getIntent();
        String detectedText = intent.getStringExtra(MainActivity.EXTRA_DETECTED_TEXT);

        // display the text
        etDisplayText.setText(detectedText);
    }
}
