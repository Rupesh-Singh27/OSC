package org.orosoft.userservice.grpcClient;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.orosoft.product.ProductRequest;
import org.orosoft.product.ProductServiceGrpc;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RecentViewUpdaterClient {

    @GrpcClient("recent-view-updater")
    ProductServiceGrpc.ProductServiceBlockingStub blockingStub;

    public void updateRecentViewTable(String userId){
        log.info("Inside Recent View Updater Client");
        blockingStub.updateRecentViewProductTable(ProductRequest.newBuilder().setUserId(userId).build());
    }
}
