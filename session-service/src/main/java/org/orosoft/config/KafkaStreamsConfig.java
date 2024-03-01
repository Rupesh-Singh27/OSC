package org.orosoft.config;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.orosoft.serdes.LoginLogoutSerdes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

import static org.apache.kafka.streams.StreamsConfig.*;

@Configuration
public class KafkaStreamsConfig {

    @Value("${kafka.streams.application-id}")
    private String applicationIdConfig;
    @Value("${kafka.streams.bootstrap-server}")
    private String bootstrapServer;

    @Bean
    public Properties kafkaStreamsProperties(){

        Properties props = new Properties();

        props.put(APPLICATION_ID_CONFIG, applicationIdConfig);
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, LoginLogoutSerdes.class.getName());

        return props;
    }

    @Bean
    public StreamsBuilder kafkaStreamsBuilder(){
        return new StreamsBuilder();
    }
}
    /*
    Doing this will lead to:
    Invalid topology: Topology has no stream threads and no global threads, must subscribe to at least one source topic or global table.

    Because Topology is a blueprint of Kafka Streams Application right from Source Node to Processor Node to Sink node and here we are not defining those building blocks.

    Basically we are just Declaring Topology and not defining it.
    @Bean
    public KafkaStreams kafkaStreams(Properties kafkaStreamsProperties, StreamsBuilder kafkaStreamsBuilder){
        return new KafkaStreams(kafkaStreamsBuilder.build(), kafkaStreamsProperties);
    }*/
