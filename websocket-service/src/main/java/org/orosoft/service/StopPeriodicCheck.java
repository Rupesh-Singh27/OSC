package org.orosoft.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import net.devh.boot.grpc.server.service.GrpcService;
import org.orosoft.common.AppConstants;
import org.orosoft.periodicstop.StopPeriodicCheckServiceGrpc;
import org.orosoft.periodicstop.StopPeriodicRequest;
import org.orosoft.periodicstop.StopPeriodicResponse;
import org.orosoft.websocket.WebSocketServer;

@GrpcService
public class StopPeriodicCheck extends StopPeriodicCheckServiceGrpc.StopPeriodicCheckServiceImplBase {
    private final WebSocketServer webSocketServer;
    private final HazelcastInstance hazelcastInstance;
    private IMap<String, Long> taskIdMap;

    StopPeriodicCheck(
            WebSocketServer webSocketServer,
            HazelcastInstance hazelcastInstance
    ){
        this.webSocketServer = webSocketServer;
        this.hazelcastInstance = hazelcastInstance;
    }

    @PostConstruct
    public void initHazelcastMap(){
        taskIdMap = hazelcastInstance.getMap(AppConstants.TASK_ID_MAP);
    }

    @Override
    public void stopPeriodicCheck(StopPeriodicRequest request, StreamObserver<StopPeriodicResponse> responseObserver) {

        String userId = request.getUserId();
        String device = request.getDevice();
        String customWebsocketId = userId+"-"+device;

        long taskId = taskIdMap.getOrDefault(customWebsocketId, 0L);
        webSocketServer.getVertx().cancelTimer(taskId);

        System.out.println(customWebsocketId + " " + taskId);
        System.out.println("Stopped");

        responseObserver.onNext(StopPeriodicResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}
