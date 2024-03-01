package org.orosoft.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MTPingRequest {
    @JsonProperty("MT")
    private String mtPing;
    @JsonProperty("prodId")
    private String productId;
    @JsonProperty("catId")
    private String categoryId;
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("filter")
    private String filter;
}
