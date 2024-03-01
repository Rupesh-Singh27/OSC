package org.orosoft.userservice.serdes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.orosoft.userservice.dto.LoginLogoutDTO;

import java.io.IOException;

public class LoginLogoutDTODeserializer implements Deserializer<LoginLogoutDTO> {
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
}
