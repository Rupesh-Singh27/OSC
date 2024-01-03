package org.orosoft.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.orosoft.dto.LoginLogoutDTO;

import java.util.Map;

public class LoginLogoutDTOSerializer implements Serializer<LoginLogoutDTO> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, LoginLogoutDTO dtoObject) {
        if (dtoObject == null)
            return null;
        try {
            return objectMapper.writeValueAsBytes(dtoObject);
        } catch (Exception e) {
            throw new SerializationException("Error serializing JSON message", e);
        }
    }

    @Override
    public void close() {
    }
}
