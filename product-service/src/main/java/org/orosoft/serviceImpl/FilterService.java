package org.orosoft.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.orosoft.common.AppConstants;
import org.orosoft.dto.ProductDto;
import org.orosoft.exception.CustomException;
import org.orosoft.hazelcastmap.TempDatabaseMapOperations;
import org.orosoft.response.FilterResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Service
public class FilterService {
    private final TempDatabaseMapOperations tempDatabaseMapOperations;
    private final ObjectMapper objectMapper;

    FilterService(
            TempDatabaseMapOperations tempDatabaseMapOperations,
            ObjectMapper objectMapper
    ){
        this.tempDatabaseMapOperations = tempDatabaseMapOperations;
        this.objectMapper = objectMapper;
    }

    public String getFilteredProducts(String ping, char categoryId, String filter) {
        try {

            if(ping == null || filter == null) throw new CustomException("Null values found");
            if(categoryId < 65 || categoryId > 76) throw new CustomException("Unknown category Id");

            List<ProductDto> filteredProductsList = new ArrayList<>();

            if(filter.equals(AppConstants.POPULARITY)){
                filteredProductsList = filterBasedOnPopularity(categoryId);
            }

            if(filter.equals(AppConstants.LOW_TO_HIGH)){
                filteredProductsList = filterBasedOnPriceAscending(categoryId);
            }

            if(filter.equals(AppConstants.HIGH_TO_LOW)){
                filteredProductsList = filterBasedOnPriceDescending(categoryId);
            }

            if(filter.equals(AppConstants.NEWEST_FIRST)){
                filteredProductsList = filterBasedOnNewestFirst(categoryId);
            }

            FilterResponse filteredProductObject = FilterResponse.builder()
                    .ping(ping)
                    .categoryId(String.valueOf(categoryId))
                    .products(filteredProductsList)
                    .build();

            return objectMapper.writeValueAsString(filteredProductObject);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ProductDto> filterBasedOnPopularity(char categoryId){
        List<ProductDto> popularityDesc = getProductCollectionBasedOnCategoryIdFromCache(categoryId)
                .stream()
                .sorted(Comparator.comparingInt(ProductDto::getProductViewCount).reversed())
                .toList();
        System.out.println(popularityDesc);
        return popularityDesc;
    }

    public List<ProductDto> filterBasedOnPriceAscending(char categoryId){
        List<ProductDto> priceAsc = getProductCollectionBasedOnCategoryIdFromCache(categoryId)
                .stream()
                .sorted(Comparator.comparingDouble(ProductDto::getProductMarketPrice))
                .toList();
        System.out.println(priceAsc);
        return priceAsc;
    }

    public List<ProductDto> filterBasedOnPriceDescending(char categoryId){
        List<ProductDto> priceDesc = getProductCollectionBasedOnCategoryIdFromCache(categoryId)
                .stream()
                .sorted(Comparator.comparingDouble(ProductDto::getProductMarketPrice).reversed())
                .toList();
        System.out.println(priceDesc);
        return priceDesc;
    }

    public List<ProductDto> filterBasedOnNewestFirst(char categoryId){
        List<ProductDto> newestFirst = getProductCollectionBasedOnCategoryIdFromCache(categoryId)
                .stream()
                .sorted(Comparator.comparing(ProductDto::getProductId).reversed())
                .toList();
        System.out.println(newestFirst);
        return newestFirst;
    }

    private Collection<ProductDto> getProductCollectionBasedOnCategoryIdFromCache(char categoryId){
        return tempDatabaseMapOperations.getProductCollectionBasedOnCategoryId(categoryId);
    }
}
