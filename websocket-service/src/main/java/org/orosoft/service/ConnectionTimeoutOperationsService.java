package org.orosoft.service;

import akka.actor.ActorRef;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.orosoft.client.grpc.LoginServiceClient;
import org.orosoft.client.grpc.LogoutServiceClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConnectionTimeoutOperationsService {

    private final LoginServiceClient loginServiceClient;
    private final LogoutServiceClient logoutServiceClient;
    private final ActorOperationsService actorOperationsService;


    public ConnectionTimeoutOperationsService(
            LoginServiceClient loginServiceClient,
            LogoutServiceClient logoutServiceClient,
            ActorOperationsService actorOperationsService
    ) {
        this.loginServiceClient = loginServiceClient;
        this.logoutServiceClient = logoutServiceClient;
        this.actorOperationsService = actorOperationsService;
    }


    public void closeWebsocketAndKillThread(ActorRef userThread, ServerWebSocket webSocket) {
        log.info("Inside 30 Seconds");

        /*Close websocket connection and kill the akka thread*/
        stopThreadGracefully(userThread);
        webSocket.close();

        log.info("Is websocket closed {}", webSocket.isClosed());
        log.info("Is thread terminated {}", userThread.isTerminated());
    }

    private void stopThreadGracefully(ActorRef userThread) {
        actorOperationsService.killUserActorThread(userThread);
    }

    public void logoutUserIfLoggedIn(String customWebsocketId, String sessionId) {
        log.info("Inside 120 Seconds");

        String userId = customWebsocketId.split("-")[0];
        String deviceType = customWebsocketId.split("-")[1];

        /*Check in user is already logged out*/
        boolean loginStatus = loginServiceClient.getLoginStatus(userId, deviceType);

        if (!loginStatus) {
            log.info("Login status of user with {} is {}", userId, false);
            return;
        }

        /*Log out user*/
        boolean isLoggedOut = logoutServiceClient.logoutUser(userId, sessionId, deviceType);
        if (isLoggedOut) {
            log.info("Websocket is closed and user has been logged out");
        }
    }
}
