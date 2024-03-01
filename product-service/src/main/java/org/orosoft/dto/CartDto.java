package org.orosoft.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDto {

    private long cartId;

    private String userId;

    @JsonProperty("prodId")
    private String productId;

    @JsonProperty("prodName")
    private String productName;

    @JsonProperty("price")
    private double productPrice;

    @JsonProperty("cartQty")
    private int productCartQuantity;
}
