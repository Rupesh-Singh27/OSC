package org.orosoft.userservice.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserIdAndOTPDto {

    private String userId;

    @JsonProperty("OTP")
    private int otp;
}
