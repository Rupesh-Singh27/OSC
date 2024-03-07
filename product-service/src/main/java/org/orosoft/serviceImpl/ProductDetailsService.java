package org.orosoft.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.orosoft.dto.ProductDto;
import org.orosoft.exception.CustomException;
import org.orosoft.hazelcastmap.TempDatabaseMapOperations;
import org.orosoft.response.ProductDetailResponse;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class ProductDetailsService {
    private final TempDatabaseMapOperations tempDatabaseMapOperations;
    private final RecentViewUpdateService recentViewUpdateService;
    private final ObjectMapper objectMapper;
/*    private final ProductViewCounterLookUpMapOperations productViewCounterLookUpMapOperations;
    private final RecentViewRepository recentViewRepository;
    private final RecentViewProducer recentViewProducer;
    private final RecentViewKTable recentViewKTable;
    private final ModelMapper modelMapper;*/

    ProductDetailsService(
            TempDatabaseMapOperations tempDatabaseMapOperations,
            RecentViewUpdateService recentViewUpdateService,
            ObjectMapper objectMapper
            /*ProductViewCounterLookUpMapOperations productViewCounterLookUpMapOperations,
            RecentViewRepository recentViewRepository,
            RecentViewProducer recentViewProducer,
            RecentViewKTable recentViewKTable,
            ModelMapper modelMapper*/
            )
    {
        this.objectMapper = objectMapper;
        this.tempDatabaseMapOperations = tempDatabaseMapOperations;
        this.recentViewUpdateService = recentViewUpdateService;
        /*this.productViewCounterLookUpMapOperations = productViewCounterLookUpMapOperations;
        this.recentViewRepository = recentViewRepository;
        this.recentViewProducer = recentViewProducer;
        this.recentViewKTable = recentViewKTable;
        this.modelMapper = modelMapper;*/
    }

    /*Prepare Product Details With Similar Products*/
    public String prepareProductDetailsWithSimilarProducts(char categoryId, String productId, String userId){
        try {
            if(productId == null || userId == null) throw new CustomException("Null values in ping");
            if(categoryId < 65 || categoryId > 76) throw new CustomException("Unknown category Id");

            /*Get the appropriate Product from tempDatabase cache*/
            ProductDto product = getProductFromCache(categoryId, productId);
            /*Get the similar products for the product fetched from tempDatabase above.*/
            List<ProductDto> similarProducts = getSimilarProducts(categoryId, productId);


            /*Since the current product is the recently viewed product, update the recently viewed cache i.e KTable*/
            recentViewUpdateService.updateRecentViewCache(product, userId);
            /*Since the currently viewed product is being seen, increasing and caching the view count of the current product in view counter map*/
            recentViewUpdateService.incrementTheViewCountOfAProduct(productId, product);

            System.out.println("Fetched Product: " + product);
            System.out.println("Similar Products: " + similarProducts);

            /*Building response*/
            ProductDetailResponse productDetailResponse = ProductDetailResponse.builder()
                    .ping("2")
                    .categoryId(String.valueOf(product.getCategory().getCategoryId()))
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .productDescription(product.getProductDescription())
                    .productPrice(String.valueOf(product.getProductMarketPrice()))
                    .similarProducts(similarProducts)
                    .build();

            return objectMapper.writeValueAsString(productDetailResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private ProductDto getProductFromCache(char categoryId, String productId) {
        return tempDatabaseMapOperations.getProductBasedOnProductId(categoryId, productId);
    }

    private List<ProductDto> getSimilarProducts(char categoryId, String productId) {
        return getProductCollectionBasedOnCategoryIdFromCache(categoryId)
                .stream()
                .filter(productDto -> !productDto.getProductId().equals(productId))
                .sorted(Comparator.comparingInt(ProductDto::getProductViewCount).reversed())
                .limit(6)
                .toList();
    }

    private Collection<ProductDto> getProductCollectionBasedOnCategoryIdFromCache(char categoryId) {
        return tempDatabaseMapOperations.getProductCollectionBasedOnCategoryId(categoryId);
    }

//    Increasing and caching the view count of the current product in view counter map
    /*private void incrementTheViewCountOfAProduct(String productId, ProductDto product) {
        int productViewCount = getProductViewCountIfExistOrZero(productId);

        if(productViewCount == 0) {
            putViewCountInMap(productId, product.getProductViewCount() + 1);
        } else{
            putViewCountInMap(productId, productViewCount + 1);
        }
    }

    private void putViewCountInMap(String productId, int viewCount) {
        productViewCounterLookUpMapOperations.insertViewCountOfAProductInMap(productId, viewCount);
    }

    private int getProductViewCountIfExistOrZero(String productId) {
        return productViewCounterLookUpMapOperations.fetchViewCounterOfAProductFromMap(productId);
    }

//    Updating Recent View Cache i.e. KTable
    private void updateRecentViewCache(ProductDto product, String userId) {

        RecentView recentlyViewedProduct = prepareRecentlyViewedProduct(product, userId);

        List<RecentView> recentViewList = updateRecentlyViewedList(userId, recentlyViewedProduct);

        produceRecentlyViewedListInTopic(userId, recentViewList);

        System.out.println("Updated View Store Data: ");
        recentViewList.forEach(recentView -> System.out.print(recentView.toString()));
        System.out.println();
    }

//    Prepare Recently Viewed Product Object
    private RecentView prepareRecentlyViewedProduct(ProductDto product, String userId) {
        return RecentView.builder()
                .userId(userId)
                .product(modelMapper.map(product, Product.class))
                .viewDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
    }

    private List<RecentView> updateRecentlyViewedList(String userId, RecentView recentlyViewedProduct) {
        List<RecentView> recentViewList = getRecentViewList(userId);

//        If there is already the same product in the recent viewed list, which is currently being viewed by the user. Remove the old one
        Iterator<RecentView> iterator = recentViewList.iterator();
        while (iterator.hasNext()){
            RecentView recentViewedProduct = iterator.next();
            if(recentViewedProduct.getProduct().getProductId().equals(recentlyViewedProduct.getProduct().getProductId())) {
                recentlyViewedProduct.setRecentViewId(recentViewedProduct.getRecentViewId());
                iterator.remove();
            }
        }

        if(recentViewList.size() >= 6){
            recentViewList.remove(recentViewList.size() - 1);
            recentViewList.add(0, recentlyViewedProduct);
        }else {
            recentViewList.add(0, recentlyViewedProduct);
        }
        return recentViewList;
    }

    private List<RecentView> getRecentViewList(String userId) {
        return recentViewKTable.getRecentViewedProductFromKTable(userId);
    }

    private void produceRecentlyViewedListInTopic(String userId, List<RecentView> recentViewList) {
        recentViewProducer.produceRecentViewProductsInKafkaTopic(userId, recentViewList);
    }

//    Updating Recent View Database Table
    public void updateRecentViewProductDatabaseTable(String userId) {
        List<RecentView> recentViewList = getRecentViewList(userId);
        recentViewList.forEach(recentViewRepository::save);
    }*/
}
