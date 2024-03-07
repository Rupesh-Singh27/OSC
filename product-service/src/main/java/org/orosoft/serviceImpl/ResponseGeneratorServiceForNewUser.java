package org.orosoft.serviceImpl;


import org.orosoft.dto.CategoryDto;
import org.orosoft.dto.ProductDto;
import org.orosoft.hazelcastmap.TempDatabaseMapOperations;
import org.orosoft.response.CategoryResponse;
import org.orosoft.response.FeaturedProductResponse;
import org.orosoft.response.NewUserDataObjectResponse;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ResponseGeneratorServiceForNewUser {
    private final TempDatabaseMapOperations tempDatabaseMapOperations;
    private final CategorySortHandler categorySortHandler;

    public ResponseGeneratorServiceForNewUser(
            TempDatabaseMapOperations tempDatabaseMapOperations,
            CategorySortHandler categorySortHandler
    ){
        this.tempDatabaseMapOperations = tempDatabaseMapOperations;
        this.categorySortHandler = categorySortHandler;
    }

    public NewUserDataObjectResponse buildResponseForNewUser(){
        /*Get all products from tempDatabase hazelcast cache and sort it in descending order based on their view count*/
        Collection<Map<String, ProductDto>> productCollection = getProductCollection();
        List<ProductDto> productList = sortProductInDescending(productCollection);

        /*Get categories in descending order*/
        List<CategoryDto> categoryList = getSortedCategoriesInDescending();

        List<Object> mainData = new ArrayList<>();
        mainData.add(CategoryResponse.builder().type("Categories").categories(categoryList).build());
        mainData.add(FeaturedProductResponse.builder().type("Featured Products").featureProducts(productList).build());

        return NewUserDataObjectResponse.builder().data(mainData).build();
    }


    private List<ProductDto> sortProductInDescending(Collection<Map<String, ProductDto>> productCollection) {
        return productCollection
                .stream()
                .flatMap(map -> map.values().stream())
                .sorted(Comparator.comparingInt(ProductDto::getProductViewCount).reversed())
                .toList();
    }

    private Collection<Map<String, ProductDto>> getProductCollection() {
         return tempDatabaseMapOperations.getProductCollection();
    }

    public List<CategoryDto> getSortedCategoriesInDescending() {
       return categorySortHandler.sortCategoriesInDescending();
    }
}
