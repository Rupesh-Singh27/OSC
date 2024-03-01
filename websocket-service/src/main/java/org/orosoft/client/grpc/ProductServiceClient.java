package org.orosoft.client.grpc;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.orosoft.product.ProductRequest;
import org.orosoft.product.ProductServiceGrpc;
import org.springframework.stereotype.Component;

@Component
public class ProductServiceClient {

    @GrpcClient("product-service")
    ProductServiceGrpc.ProductServiceBlockingStub blockingStub;

    public String getProductResponse(String ping, String userId){
        ProductRequest productRequest = ProductRequest.newBuilder().setRequest(ping).setUserId(userId).build();
        return blockingStub.getProductResponseForPings(productRequest).getResponse();
    }

}
