package org.orosoft.userservice.service;

import jakarta.servlet.http.HttpSession;
import org.orosoft.userservice.entity.User;

public interface UserService {
    String registerUser(User user);

    String validateOtp(String userId, int otp);

    String addUserInDB(String userId, String password);

    String loginUser(String userId, String password, String loginDevice, HttpSession session);

    String logoutUser(String userId, String sessionId, HttpSession session);

    String validateEmail(String email);

    String validateOtpForForgotPassword(String email, int otp);

    String updatePassword(String email, String newPassword);
}
