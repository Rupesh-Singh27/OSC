package org.orosoft.userservice.serviceImpl;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.orosoft.userservice.common.HelperClass;
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

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpServiceClient otpServiceClient;

    @Autowired
    private LoginServiceClient loginServiceClient;

    @Autowired
    private KafkaProducer<String, LoginLogoutDTO> producer;

    @Autowired
    HazelcastInstance hazelcastInstance;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.name}")
    private String topic;

//    HazelcastInstance hazelcastInstance = HazelcastClient.newHazelcastClient();

    @Override
    public String registerUser(User user) {
        try{
            /*If user with same email is present it is assumed that the user is already, since emails are unique for each user*/
            boolean isUserPresent = userRepository.existsByEmail(user.getEmail());

            if(isUserPresent){
                return "User is already registered";
            }else {
                String userId = generateUserId(user.getName());
                user.setUserId(userId);

                /*Generate an OTP and send it to Kafka topic*/
                long otp = HelperClass.generateOTP();
                user.setOtp(otp);

                String message = sendDataToKafkaForEmail(user.getEmail(), otp, userId);

                /*Storing the user object into the hazelcast cache after setting the OTP in User object for future use.*/
                IMap<String, User> userCacheMap = hazelcastInstance.getMap("userObjectCache");
                userCacheMap.put(userId, user);

                if(message.equals("mail sent")){
                    return userId;
                }else{
                    return "mail not sent";
                }
            }
        }catch (Exception exception){
            throw new RegistrationException();
        }
    }

    private String generateUserId(String userName) {
        //generate userId, format-- User's first 3 letter + random 3 digits
        Random random = new Random();
        boolean doesUserExistById;
        String userId;
        do{
            String userLetter = userName.substring(0, 3).toLowerCase();
            int digits = random.nextInt(900) + 100;

            userId = userLetter + digits;

            //Checking if user already exist with same userId, if true then repeat the userId generation process.
            doesUserExistById = isUserPresentByUserId(userId);

        }while(doesUserExistById);

        return userId;
    }

    private boolean isUserPresentByUserId(String userId) {
        return userRepository.existsByUserId(userId);
    }

    private String sendDataToKafkaForEmail(String userEmail, long otp, String userId) throws InterruptedException {

        Map<Integer, String> map = new HashMap<>();
        map.put(1, userId);
        map.put(2, userEmail);
        map.put(3, String.valueOf(otp));

        //Publishing the details in kafka topic
        kafkaTemplate.send(topic, map);

        TimeUnit.SECONDS.sleep(1);

        return "mail sent";
    }

    @Override
    public String validateOtp(String userId, int otp) {
        String message = "";
        try{
            /*Fetching count of tries from cache based on userId to validate the tries*/
            IMap<String, Integer> otpCountMap = hazelcastInstance.getMap("validOTPAttemptsCount");
            int attepmts = otpCountMap.getOrDefault(userId, 0);

            /*Fetching user object was saved earlier in the cache while registering to get the OTP for validation.*/
            IMap<String, User> userObjectCache = hazelcastInstance.getMap("userObjectCache");
            LOGGER.info("User object from User's Cache: " + userObjectCache.entrySet());

            if(!userObjectCache.containsKey(userId)){
                message = "Invalid user id";
            }else{

                long fetchedOtp = userObjectCache.get(userId).getOtp();
                LOGGER.info("Fetched OTP: " + fetchedOtp);

                if(fetchedOtp == otp){
                    /*Clearing OTP and count(tries) from cache once otp consumed.*/
                    otpCountMap.clear();
                    message = "OTP matched";
                } else{
                    /*Invalid OTP hence attempts increased*/
                    otpCountMap.put(userId, ++attepmts);
                    LOGGER.info("Total attempts: " + attepmts);

                    if(attepmts == 3){
                        /*Clearing attempts cache from hazelcast once maximum attempts are exceeded.*/
                        otpCountMap.clear();
                        userObjectCache.clear();
                        message = "Tries exceeded";
                    }else{
                        message = "Invalid OTP";
                    }
                }
            }
        }catch (RuntimeException exception){
            return "Exception Occurred";
        }
        return message;
    }

    /* Preparing object by fetching object from Hazelcast Cache and setting the password, later passing that object to addUserInDB.*/
    @Override
    public String prepareObjectToSaveInDB(String userId, String password) {

        //Fetching the user, which we stored initially into the cache while registering
        IMap<String, User> userObjectCacheMap = hazelcastInstance.getMap("userObjectCache");
        User user = userObjectCacheMap.get(userId);

        user.setPassword(password);

        /*Adding user in database*/
        boolean isUserAddedSuccessfully = addUserInDB(user);

        if(!isUserAddedSuccessfully) return "user not added" ;

        userObjectCacheMap.clear();
        LOGGER.info("User added in db");
        return "user added";
    }

    /*Saving user to user table in database.*/
    public boolean addUserInDB(User userToBeSaved){

        if(userToBeSaved.getUserId().isBlank() || userToBeSaved.getUserId().isEmpty() ||
                userToBeSaved.getPassword().isBlank() || userToBeSaved.getPassword().isEmpty()
        ) return false;

        userRepository.save(userToBeSaved);
        return true;
    }

    @Override
    public String loginUser(String userId, String password, String loginDevice) {
        String message = "";
        try{
            IMap<String, Integer> loginWrongCredentialsAttempts = hazelcastInstance.getMap("wrongCredentialsCount");
            int attempts = 0;

            /*Validating if there is user with the specified userId and password in the user db.*/
            boolean isUserPresent = userRepository.existsByUserIdAndPassword(userId, password);

            if(isUserPresent){

                /*Calling grpc client for Interactive Query to check if user is already logged in with same device.*/
                boolean isLoggedIn = loginServiceClient.getLoginStatus(userId, loginDevice);
                System.out.println("Before doing the logging in process: " + isLoggedIn);

                if(!isLoggedIn){
                    /*
                    * Producing Login object and publishing into LoginLogoutTopic to login user
                    * Session-Service will get the login object and based on the object it will let the user login.
                    * */
                    String sessionId = prepareLoginObject(userId, loginDevice);
                    System.out.println("Session Id Created is: " + sessionId);

                    /*
                     * Before KStream updates the login status, GRPC call written below get there and fetch the wrong status i.e. false from KTable
                     * Hence, giving the time of 2 seconds to KStream to update the status in KTable before GRPC makes the call.
                     * */
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    boolean loginStatus = loginServiceClient.getLoginStatus(userId, loginDevice);

                    if(loginStatus){
                        System.out.println("After doing the logging in process: " + loginStatus);

                        String userName = userRepository.findByUserId(userId).get().getName();

                        /*After 1 or 2 wrong attempts user logs in correctly then the attempts must be reset.*/
                        loginWrongCredentialsAttempts.clear();
                        return sessionId + "-" + loginDevice + "_" + userName;
                    }
                }else{
                    message = "Already logged in";
                }
            }else{
                /*
                * Checking the reason for not authenticating the user, weather its coz of userId or password.
                * Because we need to send the response stating the reason of invalidating.
                * If we not found user with the provided userId that means userId is invalid.
                * */
                Optional<User> user = userRepository.findByUserId(userId);
                if(user.isEmpty()){
                    message = "User id invalid";
                }else if(!(user.get().getPassword().equals(password))) {

                    attempts = loginWrongCredentialsAttempts.getOrDefault(userId, 0);
                    loginWrongCredentialsAttempts.put(userId, ++attempts);

                    LOGGER.info("Wrong credentials: " + loginWrongCredentialsAttempts.get(userId) + " time");

                    message = "Incorrect password";
                }

                if(attempts == 3){
                    LOGGER.info("Maximum: " + attempts + " time");
                    message = "Tries exceeded";
                    loginWrongCredentialsAttempts.clear();
                }
            }
            return message;
        }catch (RuntimeException exception){
            System.out.println(exception.getMessage());
            throw new RegistrationException();
        }
    }

    private String prepareLoginObject(String userId, String loginDevice) {

        String sessionId = HelperClass.generateCustomSessionId();
        LOGGER.info("Session Id is: "+sessionId);

        LoginLogoutDTO loginObject = new LoginLogoutDTO();

        loginObject.setSessionId(sessionId);
        loginObject.setUserId(userId);
        loginObject.setDevice(loginDevice);
        loginObject.setLoginTime(new Date().toString());
        loginObject.setLogoutTime("");
        loginObject.setLoginStatus(true);

        produceLoginLogoutObjectIntoKafkaTopic(loginDevice, loginObject);
        return sessionId;
    }

    /*Logout Functionality Business Logic*/
    @Override
    public String logoutUser(String userId, String sessionIdAndDevice) {
        try{
            String[] splitedSessionIdAndDevice = sessionIdAndDevice.split("-");
            String sessionId = splitedSessionIdAndDevice[0];
            String loginDevice = splitedSessionIdAndDevice[1];

            /*
             * Producing Logout object and publishing into LoginLogoutTopic to logout user
             * Session-Service will get the Logout object and based on the object it will let the user logout.
             * */
            prepareLogoutObject(userId, sessionId, loginDevice);

            return "User logged out";
        }catch (Exception exception){
            System.out.println(exception.getMessage());
            return "Exception occurred";
        }
    }

    private void prepareLogoutObject(String userId, String sessionId, String loginDevice) {
        LoginLogoutDTO logoutObject = new LoginLogoutDTO();

        logoutObject.setUserId(userId);
        logoutObject.setSessionId(sessionId);
        logoutObject.setDevice(loginDevice);
        logoutObject.setLoginTime("");
        logoutObject.setLogoutTime(new Date().toString());
        logoutObject.setLoginStatus(false);

        produceLoginLogoutObjectIntoKafkaTopic(loginDevice, logoutObject);
    }

    private void produceLoginLogoutObjectIntoKafkaTopic(String loginDevice, LoginLogoutDTO loginLogoutObject) {
        ProducerRecord<String, LoginLogoutDTO> record =
                new ProducerRecord<>("LoginLogoutTopic", loginLogoutObject.getUserId()+"_"+ loginDevice, loginLogoutObject);

        producer.send(record);
    }

    /*Forgot Password functionality Business Logic starts from here*/
    @Override
    public String validateEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if(user.isPresent()){
            storeOTPForForgotPasswordInHazelcast(user.get().getEmail(), user.get().getUserId());
            return "Valid email";
        }else{
            return "Invalid email";
        }
    }

    /*Storing the OTP generated for Forgot Password into hazelcast cache in order to use it later for validating */
    private void storeOTPForForgotPasswordInHazelcast(String userEmail, String userID){
        try {
            IMap<String, Long> forgotPasswordOTPCache = hazelcastInstance.getMap("forgotPasswordOTPCache");

            long otp = HelperClass.generateOTP();
            sendDataToKafkaForEmail(userEmail, otp, userID);
            forgotPasswordOTPCache.put(userEmail, otp);

        } catch (InterruptedException e) {
            throw new RuntimeException("Email not sent");
        }
    }

    @Override
    public String validateOtpForForgotPassword(String email, int otpToValidate) {

        IMap<String, Long> forgotPasswordOTPCache = hazelcastInstance.getMap("forgotPasswordOTPCache");
        long otpFetchedFromCache = forgotPasswordOTPCache.get(email);

        if (otpFetchedFromCache == otpToValidate) {
            forgotPasswordOTPCache.clear();
            return "OTP matched";
        } else{
            return "Wrong OTP";
        }
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

    @PreDestroy
    public void destroy() {
        producer.close();
    }

    /*Phase 3*/
    @Override
    public String getProductsForDashboard(String userId, String sessionId) {
        return null;
    }
}
