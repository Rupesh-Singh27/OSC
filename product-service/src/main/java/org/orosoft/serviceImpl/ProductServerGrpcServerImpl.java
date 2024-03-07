package org.orosoft.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.orosoft.product.ProductRequest;
import org.orosoft.product.ProductResponse;
import org.orosoft.product.ProductServiceGrpc;
import org.orosoft.product.VoidResponse;
import org.orosoft.request.MTPingRequest;
import org.orosoft.service.ProductService;
import org.springframework.stereotype.Component;

@GrpcService
@Component
@Slf4j
public class ProductServerGrpcServerImpl extends ProductServiceGrpc.ProductServiceImplBase {

    private final ProductService productService;
    private final ProductDetailsService productDetailsService;
    private final CartOperationService cartOperationService;
    private final RecentViewUpdateService recentViewUpdateService;

    private final ObjectMapper objectMapper;

    ProductServerGrpcServerImpl(
            ProductService productService,
            ProductDetailsService productDetailsService,
            CartOperationService cartOperationService,
            RecentViewUpdateService recentViewUpdateService, ObjectMapper objectMapper
    ){
        this.productService = productService;
        this.productDetailsService = productDetailsService;
        this.cartOperationService = cartOperationService;
        this.recentViewUpdateService = recentViewUpdateService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void getProductResponseForPings(ProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        try {
            String userId = request.getUserId();
            MTPingRequest mtPingRequest = objectMapper.readValue(request.getRequest(), MTPingRequest.class);
            log.info("Request received in getProductResponseForPings {}", request.getRequest());

            String productResponseForPings = productService.prepareProductResponseForPings(mtPingRequest, userId);
            System.out.println("Response for front end: " + productResponseForPings);

            responseObserver.onNext(ProductResponse.newBuilder().setResponse(productResponseForPings).build());
            responseObserver.onCompleted();
        } catch (Exception exception) {
            responseObserver.onError(
                    Status.UNKNOWN
                            .withDescription("Oops!! something went wrong, could not process your ping.")
                            .augmentDescription(exception.getMessage())
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void updateRecentViewProductTable(ProductRequest request, StreamObserver<VoidResponse> responseObserver) {
        try{
            log.info("Request received to update recent view database table in GRPC Server");

            String userId = request.getUserId();
            recentViewUpdateService.updateRecentViewProductDatabaseTable(userId);

            responseObserver.onNext(VoidResponse.newBuilder().build());
            responseObserver.onCompleted();
        }catch (Exception exception) {
            responseObserver.onError(
                    Status.UNKNOWN
                            .withDescription("Oops!! something went wrong, could not update Recent View Table.")
                            .augmentDescription(exception.getMessage())
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void updateCartProductsTable(ProductRequest request, StreamObserver<VoidResponse> responseObserver) {
        try{
            log.info("Request received to update cart database table in GRPC Server");

            cartOperationService.updateCartProductsDatabaseTable(request.getUserId());

            responseObserver.onNext(VoidResponse.newBuilder().build());
            responseObserver.onCompleted();
        }catch (Exception exception) {
            responseObserver.onError(
                    Status.UNKNOWN
                            .withDescription("Oops!! something went wrong, could not update Cart Product Table.")
                            .augmentDescription(exception.getMessage())
                            .asRuntimeException()
            );
        }
    }
}
