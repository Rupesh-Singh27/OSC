package org.orosoft.userservice.dto.signupDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    private String name;
    private String email;
    private long contact;
    @JsonProperty("DOB")
    private String dataOfBirth;
    private String password;
    private long otp;

}
