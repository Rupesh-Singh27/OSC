package org.orosoft.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.orosoft.entity.Cart;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDataForDashboard {

    @JsonProperty("userId")
    public String userId;

    @JsonProperty("cartProducts")
    private List<Cart> cartProducts;

    @JsonProperty("ProductsCartCount")
    private int productCountInCart;

    @JsonProperty("Price")
    private double totalPrice;
}
