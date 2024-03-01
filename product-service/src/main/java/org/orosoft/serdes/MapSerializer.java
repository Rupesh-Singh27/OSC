package org.orosoft.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import org.orosoft.entity.Cart;

import java.util.Map;

public class MapSerializer implements Serializer<Map<String, Cart>> {
    @Override
    public byte[] serialize(String topic, Map<String, Cart> cartProducts) {
        byte[] cartProductsBytes;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            cartProductsBytes = objectMapper.writeValueAsBytes(cartProducts);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return cartProductsBytes;
    }
}
