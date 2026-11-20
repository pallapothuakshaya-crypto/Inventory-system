package com.ims.inventory.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendRestockEmail(String toEmail, String productName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Good News! " + productName + " is back in stock!");
            message.setText("Hello,\n\nThe product '" + productName + "' you requested is now back in stock. " +
                            "Hurry and place your order before it sells out again!\n\nThank you,\nIMS Team");
            
            mailSender.send(message);
            System.out.println(">>> Restock email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println(">>> Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }
}