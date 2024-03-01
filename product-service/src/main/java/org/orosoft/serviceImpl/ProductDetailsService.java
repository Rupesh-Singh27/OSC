package org.orosoft.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.modelmapper.ModelMapper;
import org.orosoft.common.AppConstants;
import org.orosoft.dto.ProductDto;
import org.orosoft.entity.Product;
import org.orosoft.entity.RecentView;
import org.orosoft.repository.ProductRepository;
import org.orosoft.repository.RecentViewRepository;
import org.orosoft.response.ProductDetailResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ProductDetailsService {
    private final HazelcastInstance hazelcastInstance;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final KafkaStreams kafkaStreams;
    private final ProductRepository productRepository;
    private final RecentViewRepository recentViewRepository;
    private final KafkaProducer<String, List<RecentView>> kafkaProducer;
    private ReadOnlyKeyValueStore<String, List<RecentView>> recentViewProductStore;
    private IMap<String, Integer> productViewCounter;
    private IMap<Character, Map<String, ProductDto>> tempDatabase;

    ProductDetailsService(
            @Qualifier("kafkaProducerForRecentView") KafkaProducer<String, List<RecentView>> kafkaProducer,
            @Qualifier("kafkaStreamsForRecentView")KafkaStreams kafkaStreams,
            HazelcastInstance hazelcastInstance,
            RecentViewRepository recentViewRepository,
            ProductRepository productRepository,
            ModelMapper modelMapper,
            ObjectMapper objectMapper)
    {
        this.kafkaStreams = kafkaStreams;
        this.kafkaProducer = kafkaProducer;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.productRepository = productRepository;
        this.recentViewRepository = recentViewRepository;
        this.hazelcastInstance = hazelcastInstance;
    }

    @PostConstruct
    public void startStreamsAndInitializeStateStoresAndMaps(){
        kafkaStreams.start();

        recentViewProductStore =
                kafkaStreams.store(StoreQueryParameters.fromNameAndType("recent-view-products-store", QueryableStoreTypes.keyValueStore()));

        this.productViewCounter = hazelcastInstance.getMap("ViewCounter");

        this.tempDatabase = hazelcastInstance.getMap(AppConstants.TEMP_DATABASE);
    }

    /*Prepare Product Details With Similar Products*/
    public String prepareProductDetailsWithSimilarProducts(char categoryId, String productId, String userId){
        try {
            ProductDto product = tempDatabase.get(categoryId).get(productId);

            updateRecentViewCache(product, userId);
            incrementTheViewCountOfAProduct(productId, product);
            List<ProductDto> similarProducts = getSimilarProducts(categoryId, productId);

            System.out.println("Fetched Product: " + product);
            System.out.println("Similar Products: " + similarProducts);

            ProductDetailResponse productDetailResponse = ProductDetailResponse.builder()
                    .ping("2")
                    .categoryId(String.valueOf(product.getCategory().getCategoryId()))
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .productDescription(product.getProductDescription())
                    .productPrice(String.valueOf(product.getProductMarketPrice()))
                    .similarProducts(similarProducts)
                    .build();

            return objectMapper.writeValueAsString(productDetailResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ProductDto> getSimilarProducts(char categoryId, String productId) {
        return tempDatabase
                .get(categoryId)
                .values()
                .stream()
                .filter(productDto -> !productDto.getProductId().equals(productId))
                .sorted(Comparator.comparingInt(ProductDto::getProductViewCount).reversed())
                .limit(6)
                .toList();
    }

    private void incrementTheViewCountOfAProduct(String productId, ProductDto product) {
        /*Increasing and caching the view count of the current product in view counter map*/
        int productViewCount = productViewCounter.getOrDefault(productId, 0);

        if(productViewCount == 0) {
            productViewCounter.put(productId, product.getProductViewCount() + 1);
        } else{
            productViewCounter.put(productId, productViewCount + 1);
        }
    }

    /*Updating Recent View Cache i.e. KTable*/
    private void updateRecentViewCache(ProductDto product, String userId) {

        RecentView recentlyViewedProduct = prepareRecentlyViewedProduct(product, userId);

        List<RecentView> recentViewList = updateRecentlyViewedList(userId, recentlyViewedProduct);

        produceRecentlyViewedListInTopic(userId, recentViewList);

        System.out.println("Updated View Store Data: ");
        recentViewList.forEach(recentView -> System.out.print(recentView.toString()));
        System.out.println();
    }

    /*Prepare Recently Viewed Product Object*/
    private RecentView prepareRecentlyViewedProduct(ProductDto product, String userId) {
        return RecentView.builder()
                .userId(userId)
                .product(modelMapper.map(product, Product.class))
                .viewDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
    }

    private List<RecentView> updateRecentlyViewedList(String userId, RecentView recentlyViewedProduct) {
        List<RecentView> recentViewList = recentViewProductStore.get(userId);

        /*If there is already the same product in the recent viewed list, which is currently being viewed by the user. Remove the old one*/
        Iterator<RecentView> iterator = recentViewList.iterator();
        while (iterator.hasNext()){
            RecentView recentViewedProduct = iterator.next();
            if(recentViewedProduct.getProduct().getProductId().equals(recentlyViewedProduct.getProduct().getProductId())) {
                recentlyViewedProduct.setRecentViewId(recentViewedProduct.getRecentViewId());
                iterator.remove();
            }
        }

        if(recentViewList.size() >= 6){
            recentViewList.remove(recentViewList.size() - 1);
            recentViewList.add(0, recentlyViewedProduct);
        }else {
            recentViewList.add(0, recentlyViewedProduct);
        }
        return recentViewList;
    }

    private void produceRecentlyViewedListInTopic(String userId, List<RecentView> recentViewList) {
        ProducerRecord<String, List<RecentView>> record = new ProducerRecord<>("RecentViewProductsTopic", userId, recentViewList);
        kafkaProducer.send(record);
    }

    /*Updating Recent View Database Table*/
    public void updateRecentViewProductDatabaseTable(String userId) {
        List<RecentView> recentViewList = recentViewProductStore.get(userId);
        recentViewList.forEach(recentViewRepository::save);
    }

    /*Run every 30 sec and keep updating the view count in database table*/
    @Scheduled(cron = "0/30 * * * * ?")
    public void updateViewCount() {
        log.info("CRON Job Executed");
        productViewCounter.forEach(productRepository::updateViewCount);
    }

    @PreDestroy
    public void closeProducer(){
        kafkaStreams.close();
    }
}
