package org.orosoft.userservice.service;

import org.orosoft.userservice.entity.User;
import org.orosoft.userservice.response.ApiResponse;

public interface SignUpService {
    ApiResponse registerUser(User user);

    ApiResponse validateOtp(String userId, long otp);

    ApiResponse prepareObjectToSaveInDB(String userId, String password);
}
