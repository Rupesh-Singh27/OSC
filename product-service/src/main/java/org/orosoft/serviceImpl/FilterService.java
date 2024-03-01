package org.orosoft.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.annotation.PostConstruct;
import org.orosoft.common.AppConstants;
import org.orosoft.dto.ProductDto;
import org.orosoft.response.FilterResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class FilterService {
    private final HazelcastInstance hazelcastInstance;
    private final ObjectMapper objectMapper;
    private IMap<Character, Map<String, ProductDto>> tempDatabase;

    FilterService(
            HazelcastInstance hazelcastInstance,
            ObjectMapper objectMapper
    ){
        this.hazelcastInstance = hazelcastInstance;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initializeTempDatabase(){
        /*This could be done in constructor as well, but there could be possible bug which can arise.
        If this line gets executed before hazelcastInstance gets initialized there will be NPE to avoid it either null check can be done or this approach can be used*/
        this.tempDatabase = hazelcastInstance.getMap(AppConstants.TEMP_DATABASE);
    }

    public String getFilteredProducts(String ping, char categoryId, String filter) {
        try {
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
        Map<String, ProductDto> productMap = tempDatabase.get(categoryId);
        List<ProductDto> popularityDesc = productMap
                .values()
                .stream()
                .sorted(Comparator.comparingInt(ProductDto::getProductViewCount).reversed())
                .toList();
        System.out.println(popularityDesc);
        return popularityDesc;
    }

    public List<ProductDto> filterBasedOnPriceAscending(char categoryId){
        Map<String, ProductDto> productMap = tempDatabase.get(categoryId);
        List<ProductDto> priceAsc = productMap
                .values()
                .stream()
                .sorted(Comparator.comparingDouble(ProductDto::getProductMarketPrice))
                .toList();
        System.out.println(priceAsc);
        return priceAsc;
    }

    public List<ProductDto> filterBasedOnPriceDescending(char categoryId){
        Map<String, ProductDto> productMap = tempDatabase.get(categoryId);
        List<ProductDto> priceDesc = productMap
                .values()
                .stream()
                .sorted(Comparator.comparingDouble(ProductDto::getProductMarketPrice).reversed())
                .toList();
        System.out.println(priceDesc);
        return priceDesc;
    }

    public List<ProductDto> filterBasedOnNewestFirst(char categoryId){
        Map<String, ProductDto> productMap = tempDatabase.get(categoryId);
        List<ProductDto> newestFirst = productMap
                .values()
                .stream()
                .sorted(Comparator.comparing(ProductDto::getProductId).reversed())
                .toList();
        System.out.println(newestFirst);
        return newestFirst;
    }
}
