package com.kinnara.kecakplugins.interimparticipant;

public class RestApiException extends Exception {
    private int responseCode;

    public RestApiException(int responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
