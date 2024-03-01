package org.orosoft.serviceImpl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.orosoft.common.AppConstants;
import org.orosoft.dto.ProductDto;
import org.orosoft.entity.Product;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataCacheManager {
    private final ProductServiceDaoHandler productServiceDaoHandler;
    private final ModelMapper modelMapper;
    private final HazelcastInstance hazelcastInstance;
    private IMap<Character, Map<String, ProductDto>> tempDatabase;
    private IMap<String, ProductDto> productLookUpMap;

    public DataCacheManager(
            ProductServiceDaoHandler productServiceDaoHandler,
            ModelMapper modelMapper,
            HazelcastInstance hazelcastInstance
    ) {
        this.productServiceDaoHandler = productServiceDaoHandler;
        this.modelMapper = modelMapper;
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
    private void setupLookUpMapAndTempDatabase() {
        this.tempDatabase = hazelcastInstance.getMap(AppConstants.TEMP_DATABASE);
        this.productLookUpMap = hazelcastInstance.getMap(AppConstants.PRODUCT_LOOKUP_MAP);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeTempDatabase() {
        List<Product> productList = fetchAllProducts();
        storeDataInCache(productList);
    }

    private List<Product> fetchAllProducts() {
        return productServiceDaoHandler.fetchProductsFromDatabase();
    }

    private void storeDataInCache(List<Product> productList) {

        /*categoryId --> productId --> Product*/
        tempDatabase.putAll(
                productList.stream()
                        .collect(Collectors.groupingBy(
                                        product -> product.getCategory().getCategoryId(),
                                        Collectors.toMap(
                                                Product::getProductId,
                                                this::convertToDTO
                                        )
                                )
                        )
        );

        /*productId --> Products*/
        productLookUpMap.putAll(
                productList
                        .stream()
                        .collect(Collectors
                                .toMap(
                                        Product::getProductId,
                                        this::convertToDTO
                                )
                        )
        );
    }

    private ProductDto convertToDTO(Product product) {
        return modelMapper.map(product, ProductDto.class);
    }
}
