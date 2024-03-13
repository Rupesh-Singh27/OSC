package org.orosoft.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private String productId;

    private CategoryDto category;

    @JsonProperty("prodName")
    private String productName;

    @JsonProperty("prodMarketPrice")
    private double productMarketPrice;

    private String productDescription;

    @JsonProperty("Counts")
    private int productViewCount;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ProductDto other = (ProductDto) obj;
        return Objects.equals(productId, other.productId);
    }
}
