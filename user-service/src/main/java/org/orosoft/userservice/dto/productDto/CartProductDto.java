package org.orosoft.userservice.dto.productDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartProductDto {

    private ProductDto productDto;

    @JsonProperty("cartQty")
    private int cartQuantity;
}
