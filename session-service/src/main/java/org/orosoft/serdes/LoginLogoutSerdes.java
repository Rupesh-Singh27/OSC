package org.orosoft.serdes;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.orosoft.dto.LoginLogoutDTO;

public class LoginLogoutSerdes implements Serde<LoginLogoutDTO> {

    @Override
    public Serializer<LoginLogoutDTO> serializer() {
        return new LoginLogoutDTOSerializer();
    }

    @Override
    public Deserializer<LoginLogoutDTO> deserializer() {
        return new LoginLogoutDTODeserializer();
    }
}
