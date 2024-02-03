package org.orosoft.serverImpl;

import io.grpc.stub.StreamObserver;
import org.orosoft.dto.ProductDto;
import org.orosoft.product.ProductRequest;
import org.orosoft.product.ProductResponse;
import org.orosoft.product.ProductServiceGrpc;
import org.orosoft.repository.ProductRepository;
import org.orosoft.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl extends ProductServiceGrpc.ProductServiceImplBase implements ProductService{

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void getProductForDashboard(ProductRequest request, StreamObserver<ProductResponse> responseObserver) {

        Map<Character, Map<String, ProductDto>> tempDatabase = new HashMap<>();

        List<ProductDto> productList = productRepository.findAll();

        Map<String, ProductDto> productDtoMap = productList.stream().collect(Collectors.toMap(ProductDto::getProductId, Function.identity()));

    }
}
