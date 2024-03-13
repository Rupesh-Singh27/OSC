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
public class SimilarProductResponse {

    @JsonProperty("TYPE")
    public String type;

    @JsonProperty("Similar Products")
    private List<ProductDto> similarProducts;
}
