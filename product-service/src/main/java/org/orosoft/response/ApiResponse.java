package org.orosoft.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ApiResponse {

    private int code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("dataObject")
    private Object responseBuilder;
}
