package org.orosoft.userservice.controller;

import org.orosoft.userservice.dto.forgotPasswordDto.EmailAndOTPRequest;
import org.orosoft.userservice.dto.forgotPasswordDto.EmailAndPasswordRequest;
import org.orosoft.userservice.dto.forgotPasswordDto.EmailRequest;
import org.orosoft.userservice.response.ApiResponse;
import org.orosoft.userservice.service.ForgotPasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "http://localhost:3000")
public class ForgotPasswordController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgotPasswordController.class);

    private final ForgotPasswordService forgotPasswordService;

    ForgotPasswordController(ForgotPasswordService forgotPasswordService){
        this.forgotPasswordService = forgotPasswordService;
    }
    @PostMapping("/forgotPassword")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestBody EmailRequest emailRequest){

        ApiResponse validateEmailResponse = forgotPasswordService.validateEmail(emailRequest.getEmail());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(validateEmailResponse);

    }

    @PostMapping("/validateOTPForForgotPassword")
    public ResponseEntity<ApiResponse> validateOTPForForgotPassword(@RequestBody EmailAndOTPRequest emailAndOTPRequest){

        ApiResponse validateOtpResponse = forgotPasswordService.validateOtpForForgotPassword(emailAndOTPRequest.getEmail(), emailAndOTPRequest.getOtp());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(validateOtpResponse);

    }

    @PostMapping("/changePassword")
    public ResponseEntity<ApiResponse> changePassword(@RequestBody EmailAndPasswordRequest emailAndNewPassword){

        ApiResponse updatePasswordResponse = forgotPasswordService.updatePassword(emailAndNewPassword.getEmail(), emailAndNewPassword.getPassword());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(updatePasswordResponse);

    }
}
