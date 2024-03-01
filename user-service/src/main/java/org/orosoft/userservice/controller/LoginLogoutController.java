package org.orosoft.userservice.controller;

import org.orosoft.userservice.dto.loginLogoutDto.LoginRequest;
import org.orosoft.userservice.dto.loginLogoutDto.LogoutRequest;
import org.orosoft.userservice.response.ApiResponse;
import org.orosoft.userservice.service.LoginLogoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "http://localhost:3000")
public class LoginLogoutController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginLogoutController.class);

    private final LoginLogoutService loginLogoutService;

    LoginLogoutController(LoginLogoutService loginLogoutService){
        this.loginLogoutService = loginLogoutService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> loginUser(@RequestBody LoginRequest loginRequest){

        LOGGER.info("Calling UserService's loginUser method");
        ApiResponse loginResponse = loginLogoutService.loginUser(loginRequest.getUserId(), loginRequest.getPassword(), loginRequest.getLoginDevice());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(loginResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logoutUser(@RequestBody LogoutRequest logoutRequest){

        ApiResponse logoutResponse = loginLogoutService.logoutUser(logoutRequest.getUserId(), logoutRequest.getSessionId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(logoutResponse);
    }
}
