package org.orosoft.config;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.orosoft.entity.CartProduct;
import org.orosoft.entity.RecentView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
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
    public KafkaProducer<String, List<RecentView>> kafkaProducerForRecentView(){
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServer);
        props.put("key.serializer", keySerializer);
        props.put("value.serializer", valueSerializer);

        return new KafkaProducer<>(props);
    }

    @Bean
    public KafkaProducer<String, Map<String, CartProduct>> kafkaProducerForCartProducts(){
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.orosoft.serdes.MapSerializer");

        return new KafkaProducer<>(props);
    }
}
