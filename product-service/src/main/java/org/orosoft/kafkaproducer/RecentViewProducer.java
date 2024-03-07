package org.orosoft.kafkaproducer;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.orosoft.common.AppConstants;
import org.orosoft.entity.RecentView;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RecentViewProducer {

    private final KafkaProducer<String, List<RecentView>> kafkaProducerForRecentView;

    public RecentViewProducer(
            @Qualifier("kafkaProducerForRecentView")KafkaProducer<String, List<RecentView>> kafkaProducerForRecentView
    )
    {
        this.kafkaProducerForRecentView = kafkaProducerForRecentView;
    }

    public void produceRecentViewProductsInKafkaTopic(String userId, List<RecentView> recentlyViewedProducts) {
        ProducerRecord<String, List<RecentView>> record = new ProducerRecord<>(AppConstants.RECENT_VIEW_TOPIC_NAME, userId, recentlyViewedProducts);
        kafkaProducerForRecentView.send(record);
    }

    @PreDestroy
    public void closeProducer(){
        kafkaProducerForRecentView.close();
    }
}
