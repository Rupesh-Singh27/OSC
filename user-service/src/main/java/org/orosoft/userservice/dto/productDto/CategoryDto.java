package org.orosoft.userservice.dto.productDto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {

    private char categoryId;

    private String categoryName;

    @JsonProperty("count")
    private int categoryViewCount;
}
