package org.orosoft.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.orosoft.dto.CategoryDto;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {

    @JsonProperty("TYPE")
    public String type;

    @JsonProperty("Categories")
    private List<CategoryDto> categories;
}
