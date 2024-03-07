package org.orosoft.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.orosoft.dto.ProductDto;
import org.orosoft.entity.Product;
import org.orosoft.hazelcastmap.ProductLookUpMapOperations;
import org.orosoft.hazelcastmap.TempDatabaseMapOperations;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataCacheManager {
    private final ProductLookUpMapOperations productLookUpMapOperations;
    private final TempDatabaseMapOperations tempDatabaseMapOperations;
    private final ProductServiceDaoHandler productServiceDaoHandler;
    private final ModelMapper modelMapper;

    public DataCacheManager(
            ProductLookUpMapOperations productLookUpMapOperations,
            TempDatabaseMapOperations tempDatabaseMapOperations,
            ProductServiceDaoHandler productServiceDaoHandler,
            ModelMapper modelMapper
    ) {
        this.productLookUpMapOperations = productLookUpMapOperations;
        this.tempDatabaseMapOperations = tempDatabaseMapOperations;
        this.productServiceDaoHandler = productServiceDaoHandler;
        this.modelMapper = modelMapper;
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
        tempDatabaseMapOperations.putMapInTempDatabase(
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

        /*productId --> Products, Mainly used for cart operations*/
        productLookUpMapOperations.putMapInHazelcastMap(
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
