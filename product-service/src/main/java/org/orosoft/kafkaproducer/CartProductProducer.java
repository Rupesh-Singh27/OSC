package org.orosoft.kafkaproducer;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.orosoft.common.AppConstants;
import org.orosoft.entity.CartProduct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CartProductProducer {

    private final KafkaProducer<String, Map<String, CartProduct>> kafkaProducerForCartProducts;

    public CartProductProducer(
            @Qualifier("kafkaProducerForCartProducts")KafkaProducer<String, Map<String, CartProduct>> kafkaProducerForCartProducts
    ){
        this.kafkaProducerForCartProducts = kafkaProducerForCartProducts;
    }

    public void produceCartProductsInKafkaTopic(String userId, Map<String, CartProduct> cartProductsMap) {
        ProducerRecord<String, Map<String, CartProduct>> record = new ProducerRecord<>(AppConstants.CART_PRODUCT_TOPIC_NAME, userId, cartProductsMap);
        kafkaProducerForCartProducts.send(record);
    }

    @PreDestroy
    public void closeProducer(){
        kafkaProducerForCartProducts.close();
    }
}
