package org.orosoft.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.orosoft.entity.CartProduct;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDataForPing {

    @JsonProperty("MT")
    public String mtPing;

    @JsonProperty("cartProducts")
    private List<CartProduct> cartProductProducts;

    @JsonProperty("ProductsCartCount")
    private int productCountInCart;

    @JsonProperty("totalPrice")
    private double totalPrice;
}
