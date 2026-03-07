package com.olabrows.email.controller;

import com.olabrows.subscriber.model.Subscriber;
import com.olabrows.subscriber.repository.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = "*")
public class EmailController {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @PostMapping("/broadcast")
    public ResponseEntity<?> broadcast(@RequestBody Map<String, String> payload) {
        String subject = payload.get("subject");
        String body = payload.get("body");

        List<Subscriber> subscribers = subscriberRepository.findAll();
        int sent = 0;
        String lastError = null;

        for (Subscriber s : subscribers) {
            if (Boolean.TRUE.equals(s.getActive())) {
                try {
                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true);
                    helper.setFrom("hello@olabrows.store", "Browed by Olá");
                    helper.setTo(s.getEmail());
                    helper.setSubject(subject);
                    helper.setText(body, true);
                    mailSender.send(message);
                    sent++;
                } catch (Exception e) {
                    lastError = e.getMessage();
                    e.printStackTrace();
                }
            }
        }

        return ResponseEntity.ok(Map.of(
            "message", "Broadcast complete",
            "sent", sent,
            "total", subscribers.size(),
            "error", lastError != null ? lastError : "none"
        ));
    }
}
