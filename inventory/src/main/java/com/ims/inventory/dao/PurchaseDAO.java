package com.ims.inventory.dao;

import java.util.List;
import java.util.Optional;
import com.ims.inventory.model.Purchase;

public interface PurchaseDAO {
    Purchase createPurchase(Purchase purchase);
    List<Purchase> getAllPurchases();
    Optional<Purchase> getPurchaseById(long purchaseId);
    boolean updatePurchaseStatus(long purchaseId, String status);
}
