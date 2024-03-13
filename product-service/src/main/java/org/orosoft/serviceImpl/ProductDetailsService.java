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

    ProductDetailsService(
            TempDatabaseMapOperations tempDatabaseMapOperations,
            RecentViewUpdateService recentViewUpdateService,
            ObjectMapper objectMapper
    )
    {
        this.tempDatabaseMapOperations = tempDatabaseMapOperations;
        this.recentViewUpdateService = recentViewUpdateService;
        this.objectMapper = objectMapper;
    }

    /*Summary: Creating the MT:2 response, Getting the product with similar product and calling the recent view update service to update the recent view*/
    public String prepareProductDetailsWithSimilarProducts(char categoryId, String productId, String userId){
        try {
            if(productId == null || userId == null) throw new CustomException("Either ProductId or UserId is null");
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
}
