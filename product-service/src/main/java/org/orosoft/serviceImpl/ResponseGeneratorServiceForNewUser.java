package org.orosoft.serviceImpl;


import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.annotation.PostConstruct;
import org.orosoft.common.AppConstants;
import org.orosoft.dto.CategoryDto;
import org.orosoft.dto.ProductDto;
import org.orosoft.response.CategoryResponse;
import org.orosoft.response.FeaturedProductResponse;
import org.orosoft.response.NewUserDataObjectResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResponseGeneratorServiceForNewUser {

    private final HazelcastInstance hazelcastInstance;
    private IMap<Character, Map<String, ProductDto>> tempDatabase;

    public ResponseGeneratorServiceForNewUser(HazelcastInstance hazelcastInstance){
        this.hazelcastInstance = hazelcastInstance;
    }

    @PostConstruct
    public void initializeTempDatabase(){
        tempDatabase = hazelcastInstance.getMap(AppConstants.TEMP_DATABASE);
    }

    public NewUserDataObjectResponse buildResponseForNewUser(){
        List<ProductDto> productList = sortProductInDescending();
        List<CategoryDto> categoryList = sortCategoriesInDescending();

        List<Object> mainData = new ArrayList<>();

        mainData.add(CategoryResponse.builder().type("Categories").categories(categoryList).build());
        mainData.add(FeaturedProductResponse.builder().type("Featured Products").featureProducts(productList).build());

        return NewUserDataObjectResponse.builder().data(mainData).build();
    }

    /*Get all products from hazelcast cache and sort it in descending based on their view count*/
    private List<ProductDto> sortProductInDescending() {

        return tempDatabase
                .values()
                .stream()
                .flatMap(map -> map.values().stream())
                .sorted(Comparator.comparingInt(ProductDto::getProductViewCount).reversed())
                .toList();
    }


    public List<CategoryDto> sortCategoriesInDescending() {
        Map<Character, Integer> categoryOverAllViewCount = calculateCategoryOverAllViewCount();

        Map<Character, Integer> descSortedCategories = sortCategoriesInDescending(categoryOverAllViewCount);

        return getCategoryList(descSortedCategories);
    }

    /*Get all products from hazelcast cache group them by categoryId and do the summation of all the view counts for that category. Will return Map<CategoryId, Category's OverAll ViewCount>*/
    private Map<Character, Integer> calculateCategoryOverAllViewCount() {
        return tempDatabase
                .values()
                .stream()
                .flatMap(map -> map.values().stream())
                .collect(Collectors.groupingBy(
                                product -> product.getCategory().getCategoryId(),
                                Collectors.summingInt(ProductDto::getProductViewCount)
                        )
                );
    }

    /*Will sort the category based on Category's OverAll ViewCount and Collect in LinkedHashMap*/
    private Map<Character, Integer> sortCategoriesInDescending(Map<Character, Integer> categoryViewCounts) {
        return categoryViewCounts
                .entrySet()
                .stream()
                .sorted(this::sortInDescending)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, //when two entries have the same key it retains the value of the first entry encountered (preserving the order)
                        LinkedHashMap::new //preserves the order of elements according to their insertion order.
                ));
    }

    /*Will return Category List in Descending Order*/
    private List<CategoryDto> getCategoryList(Map<Character, Integer> descSortedCategories) {
        return descSortedCategories
                .keySet()
                .stream()
                .map(
                        categoryId -> tempDatabase
                                .get(categoryId)
                                .values()
                                .stream()
                                .findFirst()
                                .map(ProductDto::getCategory)
                                .orElse(null)

                )
                .filter(Objects::nonNull)
                .toList();
    }

    private int sortInDescending(Map.Entry<Character, Integer> comparedEntry, Map.Entry<Character, Integer> comparingEntry) {

        int comparisonResult = comparingEntry.getValue().compareTo(comparedEntry.getValue());

        if(comparisonResult == 0){
            comparisonResult = comparedEntry.getKey().compareTo(comparingEntry.getKey());
        }
        return comparisonResult;
    }
}
