package org.orosoft.serdes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.orosoft.entity.CartProduct;

import java.io.IOException;
import java.util.Map;

public class MapDeserializer implements Deserializer<Map<String, CartProduct>> {
    @Override
    public Map<String, CartProduct> deserialize(String topic, byte[] cartProductsBytes) {
        Map<String, CartProduct> cartProducts;

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            TypeReference<Map<String, CartProduct>> mapTypeReference = new TypeReference<>(){};
            cartProducts = objectMapper.readValue(cartProductsBytes,mapTypeReference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return cartProducts;
    }
}
