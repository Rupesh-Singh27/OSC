package org.orosoft.emailservice.service;

import com.hazelcast.shaded.org.json.JSONObject;
import net.devh.boot.grpc.server.service.GrpcService;
import org.orosoft.otp.OtpServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Configuration
@GrpcService
public class EmailSenderService extends OtpServiceGrpc.OtpServiceImplBase{

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSenderService.class);

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromMail;

    /***
     * <p> Kafka Consumer. Fetches the string, converts it into JSON, extracts the necessary details and calls the mailing function. <p>
     *
     * @param otpDetails String which consist of User-Id, Email-Id and OTP in JSON format.
     *
     ***/
    @KafkaListener(topics = "email_topic", groupId = "email-group")
    public void fetchDetailsFromTopic(String otpDetails) {

        LOGGER.info("OTP details: "+otpDetails);

        JSONObject jsonObject = new JSONObject(otpDetails);

        String userId = jsonObject.getString("1");
        String emailId = jsonObject.getString("2");
        String otp = jsonObject.getString("3");

        System.out.println(userId + "\n" + emailId + "\n" + otp);

        String body = "Your User-ID is: " + userId + "\n" + "Your One Time Password is: " + otp;

        sendSimpleEmail(emailId, "Your User-ID and One Time Password", body);
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
