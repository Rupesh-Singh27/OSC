package org.orosoft.userservice.exception;

public class CustomException extends RuntimeException{

    public CustomException() {
    }

    public CustomException(String message) {
        super(message);
    }
}
