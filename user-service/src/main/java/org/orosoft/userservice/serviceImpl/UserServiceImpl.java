package org.orosoft.userservice.serviceImpl;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpSession;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.orosoft.userservice.dto.LoginLogoutDTO;
import org.orosoft.userservice.entity.User;
import org.orosoft.userservice.exception.RegistrationException;
import org.orosoft.userservice.repository.UserRepository;
import org.orosoft.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private OtpServiceClient otpServiceClient;

    @Autowired
    private LoginServiceClient loginServiceClient;

    HazelcastInstance hazelcastInstance = HazelcastClient.newHazelcastClient();

    @Value("${spring.kafka.topic.name}")
    private String topic;

    @Override
    public String registerUser(User user) {
        try{
            //checking if email is present or not.
            Optional<User> userFromDB = userRepository.findByEmail(user.getEmail());

            //if user is present -- email is already registered else email is not registered
            if(userFromDB.isPresent()){
                return "User is already registered";
            } else {
                //generate userId, format-- User's first 3 letter + random 3 digits
                Random random = new Random();
                Optional<User> userById;
                String userId;

                do{
                    String userLetter = user.getName().substring(0, 3).toLowerCase();
                    int digits = random.nextInt(900) + 100;

                    userId = userLetter + digits;

                    //Checking if user already exist with same userId, if yes then repeat the userId generation process.
                    userById = userRepository.findByUserId(userId);

                }while(userById.isPresent());

                //Generate an OTP and send it to Kafka topic
                String message = generateOTP(user, random, userId);

                user.setUserId(userId);

                //Storing the user object into the hazelcast cache for future use.
                LOGGER.info("User Object in register method: " + user.toString());
                IMap<String, User> userCacheMap = hazelcastInstance.getMap("userCache");
                userCacheMap.put(userId, user);

                if(message.equalsIgnoreCase("mail sent")){
                    return userId;
                }else{
                    return "mail not sent";
                }
            }
        }catch (Exception exception){
            throw new RegistrationException();
        }
    }

    private String generateOTP(User user, Random random, String userId) throws InterruptedException {
        Long otp = random.nextLong(900000) + 100000;

        System.out.println("OTP Generated: " + otp);

        Map<Integer, String> map = new HashMap<>();
        map.put(1, userId);
        map.put(2, user.getEmail());
        map.put(3, otp.toString());

        //Publishing the details in kafka topic
        kafkaTemplate.send(topic, map);

        Thread.sleep(1000);

        return "mail sent";
    }

    @Override
    public String validateOtp(String userId, int otp) {
        return otpServiceClient.sendOtp(userId, otp);
    }

    @Override
    public String addUserInDB(String userId, String password) {

        //Fetching the user, which we stored initially into the cache while registering
        IMap<String, User> userCacheMap = hazelcastInstance.getMap("userCache");
        User user = userCacheMap.get(userId);

        user.setPassword(password);
        userRepository.save(user);

        userCacheMap.remove(userId);

        LOGGER.info("User added in db");
        return "user added";
    }

    @Override
    public String loginUser(String userId, String password, String loginDevice, HttpSession session) {
        try{
            //native kafka producer properties
            Properties props = new Properties();
            props.put("bootstrap.servers", "localhost:9092");
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer", "org.orosoft.userservice.serdes.LoginLogoutDTOSerializer");

            IMap<String, Integer> loginCache = hazelcastInstance.getMap("loginCache");
            int tries = loginCache.getOrDefault(userId, 1);
            LOGGER.info("You have attempted " + tries + " time");

            //Validating if there is user with the specified userId and password in the user db.
            Optional<User> legitUser = userRepository.findByUserIdAndPassword(userId, password);

            if(legitUser.isPresent()){

                //calling grpc client of login service to check if user is already logged in with same device
                Boolean isLoggedIn = loginServiceClient.getLoginStatus(userId, loginDevice);

                if(!isLoggedIn){
                    KafkaProducer<String, LoginLogoutDTO> producer = new KafkaProducer<String, LoginLogoutDTO>(props);

                    String sessionId = session.getId();
                    LOGGER.info("Session Id is: "+sessionId);

                    LoginLogoutDTO loginObject = new LoginLogoutDTO();

                    loginObject.setSessionId(sessionId);
                    loginObject.setUserId(userId);
                    loginObject.setDevice(loginDevice);
                    loginObject.setLoginTime(new Date().toString());
                    loginObject.setLogoutTime("");
                    loginObject.setLoginStatus(true);

                    ProducerRecord<String, LoginLogoutDTO> record =
                            new ProducerRecord<>("LoginLogoutTopic", loginObject.getUserId()+"_"+loginDevice, loginObject);

                    producer.send(record);
                    producer.close();

                    Thread.sleep(2000);

                    String userName = userRepository.findByUserId(userId).get().getName();
                    return sessionId + "-" + loginDevice + "_" + userName;
                }else{
                    return "Already logged in";
                }

            }else{
                if(tries == 3){
                    loginCache.remove(userId);
                    LOGGER.info("Maximum: " + tries + " time");
                    return "Tries exceeded";
                }else{
                    //checking the reason for not authenticating the user, weather its coz of userId or password.
                    Optional<User> user = userRepository.findByUserId(userId);
                    LOGGER.info("Wrong credentials: " + tries + " time");
                    loginCache.put(userId, ++tries);
                    if(user.isEmpty()){
                        return "User id invalid";
                    }else if(!(user.get().getPassword().equals(password))) {
                        return "Incorrect password";
                    }
                }
            }
            return "";
        }catch (RuntimeException exception){
            throw new RegistrationException();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String logoutUser(String userId, String sessionIdAndDevice, HttpSession session) {
        try{

            String[] splitedSessionIdAndDevice = sessionIdAndDevice.split("-");
            String sessionId = splitedSessionIdAndDevice[0];
            String loginDevice = splitedSessionIdAndDevice[1];

            LoginLogoutDTO logoutObject = new LoginLogoutDTO();

            logoutObject.setUserId(userId);
            logoutObject.setSessionId(sessionId);
            logoutObject.setDevice(loginDevice);
            logoutObject.setLoginTime("");
            logoutObject.setLogoutTime(new Date().toString());
            logoutObject.setLoginStatus(false);

            /*Kafka Producer Configuration*/
            Properties props = new Properties();
            props.put("bootstrap.servers", "localhost:9092");
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer", "org.orosoft.userservice.serdes.LoginLogoutDTOSerializer");

            KafkaProducer<String, LoginLogoutDTO> producer = new KafkaProducer<String, LoginLogoutDTO>(props);

            ProducerRecord<String, LoginLogoutDTO> record =
                    new ProducerRecord<>("LoginLogoutTopic", logoutObject.getUserId() + "_" + logoutObject.getDevice(), logoutObject);

            producer.send(record);
            producer.close();

            Thread.sleep(2000);

            session.invalidate();
            return "User logged out";
        }catch (Exception exception){
            return "Exception occurred";
        }
    }

    @Override
    public String validateEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        try{
            if(user.isPresent()){
                this.generateOTP(user.get(), new Random(), user.get().getUserId());
                return "Valid email";
            }else{
                return "Invalid email";
            }
        }catch (InterruptedException exception){
            return "Exception occurred while sending email";
        }
    }

    @Override
    public String validateOtpForForgotPassword(String email, int otp) {
        String userId = userRepository.findByEmail(email).get().getUserId();
        return this.validateOtp(userId, otp);
    }

    @Override
    public String updatePassword(String email, String newPassword) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if(userOptional.isPresent()){
            User user = userOptional.get();
            userOptional.get().setPassword(newPassword);
            userRepository.save(user);
            return "Password updated";
        }else{
            return "Invalid user";
        }
    }
}
