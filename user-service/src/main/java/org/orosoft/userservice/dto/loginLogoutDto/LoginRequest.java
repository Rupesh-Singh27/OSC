package org.orosoft.userservice.dto.loginLogoutDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    private String userId;
    private String password;
    private String loginDevice;
}
