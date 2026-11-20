package com.ims.inventory.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ims.inventory.dao.NotificationDAO; // <-- NEW IMPORT
import com.ims.inventory.model.Product;
import com.ims.inventory.service.HistoryService;
import com.ims.inventory.service.ProductService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final HistoryService historyService;
    private final NotificationDAO notificationDAO; // <-- NEW FIELD

    // UPDATED CONSTRUCTOR
    public ProductController(ProductService productService, HistoryService historyService, NotificationDAO notificationDAO) {
        this.productService = productService;
        this.historyService = historyService;
        this.notificationDAO = notificationDAO;
    }

    private String getUsername(HttpServletRequest request) {
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("username") != null) {
                return (String) session.getAttribute("username");
            }
        }
        return "User";
    }

    @PostMapping
    public ResponseEntity<Product> add(@RequestBody Product product, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addProduct(product, getUsername(request)));
    }

    @GetMapping
    public List<Product> getAll() {
        return productService.getAllProducts();
    }

    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seed() {
        productService.seedSampleProducts();
        List<Product> list = productService.getAllProducts();
        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Sample products added successfully",
            "count", list.size()
        ));
    }

    @GetMapping("/{id}")
    public Product getOne(@PathVariable long id) {
        return productService.getProduct(id);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable long id, @RequestBody Product product, HttpServletRequest request) {
        return productService.updateProduct(id, product, getUsername(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id, HttpServletRequest request) {
        productService.deleteProduct(id, getUsername(request));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<Product> search(@RequestParam String keyword) {
        return productService.searchProducts(keyword);
    }

    @PostMapping("/{id}/adjust-stock")
    public ResponseEntity<?> adjustStock(@PathVariable long id, @RequestBody Map<String, Object> req, HttpServletRequest request) {
        try {
            int amount = Integer.parseInt(req.get("amount").toString());
            String type = req.getOrDefault("type", "ADD").toString();
            String reason = req.getOrDefault("reason", "Manual adjustment").toString();
            Product updated = productService.adjustStock(id, amount, type, reason, getUsername(request));
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/select")
    public ResponseEntity<?> selectProduct(@PathVariable long id, HttpServletRequest request) {
        try {
            Product updated = productService.selectOrBuyProduct(id, getUsername(request));
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Product selected/purchased successfully!",
                "product", updated
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", "SOLD_OUT",
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "ERROR",
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/notify")
    public ResponseEntity<?> notifyWhenRestocked(@PathVariable long id, @RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        Product product = productService.getProduct(id);
        String contact = (body != null && body.containsKey("contact") && !body.get("contact").isBlank())
            ? body.get("contact")
            : getUsername(request);

        // --- NEW: SAVE TO DATABASE INSTEAD OF JUST LOGGING ---
        notificationDAO.saveNotification(id, contact);
        // -----------------------------------------------------

        historyService.log("NOTIFY_REQUEST", "PRODUCT", product.getProductName(),
            "Restock notification registered for customer: " + contact, getUsername(request));

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Notification recorded! We will notify you at '" + contact + "' as soon as '" + product.getProductName() + "' is restocked."
        ));
    }
}