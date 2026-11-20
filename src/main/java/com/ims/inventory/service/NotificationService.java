package com.ims.inventory.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.ims.inventory.dao.NotificationDAO;

@Service
public class NotificationService {

    private final NotificationDAO notificationDAO;
    private final EmailService emailService;

    public NotificationService(NotificationDAO notificationDAO, EmailService emailService) {
        this.notificationDAO = notificationDAO;
        this.emailService = emailService;
    }

    public void processRestockNotifications(long productId, String productName) {
        List<String> pendingEmails = notificationDAO.getPendingEmails(productId);
        for (String email : pendingEmails) {
            emailService.sendRestockEmail(email, productName);
            notificationDAO.markAsSent(productId, email);
        }
    }
}