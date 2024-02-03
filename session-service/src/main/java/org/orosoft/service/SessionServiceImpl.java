package org.orosoft.service;

import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.orosoft.common.PropsSingletonClass;
import org.orosoft.dto.LoginLogoutDTO;
import org.orosoft.entity.SessionDetail;
import org.orosoft.login.LoginRequest;
import org.orosoft.login.LoginResponse;
import org.orosoft.login.LoginServiceGrpc;
import org.orosoft.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;


@GrpcService
@Service
public class SessionServiceImpl extends LoginServiceGrpc.LoginServiceImplBase {

    @Autowired
    private SessionRepository sessionRepository;

    KafkaStreams kafkaStreams;

    /* This function is responsible to indicate whether the user is Log In or Log Out. */
    @Override
    public void getLoginStatus(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {

        String userId = request.getUserId();
        String device = request.getDevice();

        String customLoginKey = userId + "_" + device;

        System.out.println("Custom Login Key: " + customLoginKey);

        /*
        * Interactive query to check login status of the user in KTable
        * */
        ReadOnlyKeyValueStore<String, Boolean> store = kafkaStreams.store(StoreQueryParameters
                .fromNameAndType("login-status-store", QueryableStoreTypes.keyValueStore()));

        /*TODO: Check why Wrapper was written instead of primitive boolean*/
        boolean isLoggedIn = false;
        if(store.get(customLoginKey) != null &&  store.get(customLoginKey)) isLoggedIn = true;

        System.out.println("Is user logged in?: " + isLoggedIn);

        /*
        * If the user is logging in for the first time, isLoggedIn object will be null hence the default value will be false i.e. Log out.
        * */
        responseObserver.onNext(LoginResponse.newBuilder().setIsLoggedIn(isLoggedIn).build());
        responseObserver.onCompleted();
    }

    /*
    * Implementing Kafka Streams here to fetch the data from topic and processing it.
    *
    * Responsibilities:
    * Update the KTable.
    * Update the Session Table in Database.
    * */
    @PostConstruct
    public void updateLoginDetails(){
        Properties props = PropsSingletonClass.getPropsSingletonClassInstance().getPropertiesClassInstance();

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, LoginLogoutDTO> stream = builder.stream("LoginLogoutTopic");

        /*
        * Creating the KTable after fetching data from stream.
        * */
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

        loginStatusTable.toStream().foreach((key, value) -> System.out.println("Login status for " + key + " changed to " + value.toString()));

        /*
        * Calling different function to update the session table in database
        * */
        /*
        * TODO: Combine this stream operation with above stream operation and change the method name from updating to updateSessionTable
        * Cannot be combined, since foreach is a void method doesn't return anything a so couldn't be used as first operation and
        * later the data is mapped object to boolean hence cannot be used later as well
        * */
        stream.foreach((userId, loginLogoutDetails) -> {
            System.out.println(userId + ": " + loginLogoutDetails.toString());
            updateSessionTable(loginLogoutDetails);
        });

        Topology topology = builder.build();

        kafkaStreams = new KafkaStreams(topology, props);

        System.out.println("Kafka Streams is: " + kafkaStreams.state().toString());

        kafkaStreams.start();

        System.out.println("Kafka Streams is: " + kafkaStreams.state().toString());

        Runtime.getRuntime().addShutdownHook(new Thread(()-> kafkaStreams.close()));
    }

    /*Persisting login and logout details in session table in database*/
    private void updateSessionTable(LoginLogoutDTO loginLogoutDetails) {

        /*
        * Login time is not empty means user is here for login or else user is here for logout.
        * */
        if(!loginLogoutDetails.getLoginTime().isEmpty()){

            SessionDetail sessionDetail = updateLoginDetail(loginLogoutDetails);
            sessionRepository.save(sessionDetail);
        }else{
            //user wants to logout.
            Optional<SessionDetail> sessionDetailFromRepo = sessionRepository.findById(loginLogoutDetails.getSessionId());

            if(sessionDetailFromRepo.isPresent() && sessionDetailFromRepo.get().getLogoutTime().isEmpty()){
                SessionDetail logoutSessionDetail = updateLogoutDetail(loginLogoutDetails, sessionDetailFromRepo.get());

                System.out.println("Logout Object: " + logoutSessionDetail);
                sessionRepository.save(logoutSessionDetail);
            }else{
                System.out.println("User is already logged out!!!");
            }
        }
    }

    /*Preparing Session Detail Object for login to put it in Session Table in Database*/
    private static SessionDetail updateLoginDetail(LoginLogoutDTO loginLogoutDetails) {
        SessionDetail sessionDetail = new SessionDetail();

        sessionDetail.setUserId(loginLogoutDetails.getUserId());
        sessionDetail.setSessionId(loginLogoutDetails.getSessionId());
        sessionDetail.setDevice(loginLogoutDetails.getDevice());
        sessionDetail.setLoginTime(loginLogoutDetails.getLoginTime());
        sessionDetail.setLogoutTime(loginLogoutDetails.getLogoutTime());

        return sessionDetail;
    }

    /*Preparing Session Detail Object for logout to put it in Session Table in Database*/
    private static SessionDetail updateLogoutDetail(LoginLogoutDTO loginLogoutDetails, SessionDetail sessionDetailFromRepo) {
        SessionDetail sessionDetail = new SessionDetail();

        //keeping rest of the data as it is just updating the logout time.
        sessionDetail.setUserId(sessionDetailFromRepo.getUserId());
        sessionDetail.setSessionId(sessionDetailFromRepo.getSessionId());
        sessionDetail.setDevice(sessionDetailFromRepo.getDevice());
        sessionDetail.setLoginTime(sessionDetailFromRepo.getLoginTime());
        sessionDetail.setLogoutTime(loginLogoutDetails.getLogoutTime()); //updating just the logout time.

        return sessionDetail;
    }
}
