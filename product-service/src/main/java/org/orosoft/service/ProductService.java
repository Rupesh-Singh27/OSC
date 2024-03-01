package org.orosoft.service;


import org.orosoft.request.MTPingRequest;
import org.orosoft.response.ApiResponse;

public interface ProductService {
    ApiResponse prepareDashboard(String userId, String sessionId);

//    String userId, String ping, char categoryId, String productId, String filter
    String prepareProductResponseForPings(MTPingRequest mtPingRequest, String userId);
}
