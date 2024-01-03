package org.orosoft.userservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginLogoutDTO{

    private String sessionId;
    private String userId;
    private String device;
    private String loginTime;
    private String logoutTime;
    private boolean loginStatus;
}
