package org.orosoft.userservice.controller;


import org.orosoft.userservice.dto.signupDto.UserIdAndOTPRequest;
import org.orosoft.userservice.dto.signupDto.UserIdAndPasswordRequest;
import org.orosoft.userservice.entity.User;
import org.orosoft.userservice.response.ApiResponse;
import org.orosoft.userservice.service.SignUpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "http://localhost:3000")
public class SignUpController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignUpController.class);

    private final SignUpService signUpService;

    SignUpController(SignUpService signUpService){
        this.signUpService = signUpService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> registerUser(@RequestBody User userRequest){

        ApiResponse registerResponse = signUpService.registerUser(userRequest);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(registerResponse);
    }
    @PostMapping("/validateotp")
    public ResponseEntity<ApiResponse> validateOtp(@RequestBody UserIdAndOTPRequest userIdAndOTPRequest){

        System.out.println("Userid and OTP to verify: " + userIdAndOTPRequest.getUserId() + " " + userIdAndOTPRequest.getOtp());

        ApiResponse validateOtpResponse =
                signUpService.validateOtp(userIdAndOTPRequest.getUserId(), userIdAndOTPRequest.getOtp());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(validateOtpResponse);
    }

    @PostMapping("/addUserDetails")
    public ResponseEntity<ApiResponse> addUserDetails(@RequestBody UserIdAndPasswordRequest userIdAndPasswordRequest){

        ApiResponse addUserResponse = signUpService.prepareObjectToSaveInDB(userIdAndPasswordRequest.getUserId(), userIdAndPasswordRequest.getPassword());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(addUserResponse);
    }
}
