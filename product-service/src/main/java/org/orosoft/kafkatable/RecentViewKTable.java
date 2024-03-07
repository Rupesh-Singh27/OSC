package org.orosoft.kafkatable;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.orosoft.common.AppConstants;
import org.orosoft.entity.RecentView;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RecentViewKTable {

    private final KafkaStreams kafkaStreamsForRecentView;

    public RecentViewKTable(
            @Qualifier("kafkaStreamsForRecentView")KafkaStreams kafkaStreamsForRecentView
    ) {
        this.kafkaStreamsForRecentView = kafkaStreamsForRecentView;
    }

    @PostConstruct
    public void startStreams() {
        kafkaStreamsForRecentView.start();
    }

    public List<RecentView> getRecentViewedProductFromKTable(String userId) {
        ReadOnlyKeyValueStore<String, List<RecentView>> recentViewProductStore =
                kafkaStreamsForRecentView.store(StoreQueryParameters.fromNameAndType(AppConstants.RECENT_VIEW_PRODUCT_STORE, QueryableStoreTypes.keyValueStore()));

        return recentViewProductStore.get(userId);
    }

    @PreDestroy
    public void closeProducer(){
        kafkaStreamsForRecentView.close();
    }
}
