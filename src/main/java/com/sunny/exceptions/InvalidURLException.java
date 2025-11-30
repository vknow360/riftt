package com.sunny.exceptions;

public class InvalidURLException extends Exception{
    public InvalidURLException(String url) {
        super("Invalid URL: " + url);
    }
}
