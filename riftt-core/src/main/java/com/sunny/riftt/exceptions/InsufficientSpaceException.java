package com.sunny.riftt.exceptions;

public class InsufficientSpaceException extends Exception{
    public InsufficientSpaceException(String message){
        super("Insufficient Space: " + message);
    }
}
