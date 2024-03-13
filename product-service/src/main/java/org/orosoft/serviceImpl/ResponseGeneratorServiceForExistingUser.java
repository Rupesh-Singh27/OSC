package org.orosoft.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.orosoft.dto.CategoryDto;
import org.orosoft.dto.ProductDto;
import org.orosoft.entity.CartProduct;
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
        List<ProductDto> similarProductSet = getSimilarProducts(recentViewedProductList);


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

    public List<ProductDto> getSimilarProducts(List<ProductDto> recentViewProductList) {
        List<ProductDto> similarProductList = computeSimilarProductsSet(recentViewProductList);

        if(similarProductList.size() < 6)
            completeSimilarProductsForCategory(recentViewProductList, similarProductList);

        return similarProductList;
    }

    /*Get the highest viewed product from every category of the products which user has recently seen.
    If highest viewed is in recently viewed then get next highest.*/
    private List<ProductDto> computeSimilarProductsSet(List<ProductDto> recentViewProductList) {
        /*
        RV          SP
        Camera 05 	Camera 01
        Mobile 06 	Mobile 01
        Laptop 07 	Laptop 01
        Speaker 08 	Speaker 01
        Printer 09 	Printer 01
        Tablet 01 	Tablet 02
        */
        List<ProductDto> similarProductList = new ArrayList<>();

        /*Constant loop always run 6 times*/
        for(ProductDto productDto : recentViewProductList){

           /* All Products for the current category in most view desc order*/
            List<ProductDto> allProductsOfACategoryInViewCountDesc =
                    getProductCollectionBasedOnCategoryIdFromCache(productDto.getCategory().getCategoryId())
                            .stream()
                            .sorted(this::sortInDescending)
                            .toList();

           /* Make sure that the same products are not present similar product list already*/
            for(ProductDto product : allProductsOfACategoryInViewCountDesc){
                if(!recentViewProductList.contains(product) && !similarProductList.contains(product)){
                    similarProductList.add(product);
                    break;
                }
            }
        }
        return similarProductList;
    }

    /*
     * If Recent View has less than 6 product then similar products will have less than 6 as well,
     * therefore the rest of the slots of “Similar Products” would be filled with similar products from the user’s most recently viewed category
     * (in descending order of their rank).
     * */
    private void completeSimilarProductsForCategory(List<ProductDto> recentViewProductList, List<ProductDto> similarProductList) {
        /*
        RV               SP
        Camera 01       Camera 03
        Mobile 06       Mobile 01
        Laptop 07       Laptop 01
        Camera 02       Camera 04
        ---             Camera 05
        ---             Camera 06
        */
        char categoryId = recentViewProductList.get(0).getCategory().getCategoryId();

        getProductCollectionBasedOnCategoryIdFromCache(categoryId)
                .stream()
                .sorted(this::sortInDescending)
                .filter(productDto -> !recentViewProductList.contains(productDto) && !similarProductList.contains(productDto))
                .forEach(productDto -> {
                    if(similarProductList.size() < 6){
                         similarProductList.add(productDto);
                    }
                });
    }

    public CartDataForDashboard getCartProductsFromCache(String userId){
        List<CartProduct> cartProductProductList = cartProductsKTable.getCartProductList(userId);

        return CartDataForDashboard.builder()
                .userId(userId)
                .cartProducts(cartProductProductList)
                .productCountInCart(cartProductProductList.size())
                .totalPrice(cartProductProductList.stream().mapToDouble(CartProduct::getProductPrice).sum())
                .build();
    }

    private Collection<ProductDto> getProductCollectionBasedOnCategoryIdFromCache(char categoryId) {
        return tempDatabaseMapOperations.getProductCollectionBasedOnCategoryId(categoryId);
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
