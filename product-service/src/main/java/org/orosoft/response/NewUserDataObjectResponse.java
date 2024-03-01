package org.orosoft.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewUserDataObjectResponse {

    /*@JsonProperty("data")
    List<NewUserDataResponse> data;*/

    @JsonProperty("data")
    List<Object> data;
}
