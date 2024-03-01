package org.orosoft.userservice.config;


import org.apache.kafka.clients.producer.KafkaProducer;
import org.orosoft.userservice.dto.LoginLogoutDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KafkaProducerConfig {

    @Value("${kafka.producer.bootstrap-server}")
    private String bootstrapServer;

    @Value("${kafka.producer.key-serializer}")
    private String keySerializer;

    @Value("${kafka.producer.value-serializer}")
    private String valueSerializer;

    @Bean
    public KafkaProducer<String, LoginLogoutDTO> kafkaProducer(){
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServer);
        props.put("key.serializer", keySerializer);
        props.put("value.serializer", valueSerializer);

        return new KafkaProducer<>(props);
    }

    /*@Bean
    public Properties kafkaProducerProperties(){

        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServer);
        props.put("key.serializer", keySerializer);
        props.put("value.serializer", valueSerializer);

        return props;
    }*/
}