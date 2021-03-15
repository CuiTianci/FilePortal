package com.dcz.fileportal.options;

import android.content.Context;

import androidx.annotation.NonNull;

import com.dcz.fileportal.network.ProgressListener;
import com.dcz.fileportal.network.callbacks.FileUploadResultCallback;

import java.io.File;

public class UploadOptions {

    private final Context context;
    private final String uid;
    private final String apiKey;
    private final File file;
    private final FileUploadResultCallback fileUploadResultCallback;
    private final ProgressListener progressListener;
    private final String accessVerify;
    private final String mode;

    private UploadOptions(Builder builder) {
        this.context = builder.getContext();
        this.uid = builder.getUid();
        this.apiKey = builder.getApiKey();
        this.file = builder.getFile();
        this.fileUploadResultCallback = builder.getFileUploadResultCallback();
        this.progressListener = builder.getProgressListener();
        this.accessVerify = builder.getAccessVerify();
        this.mode = builder.getMode();
    }

    public Context getContext() {
        return context;
    }

    public String getUid() {
        return uid;
    }

    public String getApiKey() {
        return apiKey;
    }

    public File getFile() {
        return file;
    }

    public FileUploadResultCallback getFileUploadResultCallback() {
        return fileUploadResultCallback;
    }

    public ProgressListener getProgressListener() {
        return progressListener;
    }

    public String getAccessVerify() {
        return accessVerify;
    }

    public String getMode() {
        return mode;
    }


    public static class Builder {

        private final Context context;
        private final String uid;
        private final String apiKey;
        private final File file;
        private String accessVerify = AccessOption.ACCESS_VERIFY;
        private String mode = ModeOption.MODE_STAY;
        private FileUploadResultCallback fileUploadResultCallback = null;
        private ProgressListener progressListener = null;

        /**
         * @param context current context.
         * @param uid     Unique uid of the user who is to access the file.
         * @param apiKey  Registered api key for Marvel File System.
         * @param file    The file to upload.
         */
        public Builder(@NonNull final Context context, @NonNull String uid, @NonNull String apiKey, @NonNull File file) {
            this.context = context;
            this.uid = uid;
            this.apiKey = apiKey;
            this.file = file;
        }

        public Context getContext() {
            return context;
        }

        public String getUid() {
            return uid;
        }

        public String getApiKey() {
            return apiKey;
        }

        public File getFile() {
            return file;
        }

        public String getAccessVerify() {
            return accessVerify;
        }

        public Builder setAccessVerify(String accessVerify) {
            this.accessVerify = accessVerify;
            return this;
        }

        public String getMode() {
            return mode;
        }

        public Builder setMode(String mode) {
            this.mode = mode;
            return this;
        }

        public FileUploadResultCallback getFileUploadResultCallback() {
            return fileUploadResultCallback;
        }

        public Builder setFileUploadResultCallback(FileUploadResultCallback fileUploadResultCallback) {
            this.fileUploadResultCallback = fileUploadResultCallback;
            return this;
        }

        public ProgressListener getProgressListener() {
            return progressListener;
        }

        public Builder setProgressListener(ProgressListener progressListener) {
            this.progressListener = progressListener;
            return this;
        }

        public UploadOptions build() {
            return new UploadOptions(this);
        }
    }
}
