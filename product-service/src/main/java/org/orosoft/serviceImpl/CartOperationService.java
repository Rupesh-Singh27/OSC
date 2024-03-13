package org.orosoft.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.orosoft.dto.ProductDto;
import org.orosoft.entity.CartProduct;
import org.orosoft.exception.CustomException;
import org.orosoft.hazelcastmap.ProductLookUpMapOperations;
import org.orosoft.kafkaproducer.CartProductProducer;
import org.orosoft.kafkatable.CartProductsKTable;
import org.orosoft.response.CartDataForPing;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CartOperationService {
    private final ProductLookUpMapOperations productLookUpMapOperations;
    private final ProductServiceDaoHandler productServiceDaoHandler;
    private final CartProductProducer cartProductProducer;
    private final CartProductsKTable cartProductsKTable;
    private final ObjectMapper objectMapper;

    CartOperationService(
            ProductLookUpMapOperations productLookUpMapOperations,
            ProductServiceDaoHandler productServiceDaoHandler,
            CartProductProducer cartProductProducer,
            CartProductsKTable cartProductsKTable,
            ObjectMapper objectMapper
    ){
        this.productLookUpMapOperations = productLookUpMapOperations;
        this.productServiceDaoHandler = productServiceDaoHandler;
        this.cartProductProducer = cartProductProducer;
        this.cartProductsKTable = cartProductsKTable;
        this.objectMapper = objectMapper;
    }

    public String getCartProductsResponse(String ping, String userId) {
        try {

            if(ping == null) throw new CustomException("Ping is null");
            if(userId == null) throw new CustomException("User ID is null");

            /*Fetching the cart product list from CartProduct Cache(KTable)*/
            List<CartProduct> cartProductProductList = getCartProductListFromCache(userId);

            /*Preparing the response*/
            CartDataForPing cartProductResponse = CartDataForPing.builder()
                    .mtPing(ping)
                    .cartProductProducts(cartProductProductList)
                    .productCountInCart(cartProductProductList.size())
                    .totalPrice(cartProductProductList.stream().mapToDouble(CartProduct::getProductPrice).sum())
                    .build();

            return objectMapper.writeValueAsString(cartProductResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void increaseProductQuantity(String userId, String productId) {
        if(productId == null) throw new CustomException("Product ID is null");
        if(userId == null) throw new CustomException("User ID is null");

        Map<String, CartProduct> cartProductMap = getMapOfCartProductsFromCache(userId);

        /*If new user*/
        if (cartProductMap == null) {
            cartProductMap = new HashMap<>();
        }

        /*If product present with specified ID return the product, if absent execute the lambda and create a CartProduct object will 0 quantity*/
        CartProduct cartProduct = cartProductMap.computeIfAbsent(productId, thisProductId -> {
            ProductDto productDto = getProductFromCache(thisProductId);
            return CartProduct.builder()
                    .userId(userId)
                    .productId(thisProductId)
                    .productName(productDto.getProductName())
                    .productPrice(productDto.getProductMarketPrice())
                    .productCartQuantity(0) // initialize with 0
                    .build();
        });

        // Update the quantity, this will be executed in case of new user as well as for existing user
        cartProduct.setProductCartQuantity(cartProduct.getProductCartQuantity() + 1);

        // Produce the updated cartProductMap to Kafka
        produceCartProductMap(userId, cartProductMap);
    }

    public void decreaseProductQuantity(String userId, String productId) {
        if(productId == null) throw new CustomException("Product ID is null");
        if(userId == null) throw new CustomException("User ID is null");

        Map<String, CartProduct> cartProductMap = getMapOfCartProductsFromCache(userId);

        /*If new user or no product with productId is available there is no point decrementing*/
        if(cartProductMap == null) return;
        if(cartProductMap.get(productId) == null) return;

        CartProduct cartProduct = cartProductMap.get(productId);
        cartProduct.setProductCartQuantity(cartProduct.getProductCartQuantity() - 1);

        produceCartProductMap(userId, cartProductMap);
    }

    public void removeProductFromCart(String userId, String productId) {
        if(productId == null) throw new CustomException("Product ID is null");
        if(userId == null) throw new CustomException("User ID is null");

        Map<String, CartProduct> cartMap = getMapOfCartProductsFromCache(userId);

        /*If new user or no product with productId is available there is no point deleting*/
        if(cartMap == null) return;
        if(cartMap.get(productId) == null) return;

        cartMap.remove(productId);
        productServiceDaoHandler.deleteCartProductFromDatabase(productId);

        produceCartProductMap(userId, cartMap);
    }

    /*Updating the CartProduct DB Table at logout*/
    public void updateCartProductsDatabaseTable(String userId) {
        if(userId == null) throw new CustomException("Null values found");

        Map<String, CartProduct> cartProductMap = getMapOfCartProductsFromCache(userId);
        cartProductMap.values().forEach(System.out::println);
        cartProductMap.values().forEach(productServiceDaoHandler::saveCartProductInDatabase);
    }

    /*Getting from CartProduct Kafka Table cache*/
    private List<CartProduct> getCartProductListFromCache(String userId) {
        return cartProductsKTable.getCartProductList(userId);
    }
    private Map<String, CartProduct> getMapOfCartProductsFromCache(String userId) {
        return cartProductsKTable.getMapOfCartProducts(userId);
    }

    /*Getting from Hazelcast cache*/
    private ProductDto getProductFromCache(String productId) {
        return productLookUpMapOperations.getProductFromHazelcastMap(productId);
    }

    /*Producing in Topic*/
    private void produceCartProductMap(String userId, Map<String, CartProduct> cartProductMap) {
        cartProductProducer.produceCartProductsInKafkaTopic(userId, cartProductMap);
    }
}
