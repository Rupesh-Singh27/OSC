package org.orosoft.serviceImpl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.modelmapper.ModelMapper;
import org.orosoft.common.AppConstants;
import org.orosoft.dto.CategoryDto;
import org.orosoft.dto.ProductDto;
import org.orosoft.entity.Cart;
import org.orosoft.entity.Product;
import org.orosoft.entity.RecentView;
import org.orosoft.response.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ResponseGeneratorServiceForExistingUser {
    private final HazelcastInstance hazelcastInstance;
    private final ModelMapper modelMapper;
    private final KafkaStreams kafkaStreamsForRecentView;
    private final KafkaStreams kafkaStreamsForCartProducts;
    private final ResponseGeneratorServiceForNewUser responseGeneratorServiceForNewUser;
    private IMap<Character, Map<String, ProductDto>> tempDatabase;

    ResponseGeneratorServiceForExistingUser(
            HazelcastInstance hazelcastInstance,
            @Qualifier("kafkaStreamsForRecentView")KafkaStreams kafkaStreamsForRecentView,
            @Qualifier("kafkaStreamsForCartProducts") KafkaStreams kafkaStreamsForCartProducts,
            ModelMapper modelMapper,
            ResponseGeneratorServiceForNewUser responseGeneratorServiceForNewUser
    ){
        this.hazelcastInstance = hazelcastInstance;
        this.modelMapper = modelMapper;
        this.kafkaStreamsForRecentView = kafkaStreamsForRecentView;
        this.kafkaStreamsForCartProducts = kafkaStreamsForCartProducts;
        this.responseGeneratorServiceForNewUser = responseGeneratorServiceForNewUser;
    }

    @PostConstruct
    public void initializeTempDatabase(){
        tempDatabase = hazelcastInstance.getMap(AppConstants.TEMP_DATABASE);
    }

    /*Prepare the response by getting all the necessary lists and sets */
    public ExistingUserDataObjectResponse buildResponseForExistingUser(String userId){

        List<ProductDto> recentViewedProductList = getRecentViewedProductFromCache(userId);
        Set<ProductDto> similarProductSet = getSimilarProducts(recentViewedProductList);
        CartDataForDashboard cartDataForDashboard = getCartProducts(userId);
        List<CategoryDto> categoryListInDescending = responseGeneratorServiceForNewUser.sortCategoriesInDescending();

        similarProductSet.forEach(prod -> System.out.println(prod.getProductId()));

        List<Object> mainData = new ArrayList<>();

        mainData.add(RecentlyViewResponse.builder().type("Recently Viewed Products").recentlyViewedProducts(recentViewedProductList).build());
        mainData.add(SimilarProductResponse.builder().type("Similar Products").similarProducts(similarProductSet).build());
        mainData.add(CategoryResponse.builder().type("Categories").categories(categoryListInDescending).build());
        mainData.add(CartResponseForDashboard.builder().type("Cart").cartDataForDashboard(cartDataForDashboard).build());
        return ExistingUserDataObjectResponse.builder().data(mainData).build();
    }



    /*Get recentlyViewedProducts List from KTable*/
    public List<ProductDto> getRecentViewedProductFromCache(String userId) {
        ReadOnlyKeyValueStore<String, List<RecentView>> recentViewProductStore =
                kafkaStreamsForRecentView.store(StoreQueryParameters.fromNameAndType("recent-view-products-store", QueryableStoreTypes.keyValueStore()));

        List<RecentView> recentlyViewedProducts = recentViewProductStore.get(userId);

        return recentlyViewedProducts
                .stream()
                .map(this::productToProductDTOValueMapper)
                .collect(Collectors.toList());
        /*recentlyViewedProducts.forEach(prod -> {
            System.out.println(prod.getProduct().getProductId());
        });*/
    }

    /*Build similar products list based on recently viewed products.*/
    public Set<ProductDto> getSimilarProducts(List<ProductDto> recentViewProductList) {
        Set<ProductDto> similarProductSet = computeSimilarProductsSet(recentViewProductList);

        /*
        * If Recent View has less than 6 product then similar products will have less than 6 as well,
        * therefore the rest of the slots of “Similar Products” would be filled with similar products from the user’s most recently viewed category
        * (in descending order of their rank).
        * */
        if(similarProductSet.size() < 6) completeSimilarProductsForCategory(recentViewProductList, similarProductSet);

        return similarProductSet;
    }

    /*Get the highest viewed product from every category of the products which user has recently seen. If highest viewed is in recently viewed then get next highest.*/
    private Set<ProductDto> computeSimilarProductsSet(List<ProductDto> recentViewProductList) {
        Set<ProductDto> similarProductSet = new LinkedHashSet<>();

        /*Constant loop always run 6 times*/
        for(ProductDto productDto : recentViewProductList){
            /*All Products in most view desc from the current product's category*/
            List<ProductDto> allProductsOfACategoryInViewCountDesc = tempDatabase
                    .get(productDto.getCategory().getCategoryId())
                    .values()
                    .stream()
                    .sorted(this::sortInDescending)
                    .toList();

            /*Make sure that the same products are not present in recent view list or similar product list already*/
            for(ProductDto product : allProductsOfACategoryInViewCountDesc){
                if(!recentViewProductList.contains(product) && !similarProductSet.contains(product)){
                    similarProductSet.add(product);
                    break;
                }
            }
        }
        return similarProductSet;
    }

    /*TODO: Make the List<ProductDto> recentViewProductList as instance variable as it has been used in multiple funtions*/
    private void completeSimilarProductsForCategory(List<ProductDto> recentViewProductList, Set<ProductDto> similarProductSet) {
        char categoryId = recentViewProductList
                .stream()
                .findFirst()
                .map(ProductDto::getCategory)
                .map(CategoryDto::getCategoryId)
                .orElseThrow(() -> new NoSuchElementException("No category found in recentViewProductList"));

        tempDatabase
                .get(categoryId)
                .values()
                .stream()
                .sorted(this::sortInDescending)
                .filter(productDto -> !recentViewProductList.contains(productDto) && !similarProductSet.contains(productDto))
                .forEach(productDto -> {
                    if(similarProductSet.size() < 6){
                         similarProductSet.add(productDto);
                    }
                });
    }

    /*Fetch the cart products from cache(KTable) and return the response for the dashboard*/
    public CartDataForDashboard getCartProducts(String userId){

        ReadOnlyKeyValueStore<String, Map<String, Cart>> cartProductStore = kafkaStreamsForCartProducts.store(StoreQueryParameters
                .fromNameAndType("cart-products-store", QueryableStoreTypes.keyValueStore()));

        log.info("Cart Store Data for {} are {}", userId, cartProductStore.get(userId));

        List<Cart> cartProductList = cartProductStore.get(userId).values().stream().toList();

        return CartDataForDashboard.builder()
                .userId(userId)
                .cartProducts(cartProductList)
                .productCountInCart(cartProductList.size())
                .totalPrice(cartProductList.stream().mapToDouble(Cart::getProductPrice).sum())
                .build();
    }
    private int sortInDescending(ProductDto comparedProduct, ProductDto comparingProduct) {
        int comparisonResult = comparingProduct.getProductViewCount() - comparedProduct.getProductViewCount();

        if(comparisonResult == 0){
            comparisonResult = comparedProduct.getProductId().compareTo(comparingProduct.getProductId());
        }
        return comparisonResult;
    }

    private ProductDto productToProductDTOValueMapper(RecentView recentView) {
        Product product = recentView.getProduct();
        return modelMapper.map(product, ProductDto.class);
    }
}
