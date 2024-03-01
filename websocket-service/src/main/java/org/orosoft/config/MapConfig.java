package org.orosoft.config;

import org.orosoft.dto.UserConnectionInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MapConfig {

    @Bean
    public Map<String, UserConnectionInfo> myHashMap() {
        return new HashMap<>();
    }
}
