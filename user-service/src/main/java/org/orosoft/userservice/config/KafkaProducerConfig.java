package org.orosoft.userservice.config;


import org.apache.kafka.clients.producer.KafkaProducer;
import org.orosoft.userservice.dto.LoginLogoutDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.Properties;

@Configuration
public class KafkaProducerConfig {

    @Bean
    @Scope("singleton")
    public KafkaProducer<String, LoginLogoutDTO> producer(){

        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.orosoft.userservice.serdes.LoginLogoutDTOSerializer");

        return new KafkaProducer<>(props);
    }
}
