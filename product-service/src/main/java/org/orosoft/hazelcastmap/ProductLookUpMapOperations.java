package org.orosoft.hazelcastmap;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.annotation.PostConstruct;
import org.orosoft.common.AppConstants;
import org.orosoft.dto.ProductDto;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProductLookUpMapOperations {

    private final HazelcastInstance hazelcastInstance;
    private IMap<String, ProductDto> productLookUpHazelcastMap;

    public ProductLookUpMapOperations(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @PostConstruct
    private void setupProductLookUpHazelcastMap() {
        this.productLookUpHazelcastMap = hazelcastInstance.getMap(AppConstants.PRODUCT_LOOKUP_MAP);
    }

    public void putMapInHazelcastMap(Map<String, ProductDto> productLookUpMap){
        productLookUpHazelcastMap.putAll(productLookUpMap);
    }

    public ProductDto getProductFromHazelcastMap(String productId){
        return productLookUpHazelcastMap.get(productId);
    }
}
