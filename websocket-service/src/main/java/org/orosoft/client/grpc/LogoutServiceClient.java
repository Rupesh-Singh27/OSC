package org.orosoft.client.grpc;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.orosoft.logout.LogoutRequest;
import org.orosoft.logout.LogoutServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogoutServiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutServiceClient.class);

    @GrpcClient("logout-service-websocket")
    LogoutServiceGrpc.LogoutServiceBlockingStub blockingStub;

    public boolean logoutUser(String userId, String sessionId, String device){
        LOGGER.info("Inside GRPC client for getting logout");

        LogoutRequest logoutRequest = LogoutRequest.newBuilder()
                .setUserId(userId)
                .setDevice(device)
                .setSessionId(sessionId)
                .build();

        return blockingStub.logoutUser(logoutRequest).getIsLoggedOut();
    }
}
