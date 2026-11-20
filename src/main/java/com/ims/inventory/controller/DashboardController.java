package com.ims.inventory.controller;

import java.math.BigDecimal;
import java.util.*;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ims.inventory.model.Order;
import com.ims.inventory.model.Product;
import com.ims.inventory.model.Purchase;
import com.ims.inventory.service.CategoryService;
import com.ims.inventory.service.OrderService;
import com.ims.inventory.service.ProductService;
import com.ims.inventory.service.PurchaseService;
import com.ims.inventory.service.SupplierService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ProductService productService;
    private final OrderService orderService;
    private final PurchaseService purchaseService;
    private final SupplierService supplierService;
    private final CategoryService categoryService;

    public DashboardController(ProductService productService, OrderService orderService,
                               PurchaseService purchaseService, SupplierService supplierService,
                               CategoryService categoryService) {
        this.productService = productService;
        this.orderService = orderService;
        this.purchaseService = purchaseService;
        this.supplierService = supplierService;
        this.categoryService = categoryService;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        List<Product> products = productService.getAllProducts();
        List<Order> orders = orderService.getAllOrders();
        List<Purchase> purchases = purchaseService.getAllPurchases();

        int totalProducts = products.size();
        int totalStockUnits = products.stream().mapToInt(Product::getQuantity).sum();
        int totalSoldUnits = products.stream().mapToInt(Product::getSoldQuantity).sum();

        long lowStockCount = products.stream()
            .filter(p -> p.getQuantity() > 0 && p.getQuantity() <= (p.getMinStock() != null ? p.getMinStock() : 5))
            .count();

        long outOfStockCount = products.stream()
            .filter(p -> p.getQuantity() <= 0)
            .count();

        BigDecimal totalSalesRevenue = orders.stream()
            .filter(o -> !"CANCELLED".equalsIgnoreCase(o.getStatus()))
            .map(Order::getTotalAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPurchasesCost = purchases.stream()
            .map(Purchase::getTotalCost)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> response = new HashMap<>();
        response.put("totalProducts", totalProducts);
        response.put("totalStockUnits", totalStockUnits);
        response.put("totalSoldUnits", totalSoldUnits);
        response.put("lowStockCount", lowStockCount);
        response.put("outOfStockCount", outOfStockCount);
        response.put("totalSuppliers", supplierService.getAllSuppliers().size());
        response.put("totalCategories", categoryService.getAllCategories().size());
        response.put("totalSalesRevenue", totalSalesRevenue);
        response.put("totalSalesCount", orders.size());
        response.put("totalPurchasesCost", totalPurchasesCost);
        response.put("totalPurchasesCount", purchases.size());
        response.put("recentOrders", orders.stream().limit(5).toList());
        response.put("recentPurchases", purchases.stream().limit(5).toList());

        return response;
    }
}
