package org.orosoft.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.orosoft.common.AppConstants;
import org.orosoft.entity.CartProduct;
import org.orosoft.entity.RecentView;
import org.orosoft.exception.CustomException;
import org.orosoft.kafkaproducer.CartProductProducer;
import org.orosoft.kafkaproducer.RecentViewProducer;
import org.orosoft.request.MTPingRequest;
import org.orosoft.response.ApiResponse;
import org.orosoft.response.ExistingUserDataObjectResponse;
import org.orosoft.response.NewUserDataObjectResponse;
import org.orosoft.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService{

    private final ResponseGeneratorServiceForExistingUser recentlyViewResponseForExistingUser;
    private final ResponseGeneratorServiceForNewUser recentlyViewResponseForNewUser;
    private final ProductServiceDaoHandler productServiceDaoHandler;
    private final ProductDetailsService productDetailsService;
    private final CartOperationService cartOperationService;
    private final CartProductProducer cartProductProducer;
    private final RecentViewProducer recentViewProducer;
    private final FilterService filterService;
    private final ObjectMapper objectMapper;

    public ProductServiceImpl(
            ResponseGeneratorServiceForExistingUser recentlyViewResponseForExistingUser,
            ResponseGeneratorServiceForNewUser recentlyViewResponseForNewUser,
            ProductServiceDaoHandler productServiceDaoHandler,
            ProductDetailsService productDetailsService,
            CartOperationService cartOperationService,
            CartProductProducer cartProductProducer,
            RecentViewProducer recentViewProducer,
            FilterService filterService,
            ObjectMapper objectMapper
    ){
        this.recentlyViewResponseForExistingUser = recentlyViewResponseForExistingUser;
        this.recentlyViewResponseForNewUser = recentlyViewResponseForNewUser;
        this.productServiceDaoHandler = productServiceDaoHandler;
        this.productDetailsService = productDetailsService;
        this.cartOperationService = cartOperationService;
        this.cartProductProducer = cartProductProducer;
        this.recentViewProducer = recentViewProducer;
        this.filterService = filterService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ApiResponse prepareDashboard(String userId, String sessionId) {
        if(userId != null){
            /*If recentlyViewedProducts not empty then will use it as it is, if empty means user does not have past views hence prepare response accordingly*/
            List<RecentView> recentViewList = fetchRecentlyViewedProducts(userId);
            /*Produce recentlyViewedProducts List in kafka to make KTable for caching*/
            produceRecentViewProductsInKafka(userId, recentViewList);

            /*get all the products from the cart database table store it in Map<ProductId, CartProduct> so that performing operation(increase, decrease, remove) on cart becomes easy*/
            Map<String, CartProduct> cartProductsMap = fetchCartProducts(userId);
            /*Produce cartProductsMap List in kafka to make KTable for caching*/
            produceCartProductsInKafka(userId, cartProductsMap);

            /*prepare dashboard for the user*/
            return prepareDashboardBasedOnUser(userId, recentViewList);
        }else{
            throw new CustomException("UserId is null");
        }
    }

    private List<RecentView> fetchRecentlyViewedProducts(String userId) {
        return productServiceDaoHandler.fetchRecentlyViewedProductsFromDatabase(userId);
    }

    /*private void fetchRecentlyViewedProducts(String userId) {
        recentlyViewedProducts = productServiceDaoHandler.fetchRecentlyViewedProductsFromDatabase(userId);
        System.out.println("Recently Viewed Products are" + recentlyViewedProducts);
    }

    private void deleteOlderRecentViewRecords(String userId) {
        List<String> latestViewDates = recentlyViewedProducts.stream().map(RecentView::getViewDate).collect(Collectors.toList());
        productServiceDaoHandler.deleteLeastRecentViewProducts(userId, latestViewDates);
    }*/

    private void produceRecentViewProductsInKafka(String userId, List<RecentView> recentViewList) {
        recentViewProducer.produceRecentViewProductsInKafkaTopic(userId, recentViewList);
    }

    private Map<String, CartProduct> fetchCartProducts(String userId) {
        return productServiceDaoHandler
                .fetchCartProductsFromDatabase(userId)
                .stream()
                .collect(
                        Collectors.toMap(
                                CartProduct::getProductId,
                                cart -> cart
                        )
                );
    }

    private void produceCartProductsInKafka(String userId, Map<String, CartProduct> cartProductsMap) {
        cartProductProducer.produceCartProductsInKafkaTopic(userId, cartProductsMap);
    }

    private ApiResponse prepareDashboardBasedOnUser(String userId, List<RecentView> recentlyViewedProducts) {
        if(recentlyViewedProducts.isEmpty()){
            /*If New User*/
            NewUserDataObjectResponse newUserDataObjectResponse = recentlyViewResponseForNewUser.buildResponseForNewUser();
            return ApiResponse.builder().code(200).responseBuilder(newUserDataObjectResponse).build();
        }else{
            /*If Existing User*/
            ExistingUserDataObjectResponse existingUserDataObjectResponse = recentlyViewResponseForExistingUser.buildResponseForExistingUser(userId);
            return ApiResponse.builder().code(200).responseBuilder(existingUserDataObjectResponse).build();
        }
    }

    /*Based on actions performed by user pings are received, hence perform operations based on different pings
    @Override
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
     */
}
