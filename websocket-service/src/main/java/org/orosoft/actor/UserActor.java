package org.orosoft.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.orosoft.client.grpc.ProductServiceClient;
import org.orosoft.common.AppConstants;
import org.orosoft.dto.UserConnectionInfo;


@Slf4j
public class UserActor extends AbstractActor {

    private final ProductServiceClient productServiceClient;
    private final ServerWebSocket webSocket;
    private final ObjectMapper objectMapper;

    public static Props getProps(ServerWebSocket webSocket, ProductServiceClient productServiceClient, ObjectMapper objectMapper){
        return Props.create(UserActor.class, () -> new UserActor(webSocket, productServiceClient, objectMapper));
    }

    UserActor(ServerWebSocket webSocket, ProductServiceClient productServiceClient, ObjectMapper objectMapper){
        this.webSocket = webSocket;
        this.productServiceClient = productServiceClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(UserConnectionInfo.class, this::processUserPings)
                .matchAny((Object object) -> log.error("Unexpected message type"))
                .build();
    }

    private void processUserPings(UserConnectionInfo userConnectionInfo) {
        try {
            String jsonString = userConnectionInfo.getPing();
            String userId = userConnectionInfo.getUserId();

            String ping = objectMapper.readTree(jsonString).get(AppConstants.MESSAGE_TYPE).asText();

            getResponseForPing(ping, jsonString, userId);
        } catch (JsonProcessingException exception) {
            log.error("Exception while parsing the Message Type {}", exception.getMessage());
            throw new RuntimeException(exception);
        }
    }

    private void getResponseForPing(String ping, String jsonString, String userId) {
        switch (ping){
            case AppConstants.ACTION_ACKNOWLEDGE_PING:
                sendKeepAliveResponse(jsonString);
                break;
            case AppConstants.ACTION_GET_PRODUCT_DETAILS:
                openSelectedProduct(jsonString, userId);
                break;
            case AppConstants.ACTION_FILTER_PRODUCTS:
                filterProducts(jsonString, userId);
                break;
            case AppConstants.ACTION_GET_CART_PRODUCTS:
                getCartData(jsonString, userId);
                break;
            case AppConstants.ACTION_INCREASE_PRODUCTS_QUANTITY:
                increaseProductQuantityInCart(jsonString, userId);
                break;
            case AppConstants.ACTION_DECREASE_PRODUCTS_QUANTITY:
                decreaseProductQuantityInCart(jsonString, userId);
                break;
            case AppConstants.ACTION_DELETE_PRODUCT_FROM_CART:
                removeProductFromCart(jsonString, userId);
                break;
            case AppConstants.ACTION_REFRESH_APPLICATION:
                refreshApplication(jsonString, userId);
                break;
            default:
        }
    }

    private void sendKeepAliveResponse(String ping) {
        webSocket.writeTextMessage(ping);
    }

    private void openSelectedProduct(String jsonString, String userId) {
        String productResponse = productServiceClient.getProductResponse(jsonString, userId);
        webSocket.writeTextMessage(productResponse);
    }

    private void filterProducts(String jsonString, String userId) {
        String filteredProducts = productServiceClient.getProductResponse(jsonString, userId);
        webSocket.writeTextMessage(filteredProducts);
    }

    private void getCartData(String jsonString, String userId) {
        String cartData = productServiceClient.getProductResponse(jsonString, userId);
        webSocket.writeTextMessage(cartData);
    }

    private void increaseProductQuantityInCart(String jsonString, String userId) {
        productServiceClient.getProductResponse(jsonString, userId);
    }

    private void decreaseProductQuantityInCart(String jsonString, String userId) {
        productServiceClient.getProductResponse(jsonString, userId);
    }

    private void removeProductFromCart(String jsonString, String userId) {
        productServiceClient.getProductResponse(jsonString, userId);
    }
    private void refreshApplication(String jsonString, String userId) {
        String productResponse = productServiceClient.getProductResponse(jsonString, userId);
        webSocket.writeTextMessage(productResponse);
    }
}
