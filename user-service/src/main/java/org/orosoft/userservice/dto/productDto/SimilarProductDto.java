package org.orosoft.userservice.dto.productDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimilarProductDto {

    List<ProductDto> productDtoList;
}
