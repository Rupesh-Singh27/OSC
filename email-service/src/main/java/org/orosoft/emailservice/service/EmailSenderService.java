package org.orosoft.emailservice.service;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.shaded.org.json.JSONObject;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.orosoft.otp.OtpRequest;
import org.orosoft.otp.OtpResponse;
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

    HazelcastInstance hazelcastClient = HazelcastClient.newHazelcastClient();

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

        //Storing OTP in hazelcast -- format: userID(key) -> OTP(value)
        IMap<String, String> otpCacheMap = hazelcastClient.getMap("otpCache");
        otpCacheMap.put(userId, otp);

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

    /***
     * <p>Validating the OTP sent by the user and the OTP which is store in cache, the same OTP which we sent as a mail, if they both are same or not.</p>
     *
     * @param request The object we will be getting userId and OTP need to be validated.
     * @param responseObserver The object we need to sent based on the correctness of the OTP.
     *
     * ***/
    @Override
    public void sendOtp(OtpRequest request, StreamObserver<OtpResponse> responseObserver) {

        try{
            String userId = request.getUserId();
            int otp = request.getOtp();

            /*Fetching count of tries from cache based on userId to validate the tries*/
            IMap<String, Integer> otpCountMap = hazelcastClient.getMap("otpCount");
            Integer tries = otpCountMap.getOrDefault(userId, 1);
            LOGGER.info("Total tries: " + tries);

            /*Fetching OTP from cache saved earlier in the cache, based on userId to validate the OTP*/
            IMap<String, String> otpCacheMap = hazelcastClient.getMap("otpCache");
            LOGGER.info(otpCacheMap.entrySet().toString());

            if(!otpCacheMap.containsKey(userId)){
                responseObserver.onNext(OtpResponse.newBuilder().setResponse("Invalid user id").build());
            }else{
                int fetchedOtp = Integer.parseInt(otpCacheMap.getOrDefault(userId, "-1"));
                LOGGER.info("Fetched OTP: " + fetchedOtp);

                if(fetchedOtp == otp){
                    responseObserver.onNext(OtpResponse.newBuilder().setResponse("OTP matched").build());

                    //Clearing OTP and count(tries) from cache once otp consumed.
                    otpCacheMap.remove(userId);
                    otpCountMap.remove(userId);
                } else{
                    if(tries == 3){
                        responseObserver.onNext(OtpResponse.newBuilder().setResponse("Tries exceeded").build());

                        //Clearing OTP and count(tries) from cache once maximum attempt exceed.
                        otpCacheMap.remove(userId);
                        otpCountMap.remove(userId);
                    }else{
                        //1st or 2nd attempt failed
                        responseObserver.onNext(OtpResponse.newBuilder().setResponse("Invalid OTP").build());
                        otpCountMap.put(userId, ++tries);
                    }
                }
            }
            responseObserver.onCompleted();
        }catch (RuntimeException exception){
            responseObserver.onNext(OtpResponse.newBuilder().setResponse("Exception Occurred").build());
            responseObserver.onCompleted();
        }
    }
}
