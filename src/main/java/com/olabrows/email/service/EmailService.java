package com.olabrows.email.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.email}")
    private String fromEmail;

    @Value("${app.name}")
    private String appName;

    public void sendOrderConfirmation(String toEmail, String customerName, String productName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Your Pre-Order is Confirmed! ✨ - " + appName);
        message.setText(
            "Hi " + customerName + ",\n\n" +
            "Thank you for your pre-order of " + productName + "!\n\n" +
            "We'll notify you as soon as it ships.\n\n" +
            "With love,\nThe " + appName + " Team 🌸"
        );
        mailSender.send(message);
    }

    public void sendWelcomeEmail(String toEmail, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Welcome to " + appName + "! 🌸");
        message.setText(
            "Hi " + (name != null ? name : "Beautiful") + ",\n\n" +
            "Welcome to " + appName + "!\n\n" +
            "With love,\nThe " + appName + " Team 🌸"
        );
        mailSender.send(message);
    }

    public void sendHrNotification(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
