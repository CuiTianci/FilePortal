package com.dcz.fileportal;

class Constants {
    //Response code
    static final int RET_SUCCESS = 200;
    //API DOC:http://api.cocomobi.com:4999/web/#/39?page_id=305
    //token
    static final String API_TOKEN = "https://fs.cocomobi.com/api/v1/token";//get token
    static final String NET_KEY_UID = "uid";
    static final String NET_KEY_SIGNATURE = "signature";
    static final String NET_KEY_PKG = "pkg";
    //pre upload
    static final String API_PRE_UPLOAD = "https://fs.cocomobi.com/api/v1/preUpload";
    static final String NET_KEY_MD5 = "md5";
    static final String NET_KEY_ACCESS = "access";
    static final String NET_KEY_TOKEN = "token";
    static final String NET_KEY_MODE = "mode";
    //upload
    static final String API_UPLOAD = "https://fs.cocomobi.com/api/v1/upload";
    static final String NET_KEY_FILE = "file";
    static final String NET_KEY_MD_UPPER = "MD";
    //Shared preference
    static final String SHARED_PREFS_NAME = "file_portal_prefs";
    static final String PREFS_TOKEN_JSON = "prefs_token_json";
}
