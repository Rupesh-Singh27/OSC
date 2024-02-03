package org.orosoft.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDto {

    private String userId;

    private int productsCartCount;

    @JsonProperty("Price")
    private double price;

    private List<CartProductDto> cartProducts;
}
