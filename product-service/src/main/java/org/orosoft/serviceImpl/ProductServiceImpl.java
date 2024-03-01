package org.orosoft.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.kstream.KTable;
import org.orosoft.common.AppConstants;
import org.orosoft.entity.Cart;
import org.orosoft.entity.RecentView;
import org.orosoft.repository.CartRepository;
import org.orosoft.repository.RecentViewRepository;
import org.orosoft.request.MTPingRequest;
import org.orosoft.response.ApiResponse;
import org.orosoft.response.ExistingUserDataObjectResponse;
import org.orosoft.response.NewUserDataObjectResponse;
import org.orosoft.service.ProductService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService{

    private final RecentViewRepository recentViewRepository;
    private final ProductServiceDaoHandler productServiceDaoHandler;
    private final ResponseGeneratorServiceForNewUser recentlyViewResponseForNewUser;
    private final ResponseGeneratorServiceForExistingUser recentlyViewResponseForExistingUser;
    private final ProductDetailsService productDetailsService;
    private final KafkaProducer<String, List<RecentView>> kafkaProducerForRecentView;
    private final KafkaProducer<String, Map<String, Cart>> kafkaProducerForCartProducts;
    private final CartRepository cartRepository;
    private final FilterService filterService;
    private final CartOperationService cartOperationService;
    private final ObjectMapper objectMapper;
    private KTable<String, List<RecentView>> recentViewKTable;
    private List<RecentView> recentlyViewedProducts;

    public ProductServiceImpl(
            RecentViewRepository recentViewRepository,
            ProductServiceDaoHandler productServiceDaoHandler,
            ProductDetailsService productDetailsService,
            ResponseGeneratorServiceForNewUser recentlyViewResponseForNewUser,
            ResponseGeneratorServiceForExistingUser recentlyViewResponseForExistingUser,
            @Qualifier("kafkaProducerForRecentView")KafkaProducer<String, List<RecentView>> kafkaProducerForRecentView,
            @Qualifier("kafkaProducerForCartProducts")KafkaProducer<String, Map<String, Cart>> kafkaProducerForCartProducts,
            CartRepository cartRepository,
            FilterService filterService,
            CartOperationService cartOperationService,
            ObjectMapper objectMapper
    ){
        this.recentViewRepository = recentViewRepository;
        this.productServiceDaoHandler = productServiceDaoHandler;
        this.productDetailsService = productDetailsService;
        this.recentlyViewResponseForNewUser = recentlyViewResponseForNewUser;
        this.recentlyViewResponseForExistingUser = recentlyViewResponseForExistingUser;
        this.kafkaProducerForRecentView = kafkaProducerForRecentView;
        this.kafkaProducerForCartProducts = kafkaProducerForCartProducts;
        this.filterService = filterService;
        this.cartOperationService = cartOperationService;
        this.cartRepository = cartRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public ApiResponse prepareDashboard(String userId, String sessionId) {

        fetchRecentlyViewedProducts(userId);
        deleteOlderRecords(userId);
        produceRecentViewProductsInKafkaTopic(userId);

        Map<String, Cart> cartProductsMap = fetchCartProducts(userId);
        produceCartProductsInKafkaTopic(userId, cartProductsMap);

        return prepareDashboardBasedOnUser(userId);
    }

    /*If recentlyViewedProducts not empty then will use it as it is, if empty means user does not have past views hence prepare response accordingly*/
    private void fetchRecentlyViewedProducts(String userId) {
        recentlyViewedProducts = productServiceDaoHandler.fetchRecentlyViewedProductsFromDatabase(userId);
        System.out.println("Recently Viewed Products are" + recentlyViewedProducts);
    }

    /*Delete other older records except recent 6*/
    private void deleteOlderRecords(String userId) {
        List<String> latestViewDates = recentlyViewedProducts.stream().map(RecentView::getViewDate).collect(Collectors.toList());
        recentViewRepository.deleteOldRecentViews(userId, latestViewDates);
    }

    /*Produce recentlyViewedProducts List in kafka to make KTable for caching*/
    private void produceRecentViewProductsInKafkaTopic(String userId) {
        ProducerRecord<String, List<RecentView>> record = new ProducerRecord<>(AppConstants.RECENT_VIEW_TOPIC_NAME, userId, recentlyViewedProducts);
        kafkaProducerForRecentView.send(record);
    }

    /*get all the products from the cart database table store it in Map<ProductId, CartProduct> so that performing operation(increase, decrease, remove) on cart becomes easy*/
    private Map<String, Cart> fetchCartProducts(String userId) {
        return productServiceDaoHandler
                .fetchCartProductsFromDatabase(userId)
                .stream()
                .collect(
                        Collectors.toMap(
                                Cart::getProductId,
                                cart -> cart
                        )
                );
    }

    /*Produce cartProductsMap List in kafka to make KTable for caching*/
    private void produceCartProductsInKafkaTopic(String userId, Map<String, Cart> cartProductsMap) {
        ProducerRecord<String, Map<String, Cart>> record = new ProducerRecord<>(AppConstants.CART_PRODUCT_TOPIC_NAME, userId, cartProductsMap);
        kafkaProducerForCartProducts.send(record);
    }

    /*prepare dashboard for the user*/
    private ApiResponse prepareDashboardBasedOnUser(String userId) {
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

    /*Based on actions performed by user pings are received, hence perform operations based on different pings*/
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
            default:
        }
        return productResponseForPings;
    }

    @PreDestroy
    public void closeProducer(){
        kafkaProducerForRecentView.close();
        kafkaProducerForCartProducts.close();
    }
}
