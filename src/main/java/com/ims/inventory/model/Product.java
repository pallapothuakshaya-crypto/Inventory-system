package com.ims.inventory.model;

import java.math.BigDecimal;

public class Product {
    private Long productId;
    private String productName;
    private String description;
    private BigDecimal price;
    private Long categoryId;
    private Integer quantity;
    private Integer soldQuantity;
    private String imageUrl;

    private Integer minStock;
    private String expiryDate;

    public Product() { 
        this.quantity = 0;
        this.soldQuantity = 0;
        this.minStock = 5;
    }

    public Product(Long productId, String productName, String description, BigDecimal price, Long categoryId, String imageUrl) {
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.imageUrl = imageUrl;
        this.quantity = 0;
        this.soldQuantity = 0;
        this.minStock = 5;
    }

    public Product(Long productId, String productName, String description, BigDecimal price, Long categoryId, Integer quantity, Integer soldQuantity, String imageUrl) {
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.quantity = quantity != null ? quantity : 0;
        this.soldQuantity = soldQuantity != null ? soldQuantity : 0;
        this.imageUrl = imageUrl;
        this.minStock = 5;
    }

    public Product(Long productId, String productName, String description, BigDecimal price, Long categoryId, Integer quantity, Integer soldQuantity, Integer minStock, String expiryDate, String imageUrl) {
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.quantity = quantity != null ? quantity : 0;
        this.soldQuantity = soldQuantity != null ? soldQuantity : 0;
        this.minStock = minStock != null ? minStock : 5;
        this.expiryDate = expiryDate;
        this.imageUrl = imageUrl;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Integer getQuantity() { return quantity != null ? quantity : 0; }
    public void setQuantity(Integer quantity) { this.quantity = quantity != null ? quantity : 0; }

    public Integer getSoldQuantity() { return soldQuantity != null ? soldQuantity : 0; }
    public void setSoldQuantity(Integer soldQuantity) { this.soldQuantity = soldQuantity != null ? soldQuantity : 0; }

    public Integer getMinStock() { return minStock != null ? minStock : 5; }
    public void setMinStock(Integer minStock) { this.minStock = minStock != null ? minStock : 5; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getImageUrl() { return imageUrl; } 
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}