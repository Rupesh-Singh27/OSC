package org.orosoft.config;


import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {
    @Bean
    public HazelcastInstance hazelcastClient(){
        return HazelcastClient.newHazelcastClient();
    }

    /*@Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        config.getSerializationConfig().addSerializerConfig(
                new SerializerConfig()
                        .setTypeClass(ActorRef.class)
                        .setClass(ActorRefSerializer.class)
        );
        return Hazelcast.newHazelcastInstance(config);
    }*/
}
