package org.orosoft.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class RecentView {

    @Id
    @Column(name = "recent_view_id")
    private int recentViewId;

    @Column(name = "user_id")
    private String userId;

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "view_date")
    private String viewDate;
}
