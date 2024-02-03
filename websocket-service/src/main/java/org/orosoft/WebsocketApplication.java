package org.orosoft;

import io.vertx.core.Vertx;
import org.orosoft.websocket.WebSocketServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Hello world!
 *
 */
@SpringBootApplication
public class WebsocketApplication
{
    public static void main( String[] args ) {
        SpringApplication.run(WebsocketApplication.class, args);

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new WebSocketServer());
    }
}
