package org.orosoft.config;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.orosoft.entity.Cart;
import org.orosoft.entity.RecentView;
import org.orosoft.serdes.ListSerDes;
import org.orosoft.serdes.MapSerDe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.apache.kafka.streams.StreamsConfig.*;

@Configuration
public class KafkaStreamsConfig {

    @Value("${kafka.streams.application-id}")
    private String applicationIdConfig;
    @Value("${kafka.streams.bootstrap-server}")
    private String bootstrapServer;

    @Bean
    public Properties kafkaStreamsPropertiesForRecentView(){

        Properties props = new Properties();

        props.put(APPLICATION_ID_CONFIG, applicationIdConfig);
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, ListSerDes.class.getName());

        return props;
    }

    @Bean
    public Properties kafkaStreamsPropertiesForCartProducts(){

        Properties props = new Properties();

        props.put(APPLICATION_ID_CONFIG, "cart-products");
        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, MapSerDe.class.getName());

        return props;
    }

    /*@Bean
    public StreamsBuilder kafkaStreamsBuilder(){
        return new StreamsBuilder();
    }*/

    @Bean
    public KafkaStreams kafkaStreamsForRecentView(Properties kafkaStreamsPropertiesForRecentView){
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        KTable<String, List<RecentView>> kTable = streamsBuilder.table("RecentViewProductsTopic", Materialized.as("recent-view-products-store"));
        Topology topology = streamsBuilder.build();

        return new KafkaStreams(topology, kafkaStreamsPropertiesForRecentView);
    }

    /*@Bean
    public KafkaStreams kafkaStreamsForCartProducts(Properties kafkaStreamsPropertiesForCartProducts, StreamsBuilder kafkaStreamsBuilder){
        //TODO: Map<ProductId, Cart> should be here
        KTable<String, List<RecentView>> kTable = kafkaStreamsBuilder.table("CartProductsTopic", Materialized.as("cart-products-store"));
        Topology topology = kafkaStreamsBuilder.build();

        return new KafkaStreams(topology, kafkaStreamsPropertiesForCartProducts);
    }*/

//    , StreamsBuilder kafkaStreamsBuilder
    @Bean
    public KafkaStreams kafkaStreamsForCartProducts(Properties kafkaStreamsPropertiesForCartProducts){
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        KTable<String, Map<String, Cart>> kTable = streamsBuilder.table("CartProductsTopic", Materialized.as("cart-products-store"));
        Topology topology = streamsBuilder.build();

        return new KafkaStreams(topology, kafkaStreamsPropertiesForCartProducts);
    }
}
