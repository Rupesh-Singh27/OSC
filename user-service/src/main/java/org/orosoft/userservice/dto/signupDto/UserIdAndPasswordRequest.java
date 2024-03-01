package org.orosoft.userservice.dto.signupDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserIdAndPasswordRequest {

    private String userId;
    private String password;
}
