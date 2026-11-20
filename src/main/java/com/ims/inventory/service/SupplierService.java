package com.ims.inventory.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ims.inventory.dao.ProductDAO;
import com.ims.inventory.dao.SupplierDAO;
import com.ims.inventory.model.Supplier;
import com.ims.inventory.model.SupplierProduct;

@Service
public class SupplierService {

    private final SupplierDAO supplierDAO;
    private final ProductDAO productDAO;
    private final HistoryService historyService;

    public SupplierService(SupplierDAO supplierDAO, ProductDAO productDAO, HistoryService historyService) {
        this.supplierDAO = supplierDAO;
        this.productDAO = productDAO;
        this.historyService = historyService;
    }

    public Supplier addSupplier(Supplier supplier) {
        return addSupplier(supplier, "Admin");
    }

    public Supplier addSupplier(Supplier supplier, String username) {
        validate(supplier);
        supplierDAO.addSupplier(supplier);
        historyService.log("CREATE", "SUPPLIER", supplier.getSupplierName(), "Added new supplier", username);
        return supplier;
    }

    public List<Supplier> getAllSuppliers() {
        return supplierDAO.getAllSuppliers();
    }

    public Supplier getSupplier(long id) {
        return supplierDAO.getSupplierById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier " + id + " not found"));
    }

    public Supplier updateSupplier(long id, Supplier supplier) {
        return updateSupplier(id, supplier, "Admin");
    }

    public Supplier updateSupplier(long id, Supplier supplier, String username) {
        validate(supplier);
        supplier.setSupplierId(id);
        if (!supplierDAO.updateSupplier(supplier)) {
            throw new ResourceNotFoundException("Supplier " + id + " not found");
        }
        historyService.log("UPDATE", "SUPPLIER", supplier.getSupplierName(), "Updated supplier details", username);
        return supplier;
    }

    public void deleteSupplier(long id) {
        deleteSupplier(id, "Admin");
    }

    public void deleteSupplier(long id, String username) {
        Supplier sup = getSupplier(id);
        if (supplierDAO.isSupplierInUse(id)) {
            throw new BusinessRuleException("Cannot delete: products are linked to this supplier");
        }
        supplierDAO.deleteSupplier(id);
        historyService.log("DELETE", "SUPPLIER", sup.getSupplierName(), "Deleted supplier #" + id, username);
    }

    public SupplierProduct linkProduct(SupplierProduct sp) {
        if (sp.getSupplierId() == null || sp.getProductId() == null) {
            throw new IllegalArgumentException("Supplier ID and Product ID are required");
        }
        if (sp.getSupplyPrice() == null || sp.getSupplyPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Supply price must be positive");
        }
        if (sp.getLeadTime() == null || sp.getLeadTime() < 0) {
            throw new IllegalArgumentException("Lead time cannot be negative");
        }

        // Ensure both exist
        getSupplier(sp.getSupplierId());
        productDAO.getProductById(sp.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product " + sp.getProductId() + " not found"));

        supplierDAO.linkProductToSupplier(sp);
        historyService.log("LINK", "SUPPLIER_PRODUCT", "Supplier #" + sp.getSupplierId() + " - Product #" + sp.getProductId(), "Linked supplier with price $" + sp.getSupplyPrice(), "Admin");
        return sp;
    }

    public List<SupplierProduct> getProductsBySupplier(long supplierId) {
        getSupplier(supplierId);
        return supplierDAO.getProductsBySupplier(supplierId);
    }

    private void validate(Supplier supplier) {
        if (supplier == null || supplier.getSupplierName() == null || supplier.getSupplierName().isBlank()) {
            throw new IllegalArgumentException("Supplier name is required");
        }
        supplier.setSupplierName(supplier.getSupplierName().trim());
    }
}