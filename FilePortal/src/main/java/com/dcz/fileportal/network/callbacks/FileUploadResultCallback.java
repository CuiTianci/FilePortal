package com.dcz.fileportal.network.callbacks;

public interface FileUploadResultCallback {

    /**
     * Succeed to upload the file.
     * @param url The url of the file.
     * @param isCache Weather is the file uploaded just now or cached by the backend server.
     */
    void onSuccess(String url, boolean isCache);

    void onFailure(Exception e);
}
