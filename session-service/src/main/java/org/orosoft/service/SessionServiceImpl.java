package org.orosoft.service;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.orosoft.common.AppConstants;
import org.orosoft.dto.LoginLogoutDTO;
import org.orosoft.entity.SessionDetail;
import org.orosoft.login.LoginRequest;
import org.orosoft.login.LoginResponse;
import org.orosoft.login.LoginServiceGrpc;
import org.orosoft.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Properties;


@GrpcService
@Service
@Slf4j
public class SessionServiceImpl extends LoginServiceGrpc.LoginServiceImplBase {

    private final SessionRepository sessionRepository;
    private final StreamsBuilder streamsBuilder;

    private final Properties properties;
    private KafkaStreams kafkaStreams;
    public SessionServiceImpl(
            SessionRepository sessionRepository,
            StreamsBuilder streamsBuilder,
            @Qualifier("kafkaStreamsProperties") Properties properties
    ){
        this.sessionRepository = sessionRepository;
        this.streamsBuilder = streamsBuilder;
        this.properties = properties;
    }

    /* This function is responsible to indicate whether the user is Log In or Log Out. */
    @Override
    public void getLoginStatus(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {

        String userId = request.getUserId();
        String device = request.getDevice();

        String customLoginKey = userId + "_" + device;

        log.info("Custom Login Key: {}", customLoginKey);

        /*Retrieving a read-only key-value store from Kafka Streams based on the specified store name and type.*/
        ReadOnlyKeyValueStore<String, Boolean> store = kafkaStreams.store(StoreQueryParameters
                .fromNameAndType(AppConstants.KAFKA_STORE_NAME, QueryableStoreTypes.keyValueStore()));

        /*If the result is null, the right side && is not evaluated and the value of isLoggedIn will be false.*/
        boolean isLoggedIn = store.get(customLoginKey) != null && store.get(customLoginKey);

        log.info("Is user logged in?: {}", isLoggedIn);

        responseObserver.onNext(LoginResponse.newBuilder().setIsLoggedIn(isLoggedIn).build());
        responseObserver.onCompleted();
    }

    /*Implementing Kafka Streams here to fetch the data from topic and processing it. Update the KTable. Update the Session Table in Database.*/
    @EventListener(ApplicationReadyEvent.class)
    public void initializeKafkaStreamsAndProcessLoginEvents(){
        KStream<String, LoginLogoutDTO> stream = streamsBuilder.stream(AppConstants.KAFKA_TOPIC_NAME);

        /*Creating the KTable after fetching data from stream.*/
        KTable<String, Boolean> loginStatusTable =
                stream
                        .mapValues((loginLogoutDetails) -> loginLogoutDetails.getLoginStatus())
                        .groupByKey()
                        .aggregate(
                            () -> false,
                            (userId, newLoginStatus, currentLoginStatus) -> newLoginStatus,
                            Materialized.<String, Boolean, KeyValueStore<Bytes, byte[]>>as(AppConstants.KAFKA_STORE_NAME)
                                    .withValueSerde(Serdes.Boolean())
                        );

        loginStatusTable.toStream().peek((key, value) -> log.info("Login status for {} changed to {}", key, value.toString()));

        /*Calling different function to update the session table in database*/
        stream.foreach((userId, loginLogoutDetails) -> {
            System.out.println(userId + ": " + loginLogoutDetails.toString());
            updateSessionTable(loginLogoutDetails);
        });

        buildAndStartKafkaStreams();
    }

    private void buildAndStartKafkaStreams() {
        Topology topology = streamsBuilder.build();

        kafkaStreams = new KafkaStreams(topology, properties);
        kafkaStreams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(()-> kafkaStreams.close()));
    }

    /*Persisting login and logout details in session table in database*/
    @Transactional
    private void updateSessionTable(LoginLogoutDTO loginLogoutDetails) {

        /*Login status is true means user wants to log in or else user is here for logout.*/
        if(loginLogoutDetails.getLoginStatus()){

            SessionDetail sessionDetail = prepareLoginSessionDetail(loginLogoutDetails);
            sessionRepository.save(sessionDetail);
        }else{
            //user wants to log out.
            sessionRepository.updateLogoutTimeBySessionId(loginLogoutDetails.getSessionId(), loginLogoutDetails.getLogoutTime());
        }
    }

    /*Converting LoginDTO to Entity*/
    private SessionDetail prepareLoginSessionDetail(LoginLogoutDTO loginLogoutDetail) {
        SessionDetail sessionDetail = new SessionDetail();

        sessionDetail.setUserId(loginLogoutDetail.getUserId());
        sessionDetail.setSessionId(loginLogoutDetail.getSessionId());
        sessionDetail.setDevice(loginLogoutDetail.getDevice());
        sessionDetail.setLoginTime(loginLogoutDetail.getLoginTime());
        sessionDetail.setLogoutTime(loginLogoutDetail.getLogoutTime());

        return sessionDetail;
    }
}
