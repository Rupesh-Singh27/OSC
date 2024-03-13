package org.orosoft.serdes;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.orosoft.entity.CartProduct;

import java.util.Map;

public class MapSerDe implements Serde<Map<String, CartProduct>> {
    @Override
    public Serializer<Map<String, CartProduct>> serializer() {
        return new MapSerializer();
    }

    @Override
    public Deserializer<Map<String, CartProduct>> deserializer() {
        return new MapDeserializer();
    }
}
