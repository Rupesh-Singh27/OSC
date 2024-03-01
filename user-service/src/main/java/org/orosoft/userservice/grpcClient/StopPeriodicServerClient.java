package org.orosoft.userservice.grpcClient;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.orosoft.periodicstop.StopPeriodicCheckServiceGrpc;
import org.orosoft.periodicstop.StopPeriodicRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StopPeriodicServerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtpServiceClient.class);

    @GrpcClient("stop-periodic-check-service")
    private StopPeriodicCheckServiceGrpc.StopPeriodicCheckServiceBlockingStub blockingStub;

    public void stopPeriodicCheck(String userId, String device){

        LOGGER.info("Inside stop periodic check");
        blockingStub.stopPeriodicCheck(StopPeriodicRequest.newBuilder().setUserId(userId).setDevice(device).build());
    }
}
