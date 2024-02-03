package org.orosoft.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class MainDataDto {

    @JsonProperty("TYPE")
    private String type;

    @JsonProperty("Recently Viewed Products")
    private List<RecentlyViewedProductDto> recentlyViewedProducts;

    @JsonProperty("Similar Products")
    private List<SimilarProductDto> similarProducts;

    @JsonProperty("Categories")
    private List<CategoryDto> categories;

    @JsonProperty("Cart")
    private CartDto cartData;
}
