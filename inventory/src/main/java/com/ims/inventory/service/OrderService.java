package com.ims.inventory.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ims.inventory.dao.OrderDAO;
import com.ims.inventory.dao.ProductDAO;
import com.ims.inventory.model.Order;
import com.ims.inventory.model.Product;

@Service
public class OrderService {

    private final OrderDAO orderDAO;
    private final ProductDAO productDAO;
    private final HistoryService historyService;

    public OrderService(OrderDAO orderDAO, ProductDAO productDAO, HistoryService historyService) {
        this.orderDAO = orderDAO;
        this.productDAO = productDAO;
        this.historyService = historyService;
    }

    public Order createOrder(long productId, int quantity, String customerUsername) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        Product product = productDAO.getProductById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product #" + productId + " not found"));

        if (product.getQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock! Available: " + product.getQuantity() + " units.");
        }

        boolean deducted = productDAO.selectOrBuyProduct(productId, quantity);
        if (!deducted) {
            throw new IllegalStateException("Could not complete order. Product stock may have changed.");
        }

        BigDecimal unitPrice = product.getPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        String orderNum = "ORD-" + (System.currentTimeMillis() % 10000000);

        Order order = new Order();
        order.setOrderNumber(orderNum);
        order.setCustomerUsername(customerUsername != null ? customerUsername : "Customer");
        order.setProductId(productId);
        order.setProductName(product.getProductName());
        order.setQuantity(quantity);
        order.setUnitPrice(unitPrice);
        order.setTotalAmount(totalAmount);
        order.setStatus("COMPLETED");
        order.setPaymentStatus("PAID");

        Order saved = orderDAO.createOrder(order);
        historyService.log("ORDER", "SALES", orderNum, 
            "Order placed: " + quantity + "x " + product.getProductName() + " ($" + totalAmount + ")", customerUsername);

        return saved;
    }

    public List<Order> getAllOrders() {
        return orderDAO.getAllOrders();
    }

    public List<Order> getOrdersByCustomer(String customerUsername) {
        return orderDAO.getOrdersByCustomer(customerUsername);
    }

    public Order cancelOrder(long orderId, String username) {
        Order order = orderDAO.getOrderById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order #" + orderId + " not found"));

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Order is already cancelled.");
        }

        orderDAO.cancelOrder(orderId);
        // Restore stock
        productDAO.adjustStock(order.getProductId(), order.getQuantity(), "ADD", "Cancelled Order " + order.getOrderNumber());
        
        historyService.log("CANCEL", "SALES", order.getOrderNumber(), 
            "Cancelled order #" + orderId + " and restocked " + order.getQuantity() + " units.", username);

        order.setStatus("CANCELLED");
        return order;
    }
}
