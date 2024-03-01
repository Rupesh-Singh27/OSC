package org.orosoft.dto;


import akka.actor.ActorRef;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * UserConnectionInfo represents the information associated with a user's connection.
 * It includes the WebSocket instance, ActorRef, ping message, and the timestamp of the last heartbeat.
 * <p>
 * Fields:
 * - webSocket: ServerWebSocket instance representing the user's WebSocket connection.
 * - actorRef: ActorRef representing the associated Akka actor reference.
 * - ping: String representing the ping message for checking the connection.
 * - lastHeartbeatTime: long representing the timestamp of the last heartbeat received.
 * <p>
 * The Builder pattern is available for convenient object creation with method chaining.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConnectionInfo {

    private ActorRef actorRef;
    private String ping;
    private String userId;
    private long lastHeartbeatTime;

}
