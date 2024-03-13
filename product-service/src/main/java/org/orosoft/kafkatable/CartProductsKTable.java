package org.orosoft.kafkatable;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.orosoft.common.AppConstants;
import org.orosoft.entity.CartProduct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class CartProductsKTable {

    private final KafkaStreams kafkaStreamsForCartProducts;
    private ReadOnlyKeyValueStore<String, Map<String, CartProduct>> cartProductStore;

    public CartProductsKTable(
            @Qualifier("kafkaStreamsForCartProducts") KafkaStreams kafkaStreamsForCartProducts
    ) {
        this.kafkaStreamsForCartProducts = kafkaStreamsForCartProducts;
    }

    @PostConstruct
    public void startStreams() {
        /*Starting the Kafka Streams first initializes the necessary components, including state stores.*/
        kafkaStreamsForCartProducts.start();

        cartProductStore = kafkaStreamsForCartProducts.store(StoreQueryParameters
                .fromNameAndType(AppConstants.CART_PRODUCTS_STORE, QueryableStoreTypes.keyValueStore()));
    }

    public List<CartProduct> getCartProductList(String userId) {
        log.info("CartProduct Store Data for {} are {}", userId, cartProductStore.get(userId));
        return cartProductStore.get(userId).values().stream().toList();
    }

    public Map<String, CartProduct> getMapOfCartProducts(String userId){
        return cartProductStore.get(userId);
    }

    public CartProduct getCartProducts(String userId, String productId){
        return cartProductStore.get(userId).get(productId);
    }

    @PreDestroy
    public void closeProducer(){
        kafkaStreamsForCartProducts.close();
    }
}
