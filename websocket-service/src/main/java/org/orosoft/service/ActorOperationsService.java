package org.orosoft.service;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.orosoft.actor.UserActor;
import org.orosoft.client.grpc.ProductServiceClient;
import org.orosoft.common.AppConstants;
import org.orosoft.dto.UserConnectionInfo;
import org.springframework.stereotype.Component;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

@Component
@Slf4j
public class ActorOperationsService {
    private final ProductServiceClient productServiceClient;
    private final ObjectMapper objectMapper;
    private final ActorSystem actorSystem;

    public ActorOperationsService(
            ProductServiceClient productServiceClient,
            ObjectMapper objectMapper
    ) {
        this.actorSystem = ActorSystem.create();
        this.productServiceClient = productServiceClient;
        this.objectMapper = objectMapper;
    }

    public ActorRef createUserThread(ServerWebSocket webSocket) {
        return actorSystem.actorOf(UserActor.getProps(webSocket, productServiceClient, objectMapper));
    }

    public void acknowledgePingUsingActor(ActorRef userActor, UserConnectionInfo userConnectionInfo){
        userActor.tell(userConnectionInfo, ActorRef.noSender());
    }

    public void killUserActorThread(ActorRef userThread){
        try {
            Await.result(Patterns.gracefulStop(userThread, Duration.create(5, AppConstants.TIME_UNIT)), Duration.Inf());
        } catch (Exception exception) {
            log.error(exception.getMessage());
        }
    }
}
