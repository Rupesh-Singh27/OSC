package org.orosoft.userservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.orosoft.userservice.dto.UserIdAndOTPDto;
import org.orosoft.userservice.dto.UserIdAndPasswordDto;
import org.orosoft.userservice.entity.User;
import org.orosoft.userservice.response.CustomJsonResponse;
import org.orosoft.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody User user){

        String message = userService.registerUser(user);

        if(message.equalsIgnoreCase("User is already registered")){
            return ResponseEntity.status(HttpStatus.IM_USED)
                    .body(new CustomJsonResponse(30, null));
        }else if(message.equalsIgnoreCase("mail not sent")){
            return new ResponseEntity<>(new CustomJsonResponse(220, null), HttpStatus.INTERNAL_SERVER_ERROR);
        } else{
            Map<String,String> jsonObject = new HashMap<>();
            jsonObject.put("userId", message);

            return ResponseEntity.status(HttpStatus.OK).body(new CustomJsonResponse(200, jsonObject));
        }
    }
    @PostMapping("/validateotp")
    public ResponseEntity<?> validateOtp(@RequestBody UserIdAndOTPDto userIdAndOTPDto){

        System.out.println(userIdAndOTPDto.getUserId() + " " + userIdAndOTPDto.getOtp());

        String message = userService.validateOtp(userIdAndOTPDto.getUserId(), userIdAndOTPDto.getOtp());
        System.out.println("Otp validation response: " + message);

        Map<String,Integer> responseMap = new HashMap<>();

        if(message.equalsIgnoreCase("Invalid user id")){
            responseMap.put("code", 1999);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseMap);
        }else if (message.equalsIgnoreCase("Invalid OTP")){
            responseMap.put("code", 502);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseMap);
        }else if (message.equalsIgnoreCase("Tries exceeded")){
            responseMap.put("code", 301);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseMap);
        } else if (message.equalsIgnoreCase("Exception Occurred")) {
            throw new RuntimeException();
        } else{
            responseMap.put("code", 500);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseMap);
        }
    }

    @PostMapping("/addUserDetails")
    public ResponseEntity<?> addUserDetails(@RequestBody UserIdAndPasswordDto userIdAndPasswordDto){
        String message = userService.addUserInDB(userIdAndPasswordDto.getUserId(), userIdAndPasswordDto.getPassword());

        Map<String,Integer> responseMap = new HashMap<>();

        if(message.equalsIgnoreCase("user added")){
            responseMap.put("code", 200);
            return new ResponseEntity<>(responseMap, HttpStatus.OK);
        }else{
            responseMap.put("code", 0);
            return new ResponseEntity<>(responseMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> loginObject, HttpServletRequest request){

        String userId = loginObject.get("userId");
        String password = loginObject.get("password");
        String loginDevice = loginObject.get("loginDevice");

        LOGGER.info("Calling UserService's loginUser method");
        String message = userService.loginUser(userId, password, loginDevice, request.getSession());

        Map<String,Integer> responseMap = new HashMap<>();

        if(message.equalsIgnoreCase("User id invalid")){
            return new ResponseEntity<>(new CustomJsonResponse(201, null), HttpStatus.OK);
        }
        else if(message.equalsIgnoreCase("Incorrect password")){
            return new ResponseEntity<>(new CustomJsonResponse(202, null), HttpStatus.OK);
        }
        else if(message.equalsIgnoreCase("Already logged in")){
            return new ResponseEntity<>(new CustomJsonResponse(204, null), HttpStatus.OK);
        }
        else if(message.equalsIgnoreCase("Tries exceeded")){
            return new ResponseEntity<>(new CustomJsonResponse(205, null), HttpStatus.OK);
        }else{
            String[] sessionIdAndUserName = message.split("_");

            Map<String,String> jsonObject = new HashMap<>();
            jsonObject.put("sessionId", sessionIdAndUserName[0]);
            jsonObject.put("name", sessionIdAndUserName[1]);

            return ResponseEntity.status(HttpStatus.OK).body(new CustomJsonResponse(200, jsonObject));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestBody Map<String, String> logoutObject, HttpServletRequest request){
        String userId = logoutObject.get("userId");
        String sessionId = logoutObject.get("sessionId");

        String message = userService.logoutUser(userId, sessionId, request.getSession());

        Map<String,Integer> responseMap = new HashMap<>();

        if(message.equalsIgnoreCase("Exception occurred")) {
            responseMap.put("code", 0);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseMap);
        }else{
            responseMap.put("code", 200);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseMap);
        }
    }

    @PostMapping("/forgotPassword")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> emailMap){

        String email = emailMap.get("email");

        LOGGER.info("In forgot password: " + email);

        String message = userService.validateEmail(email);

        Map<String,Integer> responseMap = new HashMap<>();

        if(message.equalsIgnoreCase("Valid Email")) {
            responseMap.put("code", 200);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseMap);
        }else{
            responseMap.put("code", 199);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseMap);
        }
    }

    @PostMapping("/validateOTPForForgotPassword")
    public ResponseEntity<?> validateOTPForForgotPassword(@RequestBody Map<String, String> emailAndOTP){

        String email = emailAndOTP.get("email");
        int otp = Integer.parseInt(emailAndOTP.get("OTP"));

        String message = userService.validateOtpForForgotPassword(email, otp);

        Map<String,Integer> responseMap = new HashMap<>();
        if(message.equalsIgnoreCase("OTP matched")){
            responseMap.put("code", 200);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseMap);
        }else{
            responseMap.put("code", 199);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseMap);
        }
    }

    @PostMapping("/changePassword")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> emailAndNewPassword){
        String email = emailAndNewPassword.get("email");
        String newPassword = emailAndNewPassword.get("password");

        String message = userService.updatePassword(email, newPassword);

        Map<String,Integer> responseMap = new HashMap<>();
        if(message.equalsIgnoreCase("Password updated")){
            responseMap.put("code", 200);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseMap);
        }else{
            responseMap.put("code", 199);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseMap);
        }
    }
}
