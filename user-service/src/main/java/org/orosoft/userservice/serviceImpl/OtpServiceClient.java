package org.orosoft.userservice.serviceImpl;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.orosoft.otp.OtpRequest;
import org.orosoft.otp.OtpResponse;
import org.orosoft.otp.OtpServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OtpServiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtpServiceClient.class);

    @GrpcClient("otp-service")
    private OtpServiceGrpc.OtpServiceBlockingStub otpServiceBlockingStub;


    /***
     * <p>Makes the GRPC call to another microservice's in order to validate the OTP</p>
     *
     * @param otp OTP which needs to be validated
     * @param userId userId of the user
     * ***/
    public String sendOtp(String userId, int otp) {

         OtpRequest otpRequest = OtpRequest.newBuilder().setUserId(userId).setOtp(otp).build();
         OtpResponse otpResponse = this.otpServiceBlockingStub.sendOtp(otpRequest);
         return otpResponse.getResponse();
    }
}
