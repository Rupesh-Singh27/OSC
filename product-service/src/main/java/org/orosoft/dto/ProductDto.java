package org.orosoft.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private String productId;

    private String categoryId;

    @JsonProperty("prodName")
    private String productName;

    @JsonProperty("prodMarketPrice")
    private double productMarketPrice;

    private String productDescription;

    @JsonProperty("Counts")
    private int productViewCount;
}
