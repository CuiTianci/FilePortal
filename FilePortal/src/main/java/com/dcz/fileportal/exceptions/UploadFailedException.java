package com.dcz.fileportal.exceptions;

public class UploadFailedException extends Exception {
    public UploadFailedException() {
        super("Tried but failed to upload the file.");
    }
}
