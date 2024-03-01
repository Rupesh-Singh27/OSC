package org.orosoft.userservice.dto.loginLogoutDto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {

    private String userId;
    private String sessionId;
}
