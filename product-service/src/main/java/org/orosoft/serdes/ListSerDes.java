package org.orosoft.serdes;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.orosoft.entity.RecentView;

import java.util.List;

public class ListSerDes implements Serde<List<RecentView>> {
    @Override
    public Serializer<List<RecentView>> serializer() {
        return new ListSerializer();
    }

    @Override
    public Deserializer<List<RecentView>> deserializer() {
        return new ListDeserializer();
    }
}
