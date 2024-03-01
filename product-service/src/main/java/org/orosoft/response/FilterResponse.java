package org.orosoft.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.orosoft.dto.ProductDto;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FilterResponse {

    @JsonProperty("MT")
    private String ping;

    @JsonProperty("catId")
    private String categoryId;

    @JsonProperty("products")
    private List<ProductDto> products;
}
