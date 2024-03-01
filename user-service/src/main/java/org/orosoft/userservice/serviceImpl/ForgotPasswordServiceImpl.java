package org.orosoft.userservice.serviceImpl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.orosoft.userservice.common.AppConstants;
import org.orosoft.userservice.common.HelperComponent;
import org.orosoft.userservice.exception.CustomException;
import org.orosoft.userservice.repository.UserRepository;
import org.orosoft.userservice.response.ApiResponse;
import org.orosoft.userservice.service.ForgotPasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgotPasswordServiceImpl.class);
    private final UserRepository userRepository;
    private final HelperComponent helperComponent;
    private final HazelcastInstance hazelcastInstance;
    ForgotPasswordServiceImpl(
            UserRepository userRepository,
            HazelcastInstance hazelcastInstance,
            HelperComponent helperComponent
    ){
        this.userRepository = userRepository;
        this.hazelcastInstance = hazelcastInstance;
        this.helperComponent = helperComponent;
    }

    @Override
    public ApiResponse validateEmail(String email) {
        boolean isUserPresent = userRepository.existsByEmail(email);
        if (isUserPresent) {
            generateAndCacheOTP(email);
            return ApiResponse.builder().code(200).build();
        } else {
            return ApiResponse.builder().code(199).build();
        }
    }

    /*Storing the OTP generated for Forgot Password into hazelcast cache in order to use it later for validating */
    private void generateAndCacheOTP(String userEmail) {
        try {
            IMap<String, Long> forgotPasswordOTPCache = hazelcastInstance.getMap(AppConstants.FORGOT_PASSWORD_OTP_CACHE);

            long otp = helperComponent.generateOTP();
            helperComponent.sendDataToKafkaForEmail(userEmail, otp, "");
            forgotPasswordOTPCache.put(userEmail, otp);

        } catch (Exception exception) {
            throw new CustomException("Email not sent");
        }
    }

    @Override
    public ApiResponse validateOtpForForgotPassword(String email, long otpToValidate) {
        IMap<String, Integer> wrongOTPAttemptsCounter = hazelcastInstance.getMap(AppConstants.WRONG_OTP_ATTEMPT_COUNTER_MAP);

        IMap<String, Long> forgotPasswordOTPCache = hazelcastInstance.getMap(AppConstants.FORGOT_PASSWORD_OTP_CACHE);
        long otpFetchedFromCache = forgotPasswordOTPCache.get(email);

        if (otpFetchedFromCache == otpToValidate) {

            /*Clearing attempts & OTP cache once OTP is consumed*/
            forgotPasswordOTPCache.remove(email);
            wrongOTPAttemptsCounter.remove(email);
            return ApiResponse.builder().code(200).build();
        } else {
            int attempts = helperComponent.attemptCounter(email, wrongOTPAttemptsCounter);

            LOGGER.info("Wrong OTP {} times", attempts);

            if (attempts == 3) {
                /*Clearing attempts & OTP cache from hazelcast once maximum attempts are exceeded.*/
                wrongOTPAttemptsCounter.remove(email);
                forgotPasswordOTPCache.remove(email);

                return ApiResponse.builder().code(301).build();
            }
            // "Invalid OTP";
            return ApiResponse.builder().code(199).build();
        }
    }

    @Transactional
    @Override
    public ApiResponse updatePassword(String email, String newPassword) {

        int passwordUpdated = userRepository.updatePasswordByEmail(newPassword, email);

        if(passwordUpdated > 0){
            return ApiResponse.builder().code(200).build();
        } else {
            return ApiResponse. builder().code(199).build();
        }
    }
}
