package org.orosoft.userservice.dto.forgotPasswordDto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailAndPasswordRequest {

    private String email;
    private String password;

}
