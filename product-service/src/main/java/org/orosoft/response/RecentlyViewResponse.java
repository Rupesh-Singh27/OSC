package org.orosoft.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.orosoft.dto.ProductDto;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentlyViewResponse {

    @JsonProperty("TYPE")
    public String type;

    @JsonProperty("Recently Viewed Products")
//    private Set<ProductDto> recentlyViewedProducts;
    private List<ProductDto> recentlyViewedProducts;
}
