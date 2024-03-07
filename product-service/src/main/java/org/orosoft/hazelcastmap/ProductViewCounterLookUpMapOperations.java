package org.orosoft.hazelcastmap;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.orosoft.common.AppConstants;
import org.orosoft.repository.ProductRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductViewCounterLookUpMapOperations {

    private final HazelcastInstance hazelcastInstance;
    private final ProductRepository productRepository;
    private IMap<String, Integer> productViewCounter;

    public ProductViewCounterLookUpMapOperations(
            HazelcastInstance hazelcastInstance,
            ProductRepository productRepository
    ) {
        this.hazelcastInstance = hazelcastInstance;
        this.productRepository = productRepository;
    }

    @PostConstruct
    public void initProductViewCounterMap(){
        this.productViewCounter = hazelcastInstance.getMap(AppConstants.VIEW_COUNTER);
    }

    public void insertViewCountOfAProductInMap(String productId, int viewCount){
        productViewCounter.put(productId, viewCount);
    }

    public int fetchViewCounterOfAProductFromMap(String key){
        return productViewCounter.getOrDefault(key, 0);
    }

    /*Run every 30 sec and keep updating the view count in database table*/
    @Scheduled(cron = "0/30 * * * * ?")
    public void updateViewCount() {
        log.info("CRON Job Executed");
        productViewCounter.forEach(productRepository::updateViewCount);
    }
}
