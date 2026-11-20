package com.ims.inventory.dao;

import java.util.List;
import java.util.Optional;
import com.ims.inventory.model.Supplier;
import com.ims.inventory.model.SupplierProduct;

public interface SupplierDAO {
    void addSupplier(Supplier supplier);
    boolean updateSupplier(Supplier supplier);
    boolean deleteSupplier(long id);
    Optional<Supplier> getSupplierById(long id);
    List<Supplier> getAllSuppliers();
    
    void linkProductToSupplier(SupplierProduct supplierProduct);
    List<SupplierProduct> getProductsBySupplier(long supplierId);
    boolean isSupplierInUse(long id);
}