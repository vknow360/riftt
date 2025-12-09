package com.sunny.riftt.utils;

import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;

public class URLValidator {

    public static boolean isValidURL(String url) {
        try {
            URI.create(url);
        }catch(Exception e){
            return false;
        }
        return true;
    }

    public static boolean isReachable(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            int responseCode = connection.getResponseCode();
            return (responseCode >= 200 && responseCode < 400);
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean supportsResume(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            String acceptRanges = connection.getHeaderField("Accept-Ranges");
            return acceptRanges != null && !acceptRanges.equals("none");
        } catch (IOException e) {
            return false;
        }
    }
}
