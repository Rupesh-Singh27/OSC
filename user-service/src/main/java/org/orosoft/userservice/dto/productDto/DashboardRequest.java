package org.orosoft.userservice.dto.productDto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardRequest {

    private String userId;
    private String sessionId;
}
