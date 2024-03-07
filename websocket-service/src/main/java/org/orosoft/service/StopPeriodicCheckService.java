package org.orosoft.service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.orosoft.hazelcastmap.TaskIdMapOperationService;
import org.orosoft.periodicstop.StopPeriodicCheckServiceGrpc;
import org.orosoft.periodicstop.StopPeriodicRequest;
import org.orosoft.periodicstop.StopPeriodicResponse;
import org.orosoft.websocket.WebSocketServer;

@GrpcService
public class StopPeriodicCheckService extends StopPeriodicCheckServiceGrpc.StopPeriodicCheckServiceImplBase {
    private final WebSocketServer webSocketServer;
    private final TaskIdMapOperationService taskIdMapOperationService;
 /*   private final HazelcastInstance hazelcastInstance;
    private IMap<String, Long> taskIdMap;*/

    StopPeriodicCheckService(
            WebSocketServer webSocketServer,
            TaskIdMapOperationService taskIdMapOperationService
//            HazelcastInstance hazelcastInstance
    ){
        this.webSocketServer = webSocketServer;
        this.taskIdMapOperationService = taskIdMapOperationService;
//        this.hazelcastInstance = hazelcastInstance;
    }

 /*  @PostConstruct
    public void initHazelcastMap(){
        taskIdMap = hazelcastInstance.getMap(AppConstants.TASK_ID_MAP);
    }*/

    @Override
    public void stopPeriodicCheck(StopPeriodicRequest request, StreamObserver<StopPeriodicResponse> responseObserver) {
        try {
            String userId = request.getUserId();
            String device = request.getDevice();

            String customWebsocketId = userId + "-" + device;

//        long taskId = taskIdMap.getOrDefault(customWebsocketId, 0L);
            long taskId = taskIdMapOperationService.getValueForKeyFromMap(customWebsocketId);
            webSocketServer.getVertx().cancelTimer(taskId);

            System.out.println(customWebsocketId + " " + taskId);
            System.out.println("Stopped");

            responseObserver.onNext(StopPeriodicResponse.newBuilder().build());
            responseObserver.onCompleted();
        }catch (Exception exception) {
            responseObserver.onError(
                    Status.UNKNOWN
                            .withDescription("Oops!! something went wrong, could not stop the Periodic Check.")
                            .augmentDescription(exception.getMessage())
                            .asRuntimeException()
            );
        }
//        responseObserver.onError(new Error("Oops!! something went wrong, could not stop the Periodic Check."));
    }
}
