package org.orosoft.controller;


import org.orosoft.request.DashboardRequest;
import org.orosoft.response.ApiResponse;
import org.orosoft.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "http://localhost:3000")
public class ProductController {

    private final ProductService productService;

    ProductController(ProductService productService){
        this.productService = productService;
    }

    @PostMapping("/dashboard")
    public ResponseEntity<ApiResponse> getProductForDashBoard(@RequestBody DashboardRequest dashboardRequest){

        ApiResponse productResponse = productService.prepareDashboard(dashboardRequest.getUserId(), dashboardRequest.getSessionId());

        return ResponseEntity.ok(productResponse);
    }
}
