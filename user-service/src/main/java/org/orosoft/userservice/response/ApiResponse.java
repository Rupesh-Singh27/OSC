package org.orosoft.userservice.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Builder
@Data
public class ApiResponse {

    private int code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DataObject<?> dataObject;
}
