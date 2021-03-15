package com.dcz.fileportal.exceptions;

public class DirectoryProvidedException extends Exception{
    public DirectoryProvidedException() {
        super("A file should be provided,but a directory instead.");
    }
}