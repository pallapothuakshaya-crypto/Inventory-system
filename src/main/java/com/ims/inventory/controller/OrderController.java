package com.ims.inventory.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ims.inventory.model.Order;
import com.ims.inventory.service.OrderService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    private String getUsername(HttpServletRequest request) {
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("username") != null) {
                return (String) session.getAttribute("username");
            }
        }
        return "Customer";
    }

    private String getRole(HttpServletRequest request) {
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("role") != null) {
                return (String) session.getAttribute("role");
            }
        }
        return "USER";
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> req, HttpServletRequest request) {
        try {
            long productId = Long.parseLong(req.get("productId").toString());
            int quantity = req.containsKey("quantity") ? Integer.parseInt(req.get("quantity").toString()) : 1;
            String username = req.containsKey("customerUsername") ? req.get("customerUsername").toString() : getUsername(request);

            Order created = orderService.createOrder(productId, quantity, username);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error creating order: " + e.getMessage()));
        }
    }

    @GetMapping
    public List<Order> getOrders(HttpServletRequest request) {
        String role = getRole(request);
        String username = getUsername(request);
        if ("ADMIN".equalsIgnoreCase(role)) {
            return orderService.getAllOrders();
        } else {
            return orderService.getOrdersByCustomer(username);
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable long id, HttpServletRequest request) {
        try {
            Order cancelled = orderService.cancelOrder(id, getUsername(request));
            return ResponseEntity.ok(cancelled);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
