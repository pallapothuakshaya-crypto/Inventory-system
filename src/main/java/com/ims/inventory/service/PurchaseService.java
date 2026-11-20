package com.ims.inventory.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ims.inventory.dao.ProductDAO;
import com.ims.inventory.dao.PurchaseDAO;
import com.ims.inventory.model.Product;
import com.ims.inventory.model.Purchase;

@Service
public class PurchaseService {

    private final PurchaseDAO purchaseDAO;
    private final ProductDAO productDAO;
    private final HistoryService historyService;
    private final NotificationService notificationService;

    public PurchaseService(PurchaseDAO purchaseDAO, ProductDAO productDAO, HistoryService historyService, NotificationService notificationService) {
        this.purchaseDAO = purchaseDAO;
        this.productDAO = productDAO;
        this.historyService = historyService;
        this.notificationService = notificationService;
    }

    public Purchase createPurchase(Purchase purchase, String username) {
        if (purchase.getQuantity() == null || purchase.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        if (purchase.getUnitCost() == null || purchase.getUnitCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit cost must be greater than 0");
        }

        purchase.setTotalCost(purchase.getUnitCost().multiply(BigDecimal.valueOf(purchase.getQuantity())));
        if (purchase.getPoNumber() == null || purchase.getPoNumber().isBlank()) {
            purchase.setPoNumber("PO-" + (System.currentTimeMillis() % 10000000));
        }

        if (purchase.getStatus() == null || purchase.getStatus().isBlank()) {
            purchase.setStatus("RECEIVED");
        }

        Purchase saved = purchaseDAO.createPurchase(purchase);

        // If received immediately, update stock!
        if ("RECEIVED".equalsIgnoreCase(saved.getStatus())) {
            // 1. Check status BEFORE restocking
            Product productBefore = productDAO.getProductById(saved.getProductId()).orElse(null);
            boolean wasOutOfStock = (productBefore != null && productBefore.getQuantity() == 0);

            // 2. Add the stock
            productDAO.adjustStock(saved.getProductId(), saved.getQuantity(), "ADD", "Purchase Order " + saved.getPoNumber());

            // 3. Check status AFTER restocking
            if (wasOutOfStock && productBefore != null) {
                Product productAfter = productDAO.getProductById(saved.getProductId()).orElse(null);
                
                // --- YOUR NEW CONSTRAINT: Only notify if new stock >= minimum stock level ---
                if (productAfter != null && productAfter.getQuantity() >= productAfter.getMinStock()) {
                    notificationService.processRestockNotifications(productAfter.getProductId(), productAfter.getProductName());
                } else if (productAfter != null) {
                    System.out.println(">>> Restock detected for " + productAfter.getProductName() + 
                        ", but new quantity (" + productAfter.getQuantity() + 
                        ") is still below minimum stock (" + productAfter.getMinStock() + "). Notifications are on hold.");
                }
            }
        }

        historyService.log("PURCHASE", "INVENTORY", saved.getPoNumber(), 
            "Created PO from " + saved.getSupplierName() + ": " + saved.getQuantity() + "x " + saved.getProductName() + 
            " (Cost: $" + saved.getTotalCost() + ", Status: " + saved.getStatus() + ")", username);

        return saved;
    }

    public List<Purchase> getAllPurchases() {
        return purchaseDAO.getAllPurchases();
    }

    public Purchase receivePurchase(long purchaseId, String username) {
        Purchase purchase = purchaseDAO.getPurchaseById(purchaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase Order #" + purchaseId + " not found"));

        if ("RECEIVED".equalsIgnoreCase(purchase.getStatus())) {
            throw new IllegalStateException("Purchase is already marked as received.");
        }

        purchaseDAO.updatePurchaseStatus(purchaseId, "RECEIVED");
        
        // 1. Check status BEFORE restocking
        Product productBefore = productDAO.getProductById(purchase.getProductId()).orElse(null);
        boolean wasOutOfStock = (productBefore != null && productBefore.getQuantity() == 0);

        // 2. Add the stock
        productDAO.adjustStock(purchase.getProductId(), purchase.getQuantity(), "ADD", "Received PO " + purchase.getPoNumber());

        // 3. Check status AFTER restocking
        if (wasOutOfStock && productBefore != null) {
            Product productAfter = productDAO.getProductById(purchase.getProductId()).orElse(null);
            
            // --- YOUR NEW CONSTRAINT: Only notify if new stock >= minimum stock level ---
            if (productAfter != null && productAfter.getQuantity() >= productAfter.getMinStock()) {
                notificationService.processRestockNotifications(productAfter.getProductId(), productAfter.getProductName());
            } else if (productAfter != null) {
                System.out.println(">>> Restock detected for " + productAfter.getProductName() + 
                    ", but new quantity (" + productAfter.getQuantity() + 
                    ") is still below minimum stock (" + productAfter.getMinStock() + "). Notifications are on hold.");
            }
        }

        historyService.log("RECEIVE", "INVENTORY", purchase.getPoNumber(), 
            "Received " + purchase.getQuantity() + " units for PO " + purchase.getPoNumber(), username);

        purchase.setStatus("RECEIVED");
        return purchase;
    }
}