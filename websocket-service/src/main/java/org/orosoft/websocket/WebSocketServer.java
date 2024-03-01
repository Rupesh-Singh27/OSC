package org.orosoft.websocket;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.orosoft.actor.UserActor;
import org.orosoft.client.grpc.LoginServiceClient;
import org.orosoft.client.grpc.LogoutServiceClient;
import org.orosoft.client.grpc.ProductServiceClient;
import org.orosoft.common.AppConstants;
import org.orosoft.dto.UserConnectionInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.Map;

@Slf4j
@Component
public class WebSocketServer extends AbstractVerticle {

    @Value("${websocket.hostname}")
    private String hostname;
    @Value("${websocket.protocol}")
    private String websocketProtocol;
    @Value("${websocket.request.header}")
    private String websocketRequestHeader;
    @Value("${logout.url}")
    private String logoutUrl;

    private final LoginServiceClient loginServiceClient;
    private final ProductServiceClient productServiceClient;
    private final LogoutServiceClient logoutServiceClient;
    private final Map<String, UserConnectionInfo> userConnectionMap;
    private final HazelcastInstance hazelcastInstance;
    private final ObjectMapper objectMapper;
    private final ActorSystem actorSystem;
    private IMap<String, Long> taskIdMap;

    WebSocketServer(
            LoginServiceClient loginServiceClient,
            LogoutServiceClient logoutServiceClient,
            ProductServiceClient productServiceClient,
            Map<String, UserConnectionInfo> userConnectionMap,
            HazelcastInstance hazelcastInstance,
            ObjectMapper objectMapper
    ){
        this.loginServiceClient = loginServiceClient;
        this.logoutServiceClient = logoutServiceClient;
        this.productServiceClient = productServiceClient;
        this.userConnectionMap = userConnectionMap;
        this.actorSystem = ActorSystem.create();
        this.hazelcastInstance = hazelcastInstance;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initHazelcastMap(){
        taskIdMap = hazelcastInstance.getMap(AppConstants.TASK_ID_MAP);
    }

    @Override
    public void start() {
        this.serverStart(vertx);
    }

    private void serverStart(Vertx vertx) {

        HttpServerOptions options = new HttpServerOptions().addWebSocketSubProtocol(websocketProtocol);
        HttpServer server = vertx.createHttpServer(options);

        server.webSocketHandler(this::handleWebSocket).listen(8888, hostname, result -> {
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

        String header = headers.get(websocketRequestHeader);
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
            createUserThread(customWebsocketId, webSocket);

            /*Once connection is established, ping-pong message starts*/
            acknowledgePing(webSocket, customWebsocketId);

            /*Periodic Heart Beat Check, executed every 5 seconds*/
            periodicHeartbeatCheck(customWebsocketId, sessionId, webSocket);

            /*This block gets executed when user logs out*/
            closeWebSocketConnection(webSocket);
        }
    }

    private void periodicHeartbeatCheck(String customWebsocketId, String sessionId, ServerWebSocket webSocket) {
        closeAlreadyAssignedTask(customWebsocketId);

        long periodicTaskId = vertx.setPeriodic(5000, (__) -> {
            log.info("5 seconds block executed");
            checkHeartBeatOfTheClient(customWebsocketId, sessionId, webSocket);
        });
        log.info("Task Id is {} and UserId is {}", periodicTaskId, customWebsocketId);
        taskIdMap.put(customWebsocketId, periodicTaskId);
    }

    private void closeAlreadyAssignedTask(String customWebsocketId){
        if(taskIdMap.get(customWebsocketId) != null){
            vertx.cancelTimer(taskIdMap.get(customWebsocketId));
        }
    }

    private void isValidConnection(ServerWebSocket webSocket, String[] headerValues) {
        log.info("Connection request received");

        if(headerValues.length == 4){
            String webSocketProtocol = headerValues[0].trim();
            String userId = headerValues[1].trim();
            String sessionId = headerValues[2].trim();
            String deviceType = headerValues[3].trim();

            /*If any of the header values found to be absent or invalid, webSocket connection request is rejected*/
            if (!webSocketProtocol.isEmpty() && !webSocketProtocol.isBlank() &&
                    !userId.isEmpty() && !userId.isBlank() &&
                    !sessionId.isEmpty() && !sessionId.isBlank() &&
                    !deviceType.isEmpty() && !deviceType.isBlank())
            {
                webSocket.accept();
                log.info("Websocket connection made for the user {}", userId);
                return;
            }
        }
        webSocket.reject(403);
        log.error("Invalid Header");
    }

    private void createUserThread(String customWebsocketId, ServerWebSocket webSocket) {
        /*Create a AKKA Actor LWT*/
        ActorRef userThread = actorSystem.actorOf(UserActor.getProps(webSocket, productServiceClient, objectMapper));

        UserConnectionInfo userConnectionInfo = UserConnectionInfo.builder()
                .userId(customWebsocketId.split("-")[0])
                .actorRef(userThread)
                .build();

        log.info("Inside Create Thread {}", userThread.toString());

        userConnectionMap.put(customWebsocketId, userConnectionInfo);
    }

    private void acknowledgePing(ServerWebSocket webSocket, String customWebsocketId) {
        webSocket.textMessageHandler(ping -> {
            /*Store Heart Beat time and Ping type in map*/
            storePingAndLastHeartBeatTime(customWebsocketId, ping);

            /*User's akka actor is responsible for acknowledging keep alive message*/
            UserConnectionInfo userConnectionInfo = userConnectionMap.get(customWebsocketId);
            ActorRef userActor = userConnectionInfo.getActorRef();

            userActor.tell(userConnectionInfo, ActorRef.noSender());
        });
    }

    private void storePingAndLastHeartBeatTime(String customWebsocketId, String ping) {
        log.info("Ping received for {}.", customWebsocketId);

        UserConnectionInfo connectionInfo = userConnectionMap.get(customWebsocketId);
        connectionInfo.setPing(ping);
        connectionInfo.setLastHeartbeatTime(System.currentTimeMillis());
    }

    private void checkHeartBeatOfTheClient(String customWebsocketId, String sessionId, ServerWebSocket webSocket) {
        UserConnectionInfo userConnectionInfo = userConnectionMap.get(customWebsocketId);

        ActorRef userThread = userConnectionInfo.getActorRef();
        long lastHeartBeat = userConnectionInfo.getLastHeartbeatTime();

        long currentTime = System.currentTimeMillis();

        int difference = (int) ((currentTime - lastHeartBeat) / 1000);
        log.info("difference {}", difference);

        if (difference > 30 && difference <= 35) {
            closeWebsocketAndKillThread(userThread, webSocket);
        }

        if (difference > 120 && difference <= 125) {
            /*Clearing user's data*/
            userConnectionMap.remove(customWebsocketId);
            closeAlreadyAssignedTask(customWebsocketId);
            logoutUserIfLoggedIn(customWebsocketId, sessionId);
        }
    }

    private void closeWebsocketAndKillThread(ActorRef userThread, ServerWebSocket webSocket) {
        log.info("Inside 30 Seconds");

        /*Close websocket connection and kill the akka thread*/
        try {
            Await.result(Patterns.gracefulStop(userThread, Duration.create(5, AppConstants.TIME_UNIT)), Duration.Inf());
        } catch (Exception exception) {
            log.error(exception.getMessage());
        }
        webSocket.close();

        log.info("Is websocket closed {}", webSocket.isClosed());
        log.info("Is thread terminated {}", userThread.isTerminated());
    }

    private void logoutUserIfLoggedIn(String customWebsocketId, String sessionId) {
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

    private void closeWebSocketConnection(ServerWebSocket webSocket) {
        webSocket.closeHandler((__)->{
            log.info("Socket Closed");
        });
    }
}
