package com.dcz.fileportal.exceptions;

public class NoTokenException extends Exception {
    public NoTokenException() {
        super("Failed to get any token.");
    }
}
