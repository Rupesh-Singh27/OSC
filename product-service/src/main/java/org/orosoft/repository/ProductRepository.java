package org.orosoft.repository;

import jakarta.transaction.Transactional;
import org.orosoft.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    @Transactional
    @Modifying
    @Query("UPDATE Product p SET p.productViewCount =:viewCount WHERE p.productId =:productId")
    void updateViewCount(@Param("productId") String productId,@Param("viewCount") int viewCount);
}
