package org.orosoft.repository;

import jakarta.transaction.Transactional;
import org.orosoft.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId")
    List<Cart> findByUserId(@Param("userId") String userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Cart c Where c.productId = :productId")
    void deleteByProductId(@Param("productId") String productId);
}
