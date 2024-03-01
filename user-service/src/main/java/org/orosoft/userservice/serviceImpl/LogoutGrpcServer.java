package org.orosoft.userservice.serviceImpl;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.orosoft.logout.LogoutRequest;
import org.orosoft.logout.LogoutResponse;
import org.orosoft.logout.LogoutServiceGrpc;
import org.orosoft.userservice.response.ApiResponse;
import org.orosoft.userservice.service.LoginLogoutService;

@GrpcService
public class LogoutGrpcServer extends LogoutServiceGrpc.LogoutServiceImplBase {

    private final LoginLogoutService loginLogoutService;

    public LogoutGrpcServer(LoginLogoutService loginLogoutService){
        this.loginLogoutService = loginLogoutService;
    }
    @Override
    public void logoutUser(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {

        String userId = request.getUserId();
        String sessionId = request.getSessionId();
//        String device = request.getDevice();

        System.out.println("Session ID is + \"-\" + device" + sessionId);
        ApiResponse apiResponse = loginLogoutService.logoutUser(userId, sessionId);

        if(apiResponse.getCode() == 200){
            responseObserver.onNext(LogoutResponse.newBuilder().setIsLoggedOut(true).build());
        }else{
            responseObserver.onNext(LogoutResponse.newBuilder().setIsLoggedOut(false).build());
        }
        responseObserver.onCompleted();
    }
}
