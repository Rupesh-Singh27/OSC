package org.orosoft.userservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.orosoft.userservice.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> customExceptionHandler(CustomException exception, WebRequest request){
        exception.printStackTrace();
        log.error("Exception Occurred {}",exception.getMessage());
        ApiResponse customExceptionResponse = ApiResponse.builder().code(0).build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(customExceptionResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> exceptionHandler(RuntimeException exception, WebRequest request){
        exception.printStackTrace();
        log.error("Exception Occurred {}",exception.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.builder().code(0).build());
    }
}
