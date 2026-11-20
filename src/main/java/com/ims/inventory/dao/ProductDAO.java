package com.ims.inventory.dao;

import java.util.List;
import java.util.Optional;
import com.ims.inventory.model.Product;

public interface ProductDAO {
    void addProduct(Product product);
    boolean updateProduct(Product product);
    boolean deleteProduct(long id);
    Optional<Product> getProductById(long id);
    List<Product> getAllProducts();
    List<Product> searchProducts(String keyword);
    boolean selectOrBuyProduct(long id, int count);
    boolean updateQuantities(long id, int quantity, int soldQuantity);
    void seedSampleProducts();
    boolean adjustStock(long productId, int amount, String type, String reason);
}