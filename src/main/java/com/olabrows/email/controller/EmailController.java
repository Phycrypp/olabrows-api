package com.olabrows.email.controller;

import com.olabrows.email.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import com.olabrows.subscriber.repository.SubscriberRepository;
import com.olabrows.subscriber.model.Subscriber;
import jakarta.mail.internet.MimeMessage;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Value("${app.email}")
    private String fromEmail;

    @Value("${app.name}")
    private String appName;

    // Send to ALL subscribers
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> broadcast(
            @RequestHeader("Authorization") String auth,
            @RequestBody Map<String, String> payload) {
        try {
            String subject = payload.get("subject");
            String body = payload.get("body");
            List<Subscriber> subscribers = subscriberRepository.findByActiveTrue();
            int sent = 0;
            for (Subscriber s : subscribers) {
                try {
                    sendHtml(s.getEmail(), subject, body);
                    sent++;
                } catch (Exception e) {
                    System.err.println("Failed to send to " + s.getEmail() + ": " + e.getMessage());
                }
            }
            return ResponseEntity.ok(Map.of("success", true, "sent", sent));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Send to ONE specific email
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendToOne(
            @RequestHeader("Authorization") String auth,
            @RequestBody Map<String, String> payload) {
        try {
            String to = payload.get("to");
            String subject = payload.get("subject");
            String body = payload.get("body");
            sendHtml(to, subject, body);
            return ResponseEntity.ok(Map.of("success", true, "sent", 1));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private void sendHtml(String to, String subject, String body) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail, appName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);
        mailSender.send(message);
    }
}
