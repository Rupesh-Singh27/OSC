package org.orosoft.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.orosoft.dto.ProductDto;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeaturedProductResponse {

    @JsonProperty("TYPE")
    private String type;

    @JsonProperty("Featured Products")
    private List<ProductDto> featureProducts;
}
