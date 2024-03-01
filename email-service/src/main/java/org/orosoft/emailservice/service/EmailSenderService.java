package org.orosoft.emailservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.orosoft.emailservice.dto.EmailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Configuration
public class EmailSenderService{

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSenderService.class);

    @Value("${spring.mail.username}")
    private String fromMail;

    private final JavaMailSender javaMailSender;

    EmailSenderService(JavaMailSender javaMailSender){
        this.javaMailSender = javaMailSender;
    }

    @KafkaListener(topics = "email_topic", groupId = "email-group")
    public void fetchDetailsFromTopic(String emailObject) {
        EmailDto emailDto;
        String subject;
        String body;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            emailDto = objectMapper.readValue(emailObject, EmailDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        LOGGER.info("OTP details: "+emailDto.toString());

        String userId = emailDto.getUserId();
        String emailId = emailDto.getEmailId();
        String otp = String.valueOf(emailDto.getOtp());

        //If block will execute while registering the user, else block will execute while sending OTP for forgot password function
        if(userId != null && !userId.isEmpty()){
            subject = "User-ID and One Time Password";
            body = "Your User-ID is: " + userId + "\n" + "Your One Time Password is: " + otp;
        }
        else{
            subject = "One Time Password";
            body = "Your One Time Password is: " + otp;
        }

        System.out.println(userId + "\n" + emailId + "\n" + otp);

        sendSimpleEmail(emailId, subject, body);
    }

    /***
     * <p>Sends an email containing the User-Id and OTP to the specified email address.</p>
     *
     * @param toEmail The email address to which the User-Id and OTP will be sent.
     * @param subject The subject of the email
     * @param body The User-Id and OTP itself
     * ***/
    public void sendSimpleEmail(String toEmail, String subject, String body) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("rupeshsingh27121997@gmail.com");
        message.setTo(toEmail);
        message.setText(body);
        message.setSubject(subject);

        javaMailSender.send(message);

        System.out.println("Mail Sent Successfully...");
    }
}
