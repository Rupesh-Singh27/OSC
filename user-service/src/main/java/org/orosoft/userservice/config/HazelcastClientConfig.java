package org.orosoft.userservice.config;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastClientConfig {

    @Bean
    public HazelcastInstance getHazelcastClientBean(){
        return HazelcastClient.newHazelcastClient();
    }
}
