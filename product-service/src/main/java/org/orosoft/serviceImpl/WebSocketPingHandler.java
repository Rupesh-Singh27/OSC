package org.orosoft.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.orosoft.common.AppConstants;
import org.orosoft.request.MTPingRequest;
import org.orosoft.response.ApiResponse;
import org.orosoft.response.ExistingUserDataObjectResponse;
import org.springframework.stereotype.Component;

@Component
public class WebSocketPingHandler {

    private final ResponseGeneratorServiceForExistingUser recentlyViewResponseForExistingUser;
    private final ProductDetailsService productDetailsService;
    private final CartOperationService cartOperationService;
    private final FilterService filterService;
    private final ObjectMapper objectMapper;

    public WebSocketPingHandler(
            ResponseGeneratorServiceForExistingUser recentlyViewResponseForExistingUser,
            ProductDetailsService productDetailsService,
            CartOperationService cartOperationService,
            FilterService filterService,
            ObjectMapper objectMapper
    ){
        this.recentlyViewResponseForExistingUser = recentlyViewResponseForExistingUser;
        this.productDetailsService = productDetailsService;
        this.cartOperationService = cartOperationService;
        this.filterService = filterService;
        this.objectMapper = objectMapper;
    }

    /*Based on actions performed by user pings are received, hence perform operations based on different pings*/
    public String prepareProductResponseForPings(MTPingRequest mtPingRequest, String userId) {

        String productResponseForPings = "";

        switch (mtPingRequest.getMtPing()){
            case AppConstants.ACTION_GET_PRODUCT_DETAILS:

                productResponseForPings = productDetailsService.prepareProductDetailsWithSimilarProducts(mtPingRequest.getCategoryId().charAt(0), mtPingRequest.getProductId(), userId);
                break;
            case AppConstants.ACTION_FILTER_PRODUCTS:

                productResponseForPings = filterService.getFilteredProducts(mtPingRequest.getMtPing(), mtPingRequest.getCategoryId().charAt(0), mtPingRequest.getFilter());
                break;
            case AppConstants.ACTION_GET_CART_PRODUCTS:

                productResponseForPings = cartOperationService.getCartProductsResponse(mtPingRequest.getMtPing(), userId);
                break;
            case AppConstants.ACTION_INCREASE_PRODUCTS_QUANTITY:

                cartOperationService.increaseProductQuantity(userId, mtPingRequest.getProductId());
                break;
            case AppConstants.ACTION_DECREASE_PRODUCTS_QUANTITY:

                cartOperationService.decreaseProductQuantity(userId, mtPingRequest.getProductId());
                break;
            case AppConstants.ACTION_DELETE_PRODUCT_FROM_CART:

                cartOperationService.removeProductFromCart(userId, mtPingRequest.getProductId());
                break;
            case AppConstants.ACTION_REFRESH_APPLICATION:
                try {
                    ExistingUserDataObjectResponse existingUserDataObjectResponse = recentlyViewResponseForExistingUser.buildResponseForExistingUser(userId);
                    productResponseForPings = objectMapper.writeValueAsString(ApiResponse.builder().code(200).responseBuilder(existingUserDataObjectResponse).build());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Refresh Response: " + productResponseForPings);
                break;
        }
        return productResponseForPings;
    }
}
