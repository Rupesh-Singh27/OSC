package org.orosoft.serviceImpl;

import org.modelmapper.ModelMapper;
import org.orosoft.dto.ProductDto;
import org.orosoft.entity.Product;
import org.orosoft.entity.RecentView;
import org.orosoft.hazelcastmap.ProductViewCounterLookUpMapOperations;
import org.orosoft.kafkaproducer.RecentViewProducer;
import org.orosoft.kafkatable.RecentViewKTable;
import org.orosoft.repository.RecentViewRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;

@Component
public class RecentViewUpdateService {

    private final ProductViewCounterLookUpMapOperations productViewCounterLookUpMapOperations;
    private final RecentViewRepository recentViewRepository;
    private final RecentViewProducer recentViewProducer;
    private final RecentViewKTable recentViewKTable;
    private final ModelMapper modelMapper;

    public RecentViewUpdateService(
            ProductViewCounterLookUpMapOperations productViewCounterLookUpMapOperations,
            RecentViewProducer recentViewProducer,
            RecentViewRepository recentViewRepository,
            RecentViewKTable recentViewKTable,
            ModelMapper modelMapper
    ) {
        this.productViewCounterLookUpMapOperations = productViewCounterLookUpMapOperations;
        this.recentViewProducer = recentViewProducer;
        this.recentViewRepository = recentViewRepository;
        this.modelMapper = modelMapper;
        this.recentViewKTable = recentViewKTable;
    }


    /*Increasing and caching the view count of the current product in view counter map*/
    public void incrementTheViewCountOfAProduct(String productId, ProductDto product) {
        int productViewCount = getProductViewCountIfExistOrZero(productId);

        /*If view count of the product in parameter i.e. current viewed product is already being maintained in view counter map increment the count.
        * Otherwise, get the count from the product itself and increment by 1 and insert it into view counter map*/
        if(productViewCount == 0) {
            putViewCountInMap(productId, product.getProductViewCount() + 1);
        } else{
            putViewCountInMap(productId, productViewCount + 1);
        }
    }

    private void putViewCountInMap(String productId, int viewCount) {
        productViewCounterLookUpMapOperations.insertViewCountOfAProductInMap(productId, viewCount);
    }

    private int getProductViewCountIfExistOrZero(String productId) {
        return productViewCounterLookUpMapOperations.fetchViewCounterOfAProductFromMap(productId);
    }

    /*Updating Recent View Cache i.e. KTable*/
    public void updateRecentViewCache(ProductDto product, String userId) {

        /*Prepare Recently Viewed Product Object by providing appropriate values*/
        RecentView currentlyViewedProduct = prepareRecentlyViewedProduct(product, userId);

        /*Replace the most recent viewed product with the current viewed product in recentlyViewProductList, later put that list in KTable cache*/
        List<RecentView> recentViewList = updateRecentlyViewedList(userId, currentlyViewedProduct);

        /*Producing the above made updated recently viewed list in Topic to update the cache*/
        produceRecentlyViewedListInTopic(userId, recentViewList);

        System.out.println("Updated View Store Data: ");
        recentViewList.forEach(recentView -> System.out.print(recentView.toString()));
        System.out.println();
    }

    private RecentView prepareRecentlyViewedProduct(ProductDto product, String userId) {
        return RecentView.builder()
                .userId(userId)
                .product(modelMapper.map(product, Product.class))
                .viewDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
    }

    private List<RecentView> updateRecentlyViewedList(String userId, RecentView currentViewedProduct) {
        List<RecentView> recentViewList = getRecentViewList(userId);

        /*If there is already the same product in the recent viewed list, which is currently being viewed by the user. Remove the old one*/
        Iterator<RecentView> iterator = recentViewList.iterator();
        while (iterator.hasNext()){
            RecentView recentViewedProduct = iterator.next();
            if(recentViewedProduct.getProduct().getProductId().equals(currentViewedProduct.getProduct().getProductId())) {
                currentViewedProduct.setRecentViewId(recentViewedProduct.getRecentViewId());
                iterator.remove();
            }
        }

        if(recentViewList.size() >= 6){
            recentViewList.remove(recentViewList.size() - 1);
            recentViewList.add(0, currentViewedProduct);
        }else {
            recentViewList.add(0, currentViewedProduct);
        }
        return recentViewList;
    }

    private List<RecentView> getRecentViewList(String userId) {
        return recentViewKTable.getRecentViewedProductFromKTable(userId);
    }

    private void produceRecentlyViewedListInTopic(String userId, List<RecentView> recentViewList) {
        recentViewProducer.produceRecentViewProductsInKafkaTopic(userId, recentViewList);
    }

    /*Updating Recent View Database Table on logout*/
    public void updateRecentViewProductDatabaseTable(String userId) {
        List<RecentView> recentViewList = getRecentViewList(userId);
        recentViewList.forEach(recentViewRepository::save);
    }
}
