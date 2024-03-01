package org.orosoft.userservice.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import org.orosoft.userservice.dto.EmailDto;

public class EmailDTOSerializer implements Serializer<EmailDto> {

    @Override
    public byte[] serialize(String topic, EmailDto emailObject) {

        byte[] emailByteArray;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            emailByteArray = objectMapper.writeValueAsBytes(emailObject);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return emailByteArray;
    }
}
