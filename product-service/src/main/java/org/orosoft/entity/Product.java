package org.orosoft.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "ProductId")
    private String productId;

    @ManyToOne
    @JoinColumn(name = "CategoryId")
    private Category category;

    @Column(name = "ProductName")
    private String productName;

    @Column(name = "ProductPrice")
    private float productMarketPrice;

    @Column(name = "ProductDescription")
    private String productDescription;

    @Column(name = "ViewCount")
    private int productViewCount;

    @Override
    public String toString() {
        return "Product{" +
                "productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", productDescription='" + productDescription + '\'' +
                 ", category=" + category +
                ", viewCount=" + productViewCount +
                '}';
    }
}
