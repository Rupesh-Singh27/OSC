package org.orosoft.userservice.common;

import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.orosoft.userservice.dto.EmailDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class HelperComponent {

    @Value("${spring.kafka.topic.name}")
    private String topic;
    private final KafkaTemplate<String, EmailDto> kafkaTemplate;

    public HelperComponent(KafkaTemplate<String, EmailDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public String sendDataToKafkaForEmail(String userEmail, long otp, String userId)  {

        EmailDto emailData = EmailDto.builder()
                .emailId(userEmail)
                .otp(otp)
                .userId(userId)
                .build();

        //Publishing the details in kafka topic and using callback for async response
        CompletableFuture<SendResult<String, EmailDto>> future = kafkaTemplate.send(topic, emailData);
        try {
            future.get();
        } catch (ExecutionException | InterruptedException exception) {
            log.error("Exception while producing to email_topic {}", exception.getMessage());
            return "mail not sent, something went wrong!!!";
        }
        return "mail sent";
    }

    public long generateOTP() {
        Random random = new Random();
        long otp = random.nextLong(900000) + 100000;
        log.info("OTP Generated {}", otp);
        return otp;
    }

    public String generateCustomSessionId(){
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    public int attemptCounter(String key, IMap<String, Integer> attemptsCounterMap){

        /*Key could be userid or email or anything*/
        int attempts = attemptsCounterMap.getOrDefault(key, 0);

        attempts += 1;

        attemptsCounterMap.put(key, attempts);

        return attempts;
    }
}
