package com.ims.inventory.dao;

import java.util.List;
import java.util.Optional;
import com.ims.inventory.model.Order;

public interface OrderDAO {
    Order createOrder(Order order);
    List<Order> getAllOrders();
    List<Order> getOrdersByCustomer(String customerUsername);
    Optional<Order> getOrderById(long orderId);
    boolean cancelOrder(long orderId);
}
