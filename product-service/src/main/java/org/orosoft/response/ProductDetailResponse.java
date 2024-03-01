package org.orosoft.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class ProductDetailResponse {

    @JsonProperty("MT")
    private String ping;

    @JsonProperty("catId")
    private String categoryId;

    @JsonProperty("prodId")
    private String productId;

    @JsonProperty("prodName")
    private String productName;

    @JsonProperty("prodDesc")
    private String productDescription;

    @JsonProperty("prodPrice")
    private String productPrice;

    @JsonProperty("similarProducts")
    private List<ProductDto> similarProducts;
}
