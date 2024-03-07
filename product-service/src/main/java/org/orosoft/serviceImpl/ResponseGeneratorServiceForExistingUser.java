package org.orosoft.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.orosoft.dto.CategoryDto;
import org.orosoft.dto.ProductDto;
import org.orosoft.entity.Cart;
import org.orosoft.entity.Product;
import org.orosoft.entity.RecentView;
import org.orosoft.hazelcastmap.TempDatabaseMapOperations;
import org.orosoft.kafkatable.CartProductsKTable;
import org.orosoft.kafkatable.RecentViewKTable;
import org.orosoft.response.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ResponseGeneratorServiceForExistingUser {
    private final TempDatabaseMapOperations tempDatabaseMapOperations;
    private final CategorySortHandler categorySortHandler;
    private final CartProductsKTable cartProductsKTable;
    private final RecentViewKTable recentViewKTable;
    private final ModelMapper modelMapper;

    ResponseGeneratorServiceForExistingUser(
            TempDatabaseMapOperations tempDatabaseMapOperations,
            CategorySortHandler categorySortHandler,
            CartProductsKTable cartProductsKTable,
            RecentViewKTable recentViewKTable,
            ModelMapper modelMapper
    ){
        this.tempDatabaseMapOperations = tempDatabaseMapOperations;
        this.categorySortHandler = categorySortHandler;
        this.cartProductsKTable = cartProductsKTable;
        this.recentViewKTable = recentViewKTable;
        this.modelMapper = modelMapper;
    }

    /*Prepare the response by getting all the necessary lists and sets */
    public ExistingUserDataObjectResponse buildResponseForExistingUser(String userId){
        /*Get recentlyViewedProducts List from KTable Cache which was produced during login*/
        List<ProductDto> recentViewedProductList = getRecentViewedProductFromCache(userId);

        /*Build similar products list based on recently viewed products.*/
        Set<ProductDto> similarProductSet = getSimilarProducts(recentViewedProductList);

        /*Fetch the cart products from cache(KTable) and return the response for the dashboard*/
        CartDataForDashboard cartDataForDashboard = getCartProductsFromCache(userId);

        /*Get categories in descending order*/
        List<CategoryDto> categoryListInDescending = categorySortHandler.sortCategoriesInDescending();

        similarProductSet.forEach(prod -> System.out.println(prod.getProductId()));

        /*Building response*/
        List<Object> mainData = new ArrayList<>();
        mainData.add(RecentlyViewResponse.builder().type("Recently Viewed Products").recentlyViewedProducts(recentViewedProductList).build());
        mainData.add(SimilarProductResponse.builder().type("Similar Products").similarProducts(similarProductSet).build());
        mainData.add(CategoryResponse.builder().type("Categories").categories(categoryListInDescending).build());
        mainData.add(CartResponseForDashboard.builder().type("Cart").cartDataForDashboard(cartDataForDashboard).build());
        return ExistingUserDataObjectResponse.builder().data(mainData).build();
    }

    public List<ProductDto> getRecentViewedProductFromCache(String userId) {
        List<RecentView> recentlyViewedProducts = recentViewKTable.getRecentViewedProductFromKTable(userId);

        return recentlyViewedProducts
                .stream()
                .map(this::productToProductDTOValueMapper)
                .collect(Collectors.toList());
    }

    public Set<ProductDto> getSimilarProducts(List<ProductDto> recentViewProductList) {
        Set<ProductDto> similarProductSet = computeSimilarProductsSet(recentViewProductList);

        if(similarProductSet.size() < 6) completeSimilarProductsForCategory(recentViewProductList, similarProductSet);

        return similarProductSet;
    }

    /*Get the highest viewed product from every category of the products which user has recently seen.
    If highest viewed is in recently viewed then get next highest.*/
    private Set<ProductDto> computeSimilarProductsSet(List<ProductDto> recentViewProductList) {
        Set<ProductDto> similarProductSet = new LinkedHashSet<>();

        /*Constant loop always run 6 times*/
        for(ProductDto productDto : recentViewProductList){
            /*All Products for the current category in most view desc order*/
            List<ProductDto> allProductsOfACategoryInViewCountDesc =
                    getProductCollectionBasedOnCategoryIdFromCache(productDto.getCategory().getCategoryId())
                    .stream()
                    .sorted(this::sortInDescending)
                    .toList();

            /*Make sure that the same products are not present in recent view list or similar product list already*/
            for(ProductDto product : allProductsOfACategoryInViewCountDesc){
                if(!recentViewProductList.contains(product) && !similarProductSet.contains(product)){
                    similarProductSet.add(product);
                    break;
                }
            }
        }
        return similarProductSet;
    }

    private Collection<ProductDto> getProductCollectionBasedOnCategoryIdFromCache(char categoryId) {
        return tempDatabaseMapOperations.getProductCollectionBasedOnCategoryId(categoryId);
    }

    /*
     * If Recent View has less than 6 product then similar products will have less than 6 as well,
     * therefore the rest of the slots of “Similar Products” would be filled with similar products from the user’s most recently viewed category
     * (in descending order of their rank).
     * */
    private void completeSimilarProductsForCategory(List<ProductDto> recentViewProductList, Set<ProductDto> similarProductSet) {
        char categoryId = recentViewProductList
                .stream()
                .findFirst()
                .map(ProductDto::getCategory)
                .map(CategoryDto::getCategoryId)
                .orElseThrow(() -> new NoSuchElementException("No category found in recentViewProductList"));

        getProductCollectionBasedOnCategoryIdFromCache(categoryId)
                .stream()
                .sorted(this::sortInDescending)
                .filter(productDto -> !recentViewProductList.contains(productDto) && !similarProductSet.contains(productDto))
                .forEach(productDto -> {
                    if(similarProductSet.size() < 6){
                         similarProductSet.add(productDto);
                    }
                });
    }

    public CartDataForDashboard getCartProductsFromCache(String userId){
        List<Cart> cartProductList = cartProductsKTable.getCartProductList(userId);

        return CartDataForDashboard.builder()
                .userId(userId)
                .cartProducts(cartProductList)
                .productCountInCart(cartProductList.size())
                .totalPrice(cartProductList.stream().mapToDouble(Cart::getProductPrice).sum())
                .build();
    }
    private int sortInDescending(ProductDto comparedProduct, ProductDto comparingProduct) {
        int comparisonResult = comparingProduct.getProductViewCount() - comparedProduct.getProductViewCount();

        if(comparisonResult == 0){
            comparisonResult = comparedProduct.getProductId().compareTo(comparingProduct.getProductId());
        }
        return comparisonResult;
    }

    private ProductDto productToProductDTOValueMapper(RecentView recentView) {
        Product product = recentView.getProduct();
        return modelMapper.map(product, ProductDto.class);
    }
}
