package org.orosoft.serviceImpl;

import org.orosoft.dto.CategoryDto;
import org.orosoft.dto.ProductDto;
import org.orosoft.hazelcastmap.TempDatabaseMapOperations;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class CategorySortHandler {

    private final TempDatabaseMapOperations tempDatabaseMapOperations;

    public CategorySortHandler(TempDatabaseMapOperations tempDatabaseMapOperations) {
        this.tempDatabaseMapOperations = tempDatabaseMapOperations;
    }

    public List<CategoryDto> sortCategoriesInDescending() {
        /*Get all products from tempDatabase hazelcast cache, group them by categoryId and do the summation of all the view counts for that category.*/
        Map<Character, Integer> categoryOverAllViewCount = calculateCategoryOverAllViewCount();

        /*Will sort the category based on Category's OverAll ViewCount and Collect in LinkedHashMap*/
        Map<Character, Integer> descSortedCategories = sortCategoriesInDescending(categoryOverAllViewCount);

        return getCategoryList(descSortedCategories);
    }

    /*Will return Map<CategoryId, Category's OverAll ViewCount>*/
    private Map<Character, Integer> calculateCategoryOverAllViewCount() {
        Collection<Map<String, ProductDto>> productCollection = getProductCollection();

        return productCollection
                .stream()
                .flatMap(map -> map.values().stream())
                .collect(Collectors.groupingBy(
                                product -> product.getCategory().getCategoryId(),
                                Collectors.summingInt(ProductDto::getProductViewCount)
                        )
                );
    }

    private Collection<Map<String, ProductDto>> getProductCollection() {
        return tempDatabaseMapOperations.getProductCollection();
    }

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
                        categoryId -> getProductCollectionBasedOnCategoryId(categoryId)
                                .stream()
                                .findFirst()
                                .map(ProductDto::getCategory)
                                .orElse(null)

                )
                .filter(Objects::nonNull)
                .toList();
    }

    private Collection<ProductDto> getProductCollectionBasedOnCategoryId(char categoryId) {
        return tempDatabaseMapOperations.getProductCollectionBasedOnCategoryId(categoryId);
    }

    private int sortInDescending(Map.Entry<Character, Integer> comparedEntry, Map.Entry<Character, Integer> comparingEntry) {

        int comparisonResult = comparingEntry.getValue().compareTo(comparedEntry.getValue());

        if(comparisonResult == 0){
            comparisonResult = comparedEntry.getKey().compareTo(comparingEntry.getKey());
        }
        return comparisonResult;
    }
}
