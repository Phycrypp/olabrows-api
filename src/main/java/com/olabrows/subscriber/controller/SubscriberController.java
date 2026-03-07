package com.olabrows.subscriber.controller;

import com.olabrows.subscriber.model.Subscriber;
import com.olabrows.subscriber.service.SubscriberService;
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

    @GetMapping
    public ResponseEntity<List<Subscriber>> getAllSubscribers() {
        return ResponseEntity.ok(subscriberService.getAllSubscribers());
    }

    @PostMapping
    public ResponseEntity<Subscriber> subscribe(@RequestBody Subscriber subscriber) {
        return ResponseEntity.ok(subscriberService.subscribe(subscriber));
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<Void> unsubscribe(@PathVariable String email) {
        subscriberService.unsubscribe(email);
        return ResponseEntity.ok().build();
    }
}
