package org.orosoft.config;

import io.vertx.core.Vertx;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VertexConfig {
    @Bean
    public Vertx vertx() {
        return Vertx.vertx();
    }
}
