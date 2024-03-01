package org.orosoft.userservice.serviceImpl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.orosoft.userservice.common.AppConstants;
import org.orosoft.userservice.common.HelperComponent;
import org.orosoft.userservice.dto.LoginLogoutDTO;
import org.orosoft.userservice.exception.CustomException;
import org.orosoft.userservice.grpcClient.CartProductUpdaterClient;
import org.orosoft.userservice.grpcClient.LoginServiceClient;
import org.orosoft.userservice.grpcClient.RecentViewUpdaterClient;
import org.orosoft.userservice.grpcClient.StopPeriodicServerClient;
import org.orosoft.userservice.repository.UserRepository;
import org.orosoft.userservice.response.ApiResponse;
import org.orosoft.userservice.response.DataObject;
import org.orosoft.userservice.service.LoginLogoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginLogoutServiceImpl implements LoginLogoutService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginLogoutServiceImpl.class);
    private final HazelcastInstance hazelcastInstance;
    private IMap<String, Integer> wrongPasswordAttemptsCounter;
    private final UserRepository userRepository;
    private final LoginServiceClient loginServiceClient;
    private final HelperComponent helperComponent;
    private final KafkaProducer<String, LoginLogoutDTO> kafkaProducer;
    private final RecentViewUpdaterClient recentViewUpdaterClient;
    private final CartProductUpdaterClient cartProductUpdaterClient;
    private final StopPeriodicServerClient stopPeriodicServerClient;


    /*Constructor Injection*/
    public LoginLogoutServiceImpl(
            HazelcastInstance hazelcastInstance,
            UserRepository userRepository,
            LoginServiceClient loginServiceClient,
            HelperComponent helperComponent,
            KafkaProducer<String, LoginLogoutDTO> kafkaProducer,
            StopPeriodicServerClient stopPeriodicServerClient,
            RecentViewUpdaterClient recentViewUpdaterClient,
            CartProductUpdaterClient cartProductUpdaterClient
    ){
        this.hazelcastInstance = hazelcastInstance;
        this.userRepository = userRepository;
        this.loginServiceClient = loginServiceClient;
        this.helperComponent = helperComponent;
        this.kafkaProducer = kafkaProducer;
        this.stopPeriodicServerClient = stopPeriodicServerClient;
        this.recentViewUpdaterClient = recentViewUpdaterClient;
        this.cartProductUpdaterClient = cartProductUpdaterClient;
    }

    @Override
    public ApiResponse loginUser(String userId, String password, String loginDevice) {
        try {
            wrongPasswordAttemptsCounter = hazelcastInstance.getMap(AppConstants.WRONG_PASSWORD_ATTEMPT_COUNTER_MAP);
            boolean isUserPresent = userRepository.existsByUserIdAndPassword(userId, password);

            if (isUserPresent) {
                boolean isLoggedIn = loginServiceClient.getLoginStatus(userId, loginDevice);
                LOGGER.info("Before doing the logging in process: {}", isLoggedIn);
                if (!isLoggedIn) {
                    return ApiResponse.builder().code(200).dataObject(initiateLoginProcess(userId, loginDevice)).build();
                } else {
                    return ApiResponse.builder().code(204).build(); //User is already logged in
                }
            } else {
                /*Either userId or Password is incorrect*/
                return validateUserIdAndPassword(userId);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Exception {} while logging in", exception.getMessage());
            throw new CustomException();
        }
    }

    private DataObject<String> initiateLoginProcess(String userId, String loginDevice) {
        String sessionId = helperComponent.generateCustomSessionId();
        LOGGER.info("Session Id is {}", sessionId);

        prepareLoginLogoutObject(userId, sessionId, loginDevice, true);

        String userName = userRepository.findNameByUserId(userId);

        /*After 1 or 2 wrong attempts if user logs in correctly then the attempts must be reset.*/
        wrongPasswordAttemptsCounter.remove(userId);

        return DataObject.<String>builder()
                .sessionId(sessionId + "-" + loginDevice)
                .name(userName)
                .build();
    }

    private ApiResponse validateUserIdAndPassword(String userId) {
        /*True implies that userId is correct*/
        boolean isUserPresent = userRepository.existsById(userId);

        if(isUserPresent){
            /*Password incorrect*/

            /*Counting the number of attempts for wrong password*/
            int attempts = helperComponent.attemptCounter(userId, wrongPasswordAttemptsCounter);

            LOGGER.info("Wrong Password {} time ", attempts);
            if (attempts == 3) {
                wrongPasswordAttemptsCounter.remove(userId);
                return ApiResponse.builder().code(205).build();
            }else{
                return ApiResponse.builder().code(202).build();
            }
        }else{
            /*User ID incorrect*/
            return ApiResponse.builder().code(201).build();
        }
    }

    /*Logout Business Logic*/
    @Override
    public ApiResponse logoutUser(String userId, String sessionIdAndDevice) {
        try {

            System.out.println(sessionIdAndDevice);
            String[] splitedSessionIdAndDevice = sessionIdAndDevice.split("-");

            String sessionId = splitedSessionIdAndDevice[0];
            String loginDevice = splitedSessionIdAndDevice[1];

            /*Preparing Logout object and publishing into LoginLogoutTopic, based on it Session-Service will log out user.*/
            prepareLoginLogoutObject(userId, sessionId, loginDevice, false);
            stopPeriodicServerClient.stopPeriodicCheck(userId, loginDevice);
            recentViewUpdaterClient.updateRecentViewTable(userId);
            cartProductUpdaterClient.updateCartProductsTable(userId);

            return ApiResponse.builder().code(200).build();
        } catch (Exception exception) {
            LOGGER.info("Exception {} while logging out",exception.getMessage());
            throw new CustomException(exception.getMessage());
        }
    }

    private void prepareLoginLogoutObject(String userId, String sessionId, String loginDevice, boolean isLoginRequest) {

        LoginLogoutDTO loginLogoutObject = new LoginLogoutDTO();
        loginLogoutObject.setUserId(userId);
        loginLogoutObject.setSessionId(sessionId);
        loginLogoutObject.setDevice(loginDevice);

        if(isLoginRequest){
            /*Creating a login object*/
            loginLogoutObject.setLoginTime(LocalDateTime.now().toString());
            loginLogoutObject.setLogoutTime("");
            loginLogoutObject.setLoginStatus(isLoginRequest);
        }else{
            /*Creating a logout object*/
            loginLogoutObject.setLoginTime("");
            loginLogoutObject.setLogoutTime(LocalDateTime.now().toString());
            loginLogoutObject.setLoginStatus(isLoginRequest);
        }

        produceLoginLogoutObjectIntoKafkaTopic(loginDevice, loginLogoutObject);
    }

    private void produceLoginLogoutObjectIntoKafkaTopic(String loginDevice, LoginLogoutDTO loginLogoutObject) {
        ProducerRecord<String, LoginLogoutDTO> record =
                new ProducerRecord<>(AppConstants.KAFKA_TOPIC_NAME, loginLogoutObject.getUserId() + "_" + loginDevice, loginLogoutObject);

        kafkaProducer.send(record);
        Runtime.getRuntime().addShutdownHook(new Thread(kafkaProducer::close));
    }
}
