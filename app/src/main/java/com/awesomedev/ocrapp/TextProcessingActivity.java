package com.awesomedev.ocrapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextProcessingActivity extends AppCompatActivity {


    private static final String TAG = TextProcessingActivity.class.getSimpleName();
    private EditText etDisplayName=null, etDisplayPincode = null , etDisplayAddress = null , etDisplayPhone= null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_processing);

        etDisplayName = findViewById(R.id.et_display_name);
        etDisplayAddress = findViewById(R.id.et_display_address);
        etDisplayPhone = findViewById(R.id.et_display_phone);
        etDisplayPincode = findViewById(R.id.et_display_pincode);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        String detectedText = intent.getStringExtra(MainActivity.EXTRA_DETECTED_TEXT);

        String[] lines = detectedText.split("\\r?\\n");
        // process text
        processText(detectedText);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    private String[] cleanText(String text) {
        // remove all misc chars

        String lines[] = text.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].trim();
            lines[i] = lines[i].replace("\n", "");
            if (lines[i].isEmpty())
                lines[i] = null;
        }

        // remove duplicates
        for (int i=0 ; i < lines.length ; i++){
            for (int j= i+1; j < lines.length ; j++){
                if (lines[i] != null && lines[j] != null){
                    if (lines[i].equalsIgnoreCase(lines[j]))
                        lines[j] = null;
                }
            }
        }

        return lines;
    }

    private String matchRegexArray(String[] lines, String[] regexArray) {
        String result = null;
        for (String regex : regexArray) {
            Pattern pattern = Pattern.compile(regex);
            for (int i = 0; i < lines.length; i++) {
                if (lines[i] != null) {
                    Matcher matcher = pattern.matcher(lines[i]);
                    if (matcher.find()) {
                        result = lines[i];
                        lines[i] = null;
                        return result;
                    }
                }
            }
        }
        return result;
    }


    private String getFirstLine(String[] lines , String text){
        String result = null;

        for (int i=0 ; i < lines.length ; i++)
            if (lines[i]!=null) {
                result = lines[i];
                lines[i] = null;
                break;
            }
        return result;
    }


    private String matchRegexArrayWithFirstLine(String lines[] , String regexArray[]){
        String result = null;
        for (String regex : regexArray) {
            Pattern pattern = Pattern.compile(regex);

            for (int i=0 ; i<lines.length ; i++){
                if (lines[i]!=null){
                    Matcher matcher = pattern.matcher(lines[i]);
                    if (matcher.find()){
                        result = lines[i];
                        lines[i] = null;
                        return result;
                    }
                    else
                        return result;
                }
            }
        }

        return result;
    }

    private String extractName(String[] lines, String text) {
        // Heuristic name matching

        String result = null;
        // match explicit annotations
        for (String regex : REGEX.EXPLICIT_MENTIONS) {
            Pattern pattern = Pattern.compile(regex);
            for (int i = 0; i < lines.length; i++) {
                if (lines[i] != null) {
                    Matcher matcher = pattern.matcher(lines[i]);
                    if (matcher.find()) {
                        Log.d(TAG, "extractName: found regex : " + regex);
                        int end = matcher.end();
                        // if there are more characters after the word
                        if (lines[i].length() - end > 2) {
                            Log.d(TAG, "extractName: more chars found");
                            lines[i] = lines[i].substring(end + 1, lines[i].length());
                            result = lines[i];
                            lines[i] = null;
                            return result;
                        } else {
                            lines[i] = null;
                            while(lines[++i] == null);
                            result = lines[i];
                            lines[i] = null;
                            return result;
                        }
                    }
                }
            }
        }


        // match optional parameters
        result = matchRegexArrayWithFirstLine(lines, REGEX.OPTIONAL_EXPLICIT_ANNOTATIONS);
        if (result != null)
            return result;

        result = matchRegexArrayWithFirstLine(lines, REGEX.OPTIONAL_COMPANY_SUFFIX);
        if (result != null)
            return result;

        result = getFirstLine(lines,text);
        return result;
    }

    private String extractPincode(String[] lines, String text) {
        // match a 5-6 digits of consecutive digits
        Pattern pattern = Pattern.compile(REGEX.PINCODE_REGEX);

        String result = null;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i] != null) {
                Matcher matcher = pattern.matcher(lines[i]);
                if (matcher.find()) {
                    result = matcher.group(0);
                    break;
                }
            }
        }
        return result;
    }

    private String extractPhone(String[] lines, String text) {
        Pattern pattern = Pattern.compile(REGEX.PHONE_NUMBER_REGEX);
        String result = null;

        for (int i = 0; i < lines.length; i++) {
            if (lines[i] != null) {
                Matcher matcher = pattern.matcher(lines[i]);
                if (matcher.find()) {
                    result = matcher.group(0);
                    lines[i] = null;
                }
            }
        }
        return result;
    }

    private String extractAddress(String lines[], String text) {
        String address = "";
        for (int i = 0; i < lines.length; i++) {
            if (lines[i] != null)
                address = address + lines[i] + "\n";
        }

        return address;
    }

    public void processText(String text) {
        // first remove all misc. chars and replace them with spaces
        String[] lines = cleanText(text);

        for (String line : lines) {
            if (line != null)
                Log.d(TAG, "processText: " + line);
        }
        // extract name
        String nameLine = extractName(lines, text);
        Log.d(TAG, "name: " + nameLine);

        String pinCode = extractPincode(lines, text);
        Log.d(TAG, "pincode : " + pinCode);

        String phoneNumber = extractPhone(lines, text);
        Log.d(TAG, "phoneNumber : " + phoneNumber);

        String address = extractAddress(lines, text);
        Log.d(TAG, "address: " + address);

        etDisplayName.setText(nameLine);
        etDisplayPincode.setText(pinCode);
        etDisplayAddress.setText(address);
        etDisplayPhone.setText(phoneNumber);
    }
}
