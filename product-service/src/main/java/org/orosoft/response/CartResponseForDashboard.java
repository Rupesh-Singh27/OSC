package org.orosoft.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponseForDashboard {

    @JsonProperty("TYPE")
    public String type;

    @JsonProperty("Cart")
    private CartDataForDashboard cartDataForDashboard;
}
