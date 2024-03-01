package org.orosoft.serdes;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.orosoft.entity.Cart;

import java.util.Map;

public class MapSerDe implements Serde<Map<String, Cart>> {
    @Override
    public Serializer<Map<String, Cart>> serializer() {
        return new MapSerializer();
    }

    @Override
    public Deserializer<Map<String, Cart>> deserializer() {
        return new MapDeserializer();
    }
}
