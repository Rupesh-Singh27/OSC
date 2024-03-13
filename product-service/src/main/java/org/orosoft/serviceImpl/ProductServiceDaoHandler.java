package org.orosoft.serviceImpl;

import org.orosoft.entity.CartProduct;
import org.orosoft.entity.Product;
import org.orosoft.entity.RecentView;
import org.orosoft.repository.CartRepository;
import org.orosoft.repository.ProductRepository;
import org.orosoft.repository.RecentViewRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductServiceDaoHandler {
    private final RecentViewRepository recentViewRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;

    public ProductServiceDaoHandler(
            ProductRepository productRepository,
            RecentViewRepository recentViewRepository,
            CartRepository cartRepository
    ) {
        this.recentViewRepository = recentViewRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
    }

    public List<Product> fetchProductsFromDatabase() {
        return productRepository.findAll();
    }
    public List<RecentView> fetchRecentlyViewedProductsFromDatabase(String userId) {
        return recentViewRepository.findAllRecentViewForUserInDescending(userId);
    }
    /*public void deleteLeastRecentViewProducts(String userId, List<String> latestViewDates){
        recentViewRepository.deleteOldRecentViews(userId, latestViewDates);
    }*/
    public void deleteLeastRecentViewProducts(String userId){
        recentViewRepository.deleteOldRecentViews(userId);
    }
    public void saveRecentViewProducts(List<RecentView> recentViewList){
        recentViewList.forEach(recentViewRepository::save);
    }
    public List<CartProduct> fetchCartProductsFromDatabase(String userId) {
        return cartRepository.findByUserId(userId);
    }
    public void deleteCartProductFromDatabase(String productId){
        cartRepository.deleteByProductId(productId);
    }
    public void saveCartProductInDatabase(CartProduct cartProduct){
        cartRepository.save(cartProduct);
    }
}
