package com.ims.inventory.model;

import java.time.LocalDateTime;

public class RestockNotification {
    private Long notificationId;
    private Long productId;
    private String email;
    private String status;
    private LocalDateTime createdAt;

    public RestockNotification() {}

    public RestockNotification(Long notificationId, Long productId, String email, String status, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.productId = productId;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}