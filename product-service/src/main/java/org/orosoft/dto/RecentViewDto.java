package org.orosoft.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.orosoft.entity.Product;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecentViewDto {
    private int recentViewId;
    private String userId;
    private Product product;
    private String viewDate;
}
