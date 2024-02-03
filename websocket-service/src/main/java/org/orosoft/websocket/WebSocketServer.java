package org.orosoft.websocket;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import org.orosoft.actor.UserActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class WebSocketServer extends AbstractVerticle {


    @Autowired
    HazelcastInstance hazelcastClient;
    public static final String DELETE_URL = "http://localhost:8081/user/logout";
    Map<String, Long> lastPingTimeOfUsers = new HashMap<>();
    RestTemplate restTemplate;
    ActorRef userActor;

    @Override
    public void start() throws Exception {
        System.out.println("start function was called");
        this.serverStart(getVertx());
    }

    private void serverStart(Vertx vertx) {

        System.out.println("serverStart function was called");

        /*
         * Creating websocket server here using sub-protocol.
         * The addWebSocketSubProtocol method is used to specify a WebSocket sub-protocol that the server is willing to speak.
         * After that Server Initialization takes place.
         * */
        HttpServerOptions options = new HttpServerOptions().addWebSocketSubProtocol("OSC-WebSocket-Protocol");
        HttpServer httpServer = vertx.createHttpServer(options);

        validateHeaderAndHandleWebsocketRequest(vertx, httpServer);

        /*
         * Starting the websocket server on http://localhost/8888.
         * The listen method takes a callback function (Handler<AsyncResult<HttpServer>>) as an argument.
         * This callback is executed when the server starts (or fails to start).
         * */
        httpServer.listen(8888, "localhost", (result) -> {
            String message = (result.succeeded()) ? "Success" : "Failed";
            System.out.println("Connection Status: " + message);
        });
    }

    private void validateHeaderAndHandleWebsocketRequest(Vertx vertx, HttpServer httpServer) {

        httpServer.webSocketHandler((serverWebSocket) -> {
            System.out.println("Inside websocket Handler event");

            /*Passing the key to fetch the concated string values from header*/
            String header = serverWebSocket.headers().get("Sec-WebSocket-Protocol");
            String[] headerValues = header.split(",");

            String webSocketProtocol = headerValues[0];
            String userId = headerValues[1];
            String sessionId = headerValues[2];
            String deviceType = headerValues[3];

            /*If any of the header values found to be absent or invalid, webSocket connection request is rejected*/
            if (webSocketProtocol.isEmpty() || webSocketProtocol.isBlank() || userId.isEmpty() || userId.isBlank() ||
                    sessionId.isEmpty() || sessionId.isBlank() || deviceType.isEmpty() || deviceType.isBlank())
            {
                serverWebSocket.close();
                vertx.close();
            } else
            {
                System.out.println("Header validated");

                //checking last heartbeat's time every 5 second.
                keepsRunningEveryFiveSeconds(vertx, serverWebSocket, userId, sessionId);

                //Creating the Akka actor using the header
                createAkkaActorForTheUser(header, userId);

                //implementing websocket handlers
                handleWebSocketMessage(serverWebSocket, userId);
            }
        });
    }

    private void keepsRunningEveryFiveSeconds(Vertx vertx, ServerWebSocket serverWebSocket, String userId, String sessionId) {
        vertx.setPeriodic(5000, (__) -> {
            isWithinTimeFrame(serverWebSocket, userId, sessionId);
        });
    }

    private void createAkkaActorForTheUser(String header, String userId) {
        System.out.println("AKKA Actor for this user is been created");

        ActorSystem system = ActorSystem.create("actorPool");

        userActor = system.actorOf(UserActor.getProps(), userId.trim());

        userActor.tell(header, ActorRef.noSender());

        System.out.println("Thread of the user: " + userActor.toString());
    }

    private void isWithinTimeFrame(ServerWebSocket serverWebSocket, String userId, String sessionId) {

        long currentTime = System.currentTimeMillis();
        long lastPingReceivedAt = lastPingTimeOfUsers.getOrDefault(userId, currentTime);

        restTemplate = new RestTemplate();

//        System.out.println("Function called at: "+ (currentTime / 1000));

        long gap = (currentTime - lastPingReceivedAt) / 1000;
//        System.out.println("Gap between current ping and last ping is: " + gap);

        /*
        * Close the websocket connection.
        * */
        if (gap >= 30 && gap < 120) {
//            System.out.println("Last heartbeat received " + gap + " seconds ago");
            serverWebSocket.close();
        }

        /*
        * forcefully log out user, clear cache, kill user thread, close websocket.
        * */
        if (gap >= 120) {
//            System.out.println("Last heartbeat received " + gap + " seconds ago");

            //Logging out user with the specified user-id
            logoutCurrentUser(userId, sessionId);
            System.out.println("Inside 120 sec block: " + userId + " " + sessionId);

            //remove current user from map
            lastPingTimeOfUsers.remove(userId);

            //clearing cache/local storage
            hazelcastClient.getMap("headerCache").remove(userId);

            //killing actor forcefully
            if (userActor != null) userActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }
    }

    private void logoutCurrentUser(String userId, String sessionId) {
        Map<String, String> logoutObject = new HashMap<>();
        logoutObject.put("userId", userId);
        logoutObject.put("sessionId", sessionId);

        /*
        * Using rest template to log out the user, However this is not logging out the user properly, as the status and time both are not getting updated.
        * */
        ResponseEntity<Map> mapResponseEntity = restTemplate.postForEntity(DELETE_URL, logoutObject, Map.class);
        Map<String, Integer> body = mapResponseEntity.getBody();

        if(body.get("code").toString().equals("200")){
            System.out.println("Websocket is closed and user has been logged out");
        }
    }

    private void handleWebSocketMessage(ServerWebSocket serverWebSocket, String userId) {

        serverWebSocket.textMessageHandler((message) -> {
            lastPingTimeOfUsers.put(userId, System.currentTimeMillis());
            System.out.println("Map Key and Values: " + lastPingTimeOfUsers.toString());

            //Echo back the message
            serverWebSocket.writeTextMessage(message);
        });

        serverWebSocket.closeHandler((__) -> {
            System.out.println("closed connection");
            serverWebSocket.close();
        });
    }
}
