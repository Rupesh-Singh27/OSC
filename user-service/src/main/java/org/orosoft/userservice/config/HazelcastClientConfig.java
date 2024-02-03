package org.orosoft.userservice.config;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class HazelcastClientConfig {

    @Bean
    @Scope("singleton")
    public HazelcastInstance getHazelcastClientBean(){
        return HazelcastClient.newHazelcastClient();
    }
}
