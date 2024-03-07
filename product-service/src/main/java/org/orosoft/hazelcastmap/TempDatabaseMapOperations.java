package org.orosoft.hazelcastmap;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.annotation.PostConstruct;
import org.orosoft.common.AppConstants;
import org.orosoft.dto.ProductDto;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
public class TempDatabaseMapOperations {

    private final HazelcastInstance hazelcastInstance;
    private IMap<Character, Map<String, ProductDto>> tempDatabaseHazelcastMap;

    public TempDatabaseMapOperations(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    /*
     * The problem is that when the DataCacheManager bean is created, it will set tempDatabase using hazelcastInstance,
     * but if hazelcastInstance is not available at the time of bean creation, it might lead to a NullPointerException
     *
     * Therefore, I used the @PostConstruct annotation on setupTempDatabase method that initializes the tempDatabase
     * after the bean has been constructed and all dependencies have been injected.
     * */
    @PostConstruct
    private void setupTempDatabaseHazelcastMap() {
        this.tempDatabaseHazelcastMap = hazelcastInstance.getMap(AppConstants.TEMP_DATABASE);
    }

    public void putMapInTempDatabase(Map<Character, Map<String, ProductDto>> tempDatabaseLookUpMap){
        tempDatabaseHazelcastMap.putAll(tempDatabaseLookUpMap);
    }

    public Collection<Map<String, ProductDto>> getProductCollection(){
        return tempDatabaseHazelcastMap.values();
    }

    public Collection<ProductDto> getProductCollectionBasedOnCategoryId(char categoryId){
        return tempDatabaseHazelcastMap.get(categoryId).values();
    }

    public ProductDto getProductBasedOnProductId(char categoryId, String productId){
        return tempDatabaseHazelcastMap.get(categoryId).get(productId);
    }
}
