package com.ims.inventory.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ims.inventory.dao.CategoryDAO;
import com.ims.inventory.dao.ProductDAO;
import com.ims.inventory.model.Category;
import com.ims.inventory.model.Product;

@Service
public class ProductService {

    private final ProductDAO productDAO;
    private final CategoryDAO categoryDAO;
    private final HistoryService historyService;

    public ProductService(ProductDAO productDAO, CategoryDAO categoryDAO, HistoryService historyService) {
        this.productDAO = productDAO;
        this.categoryDAO = categoryDAO;
        this.historyService = historyService;
    }

    public Product addProduct(Product product) {
        return addProduct(product, "User");
    }

    public Product addProduct(Product product, String username) {
        validate(product);
        if (product.getQuantity() == null) {
            product.setQuantity(0);
        }
        if (product.getSoldQuantity() == null) {
            product.setSoldQuantity(0);
        }

        if (categoryDAO.getCategoryById(product.getCategoryId()).isEmpty()) {
            List<Category> all = categoryDAO.getAllCategories();
            if (!all.isEmpty()) {
                product.setCategoryId(all.get(0).getCategoryId());
            } else {
                try {
                    Category autoCat = new Category(null, "General", "General Category");
                    categoryDAO.addCategory(autoCat);
                    if (autoCat.getCategoryId() != null) {
                        product.setCategoryId(autoCat.getCategoryId());
                    }
                } catch (Exception ignored) {}
            }
        }
        
        productDAO.addProduct(product);
        historyService.log("CREATE", "PRODUCT", product.getProductName(), 
            "Added new product with price $" + product.getPrice() + ", initial quantity: " + product.getQuantity(), username);
        return product;
    }

    public List<Product> getAllProducts() {
        return productDAO.getAllProducts();
    }

    public void seedSampleProducts() {
        productDAO.seedSampleProducts();
        historyService.log("SEED", "PRODUCT", "Sample Inventory", "Seeded realistic product catalog into inventory", "System");
    }

    public Product getProduct(long id) {
        return productDAO.getProductById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
    }

    public Product updateProduct(long id, Product product) {
        return updateProduct(id, product, "User");
    }

    public Product updateProduct(long id, Product product, String username) {
        validate(product);
        product.setProductId(id);
        
        if (categoryDAO.getCategoryById(product.getCategoryId()).isEmpty()) {
            List<Category> all = categoryDAO.getAllCategories();
            if (!all.isEmpty()) {
                product.setCategoryId(all.get(0).getCategoryId());
            }
        }

        boolean updated = productDAO.updateProduct(product);
        if (!updated) {
            throw new ResourceNotFoundException("Product " + id + " not found");
        }
        historyService.log("UPDATE", "PRODUCT", product.getProductName(), 
            "Updated product details, price: $" + product.getPrice() + ", quantity: " + product.getQuantity() + ", sold: " + product.getSoldQuantity(), username);
        return product;
    }

    public Product selectOrBuyProduct(long id, String username) {
        Product p = getProduct(id);
        if (p.getQuantity() <= 0) {
            throw new IllegalStateException("Product '" + p.getProductName() + "' is sold out!");
        }
        boolean ok = productDAO.selectOrBuyProduct(id, 1);
        if (!ok) {
            throw new IllegalStateException("Product '" + p.getProductName() + "' is sold out or insufficient stock!");
        }
        historyService.log("ORDER", "PRODUCT", p.getProductName(), 
            "Customer selected/purchased 1 unit. Remaining stock: " + (p.getQuantity() - 1) + ", sold units: " + (p.getSoldQuantity() + 1), username);
        return getProduct(id);
    }

    public Product adjustStock(long id, int amount, String type, String reason, String username) {
        Product p = getProduct(id);
        if (amount <= 0) {
            throw new IllegalArgumentException("Adjustment amount must be greater than 0");
        }
        boolean ok = productDAO.adjustStock(id, amount, type, reason);
        if (!ok) {
            throw new IllegalStateException("Could not adjust stock for product #" + id);
        }
        historyService.log("STOCK_" + type.toUpperCase(), "PRODUCT", p.getProductName(), 
            "Stock adjustment (" + type.toUpperCase() + " " + amount + " units). Reason: " + reason, username);
        return getProduct(id);
    }

    public void deleteProduct(long id) {
        deleteProduct(id, "User");
    }

    public void deleteProduct(long id, String username) {
        Product p = getProduct(id);
        productDAO.deleteProduct(id);
        historyService.log("DELETE", "PRODUCT", p.getProductName(), 
            "Deleted product #" + id, username);
    }

    public List<Product> searchProducts(String keyword) {
        return productDAO.searchProducts(keyword);
    }

    private void validate(Product product) {
        if (product == null || product.getProductName() == null || product.getProductName().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be a positive number");
        }
        if (product.getCategoryId() == null) {
            throw new IllegalArgumentException("Category ID is required");
        }
        product.setProductName(product.getProductName().trim());
    }
}