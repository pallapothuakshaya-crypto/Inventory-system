package com.ims.inventory.dao;

import java.util.List;

public interface NotificationDAO {
    void saveNotification(long productId, String email);
    List<String> getPendingEmails(long productId);
    void markAsSent(long productId, String email);
}