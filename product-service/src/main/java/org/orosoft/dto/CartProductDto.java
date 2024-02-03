package org.orosoft.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartProductDto {

    private ProductDto product;

    @JsonProperty("cartQty")
    private int cartQuantity;
}
