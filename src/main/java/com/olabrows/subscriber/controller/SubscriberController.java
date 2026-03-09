package com.olabrows.subscriber.controller;

import com.olabrows.subscriber.model.Subscriber;
import com.olabrows.subscriber.service.SubscriberService;
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
    private EmailService emailService;

    @GetMapping
    public ResponseEntity<List<Subscriber>> getAllSubscribers() {
        return ResponseEntity.ok(subscriberService.getAllSubscribers());
    }

    @PostMapping
    public ResponseEntity<Subscriber> subscribe(@RequestBody Subscriber subscriber) {
        Subscriber saved = subscriberService.subscribe(subscriber);
        try {
            String name = saved.getName() != null ? saved.getName() : "Beautiful";
            String welcomeHtml = "<div style='font-family:Georgia,serif;max-width:560px;margin:0 auto;padding:40px 20px;color:#1a0f0f'>" +
                "<h1 style='font-size:2rem;font-weight:400;margin-bottom:0.5rem'>Welcome to the <em style='color:#8b4a4a'>Inner Circle</em></h1>" +
                "<p style='color:#8b4a4a;font-size:0.75rem;letter-spacing:0.2em;text-transform:uppercase;margin-bottom:2rem'>Browed by Olá</p>" +
                "<p style='font-size:1rem;line-height:1.8;margin-bottom:1rem'>Hi " + name + " 🌸</p>" +
                "<p style='font-size:1rem;line-height:1.8;margin-bottom:1rem'>You're officially on the list! Thank you for joining the Browed by Olá family.</p>" +
                "<p style='font-size:1rem;line-height:1.8;margin-bottom:1rem'>As a founding member, you'll get:</p>" +
                "<ul style='font-size:0.95rem;line-height:2;color:#555;margin-bottom:2rem'>" +
                "<li>✦ <strong>15% off</strong> your first order</li>" +
                "<li>✦ Early access before the public launch</li>" +
                "<li>✦ Exclusive shade previews & behind-the-scenes content</li>" +
                "</ul>" +
                "<p style='font-size:1rem;line-height:1.8;margin-bottom:2rem'>We're putting the finishing touches on something truly special. Stay close — it's almost time.</p>" +
                "<p style='font-family:Georgia,serif;font-size:1.5rem;font-style:italic;color:#8b4a4a'>With love,<br>— Olá</p>" +
                "<hr style='border:none;border-top:1px solid #f2ddd8;margin:2rem 0'/>" +
                "<p style='font-size:0.75rem;color:#aaa;text-align:center'><a href='https://olabrows.store' style='color:#c49090'>olabrows.store</a> · You're receiving this because you signed up for early access.</p>" +
                "</div>";
            emailService.sendEmail(saved.getEmail(), "Welcome to Browed by Olá 🌸", welcomeHtml);
        } catch (Exception e) {
            System.err.println("Welcome email failed: " + e.getMessage());
        }
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<Void> unsubscribe(@PathVariable String email) {
        subscriberService.unsubscribe(email);
        return ResponseEntity.ok().build();
    }
}
