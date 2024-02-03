package org.orosoft.userservice.serviceImpl;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.orosoft.login.LoginRequest;
import org.orosoft.login.LoginResponse;
import org.orosoft.login.LoginServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginServiceClient.class);

    @GrpcClient("login-service")
    LoginServiceGrpc.LoginServiceBlockingStub blockingStub;

    /***
     *
     * <p>This function is responsible for getting the status weather the user is already logged in with same device or not</p>
     * @param userId userId of the user
     * @param device device which the user is using to log in to the website
     * @return if user is already logged in, returns true else return false
     * ***/
    public Boolean getLoginStatus(String userId, String device){
        LOGGER.info("Inside GRPC client for getting login status");

        LoginRequest loginRequest = LoginRequest.newBuilder()
                .setUserId(userId)
                .setDevice(device)
                .build();

        LoginResponse loginResponse = blockingStub.getLoginStatus(loginRequest);
        return loginResponse.getIsLoggedIn();
    }
}
