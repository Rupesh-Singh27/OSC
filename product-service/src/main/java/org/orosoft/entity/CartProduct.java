package org.orosoft.entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "cart")
public class CartProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
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
