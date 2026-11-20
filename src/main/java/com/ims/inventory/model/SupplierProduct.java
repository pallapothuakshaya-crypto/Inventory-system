package com.ims.inventory.model;

import java.math.BigDecimal;

public class SupplierProduct {
    private Long supplierProductId;
    private Long supplierId;
    private Long productId;
    private BigDecimal supplyPrice;
    private Integer leadTime;

    public SupplierProduct() { }

    public SupplierProduct(Long supplierProductId, Long supplierId, Long productId, BigDecimal supplyPrice, Integer leadTime) {
        this.supplierProductId = supplierProductId;
        this.supplierId = supplierId;
        this.productId = productId;
        this.supplyPrice = supplyPrice;
        this.leadTime = leadTime;
    }

    public Long getSupplierProductId() { return supplierProductId; }
    public void setSupplierProductId(Long supplierProductId) { this.supplierProductId = supplierProductId; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public BigDecimal getSupplyPrice() { return supplyPrice; }
    public void setSupplyPrice(BigDecimal supplyPrice) { this.supplyPrice = supplyPrice; }

    public Integer getLeadTime() { return leadTime; }
    public void setLeadTime(Integer leadTime) { this.leadTime = leadTime; }
}