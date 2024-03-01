package org.orosoft.userservice.grpcClient;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.orosoft.product.ProductRequest;
import org.orosoft.product.ProductServiceGrpc;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CartProductUpdaterClient {

    @GrpcClient("cart-updater")
    ProductServiceGrpc.ProductServiceBlockingStub blockingStub;

    public void updateCartProductsTable(String userId){
        log.info("Inside Cart Updater Client");
        blockingStub.updateCartProductsTable(ProductRequest.newBuilder().setUserId(userId).build());
    }
}
