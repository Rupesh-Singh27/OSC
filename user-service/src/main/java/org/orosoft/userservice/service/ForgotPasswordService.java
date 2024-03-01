package org.orosoft.userservice.service;

import org.orosoft.userservice.response.ApiResponse;

public interface ForgotPasswordService {

    ApiResponse validateEmail(String email);

    ApiResponse validateOtpForForgotPassword(String email, long otp);

    ApiResponse updatePassword(String email, String newPassword);

}
