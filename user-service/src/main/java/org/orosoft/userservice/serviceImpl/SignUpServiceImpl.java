package org.orosoft.userservice.serviceImpl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.orosoft.userservice.common.HelperComponent;
import org.orosoft.userservice.entity.User;
import org.orosoft.userservice.exception.CustomException;
import org.orosoft.userservice.repository.UserRepository;
import org.orosoft.userservice.response.ApiResponse;
import org.orosoft.userservice.response.DataObject;
import org.orosoft.userservice.service.SignUpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@Transactional
public class SignUpServiceImpl implements SignUpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignUpServiceImpl.class);
    private final UserRepository userRepository;
    private final HazelcastInstance hazelcastInstance;
    private final HelperComponent helperComponent;

    SignUpServiceImpl(
            UserRepository userRepository,
            HazelcastInstance hazelcastInstance,
            HelperComponent helperComponent
    ){
        this.userRepository = userRepository;
        this.hazelcastInstance = hazelcastInstance;
        this.helperComponent = helperComponent;
    }

    @Override
    public ApiResponse registerUser(User user) {
        try {
            /*If user with same email is present it means that the user is already, since emails are unique for each user*/
            boolean isUserPresent = userRepository.existsByEmail(user.getEmail());

            if (isUserPresent) {
                // "User is already registered";
                return ApiResponse.builder().code(30).build();
            } else {
                String userId = generateUserId(user.getName());
                user.setUserId(userId);

                /*Generate an OTP and send it to Kafka topic*/
                long otp = helperComponent.generateOTP();
                user.setOtp(otp);

                String message = helperComponent.sendDataToKafkaForEmail(user.getEmail(), otp, userId);

                /*Storing the user object into the hazelcast cache after setting the OTP in User object for future use.*/
                IMap<String, User> userCacheMap = hazelcastInstance.getMap("userObjectCache");
                userCacheMap.put(userId, user);

                if (message.equals("mail sent")) {
                    //return userId;
                    DataObject<String> userIdDataObject = DataObject.<String>builder()
                            .userId(userId)
                            .build();

                    return ApiResponse.builder()
                            .code(200)
                            .dataObject(userIdDataObject)
                            .build();
                } else {
                   // "mail not sent";
                    return ApiResponse.builder().code(220).build();
                }
            }
        } catch (Exception exception) {
            throw new CustomException();
        }
    }

    private String generateUserId(String userName) {
        //generate userId, format-- User's first 3 letter + random 3 digits
        Random random = new Random();
        boolean doesUserExistById;
        String userId;
        do {
            String userLetter = userName.substring(0, 3).toLowerCase();
            int digits = random.nextInt(900) + 100;

            userId = userLetter + digits;

            /*Validating if user already exist with same userId, if true then repeat the userId generation process.*/
            doesUserExistById = isUserPresentByUserId(userId);

        } while (doesUserExistById);
        return userId;
    }

    private boolean isUserPresentByUserId(String userId) {
        return userRepository.existsById(userId);
    }

    /* Preparing object by fetching object from Hazelcast Cache and setting the password, later passing that object to addUserInDB.*/
    @Override
    public ApiResponse validateOtp(String userId, long otp) {
        try {
//            IMap<String, Integer> wrongOTPAttemptsCounter = hazelcastInstance.getMap("wrongOTPAttemptsCounterMap");

            /*Fetching user object was saved earlier in the cache while registering to get the OTP for validation.*/
            IMap<String, User> userObjectCache = hazelcastInstance.getMap("userObjectCache");
            LOGGER.info("User object from User's Cache: " + userObjectCache.entrySet());

            if (!userObjectCache.containsKey(userId)) {
                //"Invalid user id";
                return ApiResponse.builder().code(1999).build();
            } else {
                /*Check OTP Validation*/
                return checkOTPValidation(userId, otp, userObjectCache);
            }
        } catch (RuntimeException exception) {
            LOGGER.info("Exception while validation OTP: " + exception.getMessage());
            throw new CustomException();
        }
    }

    private ApiResponse checkOTPValidation(String userId, long otp, IMap<String, User> userObjectCache) {
        IMap<String, Integer> wrongOTPAttemptsCounter = hazelcastInstance.getMap("wrongOTPAttemptsCounterMap");

        long fetchedOtp = userObjectCache.get(userId).getOtp();

        if (fetchedOtp == otp) {
            /*Clearing OTP and count(tries) from cache once otp consumed.*/
            wrongOTPAttemptsCounter.remove(userId);
            //"OTP matched";
            return ApiResponse.builder().code(500).build();
        } else {
            /*Invalid OTP hence attempts increased*/
            int attempts = helperComponent.attemptCounter(userId, wrongOTPAttemptsCounter);

            LOGGER.info("Wrong OTP: " + attempts + " time");

            if (attempts == 3) {
                /*Clearing attempts cache from hazelcast once maximum attempts are exceeded.*/
                wrongOTPAttemptsCounter.remove(userId);
                userObjectCache.remove(userId);
                return ApiResponse.builder().code(301).build();
            } else {
                //"Invalid OTP";
                return ApiResponse.builder().code(502).build();
            }
        }
    }

    @Override
    public ApiResponse prepareObjectToSaveInDB(String userId, String password) {

        /*Fetching the user, which we stored initially into the cache while registering*/
        IMap<String, User> userObjectCacheMap = hazelcastInstance.getMap("userObjectCache");
        User user = userObjectCacheMap.get(userId);

        user.setPassword(password);

        /*Adding user in database*/
        boolean isUserAddedSuccessfully = addUserInDB(user);

        if (!isUserAddedSuccessfully) return ApiResponse.builder().code(0).build();

        userObjectCacheMap.remove(userId);
        LOGGER.info("User added in db");
        return ApiResponse.builder().code(200).build();
    }

    /*Saving user to user table in database.*/
    public boolean addUserInDB(User userToBeSaved) {

        if (userToBeSaved.getUserId().isBlank() || userToBeSaved.getUserId().isEmpty() ||
                userToBeSaved.getPassword().isBlank() || userToBeSaved.getPassword().isEmpty()
        ) return false;

        userRepository.save(userToBeSaved);
        return true;
    }
}
