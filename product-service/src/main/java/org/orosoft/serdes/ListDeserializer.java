package org.orosoft.serdes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.orosoft.entity.RecentView;

import java.io.IOException;
import java.util.List;

public class ListDeserializer implements Deserializer<List<RecentView>> {

    @Override
    public List<RecentView> deserialize(String topic, byte[] recentViewBytes) {
        List<RecentView> recentViewList = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            TypeReference<List<RecentView>> typeReference = new TypeReference<>(){};
            recentViewList = objectMapper.readValue(recentViewBytes, typeReference);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return recentViewList;
    }
}
