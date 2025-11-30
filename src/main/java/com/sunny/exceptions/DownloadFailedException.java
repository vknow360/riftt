package com.sunny.exceptions;

public class DownloadFailedException extends Exception{
    public DownloadFailedException(String message){
        super("Download Failed: " + message);
    }
}
