package org.orosoft.websocket;

import akka.actor.ActorRef;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.orosoft.client.grpc.LoginServiceClient;
import org.orosoft.common.AppConstants;
import org.orosoft.config.WebSocketConfigurationProperties;
import org.orosoft.dto.UserConnectionInfo;
import org.orosoft.hazelcastmap.TaskIdMapOperationService;
import org.orosoft.hazelcastmap.UserConnectionMapOperationService;
import org.orosoft.service.ActorOperationsService;
import org.orosoft.service.ConnectionTimeoutOperationsService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebSocketServer extends AbstractVerticle {

/*
    @Value("${websocket.hostname}")
    private String hostname;
    @Value("${websocket.protocol}")
    private String websocketProtocol;
    @Value("${websocket.request.header}")
    private String websocketRequestHeader;
    @Value("${logout.url}")
    private String logoutUrl;*/
    private final LoginServiceClient loginServiceClient;
    private final ConnectionTimeoutOperationsService connectionTimeoutOperationsService;
    private final TaskIdMapOperationService taskIdMapOperationService;
    private final UserConnectionMapOperationService userConnectionMapOperationService;
    private final ActorOperationsService actorOperationsService;
    private final WebSocketConfigurationProperties webSocketConfigurationProperties;


    WebSocketServer(
            LoginServiceClient loginServiceClient,
            ConnectionTimeoutOperationsService connectionTimeoutOperationsService,
            TaskIdMapOperationService taskIdMapOperationService,
            ActorOperationsService actorOperationsService,
            WebSocketConfigurationProperties webSocketConfigurationProperties,
            UserConnectionMapOperationService userConnectionMapOperationService
    ){
        this.loginServiceClient = loginServiceClient;
        this.connectionTimeoutOperationsService = connectionTimeoutOperationsService;
        this.taskIdMapOperationService = taskIdMapOperationService;
        this.actorOperationsService = actorOperationsService;
        this.webSocketConfigurationProperties = webSocketConfigurationProperties;
        this.userConnectionMapOperationService = userConnectionMapOperationService;
    }

    @Override
    public void start() {
        this.serverStart(vertx);
    }

    private void serverStart(Vertx vertx) {

        HttpServerOptions options = new HttpServerOptions().addWebSocketSubProtocol(webSocketConfigurationProperties.getProtocol());
        HttpServer server = vertx.createHttpServer(options);

        server.webSocketHandler(this::handleWebSocket).listen(AppConstants.PORT, webSocketConfigurationProperties.getHostname(), result -> {
            if(result.succeeded()){
                log.info("WebSocket Server started on port 8888");
            }else {
                log.error("Error starting WebSocket Server: " + result.cause());
            }
        });
    }

    private void handleWebSocket(ServerWebSocket webSocket) {
        log.info("Request in handleWebSocket");
        MultiMap headers = webSocket.headers();

        String header = headers.get(webSocketConfigurationProperties.getRequestHeader());
        String[] headerValues = header.split(",");

        /*Validating connection based on Header*/
        isValidConnection(webSocket, headerValues);

        String userId = headerValues[1].trim();
        String sessionId = headerValues[2].trim();
        String deviceType = headerValues[3].trim();
        String customWebsocketId = userId+"-"+deviceType;

        boolean loginStatus = loginServiceClient.getLoginStatus(userId, deviceType);
        if(loginStatus) {
            /*Once connection is made Actor Thread will be created*/
            ActorRef userThread = createUserThread(webSocket);
            storeUserIdAndUserThreadInLocalMap(customWebsocketId, userThread);

            /*Once connection is established, ping-pong message starts*/
            handlePing(webSocket, customWebsocketId);

            /*Periodic Heart Beat Check, executed every 5 seconds*/
            handelPeriodicHeartbeat(customWebsocketId, sessionId, webSocket);

            /*This block gets executed when user logs out*/
            closeWebSocketConnection(webSocket);
        }
    }

    private void handelPeriodicHeartbeat(String customWebsocketId, String sessionId, ServerWebSocket webSocket) {
        closeAlreadyAssignedTask(customWebsocketId);
        long periodicTaskId = doPeriodicHeartBeatCheck(customWebsocketId, sessionId, webSocket);
        insertPeriodicTaskIdInMap(customWebsocketId, periodicTaskId);
    }

    private void closeAlreadyAssignedTask(String customWebsocketId){
        long taskId = getTaskId(customWebsocketId);
        if(taskId != -1) vertx.cancelTimer(taskId);
    }

    private long doPeriodicHeartBeatCheck(String customWebsocketId, String sessionId, ServerWebSocket webSocket) {
        long periodicTaskId = vertx.setPeriodic(AppConstants.DELAY, (__) -> {
            log.info("5 seconds block executed");
            handleHeartBeatOfTheClient(customWebsocketId, sessionId, webSocket);
        });

        log.info("Task Id is {} and UserId is {}", periodicTaskId, customWebsocketId);
        return periodicTaskId;
    }

    private void isValidConnection(ServerWebSocket webSocket, String[] headerValues) {
        log.info("Connection request received");

        if(headerValues.length == 4 && headerValues[0] != null && headerValues[1] != null && headerValues[2] != null && headerValues[3] != null){

            String webSocketProtocol = headerValues[0].trim();
            String userId = headerValues[1].trim();
            String sessionId = headerValues[2].trim();
            String deviceType = headerValues[3].trim();

            /*If any of the header values found to be absent or invalid, webSocket connection request is rejected*/
            if (!webSocketProtocol.isBlank() && !userId.isBlank() && !sessionId.isBlank() && !deviceType.isBlank()) {
                webSocket.accept();
                log.info("Websocket connection made for the user {}", userId);
                return;
            }
        }
        webSocket.reject(AppConstants.STATUS);
        log.error("Invalid Header");
    }

    private void storeUserIdAndUserThreadInLocalMap(String customWebsocketId, ActorRef userThread) {
        UserConnectionInfo userConnectionInfo = UserConnectionInfo.builder()
                .userId(customWebsocketId.split("-")[0])
                .actorRef(userThread)
                .build();

        log.info("Inside Create Thread {}", userThread.toString());

        storeUserConnectionInfoInMap(customWebsocketId, userConnectionInfo);
    }

    private void handlePing(ServerWebSocket webSocket, String customWebsocketId) {
        webSocket.textMessageHandler(ping -> {
            if(ping != null){
                storePingAndLastHeartBeatTime(customWebsocketId, ping);
                acknowledgePing(customWebsocketId);
            }else{
                log.error("Inappropriate Ping");
                throw new RuntimeException("Ping is null");
            }
        });
    }

    private void storePingAndLastHeartBeatTime(String customWebsocketId, String ping) {
        /*Store Heart Beat time and Ping type in map*/
        log.info("Ping received for {}.", customWebsocketId);

        UserConnectionInfo connectionInfo = getConnectionInfoFromMap(customWebsocketId);
        connectionInfo.setPing(ping);
        connectionInfo.setLastHeartbeatTime(System.currentTimeMillis());
    }

    private void acknowledgePing(String customWebsocketId) {
        /*User's akka actor is responsible for acknowledging keep alive message*/
        UserConnectionInfo userConnectionInfo = getConnectionInfoFromMap(customWebsocketId);
        ActorRef userActor = userConnectionInfo.getActorRef();

        actorOperationsService.acknowledgePingUsingActor(userActor, userConnectionInfo);
    }

    private void handleHeartBeatOfTheClient(String customWebsocketId, String sessionId, ServerWebSocket webSocket) {
        UserConnectionInfo userConnectionInfo = getConnectionInfoFromMap(customWebsocketId);

        ActorRef userThread = userConnectionInfo.getActorRef();
        long lastHeartBeat = userConnectionInfo.getLastHeartbeatTime();

        int difference = calculateHeartBeatDifference(lastHeartBeat);
        log.info("difference {}", difference);

        if (difference > 30 && difference <= 35) {
            connectionTimeoutOperationsService.closeWebsocketAndKillThread(userThread, webSocket);
        }

        if (difference > 120 && difference <= 125) {
            /*Clearing user's data*/
            removeUserConnectionInfoFromMap(customWebsocketId);
            closeAlreadyAssignedTask(customWebsocketId);
            connectionTimeoutOperationsService.logoutUserIfLoggedIn(customWebsocketId, sessionId);
        }
    }

    private int calculateHeartBeatDifference(long lastHeartBeat){
        long currentTime = System.currentTimeMillis();
        return (int) ((currentTime - lastHeartBeat) / 1000);
    }

    private ActorRef createUserThread(ServerWebSocket webSocket) {
        return actorOperationsService.createUserThread(webSocket);
    }

    private void insertPeriodicTaskIdInMap(String customWebsocketId, long periodicTaskId) {
        taskIdMapOperationService.insertIntoMap(customWebsocketId, periodicTaskId);
    }

    private long getTaskId(String customWebsocketId) {
        return taskIdMapOperationService.getValueForKeyFromMap(customWebsocketId);
    }

    private void storeUserConnectionInfoInMap(String customWebsocketId, UserConnectionInfo userConnectionInfo) {
        userConnectionMapOperationService.insertIntoMap(customWebsocketId, userConnectionInfo);
    }

    private UserConnectionInfo getConnectionInfoFromMap(String customWebsocketId) {
        return userConnectionMapOperationService.fetchFromMap(customWebsocketId);
    }

    private void removeUserConnectionInfoFromMap(String customWebsocketId) {
        userConnectionMapOperationService.removeFromMap(customWebsocketId);
    }

    private void closeWebSocketConnection(ServerWebSocket webSocket) {
        webSocket.closeHandler((__)->{
            log.info("Socket Closed");
        });
    }
}
