package com.awesomedev.ocrapp;

/**
 * Created by sparsh on 12/22/17.
 */

public class REGEX {

    static final String MISC_CHAR_REGEX = "[^a-zA-Z\\d\\s]";
    static final String PINCODE_REGEX = "\\d{5,6}\\b";
    static final String PHONE_NUMBER_REGEX = "\\d{7,}";

    static final String EXPLICIT_MENTIONS[] = {
            "(?i)\\bto\\b",
            "(?i)\\bconsignee\\b",
            "(?i)\\bname\\b",
            "(?i)\\baddress\\b"
    };

    static final String OPTIONAL_EXPLICIT_ANNOTATIONS[] = {
            "(?i)\\bmr\\b",
            "(?i)\\bmrs\\b",
            "(?i)\\bms\\b",
            "(?i)\\bdr\\b",
            "(?i)\\bprof\\b",
            "(?i)\\bmba\\b",
            "(?i)\\bjr\\b",
            "(?i)\\bsr\\b",
    };

    static final String OPTIONAL_COMPANY_SUFFIX[] = {
            "(?i)\\bpvt\\b[\\s]+\\bltd\\b",
            "(?i)\\bltd\\b",
            "(?i)\\bprivate\\b[\\s]+limited\\b",
            "(?i)\\blimited\\b",
            "(?i)\\binc\\b",
            "(?i)\\bcorp\\b",
            "(?i)\\bcorporation\\b",
            "(?i)\\bincorporated\\b"
    };

}
