package org.orosoft.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.orosoft.dto.ProductDto;
import org.orosoft.entity.Cart;
import org.orosoft.repository.CartRepository;
import org.orosoft.response.CartDataForPing;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CartOperationService {
    private final KafkaStreams kafkaStreamsForCartProducts;
    private final KafkaProducer<String, Map<String, Cart>> kafkaProducerForCartProducts;
    private final HazelcastInstance hazelcastInstance;
    private final CartRepository cartRepository;
    private final ObjectMapper objectMapper;
    private IMap<String, ProductDto> productLookUpMap;
    private ReadOnlyKeyValueStore<String, Map<String, Cart>> cartProductStore;
    CartOperationService(
            HazelcastInstance hazelcastInstance,
            CartRepository cartRepository,
            ObjectMapper objectMapper,
            @Qualifier("kafkaStreamsForCartProducts") KafkaStreams kafkaStreamsForCartProducts,
            @Qualifier("kafkaProducerForCartProducts") KafkaProducer<String, Map<String, Cart>> kafkaProducerForCartProducts
    ){
        this.hazelcastInstance = hazelcastInstance;
        this.kafkaStreamsForCartProducts = kafkaStreamsForCartProducts;
        this.kafkaProducerForCartProducts = kafkaProducerForCartProducts;
        this.cartRepository = cartRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void startStreamsAndInitializeStateStores(){
        /*Starting the Kafka Streams first initializes the necessary components, including state stores.*/
        kafkaStreamsForCartProducts.start();

        cartProductStore = kafkaStreamsForCartProducts.store(StoreQueryParameters
                .fromNameAndType("cart-products-store", QueryableStoreTypes.keyValueStore()));

        productLookUpMap = hazelcastInstance.getMap("productMap");
    }
    public String getCartProductsResponse(String ping, String userId) {

        log.info("Cart Store Data for {} are {}", userId, cartProductStore.get(userId));

        /*Fetching the cart product list from Cart Cache(KTable)*/
        List<Cart> cartProductList = cartProductStore
                .get(userId)
                .values()
                .stream()
                .toList();

        /*Preparing the response*/
        CartDataForPing cartProductResponse = CartDataForPing.builder()
                .mtPing(ping)
                .cartProducts(cartProductList)
                .productCountInCart(cartProductList.size())
                .totalPrice(cartProductList.stream().mapToDouble(Cart::getProductPrice).sum())
                .build();

        try {
            return objectMapper.writeValueAsString(cartProductResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void increaseProductQuantity(String userId, String productId) {
        Map<String, Cart> cartMap = cartProductStore.get(userId);

        /*If new user*/
        if (cartMap == null) {
            cartMap = new HashMap<>();
        }

        /*If product present with specified ID return the product, if absent execute the lambda and create a CartProduct object will 0 quantity*/
        Cart cartProduct = cartMap.computeIfAbsent(productId, thisProductId -> {
            ProductDto productDto = productLookUpMap.get(thisProductId);
            return Cart.builder()
                    .userId(userId)
                    .productId(thisProductId)
                    .productName(productDto.getProductName())
                    .productPrice(productDto.getProductMarketPrice())
                    .productCartQuantity(0) // initialize with 0
                    .build();
        });

        // Update the quantity, this will be executed in case of new user as well as for existing user
        cartProduct.setProductCartQuantity(cartProduct.getProductCartQuantity() + 1);

        // Produce the updated cartMap to Kafka
        ProducerRecord<String, Map<String, Cart>> record = new ProducerRecord<>("CartProductsTopic", userId, cartMap);
        kafkaProducerForCartProducts.send(record);
    }

    public void decreaseProductQuantity(String userId, String productId) {
        /*If new user or no product with productId is available there is no point decrementing*/
        if(cartProductStore.get(userId) == null) return;
        if(cartProductStore.get(userId).get(productId) == null) return;

        Map<String, Cart> cartMap = cartProductStore.get(userId);

        Cart cartProduct = cartMap.get(productId);
        cartProduct.setProductCartQuantity(cartProduct.getProductCartQuantity() - 1);

        ProducerRecord<String, Map<String, Cart>> record = new ProducerRecord<>("CartProductsTopic", userId, cartMap);
        kafkaProducerForCartProducts.send(record);
    }

    public void removeProductFromCart(String userId, String productId) {

        /*If new user or no product with productId is available there is no point deleting*/
        if(cartProductStore.get(userId) == null) return;
        if(cartProductStore.get(userId).get(productId) == null) return;

        Map<String, Cart> cartMap = cartProductStore.get(userId);

        cartMap.remove(productId);
        cartRepository.deleteByProductId(productId);

        ProducerRecord<String, Map<String, Cart>> record = new ProducerRecord<>("CartProductsTopic", userId, cartMap);
        kafkaProducerForCartProducts.send(record);
    }

    /*Updating the Cart DB Table at logout*/
    public void updateCartProductsDatabaseTable(String userId) {
        Map<String, Cart> cartProductMap = cartProductStore.get(userId);
        cartProductMap.values().forEach(System.out::println);
        cartProductMap.values().forEach(cartRepository::save);
    }

    @PreDestroy
    public void closeProducer(){
        kafkaStreamsForCartProducts.close();
    }
}
