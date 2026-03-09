package com.olabrows.subscriber.controller;

import com.olabrows.subscriber.model.Subscriber;
import com.olabrows.subscriber.service.SubscriberService;
import com.olabrows.subscriber.repository.SubscriberRepository;
import com.olabrows.email.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/subscribers")
@CrossOrigin(origins = "*")
public class SubscriberController {

    @Autowired
    private SubscriberService subscriberService;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private EmailService emailService;

    @GetMapping
    public ResponseEntity<List<Subscriber>> getAllSubscribers() {
        return ResponseEntity.ok(subscriberService.getAllSubscribers());
    }

    @PostMapping
    public ResponseEntity<Subscriber> subscribe(@RequestBody Subscriber subscriber) {
        boolean isNew = subscriberRepository.findByEmail(subscriber.getEmail()).isEmpty();
        Subscriber saved = subscriberService.subscribe(subscriber);
        if (isNew) {
            try {
                emailService.sendWelcomeEmail(saved.getEmail(), saved.getName());
            } catch (Exception e) {
                System.err.println("Welcome email failed: " + e.getMessage());
            }
        }
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<Void> unsubscribe(@PathVariable String email) {
        subscriberService.unsubscribe(email);
        return ResponseEntity.ok().build();
    }
}
