package com.dcz.fileportal.exceptions;

public class InvalidTimeException extends Exception {
    public InvalidTimeException() {
        super("The provided string-typed time cannot be converted to a valid timestamp.");
    }
}
