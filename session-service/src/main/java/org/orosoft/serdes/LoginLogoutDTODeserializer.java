package org.orosoft.serdes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.orosoft.dto.LoginLogoutDTO;

import java.io.IOException;
import java.util.Map;

public class LoginLogoutDTODeserializer implements Deserializer<LoginLogoutDTO> {

    @Override
    public void configure(Map<String, ?> props, boolean isKey) {
    }

    @Override
    public LoginLogoutDTO deserialize(String topic, byte[] dtoObjectBytes) {

        LoginLogoutDTO loginLogoutDTO = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            loginLogoutDTO = objectMapper.readValue(dtoObjectBytes, LoginLogoutDTO.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return loginLogoutDTO;
    }

    @Override
    public void close() {

    }
}
