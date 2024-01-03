package org.orosoft.service;

import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.orosoft.dto.LoginLogoutDTO;
import org.orosoft.entity.SessionDetail;
import org.orosoft.login.LoginRequest;
import org.orosoft.login.LoginResponse;
import org.orosoft.login.LoginServiceGrpc;
import org.orosoft.repository.SessionRepository;
import org.orosoft.serdes.LoginLogoutSerdes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;


@GrpcService
@Service
public class SessionServiceImpl extends LoginServiceGrpc.LoginServiceImplBase {

    @Autowired
    private SessionRepository sessionRepository;

    KafkaStreams kafkaStreams;

    @Override
    public void loginStatus(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {

        String userId = request.getUserId();
        String device = request.getDevice();

        String customLoginKey = userId + "_" + device;

        System.out.println("Custom Login Key: " + customLoginKey);

        //Interactive query to check login status of the user in KTable
        ReadOnlyKeyValueStore<String, Boolean> store = kafkaStreams.store(StoreQueryParameters
                .fromNameAndType("login-status-store", QueryableStoreTypes.keyValueStore()));

        // Wait until the application is in the RUNNING state
        /*while (kafkaStreams.state() != KafkaStreams.State.RUNNING) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }*/

        Boolean isLoggedIn = store.get(userId + "_" + device);
        System.out.println("Is user logged in?: " + isLoggedIn);

        //if the user is logging in for the first time, isLoggedIn object will be null.
        responseObserver.onNext(LoginResponse.newBuilder().setIsLoggedIn(Objects.requireNonNullElse(isLoggedIn, false)).build());
        responseObserver.onCompleted();
    }

    @PostConstruct
    public void updateLoginDetails(){

        /*Kafka Stream to fetch the data from topic and manipulating it*/
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "login-details");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, LoginLogoutSerdes.class.getName());

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, LoginLogoutDTO> stream = builder.stream("LoginLogoutTopic");

        //Creating the KTable after fetching data from stream.
        KTable<String, Boolean> loginStatusTable =
                stream
                        .mapValues((loginLogoutDetails) -> loginLogoutDetails.getLoginStatus())
                        .groupByKey(Grouped.with(Serdes.String(), Serdes.Boolean()))
                        .aggregate(
                            () -> false,
                            (userId, newLoginStatus, currentLoginStatus) -> newLoginStatus,
                            Materialized.<String, Boolean, KeyValueStore<Bytes, byte[]>>as("login-status-store")
                                    .withValueSerde(Serdes.Boolean())
                        );

        loginStatusTable.toStream().foreach((key, value) -> System.out.println(key + ": " + value.toString()));

        //Updating the database
        stream.foreach((userId, loginLogoutDetails) -> {
            System.out.println(userId + ": " + loginLogoutDetails.toString());
            updatingSessionTable(loginLogoutDetails);
        });

        Topology topology = builder.build();

        kafkaStreams = new KafkaStreams(topology, props);

        System.out.println("Kafka Streams is: " + kafkaStreams.state().toString());

        kafkaStreams.start();

        System.out.println("Kafka Streams is: " + kafkaStreams.state().toString());

        Runtime.getRuntime().addShutdownHook(new Thread(()-> kafkaStreams.close()));
    }

    private void updatingSessionTable(LoginLogoutDTO loginLogoutDetails) {

        //Login time is not empty means user is here to login else here for logout.
        if(!loginLogoutDetails.getLoginTime().isEmpty()){

            //this will indicate if user is here for first time or nth time.
            Optional<SessionDetail> sessionDetailFromRepo = sessionRepository.findById(loginLogoutDetails.getSessionId());

            //user is here for nth time, hence just updating login time
            if(sessionDetailFromRepo.isPresent()){
                SessionDetail loginSessionDetail = updateLoginDetail(loginLogoutDetails, sessionDetailFromRepo);

                sessionRepository.save(loginSessionDetail);
            }else{
                //user is here for first time, hence add the data as it is.
                SessionDetail sessionDetail = new SessionDetail();

                sessionDetail.setUserId(loginLogoutDetails.getUserId());
                sessionDetail.setSessionId(loginLogoutDetails.getSessionId());
                sessionDetail.setDevice(loginLogoutDetails.getDevice());
                sessionDetail.setLoginTime(loginLogoutDetails.getLoginTime());
                sessionDetail.setLogoutTime(loginLogoutDetails.getLogoutTime());

                sessionRepository.save(sessionDetail);
            }

        }else{
            //user wants to logout
            SessionDetail sessionDetailFromRepo = sessionRepository.findById(loginLogoutDetails.getSessionId()).get();

            SessionDetail logoutSessionDetail = updateLogoutDetail(loginLogoutDetails, sessionDetailFromRepo);

            sessionRepository.save(logoutSessionDetail);
        }
    }

    private static SessionDetail updateLoginDetail(LoginLogoutDTO loginLogoutDetails, Optional<SessionDetail> sessionDetailFromRepo) {
        SessionDetail sessionDetail = new SessionDetail();

        //keeping rest of the data as it is just updating the login time, session id.
        sessionDetail.setUserId(sessionDetailFromRepo.get().getUserId());
        sessionDetail.setSessionId(loginLogoutDetails.getSessionId()); //updating the session of user.
        sessionDetail.setDevice(loginLogoutDetails.getDevice()); //updating the device from which user is logging in.
        sessionDetail.setLoginTime(loginLogoutDetails.getLoginTime()); //updating the login time of user.
        sessionDetail.setLogoutTime(sessionDetailFromRepo.get().getLogoutTime());
        sessionDetail.setLoginStatus(sessionDetailFromRepo.get().getLoginStatus());

        return sessionDetail;
    }

    private static SessionDetail updateLogoutDetail(LoginLogoutDTO loginLogoutDetails, SessionDetail sessionDetailFromRepo) {
        SessionDetail sessionDetail = new SessionDetail();

        //keeping rest of the data as it is just updating the logout time.
        sessionDetail.setUserId(sessionDetailFromRepo.getUserId());
        sessionDetail.setSessionId(sessionDetailFromRepo.getSessionId());
        sessionDetail.setDevice(sessionDetailFromRepo.getDevice());
        sessionDetail.setLoginTime(sessionDetailFromRepo.getLoginTime());
        sessionDetail.setLogoutTime(loginLogoutDetails.getLogoutTime()); //updating the logout time.
        sessionDetail.setLoginStatus(sessionDetailFromRepo.getLoginStatus());
        
        return sessionDetail;
    }
}
