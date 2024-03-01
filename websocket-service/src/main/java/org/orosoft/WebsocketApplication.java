package org.orosoft;

import io.vertx.core.Vertx;
import org.orosoft.websocket.WebSocketServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.event.EventListener;

/**
 * Hello world!
 *
 */
@SpringBootApplication
@EnableFeignClients
@ImportAutoConfiguration({FeignAutoConfiguration.class})
public class WebsocketApplication
{
    private final WebSocketServer webSocketServer;
    private final Vertx vertx;

    WebsocketApplication(WebSocketServer webSocketServer, Vertx vertx){
        this.webSocketServer = webSocketServer;
        this.vertx = vertx;
    }

    public static void main( String[] args ) {
        SpringApplication.run(WebsocketApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void deployVerticle(){
        vertx.deployVerticle(webSocketServer);
    }
}
