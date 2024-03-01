package org.orosoft.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import org.orosoft.entity.RecentView;

import java.util.List;

public class ListSerializer implements Serializer<List<RecentView>> {
    @Override
    public byte[] serialize(String topic, List<RecentView> recentViewsList) {
        byte[] recentViewListBytes;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            recentViewListBytes = objectMapper.writeValueAsBytes(recentViewsList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return recentViewListBytes;
    }
}
