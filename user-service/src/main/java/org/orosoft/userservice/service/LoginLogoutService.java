package org.orosoft.userservice.service;

import org.orosoft.userservice.response.ApiResponse;

public interface LoginLogoutService {

    ApiResponse loginUser(String userId, String password, String loginDevice);

    ApiResponse logoutUser(String userId, String sessionId);
}
