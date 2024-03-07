package org.orosoft.serviceImpl;

import org.orosoft.entity.Cart;
import org.orosoft.entity.Product;
import org.orosoft.entity.RecentView;
import org.orosoft.repository.CartRepository;
import org.orosoft.repository.ProductRepository;
import org.orosoft.repository.RecentViewRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductServiceDaoHandler {
    private final ProductRepository productRepository;
    private final RecentViewRepository recentViewRepository;
    private final CartRepository cartRepository;

    public ProductServiceDaoHandler(ProductRepository productRepository, RecentViewRepository recentViewRepository, CartRepository cartRepository) {
        this.productRepository = productRepository;
        this.recentViewRepository = recentViewRepository;
        this.cartRepository = cartRepository;
    }

    public List<Product> fetchProductsFromDatabase() {
        return productRepository.findAll();
    }
    public List<RecentView> fetchRecentlyViewedProductsFromDatabase(String userId) {
        return recentViewRepository.findAllRecentViewForUserInDescending(userId);
    }
    public void deleteLeastRecentViewProducts(String userId, List<String> latestViewDates){
        recentViewRepository.deleteOldRecentViews(userId, latestViewDates);
    }
    public List<Cart> fetchCartProductsFromDatabase(String userId) {
        return cartRepository.findByUserId(userId);
    }
    public void deleteCartProductFromDatabase(String productId){
        cartRepository.deleteByProductId(productId);
    }
    public void saveCartProductInDatabase(Cart cartProduct){
        cartRepository.save(cartProduct);
    }
}
