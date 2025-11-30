package com.sunny.exceptions;

public class DatabaseException extends Exception{
    public DatabaseException(String message){
        super("Database Error: " + message);
    }
}
