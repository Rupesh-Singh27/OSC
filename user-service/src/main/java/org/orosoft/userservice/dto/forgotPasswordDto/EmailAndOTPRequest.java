package org.orosoft.userservice.dto.forgotPasswordDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailAndOTPRequest {

    private String email;

    @JsonProperty("OTP")
    private long otp;
}
