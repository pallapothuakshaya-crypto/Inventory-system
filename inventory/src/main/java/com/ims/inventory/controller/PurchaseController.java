package com.ims.inventory.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ims.inventory.model.Purchase;
import com.ims.inventory.service.PurchaseService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    private String getUsername(HttpServletRequest request) {
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("username") != null) {
                return (String) session.getAttribute("username");
            }
        }
        return "Admin";
    }

    @PostMapping
    public ResponseEntity<?> createPurchase(@RequestBody Map<String, Object> req, HttpServletRequest request) {
        try {
            Purchase purchase = new Purchase();
            purchase.setSupplierId(Long.parseLong(req.get("supplierId").toString()));
            purchase.setSupplierName(req.get("supplierName").toString());
            purchase.setProductId(Long.parseLong(req.get("productId").toString()));
            purchase.setProductName(req.get("productName").toString());
            purchase.setQuantity(Integer.parseInt(req.get("quantity").toString()));
            purchase.setUnitCost(new BigDecimal(req.get("unitCost").toString()));
            if (req.containsKey("status") && req.get("status") != null) {
                purchase.setStatus(req.get("status").toString());
            }

            Purchase created = purchaseService.createPurchase(purchase, getUsername(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public List<Purchase> getAllPurchases() {
        return purchaseService.getAllPurchases();
    }

    @PutMapping("/{id}/receive")
    public ResponseEntity<?> receivePurchase(@PathVariable long id, HttpServletRequest request) {
        try {
            Purchase received = purchaseService.receivePurchase(id, getUsername(request));
            return ResponseEntity.ok(received);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
