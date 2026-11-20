package com.ims.inventory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ims.inventory.model.Supplier;
import com.ims.inventory.model.SupplierProduct;
import com.ims.inventory.service.SupplierService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
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
    public ResponseEntity<Supplier> add(@RequestBody Supplier supplier, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.addSupplier(supplier, getUsername(request)));
    }

    @GetMapping
    public List<Supplier> getAll() {
        return supplierService.getAllSuppliers();
    }

    @GetMapping("/{id}")
    public Supplier getOne(@PathVariable long id) {
        return supplierService.getSupplier(id);
    }

    @PutMapping("/{id}")
    public Supplier update(@PathVariable long id, @RequestBody Supplier supplier, HttpServletRequest request) {
        return supplierService.updateSupplier(id, supplier, getUsername(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id, HttpServletRequest request) {
        supplierService.deleteSupplier(id, getUsername(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/link")
    public ResponseEntity<SupplierProduct> linkProduct(@RequestBody SupplierProduct supplierProduct) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.linkProduct(supplierProduct));
    }

    @GetMapping("/{id}/products")
    public List<SupplierProduct> getProducts(@PathVariable long id) {
        return supplierService.getProductsBySupplier(id);
    }
}