package org.orosoft.userservice.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import org.orosoft.userservice.dto.LoginLogoutDTO;

public class LoginLogoutDTOSerializer implements Serializer<LoginLogoutDTO> {
    @Override
    public byte[] serialize(String topic, LoginLogoutDTO dataObject) {

        byte[] loginLogoutDTOBytes = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            loginLogoutDTOBytes = objectMapper.writeValueAsString(dataObject).getBytes();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return loginLogoutDTOBytes;
    }
}
