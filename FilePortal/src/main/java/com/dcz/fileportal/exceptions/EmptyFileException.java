package com.dcz.fileportal.exceptions;

public class EmptyFileException extends Exception{
    public EmptyFileException() {
        super("The file to upload is empty.");
    }
}
